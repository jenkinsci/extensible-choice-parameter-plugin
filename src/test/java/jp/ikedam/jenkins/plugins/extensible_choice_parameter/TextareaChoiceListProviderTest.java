/**
 * 
 */
package jp.ikedam.jenkins.plugins.extensible_choice_parameter;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests for TextareaChoiceListProvider, not corresponding to Jenkins.
 */
public class TextareaChoiceListProviderTest extends TestCase
{
    public void testTextareaChoiceListProvider_choiceListText()
    {
        // complete tests are done in TextareaStringListUtilityTest.
        String choiceListText = "a\nb\nc\n";
        TextareaChoiceListProvider target = new TextareaChoiceListProvider(choiceListText);
        assertEquals(
                "Basic test of choiceListText in TextareaChoiceListProvider()",
                Arrays.asList("a", "b", "c"),
                target.getChoiceList()
        );
    }
}
