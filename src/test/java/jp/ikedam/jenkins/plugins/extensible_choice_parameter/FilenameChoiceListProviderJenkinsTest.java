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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for FilenameChoiceListProvider, concerned with Jenkins.
 */
@WithJenkins
class FilenameChoiceListProviderJenkinsTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testGetBaseDir() {
        // relative path
        {
            String path = "path/relative";
            File test = FilenameChoiceListProvider.getBaseDir(path);
            File expect = new File(Jenkins.get().getRootDir(), path);
            assertEquals(expect, test, "relative path must start from JENKINS_HOME");
        }

        // absolute path
        {
            String path = System.getProperty("os.name").toLowerCase().contains("windows")
                    ? "C:\\path\\abosolute"
                    : "/path/absolute";
            File test = FilenameChoiceListProvider.getBaseDir(path);
            File expect = new File(path);
            assertEquals(expect, test, "absolute path must be treat as is.");
        }
    }

    private FilenameChoiceListProvider.DescriptorImpl getDescriptor() {
        return (FilenameChoiceListProvider.DescriptorImpl)
                Jenkins.get().getDescriptor(FilenameChoiceListProvider.class);
    }

    @Test
    void testConfiguration() throws Exception {
        {
            FreeStyleProject p = j.createFreeStyleProject();
            FilenameChoiceListProvider expected = new FilenameChoiceListProvider(
                    "/some/path",
                    "**/*",
                    "**/*.java",
                    FilenameChoiceListProvider.ScanType.File,
                    false,
                    FilenameChoiceListProvider.EmptyChoiceType.None);
            p.addProperty(new ParametersDefinitionProperty(
                    new ExtensibleChoiceParameterDefinition("Choice", expected, false, "")));
            j.submit(j.createWebClient().getPage(p, "configure").getFormByName("config"));
            j.assertEqualDataBoundBeans(
                    expected,
                    ((ExtensibleChoiceParameterDefinition) p.getProperty(ParametersDefinitionProperty.class)
                                    .getParameterDefinition("Choice"))
                            .getChoiceListProvider());
        }
        {
            FreeStyleProject p = j.createFreeStyleProject();
            FilenameChoiceListProvider expected = new FilenameChoiceListProvider(
                    "C:\\some\\path",
                    "**/*.java",
                    "**/*Test.java",
                    FilenameChoiceListProvider.ScanType.Directory,
                    true,
                    FilenameChoiceListProvider.EmptyChoiceType.AtTop);
            p.addProperty(new ParametersDefinitionProperty(
                    new ExtensibleChoiceParameterDefinition("Choice", expected, false, "")));
            j.submit(j.createWebClient().getPage(p, "configure").getFormByName("config"));
            j.assertEqualDataBoundBeans(
                    expected,
                    ((ExtensibleChoiceParameterDefinition) p.getProperty(ParametersDefinitionProperty.class)
                                    .getParameterDefinition("Choice"))
                            .getChoiceListProvider());
        }
        {
            FreeStyleProject p = j.createFreeStyleProject();
            FilenameChoiceListProvider expected = new FilenameChoiceListProvider(
                    "relative/path",
                    "**/*",
                    "",
                    FilenameChoiceListProvider.ScanType.FileAndDirectory,
                    true,
                    FilenameChoiceListProvider.EmptyChoiceType.AtEnd);
            p.addProperty(new ParametersDefinitionProperty(
                    new ExtensibleChoiceParameterDefinition("Choice", expected, false, "")));
            j.submit(j.createWebClient().getPage(p, "configure").getFormByName("config"));
            j.assertEqualDataBoundBeans(
                    expected,
                    ((ExtensibleChoiceParameterDefinition) p.getProperty(ParametersDefinitionProperty.class)
                                    .getParameterDefinition("Choice"))
                            .getChoiceListProvider());
        }
    }

    @Test
    void testDescriptor_doCheckBaseDirPath() throws Exception {
        FilenameChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        File tempDir = Files.createTempDirectory("junit").toFile();
        try {
            // a proper directory
            {
                assertEquals(
                        FormValidation.Kind.OK,
                        descriptor.doCheckBaseDirPath(tempDir.getAbsolutePath()).kind,
                        "a proper directory");
            }

            // not exist
            {
                assertEquals(
                        FormValidation.Kind.WARNING,
                        descriptor.doCheckBaseDirPath(new File(tempDir, "hogehoge").getAbsolutePath()).kind,
                        "not exist");
            }

            // not a directory
            {
                File testFile = new File(tempDir, "test.txt");
                FileUtils.writeStringToFile(testFile, "hogehoge", StandardCharsets.UTF_8);
                assertEquals(
                        FormValidation.Kind.WARNING,
                        descriptor.doCheckBaseDirPath(testFile.getAbsolutePath()).kind,
                        "not exist");
            }

            // null
            {
                assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckBaseDirPath(null).kind, "null");
            }

            // empty
            {
                assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckBaseDirPath("").kind, "empty");
            }

            // blank
            {
                assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckBaseDirPath("  ").kind, "blank");
            }
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    @Test
    void testDescriptor_doCheckIncludePattern() {
        FilenameChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        // a proper pattern
        {
            String pattern = "**/*";
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckIncludePattern(pattern).kind, "a proper pattern");
        }

        // null
        {
            String pattern = null;
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckIncludePattern(pattern).kind, "null");
        }

        // empty
        {
            String pattern = "";
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckIncludePattern(pattern).kind, "empty");
        }

        // blank
        {
            String pattern = "  ";
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckIncludePattern(pattern).kind, "blank");
        }
    }

    @Test
    void testDescriptor_doCheckExcludePattern() {
        FilenameChoiceListProvider.DescriptorImpl descriptor = getDescriptor();

        // a proper pattern
        {
            String pattern = "**/*";
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckExcludePattern(pattern).kind, "a proper pattern");
        }

        // null
        {
            String pattern = null;
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckExcludePattern(pattern).kind, "null");
        }

        // empty
        {
            String pattern = "";
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckExcludePattern(pattern).kind, "empty");
        }

        // blank
        {
            String pattern = "  ";
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckExcludePattern(pattern).kind, "blank");
        }
    }

    @Issue("JENKINS-28841")
    @Test
    void testDoTest() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "Choice",
                new FilenameChoiceListProvider(
                        ".",
                        "*",
                        "",
                        FilenameChoiceListProvider.ScanType.File,
                        false,
                        FilenameChoiceListProvider.EmptyChoiceType.None),
                false,
                "")));

        WebClient wc = j.createWebClient();
        HtmlPage page = wc.getPage(p, "configure");

        // find the button to call doTest
        // List<HtmlElement> elements = page.getElementsByName("choiceListProvider");
        // assertEquals(1, elements.size());
        // HtmlElement choiceListProviderBlock = elements.get(0);
        HtmlElement button = page.getFirstByXPath(
                "//*[@name='choiceListProvider']//button|//*[@name='choiceListProvider']//input[@type='button']");
        assertNotNull(button);
        button.click();
    }
}
