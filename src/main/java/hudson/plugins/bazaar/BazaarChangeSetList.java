package hudson.plugins.bazaar;

import hudson.scm.ChangeLogSet;
import hudson.model.AbstractBuild;

import java.util.List;
import java.util.Collections;
import java.util.Iterator;

/**
 * List of changeset that went into a particular build.
 * @author Trond Norbye
 */
public class BazaarChangeSetList extends ChangeLogSet<BazaarChangeSet> {

    private final List<BazaarChangeSet> changeSets;

    BazaarChangeSetList(AbstractBuild build, List<BazaarChangeSet> logs) {
        super(build);
        this.changeSets = Collections.unmodifiableList(logs);
        for (BazaarChangeSet log : logs) {
            log.setParent(this);
        }
    }

    public boolean isEmptySet() {
        return changeSets.isEmpty();
    }

    public Iterator<BazaarChangeSet> iterator() {
        return changeSets.iterator();
    }

    public List<BazaarChangeSet> getLogs() {
        return changeSets;
    }

    @Override
    public String getKind() {
        return "bzr";
    }
}
