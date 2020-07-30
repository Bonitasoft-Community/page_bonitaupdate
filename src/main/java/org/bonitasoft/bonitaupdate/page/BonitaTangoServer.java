package org.bonitasoft.bonitaupdate.page;

import java.io.File;

import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;

/**
 * Tango Server
 * This deliver all patch on the server. This is first all the patch themself (because a TANGO server may be a Bonita Server, example 
 *    PRODUCTION => VALIDATION 
 *  or the Bonita TANGO server
 *
 */
public class BonitaTangoServer extends BonitaLocalServer{

    public BonitaTangoServer(PatchConfiguration patchConfiguration) {
        super(patchConfiguration);
    }

    public ListPatches getAvailablesPatch() {
        ListPatches listPatches = getInstalledPatch();
        listPatches.add(getDownloadedPatch());
        
        // now, get the local path
        PatchDirectory patchDirectory = new PatchDirectory(STATUS.SERVER, patchConfiguration.getFolder( FOLDER.TANGOSERVER) );
        listPatches.add(patchDirectory.getListPatches());

        File folderUserName = new File( patchConfiguration.getFolder( FOLDER.TANGOSERVER)+patchConfiguration.userName);
        if (folderUserName.exists()) {
            patchDirectory = new PatchDirectory(STATUS.SERVER, folderUserName );
            listPatches.add(patchDirectory.getListPatches());
        }
        
        return listPatches;
    }


}
