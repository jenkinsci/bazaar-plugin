/*
 * The MIT License
 *
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
package hudson.plugins.bazaar;

import hudson.scm.RepositoryBrowser;

import java.io.IOException;
import java.net.URL;

/**
 * {@link RepositoryBrowser} for Bazaar.
 *
 * @author Alexandre Garnier <zigarn@dev.java.net>
 */
public abstract class BazaarRepositoryBrowser extends RepositoryBrowser<BazaarChangeSet> {

    /**
     * Determines the link to the diff between the version
     * in the specified revision of {@link BazaarAffectedFile} to its previous version.
     *
     * @return
     *      null if the browser doesn't have any URL for diff.
     */
    public abstract URL getDiffLink(BazaarAffectedFile affectedFile) throws IOException;

    /**
     * Determines the link to a single file under Bazaar.
     * This page should display all the past revisions of this file, etc.
     *
     * @return
     *      null if the browser doesn't have any suitable URL.
     */
    public abstract URL getFileLink(BazaarAffectedFile affectedFile) throws IOException;

    protected static boolean isRenaming(BazaarAffectedFile affectedFile) {
        return affectedFile.getOldPath() != null;
    }

    protected static boolean isFolderPath(String path) {
        return path.endsWith("/");
    }
}
