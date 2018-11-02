package com.some.company.mv2prd.file;

import com.some.company.mv2prd.util.logging.Logger;

public abstract class BasicFile {

    private static final Logger logger = Logger.getLogger(BasicFile.class);

    protected String workingTreeDir;
    protected String relativeGitFilePath;
    protected String absoluteGitFilePath;
    protected String absoluteTargetFilePath;

    protected BasicFile() {
    }

    public BasicFile(String workingTreeDir, String relativeGitFilePath, String absoluteGitFilePath, String absoluteTargetFilePath) {
        this.workingTreeDir = workingTreeDir;
        this.relativeGitFilePath = relativeGitFilePath;
        this.absoluteGitFilePath = absoluteGitFilePath;
        this.absoluteTargetFilePath = absoluteTargetFilePath;
    }

    public String getWorkingTreeDir() {
        return workingTreeDir;
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

    @Override
    public String toString() {
        return "BasicFile{" +
                "workingTreeDir='" + workingTreeDir + '\'' +
                ", relativeGitFilePath='" + relativeGitFilePath + '\'' +
                ", absoluteGitFilePath='" + absoluteGitFilePath + '\'' +
                ", absoluteTargetFilePath='" + absoluteTargetFilePath + '\'' +
                '}';
    }
}
