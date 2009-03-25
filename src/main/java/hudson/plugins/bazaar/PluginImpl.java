package hudson.plugins.bazaar;

import hudson.Plugin;
import hudson.scm.SCMS;

/**
 * Plugin entry point.
 *
 * @author Trond Norbye
 * @plugin
 */
public class PluginImpl extends Plugin {
    @Override
    public void start() throws Exception {
        SCMS.SCMS.add(BazaarSCM.DescriptorImpl.DESCRIPTOR);
    }
}
