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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import hudson.cli.BuildCommand;
import hudson.cli.CLICommandInvoker;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.User;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.jenkinsci.plugins.matrixauth.AuthorizationType;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Tests for SystemGroovyChoiceListProvider, corresponding to Jenkins.
 */
@WithJenkins
class SystemGroovyChoiceListProviderJenkinsTest {

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

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    protected SystemGroovyChoiceListProvider.DescriptorImpl getDescriptor() {
        return (SystemGroovyChoiceListProvider.DescriptorImpl)
                Jenkins.get().getDescriptorOrDie(SystemGroovyChoiceListProvider.class);
    }

    @Test
    void testDescriptor_doFillDefaultChoiceItems() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        // Proper script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, properScript, true, false);
            assertEquals(properScriptReturn.size() + 1, ret.size(), "Script returned an unexpected list");
            for (int i = 0; i < properScriptReturn.size(); ++i) {
                assertEquals(properScriptReturn.get(i), ret.get(i + 1).value, "Script returned an unexpected list");
            }
        }

        // Non-string list script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, nonstringScript, true, false);
            assertEquals(nonstringScriptReturn.size() + 1, ret.size(), "Script returned an unexpected list");
            for (int i = 0; i < nonstringScriptReturn.size(); ++i) {
                assertEquals(nonstringScriptReturn.get(i), ret.get(i + 1).value, "Script returned an unexpected list");
            }
        }

        // non-list script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, nonlistScript, true, false);
            assertEquals(1, ret.size(), "Script returning non-list must return an empty list");
        }

        // Empty list script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, emptyListScript, true, false);
            assertEquals(1, ret.size(), "Script must return an empty list");
        }

        // Null script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, nullScript, true, false);
            assertEquals(1, ret.size(), "Script with null must return an empty list");
        }

        // emptyScript
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, emptyScript, true, false);
            assertEquals(1, ret.size(), "empty script must return an empty list");
        }

        // blankScript
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, blankScript, true, false);
            assertEquals(1, ret.size(), "blank script must return an empty list");
        }

        // Syntax broken script
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, syntaxBrokenScript, true, false);
            assertEquals(1, ret.size(), "Syntax-broken-script must return an empty list");
        }

        // exceptionScript
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, exceptionScript, true, false);
            assertEquals(1, ret.size(), "Script throwing an exception must return an empty list");
        }

        // null
        {
            ListBoxModel ret = descriptor.doFillDefaultChoiceItems(p, null, true, false);
            assertEquals(1, ret.size(), "null must return an empty list");
        }
    }

    @Test
    void testDescriptor_doFillDefaultChoiceItemsWithoutSandbox() throws Exception {
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
    void testDescriptor_doFillDefaultChoiceItemsWithoutContext() {
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        ListBoxModel ret = descriptor.doFillDefaultChoiceItems(null, properScript, true, false);
        assertEquals(1, ret.size());
    }

    @Test
    void testDescriptor_doFillDefaultChoiceItemsWithoutPermission() throws Exception {
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
        User user = User.getOrCreateByIdOrFullName("user");
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
        User configurer = User.getOrCreateByIdOrFullName("configurer");
        wc.login(configurer.getId());

        // missing Configure permission
        page = wc.goTo(
                p.getUrl() + descriptor.getDescriptorUrl() + "/fillDefaultChoiceItems/?script=" + properScript
                        + "&sandbox=" + true + "&usePredefinedVariables=" + false,
                null);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, page.getWebResponse().getStatusCode());
    }

    @Test
    void testDescriptor_doTest() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        // Proper script
        {
            FormValidation formValidation = descriptor.doTest(p, properScript, true, false);
            assertEquals(FormValidation.Kind.OK, formValidation.kind, "Test for proper script must succeed");
        }

        // Syntax broken script
        {
            FormValidation formValidation = descriptor.doTest(p, syntaxBrokenScript, true, false);
            assertEquals(FormValidation.Kind.ERROR, formValidation.kind, "Test for broken script must fail");
        }

        // Script raising an exception
        {
            FormValidation formValidation = descriptor.doTest(p, exceptionScript, true, false);
            assertEquals(
                    FormValidation.Kind.ERROR, formValidation.kind, "Test for script raising an exception must fail");
        }

        // Script returning non-list
        {
            FormValidation formValidation = descriptor.doTest(p, nonlistScript, true, false);
            assertEquals(
                    FormValidation.Kind.ERROR, formValidation.kind, "Test for script returning non-list must fail");
        }

        // Script returning null
        {
            FormValidation formValidation = descriptor.doTest(p, nullScript, true, false);
            assertEquals(FormValidation.Kind.ERROR, formValidation.kind, "Test for script retuning null must fail");
        }
    }

    @Test
    void testDescriptor_doTestWithoutSandbox() throws Exception {
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
    void testDescriptor_doTestWithoutContext() {
        SystemGroovyChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        FormValidation formValidation = descriptor.doTest(null, properScript, true, false);
        assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    @Test
    void testDescriptor_doTestWithoutPermission() throws Exception {
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
        User user = User.getOrCreateByIdOrFullName("user");
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
        User configurer = User.getOrCreateByIdOrFullName("configurer");
        wc.login(configurer.getId());

        // missing Configure permission
        page = wc.getPage(
                p,
                descriptor.getDescriptorUrl() + "/test/?script=" + properScript + "&sandbox=" + true
                        + "&usePredefinedVariables=" + false);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, page.getWebResponse().getStatusCode());
    }

    @Test
    void testGetChoiceList() {
        // Proper script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(properScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals(properScriptReturn.size(), ret.size(), "Script returned an unexpected list");
            for (int i = 0; i < properScriptReturn.size(); ++i) {
                assertEquals(properScriptReturn.get(i), ret.get(i), "Script returned an unexpected list");
            }
        }

        // non-string list script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(nonstringScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals(nonstringScriptReturn.size(), ret.size(), "Script returned an unexpected list");
            for (int i = 0; i < nonstringScriptReturn.size(); ++i) {
                assertEquals(nonstringScriptReturn.get(i), ret.get(i), "Script returned an unexpected list");
            }
        }

        // non-list script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(nonlistScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals(0, ret.size(), "Script retuning non-list must be fixed to an empty list");
        }

        // Empty list script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(emptyListScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals(0, ret.size(), "Script must return an empty list");
        }

        // Null script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(nullScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals(0, ret.size(), "Script with null must return an empty list");
        }

        // Empty script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(emptyScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals(0, ret.size(), "Empty script must return an empty list");
        }

        // Blank script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(blankScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals(0, ret.size(), "Blank script must return an empty list");
        }

        // Syntax broken script
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(syntaxBrokenScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals(0, ret.size(), "Syntax-broken-script must return an empty list");
        }

        // exceptionScript
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(exceptionScript, null);
            List<String> ret = target.getChoiceList();
            assertEquals(0, ret.size(), "Script throwing an exception must return an empty list");
        }

        // null
        {
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(null, null);
            List<String> ret = target.getChoiceList();
            assertEquals(0, ret.size(), "null must return an empty list");
        }
    }

    @Test
    void testVariables() throws Exception {
        ScriptApproval.get().approveSignature("method hudson.model.Item getFullName");
        ScriptApproval.get().approveSignature("method jenkins.model.FullyNamed getFullName");
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "test", new SystemGroovyChoiceListProvider("[project.fullName]", null, true), false, "test")));

        WebClient wc = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.getPage(p, "build");

        List<DomElement> elements = page.getElementsByTagName("select");
        assertEquals(1, elements.size());
        assertInstanceOf(HtmlSelect.class, elements.get(0));
        HtmlSelect sel = (HtmlSelect) elements.get(0);
        assertEquals(1, sel.getOptionSize());
        assertEquals(p.getFullName(), sel.getOption(0).getValueAttribute());
    }

    @Test
    void testProjectVariable() throws Exception {
        ScriptApproval.get().approveSignature("method hudson.model.Item getFullName");
        ScriptApproval.get().approveSignature("method jenkins.model.FullyNamed getFullName");
        FreeStyleProject p = j.createFreeStyleProject();
        CaptureEnvironmentBuilder ceb = new CaptureEnvironmentBuilder();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider("return [(project != null)?project.fullName:\"none\"]", null, true),
                false,
                "test")));
        p.getBuildersList().add(ceb);

        WebClient wc = j.createWebClient().withThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.getPage(p, "build");

        List<DomElement> elements = page.getElementsByTagName("select");
        assertEquals(1, elements.size());
        assertInstanceOf(HtmlSelect.class, elements.get(0));
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
    void testSystemGroovyChoiceListProvider_defaultChoice() {
        String scriptText = "abc";

        // a value
        {
            String defaultChoice = "some value";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals(defaultChoice, target.getDefaultChoice(), "a value");
        }

        // null
        {
            String defaultChoice = null;
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals(defaultChoice, target.getDefaultChoice(), "null");
        }

        // empty
        {
            String defaultChoice = "";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals(defaultChoice, target.getDefaultChoice(), "empty");
        }

        // blank
        {
            String defaultChoice = "  ";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals(defaultChoice, target.getDefaultChoice(), "blank");
        }
    }

    @Test
    void testConfiguration1() throws Exception {
        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider(
                        new SecureGroovyScript(
                                "[1, 2, 3]",
                                true, // sandbox
                                Collections.emptyList()),
                        "1",
                        true),
                false,
                "description");
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);
        p.doReload(); // Workaround to drop transient properties in Script Security 1172.v35f6a_0b_8207e+

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test"));
    }

    @Test
    void testConfiguration2() throws Exception {
        // An arbitrary absolute path
        String classPath = new File(j.jenkins.getRootDir(), "userContent/somepath.jar").getAbsolutePath();

        ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider(
                        new SecureGroovyScript(
                                "[1, 2, 3]",
                                false, // sandbox
                                List.of(new ClasspathEntry(classPath))),
                        null, // cannot configure default choice without sandbox.
                        true),
                false,
                "description");
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(def));

        j.configRoundtrip(p);
        p.doReload(); // Workaround to drop transient properties in Script Security 1172.v35f6a_0b_8207e+

        j.assertEqualDataBoundBeans(
                def, p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("test"));
    }

    @Test
    @LocalData
    void testMigrationFrom1_3_2() {
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
    void testApproval() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy auth = new GlobalMatrixAuthorizationStrategy();
        auth.add(Jenkins.ADMINISTER, new PermissionEntry(AuthorizationType.EITHER, "admin"));
        auth.add(Jenkins.READ, new PermissionEntry(AuthorizationType.EITHER, "user"));
        auth.add(Item.READ, new PermissionEntry(AuthorizationType.EITHER, "user"));
        auth.add(Item.CONFIGURE, new PermissionEntry(AuthorizationType.EITHER, "user"));
        auth.add(Item.BUILD, new PermissionEntry(AuthorizationType.EITHER, "user"));
        j.jenkins.setAuthorizationStrategy(auth);

        final String SCRIPT = "return java.util.Arrays.asList(new File('/').list());";

        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "test",
                new SystemGroovyChoiceListProvider(
                        new SecureGroovyScript(SCRIPT, false, Collections.emptyList()), null, false),
                false,
                "test")));

        WebClient wc = j.createWebClient();
        wc.login("user");

        assertThrows(
                FailingHttpStatusCodeException.class,
                () -> j.submit(wc.getPage(p, "build?delay=0sec").getFormByName("parameters")));

        ScriptApproval.get().preapproveAll();

        wc.setThrowExceptionOnFailingStatusCode(false);
        j.submit(wc.getPage(p, "build?delay=0sec").getFormByName("parameters"));
        j.waitUntilNoActivity();
        j.assertBuildStatusSuccess(p.getLastBuild());
    }
}
