package org.bonitasoft.bonitaupdate.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.bonitasoft.bonitaupdate.page.BonitaLocalServer;
import org.bonitasoft.bonitaupdate.page.BonitaPatchJson;
import org.bonitasoft.bonitaupdate.page.PatchConfiguration;
import org.bonitasoft.bonitaupdate.patch.Patch;
import org.bonitasoft.bonitaupdate.patch.Patch.LoadPatchResult;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.store.BonitaStoreAPI;
import org.bonitasoft.store.BonitaStoreBonitaExternalServer;
import org.bonitasoft.store.BonitaStoreBonitaExternalServer.RestApiResult;
import org.bonitasoft.store.BonitaStoreFactory;
import org.bonitasoft.store.rest.CollectOutput;
import org.bonitasoft.store.rest.RESTCall;
import org.bonitasoft.store.rest.RESTCharsets;
import org.json.simple.JSONValue;

/**
 * List of patches is asked to the TANGO server
 * Note: a TANGO server is a Bonita Portal
 */
public class BonitaClientTangoServer {

    static Logger logger = Logger.getLogger(BonitaClientTangoServer.class.getName());

    private final static BEvent eventListPatches = new BEvent(BonitaClientTangoServer.class.getName(), 1, Level.APPLICATIONERROR, "Get List patch on server", "Error during the operation on the server",
            "No patch list",
            "Check errors");
    private final static BEvent eventServerIsNotATangoServer = new BEvent(BonitaClientTangoServer.class.getName(), 2, Level.APPLICATIONERROR, "Not a Patch Server", "Server given in parameter is not a Patch server (the bonitaPage must be installed on this server, available for the user)",
            "No patch list",
            "Check configuration");
    private final static BEvent eventServerNotATangoResult = new BEvent(BonitaClientTangoServer.class.getName(), 3, Level.APPLICATIONERROR, "Not a Tango Result", "The server does not return a expect result for a Patch server (the bonitaPage must be installed on this server, available for the user)",
            "No patch list",
            "Check configuration");

    private final static BEvent eventErrorWriteDownloadedFile = new BEvent(BonitaClientTangoServer.class.getName(), 4, Level.APPLICATIONERROR, "Error during download file", "Download a Patch file has an error",
            "Patch is not downloaded",
            "Check error");

    PatchConfiguration patchConfiguration;

    BonitaStoreBonitaExternalServer bonitaServer;

    public BonitaClientTangoServer(PatchConfiguration patchConfiguration) {
        this.patchConfiguration = patchConfiguration;
        BonitaStoreFactory storeFactory = BonitaStoreAPI.getInstance().getBonitaStoreFactory();
        bonitaServer = storeFactory.getInstanceBonitaExternalServer(patchConfiguration.parametersConfiguration.tangoServerProtocol,
                patchConfiguration.parametersConfiguration.tangoServerName,
                patchConfiguration.parametersConfiguration.tangoServerPort,
                "bonita",
                patchConfiguration.parametersConfiguration.tangoServerUserName,
                patchConfiguration.parametersConfiguration.tangoServerPassword, false);

    }

    /**
     * Get the list of patch on the Tango server
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public ListPatches getListPatches() {
        ListPatches listPatch = new ListPatches();
        try {

            // ask for a specific version
            Map<String, Object> paramToTheTangoServer = new HashMap<>();
            paramToTheTangoServer.put(BonitaPatchJson.CST_JSON_BONITAVERSION, patchConfiguration.parametersConfiguration.localBonitaVersion);
            String jsonParam = URLEncoder.encode(JSONValue.toJSONString(paramToTheTangoServer), "UTF-8");
            StringBuilder uri = new StringBuilder();
            uri.append("portal/custom-page/custompage_bonitaupdate/?page=BonitaUpdate&action=tangoListPatches");
            uri.append("&t=" + System.currentTimeMillis());
            uri.append("&paramjson=" + jsonParam);

            CollectOutput collectOutput = CollectOutput.getInstanceJson();
            RestApiResult restApiResult = bonitaServer.callRestJson(uri.toString(), "GET", "", "application/x-www-form-urlencoded", RESTCharsets.UTF_8.getValue(), 
                    collectOutput);
            listPatch.listEvents.addAll(restApiResult.listEvents);
            if (BEventFactory.isError(listPatch.listEvents))
                return listPatch;
            if (restApiResult.httpStatus == 404) {
                listPatch.listEvents.add(new BEvent(eventServerIsNotATangoServer, bonitaServer.getUrlDescription()));
                return listPatch;
            }
            // recalculate the list of patch
            Map<String, Object> jsonMap = (Map<String, Object>) collectOutput.getJson();

            if (jsonMap.get("listevents") instanceof List) {
                try {
                    listPatch.listEvents.addAll( BEventFactory.getListEventsFromJson( (List<Map<String, Serializable>>) jsonMap.get("listevents")));
                    
                } catch(Exception e) {}
            }
            List<Map<String, Object>> listPatchesFromServer = jsonMap == null ? null : (List<Map<String, Object>>) jsonMap.get("localPatches");
            if (listPatchesFromServer == null) {
                listPatch.listEvents.add(new BEvent(eventServerNotATangoResult, bonitaServer.getUrlDescription()));
                return listPatch;
            }
            for (Map<String, Object> mapPatch : listPatchesFromServer) {
                Patch patch = Patch.getFromMap(mapPatch);
                patch.setStatus(STATUS.SERVER);
                listPatch.listPatch.add(patch);
            }
        } catch (Exception e) {
            listPatch.listEvents.add(new BEvent(eventListPatches, e, ""));
        }
        return listPatch;
    }

    /**
     * bonitaServer keep as private the login information.
     * 
     * @param listPatchServer
     * @param bonitaLocalServer
     * @return
     */
    public ListPatches download(ListPatches listPatchServer, BonitaLocalServer bonitaLocalServer) {
        // download file per file
        ListPatches listPatchDownloaded = new ListPatches();
        try {

            for (Patch patch : listPatchServer.listPatch) {
                // ask for a specific version
                /*
                 * Map<String, Object> paramToTheTangoServer = new HashMap<>();
                 * paramToTheTangoServer.put(BonitaPatchJson.CST_JSON_BONITAVERSION, patchConfiguration.parametersConfiguration.localBonitaVersion);
                 * paramToTheTangoServer.put(BonitaPatchJson.CST_JSON_PATCHNAME, patch.getName());
                 * String jsonParam = URLEncoder.encode(JSONValue.toJSONString(paramToTheTangoServer), "UTF-8");
                 */
                Map<String, Object> paramToTheTangoServer = new HashMap<>();
                paramToTheTangoServer.put(BonitaPatchJson.CST_JSON_BONITAVERSION, patchConfiguration.parametersConfiguration.localBonitaVersion);
                paramToTheTangoServer.put(BonitaPatchJson.CST_JSON_PATCHNAME, patch.getName());
                String jsonParam = URLEncoder.encode(JSONValue.toJSONString(paramToTheTangoServer), "UTF-8");

                StringBuilder uri = new StringBuilder();
                uri.append("portal/custom-page/custompage_bonitaupdate/?page=BonitaUpdate&action=tangoGetPatchFile");
                uri.append("&t=" + System.currentTimeMillis());
                uri.append("&paramjson=" + jsonParam);

                // bonitaServer.resetConnection();
                // create new CollectOutput
                PatchDirectory patchDirectory = bonitaLocalServer.getDownloadedPatchDirecty();
                CollectOutput collectOutput = CollectOutput.getInstanceFileName(patchDirectory.getPath() + "/" + patch.getFileName());
                RestApiResult restApiResult = bonitaServer.callRestJson(uri.toString(), "GET", "", "application/json;charset=UTF-8", RESTCharsets.UTF_8.getValue(), collectOutput);
                logger.info("download Patch [" + patch.getName() + "] History=" + restApiResult.restResponse.getHistoryCall());
                listPatchDownloaded.listEvents.addAll(restApiResult.listEvents);
                listPatchDownloaded.listEvents.addAll(collectOutput.getListEvents());

                if (restApiResult.httpStatus == 404) {
                    listPatchDownloaded.listEvents.add(new BEvent(eventServerIsNotATangoServer, bonitaServer.getUrlDescription()));
                    return listPatchDownloaded;
                }
                // check that the file is a patch
                if (BEventFactory.isError(restApiResult.listEvents) || BEventFactory.isError(collectOutput.getListEvents())) {
                    patch.setStatus( STATUS.DOWNLOADEDWITHERROR);
                }
                else {
                    File filePatch = new File( patchDirectory.getPath() + "/" + patch.getFileName());
                
                    LoadPatchResult loadPatchResult = Patch.loadFromFile(STATUS.SERVER, filePatch);
                    if (BEventFactory.isError(loadPatchResult.listEvents)) {
                        // this is not a patch !
                        patch.setStatus( STATUS.ERROR);
                        listPatchDownloaded.listEvents.addAll( loadPatchResult.listEvents);
                        try { filePatch.delete(); } catch(Exception e) {};
                    } else {
                    patch.setStatus( STATUS.DOWNLOADEDWITHSUCCESS);
                    }
                }
                listPatchDownloaded.listPatch.add(patch);

            }
        } catch (Exception e) {
            listPatchDownloaded.listEvents.add(new BEvent(eventErrorWriteDownloadedFile, "error " + e.getMessage()));
        }
        return listPatchDownloaded;
    }

}
