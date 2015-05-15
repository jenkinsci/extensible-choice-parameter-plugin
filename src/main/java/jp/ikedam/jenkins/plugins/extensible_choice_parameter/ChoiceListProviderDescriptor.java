/*
 * The MIT License
 * 
 * Copyright (c) 2014 IKEDA Yasuyuki
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

import hudson.model.Descriptor;

/**
 * Descriptor for {@link ChoiceListProvider}s.
 */
public abstract class ChoiceListProviderDescriptor extends Descriptor<ChoiceListProvider>
{
    protected ChoiceListProviderDescriptor()
    {
        super();
    }
    
    /**
     * @param clazz
     */
    protected ChoiceListProviderDescriptor(Class<? extends ChoiceListProvider> clazz)
    {
        super(clazz);
    }
    
    
    /**
     * Returns whether this provider should be enabled by default (that is, just after that provider is installed).
     * 
     * Override this method and return false if you want to disable your provider
     * and want administrators enable that explicitly.
     * 
     * @return
     */
    public boolean isEnabledByDefault()
    {
        return true;
    }
    
    /**
     * Disables the global configuration page and replaces with {@link #getGlobalConfigPageForChoiceListProvider()}
     * 
     * @return false
     * @see hudson.model.Descriptor#getGlobalConfigPage()
     */
    @Override
    public String getGlobalConfigPage()
    {
        return null;
    }
    
    /**
     * @return a view page displayed in the system configuration page under the section of {@link ExtensibleChoiceParameterDefinition}
     */
    public String getGlobalConfigPageForChoiceListProvider()
    {
        for (String cand: getPossibleViewNames("global")) {
            String page = getViewPage(clazz, cand);
            // Unfortunately, Descriptor#getViewPage returns passed value
            // when that view is not found.
            // When found, path to that file is returned,
            // so I can check whether found
            // by comparing passing value and returned value.
            if (page != null && !page.equals(cand)) {
                return page;
            }
        }
        return null;
    }
}
