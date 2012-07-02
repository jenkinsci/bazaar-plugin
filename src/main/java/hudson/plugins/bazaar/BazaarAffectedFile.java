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

import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;

/**
 * {@link ChangeLogSet.AffectedFile} for Bazaar.
 *
 * @author Alexandre Garnier <zigarn@dev.java.net>
 */
public class BazaarAffectedFile implements ChangeLogSet.AffectedFile {

    private BazaarChangeSet changeSet;
    private EditType editType;
    private String oldPath;
    private String path;
    private String fileId;

    public BazaarAffectedFile(EditType editType, String oldPath, String path, String fileId) {
        this.editType = editType;
        this.oldPath = oldPath;
        this.path = path;
        this.fileId = fileId;
    }

    public void setChangeSet(BazaarChangeSet changeSet) {
        this.changeSet = changeSet;
    }

    public BazaarChangeSet getChangeSet() {
        return this.changeSet;
    }

    public EditType getEditType() {
        return this.editType;
    }

    public String getOldPath() {
        return this.oldPath;
    }

    public String getPath() {
        return this.path;
    }

    public String getFileId() {
        return this.fileId;
    }
}
