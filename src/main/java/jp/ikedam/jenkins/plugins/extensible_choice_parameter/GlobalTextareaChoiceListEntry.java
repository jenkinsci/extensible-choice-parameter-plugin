package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.List;
import java.util.regex.Pattern;
import java.io.Serializable;

import jp.ikedam.jenkins.plugins.extensible_choice_parameter.utility.TextareaStringListUtility;

import org.apache.commons.lang.StringUtils;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.AbstractDescribableImpl;
import hudson.util.FormValidation;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * A set of choices that can be used in Global Choice Parameter.
 * 
 * Holds a name and a list of choices.
 * Added in the System Configuration page.
 */
public class GlobalTextareaChoiceListEntry extends AbstractDescribableImpl<GlobalTextareaChoiceListEntry> implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private static final Pattern namePattern = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");
    
    /**
     * @return A regular expression pattern for the acceptable names.
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
     *         <dd>
     *         shown as a part of the system configuration page.
     *         Called from global.jelly of GlobalTextareaChoiceListProvider.
     *         </dd>
     *     </dt>
     * </dl>
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<GlobalTextareaChoiceListEntry>
    {
        /**
         * Don't care for this is not shown in any page.
         * 
         * @return the display name of this object.
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName()
        {
            return "GlobalTextareaChoiceListEntry";
        }
        
        /**
         * Check the input name is acceptable.
         * <ul>
         *      <li>Must not be empty.</li>
         * </ul>
         * 
         * @param name
         * @return FormValidation object.
         */
        public FormValidation doCheckName(@QueryParameter String name)
        {
            if(StringUtils.isBlank(name))
            {
                return FormValidation.error(Messages.GlobalTextareaChoiceListEntry_Name_empty());
            }
            
            if(!getNamePattern().matcher(name.trim()).matches()){
                return FormValidation.error(Messages.GlobalTextareaChoiceListEntry_Name_invalid());
            }
            
            return FormValidation.ok();
        }
    }
    
    private String name = null;
    
    /**
     * the name of this set of choices.
     * 
     * @return the name the user specified.
     */
    public String getName()
    {
        return name;
    }
    
    private List<String> choiceList = null;
    
    /**
     * The list of choices.
     * 
     * @return the list of choices the user specified.
     */
    public List<String> getChoiceList()
    {
        return choiceList;
    }
    
    /**
     * The list of choices, joined into a string.
     * 
     * Used for filling a field when the configuration page is shown.
     * The newline code is appended also after the last element.
     * 
     * @return Joined choices.
     */
    public String getChoiceListText()
    {
        return TextareaStringListUtility.textareaFromStringList(getChoiceList());
    }
    
    /**
     * Constructor instantiating with parameters in the configuration page.
     * 
     * When instantiating from the saved configuration,
     * the object is directly serialized with XStream,
     * and no constructor is used.
     * 
     * @param name the name of this set of choices.
     * @param choiceListText the text where choices are written in each line.
     */
    @DataBoundConstructor
    public GlobalTextareaChoiceListEntry(String name, String choiceListText)
    {
        this.name = (name != null)?name.trim():"";
        this.choiceList = TextareaStringListUtility.stringListFromTextarea(choiceListText);
    }
    
    /**
     * Returns whether this object is configured correctly.
     * 
     * Jenkins framework seems to accept the values that doCheckXXX alerts an error.
     * Throwing a exception from the constructor annotated with DataBoundConstructor
     * results in an exception page, so I decided to omit bad objects with this method. 
     * 
     * @return whether this object is configured correctly.
     */
    public boolean isValid()
    {
        DescriptorImpl descriptor = ((DescriptorImpl)getDescriptor());
        {
            FormValidation v = descriptor.doCheckName(name);
            if(v.kind == FormValidation.Kind.ERROR)
            {
                return false;
            }
        }
        return true;
    }
}
