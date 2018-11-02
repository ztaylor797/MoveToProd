package com.some.company.mv2prd.file.install;

import com.some.company.mv2prd.file.BasicFile;
import com.some.company.mv2prd.json.Mv2prdHooks;
import com.some.company.mv2prd.util.GitUtils;
import com.some.company.mv2prd.util.Utils;
import com.some.company.mv2prd.util.logging.Logger;
import org.eclipse.jgit.diff.DiffEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.Comparator;

public abstract class FileChange extends BasicFile {

    private static final Logger logger = Logger.getLogger(FileChange.class);

    private Boolean wasExecuted;
    private Boolean dryRun;

    protected DiffEntry diffEntry;

    private String gitTag;
    private UserPrincipal userPrincipal;
    private GroupPrincipal groupPrincipal;
    private String posixFilePermissionString;
    private String absoluteBackupFilePath;

    private FileChangeResult fileChangeResult;

    protected FileChange(Boolean dryRun, String gitTag, DiffEntry diffEntry, String workingTreeDir,
                         UserPrincipal userPrincipal, GroupPrincipal groupPrincipal,
                         String posixFilePermissionString, Mv2prdHooks mv2prdHooks) {

        this.diffEntry = diffEntry;
        this.workingTreeDir = workingTreeDir;
        this.wasExecuted = false;
        this.dryRun = dryRun;
        this.diffEntry = diffEntry;
        this.gitTag = gitTag;

        this.userPrincipal = userPrincipal;
        this.groupPrincipal = groupPrincipal;
        this.posixFilePermissionString = posixFilePermissionString;

        this.relativeGitFilePath = GitUtils.determineRelativeGitFilePathFromDiffEntry(this.diffEntry);
        this.absoluteGitFilePath = GitUtils.buildAbsoluteGitFilePath(this.workingTreeDir, this.relativeGitFilePath);
        this.absoluteTargetFilePath = GitUtils.buildAbsoluteTargetFilePath(this.relativeGitFilePath, mv2prdHooks.getGitToTargetMap());
        this.absoluteBackupFilePath = GitUtils.buildAbsoluteBackupFilePath(this.relativeGitFilePath, this.gitTag, mv2prdHooks.getBackupDirectory());

        this.fileChangeResult = FileChangeResult.PENDING;
    }

    ////////////////////////////////////
    public Boolean execChange() {
        if (this.wasExecuted) {
            // If it has already been executed we don't set the fileChangeResult again, just leave it alone.
            logger.warn("execChange has already been called once on this " + FileChange.class + " object.");
            return false;
        }
        this.wasExecuted = true;
        // If the target is null, then it didn't match to any entries in the gitToTarget map so we won't move it
        if (this.absoluteTargetFilePath == null) {
            this.fileChangeResult = FileChangeResult.NOT_APPLICABLE;
            return false;
        }
        // Only back the file up if the target file exists
        if (Files.exists(Paths.get(this.absoluteTargetFilePath))) {
            this.fileChangeResult = backupFile(this.dryRun, this.absoluteTargetFilePath, this.absoluteBackupFilePath, this.userPrincipal, this.groupPrincipal, this.posixFilePermissionString);
            if (this.fileChangeResult == FileChangeResult.BACKUP_FAILED)
                return false;
        }
        this.fileChangeResult = execPrimaryChange(this.dryRun, this.absoluteGitFilePath, this.absoluteTargetFilePath, this.userPrincipal, this.groupPrincipal, this.posixFilePermissionString);
        if (this.fileChangeResult != FileChangeResult.SUCCESS || this.fileChangeResult != FileChangeResult.DELETE_SKIPPED)
            return false;

        return true;
    }
    ////////////////////////////////////

    private FileChangeResult backupFile(Boolean dryRun, String absoluteTargetFilePath, String absoluteBackupFilePath, UserPrincipal userPrincipal, GroupPrincipal groupPrincipal, String posixFilePermissionString) {
        try {
            if (!dryRun) Utils.copyFile(absoluteTargetFilePath, absoluteBackupFilePath, userPrincipal, groupPrincipal, posixFilePermissionString);
        } catch (IOException e) {
            logger.error("backupFile: Could not back up source file: " + absoluteTargetFilePath + " to backup: " + absoluteBackupFilePath);
            return FileChangeResult.BACKUP_FAILED;
        }
        return FileChangeResult.PENDING;
    }

    abstract FileChangeResult execPrimaryChange(Boolean dryRun, String absoluteGitFilePath,
                                        String absoluteTargetFilePath, UserPrincipal userPrincipal, GroupPrincipal groupPrincipal, String posixFilePermissionString);

    protected FileChangeResult copyFile(Boolean dryRun, String absoluteGitFilePath, String absoluteTargetFilePath, UserPrincipal userPrincipal, GroupPrincipal groupPrincipal, String posixFilePermissionString) {
        try {
            if (!dryRun) Utils.copyFile(absoluteGitFilePath, absoluteTargetFilePath, userPrincipal, groupPrincipal, posixFilePermissionString);
        } catch (IOException e) {
            logger.error("copyFile: Could not copy git file: " + absoluteGitFilePath + " to target: " + absoluteTargetFilePath);
            return FileChangeResult.COPY_FAILED;
        }
        try {
            if (!dryRun) Utils.chownFile(absoluteTargetFilePath, userPrincipal);
        } catch (IOException e) {
            logger.error("copyFile: Could not chown target: " + absoluteTargetFilePath + " to " + userPrincipal.getName());
            return FileChangeResult.CHOWN_FAILED;
        }
        try {
            if (!dryRun) Utils.chgrpFile(absoluteTargetFilePath, groupPrincipal);
        } catch (IOException e) {
            logger.error("copyFile: Could not change group of target: " + absoluteTargetFilePath + " to " + groupPrincipal);
            return FileChangeResult.CHGRP_FAILED;
        }
        try {
            if (!dryRun) Utils.chmodFile(absoluteTargetFilePath, posixFilePermissionString);
        } catch (IOException e) {
            logger.error("copyFile: Could not chmod " + posixFilePermissionString + " target: " + absoluteTargetFilePath);
            return FileChangeResult.CHMOD_FAILED;
        }
        return FileChangeResult.SUCCESS;
    }

    protected FileChangeResult deleteFile(Boolean dryRun, String absoluteTargetFilePath) {
        if (!Files.exists(Paths.get(this.absoluteTargetFilePath))) {
            logger.warn("deleteFile: File to delete does not exist, skipping it: " + absoluteTargetFilePath);
            return FileChangeResult.DELETE_SKIPPED;
        }
        try {
            if (!dryRun) Utils.deleteFileWithUserConfirmation(absoluteTargetFilePath);
        } catch (IOException e) {
            logger.error("deleteFile: Could not remove target file: " + absoluteTargetFilePath);
            return FileChangeResult.DELETE_FAILED;
        }
        return FileChangeResult.SUCCESS;
    }

    abstract String getClassType();

    public Boolean getWasExecuted() {
        return wasExecuted;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public String getGitTag() {
        return gitTag;
    }

    public DiffEntry getDiffEntry() {
        return diffEntry;
    }

    public String getWorkingTreeDir() {
        return workingTreeDir;
    }

    public UserPrincipal getUserPrincipal() {
        return userPrincipal;
    }

    public GroupPrincipal getGroupPrincipal() {
        return groupPrincipal;
    }

    public String getPosixFilePermissionString() {
        return posixFilePermissionString;
    }

    public String getRelativeGitFilePath() {
        return relativeGitFilePath;
    }

    public String getAbsoluteGitFilePath() {
        return absoluteGitFilePath;
    }

    public String getAbsoluteTargetFilePath() {
        return absoluteTargetFilePath;
    }

    public String getAbsoluteBackupFilePath() {
        return absoluteBackupFilePath;
    }

    public FileChangeResult getFileChangeResult() {
        return fileChangeResult;
    }

    // Will sort by FileChangeResult, AbsoluteTargetFilePath descending
    public static Comparator<FileChange> resultComparator = (fc1, fc2) -> {
        if (fc1.getFileChangeResult() == fc2.getFileChangeResult())
            // case insensitive
            return fc1.getRelativeGitFilePath().toLowerCase().compareTo(fc2.getRelativeGitFilePath().toLowerCase());
        else
            return fc1.getFileChangeResult().compareTo(fc2.getFileChangeResult());
    };

    @Override
    public String toString() {
        return "FileChange{" +
                "wasExecuted=" + wasExecuted +
                ", dryRun=" + dryRun +
                ", gitTag='" + gitTag + '\'' +
                ", diffEntry=" + diffEntry +
                ", workingTreeDir='" + workingTreeDir + '\'' +
                ", userPrincipal=" + userPrincipal +
                ", groupPrincipal=" + groupPrincipal +
                ", posixFilePermissionString='" + posixFilePermissionString + '\'' +
                ", relativeGitFilePath='" + relativeGitFilePath + '\'' +
                ", absoluteGitFilePath='" + absoluteGitFilePath + '\'' +
                ", absoluteTargetFilePath='" + absoluteTargetFilePath + '\'' +
                ", absoluteBackupFilePath='" + absoluteBackupFilePath + '\'' +
                ", fileChangeResult=" + fileChangeResult +
                '}';
    }
}
