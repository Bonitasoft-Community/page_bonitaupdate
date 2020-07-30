package org.bonitasoft.bonitaupdate.page;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.patch.Patch;
import org.bonitasoft.bonitaupdate.patch.Patch.LoadPatchResult;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;
import org.bonitasoft.log.event.BEvent;

/**
 * This class contact the Bonita Server
 * 
 * @author Firstname Lastname
 */
public class BonitaLocalServer {

    PatchConfiguration patchConfiguration;

    public BonitaLocalServer(PatchConfiguration patchConfiguration) {
        this.patchConfiguration = patchConfiguration;
    }

    public ListPatches getInstalledPatch() {
        PatchDirectory patchDirectory = new PatchDirectory(STATUS.INSTALLED, patchConfiguration.getFolder( FOLDER.INSTALL) );
        return patchDirectory.getListPatches();
    }

    public ListPatches getDownloadedPatch() {
        PatchDirectory patchDirectory = new PatchDirectory(STATUS.DOWNLOADED, patchConfiguration.getFolder( FOLDER.DOWNLOAD) );
        return patchDirectory.getListPatches(  );
    }
    
    /**
     * Where to search the patch
     * @param folder
     * @param patchName
     * @return
     */
    public LoadPatchResult getPatchByName(FOLDER folder, String patchName ) {
            
        PatchDirectory patchDirectory = new PatchDirectory( Patch.getStatusFromFolder(folder), patchConfiguration.getFolder(folder) );
        return patchDirectory.getPatchByName( patchName );
        
    }
   
}
