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

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.util.ListBoxModel;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

/**
 * A choice provider whose choices are defined
 * in the System Configuration page, and can be refereed from all jobs.
 */
public class GlobalTextareaChoiceListProvider extends AddEditedChoiceListProvider
{
    private static final long serialVersionUID = 2L;
    private static final String NoDefaultChoice = "###NODEFAULTCHOICE###";
    private static final Logger LOGGER = Logger.getLogger(GlobalTextareaChoiceListProvider.class.getName());
    
    /**
     * The internal class to work with views.
     * Also manage the global configuration.
     * 
     * The following files are used (put in main/resource directory in the source tree).
     * <dl>
     *     <dt>config.jelly</dt>
     *         <dd>
     *             Shown as a part of a job configuration page when this provider is selected.
     *             Provides additional configuration fields of a Extensible Choice.
     *         </dd>
     *     <dt>global.jelly</dt>
     *         <dd>
     *              Shown as a part of the System Configuration page.
     *              Call config.jelly of GlobalTextareaChoiceListEntry,
     *              for each set of choices.
     *         </dd>
     * </dl>
     */
    @Extension
    public static class DescriptorImpl extends ChoiceListProviderDescriptor
    {
        /**
         * Restore from the global configuration
         */
        public DescriptorImpl()
        {
            load();
        }
        
        private List<GlobalTextareaChoiceListEntry> choiceListEntryList;
        
        /**
         * The list of available sets of choices.
         * 
         * @return the list of GlobalTextareaChoiceListEntry
         */
        public List<GlobalTextareaChoiceListEntry> getChoiceListEntryList()
        {
            return choiceListEntryList;
        }
        
        /**
         * Set a list of available sets of choices.
         * 
         * @param choiceListEntryList a list of GlobalTextareaChoiceListEntry
         */
        @SuppressWarnings("unchecked") // for the cast to List<GlobalTextareaChoiceListEntry>
        public void setChoiceListEntryList(List<GlobalTextareaChoiceListEntry> choiceListEntryList){
            // Invalid values may be submitted.
            // (Jenkins framework seems not to forbid the submission,
            // even if form validations alert errors...)
            // retrieve only valid (correctly configured) entries
            this.choiceListEntryList = (choiceListEntryList == null)?new ArrayList<GlobalTextareaChoiceListEntry>(0):
                (List<GlobalTextareaChoiceListEntry>)CollectionUtils.select(
                        choiceListEntryList,
                        new Predicate()
                        {
                            @Override
                            public boolean evaluate(Object entry)
                            {
                                return ((GlobalTextareaChoiceListEntry)entry).isValid();
                            }
                        }
                );
        }
        
        /**
         * Returns a list of the names of the available choice set.
         * 
         * Used in dropdown field of a job configuration page.
         * 
         * @return a list of the names of the available choice set
         */
        public ListBoxModel doFillNameItems()
        {
            ListBoxModel m = new ListBoxModel();
            if(getChoiceListEntryList() != null)
            {
                for(GlobalTextareaChoiceListEntry e: getChoiceListEntryList())
                {
                    m.add(e.getName());
                }
            }
            return m;
        }
        
        public ListBoxModel doFillDefaultChoiceItems(@QueryParameter String name)
        {
            ListBoxModel ret = new ListBoxModel();
            ret.add(Messages.ExtensibleChoiceParameterDefinition_NoDefaultChoice(), NoDefaultChoice);
            
            List<String> choices = getChoiceList(name);
            
            for(String choice: choices)
            {
                ret.add(choice);
            }
            return ret;
        }
        

        /**
         * @return the special value used for "No default choice" (use the top most)
         */
        public String getNoDefaultChoice()
        {
            return NoDefaultChoice;
        }

        /**
         * Retrieve the set of choices entry by the name.
         * 
         * If multiple candidates exists, returns the first one. 
         * 
         * @param name
         * @return the set of choices.
         */
        public GlobalTextareaChoiceListEntry getChoiceListEntry(String name)
        {
            if(getChoiceListEntryList() == null)
            {
                // in case GlobalTextareaChoiceListEntry is never configured.
                return null;
            }
            
            for(GlobalTextareaChoiceListEntry e: getChoiceListEntryList())
            {
                if(e.getName().equals(name))
                {
                    return e;
                }
            }
            return null;
         }
        
        /**
         * Retrieve the set of choices entry by the name.
         * 
         * if no entry matches, return empty list.
         * 
         * @param name
         * @return the list of choices. never null.
         */
        public List<String> getChoiceList(String name)
        {
           GlobalTextareaChoiceListEntry e = getChoiceListEntry(name);
           return (e != null)?e.getChoiceList():new ArrayList<String>();
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
            return Messages._GlobalTextareaChoiceListProvider_DisplayName().toString();
        }
        
        /**
         * Store the parameters specified in the System Configuration page.
         * 
         * @param req
         * @param formData
         * @return whether succeeded to store. 
         * @throws FormException
         * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
        {
            setChoiceListEntryList(req.bindJSONToList(GlobalTextareaChoiceListEntry.class, formData.get("choiceListEntryList")));
            
            save();
            
            return super.configure(req,formData);
        }
    }
    
    private String name = null;
    
    /**
     * Returns the name of the set of choice, specified by a user.
     * 
     * @return the name of the set of choice
     */
    public String getName()
    {
        return name;
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
     * Returns the choices available as a parameter value. 
     * 
     * @return choices
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider#getChoiceList()
     */
    @Override
    public List<String> getChoiceList()
    {
        return ((DescriptorImpl)getDescriptor()).getChoiceList(getName());
    }
    
    /**
     * Constructor instantiating with parameters in the configuration page.
     * 
     * When instantiating from the saved configuration,
     * the object is directly serialized with XStream,
     * and no constructor is used.
     * 
     * @param name the name of the set of choices.
     * @param defaultChoice the initial selected value.
     * @param addEditedValue
     * @param whenToAdd
     */
    @DataBoundConstructor
    public GlobalTextareaChoiceListProvider(String name, String defaultChoice, boolean addEditedValue, WhenToAdd whenToAdd)
    {
        super(addEditedValue, whenToAdd);
        // No validation is performed, for the name is selected from the dropdown.
        this.name = name;
        this.defaultChoice = (!NoDefaultChoice.equals(defaultChoice))?defaultChoice:null;
    }
    
    /**
     * Called to add a edited value to the choice list.
     * 
     * @param project
     * @param def
     * @param value
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.AddEditedChoiceListProvider#addEditedValue(hudson.model.AbstractProject, jp.ikedam.jenkins.plugins.extensible_choice_parameter.ExtensibleChoiceParameterDefinition, java.lang.String)
     */
    @Override
    protected void addEditedValue(
            AbstractProject<?, ?> project,
            ExtensibleChoiceParameterDefinition def,
            String value
    )
    {
        DescriptorImpl descriptor = (DescriptorImpl)getDescriptor();
        GlobalTextareaChoiceListEntry entry = descriptor.getChoiceListEntry(getName());
        
        if(entry == null)
        {
            LOGGER.warning(String.format("Requested to add a new value %s to parameter %s(%s) in project %s, but the choice list does not exist.", value, def.getName(), getName(), project.getName()));
            return;
        }
        
        if(!entry.isAllowAddEditedValue())
        {
            LOGGER.warning(String.format("Requested to add a new value %s to parameter %s(%s) in project %s, but the choice list is not configured to allow that.", value, def.getName(), getName(), project.getName()));
            return;
        }
        
        LOGGER.info(String.format("Add a new value %s to parameter %s(%s) in project %s", value, def.getName(), getName(), project.getName()));
        entry.addEditedValue(value);
        
        try
        {
            descriptor.save();
        }
        catch(Exception e)
        {
            LOGGER.log(Level.WARNING, "Failed to add choice value", e);
        }
    }
}
