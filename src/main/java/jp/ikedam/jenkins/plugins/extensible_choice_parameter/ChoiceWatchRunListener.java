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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterValue;
import hudson.model.listeners.RunListener;

/**
 * Listen for what value is specified with ExtensibleChoiceParameter.
 */
@Extension
public class ChoiceWatchRunListener extends RunListener<AbstractBuild<?, ?>> {
    @Override
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "TODO needs triage")
    public void onFinalized(AbstractBuild<?, ?> build) {
        AbstractProject<?, ?> job = build.getProject();

        ParametersDefinitionProperty pp = job.getProperty(ParametersDefinitionProperty.class);
        if (pp == null) {
            return;
        }

        // do following tests for all parameters.
        // * it is a string parameter?
        // * its parameter definition is a Extensible Choice Parameter ?
        // If passed the tests, notify the ChoiceListProvider of the value used with build.
        ParametersAction action = build.getAction(ParametersAction.class);
        if (action != null) {
            for (StringParameterValue value : Util.filter(action.getParameters(), StringParameterValue.class)) {
                ParameterDefinition def = pp.getParameterDefinition(value.getName());
                if (def == null || !(def instanceof ExtensibleChoiceParameterDefinition)) {
                    continue;
                }
                ExtensibleChoiceParameterDefinition choiceDef = (ExtensibleChoiceParameterDefinition) def;
                if (choiceDef.getChoiceListProvider() != null) {
                    choiceDef
                            .getChoiceListProvider()
                            .onBuildCompletedWithValue(
                                    build, choiceDef, value.getValue().toString());
                }
            }
        }
    }
}
