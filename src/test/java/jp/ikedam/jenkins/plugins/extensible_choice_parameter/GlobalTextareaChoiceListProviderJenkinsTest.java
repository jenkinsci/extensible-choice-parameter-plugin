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
import java.util.Arrays;
import java.util.List;
import jenkins.model.Jenkins;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.AddEditedChoiceListProvider.WhenToAdd;
import org.apache.commons.collections.CollectionUtils;
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
 * Tests for GlobalTextareaChoiceListProvider, corresponding to Jenkins.
 *
 */
@WithJenkins
class GlobalTextareaChoiceListProviderJenkinsTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    private static GlobalTextareaChoiceListProvider.DescriptorImpl getDescriptor() {
        return (GlobalTextareaChoiceListProvider.DescriptorImpl)
                Jenkins.get().getDescriptor(GlobalTextareaChoiceListProvider.class);
        // return new GlobalTextareaChoiceListProvider.DescriptorImpl();
    }

    @Test
    void testDescriptorSetChoiceListEntryList() {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry validEntry1 =
                new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry validEntry2 =
                new GlobalTextareaChoiceListEntry("entry2", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry validEntry3 =
                new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry invalidEntry1 =
                new GlobalTextareaChoiceListEntry("!invalid1", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry invalidEntry2 =
                new GlobalTextareaChoiceListEntry("in valid2", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry invalidEntry3 =
                new GlobalTextareaChoiceListEntry("3invalid", "value1\nvalue2\n", false);

        // all entries are valid
        {
            List<GlobalTextareaChoiceListEntry> passed = Arrays.asList(validEntry1, validEntry2, validEntry3);
            List<GlobalTextareaChoiceListEntry> expected = Arrays.asList(validEntry1, validEntry2, validEntry3);
            descriptor.setChoiceListEntryList(passed);
            assertEquals(expected, descriptor.getChoiceListEntryList(), "all entries are valid");
        }

        // empty
        {
            List<GlobalTextareaChoiceListEntry> passed = new ArrayList<>(0);
            List<GlobalTextareaChoiceListEntry> expected = new ArrayList<>(0);
            descriptor.setChoiceListEntryList(passed);
            assertEquals(expected, descriptor.getChoiceListEntryList(), "empty");
        }

        // null
        {
            List<GlobalTextareaChoiceListEntry> passed = null;
            List<GlobalTextareaChoiceListEntry> expected = new ArrayList<>(0);
            descriptor.setChoiceListEntryList(passed);
            assertEquals(expected, descriptor.getChoiceListEntryList(), "null");
        }

        // contains invalid entries
        {
            List<GlobalTextareaChoiceListEntry> passed =
                    Arrays.asList(invalidEntry1, validEntry1, validEntry2, invalidEntry2, validEntry3, invalidEntry3);
            List<GlobalTextareaChoiceListEntry> expected = Arrays.asList(validEntry1, validEntry2, validEntry3);
            descriptor.setChoiceListEntryList(passed);
            assertEquals(expected, descriptor.getChoiceListEntryList(), "contains invalid entries");
        }

        // all entries are invalid
        {
            List<GlobalTextareaChoiceListEntry> passed = Arrays.asList(invalidEntry1, invalidEntry2, invalidEntry3);
            List<GlobalTextareaChoiceListEntry> expected = new ArrayList<>(0);
            descriptor.setChoiceListEntryList(passed);
            assertEquals(expected, descriptor.getChoiceListEntryList(), "all entries are invalid");
        }
    }

    private static void assertListBoxEquals(
            String message, List<ListBoxModel.Option> expected, List<ListBoxModel.Option> test) {
        assertEquals(expected.size(), test.size(), message);
        for (int i = 0; i < test.size(); ++i) {
            assertEquals(expected.get(i).name, test.get(i).name, String.format("%s: %d-th name", message, i));
            assertEquals(expected.get(i).value, test.get(i).value, String.format("%s: %d-th value", message, i));
        }
    }

    @SuppressWarnings("unchecked")
    // CollectionUtils.transformedCollection is not generic.
    @Test
    void testDescriptorDoFillNameItems() {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        // Easy case
        {
            List<String> nameList = Arrays.asList("entry1", "entry2", "entry3");
            @SuppressWarnings("rawtypes")
            List choiceListEntry = new ArrayList(nameList);
            CollectionUtils.transform(
                    choiceListEntry,
                    input -> new GlobalTextareaChoiceListEntry((String) input, "value1\nvalue2\n", false));
            descriptor.setChoiceListEntryList(choiceListEntry);

            ListBoxModel fillList = descriptor.doFillNameItems();

            @SuppressWarnings("rawtypes")
            List optionList = new ArrayList(nameList);
            CollectionUtils.transform(optionList, input -> {
                String name = (String) input;
                return new ListBoxModel.Option(name, name);
            });
            ListBoxModel expected = new ListBoxModel(optionList);

            assertListBoxEquals("Easy case", expected, fillList);
        }

        // Empty
        {
            descriptor.setChoiceListEntryList(new ArrayList<>(0));

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
    void testDoFillDefaultChoiceItems() {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        // Easy case
        {
            List<GlobalTextareaChoiceListEntry> choiceListEntry = Arrays.asList(
                    new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2", false),
                    new GlobalTextareaChoiceListEntry("entry2", "value3\nvalue4", false));
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
                    new GlobalTextareaChoiceListEntry("entry2", "value3\nvalue4", false));
            descriptor.setChoiceListEntryList(choiceListEntry);

            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems("entry3");

            ListBoxModel expected = new ListBoxModel();

            assertListBoxEquals("No match", expected, fillList.subList(1, fillList.size()));
        }

        // Empty
        {
            List<GlobalTextareaChoiceListEntry> choiceListEntry = new ArrayList<>(0);
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
                    new GlobalTextareaChoiceListEntry("entry2", "value3\nvalue4", false));
            descriptor.setChoiceListEntryList(choiceListEntry);

            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems(null);

            ListBoxModel expected = new ListBoxModel();

            assertListBoxEquals("null is selected", expected, fillList.subList(1, fillList.size()));
        }

        // in case not initialized
        {
            GlobalTextareaChoiceListProvider.DescriptorImpl nonInitializedDescriptor =
                    new GlobalTextareaChoiceListProvider.DescriptorImpl();
            ListBoxModel fillList = nonInitializedDescriptor.doFillDefaultChoiceItems("entry2");

            ListBoxModel expected = new ListBoxModel();

            assertListBoxEquals("not initialized", expected, fillList.subList(1, fillList.size()));
        }
    }

    @Test
    void testDescriptorGetChoiceListEntry() {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry1 = new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry entry2 = new GlobalTextareaChoiceListEntry("entry2", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry entry3 = new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry sameEntry3 =
                new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n", false);

        // Easy case
        {
            descriptor.setChoiceListEntryList(Arrays.asList(entry1, entry2, entry3));

            assertEquals(entry1, descriptor.getChoiceListEntry("entry1"), "Easy case1");
            assertEquals(entry2, descriptor.getChoiceListEntry("entry2"), "Easy case2");
            assertEquals(entry3, descriptor.getChoiceListEntry("entry3"), "Easy case3");
        }

        // duplicate
        {
            descriptor.setChoiceListEntryList(Arrays.asList(sameEntry3, entry1, entry2, entry3));
            assertEquals(sameEntry3, descriptor.getChoiceListEntry("entry3"), "Duplicate 1");

            descriptor.setChoiceListEntryList(Arrays.asList(entry1, entry2, entry3, sameEntry3));
            assertEquals(entry3, descriptor.getChoiceListEntry("entry3"), "Duplicate 2");
        }

        // No matches
        {
            descriptor.setChoiceListEntryList(Arrays.asList(entry1, entry2, entry3));

            assertNull(descriptor.getChoiceListEntry("entryX"), "No matches");
            assertEquals(new ArrayList<String>(0), descriptor.getChoiceList("entryX"), "No matches");
        }

        // Empty
        {
            descriptor.setChoiceListEntryList(new ArrayList<>(0));

            assertNull(descriptor.getChoiceListEntry("entry1"), "Empty");
            assertEquals(new ArrayList<String>(0), descriptor.getChoiceList("entryX"), "Empty");
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
    void testDescriptorConfigure() throws Exception {
        WebClient wc = j.createWebClient();
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry validEntry1 =
                new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry validEntry2 =
                new GlobalTextareaChoiceListEntry("entry2", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry validEntry3 =
                new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n", false);
        GlobalTextareaChoiceListEntry invalidEntry1 =
                new GlobalTextareaChoiceListEntry("!invalid1", "value1\nvalue2\n", false);

        // Simple submission
        {
            // update the descriptor to the state I want to submit.
            descriptor.setChoiceListEntryList(Arrays.asList(validEntry1, validEntry2, validEntry3));

            HtmlForm configForm = wc.goTo("configure").getFormByName("config");

            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);

            j.submit(configForm);

            assertEquals(
                    Arrays.asList(validEntry1, validEntry2, validEntry3),
                    descriptor.getChoiceListEntryList(),
                    "Simple submission: descriptor after submission");

            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);

            GlobalTextareaChoiceListProvider.DescriptorImpl newDescriptor =
                    new GlobalTextareaChoiceListProvider.DescriptorImpl();

            assertEquals(
                    Arrays.asList(validEntry1, validEntry2, validEntry3),
                    newDescriptor.getChoiceListEntryList(),
                    "Simple submission: descriptor serialized from config.xml");
        }

        // Submission with invalid entry.
        {
            // update the descriptor to the state I want to submit.
            descriptor.setChoiceListEntryList(Arrays.asList(invalidEntry1, validEntry2, validEntry3));

            HtmlForm configForm = wc.goTo("configure").getFormByName("config");

            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);

            j.submit(configForm);

            assertEquals(
                    Arrays.asList(validEntry2, validEntry3),
                    descriptor.getChoiceListEntryList(),
                    "Submission with invalid entry: descriptor after submission");

            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);

            GlobalTextareaChoiceListProvider.DescriptorImpl newDescriptor =
                    new GlobalTextareaChoiceListProvider.DescriptorImpl();

            assertEquals(
                    Arrays.asList(validEntry2, validEntry3),
                    newDescriptor.getChoiceListEntryList(),
                    "Submission with invalid entry: descriptor serialized from config.xml");
        }

        // Empty submission
        {
            // update the descriptor to the state I want to submit.
            descriptor.setChoiceListEntryList(null);

            HtmlForm configForm = wc.goTo("configure").getFormByName("config");

            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(Arrays.asList(validEntry1, validEntry2));

            j.submit(configForm);

            assertEquals(
                    new ArrayList<GlobalTextareaChoiceListEntry>(0),
                    descriptor.getChoiceListEntryList(),
                    "Empty submission: descriptor after submission");

            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(Arrays.asList(validEntry1, validEntry2));

            GlobalTextareaChoiceListProvider.DescriptorImpl newDescriptor =
                    new GlobalTextareaChoiceListProvider.DescriptorImpl();

            assertEquals(
                    new ArrayList<GlobalTextareaChoiceListEntry>(0),
                    newDescriptor.getChoiceListEntryList(),
                    "Empty submission: descriptor serialized from config.xml");
        }
    }

    public static class CaptureChoiceListBuilder extends Builder {
        private final String entryName;
        private List<String> choiceList;

        public CaptureChoiceListBuilder(String entryName) {
            this.entryName = entryName;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            GlobalTextareaChoiceListProvider.DescriptorImpl descriptor =
                    GlobalTextareaChoiceListProviderJenkinsTest.getDescriptor();
            GlobalTextareaChoiceListEntry entry = descriptor.getChoiceListEntry(entryName);
            choiceList = new ArrayList<>(entry.getChoiceList());

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

    private boolean _testEditedValueWillBeContained(
            String jobname, Result result, String entryname, String defname, String value) throws Exception {
        // result == null stands for triggered.

        List<String> choiceList = null;

        FreeStyleProject job = (FreeStyleProject) Jenkins.get().getItem(jobname);

        job.getBuildersList().clear();

        // Used for capture choiceList after triggered.
        CaptureChoiceListBuilder ccb = new CaptureChoiceListBuilder(entryname);
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

            GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
            GlobalTextareaChoiceListEntry entry = descriptor.getChoiceListEntry(entryname);
            choiceList = new ArrayList<>(entry.getChoiceList());
        }

        return choiceList.contains(value);
    }

    @Test
    void testAddEditedValue_Disabled1() throws Exception {
        String varname = "test";
        String entryname = "test";

        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(List.of(entry));
        descriptor.save();

        GlobalTextareaChoiceListProvider provider = new GlobalTextareaChoiceListProvider(entryname, null, false, null);
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
                    _testEditedValueWillBeContained(jobname, null, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.SUCCESS, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.UNSTABLE, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Failure
        {
            String value = "Failure";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.FAILURE, entryname, varname, value),
                    "Edited value must not be contained");
        }
    }

    @Test
    void testAddEditedValue_Disabled2() throws Exception {
        String varname = "test";
        String entryname = "test";

        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", false);
        descriptor.setChoiceListEntryList(List.of(entry));
        descriptor.save();

        GlobalTextareaChoiceListProvider provider =
                new GlobalTextareaChoiceListProvider(entryname, null, true, WhenToAdd.Triggered);
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
                    _testEditedValueWillBeContained(jobname, null, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.SUCCESS, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.UNSTABLE, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Failure
        {
            String value = "Failure";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.FAILURE, entryname, varname, value),
                    "Edited value must not be contained");
        }
    }

    @Test
    void testAddEditedValue_Disabled3() throws Exception {
        String varname = "test";
        String entryname = "test";

        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(List.of(entry));
        descriptor.save();

        GlobalTextareaChoiceListProvider provider = new GlobalTextareaChoiceListProvider(entryname, null, true, null);
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
                    _testEditedValueWillBeContained(jobname, null, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.SUCCESS, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.UNSTABLE, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Failure
        {
            String value = "Failure";
            assertFalse(
                    _testEditedValueWillBeContained(jobname, Result.FAILURE, entryname, varname, value),
                    "Edited value must not be contained");
        }
    }

    @Test
    void testAddEditedValue_Trigger() throws Exception {
        String varname = "test";
        String entryname = "test";

        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(List.of(entry));
        descriptor.save();

        GlobalTextareaChoiceListProvider provider =
                new GlobalTextareaChoiceListProvider(entryname, null, true, WhenToAdd.Triggered);
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
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }

        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }

        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }
    }

    @Test
    void testAddEditedValue_Completed() throws Exception {
        String varname = "test";
        String entryname = "test";

        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(List.of(entry));
        descriptor.save();

        GlobalTextareaChoiceListProvider provider =
                new GlobalTextareaChoiceListProvider(entryname, null, true, WhenToAdd.Completed);
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
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }

        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }
    }

    @Test
    void testAddEditedValue_CompletedStable() throws Exception {
        String varname = "test";
        String entryname = "test";

        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(List.of(entry));
        descriptor.save();

        GlobalTextareaChoiceListProvider provider =
                new GlobalTextareaChoiceListProvider(entryname, null, true, WhenToAdd.CompletedStable);
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
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertFalse(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertFalse(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must not be contained");
        }
    }

    @Test
    void testAddEditedValue_CompletedUnstable() throws Exception {
        String varname = "test";
        String entryname = "test";

        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(entryname, "value1\nvalue2", true);
        descriptor.setChoiceListEntryList(List.of(entry));
        descriptor.save();

        GlobalTextareaChoiceListProvider provider =
                new GlobalTextareaChoiceListProvider(entryname, null, true, WhenToAdd.CompletedUnstable);
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
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must not be contained");
        }

        // Success
        {
            String value = "Success";
            Result result = Result.SUCCESS;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }

        // Unstable
        {
            String value = "Unstable";
            Result result = Result.UNSTABLE;
            assertTrue(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must be contained");
        }

        // Failure
        {
            String value = "Failure";
            Result result = Result.FAILURE;
            assertFalse(
                    _testEditedValueWillBeContained(jobname, result, entryname, varname, value),
                    "Edited value must not be contained");
        }
    }

    @Test
    void testConfiguration1() throws Exception {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        List<GlobalTextareaChoiceListEntry> entryList =
                List.of(new GlobalTextareaChoiceListEntry("testChoice", "a\nb\nc", false));
        descriptor.setChoiceListEntryList(entryList);

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "testVar",
                new GlobalTextareaChoiceListProvider("testChoice", null, false, WhenToAdd.Completed),
                false,
                "description");
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(j.jenkins);

        // actually, this doesn't make sense as
        // this doesn't fail even if error occurs when saving global configuration.
        j.assertEqualDataBoundBeans(entryList, descriptor.getChoiceListEntryList());

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("testVar"));
    }

    @Test
    void testGlobalConfiguration2() throws Exception {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        List<GlobalTextareaChoiceListEntry> entryList =
                List.of(new GlobalTextareaChoiceListEntry("testChoice", "a\nb\nc\n", true));
        descriptor.setChoiceListEntryList(entryList);

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "testVar",
                new GlobalTextareaChoiceListProvider("testChoice", "b", true, WhenToAdd.Triggered),
                true,
                "description");
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(j.jenkins);

        // actually, this doesn't make sense as
        // this doesn't fail even if error occurs when saving global configuration.
        j.assertEqualDataBoundBeans(entryList, descriptor.getChoiceListEntryList());

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("testVar"));
    }
}
