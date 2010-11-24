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

    private final String revNo;
    private final String rev_id;

    public BazaarRevisionState(String revNo, String revId) {
        this.revNo = revNo;
        this.rev_id = revId;
    }

    public String getRevNo() {
        return this.revNo;
    }

    public String getRevId() {
        return this.rev_id;
    }

    @Override
    public String toString() {
        return "RevisionState revno:" + this.revNo + " revid:" + this.rev_id;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other instanceof BazaarRevisionState) {
            BazaarRevisionState that = (BazaarRevisionState) other;
            result = this.rev_id.equals(that.rev_id);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return this.rev_id.hashCode();
    }
}
