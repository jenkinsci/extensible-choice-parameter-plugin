/**
 * 
 */
package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import junit.framework.TestCase;

/**
 * Tests for GlobalTextareaChoiceListProvider, not corresponding to Jenkins.
 *
 */
public class GlobalTextareaChoiceListProviderSimpleTest extends TestCase
{
    public void testGlobalTextareaChoiceListProvider()
    {
        String name = "abc";
        GlobalTextareaChoiceListProvider target = new GlobalTextareaChoiceListProvider(name);
        assertEquals("Basic test of passing name to GlobalTextareaChoiceListProvider()", name, target.getName());
    }
}
