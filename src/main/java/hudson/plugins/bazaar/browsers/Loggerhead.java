package hudson.plugins.bazaar.browsers;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.bazaar.BazaarAffectedFile;
import hudson.plugins.bazaar.BazaarChangeSet;
import hudson.plugins.bazaar.BazaarRepositoryBrowser;
import hudson.scm.RepositoryBrowser;

import java.io.IOException;
import java.net.URL;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link RepositoryBrowser} for Loggerhead.
 *
 * @author Alexandre Garnier <zigarn@dev.java.net>
 */
public class Loggerhead extends BazaarRepositoryBrowser {

    /**
     * The URL of the Loggerhead repository.
     *
     * This is normally like <tt>http://bazaar.launchpad.net/~myteam/myproject/</tt>
     * Normalized to have '/' at the tail.
     */
    public final URL url;

    @DataBoundConstructor
    public Loggerhead(URL url) {
        this.url = normalizeToEndWithSlash(url);
    }

    @Override
    public URL getChangeSetLink(BazaarChangeSet changeSet) throws IOException {
        return new URL(this.url, "./revision/" + changeSet.getRevno());
    }

    @Override
    public URL getDiffLink(BazaarAffectedFile affectedFile) throws IOException {
        URL url = null;
        String path = affectedFile.getPath().trim();
        if (! isFolderPath(path) && ! isRenaming(affectedFile)) {
            return new URL(this.url, String.format("./revision/%s/%s", affectedFile.getChangeSet().getRevno(),
                                                                       trimHeadSlash(path)));
        }
        return url;
    }

    @Override
    public URL getFileLink(BazaarAffectedFile affectedFile) throws IOException {
        String path = affectedFile.getPath().trim();
        return new URL(this.url, String.format("./%s/%s/%s?file_id=%s", getBrowsingType(path),
                                                                        affectedFile.getChangeSet().getRevno(),
                                                                        trimHeadSlash(path),
                                                                        affectedFile.getFileId()));
    }

    private static String getBrowsingType(String path) {
        String browsingType = "annotate";
        if (isFolderPath(path)) {
            browsingType = "files";
        }
        return browsingType;
    }


    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public Descriptor<RepositoryBrowser<?>> getDescriptor() {
        return DESCRIPTOR;
    }

    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public DescriptorImpl() {
            super(Loggerhead.class);
        }

        @Override
        public String getDisplayName() {
            return "Loggerhead (used by Launchpad)";
        }
    }
}
