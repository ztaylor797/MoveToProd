package com.some.company.mv2prd.file.install;

import com.some.company.mv2prd.json.Mv2prdHooks;
import com.some.company.mv2prd.util.logging.Logger;
import org.eclipse.jgit.diff.DiffEntry;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;

public class CopyFileChange extends FileChange {

    private static final Logger logger = Logger.getLogger(CopyFileChange.class);

    protected CopyFileChange(Boolean dryRun, String gitTag, DiffEntry diffEntry, String workingTreeDir, UserPrincipal userPrincipal, GroupPrincipal groupPrincipal, String posixFilePermissionString, Mv2prdHooks mv2prdHooks) {
        super(dryRun, gitTag, diffEntry, workingTreeDir, userPrincipal, groupPrincipal, posixFilePermissionString, mv2prdHooks);
    }

    @Override
    protected FileChangeResult execPrimaryChange(Boolean dryRun, String absoluteGitFilePath,
                                                 String absoluteTargetFilePath, UserPrincipal userPrincipal,
                                                 GroupPrincipal groupPrincipal, String posixFilePermissionString) {
        return copyFile(dryRun, absoluteGitFilePath, absoluteTargetFilePath, userPrincipal, groupPrincipal, posixFilePermissionString);
    }

    @Override
    String getClassType() {
        return "COPY";
    }
}
