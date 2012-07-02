/*
 * The MIT License
 *
 * Copyright (C) 2009 Trond Norbye
 * Copyright (C) 2010 Robert Collins
 * Copyright (C) 2010-2011 Monty Taylor <mordred@inaugust.com>
 * Copyright (C) 2011 Stewart Smith <stewart@flamingspork.com>
 * Copyright (C) 2010-2011 Alexandre Garnier <zigarn@dev.java.net>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.bazaar;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.scm.EditType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses the output of bzr log.
 * 
 * @author Trond Norbye
 */
public class BazaarChangeLogParser extends ChangeLogParser {

    public BazaarChangeSetList parse(AbstractBuild build, File changelogFile) throws IOException {
        List<BazaarChangeSet> entries = new ArrayList<BazaarChangeSet>();

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
            len = s.length();
            if ("------------------------------------------------------------".equals(s)) {
                if (entry != null && state > 2) {
                    if (message.length() != 0) {
                        entry.setMsg(message.toString());
                    }
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
                        if (rev.contains("[merge]")) {
                            entry.setMerge(true);
                            rev = rev.substring(0, rev.length() - "[merge]".length()).trim();
                        }
                        entry.setRevno(rev);
                        ++state;
                    }
                    break;
                case 1:
                    if (ident == nident && s.startsWith("tags:")) {
                        String tags = s.substring("tags:".length()).trim();
                        entry.setTags(Arrays.asList(tags.split(", ")));
                    }
                    if (ident == nident && s.startsWith("revision-id:")) {
                        String rev = s.substring("revision-id:".length()).trim();
                        entry.setRevid(rev);
                        ++state;
                    }
                    break;
                case 2:
                    if (ident == nident && s.startsWith("committer:")) {
                        int emailStartIndex = s.indexOf('<');
                        String author = s.substring("committer:".length(), emailStartIndex < 0 ? len : emailStartIndex).trim();
                        entry.setAuthor(author);
                        if (emailStartIndex >= 0) {
                            int emailEndIndex = s.indexOf('>');
                            if (emailEndIndex >= emailStartIndex) {
                                String authorEmail = s.substring(1 + emailStartIndex, emailEndIndex).trim();
                                entry.setAuthorEmail(authorEmail);
                            }
                        }
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
                        if (ident == nident && (s.startsWith("modified:") || s.startsWith("added:") || s.startsWith("removed:") || s.startsWith("renamed:"))) {
                            if (s.startsWith("modified")) {
                                state = 5;
                            } else if (s.startsWith("added:")) {
                                state = 6;
                            } else if (s.startsWith("removed:")) {
                                state = 7;
                            } else if (s.startsWith("renamed:")){
                                state = 8;
                            }
                            entry.setMsg(message.toString());
                            message.setLength(0);
                        } else {
                            if (message.length() != 0) {
                                message.append("\n");
                            }
                            message.append(s);
                        }
                    }
                    break;
                case 5: // modified
                    if (s.startsWith("modified")) {
                        state = 5;
                    } else if (s.startsWith("added:")) {
                        state = 6;
                    } else if (s.startsWith("removed:")) {
                        state = 7;
                    } else if (s.startsWith("renamed:")){
                        state = 8;
                    } else {
                        entry.addAffectedFile(createAffectedFile(EditType.EDIT, s));

                    }

                    break;
                case 6: // added
                    if (s.startsWith("modified")) {
                        state = 5;
                    } else if (s.startsWith("added:")) {
                        state = 6;
                    } else if (s.startsWith("removed:")) {
                        state = 7;
                    } else if (s.startsWith("renamed:")){
                        state = 8;
                    } else {
                        entry.addAffectedFile(createAffectedFile(EditType.ADD, s));
                    }

                    break;
                case 7: // removed
                    if (s.startsWith("modified")) {
                        state = 5;
                    } else if (s.startsWith("added:")) {
                        state = 6;
                    } else if (s.startsWith("removed:")) {
                        state = 7;
                    } else if (s.startsWith("renamed:")){
                        state = 8;
                    } else {
                        entry.addAffectedFile(createAffectedFile(EditType.DELETE, s));
                    }

                    break;
                case 8: // renamed
                    if (s.startsWith("modified")) {
                        state = 5;
                    } else if (s.startsWith("added:")) {
                        state = 6;
                    } else if (s.startsWith("removed:")) {
                        state = 7;
                    } else if (s.startsWith("renamed:")){
                        state = 8;
                    } else {
                        entry.addAffectedFile(createAffectedFile(EditType.EDIT, s));
                    }

                    break;

                default: {
                    Logger logger = Logger.getLogger(BazaarChangeLogParser.class.getName());
                    logger.warning("Unknown parser state: " + state);
                }
            }
        }

        if (entry != null && state > 2) {
            if (message.length() != 0) {
                entry.setMsg(message.toString());
            }
            entries.add(entry);
        }

        // Remove current revision entry
        entries = entries.subList(0, Math.max(0 ,entries.size() -1));

        in.close();
        return new BazaarChangeSetList(build, entries);
    }

    private BazaarAffectedFile createAffectedFile(EditType editType, String changelogLine) {
        String oldPath = null;
        String path = changelogLine.trim();
        String fileId = "";
        int index = changelogLine.lastIndexOf(' ');
        if (index >= 0) {
            path = changelogLine.substring(0, index).trim();
            fileId = changelogLine.substring(index, changelogLine.length()).trim();
        }
        if (path.contains("=>")) {
            String[] paths = path.split("=>");
            oldPath = paths[0].trim();
            path = paths[1].trim();
        }
        return new BazaarAffectedFile(editType, oldPath, path, fileId);
    }
}
