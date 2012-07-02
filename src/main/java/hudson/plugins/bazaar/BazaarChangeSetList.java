/*
 * The MIT License
 *
 * Copyright (C) 2009 Trond Norbye
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
package hudson.plugins.bazaar;

import hudson.scm.ChangeLogSet;
import hudson.model.AbstractBuild;

import java.util.List;
import java.util.Collections;
import java.util.Iterator;

/**
 * List of changeset that went into a particular build.
 * @author Trond Norbye
 */
public class BazaarChangeSetList extends ChangeLogSet<BazaarChangeSet> {

    private final List<BazaarChangeSet> changeSets;

    BazaarChangeSetList(AbstractBuild build, List<BazaarChangeSet> logs) {
        super(build);
        this.changeSets = Collections.unmodifiableList(logs);
        for (BazaarChangeSet log : logs) {
            log.setParent(this);
        }
    }

    public boolean isEmptySet() {
        return changeSets.isEmpty();
    }

    public Iterator<BazaarChangeSet> iterator() {
        return changeSets.iterator();
    }

    public List<BazaarChangeSet> getLogs() {
        return changeSets;
    }

    @Override
    public String getKind() {
        return "bzr";
    }
}
