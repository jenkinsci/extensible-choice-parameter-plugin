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
import hudson.ExtensionPoint;
import hudson.DescriptorExtensionList;
import hudson.model.Hudson;
import hudson.model.Descriptor;
import hudson.model.AbstractDescribableImpl;

/**
 * The abstract base class of modules provides choices.
 * 
 * Create a new choice provider in following steps:
 * <ol>
 *    <li>Define a new class derived from ChoiceListProvider</li>
 *    <li>Override getChoiceList(), which returns the choices.</li>
 *    <li>Define the internal public static class named DescriptorImpl, derived from Descriptor&lt;ChoiceListProvider&gt;</li>
 *    <li>annotate the DescriptorImpl with Extension</li>
 * </ol>
 */
abstract public class ChoiceListProvider extends AbstractDescribableImpl<ChoiceListProvider> implements ExtensionPoint
{
    /**
     * Returns the choices.
     * 
     * @return the choices list.
     */
    abstract public List<String> getChoiceList();
    
    /**
     * Returns the default choice value.
     * 
     * null indicates the first one is the default value.
     * 
     * @return the default choice value.
     */
    public String getDefaultChoice()
    {
        return null;
    }
    
    /**
     * Returns all the ChoiceListProvider subclass whose DescriptorImpl is annotated with Extension.
     * @return DescriptorExtensionList of ChoiceListProvider subclasses.
     */
    static public DescriptorExtensionList<ChoiceListProvider,Descriptor<ChoiceListProvider>> all()
    {
        return Hudson.getInstance().<ChoiceListProvider,Descriptor<ChoiceListProvider>>getDescriptorList(ChoiceListProvider.class);
    }
}
