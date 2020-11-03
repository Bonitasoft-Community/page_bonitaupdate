package org.bonitasoft.bonitaupdate.page;

import java.io.File;
import java.util.List;

import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.patch.Patch.LoadPatchResult;
import org.bonitasoft.bonitaupdate.patch.Patch.SCOPE;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;
import org.bonitasoft.log.event.BEvent;

/**
 * Tango Server
 * This deliver all patch on the server. 
 * This is first all patches that it got. 
 * 
 * Because a TANGO server may be a Bonita Server, 
 *      example PRODUCTION => VALIDATION
 *              or the Bonita Reference TANGO server
 * 
 * Nota : how a server knows it is a tango? In fact, it don't know. When the url "tangoserverlistpatches" is called, this method is called. 
 * Then, any server may be the tango on an another server 
 */
public class BonitaTangoServer extends BonitaLocalServer {

    public BonitaTangoServer(PatchConfiguration patchConfiguration) {
        super(patchConfiguration);
    }

    public ListPatches getAvailablesPatch() {
        ListPatches listPatches = new ListPatches();
        listPatches.add(getInstalledPatch());
        listPatches.add(getDownloadedPatch());

        // now, get the local path with the complete version (example, 7.8.4)
        File folderTangoRelease = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + patchConfiguration.bonitaVersion);

        PatchDirectory patchDirectoryRelease = new PatchDirectory(STATUS.SERVER, folderTangoRelease, SCOPE.PUBLIC);
        listPatches.add(patchDirectoryRelease.getListPatches( null ));
        File folderUserName = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + patchConfiguration.bonitaVersion + File.separator + patchConfiguration.connectionUserName);
        if (folderUserName.exists()) {
            PatchDirectory patchDirectoryUserRelease = new PatchDirectory(STATUS.SERVER, folderUserName, SCOPE.PRIVATE);
            listPatches.add( patchDirectoryUserRelease.getListPatches( null ) );
        }
        
        listPatches.removeDuplicates();
        return listPatches;
    }
    
    
  
    public LoadPatchResult getPatchByName( String patchName) {
        List<PatchDirectory> listPatchDirectory = getListPatchDirectory();
        for (PatchDirectory patchDirectory : listPatchDirectory) {
            LoadPatchResult loadPatchResult= patchDirectory.getPatchByName(patchName);
            if (loadPatchResult.patch!=null)
                return loadPatchResult;
        }
        LoadPatchResult loadPatchResult = new LoadPatchResult();
        loadPatchResult.listEvents.add( new BEvent( PatchDirectory.eventPatchDoesNotExist, "Patch["+patchName+"]"));
        return loadPatchResult;
    }
    
    @Override
    protected List<PatchDirectory> getListPatchDirectory() {
        List<PatchDirectory> listPatchDirectory = super.getListPatchDirectory();
        
        
        // now, get the local path with the complete version (example, 7.8.4)
        File folderTangoRelease = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + patchConfiguration.bonitaVersion);

        PatchDirectory patchDirectoryRelease = new PatchDirectory(STATUS.SERVER, folderTangoRelease, SCOPE.PUBLIC);
        listPatchDirectory.add( patchDirectoryRelease );
        
        File folderUserName = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + patchConfiguration.bonitaVersion + File.separator + patchConfiguration.connectionUserName);
        if (folderUserName.exists()) {
            PatchDirectory patchDirectoryUserRelease = new PatchDirectory(STATUS.SERVER, folderUserName, SCOPE.PRIVATE);
            listPatchDirectory.add( patchDirectoryUserRelease );
        }
        return listPatchDirectory;
        
    }

}
