package com.some.company.mv2prd.main;

import com.some.company.mv2prd.exceptions.GitTagInvalidException;
import com.some.company.mv2prd.exceptions.InvalidArgumentsException;
import com.some.company.mv2prd.file.install.FileChange;
import com.some.company.mv2prd.file.install.FileChangeUtils;
import com.some.company.mv2prd.file.sync.SyncFile;
import com.some.company.mv2prd.file.sync.SyncFileUtils;
import com.some.company.mv2prd.json.Mv2prdHooks;
import com.some.company.mv2prd.util.GitUtils;
import com.some.company.mv2prd.util.Utils;
import com.some.company.mv2prd.util.logging.DebugSettingSingleton;
import com.some.company.mv2prd.util.logging.LogWriterSingleton;
import com.some.company.mv2prd.util.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.*;

public class Mv2prd {

    private int exitCode=0;

    // We initialize here and then re-initialize once we have the mv2prd hooks file read in so we can have it use the logger file from that
    private static Logger logger = Logger.getLogger(Mv2prd.class);

    private Mv2prdMode mv2prdMode;

    @Option(name="--mode",usage="REQUIRED : Run mode, \"install\" or \"sync_check\"")
    private String modeString = null;

    @Option(name="--project",usage="REQUIRED : GIT project name (without .git suffix)")
    private String project = null;

    @Option(name="--stashProjectRootURL",usage="REQUIRED : Stash project root URL (not including *.git repo)")
    private String stashProjectRootURL = null;

    @Option(name="--workingTreeParentDir",usage="REQUIRED : Workspace directory where git working trees are/will be checked out (the working tree directory typically contains .git in its top dir)")
    private String workingTreeParentDir = null;

    @Option(name="--userId",usage="REQUIRED : UserId of the person executing this script")
    private String userId = null;

    @Option(name="--sccUserId",usage="REQUIRED : UserId of the source code managing user, files will be owned by this user after being moved in. " +
            "Mv2prd should be executed by this user (via sudo).")
    private String sccUserId = null;

    @Option(name="--group",usage="REQUIRED : Files will be set to this group after being moved in, also used for mv2prd log file directory creation")
    private String group = null;

    @Option(name="--gitTag",usage="REQUIRED(Install-only) : GIT tag name associated with this change")
    private String gitTag = null;

    @Option(name="--dryRun",usage="OPTIONAL(Install-only) : Preview/dry run, list changes but don't actually make any")
    private Boolean dryRun = false;

    @Option(name="--showInSync",usage="OPTIONAL(Sync-only) : Turn on printing of in-sync files. May result in large amount of output if most are synced already.")
    private Boolean showInSync = false;

    @Option(name="--emails",usage="OPTIONAL : Overrides the emailList in mv2prd_hooks.json. comma-delimited (no spaces) list of email recipients to send final report to.")
    private String emails = null;

    @Option(name="--debug",usage="OPTIONAL : Enable extra debug logging")
    private Boolean debug = false;

    private List<String> emailAddressList = new ArrayList<>();

    private String posixFilePermissionString = "rwxr-xr-x";

    public static void main(String[] args) {
        Mv2prd bean = new Mv2prd();
        CmdLineParser parser = new CmdLineParser(bean,ParserProperties.defaults().withUsageWidth(120));

        try {
            parser.parseArgument(args);
            bean.validateArguments();
            bean.run();
            System.exit(bean.getExitCode());
        } catch (CmdLineException | InvalidArgumentsException e) {
            StringWriter writer = new StringWriter();
            logger.error("Caught an exception during main()", e);
            writer.append("java -jar mv2prd.jar");
            parser.printSingleLineUsage(writer, null);
            writer.append("\n\n");
            parser.printUsage(writer, null);
            logger.error("Usage info below:\n ----- Usage -----\n\n" + writer.toString() + "\n-----------------\n");
            System.exit(2);
        }
    }

    // Further arg validation here, make sure all the variables are set appropriately
    private void validateArguments() throws InvalidArgumentsException {
        logger.info("Validating arguments...");
        List<String> invalidArgumentList = new ArrayList<>();

        mv2prdMode = Mv2prdMode.getMv2prdModeFromString(modeString);

        if (mv2prdMode == null)
            throw new InvalidArgumentsException("Mode option was invalid");

        Map<String,String> requiredArgsMap = new HashMap<>();
        Map<String,String> invalidArgsMap = new HashMap<>();

        if (mv2prdMode == Mv2prdMode.INSTALL) {
            requiredArgsMap = new HashMap<String, String>() {{
                    put("project", project);
                    put("stashProjectRootURL", stashProjectRootURL);
                    put("workingTreeParentDir", workingTreeParentDir);
                    put("userId", userId);
                    put("sccUserId", sccUserId);
                    put("group", group);
                    put("gitTag", gitTag);
                }};
            invalidArgsMap = new HashMap<String, String>() {{
                    put("showInSync", Boolean.toString(showInSync));
                }};
        } else if (mv2prdMode == Mv2prdMode.SYNC_CHECK) {
            requiredArgsMap = new HashMap<String, String>() {{
                    put("project", project);
                    put("stashProjectRootURL", stashProjectRootURL);
                    put("workingTreeParentDir", workingTreeParentDir);
                    put("userId", userId);
                    put("sccUserId", sccUserId);
                    put("group", group);
                }};
            invalidArgsMap = new HashMap<String, String>() {{
                    put("gitTag", gitTag);
                    put("dryRun", Boolean.toString(dryRun));
                }};
        }

        for (Map.Entry<String, String> entry : requiredArgsMap.entrySet() ) {
            if (!Utils.isNotNullOrEmptyOrBlank(entry.getValue())) {
                invalidArgumentList.add("(REQUIRED)" + entry.getKey() + ":" + entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : invalidArgsMap.entrySet() ) {
            if (Utils.isNotNullOrEmptyOrBlank(entry.getValue()) && (entry.getValue() != null && !entry.getValue().equals("false")) ) {
                invalidArgumentList.add("(INVALID_FOR_MODE)" + entry.getKey() + ":" + entry.getValue());
            }
        }

        if (invalidArgumentList.size() > 0)
            throw new InvalidArgumentsException(invalidArgumentList);

        logger.info("Arguments passed inspection. Proceeding.");
    }

    private void run() {
        try {
            this.mv2prd();
        } catch(Exception e) {
            logger.error("Caught an exception during run()", e);
            if (this.exitCode==0) this.exitCode=9;
        } finally {
            // If it got far enough to load Mv2prdHooks, this will print a final separator at the end of the run in the file.
            logger.plain("\n" + Utils.printNTimes("-",100) + "\n" + Utils.printNTimes("+",100) + "\n" + Utils.printNTimes("-",100) + "\n");
            // This will flush and close the printWriter which will send the last of the buffered data to the file.
            LogWriterSingleton.getInstance().closePrintWriter();
        }

    }

    // Primary logic here
    private void mv2prd() throws Exception {

        if (debug) DebugSettingSingleton.getInstance().enableDebugLogging();
        if (dryRun) logger.info("+++ Dry run/preview mode enabled. No filesystem changes will be made. +++");

        Git git = GitUtils.initGit(stashProjectRootURL, project, workingTreeParentDir);
        logger.info("Git repo directory: " + git.getRepository().getDirectory().toString());

        GroupPrincipal groupPrincipal = Utils.getGroupPrincipal(group);

        Mv2prdHooks mv2prdHooks = GitUtils.readMv2prdHooks(git);
        logger.info("From here on, logger statements should log to the mv2prd log: " + mv2prdHooks.getLogFile());
        LogWriterSingleton.getInstance().initPrintWriter(dryRun, mv2prdMode,
                Paths.get(mv2prdHooks.getLogFile() + mv2prdMode.getLogSuffix()),
                groupPrincipal, posixFilePermissionString);

        logger.info("BLAME USER: " + userId);
        initEmailAddressList(mv2prdHooks);

        if (mv2prdMode == Mv2prdMode.INSTALL) {
            installChanges(git, mv2prdHooks, groupPrincipal, posixFilePermissionString);
        } else if (mv2prdMode == Mv2prdMode.SYNC_CHECK) {
            syncCheck(git, mv2prdHooks);
        }
    }

    private void initEmailAddressList(Mv2prdHooks mv2prdHooks) {
        if (Utils.isNotNullOrEmptyOrBlank(this.emails)) {
            this.emailAddressList = Arrays.asList(this.emails.split(","));
            logger.info("Setting emailAddressList from user parameter: " + emailAddressList);
        } else {
            this.emailAddressList = mv2prdHooks.getEmailAddressList();
            logger.info("Setting emailAddressList from Mv2prdHooks: " + emailAddressList);
        }
        if (emailAddressList.size() < 1) {
            logger.warn("emailAddressList is empty after initialization, no email reports will be sent!");
        }
    }

    private void installChanges(Git git, Mv2prdHooks mv2prdHooks, GroupPrincipal groupPrincipal, String posixFilePermissionString) throws IOException, GitTagInvalidException {

        List<DiffEntry> diffEntryList = GitUtils.getTagDiffEntryList(git, gitTag);
        if (diffEntryList.size() == 1) {
            logger.info("No files are associated with the specified gitTag: \"" + gitTag + "\" Exiting.");
            return;
        }

        UserPrincipal userPrincipal = Utils.getUserPrincipal(sccUserId);
        logger.info("userPrincipal: " + userPrincipal.getName() + " group: " + group);

        List<FileChange> fileChangeListPostExec = FileChangeUtils.executeAllFileChanges(dryRun, gitTag, diffEntryList, git.getRepository().getWorkTree().toString(),
                userPrincipal, groupPrincipal, mv2prdHooks, posixFilePermissionString);

        logger.debug(FileChangeUtils.prettyPrintFileChangeList(fileChangeListPostExec).toString());

        StringBuilder postExecReport = FileChangeUtils.generatePostExecReport(dryRun, fileChangeListPostExec);
        logger.info(postExecReport.toString());

        sendReportEmail(generateEmailSubject(), postExecReport, "mv2prd@some.company.com", emailAddressList);

        logger.info("Successfully installed changes!");
    }

    private void syncCheck(Git git, Mv2prdHooks mv2prdHooks) throws IOException {

        List<SyncFile> syncFileList = SyncFileUtils.getFilteredSyncFiles(mv2prdHooks, git.getRepository().getWorkTree().toString());
        List<SyncFile> postExecSyncFileList = SyncFileUtils.executeAllSyncFiles(syncFileList);

        StringBuilder postExecReport = SyncFileUtils.generatePostExecReport(postExecSyncFileList, mv2prdHooks, showInSync);
        logger.info(postExecReport.toString());

        sendReportEmail(generateEmailSubject(), postExecReport, "mv2prd@some.company.com", emailAddressList);

        logger.info("Successfully finished running sync check!");
    }

    private String generateEmailSubject() throws IOException {
        String middle = (mv2prdMode == Mv2prdMode.SYNC_CHECK) ? "Sync Check" : "Install";
        return this.sccUserId + "@" + Utils.getHostname() + ": MV2PRD " + middle + " Report : " + Utils.getTodaysDate();
    }

    private void sendReportEmail(String subject, StringBuilder reportBody, String fromEmailAddress, List<String> emailAddressList) {
        try {
            Utils.sendEmail(subject, reportBody, fromEmailAddress, emailAddressList);
        } catch (Exception e) {
            logger.error("sendReportEmail: Error occurred while sending email, printing and continuing.", e);
        }
    }

    private int getExitCode() {
        return this.exitCode;
    }

}
