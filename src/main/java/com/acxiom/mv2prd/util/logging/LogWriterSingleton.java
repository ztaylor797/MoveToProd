package com.some.company.mv2prd.util.logging;

import com.some.company.mv2prd.main.Mv2prdMode;
import com.some.company.mv2prd.util.Utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;

public class LogWriterSingleton {

    private static Logger logger = Logger.getLogger(LogWriterSingleton.class);

    private static LogWriterSingleton instance;
    private static PrintWriter printWriter;

    private Boolean hasPrintedError = false;

    private LogWriterSingleton(){}

    public static LogWriterSingleton getInstance() {
        if (instance == null) {
            instance = new LogWriterSingleton();

        }
        return instance;
    }

    public void initPrintWriter(Boolean dryRun, Mv2prdMode mv2prdMode, Path logPath, GroupPrincipal groupPrincipal, String posixFilePermissionsString) {

        // If it's a dryRun, we don't want to log to the file
        if (dryRun && mv2prdMode == Mv2prdMode.INSTALL) {
            logger.info("dryRun enabled, will NOT attempt any writes to log: " + logPath.toString());
            return;
        }

        Path logPathParent = logPath.getParent();
        try {
            if (!Files.exists(logPathParent)) {
                Utils.createDirectoriesWithGroupAndPerms(logPathParent, groupPrincipal, posixFilePermissionsString);
            }
            this.printWriter = new PrintWriter(new BufferedWriter(new FileWriter(logPath.toString(), true)));
        } catch (IOException e) {
            if (!this.hasPrintedError) {
                this.hasPrintedError = true;
                logger.error("initPrintWriter: IOException while trying to init the printWriter. This will only print the first time it errors. Log: " + logPath.toString());
            }
            this.printWriter = null;
        }
    }

    // Should be called at application end
    public void closePrintWriter() {
        if (printWriter != null) {
            printWriter.flush();
            printWriter.close();
        }
    }

    public static PrintWriter getPrintWriter() {
        return printWriter;
    }

}
