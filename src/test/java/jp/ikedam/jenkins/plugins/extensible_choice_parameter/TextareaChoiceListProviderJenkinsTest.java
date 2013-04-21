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

import hudson.util.ListBoxModel;

import java.util.List;

import jenkins.model.Jenkins;

import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Tests for TextareaChoiceListProvider, corresponding to Jenkins.
 */
public class TextareaChoiceListProviderJenkinsTest extends HudsonTestCase
{
    private TextareaChoiceListProvider.DescriptorImpl getDescriptor()
    {
        return (TextareaChoiceListProvider.DescriptorImpl)Jenkins.getInstance().getDescriptor(TextareaChoiceListProvider.class);
    }
    
    static private void assertListBoxEquals(String message, List<ListBoxModel.Option> expected, List<ListBoxModel.Option> test)
    {
        assertEquals(message, expected.size(), test.size());
        for(int i = 0; i < test.size(); ++i)
        {
            assertEquals(String.format("%s: %d-th name", message, i), expected.get(i).name, test.get(i).name);
            assertEquals(String.format("%s: %d-th value", message, i), expected.get(i).value, test.get(i).value);
        }
    }
    
    public void testDoFillDefaultChoiceItems()
    {
        TextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        
        // Easy case
        {
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems("value1\nvalue2");
            
            ListBoxModel expected = new ListBoxModel();
            expected.add(new ListBoxModel.Option("value1", "value1"));
            expected.add(new ListBoxModel.Option("value2", "value2"));
            
            assertListBoxEquals("Easy case", expected, fillList.subList(1, fillList.size()));
        }
        
        // Empty
        {
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems("");
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("Empty", expected, fillList.subList(1, fillList.size()));
        }
        
        // null
        {
            ListBoxModel fillList = descriptor.doFillDefaultChoiceItems(null);
            
            ListBoxModel expected = new ListBoxModel();
            
            assertListBoxEquals("null", expected, fillList.subList(1, fillList.size()));
        }
    }
    
}
