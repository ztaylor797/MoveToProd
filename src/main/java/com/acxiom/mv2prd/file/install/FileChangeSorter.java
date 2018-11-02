package com.some.company.mv2prd.file.install;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileChangeSorter {
    List<FileChange> fileChangeList = new ArrayList<>();

    public FileChangeSorter(List<FileChange> fileChangeList) {
        this.fileChangeList = fileChangeList;
    }

    public List<FileChange> getSortedFileChangesByResultAndGitPath() {
        Collections.sort(this.fileChangeList, FileChange.resultComparator);
        return this.fileChangeList;
    }
}
