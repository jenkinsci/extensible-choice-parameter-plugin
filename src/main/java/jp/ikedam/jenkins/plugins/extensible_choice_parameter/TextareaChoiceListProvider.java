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

import java.io.Serializable;
import java.util.List;

import jp.ikedam.jenkins.plugins.extensible_choice_parameter.utility.TextareaStringListUtility;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * A choice provider whose choices are defined as a text, like the build-in choice parameter.
 */
public class TextareaChoiceListProvider extends ChoiceListProvider implements Serializable
{
    private static final long serialVersionUID = 2L;
    private static final String NoDefaultChoice = "###NODEFAULTCHOICE###";
    
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
        
        /**
         * Returns the selection of a default choice.
         * 
         * @param choiceListText
         * @return the selection of a default choice
         */
        public ListBoxModel doFillDefaultChoiceItems(@QueryParameter String choiceListText)
        {
            ListBoxModel ret = new ListBoxModel();
            ret.add(Messages.ExtensibleChoiceParameterDefinition_NoDefaultChoice(), NoDefaultChoice);
            
            List<String> choices = TextareaStringListUtility.stringListFromTextarea(choiceListText);
            for(String choice: choices)
            {
                ret.add(choice);
            }
            return ret;
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
        return TextareaStringListUtility.textareaFromStringList(getChoiceList());
    }
    
    private String defaultChoice = null;
    
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
     * When instantiating from the saved configuration,
     * the object is directly serialized with XStream,
     * and no constructor is used.
     * 
     * @param choiceListText the text where choices are written in each line.
     */
    @DataBoundConstructor
    public TextareaChoiceListProvider(String choiceListText, String defaultChoice)
    {
        this.choiceList = TextareaStringListUtility.stringListFromTextarea(choiceListText);
        this.defaultChoice = (!NoDefaultChoice.equals(defaultChoice))?defaultChoice:null;
    }
}
