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
