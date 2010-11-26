package hudson.plugins.bazaar;

import hudson.model.User;
import hudson.scm.ChangeLogSet;

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
    private String revno;
    private String revid;
    private List<String> tags = new ArrayList<String>();

    private String date;
    private String msg;

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
        return User.get(author);
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

    public void setUser(String author) {
        this.author = author;
    }

    public String getUser() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
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

    public void addAffectedFile(BazaarAffectedFile affectedFile) {
        affectedFile.setChangeSet(this);
        this.affectedFiles.add(affectedFile);
    }
}
