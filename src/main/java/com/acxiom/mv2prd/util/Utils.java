package com.some.company.mv2prd.util;

import com.some.company.mv2prd.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    private static final Logger logger = Logger.getLogger(Utils.class);

    public static Boolean promptUserYesNo(String promptText) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String userInput = null;

        logger.info(promptText + " Enter y/n: ");
        try {
            userInput = br.readLine();
        } catch (IOException e) {
            logger.error("promptUserYesNo: IOException", e);
        }

        // the magic (?i:X) syntax makes this search case-insensitive
        if (userInput != null && (userInput.matches("(?i:(y|yes))"))) {
            return true;
        } else
            return false;
    }

    public static void deleteFileWithUserConfirmation(Path filePath) throws IOException {
        Boolean confirmation = promptUserYesNo("Attempting to delete file/directory: " + filePath.toString() + "\nAre you sure you want to proceed?");
        if (confirmation == true) {
            deleteFileNoConfirmation(filePath);
        } else {
            logger.error("User denied file deletion, exiting...");
            System.exit(7);
        }
    }

    public static void deleteFileWithUserConfirmation(String filePathString) throws IOException {
        deleteFileWithUserConfirmation(Paths.get(filePathString));
    }

    public static void deleteFileNoConfirmation(Path filePath) throws IOException {
        if (Files.isDirectory(filePath)) {
            deleteDir(filePath.toFile());
        } else {
            Files.delete(filePath);
        }
    }

    public static boolean isNotNullOrEmptyOrBlank(String str) {
        return str != null && !str.isEmpty() && !str.matches("^ *$");
    }

    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }


    public static Boolean copyFile(String sourceFileString, String targetFileString, UserPrincipal userPrincipal, GroupPrincipal groupPrincipal, String posixFilePermissionString) throws IOException {
        Path sourceFilePath = Paths.get(sourceFileString);
        Path targetFilePath = Paths.get(targetFileString);
        Path targetFileParentPath = targetFilePath.getParent();
        if (!Files.exists(targetFileParentPath))
            createDirectoriesWithGroupAndPerms(targetFileParentPath, groupPrincipal, posixFilePermissionString);
        Files.copy(sourceFilePath,
                targetFilePath,
                StandardCopyOption.COPY_ATTRIBUTES,
                StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    // This method walks through all subdirectories on a single directory and creates each subdir one at a time as needed
    // in order to also set the group and permissions on each one.
    public static void createDirectoriesWithGroupAndPerms(Path pathToCreate, GroupPrincipal groupPrincipal, String posixFilePermissionString) throws IOException {
        List<Path> orderedSubPathList = buildSubPathList(pathToCreate);

        logger.info("+++ Attempting to create needed sub-directories one-by-one for: " + pathToCreate);
        for (Path path : orderedSubPathList) {
            if (!Files.exists(path)) {
                createDirectoryWithPerm(path, posixFilePermissionString);
                chgrpFile(path, groupPrincipal);
            }
        }
        logger.info("+++ All sub-directories created!");
    }

    public static Boolean createDirectoryWithPerm(Path path, String posixFilePermissionString) throws IOException {
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString(posixFilePermissionString);
        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        logger.info("Creating directory: " + path.toString() + " with permissions: " + posixFilePermissionString);
        Files.createDirectory(path, attr);
        logger.info("Directory created.");
        return true;
    }

    public static List<Path> buildSubPathList(Path path) {
        List<Path> orderedSubPathList = new ArrayList<>();
        List<String> pathPieces = Arrays.asList(path.toString().split("/"));
        String currentPathBuild = "/"; // Start with / root
        for (String pathPiece : pathPieces) {
            // We won't add / to the list since we won't try to create the root directory on the server
            currentPathBuild = currentPathBuild + pathPiece + "/";
            orderedSubPathList.add(Paths.get(currentPathBuild));
        }
        return orderedSubPathList;
    }

    public static Boolean chownFile(String targetFileString, UserPrincipal userPrincipal) throws IOException {
        Path targetFilePath = Paths.get(targetFileString);
        Files.setOwner(targetFilePath, userPrincipal);
        return true;
    }

    public static Boolean chgrpFile(Path targetFilePath, GroupPrincipal groupPrincipal) throws IOException {
        return chgrpFile(targetFilePath.toString(), groupPrincipal);
    }

    public static Boolean chgrpFile(String targetFileString, GroupPrincipal groupPrincipal) throws IOException {
        Path targetFilePath = Paths.get(targetFileString);
        Files.getFileAttributeView(targetFilePath, PosixFileAttributeView.class).setGroup(groupPrincipal);
        return true;
    }

    public static Boolean chmodFile(Path targetFilePath, String posixFilePermissionString) throws IOException {
        return chmodFile(targetFilePath.toString(), posixFilePermissionString);
    }

    public static Boolean chmodFile(String targetFileString, String posixFilePermissionString) throws IOException {
        Path targetFilePath = Paths.get(targetFileString);
        Files.setPosixFilePermissions(targetFilePath, PosixFilePermissions.fromString(posixFilePermissionString));
        return true;
    }

    public static UserPrincipal getUserPrincipal(String user) throws IOException {
        FileSystem fileSystem = FileSystems.getDefault();
        UserPrincipalLookupService service = fileSystem.getUserPrincipalLookupService();
        return service.lookupPrincipalByName(user);
    }

    public static GroupPrincipal getGroupPrincipal(String group) throws IOException {
        FileSystem fileSystem = FileSystems.getDefault();
        UserPrincipalLookupService service = fileSystem.getUserPrincipalLookupService();
        return service.lookupPrincipalByGroupName(group);
    }

    public static String formatPathString(String pathString) {
        return pathString.replaceAll("//", "/").replaceAll("/\\./", "/");
    }

    public static String printNTimes(String string, int number) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= number; i++) sb.append(string);
        return sb.toString();
    }

    public static int getLongestLineLengthInStringBuilder(StringBuilder sb) {
        int maxLen = 1;
        Scanner scanner = new Scanner(sb.toString());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            int lineLen = line.length();
            maxLen = (lineLen > maxLen) ? lineLen : maxLen;
        }
        scanner.close();
        return maxLen;
    }

    public static List<Path> getAllFilePathsInDirectory(Path directory) {
        List<Path> filePathList = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                filePathList.add(path);
            }
        } catch (IOException ex) {
        }
        return filePathList;
    }

    public static StringBuilder boxitizeMe(StringBuilder sb) {
        int maxLen = getLongestLineLengthInStringBuilder(sb);
        StringBuilder header = new StringBuilder("+" +
                printNTimes("-", maxLen + 2)
                + "+" + "\n");

        String formatString = "| %-" + maxLen + "s |\n";

        StringBuilder newerSb = new StringBuilder(String.format(formatString, ""));
        try (Scanner scanner = new Scanner(sb.toString())) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                newerSb.append(String.format(formatString, line));
            }
        }

        StringBuilder footer = new StringBuilder("+" +
                printNTimes("-", maxLen + 2) + "+" + "\n");

        return header
                .append(newerSb)
                .append(footer);
    }

    public static String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date d = new Date();
        return dateFormat.format(d);
    }

    public static String getHostname() throws IOException {
        return InetAddress.getLocalHost().getHostName();
    }

    public static void sendEmail(String subject, StringBuilder body, String fromEmailAddress, List<String> emailAddressList) throws IOException, AddressException, MessagingException {
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", "localhost");
        Session session = Session.getDefaultInstance(properties);
        MimeMessage message = new MimeMessage(session);

        message.setFrom(new InternetAddress(fromEmailAddress));
        message.addRecipients(Message.RecipientType.TO, getInternetAddressListFromStringList(emailAddressList));
        message.setSubject(subject);
        message.setText("<pre>" + body.toString() + "</pre>", "utf-8", "html");

        Transport.send(message);
        logger.info("Email sent successfully!");
    }

    public static InternetAddress[] getInternetAddressListFromStringList(List<String> emailAddressList) throws AddressException {
        List<InternetAddress> internetAddressList = emailAddressList
                .stream()
                .map(string -> getNewInternetAddress(string))
                .collect(Collectors.toList());
        return internetAddressList.toArray(new InternetAddress[0]);
    }

    public static InternetAddress getNewInternetAddress(String internetAddress) {
        try {
            return new InternetAddress(internetAddress);
        } catch (AddressException e) {
            return null;
        }
    }
}
