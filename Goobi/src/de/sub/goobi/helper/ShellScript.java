/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi
 * 			- http://digiverso.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
package de.sub.goobi.helper;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.goobi.production.enums.LogType;

import de.sub.goobi.config.ConfigurationHelper;

/**
 * The class ShellScript is intended to run shell scripts (or other system commands).
 * 
 * @author Matthias Ronge <matthias.ronge@zeutschel.de>
 */
public class ShellScript {
    private static final Logger logger = Logger.getLogger(ShellScript.class);

    public static final int ERRORLEVEL_ERROR = 1;

    private final String command;
    private LinkedList<String> outputChannel, errorChannel;
    private Integer errorLevel;

    /**
     * This returns the command.
     * 
     * @return the command
     */
    public Path getCommand() {
        return Paths.get(command);
    }

    /**
     * This returns the command string.
     * 
     * @return the command
     */
    public String getCommandString() {
        return command;
    }

    /**
     * Provides the results of the script written on standard out. Null if the script has not been run yet.
     * 
     * @return the output channel
     */
    public LinkedList<String> getStdOut() {
        return outputChannel;
    }

    /**
     * Provides the content of the standard error channel. Null if the script has not been run yet.
     * 
     * @return the error channel
     */
    public LinkedList<String> getStdErr() {
        return errorChannel;
    }

    /**
     * Provides the result error level.
     * 
     * @return the error level
     */
    public Integer getErrorLevel() {
        return errorLevel;
    }

    /**
     * A shell script must be initialised with an existing file on the local file system.
     * 
     * @param executable Script to run
     * @throws FileNotFoundException is thrown if the given executable does not exist.
     */
    public ShellScript(Path executable) throws FileNotFoundException {
        if (!StorageProvider.getInstance().isFileExists(executable)) {
            throw new FileNotFoundException("Could not find executable: " + executable.toString());
        }
        command = executable.toString();
    }

    /**
     * The function run() will execute the system command. This is a shorthand to run the script without arguments.
     * 
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the current thread is interrupted by another thread while it is waiting, then the wait is ended and an
     *             InterruptedException is thrown.
     */
    public int run() throws IOException, InterruptedException {
        return run(null);
    }

    /**
     * The function run() will execute the system command. First, the call sequence is created, including the parameters passed to run(). Then, the
     * underlying OS is contacted to run the command. Afterwards, the results are being processed and stored.
     * 
     * The behaviour is slightly different from the legacy callShell2() command, as it returns the error level as reported from the system process.
     * Use this to get the old behaviour:
     * 
     * <pre>
     * Integer err = scr.run(args);
     * if (scr.getStdErr().size() &gt; 0)
     *     err = ShellScript.ERRORLEVEL_ERROR;
     * </pre>
     * 
     * @param args A list of arguments passed to the script. May be null.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the current thread is interrupted by another thread while it is waiting, then the wait is ended and an
     *             InterruptedException is thrown.
     */
    public int run(List<String> args) throws IOException, InterruptedException {

        List<String> commandLine = new ArrayList<>();
        commandLine.add(command);
        if (args != null) {
            commandLine.addAll(args);
        }
        Process process = null;
        try {
            String[] callSequence = commandLine.toArray(new String[commandLine.size()]);
            ProcessBuilder pb = new ProcessBuilder(callSequence);
            ConfigurationHelper config = ConfigurationHelper.getInstance();
            if (config.useCustomS3()) {
                pb.environment().put("CUSTOM_S3", "true");
                pb.environment().put("S3_ENDPOINT_URL", config.getS3Endpoint());
                pb.environment().put("S3_ACCESSKEYID", config.getS3AccessKeyID());
                pb.environment().put("S3_SECRETACCESSKEY", config.getS3SecretAccessKey());
            }
            process = pb.start();

            outputChannel = inputStreamToLinkedList(process.getInputStream());
            errorChannel = inputStreamToLinkedList(process.getErrorStream());
        } catch (IOException error) {
            throw new IOException(error.getMessage());
        } finally {
            if (process != null) {
                closeStream(process.getInputStream());
                closeStream(process.getOutputStream());
                closeStream(process.getErrorStream());
            }
        }
        errorLevel = process.waitFor();
        return errorLevel;
    }

    /**
     * The function inputStreamToLinkedList() reads an InputStream and returns it as a LinkedList.
     * 
     * @param myInputStream Stream to convert
     * @return A linked list holding the single lines.
     */
    public static LinkedList<String> inputStreamToLinkedList(InputStream myInputStream) {
        LinkedList<String> result = new LinkedList<>();
        Scanner inputLines = null;
        try {
            inputLines = new Scanner(myInputStream);
            while (inputLines.hasNextLine()) {
                String myLine = inputLines.nextLine();
                result.add(myLine);
            }
        } finally {
            if (inputLines != null) {
                inputLines.close();
            }
        }
        return result;
    }

    /**
     * This behaviour was already implemented. I can’t say if it’s necessary.
     * 
     * @param inputStream A stream to close.
     */
    private static void closeStream(Closeable inputStream) {
        if (inputStream == null) {
            return;
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            logger.warn("Could not close stream.", e);
            Helper.setFehlerMeldung("Could not close open stream.");
        }
    }

    public static int callShell(List<String> parameter, Integer processID) throws IOException, InterruptedException {
        int err = ShellScript.ERRORLEVEL_ERROR;

        if (parameter.isEmpty()) {
            return 0;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(parameter);
        }

        String scriptname = parameter.get(0);
        List<String> parameterWithoutCommand = null;
        if (parameter.size() > 1) {
            parameterWithoutCommand = parameter.subList(1, parameter.size());
        }
        try {
            ShellScript s = new ShellScript(Paths.get(scriptname));

            err = s.run(parameterWithoutCommand);
            String msg = "";
            for (String line : s.getStdOut()) {
                msg += line + "\n";
                //            Helper.setMeldung(line);
            }
            Helper.addMessageToProcessLog(processID, LogType.DEBUG, "Script '" + scriptname + "' was executed with result: " + msg);
            Helper.setMeldung(msg);
            if (s.getStdErr().size() > 0) {
                err = ShellScript.ERRORLEVEL_ERROR;
                String message = "";
                for (String line : s.getStdErr()) {
                    message += line + "\n";
                }
                Helper.addMessageToProcessLog(processID, LogType.ERROR, "Error occured while executing script '" + scriptname + "': " + message);
                Helper.setFehlerMeldung(message);
            }
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in callShell2()", e);
            Helper.addMessageToProcessLog(processID, LogType.ERROR, "Exception occured while executing script '" + scriptname + "': " + e
                    .getMessage());
            Helper.setFehlerMeldung("Couldn't find script file in callShell2(), error", e.getMessage());
        }
        return err;
    }

    /**
     * This implements the legacy Helper.callShell2() command. This is subject to whitespace problems and is maintained here for backward
     * compatibility only. Please don’t use.
     * 
     * @param nonSpacesafeScriptingCommand A single line command which mustn’t contain parameters containing white spaces.
     * @return error level on success, 1 if an error occurs
     * @throws InterruptedException In case the script was interrupted due to concurrency
     * @throws IOException If an I/O error happens
     */
    public static int legacyCallShell2(String nonSpacesafeScriptingCommand, Integer processID) throws IOException, InterruptedException {
        //		String[] tokenisedCommand = nonSpacesafeScriptingCommand.split("\\s");
        ShellScript s;
        int err = ShellScript.ERRORLEVEL_ERROR;
        try {
            String scriptname = "";
            String paramList = "";
            if (nonSpacesafeScriptingCommand.contains(" ")) {
                scriptname = nonSpacesafeScriptingCommand.substring(0, nonSpacesafeScriptingCommand.indexOf(" "));
                paramList = nonSpacesafeScriptingCommand.substring(nonSpacesafeScriptingCommand.indexOf(" ") + 1);
            } else {
                scriptname = nonSpacesafeScriptingCommand;
            }
            s = new ShellScript(Paths.get(scriptname));

            List<String> scriptingArgs = new ArrayList<>();
            if (paramList != null && !paramList.isEmpty()) {
                String[] params = null;
                if (paramList.contains("\"")) {
                    params = paramList.split("\"");
                } else {
                    params = paramList.split(" ");
                }
                for (String param : params) {
                    if (!param.trim().isEmpty()) {
                        scriptingArgs.add(param.trim());
                    }
                }
            } else {
                //			for (int i = 1; i < tokenisedCommand.length; i++) {
                //				scriptingArgs.add(tokenisedCommand[i]);
                //			}
                scriptingArgs.add(paramList);
            }
            err = s.run(scriptingArgs);
            String msg = "";
            for (String line : s.getStdOut()) {
                msg += line + "\n";
                //                Helper.setMeldung(line);
            }
            Helper.addMessageToProcessLog(processID, LogType.DEBUG, "Script '" + nonSpacesafeScriptingCommand + "' was executed with result: " + msg);
            if (StringUtils.isNotBlank(msg)) {
                Helper.setMeldung(msg);
            }
            if (s.getStdErr().size() > 0) {
                err = ShellScript.ERRORLEVEL_ERROR;
                String message = "";
                for (String line : s.getStdErr()) {
                    message += line + "\n";
                }
                Helper.addMessageToProcessLog(processID, LogType.ERROR, "Error occured while executing script '" + nonSpacesafeScriptingCommand
                        + "': " + message);
                if (StringUtils.isNotBlank(message)) {
                    Helper.setFehlerMeldung(message);
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in callShell2()", e);
            Helper.addMessageToProcessLog(processID, LogType.ERROR, "Exception occured while executing script '" + nonSpacesafeScriptingCommand
                    + "': " + e.getMessage());
            Helper.setFehlerMeldung("Couldn't find script file in callShell2(), error", e.getMessage());
        }
        return err;
    }
}