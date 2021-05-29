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
import org.bonitasoft.log.event.BEvent.Level;

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


    
    private static BEvent eventNoPatchForThisVersion = new BEvent(BonitaTangoServer.class.getName(), 1, Level.INFO,
            "No patches for this release", "For the expected release, no patches are available (this version is not referenced in the Tango server)");

    
    public BonitaTangoServer(PatchConfiguration patchConfiguration) {
        super(patchConfiguration);
    }

    /**
     * 
     * @return
     */
    public ListPatches getAvailablesPatch() {
        ListPatches listPatches = new ListPatches();
        listPatches.add(getInstalledPatch());
        listPatches.add(getDownloadedPatch());

        // now, get the local path with the complete version (example, 7.8.4)
        File folderTangoRelease = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + patchConfiguration.bonitaVersion);

        listPatches.add(getPatchesInDirectory(folderTangoRelease, false));

        
        listPatches.removeDuplicates();
        return listPatches;
    }
    
    public String getPath() {
        return patchConfiguration.getFolderPath(FOLDER.TANGOSERVER);
    }
    /**
     * 
     * @return
     */
    public ListPatches getAllAvailablesPatch() {
        ListPatches listPatches = new ListPatches();

        // now, get the local path with the complete version (example, 7.8.4)
        File folderRootTango = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) );

        for (final File f : folderRootTango.listFiles()) {

            if (f.isDirectory()) {
                ListPatches localPatches = getPatchesInDirectory( f, true );
                listPatches.add( localPatches);
            }
        }
        listPatches.removeDuplicates();
        return listPatches;
    }
    
    /**
     * 
     * @param folderTangoRelease
     * @param allFolderUsers
     * @return
     */
  private ListPatches getPatchesInDirectory(File folderTangoRelease, boolean allFolderUsers ) {
          ListPatches listPatches = new ListPatches();

       
          PatchDirectory patchDirectoryRelease = PatchDirectory.getInstance(STATUS.SERVER, folderTangoRelease, SCOPE.PUBLIC);
          if (! patchDirectoryRelease.isPathExist()) {
              listPatches.listEvents.add( new BEvent( eventNoPatchForThisVersion, "Version ["+patchConfiguration.bonitaVersion+"]"));
              return listPatches;
          }
          listPatches.add(patchDirectoryRelease.getListPatches( null ));
          if (allFolderUsers) {
              for (final File subFolderUserName : folderTangoRelease.listFiles()) {
                  if (subFolderUserName.isDirectory()) {
                      PatchDirectory patchDirectoryUserRelease = PatchDirectory.getInstancePrivate(STATUS.SERVER, subFolderUserName, subFolderUserName.getName());
                      listPatches.add( patchDirectoryUserRelease.getListPatches( null ) );
                  }
              }
          }
          else {
              File folderUserName = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + patchConfiguration.bonitaVersion + File.separator + patchConfiguration.connectionUserName);
              if (folderUserName.exists()) {
                  PatchDirectory patchDirectoryUserRelease = PatchDirectory.getInstancePrivate(STATUS.SERVER, folderUserName, folderUserName.getName());
                  listPatches.add( patchDirectoryUserRelease.getListPatches( null ) );
              }
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
        
        PatchDirectory patchDirectoryRelease = PatchDirectory.getInstance(STATUS.SERVER, folderTangoRelease, SCOPE.PUBLIC);
        listPatchDirectory.add( patchDirectoryRelease );
        
        File folderUserName = new File(patchConfiguration.getFolderPath(FOLDER.TANGOSERVER) + patchConfiguration.bonitaVersion + File.separator + patchConfiguration.connectionUserName);
        if (folderUserName.exists()) {
            PatchDirectory patchDirectoryUserRelease = PatchDirectory.getInstancePrivate(STATUS.SERVER, folderUserName, folderUserName.getName());
            listPatchDirectory.add( patchDirectoryUserRelease );
        }
        return listPatchDirectory;
        
    }

}
