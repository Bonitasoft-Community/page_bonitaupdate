package org.bonitasoft.bonitaupdate.page;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.page.PatchConfiguration.ParametersConfiguration;
import org.bonitasoft.bonitaupdate.patch.Patch;
import org.bonitasoft.bonitaupdate.patch.Patch.LoadPatchResult;
import org.bonitasoft.bonitaupdate.patch.PatchDecoJson;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;
import org.bonitasoft.bonitaupdate.patch.PatchInstall;
import org.bonitasoft.bonitaupdate.patch.PatchInstall.ResultInstall;
import org.bonitasoft.bonitaupdate.server.BonitaClientTangoServer;
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

        public File bonitaRootDirectory;
        public String bonitaVersion;
        public APISession apiSession;

        public List<String> listPatchesName;

        ParametersConfiguration parametersConfiguration;

        @SuppressWarnings("unchecked")
        public static ParameterUpdate getInstanceFromJson(String jsonSt, APISession apiSession, File bonitaRootDirectory) {
            ParameterUpdate parameter = new ParameterUpdate();
            parameter.apiSession = apiSession;

            parameter.bonitaRootDirectory = bonitaRootDirectory;

            if (jsonSt == null) {
                parameter.parametersConfiguration = ParametersConfiguration.getDefault();
                parameter.parametersConfiguration.localBonitaVersion = parameter.detectBonitaVersion(bonitaRootDirectory);
                return parameter;
            }
            try {
                Map<String, Object> param = (Map<String, Object>) JSONValue.parse(jsonSt);
                parameter.listPatchesName = (List<String>) TypesCast.getList(param.get(BonitaPatchJson.CST_JSON_PATCHES), new ArrayList<>());
                parameter.bonitaVersion = TypesCast.getString(param.get(BonitaPatchJson.CST_JSON_BONITAVERSION), null);

                // Map paramServer = (Map<String, Object>) param.get(BonitaPatchJson.CST_JSON_PARAM);
                Map<String, Object> paramTango = (Map<String, Object>) param.get(BonitaPatchJson.CST_JSON_PARAMETERTANGO);
                parameter.parametersConfiguration = ParametersConfiguration.getInstanceFromJson(paramTango);

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
            return new PatchConfiguration(bonitaRootDirectory, bonitaVersion, apiSession, parametersConfiguration);
        }

        /**
         * there is no API which return the version (except the PlatformAPI, but you need to connect as the platform manager)
         * So, access the file VERSION and read the first line;
         * 
         * @param bonitaRootDirectory
         * @return
         */
        private String detectBonitaVersion(File bonitaRootDirectory) {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(bonitaRootDirectory.getPath() + "/webapps/bonita/VERSION"), StandardCharsets.UTF_8)) {
                this.bonitaVersion = reader.readLine();
                return this.bonitaVersion; // the version is the first line
            } catch (IOException e) {
                logger.severe("Can't read VERSION file under ["+bonitaRootDirectory.getPath() + "/webapps/bonita/VERSION"+"] : "+e.getMessage());
            }

            return null;

        }
    }

    public Map<String, Object> init(ParameterUpdate parameter) {
        // read all default information from the BonitaProperties
        List<BEvent> listEvents = new ArrayList<>();
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        listEvents.addAll(patchConfiguration.validateConfiguration());

        Map<String, Object> result = new HashMap<>();

        // next : read this information from BonitaProperties
        result.put(BonitaPatchJson.CST_JSON_PARAMETERTANGO, ParametersConfiguration.getDefault().toMap());

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

        if (parameter.bonitaVersion == null)
            parameter.detectBonitaVersion(parameter.bonitaRootDirectory);
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();
        // we ask the server to get patches for my Local version.
        patchConfiguration.parametersConfiguration.localBonitaVersion = parameter.bonitaVersion;

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

                if (!resultInstall.listEvents.isEmpty())
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
                if (!resultInstall.listEvents.isEmpty())
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
    /* PATCH SERVER (TANGO) : this method is on the patch server */
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

        BonitaTangoServer bonitaClientPatchServer = new BonitaTangoServer(patchConfiguration);

        ListPatches listPatchServer = bonitaClientPatchServer.getAvailablesPatch();
        result.put(BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(listPatchServer.listEvents));
        result.put(BonitaPatchJson.CST_JSON_LOCALPATCHED, PatchDecoJson.toJson(listPatchServer.listPatch));
        result.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, BEventFactory.isError(listEvents) ? CST_STATUS_FAILED : CST_STATUS_SUCCESS);

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
