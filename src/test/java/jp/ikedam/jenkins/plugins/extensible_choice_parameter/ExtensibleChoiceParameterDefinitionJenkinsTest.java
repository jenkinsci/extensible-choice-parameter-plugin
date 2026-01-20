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

import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.ExtensibleChoiceParameterDefinition.EditableType;
import net.sf.json.JSONObject;
import org.htmlunit.ElementNotFoundException;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlOption;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.htmlunit.html.HtmlTextInput;
import org.htmlunit.xml.XmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.WithTimeout;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Tests for ExtensibleChoiceParameterDefinition, corresponding to Jenkins.
 */
@WithJenkins
class ExtensibleChoiceParameterDefinitionJenkinsTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    private ExtensibleChoiceParameterDefinition.DescriptorImpl getDescriptor() {
        return (ExtensibleChoiceParameterDefinition.DescriptorImpl)
                (new ExtensibleChoiceParameterDefinition("name", null, false, "")).getDescriptor();
    }

    /**
     * Note:
     * This behavior depends on Jenkins core.
     * So when changing target Jenkins version, the behavior may change.
     */
    @Test
    void testDescriptorDoCheckNameOk() {
        ExtensibleChoiceParameterDefinition.DescriptorImpl descriptor = getDescriptor();

        // OK: lower alphabets
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckName("abc").kind);

        // OK: upper alphabets
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckName("ABC").kind);

        // OK: alphabets and numbers
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckName("abc123").kind);

        // OK: only numbers (amazing!)
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckName("123").kind);

        // OK: alphabets, numbers, and underscores.
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckName("abc_1_2_3").kind);

        // OK: starts with underscore.
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckName("_abc_1_2_3").kind);

        // OK: blank in the beginning
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckName("  _abc_1_2_3").kind);

        // OK: blank in the end
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckName("  _abc_1_2_3   ").kind);

        // OK: value contains dots (accepted since Jenkins 1.526)
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckName("a.b").kind);
    }

    /**
     * Note:
     * This behavior depends on Jenkins core.
     * So when changing target Jenkins version, the behavior may change.
     */
    @Test
    void testDescriptorDoCheckNameError() {
        ExtensibleChoiceParameterDefinition.DescriptorImpl descriptor = getDescriptor();

        // ERROR: null
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckName(null).kind);

        // ERROR: empty
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckName("").kind);

        // ERROR: blank
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckName(" ").kind);

        // WARNING: value containing blank
        assertEquals(FormValidation.Kind.WARNING, descriptor.doCheckName("a b").kind);

        // WARNING: value contains a letter, not alphabet, number, nor underscore.
        assertEquals(FormValidation.Kind.WARNING, descriptor.doCheckName("a-b-c").kind);

        // WARNING: value starts with a letter, not alphabet, number, nor underscore.
        assertEquals(FormValidation.Kind.WARNING, descriptor.doCheckName("!ab").kind);

        // WARNING: value contains a multibyte letter.
        assertEquals(FormValidation.Kind.WARNING, descriptor.doCheckName("ａb").kind);
    }

    public static class MockChoiceListProvider extends ChoiceListProvider {
        @Serial
        private static final long serialVersionUID = -8216066980119568526L;

        private List<String> choiceList = null;
        private String defaultChoice = null;
        private boolean requiresBuildPermission = false;

        public MockChoiceListProvider(List<String> choiceList, String defaultChoice, boolean requiresBuildPermission) {
            this.choiceList = choiceList;
            this.defaultChoice = defaultChoice;
            this.requiresBuildPermission = requiresBuildPermission;
        }

        public MockChoiceListProvider(List<String> choiceList, String defaultChoice) {
            this(choiceList, defaultChoice, true);
        }

        @DataBoundConstructor
        public MockChoiceListProvider(String choiceListString, String defaultChoice) {
            this.choiceList = Arrays.asList(choiceListString.split(","));
            this.defaultChoice = Util.fixEmpty(defaultChoice);
        }

        @Override
        public List<String> getChoiceList() {
            return choiceList;
        }

        public String getChoiceListString() {
            return String.join(",", getChoiceList());
        }

        @Override
        public boolean requiresBuildPermission() {
            return requiresBuildPermission;
        }

        @Override
        public String getDefaultChoice() {
            return defaultChoice;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<ChoiceListProvider> {
            @Override
            public String getDisplayName() {
                return "MockChoiceListProvider";
            }
        }
    }

    public static class EnableConfigurableMockChoiceListProvider extends ChoiceListProvider {
        @Serial
        private static final long serialVersionUID = 7643544327776225136L;

        private List<String> choiceList = null;
        private String defaultChoice = null;

        public EnableConfigurableMockChoiceListProvider(List<String> choiceList, String defaultChoice) {
            this.choiceList = choiceList;
            this.defaultChoice = defaultChoice;
        }

        @DataBoundConstructor
        public EnableConfigurableMockChoiceListProvider(String choiceListString, String defaultChoice) {
            this.choiceList = Arrays.asList(choiceListString.split(","));
            this.defaultChoice = Util.fixEmpty(defaultChoice);
        }

        @Override
        public List<String> getChoiceList() {
            return choiceList;
        }

        @Override
        public String getDefaultChoice() {
            return defaultChoice;
        }

        public String getChoiceListString() {
            return String.join(",", getChoiceList());
        }

        @Extension
        public static class DescriptorImpl extends ChoiceListProviderDescriptor {
            private boolean enabledByDefault = true;

            protected void setEnabledByDefault(boolean enabledByDefault) {
                this.enabledByDefault = enabledByDefault;
            }

            @Override
            public boolean isEnabledByDefault() {
                return enabledByDefault;
            }

            @Override
            public String getDisplayName() {
                return "EnableConfigurableMockChoiceListProvider";
            }

            @Override
            public boolean configure(StaplerRequest2 req, JSONObject json)
                    throws hudson.model.Descriptor.FormException {
                setEnabledByDefault(json.getBoolean("enabledByDefault"));
                return super.configure(req, json);
            }
        }
    }

    /**
     * Calls {@link #runBuildWithSelectParameter(ParameterDefinition, String, boolean)} with throwExceptions = false
     * @param def
     * @param value
     * @return
     * @throws Exception
     */
    private EnvVars runBuildWithSelectParameter(ParameterDefinition def, String value) throws Exception {
        return runBuildWithSelectParameter(def, value, false);
    }

    /**
     * @param def
     * @param value
     * @param throwExceptions sets WebClient#withThrowExceptionOnFailingStatusCode when posting the form
     * @return
     * @throws Exception
     */
    private EnvVars runBuildWithSelectParameter(ParameterDefinition def, String value, boolean throwExceptions)
            throws Exception {
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        job.getBuildersList().add(ceb);

        WebClient wc = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);

        HtmlPage page = wc.getPage(job, "build?delay=0sec");

        HtmlForm form;

        try {
            form = page.getFormByName("parameters");
        } catch (ElementNotFoundException e) {
            Logger logger = Logger.getLogger(this.getClass().getName());
            logger.log(Level.SEVERE, "Failed to retrieve parameter form", e);
            logger.severe(page.asXml());
            throw e;
        }

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

        wc.setThrowExceptionOnFailingStatusCode(throwExceptions);
        j.submit(form);

        j.waitUntilNoActivity();

        job.delete();

        return ceb.getEnvVars();
    }

    /**
     * test for createValue(StaplerRequest2 request, JSONObject jo)
     * @throws Exception
     */
    @Test
    @WithTimeout(300)
    void testCreateValueFromView() throws Exception {
        String name = "PARAM1";
        String description = "Some Text";
        ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), null);

        // select with editable
        {
            String value = "value3";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description), value);
            assertEquals(value, envVars.get(name), "select with non-editable");
        }

        // select with non-editable
        {
            String value = "value2";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(name, provider, false, description), value);
            assertEquals(value, envVars.get(name), "select with non-editable");
        }

        // input with editable
        {
            String value = "valueX";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description), value);
            assertEquals(value, envVars.get(name), "input with editable");
        }

        // input with non-editable. causes exception.
        {
            String value = "valueX";
            FailingHttpStatusCodeException e = assertThrows(
                    FailingHttpStatusCodeException.class,
                    () -> runBuildWithSelectParameter(
                            new ExtensibleChoiceParameterDefinition(name, provider, false, description), value, true));
            assertEquals(500, e.getStatusCode(), "Failed with unexpected status code");
        }

        // not trimmed.
        {
            String value = " a b c d  ";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description), value);
            assertEquals(value, envVars.get(name), "not trimmed");
        }

        // no choice is provided and editable. any values can be accepted.
        {
            String value = "abc";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name, new MockChoiceListProvider(new ArrayList<>(0), null), true, description),
                    value);
            assertEquals(value, envVars.get(name), "provider is null and editable");
        }

        // no choice is provided and non-editable. always throw exception.
        {
            String value = "";
            FailingHttpStatusCodeException e = assertThrows(
                    FailingHttpStatusCodeException.class,
                    () -> runBuildWithSelectParameter(
                            new ExtensibleChoiceParameterDefinition(
                                    name, new MockChoiceListProvider(new ArrayList<>(0), null), false, description),
                            value,
                            true));
            assertEquals(500, e.getStatusCode(), "Failed with unexpected status code");
        }
    }

    /**
     * test for createValue(StaplerRequest2 request, JSONObject jo)
     *
     * Test patterns with invalid choice providers.
     * It seems that too many requests in a test function results in
     * java.net.SocketTimeoutException: Read timed out (Why?),
     * so put these patterns from  testCreateValueFromView apart.
     *
     * @throws Exception
     */
    @Test
    void testCreateValueFromViewWithInvalidProvider() throws Exception {
        String name = "PARAM1";
        String description = "Some Text";

        // provider is null and editable. any values can be accepted.
        {
            String value = "";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(name, null, true, description), value);
            assertEquals(value, envVars.get(name), "provider is null and editable");
        }

        // provider is null and non-editable. always throw exception.
        {
            String value = "";
            FailingHttpStatusCodeException e = assertThrows(
                    FailingHttpStatusCodeException.class,
                    () -> runBuildWithSelectParameter(
                            new ExtensibleChoiceParameterDefinition(name, null, false, description), value, true));
            assertEquals(500, e.getStatusCode(), "Failed with unexpected status code");
        }

        // provider returns null and editable. any values can be accepted.
        {
            String value = "abc";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name, new MockChoiceListProvider((List<String>) null, null), true, description),
                    value);
            assertEquals(value, envVars.get(name), "provider returns null and editable");
        }

        // provider returns null non-editable. always throw exception.
        {
            String value = "";
            FailingHttpStatusCodeException e = assertThrows(
                    FailingHttpStatusCodeException.class,
                    () -> runBuildWithSelectParameter(
                            new ExtensibleChoiceParameterDefinition(
                                    name, new MockChoiceListProvider((List<String>) null, null), false, description),
                            value,
                            true));
            assertEquals(500, e.getStatusCode(), "Failed with unexpected status code");
        }
    }

    @Test
    void testGetDefaultParameterValue() throws Exception {
        // editable, in choice
        {
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), "value2");
            ExtensibleChoiceParameterDefinition def =
                    new ExtensibleChoiceParameterDefinition("test", provider, true, "description");
            FreeStyleProject job = j.createFreeStyleProject();
            job.addProperty(new ParametersDefinitionProperty(def));
            CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
            job.getBuildersList().add(ceb);
            job.save();

            j.assertBuildStatusSuccess(job.scheduleBuild2(0));
            assertEquals("value2", ceb.getEnvVars().get("test"), "editable, in choice");
        }

        // non-editable, in choice
        {
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), "value2");
            ExtensibleChoiceParameterDefinition def =
                    new ExtensibleChoiceParameterDefinition("test", provider, false, "description");
            FreeStyleProject job = j.createFreeStyleProject();
            job.addProperty(new ParametersDefinitionProperty(def));
            CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
            job.getBuildersList().add(ceb);
            job.save();

            j.assertBuildStatusSuccess(job.scheduleBuild2(0));
            assertEquals("value2", ceb.getEnvVars().get("test"), "non-editable, in choice");
        }

        // editable, not in choice
        {
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), "value4");
            ExtensibleChoiceParameterDefinition def =
                    new ExtensibleChoiceParameterDefinition("test", provider, true, "description");
            FreeStyleProject job = j.createFreeStyleProject();
            job.addProperty(new ParametersDefinitionProperty(def));
            CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
            job.getBuildersList().add(ceb);
            job.save();

            j.assertBuildStatusSuccess(job.scheduleBuild2(0));
            assertEquals("value4", ceb.getEnvVars().get("test"), "editable, not in choice");
        }

        // non-editable, not in choice
        // The value on the top should be used.
        {
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), "value4");
            ExtensibleChoiceParameterDefinition def =
                    new ExtensibleChoiceParameterDefinition("test", provider, false, "description");
            FreeStyleProject job = j.createFreeStyleProject();
            job.addProperty(new ParametersDefinitionProperty(def));
            CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
            job.getBuildersList().add(ceb);
            job.save();

            j.assertBuildStatusSuccess(job.scheduleBuild2(0));
            assertEquals("value1", ceb.getEnvVars().get("test"));
        }
    }

    // Test for createValue(String value)
    @Test
    void testCreateValueForCli() {
        String name = "name";
        String description = "Some text";
        ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), null);

        // select with editable
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description);
            String value = "value3";
            assertEquals(
                    new StringParameterValue(name, value, description),
                    target.createValue(value),
                    "select with editable");
        }

        // select with non-editable
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, false, description);
            String value = "value2";
            assertEquals(
                    new StringParameterValue(name, value, description),
                    target.createValue(value),
                    "select with non-editable");
        }

        // input with editable
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description);
            String value = "someValue";
            assertEquals(
                    new StringParameterValue(name, value, description),
                    target.createValue(value),
                    "input with editable");
        }

        // input with non-editable. causes exception.
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, false, description);
            String value = "someValue";
            assertThrows(IllegalArgumentException.class, () -> target.createValue(value));
        }

        // not trimmed.
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description);
            String value = "  a b\nc d e  ";
            assertEquals(new StringParameterValue(name, value, description), target.createValue(value), "not trimmed");
        }

        // provider is null and non-editable. always throw exception.
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, null, false, description);
            String value = "anyValue";
            assertThrows(IllegalArgumentException.class, () -> target.createValue(value));
        }

        // provider is null and editable. any values can be accepted.
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, null, true, description);
            String value = "anyValue";
            assertEquals(
                    new StringParameterValue(name, value, description),
                    target.createValue(value),
                    "provider is null and editable");
        }

        // no choice is provided and non-editable. always throw exception.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider(new ArrayList<>(0), null), false, description);
            String value = "anyValue";
            assertThrows(IllegalArgumentException.class, () -> target.createValue(value));
        }

        // no choice is provided and editable. any values can be accepted.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider(new ArrayList<>(0), null), true, description);
            String value = "anyValue";
            assertEquals(
                    new StringParameterValue(name, value, description),
                    target.createValue(value),
                    "no choice is provided and editable");
        }

        // provider returns null non-editable. always throw exception.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider((List<String>) null, null), false, description);
            String value = "anyValue";
            assertThrows(IllegalArgumentException.class, () -> target.createValue(value));
        }

        // provider returns null and editable. any values can be accepted.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider((List<String>) null, null), true, description);
            String value = "anyValue";
            assertEquals(
                    new StringParameterValue(name, value, description),
                    target.createValue(value),
                    "provider returns null and editable");
        }
    }

    @Test
    void testGetDefaultParameterValue_NoDefaultChoice() {
        String name = "name";
        String description = "Some text";
        ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), null);
        String firstValue = "value1";

        // Editable
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description);
            assertEquals(
                    new StringParameterValue(name, firstValue, description),
                    target.getDefaultParameterValue(),
                    "Editable");
        }

        // Non-editable
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, false, description);
            assertEquals(
                    new StringParameterValue(name, firstValue, description),
                    target.getDefaultParameterValue(),
                    "Editable");
        }

        // provider is null and non-editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, null, false, description);
            assertNull(target.getDefaultParameterValue(), "provider is null and non-editable");
        }

        // provider is null and editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, null, true, description);
            assertNull(target.getDefaultParameterValue(), "provider is null and editable");
        }

        // no choice is provided and non-editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider(new ArrayList<>(0), null), false, description);
            assertNull(target.getDefaultParameterValue(), "no choice is provided and non-editable");
        }

        // no choice is provided and editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider(new ArrayList<>(0), null), true, description);
            assertNull(target.getDefaultParameterValue(), "no choice is provided and editable");
        }

        // provider returns null non-editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider((List<String>) null, null), false, description);
            assertNull(target.getDefaultParameterValue(), "provider returns null and non-editable");
        }

        // provider returns null and editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider((List<String>) null, null), true, description);
            assertNull(target.getDefaultParameterValue(), "provider returns null and editable");
        }
    }

    @Test
    void testGetDefaultParameterValue_SpecifiedDefaultChoice() {
        String name = "name";
        String description = "Some text";

        // Editable, in choices
        {
            String defaultChoice = "value2";
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description);
            assertEquals(
                    new StringParameterValue(name, defaultChoice, description),
                    target.getDefaultParameterValue(),
                    "Editable, in choices");
        }

        // Non-editable, in choices
        {
            String defaultChoice = "value2";
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, false, description);
            assertEquals(
                    new StringParameterValue(name, defaultChoice, description),
                    target.getDefaultParameterValue(),
                    "Non-Editable, in choices");
        }

        // Editable, in choices, the first
        {
            String defaultChoice = "value1";
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description);
            assertEquals(
                    new StringParameterValue(name, defaultChoice, description),
                    target.getDefaultParameterValue(),
                    "Editable, in choices, the first");
        }

        // Non-editable, in choices, the first
        {
            String defaultChoice = "value1";
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, false, description);
            assertEquals(
                    new StringParameterValue(name, defaultChoice, description),
                    target.getDefaultParameterValue(),
                    "Non-Editable, in choices, the first");
        }

        // Editable, in choices, the last
        {
            String defaultChoice = "value3";
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description);
            assertEquals(
                    new StringParameterValue(name, defaultChoice, description),
                    target.getDefaultParameterValue(),
                    "Editable, in choices, the last");
        }

        // Non-editable, in choices, the last
        {
            String defaultChoice = "value3";
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, false, description);
            assertEquals(
                    new StringParameterValue(name, defaultChoice, description),
                    target.getDefaultParameterValue(),
                    "Non-Editable, in choices, the last");
        }

        // Editable, not in choices
        {
            String defaultChoice = "value4";
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, true, description);
            assertEquals(
                    new StringParameterValue(name, defaultChoice, description),
                    target.getDefaultParameterValue(),
                    "Editable, in choices");
        }

        // Non-editable, not in choices
        // The value on the top should be used.
        {
            String defaultChoice = "value4";
            ChoiceListProvider provider =
                    new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition(name, provider, false, description);
            assertEquals(new StringParameterValue(name, "value1", description), target.getDefaultParameterValue());
        }

        // no choice is provided and non-editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider(new ArrayList<>(0), null), false, description);
            assertNull(target.getDefaultParameterValue(), "no choice is provided and non-editable");
        }

        // no choice is provided and editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name, new MockChoiceListProvider(new ArrayList<>(0), null), true, description);
            assertNull(target.getDefaultParameterValue(), "no choice is provided and editable");
        }
    }

    @Test
    void testDisableChoiceListConfiguration() throws Exception {
        ExtensibleChoiceParameterDefinition.DescriptorImpl d = getDescriptor();

        MockChoiceListProvider.DescriptorImpl providerDesc1 =
                (MockChoiceListProvider.DescriptorImpl) j.jenkins.getDescriptor(MockChoiceListProvider.class);
        EnableConfigurableMockChoiceListProvider.DescriptorImpl providerDesc2 =
                (EnableConfigurableMockChoiceListProvider.DescriptorImpl)
                        j.jenkins.getDescriptor(EnableConfigurableMockChoiceListProvider.class);

        // not configured
        assertEquals(Collections.emptyMap(), d.getChoiceListEnabledMap());
        assertTrue(d.isProviderEnabled(providerDesc1));
        providerDesc2.setEnabledByDefault(true);
        assertTrue(d.isProviderEnabled(providerDesc2));
        providerDesc2.setEnabledByDefault(false);
        assertFalse(d.isProviderEnabled(providerDesc2));

        // configuration from GUI
        assertFalse(d.isProviderEnabled(providerDesc2));
        j.configRoundtrip();
        assertTrue(d.getChoiceListEnabledMap().get(providerDesc1.getId()));
        assertFalse(d.getChoiceListEnabledMap().get(providerDesc2.getId()));

        assertTrue(d.isProviderEnabled(providerDesc1));
        providerDesc2.setEnabledByDefault(true);
        assertFalse(d.isProviderEnabled(providerDesc2));
        providerDesc2.setEnabledByDefault(false);
        assertFalse(d.isProviderEnabled(providerDesc2));

        // revert enables
        {
            Map<String, Boolean> enableMap = new HashMap<>();
            enableMap.put(providerDesc1.getId(), false);
            enableMap.put(providerDesc2.getId(), true);
            d.setChoiceListEnabledMap(enableMap);
        }
        assertFalse(d.isProviderEnabled(providerDesc1));
        providerDesc2.setEnabledByDefault(true);
        assertTrue(d.isProviderEnabled(providerDesc2));
        providerDesc2.setEnabledByDefault(false);
        assertTrue(d.isProviderEnabled(providerDesc2));

        // enable configuration is preserved via system configuration
        j.configRoundtrip();
        assertFalse(d.getChoiceListEnabledMap().get(providerDesc1.getId()));
        assertTrue(d.getChoiceListEnabledMap().get(providerDesc2.getId()));

        // global configuration is preserved
        providerDesc2.setEnabledByDefault(true);
        j.configRoundtrip();
        assertTrue(providerDesc2.isEnabledByDefault());
        providerDesc2.setEnabledByDefault(false);
        j.configRoundtrip();
        assertFalse(providerDesc2.isEnabledByDefault());
    }

    @Test
    void testDisableChoiceListRuntime() {
        ExtensibleChoiceParameterDefinition d = new ExtensibleChoiceParameterDefinition(
                "Choice", new MockChoiceListProvider(Arrays.asList("value1", "value2"), null), false, "");

        assertEquals(Arrays.asList("value1", "value2"), d.getChoiceList());
        assertEquals("value1", ((StringParameterValue) d.getDefaultParameterValue()).value);

        // disable
        {
            Map<String, Boolean> enableMap = new HashMap<>();
            enableMap.put(d.getChoiceListProvider().getDescriptor().getId(), false);
            getDescriptor().setChoiceListEnabledMap(enableMap);
        }
        assertEquals(Collections.emptyList(), d.getChoiceList());
        assertNull(d.getDefaultParameterValue());
    }

    @Test
    void testDisableChoiceListIntegration() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "Choice", new MockChoiceListProvider(Arrays.asList("value1", "value2"), null), false, "");
        p.addProperty(new ParametersDefinitionProperty(def));

        {
            FreeStyleBuild b = p.scheduleBuild2(0).get();
            j.assertBuildStatusSuccess(b);
            assertEquals("value1", b.getEnvironment(TaskListener.NULL).get("Choice"));
        }

        j.configRoundtrip(p);
        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("Choice"));

        // disable
        {
            Map<String, Boolean> enableMap = new HashMap<>();
            enableMap.put(def.getChoiceListProvider().getDescriptor().getId(), false);
            getDescriptor().setChoiceListEnabledMap(enableMap);
        }

        {
            FreeStyleBuild b = p.scheduleBuild2(0).get();
            j.assertBuildStatusSuccess(b);
            assertNull(b.getEnvironment(TaskListener.NULL).get("Choice"));
        }

        j.configRoundtrip(p);
        assertNotSame(
                MockChoiceListProvider.class,
                ((ExtensibleChoiceParameterDefinition) p.getProperty(ParametersDefinitionProperty.class)
                                .getParameterDefinition("Choice"))
                        .getChoiceListProvider()
                        .getClass());
    }

    @Issue("JENKINS-42903")
    @Test
    void testSafeTitle() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "<span id=\"test-not-expected\">combinations</span>",
                new MockChoiceListProvider(Arrays.asList("value1", "value2"), null),
                false,
                "");
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.getPage(p, "build");

        assertNull(page.getElementById("test-not-expected"));
    }

    @Test
    void testConfiguration1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test", new MockChoiceListProvider(Arrays.asList("a", "b", "c"), null), false, "description");
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test"));
    }

    @Test
    void testConfiguration2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test", new MockChoiceListProvider(Arrays.asList("a", "b", "c"), "a"), true, "another description");
        def.setEditableType(EditableType.NoFilter);
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test"));
    }

    @Test
    void testConfiguration3() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test", new MockChoiceListProvider(Arrays.asList("a", "b", "c"), "a"), true, "yet another description");
        def.setEditableType(EditableType.Filter);
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test"));
    }

    private List<String> extractCombobox(HtmlElement combobox) {
        List<String> ret = new ArrayList<>();
        for (HtmlElement d : combobox.getElementsByTagName("div")) {
            ret.add(d.getTextContent());
        }
        return ret;
    }

    @Test
    void testComboboxNoFilter() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new MockChoiceListProvider(Arrays.asList("foo/bar/baz", "foo/bar/qux", "bar/baz/qux"), null),
                true,
                "description");
        def.setEditableType(EditableType.NoFilter);
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.getPage(p, "build?delay=0sec");
        HtmlTextInput in = page.getElementByName("value");
        assertEquals("foo/bar/baz", in.getValue());

        HtmlElement combobox = page.getFirstByXPath("//*[@class='comboBoxList']");

        in.setValue("foo/bar");
        in.focus(); // fire onfocus
        assertEquals(Arrays.asList("foo/bar/baz", "foo/bar/qux", "bar/baz/qux"), extractCombobox(combobox));
        in.blur();
    }

    @Test
    void testComboboxFilter() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new MockChoiceListProvider(Arrays.asList("foo/bar/baz", "foo/bar/qux", "bar/baz/qux"), null),
                true,
                "description");
        def.setEditableType(EditableType.Filter);
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.getPage(p, "build?delay=0sec");
        HtmlTextInput in = page.getElementByName("value");
        assertEquals("foo/bar/baz", in.getValue());

        HtmlElement combobox = page.getFirstByXPath("//*[@class='comboBoxList']");

        in.setValue("foo/bar");
        in.focus(); // fire onfocus
        assertEquals(Arrays.asList("foo/bar/baz", "foo/bar/qux"), extractCombobox(combobox));
        in.blur();

        in.setValue("bar/baz");
        in.focus(); // fire onfocus
        assertEquals(Arrays.asList("foo/bar/baz", "bar/baz/qux"), extractCombobox(combobox));
        in.blur();
    }

    private XmlPage getXmlPage(WebClient wc, String path) throws Exception {
        // WebClient#getPageXml expects content-type "application/xml"
        // and doesn't accept "text/xml".
        Page p = wc.goTo(path, null);
        if (p instanceof XmlPage) {
            return (XmlPage) p;
        }
        throw new AssertionError("Expected XML but instead the content type was "
                + p.getWebResponse().getContentType());
    }

    @Test
    void testRestApiWithPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Item.READ, Item.BUILD, Jenkins.READ)
                .everywhere()
                .to("user"));

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test", new MockChoiceListProvider(Arrays.asList("foo", "bar", "baz"), null), true, "description");
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient();
        wc.login("user");

        XmlPage page = getXmlPage(wc, p.getUrl() + "/api/xml");
        assertEquals(
                Arrays.asList("foo", "bar", "baz"),
                Lists.transform(
                        page.getByXPath("//freeStyleProject/property/parameterDefinition[name='test']/choice"),
                        e -> (e instanceof DomElement) ? ((DomElement) e).getTextContent() : null));
    }

    @Test
    void testRestApiForViewWithPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Item.READ, Item.BUILD, Jenkins.READ)
                .everywhere()
                .to("user"));

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test", new MockChoiceListProvider(Arrays.asList("foo", "bar", "baz"), null), true, "description");
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient();
        wc.login("user");

        final String onlyChoices = "?tree=jobs[property[parameterDefinitions[name,choices]]]";
        XmlPage page = getXmlPage(wc, "api/xml" + onlyChoices);
        assertEquals(
                // even with BUILD permission, empty is returned for views.
                Collections.emptyList(),
                page.getByXPath("//freeStyleProject/property/parameterDefinition[name='test']/choice"));
    }

    @Test
    void testRestApiWithoutRequireBuildPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        // No Item.BUILD permission!
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Item.READ, Jenkins.READ)
                .everywhere()
                .to("user"));

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new MockChoiceListProvider(
                        Arrays.asList("foo", "bar", "baz"), null, false // requireBuildPermission
                        ),
                true,
                "description");
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient();
        wc.login("user");

        XmlPage page = getXmlPage(wc, p.getUrl() + "/api/xml");
        assertEquals(
                Arrays.asList("foo", "bar", "baz"),
                Lists.transform(
                        page.getByXPath("//freeStyleProject/property/parameterDefinition[name='test']/choice"),
                        e -> (e instanceof DomElement) ? ((DomElement) e).getTextContent() : null));
    }

    @Test
    void testRestApiForViewWithoutRequireBuildPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Item.READ, Item.BUILD, Jenkins.READ)
                .everywhere()
                .to("user"));

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new MockChoiceListProvider(
                        Arrays.asList("foo", "bar", "baz"), null, false // requireBuildPermission
                        ),
                true,
                "description");
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient();
        wc.login("user");

        final String onlyChoices = "?tree=jobs[property[parameterDefinitions[name,choices]]]";
        XmlPage page = getXmlPage(wc, "api/xml" + onlyChoices);
        assertEquals(
                Arrays.asList("foo", "bar", "baz"),
                Lists.transform(
                        page.getByXPath("//hudson/job/property/parameterDefinition[name='test']/choice"),
                        e -> (e instanceof DomElement) ? ((DomElement) e).getTextContent() : null));
    }

    @Test
    void testRestApiWithoutPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        // No Item.BUILD permission!
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Item.READ, Jenkins.READ)
                .everywhere()
                .to("user"));

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test", new MockChoiceListProvider(Arrays.asList("foo", "bar", "baz"), null), true, "description");
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient();
        wc.login("user");

        XmlPage page = getXmlPage(wc, p.getUrl() + "/api/xml");
        assertEquals(
                Collections.emptyList(),
                page.getByXPath("//freeStyleProject/property/parameterDefinition[name='test']/choice"));
    }
}
