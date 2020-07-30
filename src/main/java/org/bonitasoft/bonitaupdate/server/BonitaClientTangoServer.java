package org.bonitasoft.bonitaupdate.server;

import java.util.List;
import java.util.Map;

import org.bonitasoft.bonitaupdate.page.PatchConfiguration;
import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory;
import org.bonitasoft.bonitaupdate.patch.Patch;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;
import org.bonitasoft.store.BonitaStoreAPI;
import org.bonitasoft.store.BonitaStoreBonitaExternalServer;
import org.bonitasoft.store.BonitaStoreBonitaExternalServer.RestApiResult;
import org.bonitasoft.store.BonitaStoreFactory;

/**
 * List of patches is asked to the TANGO server
 *
 */
public class BonitaClientTangoServer {
    
    PatchConfiguration patchConfiguration;
    public BonitaClientTangoServer(PatchConfiguration patchConfiguration) {
        this.patchConfiguration = patchConfiguration;
        
    }
    
    /**
     * Get the list of patch on the Tango server
     * @return
     */
    public ListPatches getListPatches() {
        BonitaStoreBonitaExternalServer bonitaServer = BonitaStoreAPI.getInstance().getBonitaStoreFactory().getBonitaExternalServer(patchConfiguration.serverProtocol, patchConfiguration.serverName, patchConfiguration.serverPort, "bonita", patchConfiguration.serverUserName, patchConfiguration.serverPassword,false);
        ListPatches listPatch = new ListPatches();
        RestApiResult restApiResult = bonitaServer.callRestJson("page=BonitaUpdate?action=tangoserverlistpatches","GET", "", "application/json;charset=UTF-8");
        listPatch.listEvents.addAll( restApiResult.listEvents);
        // recalculate the list of patch
        List<Map<String,Object>> listPatchesFromServer = (List<Map<String,Object>>) restApiResult.jsonResult;
        for(Map<String,Object> mapPatch: listPatchesFromServer) {
            Patch patch = Patch.getFromMap(mapPatch);
            patch.setStatus(STATUS.SERVER);
            listPatch.listPatch.add( patch );
        }
        return listPatch;
    }
    

}
