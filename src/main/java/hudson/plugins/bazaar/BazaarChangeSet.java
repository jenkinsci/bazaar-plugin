/*
 * The MIT License
 *
 * Copyright (C) 2009 Trond Norbye
 * Copyright (C) 2010-2011 Alexandre Garnier <zigarn@dev.java.net>
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

import static hudson.Util.fixEmpty;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.tasks.Mailer;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.kohsuke.stapler.export.Exported;

/**
 * Represents a change set.
 *
 * <p>
 * The object should be treated like an immutable object.
 * </p>
 *
 * @author Trond Norbye
 */
public class BazaarChangeSet extends ChangeLogSet.Entry {

    private String author;
    private String authorEmail;
    private String revno;
    private String revid;
    private List<String> tags = new ArrayList<String>();

    private String date;
    private String msg;

    private boolean isMerge = false;

    private List<BazaarAffectedFile> affectedFiles = new ArrayList<BazaarAffectedFile>();

    /**
     * Commit message.
     */
    @Exported
    public String getMsg() {
        return msg;
    }

    /**
     * Gets the user who made this change.
     */
    @Exported
    public User getAuthor() {
        User user = User.get(author, false);

        if (user == null) {
            user = User.get(author, true);

            // set email address for user
            if (fixEmpty(authorEmail) != null) {
                try {
                    user.addProperty(new Mailer.UserProperty(authorEmail));
                } catch (IOException e) {
                    // ignore error
                }
            }
        }

        return user;
    }

    /**
     * Gets repository revision number, which is local in the current repository.
     */
    @Exported
    public String getRevno() {
        return revno;
    }

    @Exported
    public String getRevision() {
        return this.getRevno();
    }

    @Exported
    public String getRevid() {
        return revid;
    }

    @Exported
    public List<String> getTags() {
        return tags;
    }

    @Exported
    public String getDate() {
        return date;
    }

    @Exported
    public boolean isMerge() {
        return this.isMerge;
    }

    @Override
    public Collection<String> getAffectedPaths() {
        return new AbstractList<String>() {
            public String get(int index) {
                return affectedFiles.get(index).getPath();
            }
            public int size() {
                return affectedFiles.size();
            }
        };
    }

    @Override
    public Collection<BazaarAffectedFile> getAffectedFiles() {
        return affectedFiles;
    }

    @Override
    protected void setParent(ChangeLogSet parent) {
        super.setParent(parent);
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public void setRevno(String revno) {
        this.revno = revno;
    }

    public void setRevid(String revid) {
        this.revid = revid;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setMerge(boolean isMerge) {
        this.isMerge = isMerge;
    }

    public void addAffectedFile(BazaarAffectedFile affectedFile) {
        affectedFile.setChangeSet(this);
        this.affectedFiles.add(affectedFile);
    }
}
