package com.some.company.mv2prd.file.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncFileSorter {
    List<SyncFile> syncFileList = new ArrayList<>();

    public SyncFileSorter(List<SyncFile> syncFileList) {
        this.syncFileList = syncFileList;
    }

    public List<SyncFile> getSortedSyncFilesByStatusAndGitPath() {
        Collections.sort(this.syncFileList, SyncFile.resultComparator);
        return this.syncFileList;
    }
}
