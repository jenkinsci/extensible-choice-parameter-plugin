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
 *     <caption>How TextareaStringListUtility works</caption>
 *     <tr>
 *         <th>textarea</th>
 *         <th>list of string</th>
 *     </tr>
 *     <tr>
 *         <td>a[LF]<br>b[LF]<br>c[LF]<br></td>
 *         <td>&quot;a&quot;, &quot;b&quot;, &quot;c&quot;</td>
 *     </tr>
 *     <tr>
 *         <td>a[LF]<br>b[LF]<br>c[LF]</td>
 *         <td>&quot;a&quot;, &quot;b&quot;, &quot;c&quot;</td>
 *     </tr>
 *     <tr>
 *         <td>a[LF]<br>b[LF]<br>c[LF]<br>[LF]<br></td>
 *         <td>&quot;a&quot;, &quot;b&quot;, &quot;c&quot;, &quot;&quot; </td>
 *     </tr>
 *     <tr>
 *         <td>[LF]<br></td>
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
public class TextareaStringListUtility {
    /**
     * Returns a list of string parsed from a input of textarea.
     *
     * @param choiceListText the input of a textarea
     * @return a list of string.
     */
    public static List<String> stringListFromTextarea(String choiceListText) {
        List<String> stringList = (choiceListText != null)
                ? Arrays.asList(choiceListText.split("\\r?\\n", -1))
                : new ArrayList<String>(0);
        if (!stringList.isEmpty()
                && (stringList.get(stringList.size() - 1) == null
                        || stringList.get(stringList.size() - 1).isEmpty())) {
            // The last empty line will be ignored.
            // The list object returned from asList() does not support remove,
            // so use subList().
            // And recreate ArrayList, as subList() returns java.util.AbstractList.RandomAccessSubList,
            // which results complicated output in XStream (that is, complicated config.xml),
            // and is not whitelisted in JEP-200.
            stringList = new ArrayList<String>(stringList.subList(0, stringList.size() - 1));
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
    public static String textareaFromStringList(List<String> stringList) {
        StringBuffer sb = new StringBuffer();
        if (stringList != null) {
            for (String s : stringList) {
                sb.append(s);
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
