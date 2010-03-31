/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.bazaar;

import hudson.scm.SCMRevisionState;

/**
 *
 * @author Robert Collins <robertc@robertcollins.net>
 */
public class BazaarRevisionState extends SCMRevisionState {
  // TODO: have this extends AbstractScmTagAction and offer after-the-fact tagging operation

    /**
     * bzr revid from {@code bzr revision-info }
     */
    public final String rev_id;

    public BazaarRevisionState(String rev_id) {
        if (null == rev_id)
            throw new AssertionError("rev_id must not be null");
        this.rev_id = rev_id;
    }

    @Override
    public String toString() {
        return "bazaar revid:" + rev_id;
    }
}
