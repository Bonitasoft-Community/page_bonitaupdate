package org.bonitasoft.bonitaupdate.page;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.bonitaupdate.toolbox.FileTool;
import org.bonitasoft.bonitaupdate.toolbox.TypesCast;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;

/**
 * contains everything is necessary for the configuration
 * 
 * @author Firstname Lastname
 */
public class PatchConfiguration {

    File serverFolder;
    File patchFolder;
    String bonitaVersion;
    /**
     *  name of the user who connect to this page
     * 
     */
    String connectionUserName;

    public enum FOLDER {
        BONITASERVER, INSTALL, UNINSTALL, DOWNLOAD, TANGOSERVER
    };

    public static class ParametersConfiguration {

        /**
         * the Tango server is a Bonita server
         */

        public String tangoServerProtocol;
        public String tangoServerName;
        public int tangoServerPort;
        public String tangoServerUserName;
        public String tangoServerPassword;

        /**
         * This is the BonitaVersion local, send to the server to get patches on this version
         */
        public String localBonitaVersion;

        public static ParametersConfiguration getInstanceFromJson(Map<String, Object> mapTango) {
            ParametersConfiguration parametersConfiguration = new ParametersConfiguration();
            if (mapTango==null)
                return parametersConfiguration;
            parametersConfiguration.tangoServerProtocol = TypesCast.getString(mapTango.get(BonitaPatchJson.CST_JSON_SERVERPROTOCOL), null);
            parametersConfiguration.tangoServerName = TypesCast.getString(mapTango.get(BonitaPatchJson.CST_JSON_SERVERNAME), null);
            parametersConfiguration.tangoServerPort = TypesCast.getInteger(mapTango.get(BonitaPatchJson.CST_JSON_SERVERPORT), 8080);

            parametersConfiguration.tangoServerUserName = TypesCast.getString(mapTango.get(BonitaPatchJson.CST_JSON_SERVERUSERNAME), null);
            parametersConfiguration.tangoServerPassword = TypesCast.getString(mapTango.get(BonitaPatchJson.CST_JSON_SERVERPASSWORD), null);
            return parametersConfiguration;
        }

        public static ParametersConfiguration getDefault() {
            ParametersConfiguration parametersConfiguration = new ParametersConfiguration();
            parametersConfiguration.tangoServerProtocol = "http";
            parametersConfiguration.tangoServerName = "localhost";
            parametersConfiguration.tangoServerPort = 8080;
            parametersConfiguration.tangoServerUserName = "helen.kelly";
            parametersConfiguration.tangoServerPassword = "bpm";
            return parametersConfiguration;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> mapTango = new HashMap<>();

            mapTango.put(BonitaPatchJson.CST_JSON_SERVERPROTOCOL, tangoServerProtocol);
            mapTango.put(BonitaPatchJson.CST_JSON_SERVERNAME, tangoServerName);
            mapTango.put(BonitaPatchJson.CST_JSON_SERVERPORT, tangoServerPort);
            mapTango.put(BonitaPatchJson.CST_JSON_SERVERUSERNAME, tangoServerUserName);
            mapTango.put(BonitaPatchJson.CST_JSON_SERVERPASSWORD, tangoServerPassword);
            return mapTango;
        }

    }

    public ParametersConfiguration parametersConfiguration = new ParametersConfiguration();

    public PatchConfiguration(File bonitaServerPath, String bonitaVersion, APISession apiSession, ParametersConfiguration parametersConfiguration) {
        this.serverFolder = bonitaServerPath;
        this.bonitaVersion = bonitaVersion;
        this.patchFolder = new File(bonitaServerPath.getAbsolutePath() + File.separator + "patches");
        this.parametersConfiguration = parametersConfiguration;
        this.connectionUserName = apiSession.getUserName();

    }

    /**
     * This method should be call to validate the configuration
     * 
     * @return
     */
    public List<BEvent> validateConfiguration() {
        List<BEvent> listEvents = new ArrayList<>();
        listEvents.addAll(FileTool.checkAndCreateDir(this.getFolderPath(FOLDER.INSTALL)));
        listEvents.addAll(FileTool.checkAndCreateDir(this.getFolderPath(FOLDER.DOWNLOAD)));
        listEvents.addAll(FileTool.checkAndCreateDir(this.getFolderPath(FOLDER.UNINSTALL)));
        listEvents.addAll(FileTool.checkAndCreateDir(this.getFolderPath(FOLDER.TANGOSERVER)));
        return listEvents;
    }

    public File getFolder(FOLDER folder) {
        if (FOLDER.BONITASERVER.equals(folder))
            return serverFolder;
        if (FOLDER.INSTALL.equals(folder))
            return new File(patchFolder.getAbsolutePath() + File.separator + "installed");
        if (FOLDER.UNINSTALL.equals(folder))
            return new File(patchFolder.getAbsolutePath() + File.separator + "uninstalled");
        if (FOLDER.DOWNLOAD.equals(folder))
            return new File(patchFolder.getAbsolutePath() + File.separator + "downloaded");
        if (FOLDER.TANGOSERVER.equals(folder))
            return new File(patchFolder.getAbsolutePath() + File.separator + "reference");
        return null;
    }

    public String getFolderPath(FOLDER folder) {
        File file = getFolder(folder);
        if (file == null)
            return null;
        return file.getAbsolutePath() + File.separator;
    }

}
