package com.some.company.mv2prd.util;

import com.some.company.mv2prd.exceptions.GitRepoInvalidException;
import com.some.company.mv2prd.exceptions.GitTagInvalidException;
import com.some.company.mv2prd.json.JsonUtils;
import com.some.company.mv2prd.json.Mv2prdHooks;
import com.some.company.mv2prd.util.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

// All JGIT-related utilities
public class GitUtils {

    private static final Logger logger = Logger.getLogger(GitUtils.class);

    // In this context, a "git repo" is the actual .git directory. A working tree is the directory where
    // the files associated with that git repo are located. Usually the working tree contains the .git directory.
    public static Git initGit(String stashProjectRootURL, String project, String workingTreeParentPathString) throws GitAPIException, GitRepoInvalidException, IOException {
        Git git;

        String remoteRepoURL = buildRemoteRepoURL(stashProjectRootURL, project);

        // Ex: /..../repo_workspace/git_project_x
        String workingTreePathString = buildWorkingTreePath(workingTreeParentPathString, project);
        // Ex: /..../repo_workspace/git_project_x/.git
        String localRepoPathString = workingTreePathString + "/.git";

        logger.debug("remoteRepoURL: " + remoteRepoURL);
        logger.debug("workingTreePathString: " + workingTreePathString);
        logger.debug("localRepoPathString: " + localRepoPathString);

        Path workingTreePath = Paths.get(workingTreePathString);
        Path localRepoPath = Paths.get(localRepoPathString);

        // Check if local repo directory already exists as a file or directory
        if (Files.exists(workingTreePath)) {
            logger.info("Local file/path exists. Proceeding with inspection...");
            // Check if it is a directory (should be)
            if (Files.isDirectory(workingTreePath)) {
                logger.info("Local path is indeed a directory. Proceeding with inspection...");
                if (isValidGitRepo(localRepoPathString)) {
                    // It is a valid git repo, do a pull/update
                    logger.info("Local repo is a valid git repo. Proceeding with pull/update.");
                        git = initExistingRepoAsGit(localRepoPath);
                        git = resetLocalToMatchRemote(git);
                } else {
                    // It is not a valid git repo, remove the directory and do a fresh clone
                    logger.info("Local repo exists, but is not a valid repo. Removing working tree and cloning fresh.");
                    try {
                        Utils.deleteFileWithUserConfirmation(workingTreePath);
                        logger.info("Local working tree deleted: " + workingTreePath.toString());
                    } catch (IOException e) {
                        logger.error("initGit: Could not delete local working tree: " + workingTreePath.toString());
                    }
                    git = cloneRepository(remoteRepoURL, workingTreePathString);
                }
            } else {
                // The repo path exists, but is not a directory so delete it and do a fresh clone
                logger.info("Local working tree exists, but is a file and not a directory. Removing working tree and cloning fresh.");
                try {
                    Utils.deleteFileWithUserConfirmation(workingTreePath);
                    logger.info("File deleted: " + workingTreePath.toString());
                } catch (IOException e) {
                    logger.error("initGit: Could not delete local file: " + workingTreePath.toString());
                }
                git = cloneRepository(remoteRepoURL, workingTreePathString);
            }
        } else {
            // Repo directory doesn't exist, so clone fresh
            logger.info("Local working tree does not exist. Cloning fresh.");
            git = cloneRepository(remoteRepoURL, workingTreePathString);
        }

        logger.info("Validating the repo once more to be safe...");
        if (!isValidGitRepo(localRepoPathString)) {
            throw new GitRepoInvalidException("Git repo failed validation after update/checkout. Cannot proceed.");
        }

        logger.info("Successfully initialized the git repo!");
        return git;
    }

    private static Git resetLocalToMatchRemote(Git git) throws GitAPIException {
        logger.info("Fetch origin");
        git.fetch()
                .setRemote("origin")
                .call();
        logger.info("Resetting hard to origin/master");
        git.reset()
                .setMode(ResetCommand.ResetType.HARD)
                .setRef("@{upstream}")
                .call();
        logger.info("Cleaning stale files");
        git.clean()
                .setCleanDirectories(true)
                .setForce(true)
                .call();
        return git;
    }

    private static String buildRemoteRepoURL(String stashProjectRootURL, String project) {
        return stashProjectRootURL + "/" + project + ".git";
    }

    private static Git cloneRepository(String remoteRepoURL, String workingTreePathString) throws GitAPIException {
        Git git;
        try {
            logger.info("Cloning remote repo: " + remoteRepoURL + " to " + workingTreePathString);
            git = Git.cloneRepository()
                    .setURI(remoteRepoURL)
                    .setDirectory(new File(workingTreePathString))
                    .call();
            logger.info("Clone successful, proceeding.");
        } catch (GitAPIException e) {
            logger.error("Clone failed: " + remoteRepoURL + " to " + workingTreePathString);
            throw e;
        }
        return git;
    }

    private static Boolean isValidGitRepo(String localRepoPathString) {
        Boolean isValid = false;

        // A partial clone could make isGitRepository() evaluate to true. To check if the git clone was done successfully,
        // need to also check at least if one reference is not null.
        if (RepositoryCache.FileKey.isGitRepository(new File(localRepoPathString), FS.DETECTED)) {
            logger.info("This is a git repo. Proceeding with further checks.");
            // Already cloned. Make sure references are valid too.
            try {
                logger.info("Initializing existing repo before checking references.");
                Repository repo = initExistingRepo(Paths.get(localRepoPathString));
                isValid = hasAtLeastOneReference(repo);
            } catch (IOException e) {
                logger.info("isValidGitRepo IOException, marking as an invalid repo.");
                // Do nothing and just let this method return false
            }
        } else {
            logger.info("This is not a git repository.");
        }
        return isValid;
    }

    private static boolean hasAtLeastOneReference(Repository repo) {
        try {
            for (Ref ref : repo.getRefDatabase().getRefs()) {
                if (ref.getObjectId() == null)
                    continue;
                logger.info("Found a valid reference.");
                return true;
            }
        } catch (IOException e) {
            logger.error("hasAtLeastOneReference: IOException while getting refs");
            return false;
        }
        logger.info("No valid references found.");
        return false;
    }

    private static Git initExistingRepoAsGit(Path localRepoPath) throws IOException {
        return new Git(initExistingRepo(localRepoPath));
    }

    private static Repository initExistingRepo(Path localRepoPath) throws IOException {
        Repository repository;
        if (!Files.exists(localRepoPath)) {
            FileNotFoundException fnfe = new FileNotFoundException(
                    "GIT repo not found, cannot init existing repo. Path: " + localRepoPath.toString());
            logger.error("FileNotFoundException in initExistingRepo", fnfe);
            throw fnfe;
        } else {
            try {
                repository = new FileRepositoryBuilder()
                        .setGitDir(new File(localRepoPath.toString()))
                        .build();
            } catch (IOException e) {
                logger.error("IOException in initExistingRepo", e);
                throw e;
            }
        }
        return repository;
    }

    private static String buildWorkingTreePath(String gitRepoParentDir, String project) {
        return Utils.formatPathString(gitRepoParentDir + "/" + project);
    }

    // Compares a gitTag commit to its immediate predecessor
    public static List<DiffEntry> getTagDiffEntryList(Git git, String gitTag) throws IOException, GitTagInvalidException {
        List<DiffEntry> entries;

        ObjectReader reader = git.getRepository().newObjectReader();

        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        ObjectId oldTree = git.getRepository().resolve(gitTag + "~1^{tree}");

        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        ObjectId newTree = git.getRepository().resolve(gitTag + "^{tree}");

        if (oldTree == null || newTree == null) {
            throw new GitTagInvalidException("getTagDiffEntryList: The gitTag does not exist in the repo (make sure to 'push --tags'): " + gitTag);
        }

        oldTreeIter.reset(reader, oldTree);
        newTreeIter.reset(reader, newTree);

        DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        diffFormatter.setRepository(git.getRepository());

        entries = diffFormatter.scan(oldTreeIter, newTreeIter);

        return entries;
    }

    // This method is in GitUtils because the other two invocations allow a Git object to be passed in
    public static Mv2prdHooks readMv2prdHooks(String hooksAbsoluteFilePath) throws IOException {

        Path mv2prdHooksPath = Paths.get(hooksAbsoluteFilePath);
        if (!Files.exists(mv2prdHooksPath)) {
            throw new FileNotFoundException("Missing mv2prd hooks file: " + mv2prdHooksPath.toString());
        }

        byte[] jsonData = JsonUtils.readJsonData(mv2prdHooksPath);
        return JsonUtils.convertJsonDataToMv2prdHooks(jsonData);
    }

    public static Mv2prdHooks readMv2prdHooks(Git git) throws IOException {
        return readMv2prdHooks(git.getRepository().getDirectory().getParent() + "/mv2prd_hooks.json");
    }

    public static Mv2prdHooks readMv2prdHooks(Git git, String hooksRelativeFileName) throws IOException {
        return readMv2prdHooks(git.getRepository().getDirectory().getParent() + "/" + hooksRelativeFileName);
    }

    public static String determineRelativeGitFilePathFromDiffEntry(DiffEntry diffEntry) {
        if (diffEntry.getNewMode() == FileMode.MISSING) {
            return diffEntry.getOldPath();
        } else {
            return diffEntry.getNewPath();
        }
    }

    public static String determineRelativeGitFilePathFromAbsTarget(String absoluteTargetFilePath, Map<String,String> gitToTargetMap) {
        for (Map.Entry<String,String> entry : gitToTargetMap.entrySet()) {
            if (absoluteTargetFilePath.matches(Utils.formatPathString(entry.getValue() + "/.*"))) {
                return Utils.formatPathString(absoluteTargetFilePath.replace(entry.getValue(),entry.getKey()));
            }
        }
        return null;
    }

    public static String determineRelativeGitFilePathFromAbsGit(String absoluteGitFilePath, Map<String,String> gitToTargetMap) {
        for (Map.Entry<String,String> entry : gitToTargetMap.entrySet()) {
            if (absoluteGitFilePath.matches(Utils.formatPathString(".*/" + entry.getKey() + "/.*"))) {
                return Utils.formatPathString(absoluteGitFilePath.replaceAll(Utils.formatPathString(".*/" + entry.getKey()), entry.getKey()));
            }
        }
        return null;
    }

    public static String buildAbsoluteGitFilePath(String workingTreeDir, String relativeGitFilePath) {
        return Utils.formatPathString(workingTreeDir + "/" + relativeGitFilePath);
    }

    public static String buildAbsoluteTargetFilePath(String relativeGitFilePath, Map<String,String> gitToTargetMap) {
        for (Map.Entry<String,String> entry : gitToTargetMap.entrySet()) {
            if (relativeGitFilePath.matches(Utils.formatPathString(entry.getKey() + "/.*"))) {
                return Utils.formatPathString(relativeGitFilePath.replace(entry.getKey(),entry.getValue()));
            }
        }
        return null;
    }

    public static String buildAbsoluteBackupFilePath(String relativeGitFilePath, String gitTag, String backupDir) {
        return Utils.formatPathString(backupDir + "/" + gitTag + "/" + relativeGitFilePath);
    }

}
