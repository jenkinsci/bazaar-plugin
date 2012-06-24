package hudson.plugins.bazaar;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Launcher.LocalLauncher;
import hudson.Launcher.ProcStarter;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.remoting.VirtualChannel;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.PollingResult.Change;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.framework.io.ByteBuffer;

/**
 * Bazaar SCM.
 * 
 * @author Trond Norbye
 */
public class BazaarSCM extends SCM implements Serializable {

    /**
     * Source repository URL from which we pull.
     */
    private final String source;
    private final boolean clean;
    private final BazaarRepositoryBrowser browser;
    private final boolean checkout;

    @DataBoundConstructor
    public BazaarSCM(String source, boolean clean, BazaarRepositoryBrowser browser, boolean checkout) {
        this.source = source;
        this.clean = clean;
        this.browser = browser;
        this.checkout = checkout;
    }

    public BazaarSCM(String source, boolean clean, BazaarRepositoryBrowser browser) {
        this(source, clean, browser, false);
    }

    /**
     * Gets the source repository path.
     * Either URL or local file path.
     * @return
     */
    public String getSource() {
        return source;
    }

    /**
     * True if we want clean check out each time. This means deleting everything in the workspace
     * @return
     */
    public boolean isClean() {
        return clean;
    }

    /**
     * True if we want to use checkout --lightweight
     * @return
     */
    public boolean isCheckout() {
        return checkout;
    }

    @Override
    @Exported
    public BazaarRepositoryBrowser getBrowser() {
        return browser;
    }

    private BazaarRevisionState getRevisionState(Launcher launcher, TaskListener listener, String root)
            throws InterruptedException {
        BazaarRevisionState rev = null;
        try {
            if (launcher == null) {
                /* Running for a VM or whathaveyou: make a launcher on master
                 * todo grab a launcher on 'any slave'
                 */
                launcher = new LocalLauncher(listener);
            }
            PrintStream output = listener.getLogger();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            final String bzr_cmd = getDescriptor().getBzrExe();
            ProcStarter starter = launcher.launch();

            starter = starter.cmds(bzr_cmd, "revision-info", "-d", root);
            // The launcher should already have the right vars!
            // starter = starter.envs(EnvVars.masterEnvVars);
            starter = starter.stdout(stdout);
            starter = starter.stderr(stderr);
            // not needed without workspaces : -d starter = starter.pwd(workspace);
            final int ret = starter.join();
            final String info_output = "bzr revision-info -d " + root + " returned " + ret + ". Command output: \"" + stdout.toString() + "\" stderr: \"" + stderr.toString() + "\"";
            if (ret != 0) {
                logger.warning(info_output);
            } else {
                String[] infos = stdout.toString().trim().split("\\s");
                if (infos.length == 2)
                {
                    rev = new BazaarRevisionState(infos[0], infos[1]);
                } else {
                    logger.log(Level.WARNING, "Issue splitting: {0}",
                               stdout.toString().trim());
                }
            }

            output.printf("info result: %s\n", info_output);
        } catch (IOException e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            logger.log(Level.WARNING, "Failed to poll repository: ", e);
        }

        if (rev == null) {
            logger.log(Level.WARNING, "Failed to get revision state for: {0}", root);
        }

        return rev;
    }

    private void getLog(Launcher launcher, FilePath workspace, BazaarRevisionState oldRevisionState, BazaarRevisionState newRevisionState, File changeLog) throws InterruptedException {
        try {
            int ret;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String version = "revid:" + oldRevisionState.getRevId() + "..revid:" + newRevisionState.getRevId();
            if ((ret = launcher.launch().cmds(getDescriptor().getBzrExe(), "log", "-v", "-r", version, "--long", "--show-ids")
                    .envs(EnvVars.masterEnvVars).stdout(baos).pwd(workspace).join()) != 0) {
                logger.log(Level.WARNING, "bzr log -v -r returned {0}", ret);
            } else {
                FileOutputStream fos = new FileOutputStream(changeLog);
                fos.write(baos.toByteArray());
                fos.close();
            }
        } catch (IOException e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            logger.log(Level.WARNING, "Failed to poll repository: ", e);
        }
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(
            AbstractProject<?, ?> project, Launcher launcher, FilePath workspace,
            TaskListener listener, SCMRevisionState baseline) throws
            IOException, InterruptedException {
        PrintStream output = listener.getLogger();
        output.printf("Getting current remote revision...");
        final BazaarRevisionState remote = getRevisionState(launcher, listener, source);
        output.println(remote);
        final Change change;
        output.printf("Baseline is %s.\n", baseline);
        if (remote != null && baseline != null && ((baseline == SCMRevisionState.NONE)
            // appears that other instances of None occur - its not a singleton.
            // so do a (fugly) class check.
            || (baseline.getClass() != BazaarRevisionState.class)
            || (!remote.equals(baseline))))
            change = Change.SIGNIFICANT;
        else
            change = Change.NONE;
        return new PollingResult(baseline,remote,change);
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return false;
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
            Launcher launcher, TaskListener listener) throws IOException,
            InterruptedException {
        PrintStream output = listener.getLogger();
        output.println("Getting local revision...");
        BazaarRevisionState local = getRevisionState(launcher, listener, build.getWorkspace().getRemote());
        output.println(local);
        return local;
    }

    @Override
    public boolean checkout(AbstractBuild<?,?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        boolean canUpdate = workspace.act(new FileCallable<Boolean>() {

            private static final long serialVersionUID = 1L;

            public Boolean invoke(File ws, VirtualChannel channel) throws IOException {
                File file = new File(ws, ".bzr");
                return file.exists();
            }
        });

        boolean result = true;
        if (canUpdate) {
            result = update(clean, build, launcher, workspace, listener, changelogFile);
        } else {
            result = clone(build, launcher, workspace, listener, changelogFile);
        }

        build.addAction(new BazaarTagAction(build));

        return result;
    }

    /**
     * Updates the current workspace.
     */
    private boolean update(boolean clean, AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws InterruptedException, IOException {
        BazaarRevisionState oldRevisionState = getRevisionState(launcher, listener, workspace.getRemote());

        boolean hasProblemOccured = false;

        if (clean) {
            hasProblemOccured = ! branch(build, launcher, workspace, listener);
        } else {
            hasProblemOccured = ! pull(build, launcher, workspace, listener);
        }
        if (hasProblemOccured) {
            return false;
        }

        BazaarRevisionState newRevisionState = getRevisionState(launcher, listener, workspace.getRemote());
        if (oldRevisionState != null && newRevisionState != null) {
            getLog(launcher, workspace, oldRevisionState, newRevisionState, changelogFile);
        }

        return true;
    }

    /**
     * Pull the remote branch into workspace.
     */
    private boolean pull(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener) throws InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        String verb = null;
        if (isCheckout()) {
            verb = "update";
            args.add(getDescriptor().getBzrExe(),
                     verb,
                     source);
        } else {
            verb = "pull";
            args.add(getDescriptor().getBzrExe(),
                     verb, "--overwrite",
                     source);
        }
        try {
            if (launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener.getLogger()).pwd(workspace).join() != 0) {
                listener.error("Failed to " + verb);
                return false;
            }
        } catch (IOException e) {
            listener.error("Failed to " + verb);
            return false;
        }

        return true;
    }

    /**
     * Start from scratch and clone the whole repository.
     */
    private boolean clone(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws InterruptedException {
        if (!branch(build, launcher, workspace, listener)) {
            return false;
        }

        return createEmptyChangeLog(changelogFile, listener, "changelog");
    }

    /**
     * Remove the workspace and branch the remote branch into a new one.
     */
    private boolean branch(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener) throws InterruptedException {
        try {
            listener.getLogger().println("Cleaning workspace...");
            workspace.deleteRecursive();
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed to clean the workspace"));
            return false;
        }

        String verb = isCheckout() ? "checkout" : "branch";
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(getDescriptor().getBzrExe(),
                 verb,
                 source, workspace.getRemote());
        try {
            if (launcher.launch().cmds(args).envs(build.getEnvironment(listener)).stdout(listener.getLogger()).join() != 0) {
                listener.error("Failed to " + verb + " " + source);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed to " + verb + " " + source));
            return false;
        }

        return true;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?,?> build, Map<String, String> env) {
        BazaarRevisionState revisionState = (BazaarRevisionState) build.getAction(SCMRevisionState.class);
        if (revisionState != null) {
          if (revisionState.getRevNo() != null) {
            env.put("BZR_REVISION", revisionState.getRevNo());
          }
          if (revisionState.getRevId() != null) {
            env.put("BZR_REVID", revisionState.getRevId());
          }
        }
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new BazaarChangeLogParser();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    public static final class DescriptorImpl extends SCMDescriptor<BazaarSCM> {
        @Extension
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
        private String bzrExe;
        private transient String version;

        private DescriptorImpl() {
            super(BazaarSCM.class, BazaarRepositoryBrowser.class);
            load();
        }

        public String getDisplayName() {
            return "Bazaar";
        }

        /**
         * Path to bazaar executable.
         * @return
         */
        public String getBzrExe() {
            return (bzrExe == null) ? "bzr" : bzrExe;
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            BazaarSCM scm = req.bindJSON(BazaarSCM.class, formData);
            return scm;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            bzrExe = req.getParameter("bazaar.bzrExe");
            version = null;
            save();
            return true;
        }

        public FormValidation doBzrExeCheck(@QueryParameter final String value) throws IOException, ServletException {
            return FormValidation.validateExecutable(value, new FormValidation.FileValidator() {
                @Override public FormValidation validate(File exe) {
                    try {
                       ByteBuffer baos = new ByteBuffer();
                       if (Hudson.getInstance().createLauncher(TaskListener.NULL).launch()
                               .cmds(getBzrExe(), "--version").stdout(baos).join() == 0) {
                          return FormValidation.ok();
                       } else {
                          return FormValidation.warning("Could not locate the executable in path");
                       }
                    } catch (IOException e) {
                        // failed
                    } catch (InterruptedException e) {
                        // failed
                    }
                    return FormValidation.error("Unable to check bazaar version");
                }
            });
        }

        /**
         * UUID version string.
         * This appears to be used for snapshot builds. See issue #1683
         */
        private static final Pattern UUID_VERSION_STRING = Pattern.compile("\\(version ([0-9a-f]+)");
    }
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(BazaarSCM.class.getName());
}
