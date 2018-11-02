package com.some.company.mv2prd.file.install;

import com.some.company.mv2prd.json.Mv2prdHooks;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.FileMode;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;

public class FileChangeFactory {
    public static FileChange getFileChange(Boolean dryRun, String gitTag, DiffEntry diffEntry,
                                           String workingTreeDir, UserPrincipal userPrincipal,
                                           GroupPrincipal groupPrincipal, String posixFilePermissionString,
                                           Mv2prdHooks mv2prdHooks) {
        if (diffEntry.getNewMode() == FileMode.MISSING)
            return new DeleteFileChange(dryRun, gitTag, diffEntry, workingTreeDir, userPrincipal, groupPrincipal, posixFilePermissionString, mv2prdHooks);
        else
            return new CopyFileChange(dryRun, gitTag, diffEntry, workingTreeDir, userPrincipal, groupPrincipal, posixFilePermissionString, mv2prdHooks);
    }
}
