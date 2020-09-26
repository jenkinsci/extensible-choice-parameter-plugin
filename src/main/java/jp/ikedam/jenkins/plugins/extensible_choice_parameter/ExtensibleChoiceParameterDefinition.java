/*
 * The MIT License
 * 
 * Copyright (c) 2012-2013 IKEDA Yasuyuki
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import jenkins.model.Jenkins;
import hudson.Extension;
import hudson.DescriptorExtensionList;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Describable;
import hudson.model.DescriptorVisibilityFilter;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

import org.apache.commons.lang.StringUtils;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.export.Exported;

/**
 * Provides a choice parameter whose choices can be extended using Extension Points.
 *
 */
public class ExtensibleChoiceParameterDefinition extends SimpleParameterDefinition
{
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ExtensibleChoiceParameterDefinition.class.getName());
    
    private static final Pattern namePattern = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");
    
    /**
     * How to display choices for input values
     */
    public enum EditableType
    {
        /**
         * The input value doesn't work as filter.
         */
        NoFilter(Messages._ExtensibleChoiceParameterDefinition_EditableType_NoFilter()),
        /**
         * The input value works as filter. Only matching values are displayed.
         */
        Filter(Messages._ExtensibleChoiceParameterDefinition_EditableType_Filter());

        private final Localizable displayName;

        private EditableType(Localizable displayName)
        {
            this.displayName = displayName;
        }

        public String getDisplayName()
        {
            return displayName.toString();
        }
    }

    /**
     * Deprecated
     */
    @Deprecated
    public static Pattern getNamePattern()
    {
        return namePattern;
    }
    
    /**
     * The internal class to work with views.
     * 
     * The following files are used (put in main/resource directory in the source tree).
     * <dl>
     *     <dt>config.jelly</dt>
     *         <dd>shown as a part of a job configuration page.</dd>
     *     <dt>index.jelly</dt>
     *         <dd>shown when a user launches a build, and specifies parameters of the build.</dd>
     * </dl>
     */
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor
    {
        private Map<String,Boolean> choiceListEnabledMap;
        
        public DescriptorImpl()
        {
            setChoiceListEnabledMap(Collections.<String, Boolean>emptyMap());
            load();
        }
        
        protected void setChoiceListEnabledMap(Map<String, Boolean> choiceListEnabledMap)
        {
            this.choiceListEnabledMap = choiceListEnabledMap;
        }
        
        protected Map<String, Boolean> getChoiceListEnabledMap()
        {
            return choiceListEnabledMap;
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException
        {
            Map<String, Boolean> configuredChoiceListEnableMap = new HashMap<String, Boolean>();
            for(Descriptor<ChoiceListProvider> d: getChoiceListProviderList())
            {
                String name = d.getJsonSafeClassName();
                JSONObject o = json.optJSONObject(name);
                
                if(o != null)
                {
                    configuredChoiceListEnableMap.put(d.getId(), true);
                    if(d instanceof ChoiceListProviderDescriptor)
                    {
                        d.configure(req, o);
                    }
                }
                else
                {
                    configuredChoiceListEnableMap.put(d.getId(), false);
                }
            }
            setChoiceListEnabledMap(configuredChoiceListEnableMap);
            
            save();
            
            return super.configure(req, json);
        }
        
        public boolean isProviderEnabled(Descriptor<?> d)
        {
            Boolean b = getChoiceListEnabledMap().get(d.getId());
            if(b != null)
            {
                return b.booleanValue();
            }
            if(!(d instanceof ChoiceListProviderDescriptor))
            {
                return true;
            }
            return ((ChoiceListProviderDescriptor)d).isEnabledByDefault();
        }
        
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
        public ExtensibleChoiceParameterDefinition newInstance(StaplerRequest req,
                JSONObject formData)
                throws hudson.model.Descriptor.FormException
        {
            ExtensibleChoiceParameterDefinition def = new ExtensibleChoiceParameterDefinition(
                    formData.getString("name"),
                    bindJSONWithDescriptor(req, formData, "choiceListProvider", ChoiceListProvider.class),
                    formData.getBoolean("editable"),
                    formData.getString("description")
            );
            if (formData.containsKey("editableType")) {
                def.setEditableType(EditableType.valueOf(formData.getString("editableType")));
            }
            return def;
        }
        
        /**
         * Create a new {@link Describable} object from user inputs.
         * 
         * @param req
         * @param formData
         * @param fieldName
         * @param clazz
         * @return
         * @throws hudson.model.Descriptor.FormException
         */
        private <T extends Describable<?>> T bindJSONWithDescriptor(
                StaplerRequest req,
                JSONObject formData,
                String fieldName,
                Class<T> clazz
        ) throws hudson.model.Descriptor.FormException {
            formData = formData.getJSONObject(fieldName);
            if (formData == null || formData.isNullObject()) {
                return null;
            }
            String staplerClazzName = formData.optString("$class", null);
            if (staplerClazzName == null) {
                // Fall back on the legacy stapler-class attribute.
                staplerClazzName = formData.optString("stapler-class", null);
            }
            if (staplerClazzName == null) {
                throw new FormException("No $stapler nor stapler-class is specified", fieldName);
            }
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                throw new IllegalStateException("Jenkins instance is unavailable.");
            }
            try {
                @SuppressWarnings("unchecked")
                Class<? extends T> staplerClass = (Class<? extends T>)jenkins.getPluginManager().uberClassLoader.loadClass(staplerClazzName);
                Descriptor<?> d = jenkins.getDescriptorOrDie(staplerClass);
                
                @SuppressWarnings("unchecked")
                T instance = (T)d.newInstance(req, formData);
                
                return instance;
            } catch(ClassNotFoundException e) {
                throw new FormException(
                        String.format("Failed to instantiate %s", staplerClazzName),
                        e,
                        fieldName
                );
            }
        }
        
        /**
         * Returns the string to be shown in a job configuration page, in the dropdown of &quot;Add Parameter&quot;.
         * 
         * @return a name of this parameter type.
         * @see hudson.model.ParameterDefinition.ParameterDescriptor#getDisplayName()
         */
        @Override
        public String getDisplayName()
        {
            return Messages._ExtensibleChoiceParameterDefinition_DisplayName().toString();
        }
        
        /**
         * Returns all the available methods to provide choices.
         * 
         * @return DescriptorExtensionList of ChoiceListProvider subclasses.
         */
        public DescriptorExtensionList<ChoiceListProvider,Descriptor<ChoiceListProvider>> getChoiceListProviderList()
        {
            return ChoiceListProvider.all();
        }
        
        /**
         * Returns all the available methods to provide choices that are enabled in the global configuration.
         * 
         * Used for showing dropdown for users to select a choice provider.
         * 
         * @return DescriptorExtensionList of ChoiceListProvider subclasses.
         */
        public List<Descriptor<ChoiceListProvider>> getEnabledChoiceListProviderList()
        {
            return DescriptorVisibilityFilter.apply(this, ChoiceListProvider.all());
        }
        
        public FormValidation doCheckName(@QueryParameter String name){
            if(StringUtils.isBlank(name))
            {
                return FormValidation.error(Messages.ExtensibleChoiceParameterDefinition_Name_empty());
            }
            
            final String trimmedName = StringUtils.trim(name);
            final String EXPANDED = "GOOD";
            String expanded = Util.replaceMacro(
                String.format("${%s}", trimmedName),
                new VariableResolver<String>()
                {
                    @Override
                    public String resolve(String name)
                    {
                        if(trimmedName.equals(name))
                        {
                            return EXPANDED;
                        }
                        return null;
                    }
                }
            );
            
            if(!EXPANDED.equals(expanded)){
                return FormValidation.warning(Messages.ExtensibleChoiceParameterDefinition_Name_invalid());
            }
            
            return FormValidation.ok();
        }
    }
    
    @Extension
    public static class DescriptorVisibilityFilterImpl extends DescriptorVisibilityFilter
    {
        @SuppressWarnings("unchecked")
        @Override
        public boolean filter(Object context, @SuppressWarnings("rawtypes") Descriptor descriptor)
        {
            if(!(context instanceof DescriptorImpl))
            {
                return true;
            }
            return ((DescriptorImpl)context).isProviderEnabled(descriptor);
        }
    }
    
    private boolean editable = false;
    
    /**
     * Is this parameter value can be set to a value not in the choices?
     * 
     * @return whether this parameter is editable.
     */
    public boolean isEditable()
    {
        return editable;
    }
    
    private EditableType editableType;

    /**
     * @return How to display choices for input values
     */
    @Nonnull
    public EditableType getEditableType()
    {
        return (editableType != null) ? editableType : EditableType.NoFilter;
    }

    /**
     * @param editableType How to display choices for input values
     */
    @DataBoundSetter
    public void setEditableType(EditableType editableType)
    {
        this.editableType = editableType;
    }

    private ChoiceListProvider choiceListProvider = null;
    
    /**
     * The choice provider the user specified.
     * 
     * @return choice provider.
     */
    public ChoiceListProvider getChoiceListProvider()
    {
        return choiceListProvider;
    }
    
    /**
     * @return choice provider only when it's enabled
     * @see DescriptorImpl#isProviderEnabled(Descriptor)
     */
    public ChoiceListProvider getEnabledChoiceListProvider()
    {
        ChoiceListProvider p = getChoiceListProvider();
        if(p == null)
        {
            return null;
        }
        
        // filter providers.
        List<Descriptor<ChoiceListProvider>> testList = DescriptorVisibilityFilter.apply(
                getDescriptor(),
                Arrays.asList(p.getDescriptor())
        );
        if(testList.isEmpty())
        {
            LOGGER.log(Level.WARNING, "{0} is configured but disabled in the system configuration.", p.getDescriptor().getDisplayName());
            return null;
        }
        return p;
    }
    
    /**
     * Return choices available for this parameter.
     * 
     * @return list of choices. never null.
     */
    @Exported
    public List<String> getChoiceList()
    {
        ChoiceListProvider provider = getEnabledChoiceListProvider();
        List<String> choiceList = (provider !=  null)?provider.getChoiceList():null;
        return (choiceList !=  null)?choiceList:new ArrayList<String>(0);
    }
    
    /**
     * Constructor instantiating with parameters in the configuration page.
     * 
     * When instantiating from the saved configuration,
     * the object is directly serialized with XStream,
     * and no constructor is used.
     * 
     * @param name the name of this parameter (used as a variable name).
     * @param choiceListProvider the choice provider
     * @param editable whether this parameter can be a value not in choices.
     * @param description the description of this parameter. Used only for the convenience of users.
     */
    @DataBoundConstructor
    public ExtensibleChoiceParameterDefinition(String name, ChoiceListProvider choiceListProvider, boolean editable, String description)
    {
        // There seems no way to forbid invalid values to be submitted.
        // SimpleParameterDefinition seems not to trim name parameter, so trim here.
        super(StringUtils.trim(name), description);
        
        this.choiceListProvider = choiceListProvider;
        this.editable = editable;
    }
    
    /**
     * Test passed ParameterValue and return.
     * 
     * Common processing of createValue
     * 
     * @param value a value to test.
     * @return a value tested. same with value.
     */
    protected ParameterValue createValueCommon(StringParameterValue value)
    {
        if(!isEditable() && !getChoiceList().contains(value.getValue()))
        {
            // Something strange!: Not editable and specified a value not in the choices.
            throw new IllegalArgumentException(String.format(
                "Illegal choice '%s' in parameter '%s'",
                value.getValue(),
                value.getName()
            ));
        }
        return value;
    }
    
    /**
     * Decide a value of this parameter from the user input.
     * 
     * @param request
     * @param jo the user input
     * @return the value of this parameter.
     * @see hudson.model.ParameterDefinition#createValue(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
     */
    @Override
    public ParameterValue createValue(StaplerRequest request, JSONObject jo)
    {
        StringParameterValue value = request.bindJSON(StringParameterValue.class, jo);
        value.setDescription(getDescription());
        
        return createValueCommon(value);
    }
    
    /**
     * Decide a value of this parameter from the user input.
     * 
     * @param value the user input
     * @return the value of this parameter.
     * @throws IllegalArgumentException The value is not in choices even the field is not editable.
     * @see hudson.model.SimpleParameterDefinition#createValue(java.lang.String)
     */
    @Override
    public ParameterValue createValue(String value) throws IllegalArgumentException
    {
        return createValueCommon(new StringParameterValue(getName(), value, getDescription()));
    }
    
    /**
     * Returns the default value of this parameter.
     * 
     * If not specified by the provider, 
     * the first value in the choice is used.
     * returns null if no choice list is defined.
     * 
     * @return the default value of this parameter.
     * @see hudson.model.ParameterDefinition#getDefaultParameterValue()
     */
    @Override
    public ParameterValue getDefaultParameterValue()
    {
        ChoiceListProvider p = getEnabledChoiceListProvider();
        String defaultChoice = (p != null)?p.getDefaultChoice():null;
        if(defaultChoice != null)
        {
            try {
                return createValue(defaultChoice);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Illegal choice for the default value. Ignore and use the value in the top of the list instead.", e);
                // pass through
            }
        }
        
        List<String> choiceList = getChoiceList();
        return (choiceList.size() <= 0)?null:
            new StringParameterValue(
                    getName(),
                    choiceList.get(0),
                    getDescription()
            );
    }
}
