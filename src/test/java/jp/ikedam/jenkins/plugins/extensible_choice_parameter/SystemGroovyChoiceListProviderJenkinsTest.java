/*
 * The MIT License
 * 
 * Copyright (c) 2013 IKEDA Yasuyuki
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
import hudson.cli.CLI;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

import jenkins.model.Jenkins;

/**
 * Tests for SystemGroovyChoiceListProvider, corresponding to Jenkins.
 */
public class SystemGroovyChoiceListProviderJenkinsTest
{
    @Rule
    public ExtensibleChoiceParameterJenkinsRule j = new ExtensibleChoiceParameterJenkinsRule();
    
    static private String properScript = "[\"a\", \"b\", \"c\"]";
    static private List<String> properScriptReturn = Arrays.asList("a", "b", "c");
    
    static private String nonstringScript = "[1, 2, 3]";
    static private List<String> nonstringScriptReturn = Arrays.asList("1", "2", "3");
    
    static private String nonlistScript = "return \"abc\"";
    
    static private String emptyListScript = "def ret = []";
    
    static private String nullScript = "null;";
    
    static private String emptyScript = "";
    
    static private String blankScript = "  ";
    
    static private String syntaxBrokenScript = "1abc = []";
    
    static private String exceptionScript = "hogehoge()";
    
    protected SystemGroovyChoiceListProvider.DescriptorImpl getDescriptor()
    {
        return (SystemGroovyChoiceListProvider.DescriptorImpl)Jenkins.getInstance().getDescriptorOrDie(SystemGroovyChoiceListProvider.class);
    }
    
    @Test
    public void testDescriptor_doFillDefaultChoiceItems()
    {
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        
        // Proper script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, properScript, true, false);
            assertEquals("Script returned an unexpected list", properScriptReturn.size() + 1, ret.size());
            for(int i = 0; i < properScriptReturn.size(); ++i)
            {
                assertEquals("Script returned an unexpected list", properScriptReturn.get(i), ret.get(i + 1).value);
            }
        }
        
        // Non-string list script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, nonstringScript, true, false);
            assertEquals("Script returned an unexpected list", nonstringScriptReturn.size() + 1, ret.size());
            for(int i = 0; i < nonstringScriptReturn.size(); ++i)
            {
                assertEquals("Script returned an unexpected list", nonstringScriptReturn.get(i), ret.get(i + 1).value);
            }
        }
        
        // non-list script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, nonlistScript, true, false);
            assertEquals("Script returning non-list must return an empty list", 1, ret.size());
        }
        
        // Empty list script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, emptyListScript, true, false);
            assertEquals("Script must return an empty list", 1, ret.size());
        }
        
        // Null script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, nullScript, true, false);
            assertEquals("Script with null must return an empty list", 1, ret.size());
        }
        
        // emptyScript
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, emptyScript, true, false);
            assertEquals("empty script must return an empty list", 1, ret.size());
        }
        
        // blankScript
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, blankScript, true, false);
            assertEquals("blank script must return an empty list", 1, ret.size());
        }
        
        // Syntax broken script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, syntaxBrokenScript, true, false);
            assertEquals("Syntax-broken-script must return an empty list", 1, ret.size());
        }
        
        // exceptionScript
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, exceptionScript, true, false);
            assertEquals("Script throwing an exception must return an empty list", 1, ret.size());
        }
        
        // null
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, null, true, false);
            assertEquals("null must return an empty list", 1, ret.size());
        }
    }
    
    @Test
    public void testDescriptor_doTest()
    {
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        
        // Proper script
        {
            FormValidation formValidation = descriptor.doTest(null, properScript, true, false);
            assertEquals("Test for proper script must succeed", FormValidation.Kind.OK, formValidation.kind);
        }
        
        // Syntax broken script
        {
            FormValidation formValidation = descriptor.doTest(null, syntaxBrokenScript, true, false);
            assertEquals("Test for broken script must fail", FormValidation.Kind.ERROR, formValidation.kind);
        }
        
        // Script raising an exception
        {
            FormValidation formValidation = descriptor.doTest(null, exceptionScript, true, false);
            assertEquals("Test for script raising an exception must fail", FormValidation.Kind.ERROR, formValidation.kind);
        }
        
        // Script returning non-list
        {
            FormValidation formValidation = descriptor.doTest(null, nonlistScript, true, false);
            assertEquals("Test for script returning non-list must fail", FormValidation.Kind.ERROR, formValidation.kind);
        }
        
        // Script returning null
        {
            FormValidation formValidation = descriptor.doTest(null, nullScript, true, false);
            assertEquals("Test for script retuning null must fail", FormValidation.Kind.ERROR, formValidation.kind);
        }
    }
    
    @Test
    public void testGetChoiceList()
    {
        // Proper script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    properScript,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("Script returned an unexpected list", properScriptReturn.size(), ret.size());
            for(int i = 0; i < properScriptReturn.size(); ++i)
            {
                assertEquals("Script returned an unexpected list", properScriptReturn.get(i), ret.get(i));
            }
        }
        
        // non-string list script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    nonstringScript,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("Script returned an unexpected list", nonstringScriptReturn.size(), ret.size());
            for(int i = 0; i < nonstringScriptReturn.size(); ++i)
            {
                assertEquals("Script returned an unexpected list", nonstringScriptReturn.get(i), ret.get(i));
            }
        }
        
        // non-list script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    nonlistScript,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("Script retuning non-list must be fixed to an empty list", 0, ret.size());
        }
        
        // Empty list script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    emptyListScript,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("Script must return an empty list", 0, ret.size());
        }
        
        // Null script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    nullScript,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("Script with null must return an empty list", 0, ret.size());
        }
        
        // Empty script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    emptyScript,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("Empty script must return an empty list", 0, ret.size());
        }
        
        // Blank script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    blankScript,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("Blank script must return an empty list", 0, ret.size());
        }
        
        // Syntax broken script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    syntaxBrokenScript,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("Syntax-broken-script must return an empty list", 0, ret.size());
        }
        
        // exceptionScript
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    exceptionScript,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("Script throwing an exception must return an empty list", 0, ret.size());
        }
        
        // null
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(
                    null,
                    null
            );
            List<String> ret = target.getChoiceList();
            assertEquals("null must return an empty list", 0, ret.size());
        }
    }
    
    @Test
    public void testVariables() throws Exception
    {
        ScriptApproval.get().approveSignature("method hudson.model.PersistenceRoot getRootDir");
        ScriptApproval.get().approveSignature("method java.io.File getAbsolutePath");
        ScriptApproval.get().approveSignature("method hudson.model.Item getFullName");
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider("[jenkins.rootDir.absolutePath, project.fullName]", null, true),
                false,
                "test"
        )));
        
        WebClient wc = j.createAllow405WebClient();
        HtmlPage page = wc.getPage(p, "build");
        
        List<HtmlElement> elements = page.getElementsByTagName("select");
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof HtmlSelect);
        HtmlSelect sel = (HtmlSelect)elements.get(0);
        assertEquals(2, sel.getOptionSize());
        assertEquals(j.jenkins.getRootDir().getAbsolutePath(), sel.getOption(0).getValueAttribute());
        assertEquals(p.getFullName(), sel.getOption(1).getValueAttribute());
    }
    
    @Test
    public void testProjectVariable() throws Exception
    {
        ScriptApproval.get().approveSignature("method hudson.model.Item getFullName");
        FreeStyleProject p = j.createFreeStyleProject();
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider("return [(project != null)?project.fullName:\"none\"]", null, true),
                false,
                "test"
        )));
        p.getBuildersList().add(ceb);
        
        WebClient wc = j.createAllow405WebClient();
        HtmlPage page = wc.getPage(p, "build");
        
        List<HtmlElement> elements = page.getElementsByTagName("select");
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof HtmlSelect);
        HtmlSelect sel = (HtmlSelect)elements.get(0);
        assertEquals(1, sel.getOptionSize());
        assertEquals(p.getFullName(), sel.getOption(0).getValueAttribute());
        
        // from CLI, the project does not passed properly.
        CLI cli = new CLI(j.getURL());
        assertEquals(0, cli.execute("build", p.getFullName(), "-s"));
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertEquals("none", ceb.getEnvVars().get("test"));
        
        // once saved in configuration page, the project gets to be passed properly.
        p.getBuildersList().clear(); // CaptureEnvironmentBuilder does not support configuration pages.
        j.submit(wc.getPage(p, "configure").getFormByName("config"));
        p.getBuildersList().add(ceb);
        
        assertEquals(0, cli.execute("build", p.getFullName(), "-s"));
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertEquals(p.getFullName(), ceb.getEnvVars().get("test"));
        
        // this should be preserved even after reboot
        p.save();
        j.jenkins.reload();
        p = j.jenkins.getItemByFullName(p.getFullName(), FreeStyleProject.class);
        ceb = p.getBuildersList().get(CaptureEnvironmentBuilder.class);
        assertEquals(0, cli.execute("build", p.getFullName(), "-s"));
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertEquals(p.getFullName(), ceb.getEnvVars().get("test"));
    }
    
    @Test
    public void testSystemGroovyChoiceListProvider_scriptText()
    {
        // simple value
        {
            String scriptText = "abc";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
        
        // value with spaces
        {
            String scriptText = "  abc  \n  cdf  ";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
        
        // empty
        {
            String scriptText = "";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
        
        // blank
        {
            String scriptText = "   ";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
        
        // null
        {
            String scriptText = null;
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
    }
    
    @Test
    public void testSystemGroovyChoiceListProvider_defaultChoice()
    {
        String scriptText = "abc";
        
        // a value
        {
            String defaultChoice = "some value";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals("a value", defaultChoice, target.getDefaultChoice());
        }
        
        // null
        {
            String defaultChoice = null;
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals("null", defaultChoice, target.getDefaultChoice());
        }
        
        // empty
        {
            String defaultChoice = "";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals("empty", defaultChoice, target.getDefaultChoice());
        }
        
        // blank
        {
            String defaultChoice = "  ";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals("blank", defaultChoice, target.getDefaultChoice());
        }
    }
}
