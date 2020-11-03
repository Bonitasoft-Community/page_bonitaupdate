package org.bonitasoft.bonitaupdate.page;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bonitasoft.bonitaupdate.toolbox.FileTool;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.properties.BonitaProperties;
import org.bonitasoft.web.extension.page.PageResourceProvider;

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
            return new File(patchFolder.getAbsolutePath() + File.separator + "local");
        if (FOLDER.UNINSTALL.equals(folder))
            return new File(patchFolder.getAbsolutePath() + File.separator + "local");
        if (FOLDER.DOWNLOAD.equals(folder))
            return new File(patchFolder.getAbsolutePath() + File.separator + "local");
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
