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

import hudson.cli.BuildCommand;
import hudson.cli.CLICommandInvoker;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.User;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import jenkins.model.Jenkins;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Tests for SystemGroovyChoiceListProvider, corresponding to Jenkins.
 */
public class SystemGroovyChoiceListProviderJenkinsTest {
    @Rule
    public ExtensibleChoiceParameterJenkinsRule j = new ExtensibleChoiceParameterJenkinsRule();

    private static String properScript = "[\"a\", \"b\", \"c\"]";
    private static List<String> properScriptReturn = Arrays.asList("a", "b", "c");

    private static String nonstringScript = "[1, 2, 3]";
    private static List<String> nonstringScriptReturn = Arrays.asList("1", "2", "3");

    private static String nonlistScript = "return \"abc\"";

    private static String emptyListScript = "def ret = []";

    private static String nullScript = "null;";

    private static String emptyScript = "";

    private static String blankScript = "  ";

    private static String syntaxBrokenScript = "1abc = []";

    private static String exceptionScript = "hogehoge()";

    protected SystemGroovyChoiceListProvider.DescriptorImpl getDescriptor() {
        return (SystemGroovyChoiceListProvider.DescriptorImpl)
                Jenkins.getInstance().getDescriptorOrDie(SystemGroovyChoiceListProvider.class);
    }

    @Test
    public void testDescriptor_doFillDefaultChoiceItems() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        // Proper script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, properScript, true, false);
            assertEquals("Script returned an unexpected list", properScriptReturn.size() + 1, ret.size());
            for (int i = 0; i < properScriptReturn.size(); ++i) {
                assertEquals("Script returned an unexpected list", properScriptReturn.get(i), ret.get(i + 1).value);
            }
        }

        // Non-string list script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, nonstringScript, true, false);
            assertEquals("Script returned an unexpected list", nonstringScriptReturn.size() + 1, ret.size());
            for (int i = 0; i < nonstringScriptReturn.size(); ++i) {
                assertEquals("Script returned an unexpected list", nonstringScriptReturn.get(i), ret.get(i + 1).value);
            }
        }

        // non-list script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, nonlistScript, true, false);
            assertEquals("Script returning non-list must return an empty list", 1, ret.size());
        }

        // Empty list script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, emptyListScript, true, false);
            assertEquals("Script must return an empty list", 1, ret.size());
        }

        // Null script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, nullScript, true, false);
            assertEquals("Script with null must return an empty list", 1, ret.size());
        }

        // emptyScript
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, emptyScript, true, false);
            assertEquals("empty script must return an empty list", 1, ret.size());
        }

        // blankScript
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, blankScript, true, false);
            assertEquals("blank script must return an empty list", 1, ret.size());
        }

        // Syntax broken script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, syntaxBrokenScript, true, false);
            assertEquals("Syntax-broken-script must return an empty list", 1, ret.size());
        }

        // exceptionScript
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, exceptionScript, true, false);
            assertEquals("Script throwing an exception must return an empty list", 1, ret.size());
        }

        // null
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, null, true, false);
            assertEquals("null must return an empty list", 1, ret.size());
        }
    }

    @Test
    public void testDescriptor_doFillDefaultChoiceItemsWithoutSandbox() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        ListBoxModel ret = descriptor.doFillDefaultChoiceItems(
                p,
                properScript,
                false, // No "use groovy sandbox"
                false);
        assertEquals(1, ret.size());
    }

    @Test
    public void testDescriptor_doFillDefaultChoiceItemsWithoutContext() throws Exception {
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, properScript, true, false);
        assertEquals(1, ret.size());
    }

    @Test
    public void testDescriptor_doFillDefaultChoiceItemsWithoutPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .to("user")
                .grant(Item.READ, Jenkins.READ)
                .everywhere()
                .to("configurer"));
        FreeStyleProject p = j.createFreeStyleProject();
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        WebClient wc = j.createWebClient();
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);

        // user has no access to the job => 404
        User user = User.get("user");
        wc.login(user.getId());

        // missing Configure permission
        Page page = wc.goTo(
                p.getUrl() + descriptor.getDescriptorUrl() + "/fillDefaultChoiceItems/?script=" + properScript
                        + "&sandbox=" + true + "&usePredefinedVariables=" + false,
                null);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, page.getWebResponse().getStatusCode());

        // no job => nothing to reply, duplicate with withoutContext, but using web calls
        page = wc.goTo(
                descriptor.getDescriptorUrl() + "/fillDefaultChoiceItems/?script=" + properScript + "&sandbox=" + true
                        + "&usePredefinedVariables=" + false,
                null);
        assertEquals(HttpServletResponse.SC_OK, page.getWebResponse().getStatusCode());

        // configurer has access to the job but without Item/Configure permission => 403
        User configurer = User.get("configurer");
        wc.login(configurer.getId());

        // missing Configure permission
        page = wc.goTo(
                p.getUrl() + descriptor.getDescriptorUrl() + "/fillDefaultChoiceItems/?script=" + properScript
                        + "&sandbox=" + true + "&usePredefinedVariables=" + false,
                null);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, page.getWebResponse().getStatusCode());
    }

    @Test
    public void testDescriptor_doTest() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        // Proper script
        {
            FormValidation formValidation = descriptor.doTest(p, properScript, true, false);
            assertEquals("Test for proper script must succeed", FormValidation.Kind.OK, formValidation.kind);
        }

        // Syntax broken script
        {
            FormValidation formValidation = descriptor.doTest(p, syntaxBrokenScript, true, false);
            assertEquals("Test for broken script must fail", FormValidation.Kind.ERROR, formValidation.kind);
        }

        // Script raising an exception
        {
            FormValidation formValidation = descriptor.doTest(p, exceptionScript, true, false);
            assertEquals(
                    "Test for script raising an exception must fail", FormValidation.Kind.ERROR, formValidation.kind);
        }

        // Script returning non-list
        {
            FormValidation formValidation = descriptor.doTest(p, nonlistScript, true, false);
            assertEquals(
                    "Test for script returning non-list must fail", FormValidation.Kind.ERROR, formValidation.kind);
        }

        // Script returning null
        {
            FormValidation formValidation = descriptor.doTest(p, nullScript, true, false);
            assertEquals("Test for script retuning null must fail", FormValidation.Kind.ERROR, formValidation.kind);
        }
    }

    @Test
    public void testDescriptor_doTestWithoutSandbox() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        FormValidation formValidation = descriptor.doTest(
                p,
                properScript,
                false, // No "use groovy sandbox"
                false);
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    @Test
    public void testDescriptor_doTestWithoutContext() throws Exception {
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        FormValidation formValidation = descriptor.doTest(null, properScript, true, false);
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    @Test
    public void testDescriptor_doTestWithoutPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ)
                .everywhere()
                .to("user")
                .grant(Item.READ, Jenkins.READ)
                .everywhere()
                .to("configurer"));
        FreeStyleProject p = j.createFreeStyleProject();
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        WebClient wc = j.createWebClient();
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);

        // user has no access to the job => 404
        User user = User.get("user");
        wc.login(user.getId());

        // missing Configure permission
        Page page = wc.goTo(
                p.getUrl() + descriptor.getDescriptorUrl() + "/fillDefaultChoiceItems/?script=" + properScript
                        + "&sandbox=" + true + "&usePredefinedVariables=" + false,
                null);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, page.getWebResponse().getStatusCode());

        // no job => nothing to reply, duplicate with withoutContext, but using web calls
        page = wc.goTo(
                descriptor.getDescriptorUrl() + "/test/?script=" + properScript + "&sandbox=" + true
                        + "&usePredefinedVariables=" + false,
                null);
        assertEquals(HttpServletResponse.SC_OK, page.getWebResponse().getStatusCode());

        // configurer has access to the job but without Item/Configure permission => 403
        User configurer = User.get("configurer");
        wc.login(configurer.getId());

        // missing Configure permission
        page = wc.getPage(
                p,
                descriptor.getDescriptorUrl() + "/test/?script=" + properScript + "&sandbox=" + true
                        + "&usePredefinedVariables=" + false);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, page.getWebResponse().getStatusCode());
    }

    @Test
    public void testGetChoiceList() {
        // Proper script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(properScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals("Script returned an unexpected list", properScriptReturn.size(), ret.size());
            for (int i = 0; i < properScriptReturn.size(); ++i) {
                assertEquals("Script returned an unexpected list", properScriptReturn.get(i), ret.get(i));
            }
        }

        // non-string list script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(nonstringScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals("Script returned an unexpected list", nonstringScriptReturn.size(), ret.size());
            for (int i = 0; i < nonstringScriptReturn.size(); ++i) {
                assertEquals("Script returned an unexpected list", nonstringScriptReturn.get(i), ret.get(i));
            }
        }

        // non-list script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(nonlistScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals("Script retuning non-list must be fixed to an empty list", 0, ret.size());
        }

        // Empty list script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(emptyListScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals("Script must return an empty list", 0, ret.size());
        }

        // Null script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(nullScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals("Script with null must return an empty list", 0, ret.size());
        }

        // Empty script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(emptyScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals("Empty script must return an empty list", 0, ret.size());
        }

        // Blank script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(blankScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals("Blank script must return an empty list", 0, ret.size());
        }

        // Syntax broken script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(syntaxBrokenScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals("Syntax-broken-script must return an empty list", 0, ret.size());
        }

        // exceptionScript
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(exceptionScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals("Script throwing an exception must return an empty list", 0, ret.size());
        }

        // null
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(null, null);
            List<String> ret = target.getChoiceList();
            assertEquals("null must return an empty list", 0, ret.size());
        }
    }

    @Test
    public void testVariables() throws Exception {
        ScriptApproval.get().approveSignature("method hudson.model.Item getFullName");
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "test", new SystemGroovyChoiceListProvider("[project.fullName]", null, true), false, "test")));

        WebClient wc = j.createAllow405WebClient();
        HtmlPage page = wc.getPage(p, "build");

        List<DomElement> elements = page.getElementsByTagName("select");
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof HtmlSelect);
        HtmlSelect sel = (HtmlSelect) elements.get(0);
        assertEquals(1, sel.getOptionSize());
        assertEquals(p.getFullName(), sel.getOption(0).getValueAttribute());
    }

    @Test
    public void testProjectVariable() throws Exception {
        ScriptApproval.get().approveSignature("method hudson.model.Item getFullName");
        FreeStyleProject p = j.createFreeStyleProject();
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider("return [(project != null)?project.fullName:\"none\"]", null, true),
                false,
                "test")));
        p.getBuildersList().add(ceb);

        WebClient wc = j.createAllow405WebClient();
        HtmlPage page = wc.getPage(p, "build");

        List<DomElement> elements = page.getElementsByTagName("select");
        assertEquals(1, elements.size());
        assertTrue(elements.get(0) instanceof HtmlSelect);
        HtmlSelect sel = (HtmlSelect) elements.get(0);
        assertEquals(1, sel.getOptionSize());
        assertEquals(p.getFullName(), sel.getOption(0).getValueAttribute());

        // from CLI, the project does not passed properly.
        assertThat(
                new CLICommandInvoker(j, new BuildCommand()).invokeWithArgs("-s", p.getFullName()),
                CLICommandInvoker.Matcher.succeeded());
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertEquals("none", ceb.getEnvVars().get("test"));

        // once saved in configuration page, the project gets to be passed properly.
        p.getBuildersList().clear(); // CaptureEnvironmentBuilder does not support configuration pages.
        j.submit(wc.getPage(p, "configure").getFormByName("config"));
        p.getBuildersList().add(ceb);

        assertThat(
                new CLICommandInvoker(j, new BuildCommand()).invokeWithArgs("-s", p.getFullName()),
                CLICommandInvoker.Matcher.succeeded());
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertEquals(p.getFullName(), ceb.getEnvVars().get("test"));

        // this should be preserved even after reboot
        p.save();
        j.jenkins.reload();
        p = j.jenkins.getItemByFullName(p.getFullName(), FreeStyleProject.class);
        ceb = p.getBuildersList().get(CaptureEnvironmentBuilder.class);
        assertThat(
                new CLICommandInvoker(j, new BuildCommand()).invokeWithArgs("-s", p.getFullName()),
                CLICommandInvoker.Matcher.succeeded());
        j.assertBuildStatusSuccess(p.getLastBuild());
        assertEquals(p.getFullName(), ceb.getEnvVars().get("test"));
    }

    @Test
    public void testSystemGroovyChoiceListProvider_defaultChoice() {
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

    @Test
    public void testConfiguration1() throws Exception {
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider(
                        new SecureGroovyScript(
                                "[1, 2, 3]",
                                true, // sandbox
                                Collections.<ClasspathEntry>emptyList()),
                        "1",
                        true),
                false,
                "description");
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test"));
    }

    @Test
    public void testConfiguration2() throws Exception {
        // An arbitrary absolute path
        String classPath = new File(j.jenkins.getRootDir(), "userContent/somepath.jar").getAbsolutePath();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider(
                        new SecureGroovyScript(
                                "[1, 2, 3]",
                                false, // sandbox
                                Arrays.asList(new ClasspathEntry(classPath))),
                        null, // cannot configure default choice without sandbox.
                        true),
                false,
                "description");
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test"));
    }

    @Test
    @LocalData
    public void testMigrationFrom1_3_2() throws Exception {
        FreeStyleProject p = j.jenkins.getItemByFullName("test", FreeStyleProject.class);
        ExtensibleChoiceParameterDefinition def = (ExtensibleChoiceParameterDefinition)
                p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test");
        SystemGroovyChoiceListProvider provider = (SystemGroovyChoiceListProvider) def.getChoiceListProvider();

        SecureGroovyScript groovyScript = provider.getGroovyScript();
        assertNotNull(groovyScript);
        assertEquals("return [1, 2, 3]", groovyScript.getScript());
        assertTrue(groovyScript.isSandbox());
    }

    /**
     * Test that script-security is integrated as expected.
     *
     * @throws Exception
     */
    @Test
    public void testApproval() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy auth = new GlobalMatrixAuthorizationStrategy();
        auth.add(Jenkins.ADMINISTER, "admin");
        auth.add(Jenkins.READ, "user");
        auth.add(Item.READ, "user");
        auth.add(Item.CONFIGURE, "user");
        auth.add(Item.BUILD, "user");
        j.jenkins.setAuthorizationStrategy(auth);

        final String SCRIPT = "return java.util.Arrays.asList(new File('/').list());";

        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider(
                        new SecureGroovyScript(SCRIPT, false, Collections.<ClasspathEntry>emptyList()), null, false),
                false,
                "test")));

        WebClient wc = j.createAllow405WebClient();
        wc.login("user");
        try {
            j.submit(wc.getPage(p, "build?delay=0sec").getFormByName("parameters"));
            fail();
        } catch (FailingHttpStatusCodeException e) {
            // illegal choice
        }

        ScriptApproval.get().preapproveAll();

        j.submit(wc.getPage(p, "build?delay=0sec").getFormByName("parameters"));
        j.waitUntilNoActivity();
        j.assertBuildStatusSuccess(p.getLastBuild());
    }
}
