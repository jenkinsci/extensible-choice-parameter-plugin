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

import static org.junit.Assert.*;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jenkins.model.Jenkins;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.AddEditedChoiceListProvider.WhenToAdd;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

/**
 * Tests for GlobalTextareaChoiceListProvider, corresponding to Jenkins.
 *
 */
public class GlobalTextareaChoiceListProviderJenkinsTest
{
    @Rule
    public ExtensibleChoiceParameterJenkinsRule j = new ExtensibleChoiceParameterJenkinsRule();
    
    private static GlobalTextareaChoiceListProvider.DescriptorImpl getDescriptor()
    {
        return (GlobalTextareaChoiceListProvider.DescriptorImpl)Jenkins.getInstance().getDescriptor(GlobalTextareaChoiceListProvider.class);
        //return new GlobalTextareaChoiceListProvider.DescriptorImpl();
    }
    
    @Test
    public void testDescriptorSetChoiceListEntryList()
    {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry validEntry1 = new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry validEntry2 = new GlobalTextareaChoiceListEntry("entry2", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry validEntry3 = new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry invalidEntry1 = new GlobalTextareaChoiceListEntry("!invalid1", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry invalidEntry2 = new GlobalTextareaChoiceListEntry("in valid2", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry invalidEntry3 = new GlobalTextareaChoiceListEntry("3invalid", "value1\nvalue2\n", false);
        
        // all entries are valid
        {
            List<GlobalTextareaChoiceListEntry> passed =
                    Arrays.asList(validEntry1, validEntry2, validEntry3);
            List<GlobalTextareaChoiceListEntry> expected =
                    Arrays.asList(validEntry1, validEntry2, validEntry3);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("all entries are valid", expected, descriptor.getChoiceListEntryList());
        }
        
        // empty
        {
            List<GlobalTextareaChoiceListEntry> passed = new ArrayList<GlobalTextareaChoiceListEntry>(0);
            List<GlobalTextareaChoiceListEntry> expected = new ArrayList<GlobalTextareaChoiceListEntry>(0);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("empty", expected, descriptor.getChoiceListEntryList());
        }
        
        // null
        {
            List<GlobalTextareaChoiceListEntry> passed = null;
            List<GlobalTextareaChoiceListEntry> expected = new ArrayList<GlobalTextareaChoiceListEntry>(0);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("null", expected, descriptor.getChoiceListEntryList());
        }
        
        // contains invalid entries
        {
            List<GlobalTextareaChoiceListEntry> passed =
                    Arrays.asList(invalidEntry1, validEntry1, validEntry2, invalidEntry2, validEntry3, invalidEntry3);
            List<GlobalTextareaChoiceListEntry> expected =
                    Arrays.asList(validEntry1, validEntry2, validEntry3);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("contains invalid entries", expected, descriptor.getChoiceListEntryList());
        }
        
        // all entries are invalid
        {
            List<GlobalTextareaChoiceListEntry> passed =
                    Arrays.asList(invalidEntry1, invalidEntry2, invalidEntry3);
            List<GlobalTextareaChoiceListEntry> expected = new ArrayList<GlobalTextareaChoiceListEntry>(0);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("all entries are invalid", expected, descriptor.getChoiceListEntryList());
        }
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
    
    @SuppressWarnings("unchecked")  // CollectionUtils.transformedCollection is not generic.
    @Test
    public void testDescriptorDoFillNameItems()
    {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        // Easy case
        {
            List<String> nameList = Arrays.asList(
                    "entry1",
                    "entry2",
                    "entry3"
            );
            @SuppressWarnings("rawtypes")
            List choiceListEntry = new ArrayList(nameList);
            CollectionUtils.transform(
                    choiceListEntry,
                    new Transformer(){
                        @Override
                        public Object transform(Object input)
                        {
                            return new GlobalTextareaChoiceListEntry((String)input, "value1\nvalue2\n", false);
                        }
                    }
            );
            descriptor.setChoiceListEntryList(choiceListEntry);
            
            ListBoxModel fillList = descriptor.doFillNameItems();
            
            @SuppressWarnings("rawtypes")
            List optionList = new ArrayList(nameList);
            CollectionUtils.transform(
                    optionList,
                    new Transformer(){
                        @Override
                        public Object transform(Object input)
                        {
                            String name = (String)input;
                            return new ListBoxModel.Option(name, name);
                        }
                    }
            );
            ListBoxModel expected = new ListBoxModel(optionList);
            
            assertListBoxEquals("Easy case", expected, fillList);
        }
        
        // Empty
        {
            descriptor.setChoiceListEntryList(new ArrayList<GlobalTextareaChoiceListEntry>(0));
            
            ListBoxModel fillList = descriptor.doFillNameItems();
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("Empty", expected, fillList);
        }
        
        // null
        {
            descriptor.setChoiceListEntryList(null);
            
            ListBoxModel fillList = descriptor.doFillNameItems();
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("null", expected, fillList);
        }
    }
    
    @Test
    public void testDoFillDefaultChoiceItems()
    {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        
        // Easy case
        {
            List<GlobalTextareaChoiceListEntry> choiceListEntry = Arrays.asList(
                    new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2", false),
                    new GlobalTextareaChoiceListEntry("entry2", "value3\nvalue4", false)
            );
            descriptor.setChoiceListEntryList(choiceListEntry);
            
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems("entry2");
            
            ListBoxModel expected = new ListBoxModel();
            expected.add(new ListBoxModel.Option("value3", "value3"));
            expected.add(new ListBoxModel.Option("value4", "value4"));
            
            assertListBoxEquals("Easy case", expected, fillList.subList(1, fillList.size()));
        }
        
        // No match
        {
            List<GlobalTextareaChoiceListEntry> choiceListEntry = Arrays.asList(
                    new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2", false),
                    new GlobalTextareaChoiceListEntry("entry2", "value3\nvalue4", false)
            );
            descriptor.setChoiceListEntryList(choiceListEntry);
            
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems("entry3");
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("No match", expected, fillList.subList(1, fillList.size()));
        }
        
        // Empty
        {
            List<GlobalTextareaChoiceListEntry> choiceListEntry = new ArrayList<GlobalTextareaChoiceListEntry>(0);
            descriptor.setChoiceListEntryList(choiceListEntry);
            
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems("entry2");
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("Empty", expected, fillList.subList(1, fillList.size()));
        }
        
        // null
        {
            descriptor.setChoiceListEntryList(null);
            
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems("entry2");
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("null", expected, fillList.subList(1, fillList.size()));
        }
        
        // null is selected
        {
            List<GlobalTextareaChoiceListEntry> choiceListEntry = Arrays.asList(
                    new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2", false),
                    new GlobalTextareaChoiceListEntry("entry2", "value3\nvalue4", false)
            );
            descriptor.setChoiceListEntryList(choiceListEntry);
            
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems(null);
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("null is selected", expected, fillList.subList(1, fillList.size()));
        }
        
        // in case not initialized
        {
            GlobalTextareaChoiceListProvider.DescriptorImpl nonInitializedDescriptor
                    = new GlobalTextareaChoiceListProvider.DescriptorImpl();
            ListBoxModel fillList = nonInitializedDescriptor.doFillDefaultChoiceItems("entry2");
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("not initialized", expected, fillList.subList(1, fillList.size()));
        }
    }
    
    @Test
    public void testDescriptorGetChoiceListEntry()
    {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry1 = new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry entry2 = new GlobalTextareaChoiceListEntry("entry2", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry entry3 = new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry sameEntry3 = new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n", false);
        
        // Easy case
        {
            descriptor.setChoiceListEntryList(Arrays.asList(entry1, entry2, entry3));
            
            assertEquals("Easy case1", entry1, descriptor.getChoiceListEntry("entry1"));
            assertEquals("Easy case2", entry2, descriptor.getChoiceListEntry("entry2"));
            assertEquals("Easy case3", entry3, descriptor.getChoiceListEntry("entry3"));
        }
        
        // duplicate
        {
            descriptor.setChoiceListEntryList(Arrays.asList(sameEntry3, entry1, entry2, entry3));
            assertEquals("Duplicate 1", sameEntry3, descriptor.getChoiceListEntry("entry3"));
            
            descriptor.setChoiceListEntryList(Arrays.asList(entry1, entry2, entry3, sameEntry3));
            assertEquals("Duplicate 2", entry3, descriptor.getChoiceListEntry("entry3"));
        }
        
        // No matches
        {
            descriptor.setChoiceListEntryList(Arrays.asList(entry1, entry2, entry3));
            
            assertEquals("No matches", null, descriptor.getChoiceListEntry("entryX"));
            assertEquals("No matches", new ArrayList<String>(0), descriptor.getChoiceList("entryX"));
        }
        
        // Empty
        {
            descriptor.setChoiceListEntryList(new ArrayList<GlobalTextareaChoiceListEntry>(0));
            
            assertEquals("Empty", null, descriptor.getChoiceListEntry("entry1"));
            assertEquals("Empty", new ArrayList<String>(0), descriptor.getChoiceList("entryX"));
        }
    }
    
    /**
     * Tests the submitted form is saved correctly,
     * and can be loaded correctly.
     * 
     * This is performed in following steps.
     * 1. update the descriptor to the state to be submitted.
     * 2. show form.
     * 3. submit the form.
     * 4. update the descriptor to another state.
     * 5. Re-construct the descriptor
     * 6. assert if the new created descriptor is in the state of 1.
     * @throws Exception 
     */
    @Test
    public void testDescriptorConfigure() throws Exception
    {
        WebClient wc = j.createWebClient();
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry validEntry1 = new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry validEntry2 = new GlobalTextareaChoiceListEntry("entry2", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry validEntry3 = new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry invalidEntry1 = new GlobalTextareaChoiceListEntry("!invalid1", "value1\nvalue2\n", false);
        
        // Simple submission
        {
            // update the descriptor to the state I want to submit.
            descriptor.setChoiceListEntryList(Arrays.asList(validEntry1, validEntry2, validEntry3));
            
            HtmlForm configForm = wc.goTo("/configure").getFormByName("config");
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);
            
            j.submit(configForm);
            
            assertEquals("Simple submission: descriptor after submission", 
                    Arrays.asList(validEntry1, validEntry2, validEntry3),
                    descriptor.getChoiceListEntryList()
            );
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);
            
            GlobalTextareaChoiceListProvider.DescriptorImpl newDescriptor
                = new GlobalTextareaChoiceListProvider.DescriptorImpl();
            
            assertEquals("Simple submission: descriptor serialized from config.xml",
                    Arrays.asList(validEntry1, validEntry2, validEntry3),
                    newDescriptor.getChoiceListEntryList()
            );
        }
        
        // Submission with invalid entry.
        {
            // update the descriptor to the state I want to submit.
            descriptor.setChoiceListEntryList(Arrays.asList(invalidEntry1, validEntry2, validEntry3));
            
            HtmlForm configForm = wc.goTo("/configure").getFormByName("config");
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);
            
            j.submit(configForm);
            
            assertEquals("Submission with invalid entry: descriptor after submission", 
                    Arrays.asList(validEntry2, validEntry3),
                    descriptor.getChoiceListEntryList()
            );
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);
            
            GlobalTextareaChoiceListProvider.DescriptorImpl newDescriptor
                = new GlobalTextareaChoiceListProvider.DescriptorImpl();
            
            assertEquals("Submission with invalid entry: descriptor serialized from config.xml",
                    Arrays.asList(validEntry2, validEntry3),
                    newDescriptor.getChoiceListEntryList()
            );
        }
        
        // Empty submission
        {
            // update the descriptor to the state I want to submit.
            descriptor.setChoiceListEntryList(null);
            
            HtmlForm configForm = wc.goTo("/configure").getFormByName("config");
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(Arrays.asList(validEntry1, validEntry2));
            
            j.submit(configForm);
            
            assertEquals("Empty submission: descriptor after submission", 
                    new ArrayList<GlobalTextareaChoiceListEntry>(0),
                    descriptor.getChoiceListEntryList()
            );
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(Arrays.asList(validEntry1, validEntry2));
            
            GlobalTextareaChoiceListProvider.DescriptorImpl newDescriptor
                = new GlobalTextareaChoiceListProvider.DescriptorImpl();
            
            assertEquals("Empty submission: descriptor serialized from config.xml",
                    new ArrayList<GlobalTextareaChoiceListEntry>(0),
                    newDescriptor.getChoiceListEntryList()
            );
        }
    }
    
    public static class CaptureChoiceListBuilder extends Builder
    {
        private final String entryName;
        private List<String> choiceList;
        
        public CaptureChoiceListBuilder(String entryName)
        {
            this.entryName = entryName;
        }
        
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                BuildListener listener) throws InterruptedException,
                IOException
        {
            GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = GlobalTextareaChoiceListProviderJenkinsTest.getDescriptor();
            GlobalTextareaChoiceListEntry entry = descriptor.getChoiceListEntry(entryName);
            choiceList = new ArrayList<String>(entry.getChoiceList());
            
            return true;
        }
        
        public List<String> getChoiceList()
        {
            return choiceList;
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
            if(result != null)
            {
                build.setResult(result);
            }
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
    
    private boolean _testEditedValueWillBeContained(String jobname, Result result, String entryname, String defname, String value) throws Exception
    {
        // result == null stands for triggered.
        
        List<String> choiceList = null;
        
        FreeStyleProject job = (FreeStyleProject)Jenkins.getInstance().getItem(jobname);
        
        job.getBuildersList().clear();
        
        // Used for capture choiceList after triggered.
        CaptureChoiceListBuilder ccb = new CaptureChoiceListBuilder(entryname);
        job.getBuildersList().add(ccb);
        
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        job.getBuildersList().add(ceb);
        
        job.getPublishersList().clear();
        job.getPublishersList().add(new SetBuildResultPublisher(result));
        
        job.save();
        
        WebClient wc = j.createAllow405WebClient();
        
        HtmlPage page = wc.getPage(job, "build?delay=0sec");
        
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
        j.submit(form);
        
        if(result == null)
        {
            choiceList = ccb.getChoiceList();
            result = Result.SUCCESS;
        }
        
        j.waitUntilNoActivity();
        
        j.assertBuildStatus(result, job.getLastBuild());
        assertEquals("build launched with unexpected value", value, ceb.getEnvVars().get(defname));
        
        if(choiceList == null)
        {
            // reload configuration to test saved configuration.
            Jenkins.getInstance().reload();
            
            GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
            GlobalTextareaChoiceListEntry entry = descriptor.getChoiceListEntry(entryname);
            choiceList = new ArrayList<String>(entry.getChoiceList());
        }
        
        return choiceList.contains(value);
    }
    
    @Test
    public void testAddEditedValue_Disabled1() throws Exception
    {
        String varname = "test";
        String entryname = "test";
        
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(Arrays.asList(entry));
        descriptor.save();
        
        GlobalTextareaChoiceListProvider provider = new GlobalTextareaChoiceListProvider(
                entryname,
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
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, null, entryname, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.SUCCESS, entryname, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.UNSTABLE, entryname, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.FAILURE, entryname, varname, value));
        }
    }
    
    @Test
    public void testAddEditedValue_Disabled2() throws Exception
    {
        String varname = "test";
        String entryname = "test";
        
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", false);
        descriptor.setChoiceListEntryList(Arrays.asList(entry));
        descriptor.save();
        
        GlobalTextareaChoiceListProvider provider = new GlobalTextareaChoiceListProvider(
                entryname,
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
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, null, entryname, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.SUCCESS, entryname, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.UNSTABLE, entryname, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.FAILURE, entryname, varname, value));
        }
    }
    
    @Test
    public void testAddEditedValue_Disabled3() throws Exception
    {
        String varname = "test";
        String entryname = "test";
        
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(Arrays.asList(entry));
        descriptor.save();
        
        GlobalTextareaChoiceListProvider provider = new GlobalTextareaChoiceListProvider(
                entryname,
                null,
                true,
                null
        );
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                varname,
                provider,
                true,
                "description"
                );
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, null, entryname, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.SUCCESS, entryname, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.UNSTABLE, entryname, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, Result.FAILURE, entryname, varname, value));
        }
    }
    
    @Test
    public void testAddEditedValue_Trigger() throws Exception
    {
        String varname = "test";
        String entryname = "test";
        
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(Arrays.asList(entry));
        descriptor.save();
        
        GlobalTextareaChoiceListProvider provider = new GlobalTextareaChoiceListProvider(
                entryname,
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
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
    }
    
    @Test
    public void testAddEditedValue_Completed() throws Exception
    {
        String varname = "test";
        String entryname = "test";
        
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(Arrays.asList(entry));
        descriptor.save();
        
        GlobalTextareaChoiceListProvider provider = new GlobalTextareaChoiceListProvider(
                entryname,
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
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, entryname,varname, value));
        }
        
        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
    }
    
    @Test
    public void testAddEditedValue_CompletedStable() throws Exception
    {
        String varname = "test";
        String entryname = "test";
        
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(Arrays.asList(entry));
        descriptor.save();
        
        GlobalTextareaChoiceListProvider provider = new GlobalTextareaChoiceListProvider(
                entryname,
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
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
    }
    
    @Test
    public void testAddEditedValue_CompletedUnstable() throws Exception
    {
        String varname = "test";
        String entryname = "test";
        
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(Arrays.asList(entry));
        descriptor.save();
        
        GlobalTextareaChoiceListProvider provider = new GlobalTextareaChoiceListProvider(
                entryname,
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
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();
        
        String jobname = job.getName();
        
        
        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue("Edited value must be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
        
        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertFalse("Edited value must not be containd", _testEditedValueWillBeContained(jobname, result, entryname, varname, value));
        }
    }
}
