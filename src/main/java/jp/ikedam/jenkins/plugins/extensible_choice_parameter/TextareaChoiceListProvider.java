package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.io.Serializable;
import java.util.List;
import java.util.Arrays;
import org.apache.commons.lang.StringUtils;

import hudson.Extension;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A choice provider whose choices are defined as a text, like the build-in choice parameter.
 */
public class TextareaChoiceListProvider extends ChoiceListProvider implements Serializable
{
    private static final long serialVersionUID = 1L;
    
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
         * the display name shown in the dropdown to select a choice provider.
         * 
         * @return display name
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName()
        {
            return Messages._TextareaChoiceListProvider_DisplayName().toString();
        }
    }
    
    private List<String> choiceList = null;
    
    /**
     * Returns the list of choices the user specified in the job configuration page.
     * 
     * @return the list of choices.
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider#getChoiceList()
     */
    @Override
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
     * @param choiceListText the text where choices are written in each line.
     */
    @DataBoundConstructor
    public TextareaChoiceListProvider(String choiceListText)
    {
        this.choiceList = Arrays.asList(choiceListText.split("\\r?\\n"));
    }
}
