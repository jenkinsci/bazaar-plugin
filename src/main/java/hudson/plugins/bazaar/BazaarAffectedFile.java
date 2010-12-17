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
