package hudson.plugins.bazaar.browsers;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.plugins.bazaar.BazaarAffectedFile;
import hudson.plugins.bazaar.BazaarChangeSet;
import hudson.plugins.bazaar.BazaarRepositoryBrowser;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;

import java.io.IOException;
import java.net.URL;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link RepositoryBrowser} for OpenGrok.
 *
 * @author Alexandre Garnier <zigarn@dev.java.net>
 */
public class OpenGrok extends BazaarRepositoryBrowser {

    /**
     * The URL of the OpenGrok repository.
     *
     * This is normally like <tt>http://src.opensolaris.org/source/</tt>
     * Normalized to have '/' at the tail.
     */
    public final URL url;

    /**
     * Root Bazaar module name (like 'foo/bar' &mdash; normalized to
     * have no leading nor trailing slash.) Can be empty.
     */
    private final String rootModule;

    @DataBoundConstructor
    public OpenGrok(URL url, String rootModule) {
        this.url = normalizeToEndWithSlash(url);

        // normalize
        rootModule = rootModule.trim();
        if(rootModule.startsWith("/"))
            rootModule = rootModule.substring(1);
        if(rootModule.endsWith("/"))
            rootModule = rootModule.substring(0,rootModule.length()-1);

        this.rootModule = rootModule;
    }

    @Override
    public URL getChangeSetLink(BazaarChangeSet changeSet) throws IOException {
        return null;
    }

    @Override
    public URL getDiffLink(BazaarAffectedFile affectedFile) throws IOException {
        URL url = null;
        String path = affectedFile.getPath().trim();
        if (affectedFile.getEditType() == EditType.EDIT && ! isFolderPath(path) && ! isRenaming(affectedFile)) {
            int revision = extractRevision(affectedFile);
            path = getFullPath(path);
            url = new URL(this.url, String.format("./diff/%s?r1=/%s@%s&r2=/%s@%s",path,
                                                                                  path,
                                                                                  revision-1,
                                                                                  path,
                                                                                  revision));
        }
        return url;
    }

    @Override
    public URL getFileLink(BazaarAffectedFile affectedFile) throws IOException {
        URL url = null;
        String path = affectedFile.getPath().trim();
        if (affectedFile.getEditType() == EditType.EDIT && ! isFolderPath(path) && ! isRenaming(affectedFile)) {
            int revision = extractRevision(affectedFile);
            path = getFullPath(path);
            url = new URL(this.url, String.format("./xref/%s?r=%s", path, revision));
        }
        return url;
    }

    private static int extractRevision(BazaarAffectedFile affectedFile) {
        return Integer.valueOf(affectedFile.getChangeSet().getRevision());
    }

    private String getFullPath(String path) {
        return this.rootModule + "/" + path;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public Descriptor<RepositoryBrowser<?>> getDescriptor() {
        return DESCRIPTOR;
    }

    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public DescriptorImpl() {
            super(OpenGrok.class);
        }

        @Override
        public String getDisplayName() {
            return "OpenGrok";
        }
    }
}
