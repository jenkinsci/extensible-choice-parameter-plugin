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

import jenkins.model.Jenkins;
import hudson.util.FormValidation;

import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Tests for GlobalTextareaChoiceListEntry, corresponding to Jenkins.
 *
 */
public class GlobalTextareaChoiceListEntryJenkinsTest extends HudsonTestCase
{
    private GlobalTextareaChoiceListEntry.DescriptorImpl getDescriptor()
    {
        return (GlobalTextareaChoiceListEntry.DescriptorImpl)Jenkins.getInstance().getDescriptor(GlobalTextareaChoiceListEntry.class);
    }
    
    /**
     * Good inputs for name.
     */
    public void testDescriptorDoCheckNameOk()
    {
        GlobalTextareaChoiceListEntry.DescriptorImpl descriptor = getDescriptor();
        
        // OK: lower alphabets
        assertEquals(descriptor.doCheckName("abc").kind, FormValidation.Kind.OK);
        
        // OK: upper alphabets
        assertEquals(descriptor.doCheckName("ABC").kind, FormValidation.Kind.OK);
        
        // OK: alphabets and numbers
        assertEquals(descriptor.doCheckName("abc123").kind, FormValidation.Kind.OK);
        
        // OK: alphabets, numbers, and underscores.
        assertEquals(descriptor.doCheckName("abc_1_2_3").kind, FormValidation.Kind.OK);
        
        // OK: starts with underscore.
        assertEquals(descriptor.doCheckName("_abc_1_2_3").kind, FormValidation.Kind.OK);
        
        // OK: blank in the beginning
        assertEquals(descriptor.doCheckName("  _abc_1_2_3").kind, FormValidation.Kind.OK);
        
        // OK: blank in the end
        assertEquals(descriptor.doCheckName("  _abc_1_2_3   ").kind, FormValidation.Kind.OK);
    }
    
    /**
     * Bad inputs for name.
     */
    public void testDescriptorDoCheckNameError()
    {
        GlobalTextareaChoiceListEntry.DescriptorImpl descriptor = getDescriptor();
        
        // ERROR: null
        assertEquals(descriptor.doCheckName(null).kind, FormValidation.Kind.ERROR);
        
        // ERROR: empty
        assertEquals(descriptor.doCheckName("").kind, FormValidation.Kind.ERROR);
        
        // ERROR: blank
        assertEquals(descriptor.doCheckName(" ").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value containing blank
        assertEquals(descriptor.doCheckName("a b").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value starts with a numeric letter.
        assertEquals(descriptor.doCheckName("1ab").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value contains a letter, not alphabet, number, nor underscore.
        assertEquals(descriptor.doCheckName("a-b-c").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value starts with a letter, not alphabet, number, nor underscore.
        assertEquals(descriptor.doCheckName("!ab").kind, FormValidation.Kind.ERROR);
        
        // ERROR: value contains a multibyte letter.
        assertEquals(descriptor.doCheckName("ａb").kind, FormValidation.Kind.ERROR);
    }
    
    
    public void testIsValid()
    {
        // OK
        {
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("  _abc_123  ", "", false);
            assertTrue("Valid entry", entry.isValid());
        }
        
        // NG
        {
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("a b c", "", false);
            assertFalse("Invalid entry", entry.isValid());
        }
    }
}
