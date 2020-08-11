package org.bonitasoft.bonitaupdate.page;

import java.io.File;
import java.util.StringTokenizer;

import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.patch.PatchDirectory.ListPatches;

/**
 * Tango Server
 * This deliver all patch on the server. This is first all the patch themself (because a TANGO server may be a Bonita Server, example
 * PRODUCTION => VALIDATION
 * or the Bonita TANGO server
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

        PatchDirectory patchDirectoryRelease = new PatchDirectory(STATUS.SERVER, folderTangoRelease);
        listPatches.add(patchDirectoryRelease.getListPatches());
        File folderUserName = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + patchConfiguration.bonitaVersion + File.separator + patchConfiguration.connectionUserName);
        if (folderUserName.exists()) {
            PatchDirectory patchDirectoryUserRelease = new PatchDirectory(STATUS.SERVER, folderUserName);
            listPatches.add(patchDirectoryUserRelease.getListPatches());
        }
        
        
        // then extract the version (example, 7.8)
        if (patchConfiguration.bonitaVersion != null) {
            String bonitaMainVersion = "";
            StringTokenizer st = new StringTokenizer(patchConfiguration.bonitaVersion, ".");
            bonitaMainVersion += st.hasMoreElements() ? st.nextElement() + "." : "";
            bonitaMainVersion += st.hasMoreElements() ? st.nextElement() : "";
            File folderTangoVersion = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + bonitaMainVersion);

            PatchDirectory patchDirectoryVersion = new PatchDirectory(STATUS.SERVER, folderTangoVersion);
            listPatches.add(patchDirectoryVersion.getListPatches());
            File folderUserNameVersion = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + bonitaMainVersion + File.separator + patchConfiguration.connectionUserName);
            if (folderUserNameVersion.exists()) {
                PatchDirectory patchDirectoryUserVersion = new PatchDirectory(STATUS.SERVER, folderUserNameVersion);
                listPatches.add(patchDirectoryUserVersion.getListPatches());
            }
        }
        

        return listPatches;
    }

}
