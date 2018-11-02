package com.some.company.mv2prd.file.sync;

import com.some.company.mv2prd.json.Mv2prdHooks;
import com.some.company.mv2prd.util.GitUtils;
import com.some.company.mv2prd.util.logging.Logger;
import com.some.company.mv2prd.util.Utils;
import org.apache.commons.text.WordUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SyncFileUtils {

    private static final Logger logger = Logger.getLogger(SyncFileUtils.class);

    private static List<SyncFile> getAllTargetFiles(Mv2prdHooks mv2prdHooks, String workingTreeDir) {
        List<SyncFile> syncFileList = new ArrayList<>();

        for (Map.Entry<String,String> entry : mv2prdHooks.getGitToTargetMap().entrySet()) {
            Path targetPath = Paths.get(entry.getValue());
            logger.info("Parsing target directory: " + targetPath.toString());
            List<Path> pathList = Utils.getAllFilePathsInDirectory(targetPath);
            for (Path path : pathList)
            syncFileList.add(new SyncFile(SyncFileSource.TAR, path.toString(), workingTreeDir, mv2prdHooks));
        }
        return syncFileList;
    }

    private static List<SyncFile> getAllGitFiles(Mv2prdHooks mv2prdHooks, String workingTreeDir) {
        List<SyncFile> syncFileList = new ArrayList<>();

        for (Map.Entry<String,String> entry : mv2prdHooks.getGitToTargetMap().entrySet()) {
            Path gitPath = Paths.get(GitUtils.buildAbsoluteGitFilePath(workingTreeDir, entry.getKey()));
            logger.info("Parsing git directory: " + gitPath.toString());
            List<Path> pathList = Utils.getAllFilePathsInDirectory(gitPath);
            for (Path path : pathList)
                syncFileList.add(new SyncFile(SyncFileSource.SCM, path.toString(), workingTreeDir, mv2prdHooks));
        }
        return syncFileList;
    }

    public static List<SyncFile> getFilteredSyncFiles(Mv2prdHooks mv2prdHooks, String workingTreeDir) {
        List<SyncFile> gitSyncFileList = getAllGitFiles(mv2prdHooks, workingTreeDir);
        List<SyncFile> tarSyncFileList = getAllTargetFiles(mv2prdHooks, workingTreeDir);

        List<SyncFile> withDupsSyncFileList = combineTwoLists(gitSyncFileList, tarSyncFileList);
        List<SyncFile> dedupSyncFileList = dedupSyncFileList(withDupsSyncFileList);

        List<SyncFile> filtDedupSyncFileList = filterSyncFileList(dedupSyncFileList, mv2prdHooks.getSyncFilterList());

        return filtDedupSyncFileList;
    }

    public static List<SyncFile> combineTwoLists(List<SyncFile> list1, List<SyncFile> list2) {
        list1.addAll(list2);
        return list1;
    }

    // This removes duplicates using the overridden equals method in SyncFile
    public static List<SyncFile> dedupSyncFileList(List<SyncFile> withDupsSyncFileList) {
        return new ArrayList<>(new LinkedHashSet<>(withDupsSyncFileList));
    }

    public static List<SyncFile> filterSyncFileList(List<SyncFile> unfiltSyncFileList, List<String> syncFilterList) {
        Pattern pattern = buildFilterRegexPattern(syncFilterList);
        logger.info("syncFilterList regex:\n\n" + pattern.toString() + "\n\n");

//        for (SyncFile syncFile : unfiltSyncFileList) {
//            if (pattern.matcher(syncFile.getAbsoluteTargetFilePath()).find()) {
//                logger.debug("MATCHED: " + syncFile);
//            }
//        }
//        logger.debug("HERE01");

        // If the absoluteTargetFilePath does not match the filter regex, we add it and keep it on the list
        return unfiltSyncFileList.stream()
                .filter(s -> !pattern.matcher(s.getAbsoluteTargetFilePath()).find())
                .collect(Collectors.toList());

    }

    public static Pattern buildFilterRegexPattern(List<String> syncFilterList) {
        return Pattern.compile(syncFilterList
                                .stream()
                                .collect(Collectors.joining("|")));
    }

    public static List<SyncFile> executeAllSyncFiles(List<SyncFile> syncFileList) {
        List<SyncFile> resultSyncFileList = new ArrayList<>();

        for (SyncFile syncFile : syncFileList) {
            syncFile.execCompare();

//            logger.debug("\n" + syncFile.toString() + "\n");
            resultSyncFileList.add(syncFile);
        }

        return resultSyncFileList;
    }

    public static StringBuilder prettyPrintSyncFileList(List<SyncFile> syncFileList) {
        int col1Len = getMaxLengthSyncFileStatus(syncFileList);
        int col2Len = getMaxLengthRelativeGit(syncFileList);
        int col3Len = getMaxLengthAbsoluteTarget(syncFileList);

        StringBuilder sb = new StringBuilder();
        sb.append("Entries below:\n\n---------- SyncFileList START ----------\n");
        List<SyncFile> sortedSyncFileList = new SyncFileSorter(syncFileList).getSortedSyncFilesByStatusAndGitPath();
        for (SyncFile syncFile : sortedSyncFileList) {
            sb.append(String.format("STATUS: %-" + col1Len + "s | GIT: %-" + col2Len + "s | TAR: %-" + col3Len + "s\n", syncFile.getSyncFileStatus().toString(), syncFile.getRelativeGitFilePath(),  syncFile.getAbsoluteTargetFilePath()));
        }
        sb.append("---------- SyncFileList  END  ----------\n\n");
        return sb;
    }


    public static int getMaxLengthSyncFileStatus(List<SyncFile> syncFileList) {
        int maxLen = 1;
        for (SyncFile syncFile : syncFileList) {
            int len = (syncFile.getSyncFileStatus().toString() != null) ? syncFile.getSyncFileStatus().toString().length() : 4;
            maxLen = (maxLen > len) ? maxLen : len;
        }
        return maxLen;
    }

    public static int getMaxLengthRelativeGit(List<SyncFile> syncFileList) {
        int maxLen = 1;
        for (SyncFile syncFile : syncFileList) {
            int len = (syncFile.getRelativeGitFilePath() != null) ? syncFile.getRelativeGitFilePath().toString().length() : 4;
            maxLen = (maxLen > len) ? maxLen : len;
        }
        return maxLen;
    }

    public static int getMaxLengthAbsoluteTarget(List<SyncFile> syncFileList) {
        int maxLen = 1;
        for (SyncFile syncFile : syncFileList) {
            int len = (syncFile.getAbsoluteTargetFilePath() != null) ? syncFile.getAbsoluteTargetFilePath().toString().length() : 4;
            maxLen = (maxLen > len) ? maxLen : len;
        }
        return maxLen;
    }

    public static StringBuilder generatePostExecReport(List<SyncFile> syncFileListPostExec, Mv2prdHooks mv2prdHooks, Boolean showInSync) {
        int col1Len = getMaxLengthSyncFileStatus(syncFileListPostExec);
        int col2Len = getMaxLengthRelativeGit(syncFileListPostExec);
        int col3Len = getMaxLengthAbsoluteTarget(syncFileListPostExec);
        String formatString = "%-6s  %-" + col1Len + "s  %-" + col2Len + "s  %-" + col3Len + "s\n";

        StringBuilder sb = new StringBuilder();

        for (Enum e : SyncFileStatus.values()) {

            // Don't show synced files since they are good to go and should be the largest list of files
            if (e == SyncFileStatus.SYNCED && !showInSync) continue;

            List<SyncFile> tempSyncFileList = new ArrayList<>();

            for (SyncFile syncFile : syncFileListPostExec) {
                if (syncFile.getSyncFileStatus() == e) {
                    tempSyncFileList.add(syncFile);
                }
            }

            if (tempSyncFileList.size() > 0) {
                sb.append("--- The following files are in state: " + e + " ---\n\n"
                        + String.format(
                        formatString,
                        "OpType",
                        "Status",
                        "GIT Path",
                        "Target Path"
                )
                        + "\n");
                SyncFileSorter syncFileSorter = new SyncFileSorter(tempSyncFileList);
                List<SyncFile> sortedSyncFileList = syncFileSorter.getSortedSyncFilesByStatusAndGitPath();
                for (SyncFile syncFile : sortedSyncFileList) {
                    sb.append(String.format(
                            formatString,
                            "SYNC",
                            syncFile.getSyncFileStatus(),
                            syncFile.getRelativeGitFilePath(),
                            syncFile.getAbsoluteTargetFilePath()
                    ));
                }
                sb.append("\n\n");
            }
        }

        int maxLen = Utils.getLongestLineLengthInStringBuilder(sb);
        int halfLen = (maxLen / 2) - 8;

        StringBuilder header = new StringBuilder("Report below:\n\n");
        StringBuilder innerHeaderSb = new StringBuilder("   " + Utils.printNTimes("_",halfLen-3) + " M2PRD SYNC REPORT " + Utils.printNTimes("_",halfLen-3) + "   \n\n");

        Pattern pattern = buildFilterRegexPattern(mv2prdHooks.getSyncFilterList());
        // Wrap on pipe (since the regex is pipe-delimited)
        innerHeaderSb.append(
                "--- Sync filter regex ---\n\n"
                + WordUtils.wrap(
                    pattern.toString(),
                    maxLen,
                    "\n|",
                    true,
                    "\\|")
                + "\n\n");

//        int maxLen = Utils.getLongestLineLengthInStringBuilder(sb);
//        StringBuilder header = new StringBuilder("Report below:\n\n");
//        StringBuilder footer = new StringBuilder(Utils.printNTimes("*",maxLen) + "\n\n");

//        StringBuilder preExtraFormattingSb = new StringBuilder()
////                .append(header)
//                .append(sb)
////                .append(footer);

        StringBuilder finalSb = header
                .append(
                        Utils.boxitizeMe(
                                innerHeaderSb
                                        .append(sb)) + "\n");
        return finalSb;
    }

    public static StringBuilder printSyncFileList(List<SyncFile> syncFileList) {
        StringBuilder sb = new StringBuilder();
        for (SyncFile syncFile : syncFileList) {
            sb.append("\n\nSOURCE: " + syncFile.getSyncFileSource());
            sb.append("\nSTATUS: " + syncFile.getSyncFileStatus());
            sb.append("\nRELGIT: " + syncFile.getRelativeGitFilePath());
            sb.append("\nABSGIT: " + syncFile.getAbsoluteGitFilePath());
            sb.append("\nABSTAR: " + syncFile.getAbsoluteTargetFilePath());
        }
        sb.append("\n\n");
        return sb;
    }
}
