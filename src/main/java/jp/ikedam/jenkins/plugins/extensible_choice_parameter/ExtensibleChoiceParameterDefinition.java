package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.List;

import hudson.Extension;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.model.SimpleParameterDefinition;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;

/**
 * Provides a choice parameter whose choices can be extended using Extension Points.
 *
 */
public class ExtensibleChoiceParameterDefinition extends SimpleParameterDefinition
{
    private static final long serialVersionUID = 1L;
    
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
        // TODO: Form validation.
        
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
     * @return list of choices
     */
    public List<String> getChoiceList()
    {
        return getChoiceListProvider().getChoiceList();
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
        super(name, description);
        
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
     * @see hudson.model.SimpleParameterDefinition#createValue(java.lang.String)
     */
    @Override
    public ParameterValue createValue(String value)
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
     * The first value in the choice is used.
     * @return the default value of this parameter.
     * @see hudson.model.ParameterDefinition#getDefaultParameterValue()
     */
    @Override
    public ParameterValue getDefaultParameterValue()
    {
        return new StringParameterValue(
            getName(),
            (getChoiceList().size() > 0)?getChoiceList().get(0):null,
            getDescription()
        );
    }
}
