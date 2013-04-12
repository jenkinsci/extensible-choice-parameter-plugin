/*
 * The MIT License
 * 
 * Copyright (c) 2013 IKEDA Yasuyuki
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

import junit.framework.TestCase;

/**
 * Tests for SystemGroovyChoiceListProvider, corresponding to Jenkins.
 */
public class SystemGroovyChoiceListProviderSimpleTest extends TestCase
{
    public void testSystemGroovyChoiceListProvider_scriptText()
    {
        // simple value
        {
            String scriptText = "abc";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
        
        // value with spaces
        {
            String scriptText = "  abc  \n  cdf  ";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
        
        // empty
        {
            String scriptText = "";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
        
        // blank
        {
            String scriptText = "   ";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
        
        // null
        {
            String scriptText = null;
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, null);
            assertEquals("SystemGroovyChoiceListProvider must preserve scriptText", scriptText, target.getScriptText());
        }
    }
    
    public void testSystemGroovyChoiceListProvider_defaultChoice()
    {
        String scriptText = "abc";
        
        // a value
        {
            String defaultChoice = "some value";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals("a value", defaultChoice, target.getDefaultChoice());
        }
        
        // null
        {
            String defaultChoice = null;
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals("null", defaultChoice, target.getDefaultChoice());
        }
        
        // empty
        {
            String defaultChoice = "";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals("empty", defaultChoice, target.getDefaultChoice());
        }
        
        // blank
        {
            String defaultChoice = "  ";
            SystemGroovyChoiceListProvider target = new SystemGroovyChoiceListProvider(scriptText, defaultChoice);
            assertEquals("blank", defaultChoice, target.getDefaultChoice());
        }
    }
}
