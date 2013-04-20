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

import java.util.List;

import hudson.Extension;
import hudson.Util;
import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue.QueueDecisionHandler;
import hudson.model.Queue.Task;
import hudson.model.StringParameterValue;

/**
 * Listen for what value is specified with ExtensibleChoiceParameter.
 * 
 * Registered only for watching enqueued jobs, not for decide whether a job should be enqueued.
 * 
 */
@Extension
public class ChoiceWatchQueueDecisionHandler extends QueueDecisionHandler
{
    
    /**
     * Notify ChoiceListProvider of the value selected.
     * 
     * Called when Jenkins decides a build is queued.
     * 
     * @return always true (can be queued)
     * @see hudson.model.Queue.QueueDecisionHandler#shouldSchedule(hudson.model.Queue.Task, java.util.List)
     */
    @Override
    public boolean shouldSchedule(Task p, List<Action> actions)
    {
        onQueueing(p, actions);
        return true;
    }
    
    /**
     * Notify ChoiceListProvider of the value selected.
     */
    protected void onQueueing(Task p, List<Action> actions)
    {
        if(!(p instanceof AbstractProject<?, ?>))
        {
            return;
        }
        
        AbstractProject<?, ?> job = (AbstractProject<?, ?>)p;
        ParametersDefinitionProperty pp = job.getProperty(ParametersDefinitionProperty.class);
        if(pp == null)
        {
            return;
        }
        
        // do following tests for all parameters.
        // * it is a string parameter?
        // * its parameter definition is a Extensible Choice Parameter ?
        // If passed the tests, notify the ChoiceListProvider of the value used with build.
        for(ParametersAction action: Util.filter(actions, ParametersAction.class))
        {
            for(StringParameterValue value: Util.filter(action.getParameters(), StringParameterValue.class))
            {
                ParameterDefinition def = pp.getParameterDefinition(value.getName());
                if(def == null || !(def instanceof ExtensibleChoiceParameterDefinition))
                {
                    continue;
                }
                ExtensibleChoiceParameterDefinition choiceDef = (ExtensibleChoiceParameterDefinition)def;
                if(choiceDef.getChoiceListProvider() != null)
                {
                    choiceDef.getChoiceListProvider().onBuildTriggeredWithValue(job, value);
                }
            }
        }
    }
}
