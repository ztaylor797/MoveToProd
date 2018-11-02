package com.some.company.mv2prd.json;

import java.util.List;
import java.util.Map;

// This class maps to mv2prd_hooks.json
public class Mv2prdHooks {
    private String logFile;
    private String backupDirectory;
    private Map<String,String> gitToTargetMap;
    private List<String> syncFilterList;
    private List<String> emailAddressList;

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    public Map<String, String> getGitToTargetMap() {
        return gitToTargetMap;
    }

    public void setGitToTargetMap(Map<String, String> gitToTargetMap) {
        this.gitToTargetMap = gitToTargetMap;
    }

    public List<String> getSyncFilterList() {
        return syncFilterList;
    }

    public void setSyncFilterList(List<String> syncFilterList) {
        this.syncFilterList = syncFilterList;
    }

    public List<String> getEmailAddressList() {
        return emailAddressList;
    }

    public void setEmailAddressList(List<String> emailAddressList) {
        this.emailAddressList = emailAddressList;
    }

    @Override
    public String toString() {
        return "Mv2prdHooks{" +
                "logFile='" + logFile + '\'' +
                ", backupDirectory='" + backupDirectory + '\'' +
                ", gitToTargetMap=" + gitToTargetMap +
                ", syncFilterList=" + syncFilterList +
                ", emailAddressList=" + emailAddressList +
                '}';
    }
}
