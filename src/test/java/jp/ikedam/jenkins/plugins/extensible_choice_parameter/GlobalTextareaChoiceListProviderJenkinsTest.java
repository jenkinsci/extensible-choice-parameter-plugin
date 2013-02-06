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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.jvnet.hudson.test.HudsonTestCase;

import com.gargoylesoftware.htmlunit.html.HtmlForm;

/**
 * Tests for GlobalTextareaChoiceListProvider, corresponding to Jenkins.
 *
 */
public class GlobalTextareaChoiceListProviderJenkinsTest extends HudsonTestCase
{
    private GlobalTextareaChoiceListProvider.DescriptorImpl getDescriptor()
    {
        return (GlobalTextareaChoiceListProvider.DescriptorImpl)(new GlobalTextareaChoiceListProvider("test", null)).getDescriptor();
        //return new GlobalTextareaChoiceListProvider.DescriptorImpl();
    }
    
    public void testDescriptorSetChoiceListEntryList()
    {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry validEntry1 = new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry validEntry2 = new GlobalTextareaChoiceListEntry("entry2", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry validEntry3 = new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry invalidEntry1 = new GlobalTextareaChoiceListEntry("!invalid1", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry invalidEntry2 = new GlobalTextareaChoiceListEntry("in valid2", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry invalidEntry3 = new GlobalTextareaChoiceListEntry("3invalid", "value1\nvalue2\n");
        
        // all entries are valid
        {
            List<GlobalTextareaChoiceListEntry> passed =
                    Arrays.asList(validEntry1, validEntry2, validEntry3);
            List<GlobalTextareaChoiceListEntry> expected =
                    Arrays.asList(validEntry1, validEntry2, validEntry3);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("all entries are valid", expected, descriptor.getChoiceListEntryList());
        }
        
        // empty
        {
            List<GlobalTextareaChoiceListEntry> passed = new ArrayList<GlobalTextareaChoiceListEntry>(0);
            List<GlobalTextareaChoiceListEntry> expected = new ArrayList<GlobalTextareaChoiceListEntry>(0);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("empty", expected, descriptor.getChoiceListEntryList());
        }
        
        // null
        {
            List<GlobalTextareaChoiceListEntry> passed = null;
            List<GlobalTextareaChoiceListEntry> expected = new ArrayList<GlobalTextareaChoiceListEntry>(0);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("null", expected, descriptor.getChoiceListEntryList());
        }
        
        // contains invalid entries
        {
            List<GlobalTextareaChoiceListEntry> passed =
                    Arrays.asList(invalidEntry1, validEntry1, validEntry2, invalidEntry2, validEntry3, invalidEntry3);
            List<GlobalTextareaChoiceListEntry> expected =
                    Arrays.asList(validEntry1, validEntry2, validEntry3);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("contains invalid entries", expected, descriptor.getChoiceListEntryList());
        }
        
        // all entries are invalid
        {
            List<GlobalTextareaChoiceListEntry> passed =
                    Arrays.asList(invalidEntry1, invalidEntry2, invalidEntry3);
            List<GlobalTextareaChoiceListEntry> expected = new ArrayList<GlobalTextareaChoiceListEntry>(0);
            descriptor.setChoiceListEntryList(passed);
            assertEquals("all entries are invalid", expected, descriptor.getChoiceListEntryList());
        }
    }
    
    public void assertEquals(String message, ListBoxModel expected, ListBoxModel actual)
    {
        assertEquals(String.format("%s: size", message), expected.size(), actual.size());
        for(int i = 0; i < expected.size(); ++i){
            ListBoxModel.Option expectedOption = expected.get(i);
            ListBoxModel.Option actualOption = actual.get(i);
            
            assertEquals(String.format("%s: %d-th name", message, i),
                    expectedOption.name,
                    actualOption.name
            );
            assertEquals(String.format("%s: %d-th value", message, i),
                    expectedOption.value,
                    actualOption.value
            );
            assertEquals(String.format("%s: %d-th selected", message, i),
                    expectedOption.selected,
                    actualOption.selected
            );
        }
    }
    
    @SuppressWarnings("unchecked")  // CollectionUtils.transformedCollection is not generic.
    public void testDescriptorDoFillNameItems()
    {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        // Easy case
        {
            List<String> nameList = Arrays.asList(
                    "entry1",
                    "entry2",
                    "entry3"
            );
            @SuppressWarnings("rawtypes")
            List choiceListEntry = new ArrayList(nameList);
            CollectionUtils.transform(
                    choiceListEntry,
                    new Transformer(){
                        @Override
                        public Object transform(Object input)
                        {
                            return new GlobalTextareaChoiceListEntry((String)input, "value1\nvalue2\n");
                        }
                    }
            );
            descriptor.setChoiceListEntryList(choiceListEntry);
            
            ListBoxModel fillList = descriptor.doFillNameItems();
            
            @SuppressWarnings("rawtypes")
            List optionList = new ArrayList(nameList);
            CollectionUtils.transform(
                    optionList,
                    new Transformer(){
                        @Override
                        public Object transform(Object input)
                        {
                            String name = (String)input;
                            return new ListBoxModel.Option(name, name);
                        }
                    }
            );
            ListBoxModel expected = new ListBoxModel(optionList);
            
            assertEquals("Easy case", fillList, expected);
        }
        
        // Empty
        {
            descriptor.setChoiceListEntryList(new ArrayList<GlobalTextareaChoiceListEntry>(0));
            
            ListBoxModel fillList = descriptor.doFillNameItems();
            
            ListBoxModel expected = new ListBoxModel();
            
            assertEquals("Empty", expected, fillList);
        }
        
        // null
        {
            descriptor.setChoiceListEntryList(null);
            
            ListBoxModel fillList = descriptor.doFillNameItems();
            
            ListBoxModel expected = new ListBoxModel();
            
            assertEquals("null", expected, fillList);
        }
    }
    
    public void testDescriptorGetChoiceListEntry()
    {
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry entry1 = new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry entry2 = new GlobalTextareaChoiceListEntry("entry2", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry entry3 = new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry sameEntry3 = new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n");
        
        // Easy case
        {
            descriptor.setChoiceListEntryList(Arrays.asList(entry1, entry2, entry3));
            
            assertEquals("Easy case1", entry1, descriptor.getChoiceListEntry("entry1"));
            assertEquals("Easy case2", entry2, descriptor.getChoiceListEntry("entry2"));
            assertEquals("Easy case3", entry3, descriptor.getChoiceListEntry("entry3"));
        }
        
        // duplicate
        {
            descriptor.setChoiceListEntryList(Arrays.asList(sameEntry3, entry1, entry2, entry3));
            assertEquals("Duplicate 1", sameEntry3, descriptor.getChoiceListEntry("entry3"));
            
            descriptor.setChoiceListEntryList(Arrays.asList(entry1, entry2, entry3, sameEntry3));
            assertEquals("Duplicate 2", entry3, descriptor.getChoiceListEntry("entry3"));
        }
        
        // No matches
        {
            descriptor.setChoiceListEntryList(Arrays.asList(entry1, entry2, entry3));
            
            assertEquals("No matches", null, descriptor.getChoiceListEntry("entryX"));
            assertEquals("No matches", new ArrayList<String>(0), descriptor.getChoiceList("entryX"));
        }
        
        // Empty
        {
            descriptor.setChoiceListEntryList(new ArrayList<GlobalTextareaChoiceListEntry>(0));
            
            assertEquals("Empty", null, descriptor.getChoiceListEntry("entry1"));
            assertEquals("Empty", new ArrayList<String>(0), descriptor.getChoiceList("entryX"));
        }
    }
    
    /**
     * Tests the submitted form is saved correctly,
     * and can be loaded correctly.
     * 
     * This is performed in following steps.
     * 1. update the descriptor to the state to be submitted.
     * 2. show form.
     * 3. submit the form.
     * 4. update the descriptor to another state.
     * 5. Re-construct the descriptor
     * 6. assert if the new created descriptor is in the state of 1.
     * @throws Exception 
     */
    public void testDescriptorConfigure() throws Exception
    {
        WebClient wc = new WebClient();
        GlobalTextareaChoiceListProvider.DescriptorImpl descriptor = getDescriptor();
        GlobalTextareaChoiceListEntry validEntry1 = new GlobalTextareaChoiceListEntry("entry1", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry validEntry2 = new GlobalTextareaChoiceListEntry("entry2", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry validEntry3 = new GlobalTextareaChoiceListEntry("entry3", "value1\nvalue2\n");
        GlobalTextareaChoiceListEntry invalidEntry1 = new GlobalTextareaChoiceListEntry("!invalid1", "value1\nvalue2\n");
        
        // Simple submission
        {
            // update the descriptor to the state I want to submit.
            descriptor.setChoiceListEntryList(Arrays.asList(validEntry1, validEntry2, validEntry3));
            
            HtmlForm configForm = wc.goTo("/configure").getFormByName("config");
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);
            
            submit(configForm);
            
            assertEquals("Simple submission: descriptor after submission", 
                    Arrays.asList(validEntry1, validEntry2, validEntry3),
                    descriptor.getChoiceListEntryList()
            );
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);
            
            GlobalTextareaChoiceListProvider.DescriptorImpl newDescriptor
                = new GlobalTextareaChoiceListProvider.DescriptorImpl();
            
            assertEquals("Simple submission: descriptor serialized from config.xml",
                    Arrays.asList(validEntry1, validEntry2, validEntry3),
                    newDescriptor.getChoiceListEntryList()
            );
        }
        
        // Submission with invalid entry.
        {
            // update the descriptor to the state I want to submit.
            descriptor.setChoiceListEntryList(Arrays.asList(invalidEntry1, validEntry2, validEntry3));
            
            HtmlForm configForm = wc.goTo("/configure").getFormByName("config");
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);
            
            submit(configForm);
            
            assertEquals("Submission with invalid entry: descriptor after submission", 
                    Arrays.asList(validEntry2, validEntry3),
                    descriptor.getChoiceListEntryList()
            );
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(null);
            
            GlobalTextareaChoiceListProvider.DescriptorImpl newDescriptor
                = new GlobalTextareaChoiceListProvider.DescriptorImpl();
            
            assertEquals("Submission with invalid entry: descriptor serialized from config.xml",
                    Arrays.asList(validEntry2, validEntry3),
                    newDescriptor.getChoiceListEntryList()
            );
        }
        
        // Empty submission
        {
            // update the descriptor to the state I want to submit.
            descriptor.setChoiceListEntryList(null);
            
            HtmlForm configForm = wc.goTo("/configure").getFormByName("config");
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(Arrays.asList(validEntry1, validEntry2));
            
            submit(configForm);
            
            assertEquals("Empty submission: descriptor after submission", 
                    new ArrayList<GlobalTextareaChoiceListEntry>(0),
                    descriptor.getChoiceListEntryList()
            );
            
            // update the descriptor to the another state.
            descriptor.setChoiceListEntryList(Arrays.asList(validEntry1, validEntry2));
            
            GlobalTextareaChoiceListProvider.DescriptorImpl newDescriptor
                = new GlobalTextareaChoiceListProvider.DescriptorImpl();
            
            assertEquals("Empty submission: descriptor serialized from config.xml",
                    new ArrayList<GlobalTextareaChoiceListEntry>(0),
                    newDescriptor.getChoiceListEntryList()
            );
        }
    }
    
}
