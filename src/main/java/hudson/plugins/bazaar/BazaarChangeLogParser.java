package hudson.plugins.bazaar;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Parses the output of bzr log.
 * 
 * @author Trond Norbye
 */
public class BazaarChangeLogParser extends ChangeLogParser {

    public BazaarChangeSetList parse(AbstractBuild build, File changelogFile) throws IOException {
        ArrayList<BazaarChangeSet> entries = new ArrayList<BazaarChangeSet>();

        BufferedReader in = new BufferedReader(new FileReader(changelogFile));
        StringBuilder message = new StringBuilder();
        String s;

        BazaarChangeSet entry = null;
        int state = 0;
        int ident = 0;
        while ((s = in.readLine()) != null) {
            int nident = 0;
            int len = s.length();
            while (nident < len && s.charAt(nident) == ' ') {
                ++nident;
            }

            s = s.trim();
            if ("------------------------------------------------------------".equals(s)) {
                if (entry != null && state > 2) {
                    entries.add(entry);
                }
                entry = new BazaarChangeSet();
                state = 0;
                message.setLength(0);
                ident = nident;
                continue;
            }

            switch (state) {
                case 0:
                    if (ident == nident && s.startsWith("revno:")) {
                        String rev = s.substring("revno:".length()).trim();
                        entry.setRevno(rev);
                        ++state;
                    }
                    break;
                case 1:
                    if (ident == nident && s.startsWith("revision-id:")) {
                        String rev = s.substring("revision-id:".length()).trim();
                        entry.setRevid(rev);
                        ++state;
                    }
                    break;
                case 2:
                    if (ident == nident && s.startsWith("committer:")) {
                        entry.setAuthor(s.substring("committer:".length()).trim());
                        ++state;
                    }
                    break;
                case 3:
                    if (ident == nident && s.startsWith("timestamp:")) {
                        entry.setDate(s.substring("timestamp:".length()).trim());
                        ++state;
                    }
                    break;
                case 4:
                    if (!(ident == nident && s.startsWith("message:"))) {
                        if (ident == nident && (s.startsWith("modified:") || s.startsWith("added:") || s.startsWith("removed:"))) {
                            if (s.startsWith("modified")) {
                                state = 5;
                            } else if (s.startsWith("added:")) {
                                state = 6;
                            } else if (s.startsWith("removed:")) {
                                state = 7;
                            }
                            entry.setMsg(message.toString());
                            message.setLength(0);
                        } else {
                            message.append(s);
                            message.append("\n");
                        }
                    }
                    break;
                case 5:
                    if (s.startsWith("modified")) {
                        state = 5;
                    } else if (s.startsWith("added:")) {
                        state = 6;
                    } else if (s.startsWith("removed:")) {
                        state = 7;
                    } else {
                        int idx = s.indexOf(" => ");
                        if (idx != -1) {
                            s = s.substring(idx + 4);
                        }
                        entry.getModifiedPaths().add(s);

                    }

                    break;
                case 6:
                    if (s.startsWith("modified")) {
                        state = 5;
                    } else if (s.startsWith("added:")) {
                        state = 6;
                    } else if (s.startsWith("removed:")) {
                        state = 7;
                    } else {
                        int idx = s.indexOf(" => ");
                        if (idx != -1) {
                            s = s.substring(idx + 4);
                        }
                        entry.getAddedPaths().add(s);
                    }

                    break;
                case 7:
                    if (s.startsWith("modified")) {
                        state = 5;
                    } else if (s.startsWith("added:")) {
                        state = 6;
                    } else if (s.startsWith("removed:")) {
                        state = 7;
                    } else {
                        int idx = s.indexOf(" => ");
                        if (idx != -1) {
                            s = s.substring(idx + 4);
                        }
                        entry.getDeletedPaths().add(s);
                    }

                    break;

                default: {
                    Logger logger = Logger.getLogger(BazaarChangeLogParser.class.getName());
                    logger.warning("Unknown parser state: " + state);
                }
            }
        }

        if (entry != null && state > 2) {
            entries.add(entry);
        }

        return new BazaarChangeSetList(build, entries);
    }
}
