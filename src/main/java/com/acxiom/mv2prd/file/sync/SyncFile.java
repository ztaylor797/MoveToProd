package com.some.company.mv2prd.file.sync;

import com.some.company.mv2prd.file.BasicFile;
import com.some.company.mv2prd.json.Mv2prdHooks;
import com.some.company.mv2prd.util.GitUtils;
import com.some.company.mv2prd.util.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class SyncFile extends BasicFile {

    private static final Logger logger = Logger.getLogger(SyncFile.class);

    private SyncFileStatus syncFileStatus;
    private SyncFileSource syncFileSource;

    protected SyncFile(SyncFileSource syncFileSource, String absoluteFilePath, String workingTreeDir, Mv2prdHooks mv2prdHooks) {

        this.syncFileStatus = SyncFileStatus.PENDING;
        this.syncFileSource = syncFileSource;
        this.workingTreeDir = workingTreeDir;

        if (syncFileSource == SyncFileSource.SCM) {
            this.absoluteGitFilePath = absoluteFilePath;
            this.relativeGitFilePath = GitUtils.determineRelativeGitFilePathFromAbsGit(this.absoluteGitFilePath, mv2prdHooks.getGitToTargetMap());
            this.absoluteTargetFilePath = GitUtils.buildAbsoluteTargetFilePath(this.relativeGitFilePath, mv2prdHooks.getGitToTargetMap());
        } else if (syncFileSource == SyncFileSource.TAR) {
            this.absoluteTargetFilePath = absoluteFilePath;
            this.relativeGitFilePath = GitUtils.determineRelativeGitFilePathFromAbsTarget(this.absoluteTargetFilePath, mv2prdHooks.getGitToTargetMap());
            this.absoluteGitFilePath = GitUtils.buildAbsoluteGitFilePath(this.workingTreeDir, this.relativeGitFilePath);
        } else
            this.syncFileStatus = SyncFileStatus.COMP_FAILED;
    }

    public SyncFileStatus execCompare() {

        // If constructor failed, then don't try to compare and just return failed still
        if (this.syncFileStatus == SyncFileStatus.COMP_FAILED) {
            return this.syncFileStatus;
        }

        Path gitFilePath = Paths.get(this.absoluteGitFilePath);
        Path targetFilePath = Paths.get(this.absoluteTargetFilePath);

        if (Files.exists(gitFilePath) && Files.exists(targetFilePath)) {
            // Check for differences in size
            try {
                if (Files.size(gitFilePath) == Files.size(targetFilePath)) {
                    this.syncFileStatus = SyncFileStatus.SYNCED;
                } else
                    this.syncFileStatus = SyncFileStatus.DIFF;
            } catch (IOException e) {
                logger.error("execCompare: Could not compare file sizes: " + gitFilePath.toString() + " <-> " + targetFilePath);
                this.syncFileStatus = SyncFileStatus.COMP_FAILED;
            }
        } else if (Files.exists(gitFilePath)) {
            this.syncFileStatus = SyncFileStatus.SCM_ONLY;
        } else if (Files.exists(targetFilePath)) {
            this.syncFileStatus = SyncFileStatus.TAR_ONLY;
        } else {
            this.syncFileStatus = SyncFileStatus.MISSING_BOTH;
        }

        return this.syncFileStatus;
    }

    public SyncFileStatus getSyncFileStatus() {
        return syncFileStatus;
    }

    public SyncFileSource getSyncFileSource() {
        return syncFileSource;
    }

    // Will sort by FileChangeResult, AbsoluteTargetFilePath descending
    public static Comparator<SyncFile> resultComparator = (fc1, fc2) -> {
        if (fc1.getSyncFileStatus() == fc2.getSyncFileStatus())
            // case insensitive
            return fc1.getRelativeGitFilePath().toLowerCase().compareTo(fc2.getRelativeGitFilePath().toLowerCase());
        else
            return fc1.getSyncFileStatus().compareTo(fc2.getSyncFileStatus());
    };

    @Override
    public String toString() {
        return "SyncFile{" +
                "syncFileStatus=" + syncFileStatus +
                ", syncFileSource=" + syncFileSource +
                ", workingTreeDir='" + workingTreeDir + '\'' +
                ", relativeGitFilePath='" + relativeGitFilePath + '\'' +
                ", absoluteGitFilePath='" + absoluteGitFilePath + '\'' +
                ", absoluteTargetFilePath='" + absoluteTargetFilePath + '\'' +
                '}';
    }

    // This only compares the objects on the three path members and status. It does not check the source.
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof SyncFile))
            return false;

        SyncFile other = (SyncFile) obj;
        if (this.getRelativeGitFilePath().equals(other.getRelativeGitFilePath()))
            if (this.getAbsoluteGitFilePath().equals(other.getAbsoluteGitFilePath()))
                if (this.getAbsoluteTargetFilePath().equals(other.getAbsoluteTargetFilePath()))
                    if (this.getSyncFileStatus().equals(other.getSyncFileStatus()))
                        return true;

        return false;
    }

    @Override
    public int hashCode() {
        return (this.getRelativeGitFilePath() + this.getAbsoluteGitFilePath() + this.getAbsoluteTargetFilePath() + this.getSyncFileStatus()).hashCode();
    }
}
