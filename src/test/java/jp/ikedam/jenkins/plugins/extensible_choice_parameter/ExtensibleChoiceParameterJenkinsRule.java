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
import hudson.model.FreeStyleProject;

import java.io.IOException;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestEnvironment;
import org.jvnet.hudson.test.TestPluginManager;

/**
 * Utility class for Tests.
 * For JenkinsRules have its may tests methods protected in Jenkins < 1.479,
 * make these methods public.
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
    
    @Override
    public FreeStyleProject createFreeStyleProject() throws IOException
    {
        return super.createFreeStyleProject();
    }
    
    @Override
    public FreeStyleProject createFreeStyleProject(String name)
            throws IOException
    {
        return super.createFreeStyleProject(name);
    }
}
