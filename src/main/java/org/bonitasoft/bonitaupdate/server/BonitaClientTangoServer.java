package org.bonitasoft.bonitaupdate.server;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.bonitaupdate.page.BonitaPatchJson;
import org.bonitasoft.bonitaupdate.page.PatchConfiguration;
import org.bonitasoft.bonitaupdate.patch.Patch;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.BonitaStoreAPI;
import org.bonitasoft.store.BonitaStoreBonitaExternalServer;
import org.bonitasoft.store.BonitaStoreBonitaExternalServer.RestApiResult;
import org.bonitasoft.store.BonitaStoreFactory;
import org.bonitasoft.store.rest.RESTCharsets;
import org.json.simple.JSONValue;

/**
 * List of patches is asked to the TANGO server
 * Note: a TANGO server is a Bonita Portal
 */
public class BonitaClientTangoServer {

    private final static BEvent eventListPatches = new BEvent(BonitaClientTangoServer.class.getName(), 1, Level.APPLICATIONERROR, "Get List patch on server", "Error during the operation on the server",
            "No patch list",
            "Check errors");
    private final static BEvent eventServerIsNotATangoServer = new BEvent(BonitaClientTangoServer.class.getName(), 2, Level.APPLICATIONERROR, "Not a Patch Server", "Server given in parameter is not a Patch server (the bonitaPage must be installed on this server, available for the user)",
            "No patch list",
            "Check configuration");
    private final static BEvent eventServerNotATangoResult = new BEvent(BonitaClientTangoServer.class.getName(), 3, Level.APPLICATIONERROR, "Not a Tango Result", "The server does not return a expect result for a Patch server (the bonitaPage must be installed on this server, available for the user)",
            "No patch list",
            "Check configuration");
    
    

    PatchConfiguration patchConfiguration;

    public BonitaClientTangoServer(PatchConfiguration patchConfiguration) {
        this.patchConfiguration = patchConfiguration;

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
            BonitaStoreFactory storeFactory = BonitaStoreAPI.getInstance().getBonitaStoreFactory();
            BonitaStoreBonitaExternalServer bonitaServer = storeFactory.getBonitaExternalServer(patchConfiguration.parametersConfiguration.tangoServerProtocol, 
                    patchConfiguration.parametersConfiguration.tangoServerName, 
                    patchConfiguration.parametersConfiguration.tangoServerPort, 
                    "bonita", 
                    patchConfiguration.parametersConfiguration.tangoServerUserName, 
                    patchConfiguration.parametersConfiguration.tangoServerPassword, false);
            
            // ask for a specific version
            Map<String,Object> paramToTheTangoServer = new HashMap<>();
            paramToTheTangoServer.put( BonitaPatchJson.CST_JSON_BONITAVERSION, patchConfiguration.parametersConfiguration.localBonitaVersion);
            String jsonParam = URLEncoder.encode( JSONValue.toJSONString(paramToTheTangoServer), "UTF-8");
            StringBuilder uri= new StringBuilder();
            uri.append("portal/custom-page/custompage_bonitaupdate?page=BonitaUpdate&action=tangoserverlistpatches");
            uri.append("&t="+System.currentTimeMillis());
            uri.append("&paramjson="+jsonParam);
            
            RestApiResult restApiResult = bonitaServer.callRestJson( uri.toString(), "GET", "", "application/json;charset=UTF-8", RESTCharsets.UTF_8.getValue());
            listPatch.listEvents.addAll(restApiResult.listEvents);
            if (restApiResult.httpStatus == 404 )
            {
                listPatch.listEvents.add( new BEvent(eventServerIsNotATangoServer, bonitaServer.getUrlDescription()));
                return listPatch;
            }
            // recalculate the list of patch
            List<Map<String, Object>> listPatchesFromServer = (List<Map<String, Object>>) restApiResult.jsonResult;
            if (listPatchesFromServer==null) {
                listPatch.listEvents.add( new BEvent(eventServerNotATangoResult, bonitaServer.getUrlDescription()));
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

}
