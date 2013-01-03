package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.List;
import java.util.Arrays;
import java.io.Serializable;
import org.apache.commons.lang.StringUtils;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.AbstractDescribableImpl;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A set of choices that can be used in Global Choice Parameter.
 * 
 * Holds a name and a list of choices.
 * Added in the System Configuration page.
 */
public class GlobalTextareaChoiceListEntry extends AbstractDescribableImpl<GlobalTextareaChoiceListEntry> implements Serializable
{
    private static final long serialVersionUID = 1L;
    
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
        // TODO: Form validation
        
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
     * 
     * @return Joined choices.
     */
    public String getChoiceListText()
    {
        return StringUtils.join(choiceList, "\n");
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
        this.name = name;
        this.choiceList = Arrays.asList(choiceListText.split("\\r?\\n"));
    }
}
