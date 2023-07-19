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

import org.htmlunit.WebResponse;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Utility class for Tests.
 */
public class ExtensibleChoiceParameterJenkinsRule extends JenkinsRule {
    /**
     * Get Web Client that allows 405 Method Not Allowed.
     * This happens when accessing build page of a project with parameters.
     *
     * @return WebClient
     */
    public WebClient createAllow405WebClient() {
        return new WebClient() {
            private static final long serialVersionUID = -7231209645303821638L;

            @Override
            public void throwFailingHttpStatusCodeExceptionIfNecessary(WebResponse webResponse) {
                if (webResponse.getStatusCode() == 405) {
                    // allow 405.
                    return;
                }
                super.throwFailingHttpStatusCodeExceptionIfNecessary(webResponse);
            }

            @Override
            public void printContentIfNecessary(WebResponse webResponse) {
                if (webResponse.getStatusCode() == 405) {
                    // allow 405.
                    return;
                }
                super.printContentIfNecessary(webResponse);
            }
        };
    }
}
