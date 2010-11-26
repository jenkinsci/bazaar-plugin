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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class BazaarTagAction extends AbstractScmTagAction implements Describable<BazaarTagAction> {

    private final Map<BazaarRevision, List<String>> tags = new HashMap<BazaarRevision, List<String>>();

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
        for (List<String> t : tags.values()) {
            if (!t.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public Map<BazaarRevision, List<String>> getTags() {
        return this.tags;
    }

    public synchronized void doSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        getACL().checkPermission(getPermission());

        MultipartFormDataParser parser = new MultipartFormDataParser(req);

        Map<BazaarRevision, String> newTags = new HashMap<BazaarRevision, String>();

        int i=-1;
        for (BazaarRevision e : tags.keySet()) {
            ++i;
            if (parser.get("tag"+i) != null) {
                newTags.put(e, parser.get("name" + i));
            }
        }
        // TODO : add forcing and deleting

        new TagWorkerThread(newTags).start();

        rsp.sendRedirect(".");
    }

    public static final class BazaarRevision implements Serializable {
        private String revId;
        private String revNo;

        public BazaarRevision(String revId, String revNo) {
            this.revId = revId;
            this.revNo = revNo;
        }

        public String getRevId() {
            return revId;
        }

        public String getRevNo() {
            return revNo;
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
                BazaarChangeSet changeset = (BazaarChangeSet) changelogEntry;
                tags.put(new BazaarRevision(changeset.getRevid(), changeset.getRevno()), changeset.getTags());
            }
        }
    }

    /**
     * The thread that performs tagging operation asynchronously.
     */
    private final class TagWorkerThread extends TaskThread {
        private final Map<BazaarRevision, String> tagSet;

        public TagWorkerThread(Map<BazaarRevision, String> tagSet) {
            super(BazaarTagAction.this, ListenerAndText.forMemory());
            this.tagSet = tagSet;
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
                    args.add(e.getValue());

                    if (launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener.getLogger()).join() != 0) {
                        listener.error("Failed to tag");
                    } else {
                        BazaarTagAction.this.tags.get(e.getKey()).add(e.getValue());
                    }
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
