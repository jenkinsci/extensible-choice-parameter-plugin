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

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import jp.ikedam.jenkins.plugins.extensible_choice_parameter.utility.TextareaStringListUtility;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * A set of choices that can be used in Global Choice Parameter.
 *
 * Holds a name and a list of choices.
 * Added in the System Configuration page.
 */
public class GlobalTextareaChoiceListEntry extends AbstractDescribableImpl<GlobalTextareaChoiceListEntry>
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Pattern namePattern = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");

    /**
     * @return A regular expression pattern for the acceptable names.
     */
    public static Pattern getNamePattern() {
        return namePattern;
    }

    /**
     * The internal class to work with views.
     *
     * The following files are used (put in main/resource directory in the source tree).
     * <dl>
     *     <dt>config.jelly</dt>
     *         <dd>
     *         shown as a part of the system configuration page.
     *         Called from global.jelly of GlobalTextareaChoiceListProvider.
     *         </dd>
     * </dl>
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<GlobalTextareaChoiceListEntry> {
        /**
         * Don't care for this is not shown in any page.
         *
         * @return the display name of this object.
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return "GlobalTextareaChoiceListEntry";
        }

        /**
         * Check the input name is acceptable.
         * <ul>
         *      <li>Must not be empty.</li>
         * </ul>
         *
         * @param name
         * @return FormValidation object.
         */
        public FormValidation doCheckName(@QueryParameter String name) {
            if (StringUtils.isBlank(name)) {
                return FormValidation.error(Messages.GlobalTextareaChoiceListEntry_Name_empty());
            }

            if (!getNamePattern().matcher(name.trim()).matches()) {
                return FormValidation.error(Messages.GlobalTextareaChoiceListEntry_Name_invalid());
            }

            return FormValidation.ok();
        }
    }

    private String name = null;

    /**
     * the name of this set of choices.
     *
     * @return the name the user specified.
     */
    public String getName() {
        return name;
    }

    private List<String> choiceList = null;

    /**
     * The list of choices.
     *
     * @return the list of choices the user specified.
     */
    public List<String> getChoiceList() {
        return choiceList;
    }

    /**
     * The list of choices, joined into a string.
     *
     * Used for filling a field when the configuration page is shown.
     * The newline code is appended also after the last element.
     *
     * @return Joined choices.
     */
    public String getChoiceListText() {
        return TextareaStringListUtility.textareaFromStringList(getChoiceList());
    }

    /**
     * Set choiceList
     *
     * @param choiceList the choiceList to set
     */
    protected void setChoiceList(List<String> choiceList) {
        this.choiceList = choiceList;
    }

    private boolean allowAddEditedValue;

    /**
     * Return whether users can add a new value by building a job using this choice list.
     *
     * @return the allowAddEditedValue
     */
    public boolean isAllowAddEditedValue() {
        return allowAddEditedValue;
    }

    /**
     * Constructor instantiating with parameters in the configuration page.
     *
     * When instantiating from the saved configuration,
     * the object is directly serialized with XStream,
     * and no constructor is used.
     *
     * @param name the name of this set of choices.
     * @param choiceListText the text where choices are written in each line.
     * @param allowAddEditedValue whether users can add a new value by building a job using this choice list.
     */
    @DataBoundConstructor
    public GlobalTextareaChoiceListEntry(String name, String choiceListText, boolean allowAddEditedValue) {
        this.name = (name != null) ? name.trim() : "";
        this.choiceList = TextareaStringListUtility.stringListFromTextarea(choiceListText);
        this.allowAddEditedValue = allowAddEditedValue;
    }

    /**
     * Returns whether this object is configured correctly.
     *
     * Jenkins framework seems to accept the values that doCheckXXX alerts an error.
     * Throwing a exception from the constructor annotated with DataBoundConstructor
     * results in an exception page, so I decided to omit bad objects with this method.
     *
     * @return whether this object is configured correctly.
     */
    public boolean isValid() {
        DescriptorImpl descriptor = ((DescriptorImpl) getDescriptor());
        {
            FormValidation v = descriptor.doCheckName(name);
            if (v.kind == FormValidation.Kind.ERROR) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param value
     */
    public void addEditedValue(String value) {
        List<String> newChoiceList = new ArrayList<String>(getChoiceList());
        newChoiceList.add(value);
        setChoiceList(newChoiceList);
    }

    /**
     * @param o
     * @return
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GlobalTextareaChoiceListEntry)) {
            return false;
        }

        GlobalTextareaChoiceListEntry entry = (GlobalTextareaChoiceListEntry) o;

        if (name == null) {
            if (entry.name != null) {
                return false;
            }
        } else if (!name.equals(entry.name)) {
            return false;
        }

        if (choiceList == null) {
            if (entry.choiceList != null) {
                return false;
            }
        } else if (!choiceList.equals(entry.choiceList)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int r = (name != null) ? name.hashCode() : 0;
        r = r * 31 + ((choiceList != null) ? choiceList.hashCode() : 0);
        return r;
    }

    /**
     * @return
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format(
                "[%s: name=%s, choiceList=%s]",
                getClass().getName(), getName(), StringUtils.join(getChoiceList(), ","));
    }
}
