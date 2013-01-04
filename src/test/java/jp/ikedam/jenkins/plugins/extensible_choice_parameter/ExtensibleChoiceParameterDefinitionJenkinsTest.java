/**
 * 
 */
package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hudson.EnvVars;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.ExtensibleChoiceParameterDefinition.DescriptorImpl;

import org.junit.Ignore;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

/**
 * Tests for ExtensibleChoiceParameterDefinition, corresponding to Jenkins.
 */
public class ExtensibleChoiceParameterDefinitionJenkinsTest extends HudsonTestCase
{
    private ExtensibleChoiceParameterDefinition.DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl)(new ExtensibleChoiceParameterDefinition("name", null, false, "")).getDescriptor();
    }
    
    public void testDescriptorDoCheckNameOk()
    {
        ExtensibleChoiceParameterDefinition.DescriptorImpl descriptor = getDescriptor();
        
        // OK: lower alphabets
        assertEquals(descriptor.doCheckName("abc").kind, FormValidation.Kind.OK);
        
        // OK: upper alphabets
        assertEquals(descriptor.doCheckName("ABC").kind, FormValidation.Kind.OK);
        
        // OK: alphabets and numbers
        assertEquals(descriptor.doCheckName("abc123").kind, FormValidation.Kind.OK);
        
        // OK: alphabets, numbers, and underscores.
        assertEquals(descriptor.doCheckName("abc_1_2_3").kind, FormValidation.Kind.OK);
        
        // OK: starts with underscore.
        assertEquals(descriptor.doCheckName("_abc_1_2_3").kind, FormValidation.Kind.OK);
        
        // OK: blank in the beginning
        assertEquals(descriptor.doCheckName("  _abc_1_2_3").kind, FormValidation.Kind.OK);
        
        // OK: blank in the end
        assertEquals(descriptor.doCheckName("  _abc_1_2_3   ").kind, FormValidation.Kind.OK);
    }
    
    public void testDescriptorDoCheckNameError()
    {
        ExtensibleChoiceParameterDefinition.DescriptorImpl descriptor = getDescriptor();
        
        // ERROR: null
        assertEquals(descriptor.doCheckName(null).kind, FormValidation.Kind.ERROR);
        
        // ERROR: empty
        assertEquals(descriptor.doCheckName("").kind, FormValidation.Kind.ERROR);
        
        // ERROR: blank
        assertEquals(descriptor.doCheckName(" ").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value containing blank
        assertEquals(descriptor.doCheckName("a b").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value starts with a numeric letter.
        assertEquals(descriptor.doCheckName("1ab").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value contains a letter, not alphabet, number, nor underscore.
        assertEquals(descriptor.doCheckName("a-b-c").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value starts with a letter, not alphabet, number, nor underscore.
        assertEquals(descriptor.doCheckName("!ab").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value contains a multibyte letter.
        assertEquals(descriptor.doCheckName("ａb").kind, FormValidation.Kind.ERROR);
    }
    
    private static class MockChoiceListProvider extends ChoiceListProvider
    {
        private List<String> choiceList = null;
        public MockChoiceListProvider(List<String> choiceList){
            this.choiceList = choiceList;
        }
        @Override
        public List<String> getChoiceList()
        {
            return choiceList;
        }
        
    }
    
    /**
     * @param def
     * @param value
     * @return
     * @throws Exception 
     */
    private EnvVars runBuildWithSelectParameter(ParameterDefinition def, String value) throws Exception
    {
        FreeStyleProject job = createFreeStyleProject();
        job.addProperty(new ParametersDefinitionProperty(def));
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        job.getBuildersList().add(ceb);
        
        WebClient wc = new WebClient();
        
        // make output quiet.
        // comment out here if an unexpected behavior occurs.
        wc.setPrintContentOnFailingStatusCode(false);
        
        // Accessing build page causes 405 Method not allowed (I'm not sure why...), 
        // but the contents are correctly returned.
        // So suppress the exception.
        wc.setThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.getPage(job, "build?delay=0sec");
        wc.setThrowExceptionOnFailingStatusCode(true);
        
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
        
        while (job.getLastBuild().isBuilding()) Thread.sleep(100);
        
        job.delete();
        
        return ceb.getEnvVars();
    }
    
    private static void assertIllegalChoice(String message, Exception e) throws Exception
    {
        assertTrue(message, e instanceof FailingHttpStatusCodeException);
        if(!e.getMessage().startsWith("500 Illegal choice: ")){
            throw e;
        }
    }
    
    /**
     * test for createValue(StaplerRequest request, JSONObject jo)
     * @throws Exception 
     */
    public void testCreateValueFromView() throws Exception
    {
        String name = "PARAM1";
        String description = "Some Text";
        ChoiceListProvider provider = new MockChoiceListProvider(Arrays.asList("value1", "value2", "value3"));
        
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
            catch(Exception e)
            {
                assertIllegalChoice("input with non-editable", e);
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
                            new MockChoiceListProvider(new ArrayList<String>(0)),
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
                                new MockChoiceListProvider(new ArrayList<String>(0)),
                                false,
                                description
                        ),
                        value
                );
                assertEquals("no choice is provided and non-editable", value, envVars.get(name));
                assertTrue("This code would not be reached.", false);
            }
            catch(Exception e)
            {
                assertIllegalChoice("no choice is provided and non-editable", e);
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
            catch(Exception e)
            {
                assertIllegalChoice("provider is null and non-editable", e);
            }
        }
        
        
        // provider returns null and editable. any values can be accepted.
        {
            String value = "abc";
            EnvVars envVars = runBuildWithSelectParameter(
                    new ExtensibleChoiceParameterDefinition(
                            name,
                            new MockChoiceListProvider(null),
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
                                new MockChoiceListProvider(null),
                                false,
                                description
                        ),
                        value
                );
                assertEquals("provider returns null non-editable", value, envVars.get(name));
                assertTrue("This code would not be reached.", false);
            }
            catch(Exception e)
            {
                assertIllegalChoice("provider returns null non-editable", e);
            }
        }
    }
}
