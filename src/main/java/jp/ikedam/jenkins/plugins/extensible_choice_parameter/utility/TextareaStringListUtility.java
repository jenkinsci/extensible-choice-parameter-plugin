/**
 * 
 */
package jp.ikedam.jenkins.plugins.extensible_choice_parameter.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility Class to work with a list of strings in a textarea.
 * 
 * Strings are written in each line of a textarea.
 * Each line must be ended with new line codes (LF or CRLF).
 * If there is a string after the last new line code,
 * that string will be treated as a last string.
 * 
 * white spaces are never trimmed.
 * 
 * Here is a examples:
 * <table>
 *     <tr>
 *         <th>textarea</th>
 *         <th>list of string</th>
 *     </tr>
 *     <tr>
 *         <td>a[LF]<br />b[LF]<br />c[LF]<br /></td>
 *         <td>&quot;a&quot;, &quot;b&quot;, &quot;c&quot;</td>
 *     </tr>
 *     <tr>
 *         <td>a[LF]<br />b[LF]<br />c[LF]</td>
 *         <td>&quot;a&quot;, &quot;b&quot;, &quot;c&quot;</td>
 *     </tr>
 *     <tr>
 *         <td>a[LF]<br />b[LF]<br />c[LF]<br />[LF]<br /></td>
 *         <td>&quot;a&quot;, &quot;b&quot;, &quot;c&quot;, &quot;&quot; </td>
 *     </tr>
 *     <tr>
 *         <td>[LF]<br /></td>
 *         <td>&quot;&quot;</td>
 *     </tr>
 *     <tr>
 *         <td>&nbsp;</td>
 *         <td>(empty list)</td>
 *     </tr>
 *     <tr>
 *         <td>(null)</td>
 *         <td>(empty list)</td>
 *     </tr>
 * </table>
 */
public class TextareaStringListUtility
{
    /**
     * Returns a list of string parsed from a input of textarea.
     * 
     * @param choiceListText the input of a textarea
     * @return a list of string.
     */
    public static List<String> stringListFromTextarea(String choiceListText)
    {
        List<String> stringList = (choiceListText != null)?Arrays.asList(choiceListText.split("\\r?\\n", -1)):new ArrayList<String>(0);
        if(!stringList.isEmpty() && stringList.get(stringList.size() - 1).isEmpty())
        {
            // The last empty line will be ignored.
            // The list object returned from asList() does not support remove,
            // so use subList().
            stringList = stringList.subList(0, stringList.size() - 1);
        }
        
        return stringList;
    }
    
    /**
     * Join the list of strings into a text.
     * 
     * Each string will end with LF.
     * 
     * @param stringList a list of strings to join.
     * @return a text the strings joined.
     */
    public static String textareaFromStringList(List<String> stringList)
    {
        StringBuffer sb = new StringBuffer();
        for(String s: stringList)
        {
            sb.append(s);
            sb.append('\n');
        }
        return sb.toString();
    }
}
