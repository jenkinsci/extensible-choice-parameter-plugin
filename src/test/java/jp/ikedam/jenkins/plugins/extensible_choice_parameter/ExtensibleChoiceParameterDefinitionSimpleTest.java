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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test for ExtensibleChoiceParameterDefinition, not corresponding to Jenkins.
 */
class ExtensibleChoiceParameterDefinitionSimpleTest {

    @Test
    void testExtensibleChoiceParameterDefinition_name() {
        // Simple value
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("name", null, false, "Some text");
            assertEquals("name", target.getName(), "Simple value");
        }
        // value surrounded with spaces.
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("  name ", null, false, "Some text");
            assertEquals("name", target.getName(), "value surrounded with spaces.");
        }
    }

    @Test
    void testExtensibleChoiceParameterDefinition_nameWithInvalidValue() {
        // empty
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("", null, false, "Some text");
            assertEquals("", target.getName(), "Empty");
        }
        // blank.
        {
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("  ", null, false, "Some text");
            assertEquals("", target.getName(), "blank");
        }
    }

    @Test
    void testExtensibleChoiceParameterDefinition_description() {
        // Simple value
        {
            String description = "Some text";
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("name", null, false, description);
            assertEquals(description, target.getDescription(), "Simple value");
        }

        // value surrounded with blank letters
        {
            String description = " \nSome\n text ";
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("name", null, false, description);
            assertEquals(description, target.getDescription(), "value surrounded with blank letters");
        }

        // null
        {
            String description = null;
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("name", null, false, description);
            assertEquals(description, target.getDescription(), "null");
        }
    }

    @Test
    void testExtensibleChoiceParameterDefinition_choiceListProvider() {
        // Simple value
        {
            ChoiceListProvider provider = new TextareaChoiceListProvider("a\nb\nc\n", null, false, null);
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("name", provider, false, "Some Text");
            assertEquals(provider, target.getChoiceListProvider(), "Simple value");
        }

        // null
        {
            ChoiceListProvider provider = null;
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("name", provider, false, "Some Text");
            assertEquals(provider, target.getChoiceListProvider(), "null");
        }
    }

    @Test
    void testExtensibleChoiceParameterDefinition_editable() {
        // editable
        {
            boolean editable = true;
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("name", null, editable, "Some Text");
            assertEquals(editable, target.isEditable(), "editable");
        }

        // noneditable
        {
            boolean editable = false;
            ExtensibleChoiceParameterDefinition target =
                    new ExtensibleChoiceParameterDefinition("name", null, editable, "Some Text");
            assertEquals(editable, target.isEditable(), "noneditable");
        }
    }
}
