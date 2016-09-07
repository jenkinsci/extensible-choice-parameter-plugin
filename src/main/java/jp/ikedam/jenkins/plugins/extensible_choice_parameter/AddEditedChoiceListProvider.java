/*
 * The MIT License
 * 
 * Copyright (c) 2013 IKEDA Yasuyuki
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

import org.jvnet.localizer.Localizable;

import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

/**
 * ChoiceListProvider that can add edited value.
 */
public abstract class AddEditedChoiceListProvider extends ChoiceListProvider
{
    private static final long serialVersionUID = 2844637600583498205L;
    
    /**
     * Used to Specify when to add a edited value
     */
    public enum WhenToAdd
    {
        /**
         * When a build is triggered and enqueued.
         */
        Triggered(Messages._AddEditedChoiceListProvider_WhenToAdd_Triggered()),
        /**
         * When a build is completed
         */
        Completed(Messages._AddEditedChoiceListProvider_WhenToAdd_Completed(), Result.SUCCESS, Result.UNSTABLE, Result.FAILURE),
        /**
         * When a build is completed successfully
         */
        CompletedStable(Messages._AddEditedChoiceListProvider_WhenToAdd_CompletedStable(), Result.SUCCESS),
        /**
         * When a build is completed successfully, including unstable.
         */
        CompletedUnstable(Messages._AddEditedChoiceListProvider_WhenToAdd_CompletedUnstable(), Result.SUCCESS, Result.UNSTABLE);
        
        private Localizable name;
        private Result[] results;
        
        private WhenToAdd(Localizable name, Result ... results)
        {
            this.name = name;
            this.results = results;
        }
        
        @Override
        public String toString()
        {
            return name.toString();
        }
        
        public boolean contains(Result result)
        {
            for(Result r: this.results)
            {
                if(r == result)
                {
                    return true;
                }
            }
            return false;
        }
    }
    
    private WhenToAdd whenToAdd = null;
    
    /**
     * Returns when to add a edited value.
     * 
     * null stands for no adding.
     * 
     * @return when to add a edited value
     */
    public WhenToAdd getWhenToAdd()
    {
        return whenToAdd;
    }
    
    private boolean addToTop;

    /**
     * Returns if user-added values should be added to the top of the choice list.
     * 
     * @return if user-added values should be added to the top of the choice list.
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider#isAddToTop()
     */
    public boolean isAddToTop()
    {
        return addToTop;
    }
    
    /**
     * Returns whether to add edited value.
     * 
     * @return whether to add edited value
     */
    public boolean isAddEditedValue()
    {
        return (getWhenToAdd() != null);
    }
    
    /**
     * Constructor
     * 
     * @param addEditedValue decide if edited value should be added.
     * @param whenToAdd when to add a edited value.
     */
    public AddEditedChoiceListProvider(boolean addEditedValue, WhenToAdd whenToAdd)
    {
        this(addEditedValue, whenToAdd, false);
    }

    /**
     * Constructor
     * 
     * @param whenToAdd when to add a edited value.
     * @param whenToAdd when to add a edited value.
     * @param whenToAdd when to add a edited value.
     */
    public AddEditedChoiceListProvider(boolean addEditedValue, WhenToAdd whenToAdd, boolean addToTop)
    {
        this.whenToAdd = addEditedValue ? whenToAdd : null;
        this.addToTop = addToTop;
    }
    
    /**
     * Called when a build is completed
     * 
     * Call addEditedValue() if needed
     * 
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider#onBuildCompletedWithValue(hudson.model.AbstractBuild, jp.ikedam.jenkins.plugins.extensible_choice_parameter.ExtensibleChoiceParameterDefinition, java.lang.String)
     */
    @Override
    public void onBuildCompletedWithValue(
            AbstractBuild<?, ?> build,
            ExtensibleChoiceParameterDefinition def,
            String value)
    {
        if(getWhenToAdd() == null || !getWhenToAdd().contains(build.getResult()))
        {
            return;
        }
        
        if(getChoiceList().contains(value))
        {
            // not a edited value
            return;
        }
        AbstractProject<?,?> project = build.getProject();
        addEditedValue(
                project,
                def,
                value
        );
    }
    
    /**
     * Called when a build is triggered
     * 
     * Call addEditedValue() if needed
     * 
     * @see jp.ikedam.jenkins.plugins.extensible_choice_parameter.ChoiceListProvider#onBuildTriggeredWithValue(hudson.model.AbstractProject, jp.ikedam.jenkins.plugins.extensible_choice_parameter.ExtensibleChoiceParameterDefinition, java.lang.String)
     */
    @Override
    public void onBuildTriggeredWithValue(
            AbstractProject<?, ?> job,
            ExtensibleChoiceParameterDefinition def,
            String value)
    {
        if(getWhenToAdd() != WhenToAdd.Triggered)
        {
            return;
        }
        
        if(getChoiceList().contains(value))
        {
            // not a edited value
            return;
        }
        addEditedValue(
                job,
                def,
                value
        );
    }
    
    /**
     * Called to add a edited value to the choice list.
     * 
     * @param project
     * @param def
     * @param value
     */
    abstract protected void addEditedValue(
            AbstractProject<?, ?> project,
            ExtensibleChoiceParameterDefinition def,
            String value
    );
}
