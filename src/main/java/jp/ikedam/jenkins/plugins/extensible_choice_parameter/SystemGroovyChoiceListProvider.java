/*
 * The MIT License
 * 
 * Copyright (c) 2013 Michael Rumpf
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

import groovy.lang.Binding;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.XStream2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * A choice provider whose choices are determined by a Groovy script.
 */
public class SystemGroovyChoiceListProvider extends ChoiceListProvider
{
    private static final long serialVersionUID = 3L;
    private static final String NoDefaultChoice = "###NODEFAULTCHOICE###";
    private static final Logger LOGGER = Logger.getLogger(SystemGroovyChoiceListProvider.class.getName());
    
    /**
     * The internal class to work with views.
     * 
     * The following files are used (put in main/resource directory in the source tree).
     * <dl>
     *     <dt>config.jelly</dt>
     *         <dd>
     *             Shown as a part of a job configuration page when this provider is selected.
     *             Provides additional configuration fields of a Extensible Choice.
     *         </dd>
     * </dl>
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<ChoiceListProvider>
    {
        /**
         * Create a new instance of {@link SystemGroovyChoiceListProvider} from user inputs.
         * 
         * @param req
         * @param formData
         * @return
         * @throws hudson.model.Descriptor.FormException
         * @see hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
         */
        @Override
        public SystemGroovyChoiceListProvider newInstance(StaplerRequest req, JSONObject formData)
                throws hudson.model.Descriptor.FormException
        {
            SystemGroovyChoiceListProvider provider = (SystemGroovyChoiceListProvider)super.newInstance(req, formData);
            if(provider.isUsePredefinedVariables())
            {
                // set project only when variables is requested.
                provider.setProject(req.findAncestorObject(AbstractProject.class));
            }
            return provider;
        }
        
        /**
         * the display name shown in the dropdown to select a choice provider.
         * 
         * @return display name
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName()
        {
            return Messages._SystemGroovyChoiceListProvider_DisplayName().toString();
        }
        
        /**
         * Returns the selection of a default choice.
         * 
         * @param req null passed in tests.
         * @param scriptText
         * @return the selection of a default choice
         */
        public ListBoxModel doFillDefaultChoiceItems(StaplerRequest req, @QueryParameter String script, @QueryParameter boolean sandbox, @QueryParameter boolean usePredefinedVariables)
        {
            ListBoxModel ret = new ListBoxModel();
            ret.add(Messages.ExtensibleChoiceParameterDefinition_NoDefaultChoice(), NoDefaultChoice);
            
            if (!sandbox)
            {
                // You cannot evaluate scripts outside sandbox before configuring.
                return ret;
            }
            
            List<String> choices = null;
            AbstractProject<?,?> project = null;
            
            if(usePredefinedVariables && req != null)
            {
                project = req.findAncestorObject(AbstractProject.class);
            }
            
            try
            {
                choices = runScript(
                    new SecureGroovyScript(script, sandbox, Collections.<ClasspathEntry>emptyList()).configuringWithNonKeyItem(),
                    usePredefinedVariables,
                    project
                );
            }
            catch(Exception e)
            {
                LOGGER.log(Level.WARNING, "Failed to execute script", e);
            }
            
            if(choices != null)
            {
                for(String choice: choices)
                {
                    ret.add(choice);
                }
            }
            
            return ret;
        }
        
        public FormValidation doTest(StaplerRequest req, @QueryParameter String script, @QueryParameter boolean sandbox, @QueryParameter boolean usePredefinedVariables)
        {
            List<String> choices = null;
            AbstractProject<?,?> project = null;
            
            if (!sandbox)
            {
                // You cannot evaluate scripts outside sandbox before configuring.
                return FormValidation.warning(Messages.SystemGroovyChoiceListProvider_groovyScript_TestableOnlyWithSandbox());
            }
            
            if(usePredefinedVariables && req != null)
            {
                project = req.findAncestorObject(AbstractProject.class);
            }
            
            try
            {
                choices = runScript(
                    new SecureGroovyScript(script, sandbox, Collections.<ClasspathEntry>emptyList()).configuringWithNonKeyItem(),
                    usePredefinedVariables,
                    project
                );
            }
            catch(Exception e)
            {
                return FormValidation.error(e, "Failed to execute script");
            }
            
            if(choices == null)
            {
                return FormValidation.error("Script returned null.");
            }
            
            return FormValidation.ok(StringUtils.join(choices, '\n'));
        }
    }
    
    /**
     * Returns the list of choices the user specified in the job configuration page.
     * 
     * @return the list of choices.
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider#getChoiceList()
     */
    @Override
    public List<String> getChoiceList()
    {
        List<String> ret = null;
        AbstractProject<?,?> project = getProject();
        if(isUsePredefinedVariables() && project == null)
        {
            // try to retrieve from current request.
            if(Stapler.getCurrentRequest() != null)
            {
                project = Stapler.getCurrentRequest().findAncestorObject(AbstractProject.class);
            }
        }
        
        try
        {
            ret = runScript(getGroovyScript(), isUsePredefinedVariables(), project);
        }
        catch(Exception e)
        {
            LOGGER.log(Level.WARNING, "Failed to execute script", e);
        }
        return (ret != null)?ret:new ArrayList<String>(0);
    }

    private static List<String> runScript(SecureGroovyScript groovyScript, boolean usePredefinedVariables, AbstractProject<?,?> project) throws Exception {
        // see RemotingDiagnostics.Script
        ClassLoader cl = Jenkins.getInstance().getPluginManager().uberClassLoader;

        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }

        Binding binding = new Binding();
        if(usePredefinedVariables)
        {
            binding.setVariable("jenkins", Jenkins.getInstance());
            binding.setVariable("project", project);
        }

        Object out = groovyScript.evaluate(cl,  binding);
        if(out == null)
        {
            return null;
        }
        
        if  (!(out instanceof List<?>)) {
            throw new IllegalArgumentException("Return type of the Groovy script mus be List<String>");
        }
        
        List<String> ret = new ArrayList<String>();
        for (Object obj : (List<?>) out) {
            if(obj != null) {
                ret.add(obj.toString());
            }
        }
        return ret;
    }
    private transient String scriptText;

    /**
     * @deprecated use {@link #getGroovyScript()}
     */
    @Deprecated
    public String getScriptText()
    {
        return getGroovyScript().getScript();
    }
    
    private final SecureGroovyScript groovyScript;
    
    /**
     * @return script to generate choices.
     * @since 1.4.0
     */
    public SecureGroovyScript getGroovyScript()
    {
        return groovyScript;
    }
    
    private final String defaultChoice;
    
    /**
     * Returns the default choice.
     * 
     * @return the default choice
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider#getDefaultChoice()
     */
    @Override
    public String getDefaultChoice()
    {
        return defaultChoice;
    }
    
    /**
     * Constructor instantiating with parameters in the configuration page.
     * 
     * @param groovyScript
     * @param defaultChoice
     * @param usePredefinedVariables
     * 
     * @since 1.4.0
     */
    @DataBoundConstructor
    public SystemGroovyChoiceListProvider(SecureGroovyScript groovyScript, String defaultChoice, boolean usePredefinedVariables)
    {
        this.groovyScript = groovyScript.configuringWithNonKeyItem();
        this.defaultChoice = (!NoDefaultChoice.equals(defaultChoice))?defaultChoice:null;
        this.usePredefinedVariables = usePredefinedVariables;
    }
    
    public SystemGroovyChoiceListProvider(String scriptText, String defaultChoice, boolean usePredefinedVariables)
    {
        this(
            new SecureGroovyScript(scriptText, true, Collections.<ClasspathEntry>emptyList()),
            defaultChoice,
            usePredefinedVariables
        );
    }
    
    public SystemGroovyChoiceListProvider(String scriptText, String defaultChoice)
    {
        this(scriptText, defaultChoice, false);
    }
    
    private Object readResolve() {
        if (groovyScript != null)
        {
            return this;
        }
        // < 1.4.0
        return new SystemGroovyChoiceListProvider(
            scriptText,
            getDefaultChoice(),
            isUsePredefinedVariables()
        );
    }
    
    private final boolean usePredefinedVariables;
    
    /**
     * @return whether to use predefined variables
     */
    public boolean isUsePredefinedVariables()
    {
        return usePredefinedVariables;
    }
    
    /**
     * The project of this is configured in.
     * This will be stored in job configuration XML like
     * &lt;project class=&quot;project&quot; reference=&quot;../../../../../..&quot; /&gt;
     */
    private transient AbstractProject<?,?> project;
    
    /**
     * @param project
     */
    protected void setProject(AbstractProject<?,?> project)
    {
        this.project = project;
    }
    
    /**
     * Return the project where this is configured.
     * This is set only when {@link SystemGroovyChoiceListProvider#isUsePredefinedVariables()} is true.
     * 
     * @return project
     */
    protected AbstractProject<?,?> getProject()
    {
        return project;
    }
    
    public static class ConverterImpl extends XStream2.PassthruConverter<SystemGroovyChoiceListProvider> {
        private final Mapper mapper;
        
        public ConverterImpl(XStream2 xstream) {
            super(xstream);
            mapper = xstream.getMapper();
        }
        
        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            super.marshal(source, writer, context);
            
            // As project is transient, serialize it forcibly.
            SystemGroovyChoiceListProvider src = (SystemGroovyChoiceListProvider)source;
            if (src.project != null) {
                writer.startNode("project");
                String attributeName = mapper.aliasForSystemAttribute("class");
                if (attributeName != null) {
                    writer.addAttribute(attributeName, mapper.serializedClass(src.project.getClass()));
                }
                context.convertAnother(src.project);
                writer.endNode();
            }
        }
        @Override protected void callback(SystemGroovyChoiceListProvider obj, UnmarshallingContext context) {
            // nothing to do
        }
    }
}
