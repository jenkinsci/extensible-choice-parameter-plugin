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
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;

import jenkins.model.Jenkins;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Tests for FilenameChoiceListProvider, concerned with Jenkins.
 */
public class FilenameChoiceListProviderJenkinsTest
{
    @Rule
    public ExtensibleChoiceParameterJenkinsRule j = new ExtensibleChoiceParameterJenkinsRule();
    
    @Test
    public void testGetBaseDir()
    {
        // relative path
        {
            String path = "path/relative";
            File test = FilenameChoiceListProvider.getBaseDir(path);
            File expect = new File(Jenkins.getInstance().getRootDir(), path);
            assertEquals("relative path must start from JENKINS_HOME", expect, test);
        }
        
        // absolute path
        {
            String path = (SystemUtils.IS_OS_WINDOWS)?"C:\\path\\abosolute":"/path/absolute";
            File test = FilenameChoiceListProvider.getBaseDir(path);
            File expect = new File(path);
            assertEquals("absolute path must be treat as is.", expect, test);
        }
    }
    
    private FilenameChoiceListProvider.DescriptorImpl getDescriptor()
    {
        return (FilenameChoiceListProvider.DescriptorImpl)Jenkins.getInstance().getDescriptor(FilenameChoiceListProvider.class);
    }
    
    @Test
    public void testConfiguration() throws Exception
    {
        {
            FreeStyleProject p = j.createFreeStyleProject();
            FilenameChoiceListProvider expected = new FilenameChoiceListProvider(
                    "/some/path",
                    "**/*",
                    "**/*.java",
                    FilenameChoiceListProvider.ScanType.File,
                    false,
                    FilenameChoiceListProvider.EmptyChoiceType.None
            );
            p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                    "Choice",
                    expected,
                    false,
                    ""
            )));
            j.submit(j.createWebClient().getPage(p, "configure").getFormByName("config"));
            j.assertEqualDataBoundBeans(
                    expected,
                    ((ExtensibleChoiceParameterDefinition)p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("Choice")).getChoiceListProvider()
            );
        }
        {
            FreeStyleProject p = j.createFreeStyleProject();
            FilenameChoiceListProvider expected = new FilenameChoiceListProvider(
                    "C:\\some\\path",
                    "**/*.java",
                    "**/*Test.java",
                    FilenameChoiceListProvider.ScanType.Directory,
                    true,
                    FilenameChoiceListProvider.EmptyChoiceType.AtTop
            );
            p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                    "Choice",
                    expected,
                    false,
                    ""
            )));
            j.submit(j.createWebClient().getPage(p, "configure").getFormByName("config"));
            j.assertEqualDataBoundBeans(
                    expected,
                    ((ExtensibleChoiceParameterDefinition)p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("Choice")).getChoiceListProvider()
            );
        }
        {
            FreeStyleProject p = j.createFreeStyleProject();
            FilenameChoiceListProvider expected = new FilenameChoiceListProvider(
                    "relative/path",
                    "**/*",
                    "",
                    FilenameChoiceListProvider.ScanType.FileAndDirectory,
                    true,
                    FilenameChoiceListProvider.EmptyChoiceType.AtEnd
            );
            p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                    "Choice",
                    expected,
                    false,
                    ""
            )));
            j.submit(j.createWebClient().getPage(p, "configure").getFormByName("config"));
            j.assertEqualDataBoundBeans(
                    expected,
                    ((ExtensibleChoiceParameterDefinition)p.getProperty(ParametersDefinitionProperty.class).getParameterDefinition("Choice")).getChoiceListProvider()
            );
        }
    }
    
    @Test
    public void testDescriptor_doCheckBaseDirPath() throws IOException
    {
        FilenameChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        
        File tempDir = j.createTmpDir();
        try
        {
            // a proper directory
            {
                assertEquals(
                        "a proper directory",
                        FormValidation.Kind.OK,
                        descriptor.doCheckBaseDirPath(tempDir.getAbsolutePath()).kind
                );
            }
            
            // not exist
            {
                assertEquals(
                        "not exist",
                        FormValidation.Kind.WARNING,
                        descriptor.doCheckBaseDirPath(new File(tempDir, "hogehoge").getAbsolutePath()).kind
                );
            }
            
            // not a directory
            {
                File testFile = new File(tempDir, "test.txt");
                FileUtils.writeStringToFile(testFile, "hogehoge");
                assertEquals(
                        "not exist",
                        FormValidation.Kind.WARNING,
                        descriptor.doCheckBaseDirPath(testFile.getAbsolutePath()).kind
                );
            }
            
            // null
            {
                assertEquals(
                        "null",
                        FormValidation.Kind.ERROR,
                        descriptor.doCheckBaseDirPath(null).kind
                );
            }
            
            // empty
            {
                assertEquals(
                        "empty",
                        FormValidation.Kind.ERROR,
                        descriptor.doCheckBaseDirPath("").kind
                );
            }
            
            // blank
            {
                assertEquals(
                        "blank",
                        FormValidation.Kind.ERROR,
                        descriptor.doCheckBaseDirPath("  ").kind
                );
            }
        }
        finally
        {
            FileUtils.deleteDirectory(tempDir);
        }
    }
    
    @Test
    public void testDescriptor_doCheckIncludePattern() throws IOException
    {
        FilenameChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        
        // a proper pattern
        {
            String pattern = "**/*";
            assertEquals(
                    "a proper pattern",
                    FormValidation.Kind.OK,
                    descriptor.doCheckIncludePattern(pattern).kind
            );
        }
        
        // null
        {
            String pattern = null;
            assertEquals(
                    "null",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckIncludePattern(pattern).kind
            );
        }
        
        // empty
        {
            String pattern = "";
            assertEquals(
                    "empty",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckIncludePattern(pattern).kind
            );
        }
        
        // blank
        {
            String pattern = "  ";
            assertEquals(
                    "blank",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckIncludePattern(pattern).kind
            );
        }
    }
    
    @Test
    public void testDescriptor_doCheckExcludePattern() throws IOException
    {
        FilenameChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        
        // a proper pattern
        {
            String pattern = "**/*";
            assertEquals(
                    "a proper pattern",
                    FormValidation.Kind.OK,
                    descriptor.doCheckExcludePattern(pattern).kind
            );
        }
        
        // null
        {
            String pattern = null;
            assertEquals(
                    "null",
                    FormValidation.Kind.OK,
                    descriptor.doCheckExcludePattern(pattern).kind
            );
        }
        
        // empty
        {
            String pattern = "";
            assertEquals(
                    "empty",
                    FormValidation.Kind.OK,
                    descriptor.doCheckExcludePattern(pattern).kind
            );
        }
        
        // blank
        {
            String pattern = "  ";
            assertEquals(
                    "blank",
                    FormValidation.Kind.OK,
                    descriptor.doCheckExcludePattern(pattern).kind
            );
        }
    }
    
    @Bug(28841)
    @Test
    public void testDoTest() throws Exception
    {
        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new ExtensibleChoiceParameterDefinition(
                "Choice",
                new FilenameChoiceListProvider(
                        ".",
                        "*",
                        "",
                        FilenameChoiceListProvider.ScanType.File,
                        false,
                        FilenameChoiceListProvider.EmptyChoiceType.None
                ),
                false,
                ""
        )));
        
        WebClient wc = j.createWebClient();
        HtmlPage page = wc.getPage(p, "configure");
        
        // find the button to call doTest
        //List<HtmlElement> elements = page.getElementsByName("choiceListProvider");
        //assertEquals(1, elements.size());
        //HtmlElement choiceListProviderBlock = elements.get(0);
        HtmlElement button = page.<HtmlElement>getFirstByXPath("//*[@name='choiceListProvider']//button|//*[@name='choiceListProvider']//input[@type='button']");
        assertNotNull(button);
        button.click();
    }
}
