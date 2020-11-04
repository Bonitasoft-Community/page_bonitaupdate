package org.bonitasoft.bonitaupdate.page;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.patch.Patch;
import org.bonitasoft.bonitaupdate.patch.Patch.LoadPatchResult;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.patch.PatchDecoJson;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;
import org.bonitasoft.bonitaupdate.patch.PatchInstall;
import org.bonitasoft.bonitaupdate.patch.PatchInstall.ResultInstall;
import org.bonitasoft.bonitaupdate.server.BonitaClientTangoServer;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.web.extension.page.PageResourceProvider;

public class BonitaUpdateAPI {

    private static final String CST_STATUS_SUCCESS = "SUCCESS";
    private static final String CST_STATUS_FAILED = "FAILED";
    static Logger logger = Logger.getLogger(BonitaUpdateAPI.class.getName());

    private final static BEvent eventPatchDownloadedSynthesis = new BEvent(BonitaUpdateAPI.class.getName(), 1, Level.INFO, "Status Download", "Status patch downloaded");
    private final static BEvent eventErrorDuringExecution = new BEvent(BonitaUpdateAPI.class.getName(), 2, Level.ERROR, "Error during execution", "Error during the execution of a command", "Command failed", "Check exception");

    public Map<String, Object> init(ParameterUpdate parameter, PageResourceProvider pageResourceProvider) {
        // read all default information from the BonitaProperties
        List<BEvent> listEvents = new ArrayList<>();
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        listEvents.addAll(patchConfiguration.validateConfiguration());

        Map<String, Object> result = new HashMap<>();

        // next : read this information from BonitaProperties
        ParametersConfiguration parametersConfiguration = new ParametersConfiguration();
        listEvents.addAll(parametersConfiguration.load(true, pageResourceProvider));
        result.put(BonitaPatchJson.CST_JSON_PARAMETERTANGO, parametersConfiguration.toMap());

        ResultRefresh resultRefresh = getListPatches(patchConfiguration);
        listEvents.addAll(resultRefresh.listEvents);

        result.put(BonitaPatchJson.CST_JSON_LOCALPATCHED, PatchDecoJson.getInstance().toJsonSortedBySequence(resultRefresh.listAllPatches));
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

        result.put(BonitaPatchJson.CST_JSON_LOCALPATCHED, PatchDecoJson.getInstance().toJsonSortedBySequence( resultRefresh.listAllPatches));
        result.put(BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(resultRefresh.listEvents));

        result.put(BonitaPatchJson.CST_JSON_LOCALPATCHED, PatchDecoJson.getInstance().toJsonSortedBySequence(resultRefresh.listAllPatches));

        return result;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Server refesh/download */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * Call the Reference (Tango) server to get the list of patch available
     * 
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
        BonitaClientTangoServer bonitaClientTangoServer = new BonitaClientTangoServer(patchConfiguration);

        ListPatches listPatchServer = bonitaClientTangoServer.getListPatches();

        BonitaLocalServer bonitaLocalServer = new BonitaLocalServer(patchConfiguration);
        ListPatches listPatchInstalled = bonitaLocalServer.getInstalledPatch();
        ListPatches listPatchedDownloaded = bonitaLocalServer.getDownloadedPatch();

        for(Patch patch : listPatchServer.listPatch) {
            if (listPatchInstalled.isContains( patch.getName()))
                patch.setStatus(STATUS.INSTALLED);
            else if (listPatchedDownloaded.isContains( patch.getName()))
                patch.setStatus(STATUS.DOWNLOADED);
        }
        // listPatchServer.removeFromList(listPatchInstalled);
        // listPatchServer.removeFromList(listPatchedDownloaded);

        result.put(BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(listPatchServer.listEvents));
        result.put(BonitaPatchJson.CST_JSON_SERVERPATCHES, PatchDecoJson.getInstance().toJsonSortedBySequence(listPatchServer.listPatch));

        return result;
    }

    /**
     * Dowload all patch from server
     * 
     * @param parameter
     * @return
     */
    public Map<String, Object> downloadAllPatches(ParameterUpdate parameter) {
        Map<String, Object> result = new HashMap<>();
        ArrayList<BEvent> listEvents = new ArrayList<>();
        
        if (parameter.bonitaVersion == null)
            parameter.detectBonitaVersion(parameter.bonitaRootDirectory);
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();
        // we ask the server to get patches for my Local version.
        patchConfiguration.parametersConfiguration.localBonitaVersion = parameter.bonitaVersion;

        // Contact the server
        BonitaClientTangoServer bonitaClientTangoServer = new BonitaClientTangoServer(patchConfiguration);

        // calculated the list of patch to download
        ListPatches listPatchServer = bonitaClientTangoServer.getListPatches();
        int numberPatchesOnServer = listPatchServer.listPatch.size();
        listEvents.addAll(listPatchServer.listEvents);

        BonitaLocalServer bonitaLocalServer = new BonitaLocalServer(patchConfiguration);
        ListPatches allPatches = new ListPatches();
        ListPatches listPatchDownloaded = null;
        ListPatches listPatchInstalled = bonitaLocalServer.getInstalledPatch();
        ListPatches listPatchedDownloaded = bonitaLocalServer.getDownloadedPatch();
        int numberOfPatchesLocal = listPatchInstalled.listPatch.size() + listPatchedDownloaded.listPatch.size();
        
        if (! BEventFactory.isError(listEvents)) {
                    
            listPatchServer.removeFromList(listPatchInstalled);
            listPatchServer.removeFromList(listPatchedDownloaded);
    
            listPatchDownloaded = bonitaClientTangoServer.download(listPatchServer, bonitaLocalServer);
            listPatchDownloaded.listEvents.add(new BEvent(eventPatchDownloadedSynthesis, "Patches on server:" + numberPatchesOnServer + "; patches locals:" + numberOfPatchesLocal + "; patches Downloaded:" + listPatchDownloaded.listPatch.size()));
            listEvents.addAll( listPatchDownloaded.listEvents );
 
            for(Patch patch : listPatchDownloaded.listPatch) {
                allPatches.listPatch.add( patch);
            }
        }
        // rebuild the complete liste
        for(Patch patch : listPatchInstalled.listPatch) {
            patch.setStatus(STATUS.INSTALLED);
            allPatches.listPatch.add( patch);
        }
        for(Patch patch : listPatchedDownloaded.listPatch) {
            if (listPatchDownloaded !=null && ! listPatchDownloaded.isContains(patch.getName() ) ) {
                patch.setStatus(STATUS.DOWNLOADED);
            allPatches.listPatch.add( patch);
        }
        }

        
          
        result.put(BonitaPatchJson.CST_JSON_SERVERPATCHES, PatchDecoJson.getInstance().toJsonSortedBySequence(allPatches.listPatch));
        result.put(BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(listEvents));

        return result;

    }
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Operation install/ uninstall method */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * Install a list of downloaded patch in the list listPatchesName
     * 
     * @param parameter
     * @return
     */
    public Map<String, Object> install(ParameterUpdate parameter) {
        Map<String, Object> result = new HashMap<>();
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        BonitaLocalServer bonitaClientPatchServer = new BonitaLocalServer(patchConfiguration);
        PatchInstall patchInstall = new PatchInstall();

        // first step, Reorder the list, and complete it by 
        ResultInstall resultInstall = patchInstall.installManagement(parameter.listPatchesName, bonitaClientPatchServer, patchConfiguration);

        // result.put( BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(listEvents));
        result.put(BonitaPatchJson.CST_JSON_LISTPATCHOPERATIONSTATUS, resultInstall.listStatusPatches);
        result.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, BEventFactory.isError(resultInstall.listEvents) ? CST_STATUS_FAILED : CST_STATUS_SUCCESS);

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
    public Map<String, Object> tangoListPatches(ParameterUpdate parameter) {
        Map<String, Object> result = new HashMap<>();
        List<BEvent> listEvents = new ArrayList<>();
        try {

            PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();
            BonitaTangoServer bonitaTangoPatchServer = new BonitaTangoServer(patchConfiguration);

            ListPatches listPatchServer = bonitaTangoPatchServer.getAvailablesPatch();
            listEvents.addAll(listPatchServer.listEvents);
            result.put(BonitaPatchJson.CST_JSON_LOCALPATCHED, PatchDecoJson.getInstance().toJsonSortedBySequence(listPatchServer.listPatch));
            result.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, BEventFactory.isError(listEvents) ? CST_STATUS_FAILED : CST_STATUS_SUCCESS);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("Error " + e.getMessage() + " at " + exceptionDetails);
            listEvents.add( new BEvent(eventErrorDuringExecution, e, "During tangoserverListPatches at "+exceptionDetails));
        }
        result.put(BonitaPatchJson.CST_JSON_LISTEVENTS, BEventFactory.getHtml(listEvents));
        return result;

    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Download patch */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * return the header. Header must be done beofre
     * 
     * @param parameter
     * @return
     */
    public void tangoDownloadPatch(ParameterUpdate parameter, HttpServletResponse response) {

        // first, put the head in the response

        response.addHeader("content-disposition", "attachment; filename=" + parameter.patchName + ".zip");
        response.addHeader("content-type", "application/zip");

        // write the content
        PatchConfiguration patchConfiguration = parameter.getPatchConfiguration();

        BonitaTangoServer bonitaTangoPatchServer = new BonitaTangoServer(patchConfiguration);
        LoadPatchResult loadPatchResult = bonitaTangoPatchServer.getPatchByName(parameter.patchName);
        if (loadPatchResult.patch == null)
            return;

        try (InputStream instream = new FileInputStream(loadPatchResult.patch.getPatchFile())) {

            OutputStream output = response.getOutputStream();

            // the document is not uploaded - not consider as an error
            int totalByteRead=0;
            byte[] buffer = new byte[10000];
            while (true) {
                int bytesRead;
                bytesRead = instream.read(buffer);
                if (bytesRead == -1)
                    break;
                totalByteRead+= bytesRead;
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
            output.close();
            logger.info("tangoDownloadPatch File["+loadPatchResult.patch.getPatchFile()+"] bytes=["+totalByteRead+"]");
        } catch (IOException e) {
            logger.severe(".getDownloadPatch: error writing output " + e.getMessage());
        }
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Update parameters */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public Map<String, Object> updateParameters(ParameterUpdate parameter, PageResourceProvider pageResourceProvider) {
        List<BEvent> listEvents = parameter.parametersConfiguration.save(pageResourceProvider);
        Map<String, Object> result = new HashMap<>();
        result.put("listevents", BEventFactory.getHtml(listEvents));
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
