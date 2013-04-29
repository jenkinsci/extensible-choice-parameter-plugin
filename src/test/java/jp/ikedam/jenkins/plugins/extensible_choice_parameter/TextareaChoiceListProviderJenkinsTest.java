/*
 * The MIT License
 * 
 * Copyright (c) 2012-2013 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.AddEditedChoiceListProvider.WhenToAdd;

import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.ExtensiableChoiceParameterJenkinsTestCase;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

/**
 * Tests for TextareaChoiceListProvider, corresponding to Jenkins.
 */
public class TextareaChoiceListProviderJenkinsTest extends ExtensiableChoiceParameterJenkinsTestCase
{
    private TextareaChoiceListProvider.DescriptorImpl getDescriptor()
    {
        return (TextareaChoiceListProvider.DescriptorImpl)Jenkins.getInstance().getDescriptor(TextareaChoiceListProvider.class);
    }
    
    static private void assertListBoxEquals(String message, List<ListBoxModel.Option> expected, List<ListBoxModel.Option> test)
    {
        assertEquals(message, expected.size(), test.size());
        for(int i = 0; i < test.size(); ++i)
        {
            assertEquals(String.format("%s: %d-th name", message, i), expected.get(i).name, test.get(i).name);
            assertEquals(String.format("%s: %d-th value", message, i), expected.get(i).value, test.get(i).value);
        }
    }
    
    public void testDoFillDefaultChoiceItems()
    {
        TextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        
        // Easy case
        {
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems("value1\nvalue2");
            
            ListBoxModel expected = new ListBoxModel();
            expected.add(new ListBoxModel.Option("value1", "value1"));
            expected.add(new ListBoxModel.Option("value2", "value2"));
            
            assertListBoxEquals("Easy case", expected, fillList.subList(1, fillList.size()));
        }
        
        // Empty
        {
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems("");
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("Empty", expected, fillList.subList(1, fillList.size()));
        }
        
        // null
        {
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems(null);
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("null", expected, fillList.subList(1, fillList.size()));
        }
    }
    
    public static class SleepBuilder extends Builder
    {
        private long milliseconds;
        
        public SleepBuilder(long milliseconds)
        {
            this.milliseconds = milliseconds;
        }
        
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                BuildListener listener) throws InterruptedException,
                IOException
        {
            Thread.sleep(milliseconds);
            return true;
        }
    }

    public static class SetBuildResultPublisher extends Recorder
    {
        private Result result;
        
        public SetBuildResultPublisher(Result result)
        {
            this.result = result;
        }
        
        
        @Override
        public BuildStepMonitor getRequiredMonitorService()
        {
            return BuildStepMonitor.NONE;
        }
        
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                BuildListener listener) throws InterruptedException,
                IOException
        {
            build.setResult(result);
            return true;
        }
        
        public static class DescriptorImpl extends BuildStepDescriptor<Publisher>
        {
            @Override
            public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType)
            {
                return true;
            }
            @Override
            public String getDisplayName()
            {
                return "UnstablePublisher";
            }
        }
        
        private static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
        
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public BuildStepDescriptor getDescriptor()
        {
            return DESCRIPTOR;
        }
    }
    
    private boolean _testEditedValueWillBeContained(String jobname, Result result, String defname, String value) throws Exception
    {
        // result == null stands for triggered.
        
        List<String> choiceList = null;
        
        FreeStyleProject job = (FreeStyleProject)Jenkins.getInstance().getItem(jobname);
        
        job.getBuildersList().clear();
        
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        job.getBuildersList().add(ceb);
        
        job.getPublishersList().clear();
        
        if(result == null)
        {
            job.getBuildersList().add(new SleepBuilder(5000));
        }
        else
        {
            job.getPublishersList().add(new SetBuildResultPublisher(result));
        }
        
        job.save();
        
        WebClient wc = new WebClient();
        
        wc.setPrintContentOnFailingStatusCode(false);
        wc.setThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.getPage(job, "build?delay=0sec");
        wc.setThrowExceptionOnFailingStatusCode(true);
        wc.setPrintContentOnFailingStatusCode(true);
        
        HtmlForm form = page.getFormByName("parameters");
        
        try
        {
            HtmlSelect select = form.getSelectByName("value");
            HtmlOption opt;
            try
            {
                opt = select.getOptionByText(value);
            }
            catch(ElementNotFoundException e)
            {
                int newOptPos = select.getOptionSize();
                // There's no such option, so create new option tag.
                DomElement newOpt = page.createElement("option");
                // this seems trim the value...
                //newOpt.setAttribute("value", value);
                newOpt.appendChild(page.createTextNode(value));
                select.appendChild(newOpt);
                
                opt = select.getOption(newOptPos);
                opt.setValueAttribute(value);
            }
            opt.setSelected(true);
        }
        catch(ElementNotFoundException e)
        {
            // selectbox was not found.
            // selectbox is replaced with input field.
            HtmlTextInput input = (HtmlTextInput)form.getInputByName("value");
            input.setValueAttribute(value);
        }
        submit(form);
        
        FreeStyleBuild build = job.getLastBuild();
        
        if(result == null)
        {
            // There's no way to test saved configuration...
            ExtensibleChoiceParameterDefinition def = (ExtensibleChoiceParameterDefinition)job.getProperty(ParametersDefinitionProperty.class).getParameterDefinition(defname);
            choiceList = new ArrayList<String>(def.getChoiceList());
            assertTrue("Build finished too early...", build.isBuilding());
            
            result = Result.SUCCESS;
        }
        
        while (build.isBuilding()) Thread.sleep(100);
        
        assertEquals("build result differs from expected state.", result, build.getResult());
        assertEquals("build launched with unexpected value", value, ceb.getEnvVars().get(defname));
        
        if(choiceList == null)
        {
            // reload configuration to test saved configuration.
            Jenkins.getInstance().reload();
            
            job = (FreeStyleProject)Jenkins.getInstance().getItem(jobname);
            ExtensibleChoiceParameterDefinition def = (ExtensibleChoiceParameterDefinition)job.getProperty(ParametersDefinitionProperty.class).getParameterDefinition(defname);
            choiceList = new ArrayList<String>(def.getChoiceList());
        }
        
        return choiceList.contains(value);
    }
    
    public void testAddEditedValue_Disabled() throws Exception
    {
        String varname = "test";
        TextareaChoiceListProvider provider = new TextareaChoiceListProvider(
                "a\nb\nc",
                null,
                false,
                null
        );
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                varname,
                provider,
                true,
                "description"
                );
        FreeStyleProject job = createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, null, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.SUCCESS, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.UNSTABLE, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.FAILURE, varname, value));
        }
    }
    
    public void testAddEditedValue_Trigger() throws Exception
    {
        String varname = "test";
        TextareaChoiceListProvider provider = new TextareaChoiceListProvider(
                "a\nb\nc",
                null,
                true,
                WhenToAdd.Triggered
        );
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                varname,
                provider,
                true,
                "description"
                );
        FreeStyleProject job = createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
    }
    
    public void testAddEditedValue_Completed() throws Exception
    {
        String varname = "test";
        TextareaChoiceListProvider provider = new TextareaChoiceListProvider(
                "a\nb\nc",
                null,
                true,
                WhenToAdd.Completed
        );
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                varname,
                provider,
                true,
                "description"
                );
        FreeStyleProject job = createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
    }
    
    public void testAddEditedValue_CompletedStable() throws Exception
    {
        String varname = "test";
        TextareaChoiceListProvider provider = new TextareaChoiceListProvider(
                "a\nb\nc",
                null,
                true,
                WhenToAdd.CompletedStable
        );
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                varname,
                provider,
                true,
                "description"
                );
        FreeStyleProject job = createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
    }
    
    
    public void testAddEditedValue_CompletedUnstable() throws Exception
    {
        String varname = "test";
        TextareaChoiceListProvider provider = new TextareaChoiceListProvider(
                "a\nb\nc",
                null,
                true,
                WhenToAdd.CompletedUnstable
        );
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                varname,
                provider,
                true,
                "description"
                );
        FreeStyleProject job = createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, varname, value));
        }
    }
}
