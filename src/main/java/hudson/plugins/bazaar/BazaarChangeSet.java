package hudson.plugins.bazaar;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import java.util.ArrayList;
import java.util.Arrays;
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

    private String date;
    private String msg;
    private List<String> added = new ArrayList<String>();
    private List<String> deleted = new ArrayList<String>();
    private List<String> modified = new ArrayList<String>();

    /**
     * Lazily computed.
     */
    private volatile List<String> affectedPaths;

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
    public String getRevid() {
        return revid;
    }

    @Exported
    public String getDate() {
        return date;
    }

    @Override
    public Collection<String> getAffectedPaths() {
        if (affectedPaths == null) {
            List<String> r = new ArrayList<String>(added.size() + modified.size() + deleted.size());
            r.addAll(added);
            r.addAll(modified);
            r.addAll(deleted);
            affectedPaths = r;
        }
        return affectedPaths;
    }

    /**
     * Gets all the files that were added.
     */
    @Exported
    public List<String> getAddedPaths() {
        return added;
    }

    /**
     * Gets all the files that were deleted.
     */
    @Exported
    public List<String> getDeletedPaths() {
        return deleted;
    }

    /**
     * Gets all the files that were modified.
     */
    @Exported
    public List<String> getModifiedPaths() {
        return modified;
    }

    public List<String> getPaths(EditType kind) {
        if (kind == EditType.ADD) {
            return getAddedPaths();
        }
        if (kind == EditType.EDIT) {
            return getModifiedPaths();
        }
        if (kind == EditType.DELETE) {
            return getDeletedPaths();
        }
        return null;
    }

    /**
     * Returns all three variations of {@link EditType}.
     * Placed here to simplify access from views.
     */
    public List<EditType> getEditTypes() {
        // return EditType.ALL;
        return Arrays.asList(EditType.ADD, EditType.EDIT, EditType.DELETE);
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

    public void setDate(String date) {
        this.date = date;
    }
}
