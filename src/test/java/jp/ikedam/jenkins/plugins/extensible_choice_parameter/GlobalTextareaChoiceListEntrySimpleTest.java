/**
 * 
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
    
    public void testGlobalTextareaChoiceListEntry_choiceList()
    {
        // Easy case.
        {
            String text = "a\nb\nc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals(Arrays.asList("a", "b", "c"), entry.getChoiceList());
        }
        
        // null
        {
            String text = null;
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals(0, entry.getChoiceList().size());
        }
        
        // Empty
        {
            String text = "";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals(0, entry.getChoiceList().size());
        }
        
        // Empty line in the middle.
        {
            String text = "a\n\nc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals(Arrays.asList("a", "", "c"), entry.getChoiceList());
        }
        
        // Multiple empty line in the middle.
        {
            String text = "a\n\n\nc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals(Arrays.asList("a", "", "", "c"), entry.getChoiceList());
        }
        
        // Empty line in the beginning.
        {
            String text = "\na\nb\nc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals(Arrays.asList("", "a", "b", "c"), entry.getChoiceList());
        }
        
        // Multiple empty line in the beginning.
        {
            String text = "\n\na\nb\nc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals(Arrays.asList("", "", "a", "b", "c"), entry.getChoiceList());
        }
        
        // Empty line in the end.
        {
            String text = "a\nb\nc\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals(Arrays.asList("a", "b", "c"), entry.getChoiceList());
        }
        
        // Multiple empty line in the end.
        {
            String text = "a\nb\nc\n\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Multiple empty line in the end.",
                    Arrays.asList("a", "b", "c", ""),
                    entry.getChoiceList()
            );
        }
        
        // Multiple empty line only
        {
            String text = "\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Multiple empty line only",
                    Arrays.asList(""),
                    entry.getChoiceList()
            );
        }
        
        // Newline code is CR LF.
        {
            String text = "a\r\nb\r\nc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Newline code is CR LF.",
                    Arrays.asList("a", "b", "c"),
                    entry.getChoiceList()
            );
        }
        
        
        // Newline code is mixed.
        {
            String text = "a\nb\r\nc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Newline code is mixed.",
                    Arrays.asList("a", "b", "c"),
                    entry.getChoiceList()
            );
        }
        
        
        // Newline code is mixed, and continued
        {
            String text = "a\n\r\n\nc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Newline code is mixed, and continued", 
                    Arrays.asList("a", "", "", "c"), entry.getChoiceList());
        }
    }
    
    public void testGetChoiceListText()
    {
        // Easy case.
        {
            String text = "a\nb\nc\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Easy case.", text, entry.getChoiceListText());
        }
        
        // Last line is not terminated with LF.
        {
            String text = "a\nb\nc";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Last line is not terminated with LF.", text + "\n", entry.getChoiceListText());
        }
        
        // Empty
        {
            String text = "";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Empty", text, entry.getChoiceListText());
        }
        
        // Empty choice in the middle.
        {
            String text = "a\n\nc\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Empty choice in the middle.", text, entry.getChoiceListText());
        }
        
        // Multiple empty choices in the middle.
        {
            String text = "a\n\n\nc\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Multiple empty choices in the middle.", text, entry.getChoiceListText());
        }
        
        // Empty choice in the beginning.
        {
            String text = "\na\nb\nc\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Empty choice in the beginning.", text, entry.getChoiceListText());
        }
        
        // Multiple empty choices in the beginning.
        {
            String text = "\n\na\nb\nc\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Multiple empty choices in the beginning.", text, entry.getChoiceListText());
        }
        
        // Empty choice in the end.
        {
            String text = "a\nb\nc\n\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Empty choice in the end.", text, entry.getChoiceListText());
        }
        
        // Multiple empty choice in the end.
        {
            String text = "a\nb\nc\n\n\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Multiple empty choice in the end.", text, entry.getChoiceListText());
        }
        
        // Empty choice only
        {
            String text = "\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Empty choice only", text, entry.getChoiceListText());
        }
        
        // Multiple empty choice only
        {
            String text = "\n\n";
            GlobalTextareaChoiceListEntry entry = new GlobalTextareaChoiceListEntry("test", text);
            assertEquals("Multiple empty choice only", text, entry.getChoiceListText());
        }
    }
}
