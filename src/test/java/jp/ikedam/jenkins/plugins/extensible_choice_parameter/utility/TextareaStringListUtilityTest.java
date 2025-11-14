/**
 *
 */
package jp.ikedam.jenkins.plugins.extensible_choice_parameter.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for TextareaStringListUtility
 */
class TextareaStringListUtilityTest {

    @Test
    void testStringListFromTextarea() {
        // Easy case
        {
            String text = "a\nb\nc";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals(expected, TextareaStringListUtility.stringListFromTextarea(text), "Easy case");
        }

        // null
        {
            String text = null;
            List<String> expected = new ArrayList<>(0);
            assertEquals(expected, TextareaStringListUtility.stringListFromTextarea(text), "null");
        }

        // Empty
        {
            String text = "";
            List<String> expected = new ArrayList<>(0);
            assertEquals(expected, TextareaStringListUtility.stringListFromTextarea(text), "Empty");
        }

        // Empty line in the middle.
        {
            String text = "a\n\nc";
            List<String> expected = Arrays.asList("a", "", "c");
            assertEquals(expected, TextareaStringListUtility.stringListFromTextarea(text), "Empty line in the middle");
        }

        // Multiple empty line in the middle.
        {
            String text = "a\n\n\nc";
            List<String> expected = Arrays.asList("a", "", "", "c");
            assertEquals(
                    expected,
                    TextareaStringListUtility.stringListFromTextarea(text),
                    "Multiple empty line in the middle");
        }

        // Empty line in the beginning.
        {
            String text = "\na\nb\nc";
            List<String> expected = Arrays.asList("", "a", "b", "c");
            assertEquals(
                    expected, TextareaStringListUtility.stringListFromTextarea(text), "Empty line in the beginning");
        }

        // Multiple empty line in the beginning.
        {
            String text = "\n\na\nb\nc";
            List<String> expected = Arrays.asList("", "", "a", "b", "c");
            assertEquals(
                    expected,
                    TextareaStringListUtility.stringListFromTextarea(text),
                    "Multiple empty line in the beginning");
        }

        // Empty line in the end.
        {
            String text = "a\nb\nc\n";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals(expected, TextareaStringListUtility.stringListFromTextarea(text), "Empty line in the end");
        }

        // Multiple empty line in the end.
        {
            String text = "a\nb\nc\n\n";
            List<String> expected = Arrays.asList("a", "b", "c", "");
            assertEquals(
                    expected, TextareaStringListUtility.stringListFromTextarea(text), "Multiple empty line in the end");
        }

        // Multiple empty line only
        {
            String text = "\n";
            List<String> expected = List.of("");
            assertEquals(expected, TextareaStringListUtility.stringListFromTextarea(text), "Multiple empty line only");
        }

        // Newline code is CR LF.
        {
            String text = "a\r\nb\r\nc";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals(expected, TextareaStringListUtility.stringListFromTextarea(text), "Newline code is CR LF.");
        }

        // Newline code is mixed.
        {
            String text = "a\nb\r\nc";
            List<String> expected = Arrays.asList("a", "b", "c");
            assertEquals(expected, TextareaStringListUtility.stringListFromTextarea(text), "Newline code is mixed.");
        }

        // Newline code is mixed, and occurred in sequence
        {
            String text = "a\n\r\n\nc";
            List<String> expected = Arrays.asList("a", "", "", "c");
            assertEquals(
                    expected,
                    TextareaStringListUtility.stringListFromTextarea(text),
                    "Newline code is mixed, and occurred in sequence.");
        }

        // lines containing white spaces
        {
            String text = " a\nb \n c \n ";
            List<String> expected = Arrays.asList(" a", "b ", " c ", " ");
            assertEquals(
                    expected, TextareaStringListUtility.stringListFromTextarea(text), "lines containing white spaces.");
        }
    }

    @Test
    void testTextareaFromStringList() {
        // Easy case.
        {
            List<String> lst = Arrays.asList("a", "b", "c");
            String expected = "a\nb\nc\n";
            assertEquals(expected, TextareaStringListUtility.textareaFromStringList(lst), "Easy case.");
        }

        // null
        {
            List<String> lst = null;
            String expected = "";
            assertEquals(expected, TextareaStringListUtility.textareaFromStringList(lst), "Empty.");
        }

        // Empty
        {
            List<String> lst = new ArrayList<>(0);
            String expected = "";
            assertEquals(expected, TextareaStringListUtility.textareaFromStringList(lst), "Empty.");
        }

        // Empty string in the middle.
        {
            List<String> lst = Arrays.asList("a", "", "c");
            String expected = "a\n\nc\n";
            assertEquals(
                    expected, TextareaStringListUtility.textareaFromStringList(lst), "Empty string in the middle.");
        }

        // Multiple empty strings in the middle.
        {
            List<String> lst = Arrays.asList("a", "", "", "c");
            String expected = "a\n\n\nc\n";
            assertEquals(
                    expected,
                    TextareaStringListUtility.textareaFromStringList(lst),
                    "Multiple empty strings in the middle.");
        }

        // Empty string in the beginning.
        {
            List<String> lst = Arrays.asList("", "a", "b", "c");
            String expected = "\na\nb\nc\n";
            assertEquals(
                    expected, TextareaStringListUtility.textareaFromStringList(lst), "Empty string in the beginning.");
        }

        // Multiple empty strings in the beginning.
        {
            List<String> lst = Arrays.asList("", "", "a", "b", "c");
            String expected = "\n\na\nb\nc\n";
            assertEquals(
                    expected,
                    TextareaStringListUtility.textareaFromStringList(lst),
                    "Multiple empty strings in the beginning.");
        }

        // Empty string in the end.
        {
            List<String> lst = Arrays.asList("a", "b", "c", "");
            String expected = "a\nb\nc\n\n";
            assertEquals(expected, TextareaStringListUtility.textareaFromStringList(lst), "Empty string in the end.");
        }

        // Multiple empty strings in the end.
        {
            List<String> lst = Arrays.asList("a", "b", "c", "", "");
            String expected = "a\nb\nc\n\n\n";
            assertEquals(
                    expected,
                    TextareaStringListUtility.textareaFromStringList(lst),
                    "Multiple empty strings in the end.");
        }

        // Empty string only
        {
            List<String> lst = List.of("");
            String expected = "\n";
            assertEquals(expected, TextareaStringListUtility.textareaFromStringList(lst), "Empty string only.");
        }

        // Multiple empty strings only
        {
            List<String> lst = Arrays.asList("", "");
            String expected = "\n\n";
            assertEquals(
                    expected, TextareaStringListUtility.textareaFromStringList(lst), "Multiple empty strings only.");
        }

        // Strings containing white spaces
        {
            List<String> lst = Arrays.asList(" ", " a", " ", "b ", " c ", " ");
            String expected = " \n a\n \nb \n c \n \n";
            assertEquals(
                    expected, TextareaStringListUtility.textareaFromStringList(lst), "Multiple empty strings only.");
        }
    }
}
