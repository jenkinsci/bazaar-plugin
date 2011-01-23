package hudson.plugins.bazaar;

import hudson.Extension;
import hudson.Launcher;
import hudson.Launcher.LocalLauncher;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.model.TaskThread;
import hudson.model.AbstractBuild;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.AbstractScmTagAction;
import hudson.util.ArgumentListBuilder;
import hudson.util.MultipartFormDataParser;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class BazaarTagAction extends AbstractScmTagAction implements Describable<BazaarTagAction> {

    private final List<BazaarRevision> revisions = new ArrayList<BazaarRevision>();

    protected BazaarTagAction(AbstractBuild<?,?> build) {
        super(build);
        new BazaarTagListener().register();
    }

    public String getIconFileName() {
        if(!isTagged() && !getACL().hasPermission(getPermission()))
            return null;
        return "save.gif";
    }

    public String getDisplayName() {
        return "Tags";
    }

    @Override
    public boolean isTagged() {
        if (! hasRevisions()) {
            return false;
        }
        for (BazaarRevision revision : this.revisions) {
            if (revision.isTagged()) {
                return true;
            }
        }
        return false;
    }

    public List<BazaarRevision> getRevisions() {
        return this.revisions;
    }

    public boolean hasRevisions() {
        return this.revisions != null && ! this.revisions.isEmpty();
    }

    public synchronized void doSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        getACL().checkPermission(getPermission());

        MultipartFormDataParser parser = new MultipartFormDataParser(req);

        Map<BazaarRevision, String> newTags = new HashMap<BazaarRevision, String>();

        int i=-1;
        for (BazaarRevision e : this.revisions) {
            ++i;
            if (parser.get("tag" + i) != null && ! parser.get("name" + i).isEmpty()) {
                newTags.put(e, parser.get("name" + i));
            }
        }

        new TagWorkerThread(newTags, parser.get("force") != null).start();

        rsp.sendRedirect(".");
    }

    public synchronized void doDelete(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        getACL().checkPermission(getPermission());

        if (req.getParameter("tag") != null) {
            BazaarRevision revision = null;
            String tag = null;

            for (BazaarRevision e : this.revisions) {
                if (e.getRevId().equals(req.getParameter("revid"))) {
                    revision = e;
                    tag = req.getParameter("tag");
                }
            }

            new TagDeletionWorkerThread(revision, tag).start();
        }

        rsp.sendRedirect(".");
    }

    public static final class BazaarRevision implements Serializable {
        private String revId;
        private String revNo;
        private List<String> tags = new ArrayList<String>();

        public BazaarRevision(String revId, String revNo, List<String> tags) {
            this.revId = revId;
            this.revNo = revNo;
            this.tags = tags;
        }

        public String getRevId() {
            return revId;
        }

        public String getRevNo() {
            return revNo;
        }

        public List<String> getTags() {
            if (this.tags == null) {
                this.tags = new ArrayList<String>();
            }
            return this.tags;
        }

        public void addTag(String tag) {
            this.getTags().add(tag);
        }

        public void removeTag(String tag) {
            this.getTags().remove(tag);
        }

        public boolean isTagged() {
            return ! this.getTags().isEmpty();
        }

        @Override
        public String toString() {
            return this.revNo + " (revid: " + this.revId + ")";
        }
    }

    private class BazaarTagListener extends SCMListener {
        @Override
        public void onChangeLogParsed(AbstractBuild<?,?> build, BuildListener listener, ChangeLogSet<?> changelog) throws Exception {
            for (Object changelogEntry : changelog) {
                if (changelogEntry instanceof BazaarChangeSet) {
                    BazaarChangeSet changeset = (BazaarChangeSet) changelogEntry;
                    revisions.add(new BazaarRevision(changeset.getRevid(), changeset.getRevno(), changeset.getTags()));
                }
            }
        }
    }

    /**
     * The thread that performs tagging operation asynchronously.
     */
    private final class TagWorkerThread extends TaskThread {
        private final Map<BazaarRevision, String> tagSet;
        private final boolean force;

        public TagWorkerThread(Map<BazaarRevision, String> tagSet, boolean force) {
            super(BazaarTagAction.this, ListenerAndText.forMemory());
            this.tagSet = tagSet;
            this.force = force;
        }

        @Override
        protected void perform(TaskListener listener) {
            try {
                PrintStream logger = listener.getLogger();
                Launcher launcher = new LocalLauncher(listener);
                BazaarSCM bazaarSCM = (BazaarSCM) getBuild().getProject().getScm();

                for (Entry<BazaarRevision, String> e : tagSet.entrySet()) {
                    logger.println("Tagging " + e.getKey() + " to " + e.getValue());

                    ArgumentListBuilder args = new ArgumentListBuilder();
                    args.add(bazaarSCM.getDescriptor().getBzrExe(), "tag");
                    args.add("-r", e.getKey().getRevId());
                    args.add("-d", bazaarSCM.getSource());
                    if (this.force) {
                        args.add("--force");
                    }
                    args.add(e.getValue());

                    if (launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener.getLogger()).join() != 0) {
                        listener.error("Failed to tag");
                    } else {
                        e.getKey().addTag(e.getValue());
                    }
                }
                getBuild().save();
           } catch (Throwable e) {
               e.printStackTrace(listener.fatalError(e.getMessage()));
           }
        }
    }

    /**
     * The thread that performs tag deleting operation asynchronously.
     */
    private final class TagDeletionWorkerThread extends TaskThread {
        private final BazaarRevision revision;
        private final String tag;

        public TagDeletionWorkerThread(BazaarRevision revision , String tag) {
            super(BazaarTagAction.this, ListenerAndText.forMemory());
            this.revision = revision;
            this.tag = tag;
        }

        @Override
        protected void perform(TaskListener listener) {
            try {
                PrintStream logger = listener.getLogger();
                Launcher launcher = new LocalLauncher(listener);
                BazaarSCM bazaarSCM = (BazaarSCM) getBuild().getProject().getScm();

                logger.println("Removing tag " + tag);

                ArgumentListBuilder args = new ArgumentListBuilder();
                args.add(bazaarSCM.getDescriptor().getBzrExe(), "tag");
                args.add("-r", revision.getRevId());
                args.add("-d", bazaarSCM.getSource());
                args.add("--delete");
                args.add(tag);

                if (launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener.getLogger()).join() != 0) {
                    listener.error("Failed to delete tag");
                } else {
                    revision.removeTag(tag);
                }
                getBuild().save();
           } catch (Throwable e) {
               e.printStackTrace(listener.fatalError(e.getMessage()));
           }
        }
    }


    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public Descriptor<BazaarTagAction> getDescriptor() {
        return DESCRIPTOR;
    }

    public static class DescriptorImpl extends Descriptor<BazaarTagAction> {
        protected DescriptorImpl() {
            super(BazaarTagAction.class);
        }

        @Override
        public String getDisplayName() {
            return null;
        }
    }

}
