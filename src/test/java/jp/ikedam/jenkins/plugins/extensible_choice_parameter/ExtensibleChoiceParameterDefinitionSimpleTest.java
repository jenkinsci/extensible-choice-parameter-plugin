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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Test for ExtensibleChoiceParameterDefinition, not corresponding to Jenkins.
 */
public class ExtensibleChoiceParameterDefinitionSimpleTest
{
    @Test
    public void testExtensibleChoiceParameterDefinition_name()
    {
        // Simple value
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "name",
                    null,
                    false,
                    "Some text"
            );
            assertEquals("Simple value", "name", target.getName());
        }
        // value surrounded with spaces.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "  name ",
                    null,
                    false,
                    "Some text"
            );
            assertEquals("value surrounded with spaces.", "name", target.getName());
        }
    }
    
    @Test
    public void testExtensibleChoiceParameterDefinition_nameWithInvalidValue()
    {
        // empty
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "",
                    null,
                    false,
                    "Some text"
            );
            assertEquals("Empty", "", target.getName());
        }
        // blank.
        {
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "  ",
                    null,
                    false,
                    "Some text"
            );
            assertEquals("blank", "", target.getName());
        }
    }
    
    @Test
    public void testExtensibleChoiceParameterDefinition_description()
    {
        // Simple value
        {
            String description = "Some text";
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "name",
                    null,
                    false,
                    description
            );
            assertEquals("Simple value", description, target.getDescription());
        }
        
        // value surrounded with blank letters
        {
            String description = " \nSome\n text ";
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "name",
                    null,
                    false,
                    description
            );
            assertEquals("value surrounded with blank letters", description, target.getDescription());
        }
        
        // null
        {
            String description = null;
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "name",
                    null,
                    false,
                    description
            );
            assertEquals("null", description, target.getDescription());
        }
    }
    
    
    @Test
    public void testExtensibleChoiceParameterDefinition_choiceListProvider()
    {
        // Simple value
        {
            ChoiceListProvider provider = new TextareaChoiceListProvider("a\nb\nc\n", null, false, null);
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "name",
                    provider,
                    false,
                    "Some Text"
            );
            assertEquals("Simple value", provider, target.getChoiceListProvider());
        }
        
        // null
        {
            ChoiceListProvider provider = null;
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "name",
                    provider,
                    false,
                    "Some Text"
            );
            assertEquals("null", provider, target.getChoiceListProvider());
        }
    }
    
    @Test
    public void testExtensibleChoiceParameterDefinition_editable()
    {
        // editable
        {
            boolean editable = true;
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "name",
                    null,
                    editable,
                    "Some Text"
            );
            assertEquals("editable", editable, target.isEditable());
        }
        
        // noneditable
        {
            boolean editable = false;
            ExtensibleChoiceParameterDefinition target = new ExtensibleChoiceParameterDefinition(
                    "name",
                    null,
                    editable,
                    "Some Text"
            );
            assertEquals("noneditable", editable, target.isEditable());
        }
    }
}
