/*
 * The MIT License
 *
 * Copyright (C) 2010 Robert Collins
 * Copyright (C) 2010 Alexandre Garnier <zigarn@dev.java.net>
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.bazaar;

import hudson.scm.SCMRevisionState;

/**
 *
 * @author Robert Collins <robertc@robertcollins.net>
 */
public class BazaarRevisionState extends SCMRevisionState {

    private final String revNo;
    private final String rev_id;

    public BazaarRevisionState(String revNo, String revId) {
        this.revNo = revNo;
        this.rev_id = revId;
    }

    public String getRevNo() {
        return this.revNo;
    }

    public String getRevId() {
        return this.rev_id;
    }

    @Override
    public String toString() {
        return "RevisionState revno:" + this.revNo + " revid:" + this.rev_id;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof BazaarRevisionState) {
            BazaarRevisionState that = (BazaarRevisionState) other;
            result = this.rev_id.equals(that.rev_id);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return this.rev_id.hashCode();
    }
}
