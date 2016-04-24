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

import hudson.Util;
import hudson.PluginWrapper;

import java.io.IOException;

import org.apache.commons.httpclient.HttpStatus;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;
import org.jvnet.hudson.test.TestPluginManager;

import com.gargoylesoftware.htmlunit.WebResponse;

/**
 * Utility class for Tests.
 */
public class ExtensibleChoiceParameterJenkinsRule extends JenkinsRule
{
    private static Thread deleteThread = null;
    
    /**
     * Cleanup the temporary directory created by org.jvnet.hudson.test.TestPluginManager.
     * Needed for Jenkins < 1.510
     */
    public static synchronized void registerCleanup()
    {
        if(deleteThread != null)
        {
            return;
        }
        deleteThread = new Thread("HOTFIX: cleanup " + TestPluginManager.INSTANCE.rootDir)
        {
            @Override public void run()
            {
                if(TestPluginManager.INSTANCE != null
                        && TestPluginManager.INSTANCE.rootDir != null
                        && TestPluginManager.INSTANCE.rootDir.exists())
                {
                    // Work as PluginManager#stop
                    for(PluginWrapper p: TestPluginManager.INSTANCE.getPlugins())
                    {
                        p.stop();
                        p.releaseClassLoader();
                    }
                    TestPluginManager.INSTANCE.getPlugins().clear();
                    System.gc();
                    try
                    {
                        Util.deleteRecursive(TestPluginManager.INSTANCE.rootDir);
                    }
                    catch (IOException x)
                    {
                        x.printStackTrace();
                    }
                }
            }
        };
        
        Runtime.getRuntime().addShutdownHook(deleteThread);
    }
    
    static
    {
        registerCleanup();
    }
    
    @Override
    protected void after()
    {
        super.after();
        
        // TestEnvironment is not cleaned in Jenkins < 1.482.
        if(TestEnvironment.get() != null)
        {
            try
            {
                TestEnvironment.get().dispose();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Get Web Client that allows 405 Method Not Allowed.
     * This happens when accessing build page of a project with parameters.
     * 
     * @return WebClient
     */
    public WebClient createAllow405WebClient()
    {
        return new WebClient()
        {
            private static final long serialVersionUID = -7231209645303821638L;
            
            @Override
            public void throwFailingHttpStatusCodeExceptionIfNecessary(
                    WebResponse webResponse)
            {
                if(webResponse.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED)
                {
                    // allow 405.
                    return;
                }
                super.throwFailingHttpStatusCodeExceptionIfNecessary(webResponse);
            }
            
            @Override
            public void printContentIfNecessary(WebResponse webResponse)
            {
                if(webResponse.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED)
                {
                    // allow 405.
                    return;
                }
                super.printContentIfNecessary(webResponse);
            }
        };
    }
}
