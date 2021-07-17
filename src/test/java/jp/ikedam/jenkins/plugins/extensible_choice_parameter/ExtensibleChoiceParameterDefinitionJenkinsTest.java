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

import hudson.model.AbstractProject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.markup.RawHtmlMarkupFormatter;
import hudson.model.FreeStyleBuild;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.ExtensibleChoiceParameterDefinition.EditableType;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.Issue;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.xml.XmlPage;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Tests for ExtensibleChoiceParameterDefinition, corresponding to Jenkins.
 */
public class ExtensibleChoiceParameterDefinitionJenkinsTest
{
    @Rule
    public ExtensibleChoiceParameterJenkinsRule j = new ExtensibleChoiceParameterJenkinsRule();
    
    private ExtensibleChoiceParameterDefinition.DescriptorImpl getDescriptor()
    {
        return (ExtensibleChoiceParameterDefinition.DescriptorImpl)(new ExtensibleChoiceParameterDefinition("name", null, false, "")).getDescriptor();
    }
    
    /**
     * Note:
     * This behavior depends on Jenkins core.
     * So when changing target Jenkins version, the behavior may change.
     */
    @Test
    public void testDescriptorDoCheckNameOk()
    {
        ExtensibleChoiceParameterDefinition.DescriptorImpl descriptor = getDescriptor();
        
        // OK: lower alphabets
        assertEquals(descriptor.doCheckName("abc").kind, FormValidation.Kind.OK);
        
        // OK: upper alphabets
        assertEquals(descriptor.doCheckName("ABC").kind, FormValidation.Kind.OK);
        
        // OK: alphabets and numbers
        assertEquals(descriptor.doCheckName("abc123").kind, FormValidation.Kind.OK);
        
        // OK: only numbers (amazing!)
        assertEquals(descriptor.doCheckName("123").kind, FormValidation.Kind.OK);
        
        // OK: alphabets, numbers, and underscores.
        assertEquals(descriptor.doCheckName("abc_1_2_3").kind, FormValidation.Kind.OK);
        
        // OK: starts with underscore.
        assertEquals(descriptor.doCheckName("_abc_1_2_3").kind, FormValidation.Kind.OK);
        
        // OK: blank in the beginning
        assertEquals(descriptor.doCheckName("  _abc_1_2_3").kind, FormValidation.Kind.OK);
        
        // OK: blank in the end
        assertEquals(descriptor.doCheckName("  _abc_1_2_3   ").kind, FormValidation.Kind.OK);
        
        // OK: value contains dots (accepted since Jenkins 1.526)
        assertEquals(descriptor.doCheckName("a.b").kind, FormValidation.Kind.OK);
    }
    
    /**
     * Note:
     * This behavior depends on Jenkins core.
     * So when changing target Jenkins version, the behavior may change.
     */
    @Test
    public void testDescriptorDoCheckNameError()
    {
        ExtensibleChoiceParameterDefinition.DescriptorImpl descriptor = getDescriptor();
        
        // ERROR: null
        assertEquals(descriptor.doCheckName(null).kind, FormValidation.Kind.ERROR);
        
        // ERROR: empty
        assertEquals(descriptor.doCheckName("").kind, FormValidation.Kind.ERROR);
        
        // ERROR: blank
        assertEquals(descriptor.doCheckName(" ").kind, FormValidation.Kind.ERROR);
        
        // WARNING: value containing blank
        assertEquals(descriptor.doCheckName("a b").kind, FormValidation.Kind.WARNING);
        
        // WARNING: value contains a letter, not alphabet, number, nor underscore.
        assertEquals(descriptor.doCheckName("a-b-c").kind, FormValidation.Kind.WARNING);
        
        // WARNING: value starts with a letter, not alphabet, number, nor underscore.
        assertEquals(descriptor.doCheckName("!ab").kind, FormValidation.Kind.WARNING);
        
        // WARNING: value contains a multibyte letter.
        assertEquals(descriptor.doCheckName("ａb").kind, FormValidation.Kind.WARNING);
    }
    
    public static class MockChoiceListProvider extends ChoiceListProvider
    {
        private static final long serialVersionUID = -8216066980119568526L;
        private List<String> choiceList = null;
        private String defaultChoice = null;
        public MockChoiceListProvider(List<String> choiceList, String defaultChoice){
            this.choiceList = choiceList;
            this.defaultChoice = defaultChoice;
        }
        @DataBoundConstructor
        public MockChoiceListProvider(String choiceListString, String defaultChoice){
            this.choiceList = Arrays.asList(StringUtils.split(choiceListString, ","));
            this.defaultChoice = Util.fixEmpty(defaultChoice);
        }
        @Override
        public List<String> getChoiceList()
        {
            return choiceList;
        }
        public String getChoiceListString()
        {
            return StringUtils.join(getChoiceList(),",");
        }
        
        @Override
        public String getDefaultChoice()
        {
            return defaultChoice;
        }
        
        @Extension
        public static class DescriptorImpl extends Descriptor<ChoiceListProvider>
        {
            @Override
            public String getDisplayName()
            {
                return "MockChoiceListProvider";
            }
            
        }
    }

    public static class MockTextAreaChoiceListProvider extends AddEditedChoiceListProvider
    {
        private static final long serialVersionUID = -8216066980119568526L;
        private List<String> choiceList = null;
        private String defaultChoice = null;
        public MockTextAreaChoiceListProvider(List<String> choiceList, String defaultChoice){
            super(false,WhenToAdd.Triggered);
            this.choiceList = choiceList;
            this.defaultChoice = defaultChoice;
        }
        @DataBoundConstructor
        public MockTextAreaChoiceListProvider(String choiceListString, String defaultChoice){
            super(false,WhenToAdd.Triggered);
            this.choiceList = Arrays.asList(StringUtils.split(choiceListString, ","));
            this.defaultChoice = Util.fixEmpty(defaultChoice);
        }
        @Override
        public List<String> getChoiceList()
        {
            return choiceList;
        }
        public String getChoiceListString()
        {
            return StringUtils.join(getChoiceList(),",");
        }

        @Override
        public String getDefaultChoice()
        {
            return defaultChoice;
        }

        @Override
        protected void addEditedValue(AbstractProject<?, ?> project,
            ExtensibleChoiceParameterDefinition def, String value) {
            choiceList.add(value);
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<ChoiceListProvider>
        {
            @Override
            public String getDisplayName()
            {
                return "MockChoiceListProvider";
            }

        }
    }
    
    public static class EnableConfigurableMockChoiceListProvider extends ChoiceListProvider
    {
        private static final long serialVersionUID = 7643544327776225136L;
        private List<String> choiceList = null;
        private String defaultChoice = null;
        public EnableConfigurableMockChoiceListProvider(List<String> choiceList, String defaultChoice){
            this.choiceList = choiceList;
            this.defaultChoice = defaultChoice;
        }
        @DataBoundConstructor
        public EnableConfigurableMockChoiceListProvider(String choiceListString, String defaultChoice){
            this.choiceList = Arrays.asList(StringUtils.split(choiceListString, ","));
            this.defaultChoice = Util.fixEmpty(defaultChoice);
        }
        @Override
        public List<String> getChoiceList()
        {
            return choiceList;
        }
        
        @Override
        public String getDefaultChoice()
        {
            return defaultChoice;
        }
        public String getChoiceListString()
        {
            return StringUtils.join(getChoiceList(),",");
        }
        
        @Extension
        public static class DescriptorImpl extends ChoiceListProviderDescriptor
        {
            private boolean enabledByDefault = true;
            
            protected void setEnabledByDefault(boolean enabledByDefault)
            {
                this.enabledByDefault = enabledByDefault;
            }
            
            @Override
            public boolean isEnabledByDefault()
            {
                return enabledByDefault;
            }
            
            @Override
            public String getDisplayName()
            {
                return "EnableConfigurableMockChoiceListProvider";
            }
            
            @Override
            public boolean configure(StaplerRequest req, JSONObject json)
                    throws hudson.model.Descriptor.FormException
            {
                setEnabledByDefault(json.getBoolean("enabledByDefault"));
                return super.configure(req, json);
            }
        }
    }
    
    /**
     * @param def
     * @param value
     * @return
     * @throws Exception 
     */
    /**
     * @param def
     * @param value
     * @return
     * @throws Exception
     */
    private EnvVars runBuildWithSelectParameter(ParameterDefinition def, String value) throws Exception
    {
        FreeStyleProject job = j.createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        job.getBuildersList().add(ceb);
        
        WebClient wc = j.createAllow405WebClient();
        
        HtmlPage page = wc.getPage(job, "build?delay=0sec");
        
        HtmlForm form = null;
        
        try
        {
            form = page.getFormByName("parameters");
        }
        catch(ElementNotFoundException e)
        {
            Logger logger = Logger.getLogger(this.getClass().getName());
            logger.log(Level.SEVERE, "Failed to retrieve parameter form", e);
            logger.severe(page.asXml());
            throw e;
        }
        
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
        
        j.waitUntilNoActivity();
        
        job.delete();
        
        return ceb.getEnvVars();
    }
    
    /**
     * test for createValue(StaplerRequest request, JSONObject jo)
     * @throws Exception 
     */
    @Test
    public void testCreateValueFromView() throws Exception
    {
        String name = "PARAM1";
        String description = "Some Text";
        ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), null);
        
        // select with editable
        {
            String value = "value3";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name,
                            provider,
                            true,
                            description
                    ),
                    value
            );
            assertEquals("select with non-editable", value, envVars.get(name)); 
        }
        
        // select with non-editable
        {
            String value = "value2";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name,
                            provider,
                            false,
                            description
                    ),
                    value
            );
            assertEquals("select with non-editable", value, envVars.get(name)); 
        }
        
        // input with editable
        {
            String value = "valueX";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name,
                            provider,
                            true,
                            description
                    ),
                    value
            );
            assertEquals("input with editable", value, envVars.get(name)); 
        }
        
        // input with non-editable. causes exception.
        {
            String value = "valueX";
            try{
                EnvVars envVars = runBuildWithSelectParameter(
                        new ExtensibleChoiceParameterDefinition(
                                name,
                                provider,
                                false,
                                description
                        ),
                        value
                );
                assertEquals("input with non-editable", value, envVars.get(name));
                assertTrue("This code would not be reached.", false);
            }
            catch(FailingHttpStatusCodeException e)
            {
                assertEquals("Failed with unexpected status code", 500, e.getStatusCode());
            }
        }
        
        // not trimmed.
        {
            String value = " a b c d  ";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name,
                            provider,
                            true,
                            description
                    ),
                    value
            );
            assertEquals("not trimmed", value, envVars.get(name)); 
        }
        
        // no choice is provided and editable. any values can be accepted.
        {
            String value = "abc";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name,
                            new MockChoiceListProvider(new ArrayList<String>(0), null),
                            true,
                            description
                    ),
                    value
            );
            assertEquals("provider is null and editable", value, envVars.get(name)); 
        }
        
        // no choice is provided and non-editable. always throw exception.
        {
            String value = "";
            try{
                EnvVars envVars = runBuildWithSelectParameter(
                        new ExtensibleChoiceParameterDefinition(
                                name,
                                new MockChoiceListProvider(new ArrayList<String>(0), null),
                                false,
                                description
                        ),
                        value
                );
                assertEquals("no choice is provided and non-editable", value, envVars.get(name));
                assertTrue("This code would not be reached.", false);
            }
            catch(FailingHttpStatusCodeException e)
            {
                assertEquals("Failed with unexpected status code", 500, e.getStatusCode());
            }
        }
    }
    
    
    /**
     * test for createValue(StaplerRequest request, JSONObject jo)
     * 
     * Test patterns with invalid choice providers.
     * It seems that too many requests in a test function results in
     * java.net.SocketTimeoutException: Read timed out (Why?),
     * so put these patterns from  testCreateValueFromView apart.
     * 
     * @throws Exception 
     */
    @Test
    public void testCreateValueFromViewWithInvalidProvider() throws Exception
    {
        String name = "PARAM1";
        String description = "Some Text";
        
        // provider is null and editable. any values can be accepted.
        {
            String value = "";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name,
                            null,
                            true,
                            description
                    ),
                    value
            );
            assertEquals("provider is null and editable", value, envVars.get(name)); 
        }
        
        // provider is null and non-editable. always throw exception.
        {
            String value = "";
            try{
                EnvVars envVars = runBuildWithSelectParameter(
                        new ExtensibleChoiceParameterDefinition(
                                name,
                                null,
                                false,
                                description
                        ),
                        value
                );
                assertEquals("provider is null and non-editable", value, envVars.get(name));
                assertTrue("This code would not be reached.", false);
            }
            catch(FailingHttpStatusCodeException e)
            {
                assertEquals("Failed with unexpected status code", 500, e.getStatusCode());
            }
        }
        
        
        // provider returns null and editable. any values can be accepted.
        {
            String value = "abc";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name,
                            new MockChoiceListProvider((List<String>)null, null),
                            true,
                            description
                    ),
                    value
            );
            assertEquals("provider returns null and editable", value, envVars.get(name)); 
        }
        
        // provider returns null non-editable. always throw exception.
        {
            String value = "";
            try{
                EnvVars envVars = runBuildWithSelectParameter(
                        new ExtensibleChoiceParameterDefinition(
                                name,
                                new MockChoiceListProvider((List<String>)null, null),
                                false,
                                description
                        ),
                        value
                );
                assertEquals("provider returns null non-editable", value, envVars.get(name));
                assertTrue("This code would not be reached.", false);
            }
            catch(FailingHttpStatusCodeException e)
            {
                assertEquals("Failed with unexpected status code", 500, e.getStatusCode());
            }
        }
    }
    
    @Test
    public void testGetDefaultParameterValue() throws Exception
    {
        // editable, in choice
        {
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), "value2");
            ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                    "test",
                    provider,
                    true,
                    "description"
                    );
            FreeStyleProject job = j.createFreeStyleProject();
            job.addProperty(new ParametersDefinitionProperty(def));
            CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
            job.getBuildersList().add(ceb);
            job.save();
            
            j.assertBuildStatusSuccess(job.scheduleBuild2(0));
            assertEquals("editable, in choice", "value2", ceb.getEnvVars().get("test"));
        }
        
        
        // non-editable, in choice
        {
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), "value2");
            ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                    "test",
                    provider,
                    false,
                    "description"
                    );
            FreeStyleProject job = j.createFreeStyleProject();
            job.addProperty(new ParametersDefinitionProperty(def));
            CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
            job.getBuildersList().add(ceb);
            job.save();
            
            j.assertBuildStatusSuccess(job.scheduleBuild2(0));
            assertEquals("non-editable, in choice", "value2", ceb.getEnvVars().get("test"));
        }
        
        // editable, not in choice
        {
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), "value4");
            ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                    "test",
                    provider,
                    true,
                    "description"
                    );
            FreeStyleProject job = j.createFreeStyleProject();
            job.addProperty(new ParametersDefinitionProperty(def));
            CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
            job.getBuildersList().add(ceb);
            job.save();
            
            j.assertBuildStatusSuccess(job.scheduleBuild2(0));
            assertEquals("editable, not in choice", "value4", ceb.getEnvVars().get("test"));
        }
        
        // non-editable, not in choice
        // The value on the top should be used.
        {
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), "value4");
            ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                    "test",
                    provider,
                    false,
                    "description"
                    );
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
    public void testCreateValueForCli()
    {
        String name = "name";
        String description = "Some text";
        ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), null);
        
        // select with editable
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    true,
                    description
            );
            String value = "value3";
            assertEquals("select with editable", new StringParameterValue(name, value, description), target.createValue(value));
        }
        
        // select with non-editable
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    false,
                    description
            );
            String value = "value2";
            assertEquals("select with non-editable", new StringParameterValue(name, value, description), target.createValue(value));
        }
        
        // input with editable
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    true,
                    description
            );
            String value = "someValue";
            assertEquals("input with editable", new StringParameterValue(name, value, description), target.createValue(value));
        }
        
        // input with non-editable. causes exception.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    false,
                    description
            );
            String value = "someValue";
            try{
                assertEquals("input with non-editable", new StringParameterValue(name, value, description), target.createValue(value));
                assertTrue("input with non-editable: Code would not be reached, for an exception was triggered.", false);
            }catch(IllegalArgumentException e){
                assertTrue(true);
            }
        }
        
        // not trimmed.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    true,
                    description
            );
            String value = "  a b\nc d e  ";
            assertEquals("not trimmed", new StringParameterValue(name, value, description), target.createValue(value));
        }
        
        // provider is null and non-editable. always throw exception.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    null,
                    false,
                    description
            );
            String value = "anyValue";
            try{
                assertEquals("provider is null and non-editable", new StringParameterValue(name, value, description), target.createValue(value));
                assertTrue("input with non-editable: Code would not be reached, for an exception was triggered.", false);
            }catch(IllegalArgumentException e){
                assertTrue(true);
            }
        }
        
        // provider is null and editable. any values can be accepted.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    null,
                    true,
                    description
            );
            String value = "anyValue";
            assertEquals("provider is null and editable", new StringParameterValue(name, value, description), target.createValue(value));
        }
        
        // no choice is provided and non-editable. always throw exception.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider(new ArrayList<String>(0), null),
                    false,
                    description
            );
            String value = "anyValue";
            try{
                assertEquals("no choice is provided and non-editable", new StringParameterValue(name, value, description), target.createValue(value));
                assertTrue("input with non-editable: Code would not be reached, for an exception was triggered.", false);
            }catch(IllegalArgumentException e){
                assertTrue(true);
            }
        }
        
        // no choice is provided and editable. any values can be accepted.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider(new ArrayList<String>(0), null),
                    true,
                    description
            );
            String value = "anyValue";
            assertEquals("no choice is provided and editable", new StringParameterValue(name, value, description), target.createValue(value));
        }
        
        // provider returns null non-editable. always throw exception.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider((List<String>)null, null),
                    false,
                    description
            );
            String value = "anyValue";
            try{
                assertEquals("provider returns null and non-editable", new StringParameterValue(name, value, description), target.createValue(value));
                assertTrue("input with non-editable: Code would not be reached, for an exception was triggered.", false);
            }catch(IllegalArgumentException e){
                assertTrue(true);
            }
        }
        
        // provider returns null and editable. any values can be accepted.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider((List<String>)null, null),
                    true,
                    description
            );
            String value = "anyValue";
            assertEquals("provider returns null and editable", new StringParameterValue(name, value, description), target.createValue(value));
        }
    }
    
    @Test
    public void testGetDefaultParameterValue_NoDefaultChoice()
    {
        String name = "name";
        String description = "Some text";
        ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), null);
        String firstValue = "value1";
        
        // Editable
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    true,
                    description
            );
            assertEquals("Editable", new StringParameterValue(name, firstValue, description), target.getDefaultParameterValue());
        }
        
        // Non-editable
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    false,
                    description
            );
            assertEquals("Editable", new StringParameterValue(name, firstValue, description), target.getDefaultParameterValue());
        }
        
        // provider is null and non-editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    null,
                    false,
                    description
            );
            assertEquals("provider is null and non-editable", null, target.getDefaultParameterValue());
        }
        
        // provider is null and editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    null,
                    true,
                    description
            );
            assertEquals("provider is null and editable", null, target.getDefaultParameterValue());
        }
        
        // no choice is provided and non-editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider(new ArrayList<String>(0), null),
                    false,
                    description
            );
            assertEquals("no choice is provided and non-editable", null, target.getDefaultParameterValue());
        }
        
        // no choice is provided and editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider(new ArrayList<String>(0), null),
                    true,
                    description
            );
            assertEquals("no choice is provided and editable", null, target.getDefaultParameterValue());
        }
        
        // provider returns null non-editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider((List<String>)null, null),
                    false,
                    description
            );
            assertEquals("provider returns null and non-editable", null, target.getDefaultParameterValue());
        }
        
        // provider returns null and editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider((List<String>)null, null),
                    true,
                    description
            );
            assertEquals("provider returns null and editable", null, target.getDefaultParameterValue());
        }
    }
    
    @Test
    public void testGetDefaultParameterValue_SpecifiedDefaultChoice()
    {
        String name = "name";
        String description = "Some text";
        
        // Editable, in choices
        {
            String defaultChoice = "value2";
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    true,
                    description
            );
            assertEquals("Editable, in choices", new StringParameterValue(name, defaultChoice, description), target.getDefaultParameterValue());
        }
        
        // Non-editable, in choices
        {
            String defaultChoice = "value2";
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    false,
                    description
            );
            assertEquals("Non-Editable, in choices", new StringParameterValue(name, defaultChoice, description), target.getDefaultParameterValue());
        }
        
        // Editable, in choices, the first
        {
            String defaultChoice = "value1";
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    true,
                    description
            );
            assertEquals("Editable, in choices, the first", new StringParameterValue(name, defaultChoice, description), target.getDefaultParameterValue());
        }
        
        // Non-editable, in choices, the first
        {
            String defaultChoice = "value1";
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    false,
                    description
            );
            assertEquals("Non-Editable, in choices, the first", new StringParameterValue(name, defaultChoice, description), target.getDefaultParameterValue());
        }
        
        // Editable, in choices, the last
        {
            String defaultChoice = "value3";
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    true,
                    description
            );
            assertEquals("Editable, in choices, the last", new StringParameterValue(name, defaultChoice, description), target.getDefaultParameterValue());
        }
        
        // Non-editable, in choices, the last
        {
            String defaultChoice = "value3";
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    false,
                    description
            );
            assertEquals("Non-Editable, in choices, the last", new StringParameterValue(name, defaultChoice, description), target.getDefaultParameterValue());
        }
        
        // Editable, not in choices
        {
            String defaultChoice = "value4";
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    true,
                    description
            );
            assertEquals("Editable, in choices", new StringParameterValue(name, defaultChoice, description), target.getDefaultParameterValue());
        }
        
        // Non-editable, not in choices
        // The value on the top should be used.
        {
            String defaultChoice = "value4";
            ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"), defaultChoice);
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    provider,
                    false,
                    description
            );
            assertEquals(new StringParameterValue(name, "value1", description), target.getDefaultParameterValue());
        }
        
        // no choice is provided and non-editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider(new ArrayList<String>(0), null),
                    false,
                    description
            );
            assertEquals("no choice is provided and non-editable", null, target.getDefaultParameterValue());
        }
        
        // no choice is provided and editable. returns null.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    name,
                    new MockChoiceListProvider(new ArrayList<String>(0), null),
                    true,
                    description
            );
            assertEquals("no choice is provided and editable", null, target.getDefaultParameterValue());
        }
    }
    
    @Test
    public void testDisableChoiceListConfiguration() throws Exception
    {
        ExtensibleChoiceParameterDefinition.DescriptorImpl d = getDescriptor();
        
        MockChoiceListProvider.DescriptorImpl providerDesc1 = (MockChoiceListProvider.DescriptorImpl)j.jenkins.getDescriptor(MockChoiceListProvider.class);
        EnableConfigurableMockChoiceListProvider.DescriptorImpl providerDesc2 = (EnableConfigurableMockChoiceListProvider.DescriptorImpl)j.jenkins.getDescriptor(EnableConfigurableMockChoiceListProvider.class);
        
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
            Map<String,Boolean> enableMap = new HashMap<String,Boolean>();
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
    public void testDisableChoiceListRuntime() throws Exception
    {
        ExtensibleChoiceParameterDefinition d = new ExtensibleChoiceParameterDefinition(
                "Choice",
                new MockChoiceListProvider(Arrays.asList("value1", "value2"), null),
                false,
                ""
        );
        
        assertEquals(Arrays.asList("value1", "value2"), d.getChoiceList());
        assertEquals("value1", ((StringParameterValue)d.getDefaultParameterValue()).value);
        
        // disable
        {
            Map<String,Boolean> enableMap = new HashMap<String,Boolean>();
            enableMap.put(d.getChoiceListProvider().getDescriptor().getId(), false);
            getDescriptor().setChoiceListEnabledMap(enableMap);
        }
        assertEquals(Collections.emptyList(), d.getChoiceList());
        assertNull(d.getDefaultParameterValue());
    }
    
    @Test
    public void testDisableChoiceListIntegration() throws Exception
    {
        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "Choice",
                new MockChoiceListProvider(Arrays.asList("value1", "value2"), null),
                false,
                ""
        );
        p.addProperty(new ParametersDefinitionProperty(def));
        
        {
            FreeStyleBuild b = p.scheduleBuild2(0).get();
            j.assertBuildStatusSuccess(b);
            assertEquals("value1", b.getEnvironment(TaskListener.NULL).get("Choice"));
        }
        
        j.configRoundtrip(p);
        j.assertEqualDataBoundBeans(def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("Choice"));
        
        // disable
        {
            Map<String,Boolean> enableMap = new HashMap<String,Boolean>();
            enableMap.put(def.getChoiceListProvider().getDescriptor().getId(), false);
            getDescriptor().setChoiceListEnabledMap(enableMap);
        }
        
        {
            FreeStyleBuild b = p.scheduleBuild2(0).get();
            j.assertBuildStatusSuccess(b);
            assertNull(b.getEnvironment(TaskListener.NULL).get("Choice"));
        }
        
        j.configRoundtrip(p);
        assertNotSame(MockChoiceListProvider.class, ((ExtensibleChoiceParameterDefinition)p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("Choice")).getChoiceListProvider().getClass());
        
    }

    @Issue("JENKINS-42903")
    @Test
    public void testSafeTitle() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "<span id=\"test-not-expected\">combinations</span>",
                new MockChoiceListProvider(Arrays.asList("value1", "value2"), null),
                false,
                ""
        );
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createAllow405WebClient();
        HtmlPage page = wc.getPage(p, "build");

        assertNull(page.getElementById("test-not-expected"));
    }

    @Issue("JENKINS-42903")
    @Test
    public void testSafeDescription() throws Exception {
        j.jenkins.setMarkupFormatter(new RawHtmlMarkupFormatter(false));

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "Choice",
                new MockChoiceListProvider(Arrays.asList("value1", "value2"), null),
                false,
                "<span id=\"test-expected\">blahblah</span>"
                + "<script id=\"test-not-expected\"></script>"
        );
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createAllow405WebClient();
        HtmlPage page = wc.getPage(p, "build");

        assertNotNull(page.getElementById("test-expected"));
        assertNull(page.getElementById("test-not-expected"));
    }


    @Test
    public void testConfiguration1() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
            "test",
            new MockChoiceListProvider(
                Arrays.asList("a", "b", "c"),
                null
            ),
            false,
            "description"
        );
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
            def,
            p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test")
        );
    }

    @Test
    public void testConfiguration2() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
            "test",
            new MockChoiceListProvider(
                Arrays.asList("a", "b", "c"),
                "a"
            ),
            true,
            "another description"
        );
        def.setEditableType(EditableType.NoFilter);
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
            def,
            p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test")
        );
    }

    @Test
    public void testConfiguration3() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
            "test",
            new MockChoiceListProvider(
                Arrays.asList("a", "b", "c"),
                "a"
            ),
            true,
            "yet another description"
        );
        def.setEditableType(EditableType.Filter);
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
            def,
            p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test")
        );
    }

    private List<String> extractCombobox(HtmlElement combobox) throws Exception
    {
        List<String> ret = new ArrayList<String>();
        for (HtmlElement d: combobox.getElementsByTagName("div"))
        {
            ret.add(d.getTextContent());
        }
        return ret;
    }

    @Test
    public void testComboboxNoFilter() throws Exception
    {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
            "test",
            new MockChoiceListProvider(
                Arrays.asList("foo/bar/baz", "foo/bar/qux", "bar/baz/qux"),
                null
            ),
            true,
            "description"
        );
        def.setEditableType(EditableType.NoFilter);
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createAllow405WebClient();
        HtmlPage page = wc.getPage(p, "build?delay=0sec");
        HtmlTextInput in = page.getElementByName("value");
        assertEquals("foo/bar/baz", in.getValueAttribute());

        HtmlElement combobox = page.getFirstByXPath("//*[@class='comboBoxList']");

        in.setValueAttribute("foo/bar");
        in.focus(); // fire onfocus
        assertEquals(
            Arrays.asList(
                "foo/bar/baz",
                "foo/bar/qux",
                "bar/baz/qux"
            ),
            extractCombobox(combobox)
        );
        in.blur();
    }

    @Test
    public void testComboboxFilter() throws Exception
    {
        FreeStyleProject p = j.createFreeStyleProject();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
            "test",
            new MockChoiceListProvider(
                Arrays.asList("foo/bar/baz", "foo/bar/qux", "bar/baz/qux"),
                null
            ),
            true,
            "description"
        );
        def.setEditableType(EditableType.Filter);
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createAllow405WebClient();
        HtmlPage page = wc.getPage(p, "build?delay=0sec");
        HtmlTextInput in = page.getElementByName("value");
        assertEquals("foo/bar/baz", in.getValueAttribute());

        HtmlElement combobox = page.getFirstByXPath("//*[@class='comboBoxList']");

        in.setValueAttribute("foo/bar");
        in.focus(); // fire onfocus
        assertEquals(
            Arrays.asList(
                "foo/bar/baz",
                "foo/bar/qux"
            ),
            extractCombobox(combobox)
        );
        in.blur();

        in.setValueAttribute("bar/baz");
        in.focus(); // fire onfocus
        assertEquals(
            Arrays.asList(
                "foo/bar/baz",
                "bar/baz/qux"
            ),
            extractCombobox(combobox)
        );
        in.blur();
    }

    private XmlPage getXmlPage(WebClient wc, String path) throws Exception
    {
        // WebClient#getPageXml expects content-type "application/xml"
        // and doesn't accept "text/xml".
        Page p = wc.goTo(path, null);
        if (p instanceof XmlPage) {
            return (XmlPage)p;
        }
        throw new AssertionError(
            "Expected XML but instead the content type was "
                + p.getWebResponse().getContentType()
        );
    }

    @Test
    public void testRestApiWithPermission() throws Exception
    {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Item.READ, Item.BUILD, Jenkins.READ).everywhere().to("user")
        );

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
            "test",
            new MockChoiceListProvider(
                Arrays.asList("foo", "bar", "baz"),
                null
            ),
            true,
            "description"
        );
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient();
        wc.login("user");

        XmlPage page = getXmlPage(wc, p.getUrl() + "/api/xml");
        assertEquals(
            Arrays.asList("foo", "bar", "baz"),
            Lists.transform(
                page.getByXPath("//freeStyleProject/property/parameterDefinition[name='test']/choice"),
                new Function<Object, String>() {
                    public String apply(Object e) {
                        return (e instanceof DomElement) ? ((DomElement)e).getTextContent() : null;
                    }
                }
            )
        );
    }

    @Test
    public void testRestApiWithPermissionForView() throws Exception
    {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
            .grant(Item.READ, Item.BUILD, Jenkins.READ).everywhere().to("user")
        );

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
            "test",
            new MockChoiceListProvider(
                Arrays.asList("foo", "bar", "baz"),
                null
            ),
            true,
            "description"
        );
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient();
        wc.login("user");

        final String onlyChoices = "?tree=jobs[property[parameterDefinitions[name,choices]]]";
        XmlPage page = getXmlPage(wc, "api/xml" + onlyChoices);
        assertEquals(
            Collections.emptyList(),
            Lists.transform(
                page.getByXPath("//hudson/job/property/parameterDefinition[name='test']/choice"),
                new Function<Object, String>() {
                    public String apply(Object e) {
                        return (e instanceof DomElement) ? ((DomElement)e).getTextContent() : null;
                    }
                }
            )
        );
    }

    @Test
    public void testRestApiForTextAreaWithPermissionForView() throws Exception
    {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
            .grant(Item.READ, Item.BUILD, Jenkins.READ).everywhere().to("user")
        );

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
            "test",
            new MockTextAreaChoiceListProvider(
                Arrays.asList("foo", "bar", "baz"),
                null
            ),
            true,
            "description"
        );
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient();
        wc.login("user");

        final String onlyChoices = "?tree=jobs[property[parameterDefinitions[name,choices]]]";
        XmlPage page = getXmlPage(wc, "api/xml" + onlyChoices);
        assertEquals(
            Arrays.asList("foo", "bar", "baz"),
            Lists.transform(
                page.getByXPath("//hudson/job/property/parameterDefinition[name='test']/choice"),
                new Function<Object, String>() {
                    public String apply(Object e) {
                        return (e instanceof DomElement) ? ((DomElement)e).getTextContent() : null;
                    }
                }
            )
        );
    }

    @Test
    public void testRestApiWithoutPermission() throws Exception
    {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        // No Item.BUILD permission!
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Item.READ, Jenkins.READ).everywhere().to("user")
        );

        FreeStyleProject p = j.createFreeStyleProject();
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
            "test",
            new MockChoiceListProvider(
                Arrays.asList("foo", "bar", "baz"),
                null
            ),
            true,
            "description"
        );
        p.addProperty(new ParametersDefinitionProperty(def));

        WebClient wc = j.createWebClient();
        wc.login("user");

        XmlPage page = getXmlPage(wc, p.getUrl() + "/api/xml");
        assertEquals(
            Collections.emptyList(),
            page.getByXPath("//freeStyleProject/property/parameterDefinition[name='test']/choice")
        );
    }
}
