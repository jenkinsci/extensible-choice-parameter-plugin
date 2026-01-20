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

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import groovy.lang.Binding;
import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Job;
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
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.verb.POST;

/**
 * A choice provider whose choices are determined by a Groovy script.
 */
public class SystemGroovyChoiceListProvider extends ChoiceListProvider {
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
    public static class DescriptorImpl extends Descriptor<ChoiceListProvider> {
        /**
         * Create a new instance of {@link SystemGroovyChoiceListProvider} from user inputs.
         *
         * @param req
         * @param formData
         * @return
         * @throws hudson.model.Descriptor.FormException
         * @see hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest2, net.sf.json.JSONObject)
         */
        @Override
        public SystemGroovyChoiceListProvider newInstance(StaplerRequest2 req, JSONObject formData)
                throws hudson.model.Descriptor.FormException {
            SystemGroovyChoiceListProvider provider = (SystemGroovyChoiceListProvider) super.newInstance(req, formData);
            if (provider.isUsePredefinedVariables()) {
                // set project only when variables is requested.
                provider.setProject(req.findAncestorObject(Job.class));
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
        public String getDisplayName() {
            return Messages._SystemGroovyChoiceListProvider_DisplayName().toString();
        }

        /**
         * Returns the selection of a default choice.
         *
         * @param job
         * @param script
         * @param sandbox
         * @param usePredefinedVariables
         * @return the selection of a default choice
         */
        @POST
        public ListBoxModel doFillDefaultChoiceItems(
                @AncestorInPath Job<?, ?> job,
                @RelativePath("groovyScript") @QueryParameter String script,
                @RelativePath("groovyScript") @QueryParameter boolean sandbox,
                @QueryParameter boolean usePredefinedVariables) {
            ListBoxModel ret = new ListBoxModel();
            ret.add(Messages.ExtensibleChoiceParameterDefinition_NoDefaultChoice(), NoDefaultChoice);

            if (job == null) {
                // You cannot evaluate scripts without permission checks
                return ret;
            }
            job.checkPermission(Item.CONFIGURE);

            if (!sandbox) {
                // You cannot evaluate scripts outside sandbox before configuring.
                return ret;
            }

            List<String> choices = null;
            Job<?, ?> project = null;

            if (usePredefinedVariables) {
                project = job;
            }

            try {
                choices = runScript(
                        new SecureGroovyScript(script, sandbox, null).configuringWithNonKeyItem(),
                        usePredefinedVariables,
                        project);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to execute script", e);
            }

            if (choices != null) {
                for (String choice : choices) {
                    ret.add(choice);
                }
            }

            return ret;
        }

        /**
         * @return the special value used for "No default choice" (use the top most)
         */
        public String getNoDefaultChoice() {
            return NoDefaultChoice;
        }

        @POST
        public FormValidation doTest(
                @AncestorInPath Job<?, ?> job,
                // Define same as `doFillDefaultChoiceItems`
                // though @RelativePath isn't actually necessary here.
                @RelativePath("groovyScript") @QueryParameter String script,
                @RelativePath("groovyScript") @QueryParameter boolean sandbox,
                @QueryParameter boolean usePredefinedVariables) {
            List<String> choices = null;
            Job<?, ?> project = null;

            if (job == null) {
                // You cannot evaluate scripts without permission checks
                return FormValidation.warning("You cannot evaluate scripts outside project configurations");
            }
            job.checkPermission(Item.CONFIGURE);

            if (!sandbox) {
                // You cannot evaluate scripts outside sandbox before configuring.
                return FormValidation.warning(
                        Messages.SystemGroovyChoiceListProvider_groovyScript_TestableOnlyWithSandbox());
            }

            if (usePredefinedVariables) {
                project = job;
            }

            try {
                choices = runScript(
                        new SecureGroovyScript(script, sandbox, Collections.<ClasspathEntry>emptyList())
                                .configuringWithNonKeyItem(),
                        usePredefinedVariables,
                        project);
            } catch (Exception e) {
                return FormValidation.error(e, "Failed to execute script");
            }

            if (choices == null) {
                return FormValidation.error("Script returned null.");
            }

            return FormValidation.ok(String.join("\n", choices));
        }
    }

    /**
     * Returns the list of choices the user specified in the job configuration page.
     *
     * @return the list of choices.
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider#getChoiceList()
     */
    @Override
    public List<String> getChoiceList() {
        List<String> ret = null;
        Job<?, ?> project = getProject();
        if (isUsePredefinedVariables() && project == null) {
            // try to retrieve from current request.
            if (Stapler.getCurrentRequest2() != null) {
                project = Stapler.getCurrentRequest2().findAncestorObject(Job.class);
            }
        }

        try {
            ret = runScript(getGroovyScript(), isUsePredefinedVariables(), project);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to execute script", e);
        }
        return (ret != null) ? ret : new ArrayList<String>(0);
    }

    private static List<String> runScript(
            SecureGroovyScript groovyScript, boolean usePredefinedVariables, Job<?, ?> project) throws Exception {
        // see RemotingDiagnostics.Script
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins instance is unavailable.");
        }
        ClassLoader cl = jenkins.getPluginManager().uberClassLoader;

        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }

        Binding binding = new Binding();
        if (usePredefinedVariables) {
            if (project != null && project.hasPermission(Item.READ)) {
                binding.setVariable("project", project);
            } else {
                binding.setVariable("project", null);
            }
        }

        Object out = groovyScript.evaluate(cl, binding);
        if (out == null) {
            return null;
        }

        if (!(out instanceof List<?>)) {
            throw new IllegalArgumentException("Return type of the Groovy script must be List<String>");
        }

        List<String> ret = new ArrayList<String>();
        for (Object obj : (List<?>) out) {
            if (obj != null) {
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
    public String getScriptText() {
        return getGroovyScript().getScript();
    }

    private final SecureGroovyScript groovyScript;

    /**
     * @return script to generate choices.
     * @since 1.4.0
     */
    public SecureGroovyScript getGroovyScript() {
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
    public String getDefaultChoice() {
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
    public SystemGroovyChoiceListProvider(
            SecureGroovyScript groovyScript, String defaultChoice, boolean usePredefinedVariables) {
        this.groovyScript = groovyScript.configuringWithNonKeyItem();
        this.defaultChoice = (!NoDefaultChoice.equals(defaultChoice)) ? defaultChoice : null;
        this.usePredefinedVariables = usePredefinedVariables;
    }

    public SystemGroovyChoiceListProvider(String scriptText, String defaultChoice, boolean usePredefinedVariables) {
        this(newSecureGroovyScript(scriptText), defaultChoice, usePredefinedVariables);
    }

    private static SecureGroovyScript newSecureGroovyScript(String scriptText) {
        try {
            return new SecureGroovyScript(scriptText, true, Collections.emptyList());
        } catch (Descriptor.FormException e) {
            throw new RuntimeException(e);
        }
    }

    public SystemGroovyChoiceListProvider(String scriptText, String defaultChoice) {
        this(scriptText, defaultChoice, false);
    }

    private Object readResolve() {
        if (groovyScript != null) {
            return this;
        }
        // < 1.4.0
        return new SystemGroovyChoiceListProvider(scriptText, getDefaultChoice(), isUsePredefinedVariables());
    }

    private final boolean usePredefinedVariables;

    /**
     * @return whether to use predefined variables
     */
    public boolean isUsePredefinedVariables() {
        return usePredefinedVariables;
    }

    /**
     * The project of this is configured in.
     * This will be stored in job configuration XML like
     * &lt;project class=&quot;project&quot; reference=&quot;../../../../../..&quot; /&gt;
     */
    private transient Job<?, ?> project;

    /**
     * @param project
     */
    protected void setProject(Job<?, ?> project) {
        this.project = project;
    }

    @Deprecated
    protected void setProject(AbstractProject<?, ?> project) {
        setProject((Job<?, ?>) project);
    }

    /**
     * Return the project where this is configured.
     * This is set only when {@link SystemGroovyChoiceListProvider#isUsePredefinedVariables()} is true.
     *
     * @return project
     */
    protected Job<?, ?> getProject() {
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
            SystemGroovyChoiceListProvider src = (SystemGroovyChoiceListProvider) source;
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

        @Override
        protected void callback(SystemGroovyChoiceListProvider obj, UnmarshallingContext context) {
            // nothing to do
        }
    }
}
