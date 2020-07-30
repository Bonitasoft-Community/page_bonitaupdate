package org.bonitasoft.bonitaupdate.page;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.patch.Patch;
import org.bonitasoft.bonitaupdate.patch.Patch.LoadPatchResult;
import org.bonitasoft.bonitaupdate.patch.PatchDecoJson;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;
import org.bonitasoft.bonitaupdate.patch.PatchInstall.ResultInstall;
import org.bonitasoft.bonitaupdate.server.BonitaClientTangoServer;
import org.bonitasoft.bonitaupdate.patch.PatchInstall;
import org.bonitasoft.bonitaupdate.toolbox.TypesCast;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.json.simple.JSONValue;

public class BonitaUpdateAPI {

    private static final String CST_STATUS_SUCCESS = "SUCCESS";
    private static final String CST_STATUS_FAILED = "FAILED";
    static Logger logger = Logger.getLogger(BonitaUpdateAPI.class.getName());

    public static class ParameterUpdate {

        public String serverUrl;
        public File bonitaRootDirectory;
        public String bonitaVersion;

        public List<String> listPatchesName;

        public String serverProtocol;
        public String serverName;
        public int serverPort;
        public String serverUserName;
        public String serverPassword;
        public String userName;

        public static ParameterUpdate getInstanceFromJson(String jsonSt, APISession apiSession) {
            ParameterUpdate parameter = new ParameterUpdate();
            if (jsonSt == null)
                return parameter;

            try {
                Map param = (Map<String, Object>) JSONValue.parse(jsonSt);
                parameter.listPatchesName = (List<String>) TypesCast.getList(param.get(BonitaPatchJson.CST_JSON_PATCHES), new ArrayList());

                Map paramServer = (Map<String, Object>) param.get(BonitaPatchJson.CST_JSON_PARAM);

                parameter.serverUrl = TypesCast.getString(paramServer.get(BonitaPatchJson.CST_JSON_SERVERURL), null);

                parameter.serverProtocol = TypesCast.getString(paramServer.get(BonitaPatchJson.CST_JSON_SERVERPROTOCOL), null);
                parameter.serverName = TypesCast.getString(paramServer.get(BonitaPatchJson.CST_JSON_SERVERNAME), null);
                parameter.serverPort = TypesCast.getInteger(paramServer.get(BonitaPatchJson.CST_JSON_SERVERPORT), 8080);
                parameter.serverUserName = TypesCast.getString(paramServer.get(BonitaPatchJson.CST_JSON_SERVERUSERNAME), null);
                parameter.serverPassword = TypesCast.getString(paramServer.get(BonitaPatchJson.CST_JSON_SERVERPASSWORD), null);
                parameter.userName = apiSession.getUserName();

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionDetails = sw.toString();
                logger.severe("Parameter: ~~~~~~~~~~  : ERROR " + e + " at " + exceptionDetails);
            }
            return parameter;
        }

        public void setBonitaRootDirectory(File bonitaRootDirectory) {
            this.bonitaRootDirectory = bonitaRootDirectory;
        }

        public void setBonitaVersion(String bonitaVersion) {
            this.bonitaVersion = bonitaVersion;
        }

        public PatchConfiguration getPatchConfiguration() {
            return new PatchConfiguration(bonitaRootDirectory, bonitaVersion, serverProtocol, serverName, serverPort, serverUserName, serverPassword, userName);
        }
    }

    public Map<String, Object> init(ParameterUpdate parameter) {
        // read all default information from the BonitaProperties
        List<BEvent> listEvents = new ArrayList<>();
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        listEvents.addAll(patchConfiguration.validateConfiguration());

        Map<String, Object> result = new HashMap<>();
        result.put(BonitaPatchJson.CST_JSON_SERVERPROTOCOL, "http");
        result.put(BonitaPatchJson.CST_JSON_SERVERNAME, "localhost");
        result.put(BonitaPatchJson.CST_JSON_SERVERPORT, 8080);
        result.put(BonitaPatchJson.CST_JSON_SERVERUSERNAME, "helen.kelly");
        result.put(BonitaPatchJson.CST_JSON_SERVERPASSWORD, "bpm");
        ResultRefresh resultRefresh = getListPatches(patchConfiguration);
        listEvents.addAll(resultRefresh.listEvents);

        result.put(BonitaPatchJson.CST_JSON_LOCALPATCHED, PatchDecoJson.toJson(resultRefresh.listAllPatches));
        result.put(BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(listEvents));

        return result;
    }

    /**
     * do the startup and the first refresh
     * 
     * @param parameter
     * @return
     */

    public Map<String, Object> refresh(ParameterUpdate parameter) {
        Map<String, Object> result = new HashMap<>();
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        ResultRefresh resultRefresh = getListPatches(patchConfiguration);

        result.put(BonitaPatchJson.CST_JSON_LOCALPATCHED, PatchDecoJson.toJson(resultRefresh.listAllPatches));
        result.put(BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(resultRefresh.listEvents));

        result.put(BonitaPatchJson.CST_JSON_LOCALPATCHED, PatchDecoJson.toJson(resultRefresh.listAllPatches));

        return result;
    }

    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Server refesh/download */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * @param parameter
     * @return
     */
    public Map<String, Object> refreshServer(ParameterUpdate parameter) {
        Map<String, Object> result = new HashMap<>();

        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        // Contact the server
        BonitaClientTangoServer bonitaPatchServer = new BonitaClientTangoServer(patchConfiguration);

        ListPatches listPatchServer = bonitaPatchServer.getListPatches();
        result.put(BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(listPatchServer.listEvents));
        result.put(BonitaPatchJson.CST_JSON_LOCALPATCHED, PatchDecoJson.toJson(listPatchServer.listPatch));

        return result;

    }
    
    public Map<String, Object> download(ParameterUpdate parameter) {
        Map<String, Object> result = new HashMap<>();
        return result;

    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Operation install/ uninstall method */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * Install a Downloaded patch
     * 
     * @param parameter
     * @return
     */
    public Map<String, Object> install(ParameterUpdate parameter) {
        Map<String, Object> result = new HashMap<>();
        List<BEvent> listEvents = new ArrayList<>();
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        BonitaLocalServer bonitaClientPatchServer = new BonitaLocalServer(patchConfiguration);
        List<Map<String, Object>> listStatusPatches = new ArrayList<>();

        PatchInstall patchInstall = new PatchInstall();
        for (String patchName : parameter.listPatchesName) {
            Map<String, Object> statusPatch = new HashMap<>();
            listStatusPatches.add(statusPatch);
            statusPatch.put(BonitaPatchJson.CST_JSON_PATCHNAME, patchName);
            LoadPatchResult loadedPatch = bonitaClientPatchServer.getPatchByName(FOLDER.DOWNLOAD, patchName);
            if (loadedPatch.patch == null) {
                listEvents.addAll(loadedPatch.listEvents);
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSLISTEVENTS, BEventFactory.getSyntheticHtml(loadedPatch.listEvents));
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, CST_STATUS_FAILED);

            } else {
                ResultInstall resultInstall = patchInstall.installPatch(patchConfiguration, loadedPatch.patch);
                listEvents.addAll(resultInstall.listEvents);
                statusPatch.put(BonitaPatchJson.CST_JSON_PATCHSTATUS, resultInstall.statusPatch.toString());

                if (resultInstall.listEvents.size()>0)
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSLISTEVENTS, BEventFactory.getSyntheticHtml(resultInstall.listEvents));
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, BEventFactory.isError(listEvents) ? CST_STATUS_FAILED : CST_STATUS_SUCCESS);
            }
        }
        // result.put( BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(listEvents));
        result.put(BonitaPatchJson.CST_JSON_LISTPATCHOPERATIONSTATUS, listStatusPatches);
        result.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, BEventFactory.isError(listEvents) ? CST_STATUS_FAILED : CST_STATUS_SUCCESS);

        return result;

    }

    public Map<String, Object> uninstall(ParameterUpdate parameter) {
        Map<String, Object> result = new HashMap<>();
        List<BEvent> listEvents = new ArrayList<>();
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        BonitaLocalServer bonitaClientPatchServer = new BonitaLocalServer(patchConfiguration);
        List<Map<String, Object>> listStatusPatches = new ArrayList<>();

        PatchInstall patchInstall = new PatchInstall();
        for (String patchName : parameter.listPatchesName) {
            Map<String, Object> statusPatch = new HashMap<>();
            listStatusPatches.add(statusPatch);
            statusPatch.put(BonitaPatchJson.CST_JSON_PATCHNAME, patchName);
            LoadPatchResult loadedPatch = bonitaClientPatchServer.getPatchByName(FOLDER.INSTALL, patchName);
            if (loadedPatch.patch == null) {
                listEvents.addAll(loadedPatch.listEvents);
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSLISTEVENTS, BEventFactory.getSyntheticHtml(loadedPatch.listEvents));
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, CST_STATUS_FAILED);
            } else {
                ResultInstall resultInstall = patchInstall.unInstallPatch(patchConfiguration, loadedPatch.patch);
                listEvents.addAll(resultInstall.listEvents);
                statusPatch.put(BonitaPatchJson.CST_JSON_PATCHSTATUS, resultInstall.statusPatch.toString());
                if (resultInstall.listEvents.size()>0)
                    statusPatch.put(BonitaPatchJson.CST_JSON_STATUSLISTEVENTS, BEventFactory.getSyntheticHtml(resultInstall.listEvents));
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, BEventFactory.isError(listEvents) ? CST_STATUS_FAILED : CST_STATUS_SUCCESS);
            }
        }
        // result.put( BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(listEvents));
        result.put(BonitaPatchJson.CST_JSON_LISTPATCHOPERATIONSTATUS, listStatusPatches);
        result.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, BEventFactory.isError(listEvents) ? CST_STATUS_FAILED : CST_STATUS_SUCCESS);

        return result;
    }

  
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* PATCH SERVER (TANGO) : this method is on the patch server                    */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * List all patch on the Server
     * 
     * @param parameter
     * @return
     */
    public Map<String, Object> tangoserverListPatches(ParameterUpdate parameter) {
        Map<String, Object> result = new HashMap<>();
        List<BEvent> listEvents = new ArrayList<>();
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        BonitaLocalServer bonitaClientPatchServer = new BonitaTangoServer(patchConfiguration);

        result.put("listevent", BEventFactory.getHtml(listEvents));
        result.put("status", BEventFactory.isError(listEvents) ? CST_STATUS_FAILED : CST_STATUS_SUCCESS);

        return result;

    }
    
    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Internal method */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public static class ResultRefresh {

        List<BEvent> listEvents = new ArrayList<>();
        List<Patch> listAllPatches = new ArrayList<>();

    }

    private ResultRefresh getListPatches(PatchConfiguration patchConfiguration) {
        ResultRefresh resultRefresh = new ResultRefresh();
        // get the local patch installed
        BonitaLocalServer bonitaLocalServer = new BonitaLocalServer(patchConfiguration);

        if (!BEventFactory.isError(resultRefresh.listEvents)) {

            ListPatches listPatchInstalled = bonitaLocalServer.getInstalledPatch();
            ListPatches listPatchedDownloaded = bonitaLocalServer.getDownloadedPatch();
            resultRefresh.listEvents.addAll(listPatchInstalled.listEvents);
            resultRefresh.listEvents.addAll(listPatchedDownloaded.listEvents);

            resultRefresh.listAllPatches.addAll(listPatchInstalled.listPatch);
            resultRefresh.listAllPatches.addAll(listPatchedDownloaded.listPatch);
        }
        return resultRefresh;

    }

}
