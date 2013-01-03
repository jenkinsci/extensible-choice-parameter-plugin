/**
 * 
 */
package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

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
        return (GlobalTextareaChoiceListEntry.DescriptorImpl) (new GlobalTextareaChoiceListEntry(null, null)).getDescriptor();
    }
    
    /**
     * Good inputs for name.
     */
    public void testDoCheckNameOk()
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
    public void testDoCheckNameError()
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
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("  _abc_123  ", "");
            assertTrue("Valid entry", entry.isValid());
        }
        
        // NG
        {
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("a b c", "");
            assertFalse("Invalid entry", entry.isValid());
        }
    }
}
