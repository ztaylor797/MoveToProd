package com.some.company.mv2prd.file.install;

import com.some.company.mv2prd.json.Mv2prdHooks;
import com.some.company.mv2prd.util.Utils;
import com.some.company.mv2prd.util.logging.Logger;
import org.eclipse.jgit.diff.DiffEntry;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;

public class FileChangeUtils {

    private static final Logger logger = Logger.getLogger(FileChangeUtils.class);

    public static List<FileChange> executeAllFileChanges(Boolean dryRun, String gitTag, List<DiffEntry> diffEntryList, String workingTreeDir, UserPrincipal userPrincipal, GroupPrincipal groupPrincipal, Mv2prdHooks mv2prdHooks, String posixFilePermissionString) {
        List<FileChange> fileChangeList = new ArrayList<>();

        for (DiffEntry diffEntry : diffEntryList) {
//            logger.debug("\n" + diffEntry.getChangeType().toString() + "\nnewpath: " + diffEntry.getNewPath()
//                    + "\noldpath: " + diffEntry.getOldPath() + "\n");

            FileChange fileChange = FileChangeFactory.getFileChange(dryRun, gitTag, diffEntry, workingTreeDir, userPrincipal, groupPrincipal, posixFilePermissionString, mv2prdHooks);
            fileChange.execChange();
            fileChangeList.add(fileChange);
        }

        return fileChangeList;
    }

    public static StringBuilder prettyPrintFileChangeList(List<FileChange> fileChangeList) {
        int col1Len = getMaxLengthChangeResult(fileChangeList);
        int col2Len = getMaxLengthRelativeGit(fileChangeList);
        int col3Len = getMaxLengthAbsoluteTarget(fileChangeList);

        StringBuilder sb = new StringBuilder();
        sb.append("Entries below:\n\n---------- FileChangeList START ----------\n");
        List<FileChange> sortedFileChangeList = new FileChangeSorter(fileChangeList).getSortedFileChangesByResultAndGitPath();
        for (FileChange fileChange : sortedFileChangeList) {
            sb.append(String.format("RESULT: %-" + col1Len + "s | GIT: %-" + col2Len + "s | TAR: %-" + col3Len + "s\n",fileChange.getFileChangeResult().toString(), fileChange.getRelativeGitFilePath(),  fileChange.getAbsoluteTargetFilePath()));
        }
        sb.append("---------- FileChangeList  END  ----------\n\n");
        return sb;
    }

    public static int getMaxLengthChangeResult(List<FileChange> fileChangeList) {
        int maxLen = 1;
        for (FileChange fileChange : fileChangeList) {
            int len = fileChange.getFileChangeResult().toString().length();
            maxLen = (maxLen > len) ? maxLen : len;
        }
        return maxLen;
    }

    public static int getMaxLengthRelativeGit(List<FileChange> fileChangeList) {
        int maxLen = 1;
        for (FileChange fileChange : fileChangeList) {
            int len = fileChange.getRelativeGitFilePath().toString().length();
            maxLen = (maxLen > len) ? maxLen : len;
        }
        return maxLen;
    }

    public static int getMaxLengthAbsoluteTarget(List<FileChange> fileChangeList) {
        int maxLen = 1;
        for (FileChange fileChange : fileChangeList) {
            int len = fileChange.getAbsoluteTargetFilePath().toString().length();
            maxLen = (maxLen > len) ? maxLen : len;
        }
        return maxLen;
    }

    public static StringBuilder generatePostExecReport(Boolean dryRun, List<FileChange> fileChangeListPostExec) {
        int col1Len = getMaxLengthChangeResult(fileChangeListPostExec);
        int col2Len = getMaxLengthRelativeGit(fileChangeListPostExec);
        int col3Len = getMaxLengthAbsoluteTarget(fileChangeListPostExec);
        String formatString = "%-6s  %-" + col1Len + "s  %-" + col2Len + "s  %-" + col3Len + "s\n";

        StringBuilder sb = new StringBuilder();

        for (Enum e : FileChangeResult.values()) {
            List<FileChange> tempFileChangeList = new ArrayList<>();

            for (FileChange fileChange : fileChangeListPostExec) {
                if (fileChange.getFileChangeResult() == e) {
                    tempFileChangeList.add(fileChange);
                }
            }

            if (tempFileChangeList.size() > 0) {
                sb.append("--- The following file changes are in state: " + e + " ---\n\n"
                        + String.format(
                                formatString,
                                "OpType",
                                "Result",
                                "GIT Path",
                                "Target Path"
                        )
                        + "\n");
                FileChangeSorter fileChangeSorter = new FileChangeSorter(tempFileChangeList);
                List<FileChange> sortedFileChangeList = fileChangeSorter.getSortedFileChangesByResultAndGitPath();
                for (FileChange fileChange : sortedFileChangeList) {
                    sb.append(String.format(
                            formatString,
                            fileChange.getClassType(),
                            fileChange.getFileChangeResult(),
                            fileChange.getRelativeGitFilePath(),
                            fileChange.getAbsoluteTargetFilePath()
                    ));
                }
                sb.append("\n\n");
            }
        }

        int maxLen = Utils.getLongestLineLengthInStringBuilder(sb);
        int halfLen = (maxLen / 2) - 10;

        StringBuilder header = new StringBuilder("Report below:\n\n");
        StringBuilder dryRunHeader = new StringBuilder();
        if (dryRun) dryRunHeader = new StringBuilder(Utils.printNTimes(" ",halfLen+1) + "*** DRY RUN ONLY ***" + Utils.printNTimes(" ",halfLen+1) + "\n\n");
        StringBuilder innerHeaderSb = new StringBuilder(
                        "   "
                        + Utils.printNTimes("_",halfLen-3)
                        + " M2PRD INSTALL REPORT "
                        + Utils.printNTimes("_",halfLen-3)
                        + "   \n\n")
                .append(dryRunHeader);

        StringBuilder finalSb = header
                .append(
                        Utils.boxitizeMe(
                                innerHeaderSb
                                        .append(sb)) + "\n");
        return finalSb;
    }

}
