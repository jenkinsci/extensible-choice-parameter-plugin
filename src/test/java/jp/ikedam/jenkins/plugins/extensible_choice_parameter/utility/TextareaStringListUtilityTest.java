/**
 * 
 */
package jp.ikedam.jenkins.plugins.extensible_choice_parameter.utility;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Tests for TextareaStringListUtility
 */
public class TextareaStringListUtilityTest
{
    @Test
    public void testStringListFromTextarea()
    {
        // Easy case
        {
            String text = "a\nb\nc";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals("Easy case", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // null
        {
            String text = null;
            List<String> expected = new ArrayList<String>(0);
            assertEquals("null", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // Empty
        {
            String text = "";
            List<String> expected = new ArrayList<String>(0);
            assertEquals("Empty", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }

        // Empty line in the middle.
        {
            String text = "a\n\nc";
            List<String> expected = Arrays.asList("a", "c");
            assertEquals("Empty line in the middle", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }

        // Multiple empty line in the middle.
        {
            String text = "a\n\n\nc";
            List<String> expected = Arrays.asList("a", "c");
            assertEquals("Multiple empty line in the middle", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // Empty line in the beginning.
        {
            String text = "\na\nb\nc";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals("Empty line in the beginning", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // Multiple empty line in the beginning.
        {
            String text = "\n\na\nb\nc";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals("Multiple empty line in the beginning", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // Empty line in the end.
        {
            String text = "a\nb\nc\n";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals("Empty line in the end", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // Multiple empty line in the end.
        {
            String text = "a\nb\nc\n\n";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals("Multiple empty line in the end", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // Multiple empty line only
        {
            String text = "\n";
            List<String> expected = new ArrayList<String>(0);
            assertEquals("Multiple empty line only", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // Newline code is CR LF.
        {
            String text = "a\r\nb\r\nc";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals("Newline code is CR LF.", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // Newline code is mixed.
        {
            String text = "a\nb\r\nc";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals("Newline code is mixed.", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // Newline code is mixed, and occurred in sequence
        {
            String text = "a\n\r\n\nc";
            List<String> expected = Arrays.asList("a", "c");
            assertEquals("Newline code is mixed, and occurred in sequence.", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
        
        // lines containing white spaces
        {
            String text = " a\nb \n c \n ";
            List<String> expected = Arrays.asList(" a", "b ", " c ", " ");
            assertEquals("lines containing white spaces.", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }

        // Removing all 'newline' at the end of the list
        {
            String text = "a\nb\n\n\n\n";
            List<String> expected = Arrays.asList("a", "b");
            assertEquals("Newlines at the end are removed", expected, TextareaStringListUtility.stringListFromTextarea(text));
        }
    }
    
    @Test
    public void testTextareaFromStringList()
    {
        // Easy case.
        {
            List<String> lst = Arrays.asList("a", "b", "c");
            String expected = "a\nb\nc\n";
            assertEquals("Easy case.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
        // null
        {
            List<String> lst = null;
            String expected = "";
            assertEquals("Empty.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
        // Empty
        {
            List<String> lst = new ArrayList<String>(0);
            String expected = "";
            assertEquals("Empty.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
        // Empty string in the middle.
        {
            List<String> lst = Arrays.asList("a", "c");
            String expected = "a\nc\n";
            assertEquals("Empty string in the middle.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
        // Multiple empty strings in the middle.
        {
            List<String> lst = Arrays.asList("a", "c");
            String expected = "a\nc\n";
            assertEquals("Multiple empty strings in the middle.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
        // Empty string in the beginning.
        {
            List<String> lst = Arrays.asList("a", "b", "c");
            String expected = "a\nb\nc\n";
            assertEquals("Empty string in the beginning.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
        // Multiple empty strings in the beginning.
        {
            List<String> lst = Arrays.asList("a", "b", "c");
            String expected = "a\nb\nc\n";
            assertEquals("Multiple empty strings in the beginning.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
        // Empty string in the end.
        {
            List<String> lst = Arrays.asList("a", "b", "c");
            String expected = "a\nb\nc\n";
            assertEquals("Empty string in the end.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
        // Empty string only
        {
            List<String> lst = new ArrayList<String>(0);
            String expected = "";
            assertEquals("Empty string only.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
        // Strings containing white spaces
        {
            List<String> lst = Arrays.asList(" ", " a", " ", "b ", " c ", " ");
            String expected = " \n a\n \nb \n c \n \n";
            assertEquals("Multiple empty strings only.", expected, TextareaStringListUtility.textareaFromStringList(lst));
        }
        
    }
}
