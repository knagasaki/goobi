package org.goobi.goobiScript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.enums.GoobiScriptResultType;
import org.goobi.production.enums.LogType;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.HelperSchritte;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;

public class GoobiScriptRunScript extends AbstractIGoobiScript implements IGoobiScript {
    private static final Logger logger = Logger.getLogger(GoobiScriptRunScript.class);

    @Override
    public boolean prepare(List<Integer> processes, String command, HashMap<String, String> parameters) {
        super.prepare(processes, command, parameters);

        if (parameters.get("steptitle") == null || parameters.get("steptitle").equals("")) {
            Helper.setFehlerMeldung("goobiScriptfield", "Missing parameter: ", "steptitle");
            return false;
        }

        // add all valid commands to list
        for (Integer i : processes) {
            GoobiScriptResult gsr = new GoobiScriptResult(i, command, username);
            resultList.add(gsr);
        }

        return true;
    }

    @Override
    public void execute() {
        RunScriptThread et = new RunScriptThread();
        et.start();
    }

    class RunScriptThread extends Thread {

        @Override
        public void run() {
            HelperSchritte hs = new HelperSchritte();
            String steptitle = parameters.get("steptitle");
            String scriptname = parameters.get("script");

            // execute all jobs that are still in waiting state
            ArrayList<GoobiScriptResult> templist = new ArrayList<>(resultList);
            for (GoobiScriptResult gsr : templist) {
                if (gsm.getAreScriptsWaiting(command) && gsr.getResultType() == GoobiScriptResultType.WAITING && gsr.getCommand().equals(command)) {
                    Process p = ProcessManager.getProcessById(gsr.getProcessId());
                    gsr.setProcessTitle(p.getTitel());
                    gsr.setResultType(GoobiScriptResultType.RUNNING);
                    gsr.updateTimestamp();

                    for (Step step : p.getSchritteList()) {
                        if (step.getTitel().equalsIgnoreCase(steptitle)) {
                            Step so = StepManager.getStepById(step.getId());
                            if (scriptname != null) {
                                if (step.getAllScripts().containsKey(scriptname)) {
                                    String path = step.getAllScripts().get(scriptname);
                                    int returncode = hs.executeScriptForStepObject(so, path, false);
                                    Helper.addMessageToProcessLog(p.getId(), LogType.DEBUG, "Script '" + scriptname + "' for step '" + steptitle
                                            + "' executed using GoobiScript.", username);
                                    logger.info("Script '" + scriptname + "' for step '" + steptitle
                                            + "' executed using GoobiScript for process with ID " + p.getId());
                                    if (returncode == 0 || returncode == 98 || returncode == 99) {
                                        gsr.setResultMessage("Script '" + scriptname + "' for step '" + steptitle + "' executed successfully.");
                                        gsr.setResultType(GoobiScriptResultType.OK);
                                    } else {
                                        gsr.setResultMessage("A problem occured while executing script '" + scriptname + "' for step '" + steptitle
                                                + "': " + returncode);
                                        gsr.setResultType(GoobiScriptResultType.ERROR);
                                    }
                                } else {
                                    gsr.setResultMessage("Cant find script '" + scriptname + "' for step '" + steptitle + "'.");
                                    gsr.setResultType(GoobiScriptResultType.ERROR);
                                }
                            } else {
                                int returncode = hs.executeAllScriptsForStep(so, false);
                                Helper.addMessageToProcessLog(p.getId(), LogType.DEBUG, "All scripts for step '" + steptitle
                                        + "' executed using GoobiScript.", username);
                                logger.info("All scripts for step '" + steptitle + "' executed using GoobiScript for process with ID " + p.getId());
                                if (returncode == 0 || returncode == 98 || returncode == 99) {
                                    gsr.setResultMessage("All scripts for step '" + steptitle + "' executed successfully.");
                                    gsr.setResultType(GoobiScriptResultType.OK);
                                } else {
                                    gsr.setResultMessage("A problem occured while executing all scripts for step '" + steptitle + "': " + returncode);
                                    gsr.setResultType(GoobiScriptResultType.ERROR);
                                }
                            }
                        }
                    }
                    gsr.updateTimestamp();
                }
            }
        }
    }

}
