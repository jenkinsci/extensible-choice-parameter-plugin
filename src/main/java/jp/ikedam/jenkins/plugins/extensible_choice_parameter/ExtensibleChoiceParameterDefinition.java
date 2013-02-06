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
import java.util.List;
import java.util.regex.Pattern;

import hudson.Extension;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.util.FormValidation;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;

/**
 * Provides a choice parameter whose choices can be extended using Extension Points.
 *
 */
public class ExtensibleChoiceParameterDefinition extends SimpleParameterDefinition
{
    private static final long serialVersionUID = 1L;
    
    private static final Pattern namePattern = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");
    
    /**
     * Returns a regular expression pattern for the acceptable parameter names.
     * 
     * Strangely, Jenkins has no limitation for the name of parameters.
     * But in many cases, it is good to be limited to the symbol name in C.
     * 
     * @return A regular expression pattern for the acceptable variable names.
     */
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
     *     </dt>
     * </dl>
     */
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor
    {
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
         * Used for showing dropdown for users to select a choice provider.
         * 
         * @return DescriptorExtensionList of ChoiceListProvider subclasses.
         */
        public DescriptorExtensionList<ChoiceListProvider,Descriptor<ChoiceListProvider>> getChoiceListProviderList()
        {
            return ChoiceListProvider.all();
        }
        
        public FormValidation doCheckName(@QueryParameter String name){
            if(StringUtils.isBlank(name))
            {
                return FormValidation.error(Messages.ExtensibleChoiceParameterDefinition_Name_empty());
            }
            
            if(!getNamePattern().matcher(name.trim()).matches()){
                return FormValidation.error(Messages.ExtensibleChoiceParameterDefinition_Name_invalid());
            }
            
            return FormValidation.ok();
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
     * Return choices available for this parameter.
     * 
     * @return list of choices. never null.
     */
    public List<String> getChoiceList()
    {
        ChoiceListProvider provider = getChoiceListProvider();
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
        if(!isEditable() && !getChoiceList().contains(value.value))
        {
            // Something strange!: Not editable and specified a value not in the choices.
            throw new IllegalArgumentException("Illegal choice: " + value.value);
        }
        return value;
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
        if(!isEditable() && !getChoiceList().contains(value))
        {
            // Something strange!: Not editable and specified a value not in the choices.
            throw new IllegalArgumentException("Illegal choice: " + value);
        }
        return new StringParameterValue(getName(), value, getDescription());
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
        String defaultChoice = (getChoiceListProvider() != null)?getChoiceListProvider().getDefaultChoice():null;
        if(defaultChoice != null)
        {
            return createValue(defaultChoice);
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
