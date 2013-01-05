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


import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests for GlobalTextareaChoiceListEntry, not corresponding to Jenkins.
 *
 */
public class GlobalTextareaChoiceListEntrySimpleTest extends TestCase
{
    public void testGlobalTextareaChoiceListEntry_name()
    {
        // Easy case.
        {
            String name = "abc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals("abc", entry.getName());
        }
        
        // padded in the beginning.
        {
            String name = "  abc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals("abc", entry.getName());
        }
        
        // padded in the end.
        {
            String name = "abc  ";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals("abc", entry.getName());
        }
        
        // padded in the beginning and end.
        {
            String name = "   abc  ";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals("abc", entry.getName());
        }
    }
    
    /**
     * Tests to initialize with invalid parameters
     * 
     * Calling the constructor with invalid parameters succeeds now.
     * If the constructor gets to return errors with invalid parameters,
     * you will have to rewrite this tests.
     * Also, you will have to move this method to GlobalTextareaChoiceListEntryJenkinsTest,
     * for the parameter check must be performed with the descriptor.
     */
    public void testGlobalTextareaChoiceListEntry_nameWithImproperValue()
    {
        // null.
        {
            String name = null;
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals("", entry.getName());
        }
        
        // empty.
        {
            String name = "";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals(name, entry.getName());
        }
        
        // blank.
        {
            String name = "   ";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals("", entry.getName());
        }
        
        // value containing blank.
        {
            String name = "a b";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals(name, entry.getName());
        }
        
        // value starts with a numeric letter.
        {
            String name = "1ab";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals(name, entry.getName());
        }
        
        // value contains a letter, not alphabet, number, nor underscore.
        {
            String name = "a-b-c";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals(name, entry.getName());
        }
        
        // value starts with a letter, not alphabet, number, nor underscore.
        {
            String name = "!ab";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals(name, entry.getName());
        }
        
        // value contains a multibyte letter.
        {
            String name = "ａb";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry(name, null);
            assertEquals(name, entry.getName());
        }
    }
    
    public void testGlobalTextareaChoiceListEntry_choiceListText()
    {
        // complete tests are done in TextareaStringListUtilityTest.
        String choiceListText = "a\nb\nc\n";
        GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", choiceListText);
        assertEquals(
                "Basic test of choiceListText in GlobalTextareaChoiceListEntry()",
                Arrays.asList("a", "b", "c"),
                entry.getChoiceList()
        );
        assertEquals(
                "Basic test of getChoiceListText()",
                choiceListText,
                entry.getChoiceListText()
        );
    }
}
