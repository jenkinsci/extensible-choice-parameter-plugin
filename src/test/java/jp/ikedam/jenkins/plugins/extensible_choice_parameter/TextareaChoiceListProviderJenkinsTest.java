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

import static org.junit.jupiter.api.Assertions.*;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.AddEditedChoiceListProvider.WhenToAdd;
import org.htmlunit.ElementNotFoundException;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlOption;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.htmlunit.html.HtmlTextInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for TextareaChoiceListProvider, corresponding to Jenkins.
 */
@WithJenkins
class TextareaChoiceListProviderJenkinsTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    private TextareaChoiceListProvider.DescriptorImpl getDescriptor() {
        return (TextareaChoiceListProvider.DescriptorImpl)
                Jenkins.get().getDescriptor(TextareaChoiceListProvider.class);
    }

    private static void assertListBoxEquals(
            String message, List<ListBoxModel.Option> expected, List<ListBoxModel.Option> test) {
        assertEquals(expected.size(), test.size(), message);
        for (int i = 0; i < test.size(); ++i) {
            assertEquals(expected.get(i).name, test.get(i).name, String.format("%s: %d-th name", message, i));
            assertEquals(expected.get(i).value, test.get(i).value, String.format("%s: %d-th value", message, i));
        }
    }

    @Test
    void testDoFillDefaultChoiceItems() {
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

    public static class CaptureChoiceListBuilder extends Builder {
        private final String defname;
        private List<String> choiceList;

        public CaptureChoiceListBuilder(String defname) {
            this.defname = defname;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            ExtensibleChoiceParameterDefinition def = (ExtensibleChoiceParameterDefinition) build.getParent()
                    .getRootProject()
                    .getProperty(ParametersDefinitionProperty.class)
                    .getParameterDefinition(defname);
            choiceList = new ArrayList<>(def.getChoiceList());

            return true;
        }

        public List<String> getChoiceList() {
            return choiceList;
        }
    }

    public static class SetBuildResultPublisher extends Recorder {
        private Result result;

        public SetBuildResultPublisher(Result result) {
            this.result = result;
        }

        @Override
        public BuildStepMonitor getRequiredMonitorService() {
            return BuildStepMonitor.NONE;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            if (result != null) {
                build.setResult(result);
            }
            return true;
        }

        public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
            @Override
            public boolean isApplicable(Class<? extends AbstractProject> jobType) {
                return true;
            }

            @Override
            public String getDisplayName() {
                return "UnstablePublisher";
            }
        }

        private static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        @SuppressWarnings({"rawtypes"})
        @Override
        public BuildStepDescriptor getDescriptor() {
            return DESCRIPTOR;
        }
    }

    private boolean _testEditedValueWillBeContained(String jobname, Result result, String defname, String value)
            throws Exception {
        // result == null stands for triggered.

        List<String> choiceList = null;

        FreeStyleProject job = (FreeStyleProject) Jenkins.get().getItem(jobname);

        job.getBuildersList().clear();

        // Used for capture choiceList after triggered.
        CaptureChoiceListBuilder ccb = new CaptureChoiceListBuilder(defname);
        job.getBuildersList().add(ccb);

        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        job.getBuildersList().add(ceb);

        job.getPublishersList().clear();
        job.getPublishersList().add(new SetBuildResultPublisher(result));

        job.save();

        WebClient wc = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);

        HtmlPage page = wc.getPage(job, "build?delay=0sec");

        HtmlForm form = page.getFormByName("parameters");

        try {
            HtmlSelect select = form.getSelectByName("value");
            HtmlOption opt;
            try {
                opt = select.getOptionByText(value);
            } catch (ElementNotFoundException e) {
                int newOptPos = select.getOptionSize();
                // There's no such option, so create new option tag.
                DomElement newOpt = page.createElement("option");
                // this seems trim the value...
                // newOpt.setAttribute("value", value);
                newOpt.appendChild(page.createTextNode(value));
                select.appendChild(newOpt);

                opt = select.getOption(newOptPos);
                opt.setValueAttribute(value);
            }
            opt.setSelected(true);
        } catch (ElementNotFoundException e) {
            // selectbox was not found.
            // selectbox is replaced with input field.
            HtmlTextInput input = form.getInputByName("value");
            input.setValue(value);
        }
        j.submit(form);

        if (result == null) {
            choiceList = ccb.getChoiceList();
            result = Result.SUCCESS;
        }

        j.waitUntilNoActivity();

        j.assertBuildStatus(result, job.getLastBuild());
        assertEquals(value, ceb.getEnvVars().get(defname), "build launched with unexpected value");

        if (choiceList == null) {
            // reload configuration to test saved configuration.
            Jenkins.get().reload();

            job = (FreeStyleProject) Jenkins.get().getItem(jobname);
            ExtensibleChoiceParameterDefinition def = (ExtensibleChoiceParameterDefinition)
                    job.getProperty(ParametersDefinitionProperty.class).getParameterDefinition(defname);
            choiceList = new ArrayList<>(def.getChoiceList());
        }

        return choiceList.contains(value);
    }

    @Test
    void testAddEditedValue_Disabled() throws Exception {
        String varname = "test";
        TextareaChoiceListProvider provider = new TextareaChoiceListProvider("a\nb\nc", null, false, null);
        ExtensibleChoiceParameterDefinition def =
                new ExtensibleChoiceParameterDefinition(varname, provider, true, "description");
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();

        String jobname = job.getName();

        // Triggered
        {
            String value = "Triggered";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, null, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.SUCCESS, varname, value),
                    "Edited value must not be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.UNSTABLE, varname, value),
                    "Edited value must not be contained");
        }

        // Failure
        {
            String value = "Failure";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.FAILURE, varname, value),
                    "Edited value must not be contained");
        }
    }

    @Test
    void testAddEditedValue_Trigger() throws Exception {
        String varname = "test";
        TextareaChoiceListProvider provider =
                new TextareaChoiceListProvider("a\nb\nc", null, true, WhenToAdd.Triggered);
        ExtensibleChoiceParameterDefinition def =
                new ExtensibleChoiceParameterDefinition(varname, provider, true, "description");
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();

        String jobname = job.getName();

        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }

        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }

        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }
    }

    @Test
    void testAddEditedValue_Completed() throws Exception {
        String varname = "test";
        TextareaChoiceListProvider provider =
                new TextareaChoiceListProvider("a\nb\nc", null, true, WhenToAdd.Completed);
        ExtensibleChoiceParameterDefinition def =
                new ExtensibleChoiceParameterDefinition(varname, provider, true, "description");
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();

        String jobname = job.getName();

        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertFalse(
                    _testEditedValueWillBeContained(jobname, result, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }

        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }
    }

    @Test
    void testAddEditedValue_CompletedStable() throws Exception {
        String varname = "test";
        TextareaChoiceListProvider provider =
                new TextareaChoiceListProvider("a\nb\nc", null, true, WhenToAdd.CompletedStable);
        ExtensibleChoiceParameterDefinition def =
                new ExtensibleChoiceParameterDefinition(varname, provider, true, "description");
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();

        String jobname = job.getName();

        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertFalse(
                    _testEditedValueWillBeContained(jobname, result, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertFalse(
                    _testEditedValueWillBeContained(jobname, result, varname, value),
                    "Edited value must not be contained");
        }

        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertFalse(
                    _testEditedValueWillBeContained(jobname, result, varname, value),
                    "Edited value must not be contained");
        }
    }

    @Test
    void testAddEditedValue_CompletedUnstable() throws Exception {
        String varname = "test";
        TextareaChoiceListProvider provider =
                new TextareaChoiceListProvider("a\nb\nc", null, true, WhenToAdd.CompletedUnstable);
        ExtensibleChoiceParameterDefinition def =
                new ExtensibleChoiceParameterDefinition(varname, provider, true, "description");
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        job.save();

        String jobname = job.getName();

        // Triggered
        {
            String value = "Triggered";
            Result result = null;
            assertFalse(
                    _testEditedValueWillBeContained(jobname, result, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, varname, value), "Edited value must be contained");
        }

        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertFalse(
                    _testEditedValueWillBeContained(jobname, result, varname, value),
                    "Edited value must not be contained");
        }
    }

    @Test
    void testConfiguration1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new TextareaChoiceListProvider("a\nb\nc", null, false, WhenToAdd.Completed),
                false,
                "description");
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test"));
    }

    @Test
    void testConfiguration2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new TextareaChoiceListProvider("a\nb\nc\n", "b", true, WhenToAdd.Triggered),
                true,
                "another description");
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test"));
    }
}
