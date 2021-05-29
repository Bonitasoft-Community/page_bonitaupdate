/*
 * ==================================================
 * Noon/patch : Manage patch
 * ==================================================
 * Creation 2006, by Imagina International
 * This library is owned by Imagina International. You can't
 * redistribute it and/or modify it without the permission of
 * Imagina international
 * -----------------------
 * NoPatch
 * -----------------------
 */

package org.bonitasoft.bonitaupdate.patch;

// -----------------------------------------------------------------------------
import java.io.File;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bonitasoft.bonitaupdate.patch.Patch.LoadPatchResult;
import org.bonitasoft.bonitaupdate.patch.Patch.SCOPE;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

// -------------------------------------------------
/**
 * This method list of patch in a Directory
 */
public class PatchDirectory {

    private static BEvent eventCantAccessFolder = new BEvent(PatchDirectory.class.getName(), 1, Level.APPLICATIONERROR,
            "Patch folder does not exist", "The path given to access the folder does not exist", "The list of patch is not calculated",
            "Check the folder");
    
    
    public final static BEvent eventPatchDoesNotExist = new BEvent(PatchDirectory.class.getName(), 2, Level.APPLICATIONERROR,
            "Patch does not exist", "The patch name given does not match any patch", "patch can't be retrieved",
            "Check the folder / patch name");
    
    private File patchSourcePath;
    private STATUS statusPath;
    /**
     * keep the scope of this directory. May be null if the path contains different scope.
     */
    private SCOPE scope;
    
    private String userName;
    
    
    public static PatchDirectory getInstance( STATUS statusPath, File patchSourcePath, SCOPE scope) {
        PatchDirectory patchDirectory = new PatchDirectory();
        patchDirectory.patchSourcePath = patchSourcePath;
        patchDirectory.statusPath = statusPath;
        patchDirectory.scope = scope;
        return patchDirectory;
    }
    public static PatchDirectory getInstancePrivate( STATUS statusPath, File patchSourcePath, String userName) {
        PatchDirectory patchDirectory = new PatchDirectory();
        patchDirectory.patchSourcePath = patchSourcePath;
        patchDirectory.statusPath = statusPath;
        patchDirectory.scope = SCOPE.PRIVATE;
        patchDirectory.userName = userName;
        return patchDirectory;
    }

    public static class ListPatches {

        public List<Patch> listPatch = new ArrayList<>();
        public List<BEvent> listEvents = new ArrayList<>();
        /**
         * Add only the non already patch
         * @param listPatches
         */
        public void add(ListPatches listPatches ) {
            for (Patch patch : listPatches.listPatch)
            {
                // uniq based on the patch name
                if (! this.isContains(patch.getName()))
                    this.listPatch.add( patch );
            }
            
            this.listEvents.addAll( listPatches.listEvents);
        }
        
        public boolean isContains(String patchName ) 
        {
            for (Patch patch:  listPatch) {
                if (patch.getName().equals(patchName))
                    return true;
            }
            return false;
        }
        /**
         * Remove patch from this list of path
         * @param listPatchToRemove
         */
        public void removeFromList( ListPatches listPatchToRemove) {
            for( Patch patch : listPatchToRemove.listPatch) {
                if (listPatch.contains( patch ))
                    listPatch.remove(patch);
            }
        }
        
        public void removeDuplicates() {
            List<Patch> listWithoutDuplicates = listPatch.stream()
                    .distinct().collect(Collectors.toList());
            listPatch= listWithoutDuplicates;
           }
        
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (Patch patch : listPatch) {
                result.append(patch.toString()+";");                
            }
            return result.toString();
        }
        public void sort() {
            Collections.sort(listPatch, new Comparator<Patch>()
            {
              public int compare(Patch s1,
                      Patch s2)
              {
                return s1.getName().compareTo(s2.getName());
              }
            });
        }
    }

    
    public File getPath() {
        return patchSourcePath;  
        }
    /**
     * 
     * @param patchName
     * @return
     */
    public LoadPatchResult getPatchByName( String patchName ) {
        File filePatch = new File(patchSourcePath.getAbsoluteFile()+File.separator+Patch.getFileNameByPatchName(patchName) );
        if (filePatch.exists()) {
            return  Patch.loadFromFile(statusPath, filePatch);
            }
        LoadPatchResult loadPatchResult = new LoadPatchResult();
        loadPatchResult.listEvents.add( new BEvent( eventPatchDoesNotExist, "Patch["+patchName+"] in folder["+patchSourcePath.getAbsoluteFile()+"]"));
        return loadPatchResult;
    }
    /**
     * return true if the path exist. For a TangoServer, there is maybe no path for this release
     * @return
     */
    public boolean isPathExist() {
        return ( patchSourcePath.exists() && patchSourcePath.isDirectory());
    }
    /**
     * @param onlyInstalledPatch : if null, all patches. If true, only installed patch (a file <patch>_uninstall.zip exist). if false, only non installed patch
     * @return
     */
    public ListPatches getListPatches(Boolean onlyInstalledPatch) {

        ListPatches listPatch = new ListPatches();
        if (! patchSourcePath.exists() || ! patchSourcePath.isDirectory())
        {
            listPatch.listEvents.add( new BEvent(eventCantAccessFolder, "Path["+patchSourcePath+"]"));
            return listPatch;
        }
        try {
            Set<String> allFilesName = new HashSet<>();
            for (final File f : patchSourcePath.listFiles()) {
                if (f.isFile() ) {
                    allFilesName.add( f.getName());
                }
            }
            for (final File f : patchSourcePath.listFiles()) {
                
                if (f.isFile() && Patch.isAPatchName( f.getName())) {
                    // this is a Patch INSTALLED or NOT ?
                    if (onlyInstalledPatch != null)
                    {
                        boolean isInstall= checkIfPatchIsInstalled( f, allFilesName);
                        if (isInstall && Boolean.FALSE.equals(onlyInstalledPatch))
                            continue;
                        if (! isInstall && Boolean.TRUE.equals(onlyInstalledPatch))
                            continue;
                        
                    }
                    LoadPatchResult loadPatchResult = Patch.loadFromFile(statusPath, f);
                    listPatch.listEvents.addAll(loadPatchResult.listEvents);
                    if (! BEventFactory.isError(loadPatchResult.listEvents)) {
                        if (scope!=null)
                            loadPatchResult.patch.scope = scope;
                        if (SCOPE.PRIVATE.equals(scope)) {
                            // set the username
                            loadPatchResult.patch.userName= userName;
                        }
                        listPatch.listPatch.add(loadPatchResult.patch);
                    }
                }
            }
        } catch (Exception e) {
            listPatch.listEvents.add(new BEvent(eventCantAccessFolder, e, "Folder[" + patchSourcePath + "]"));
        }
        return listPatch;

    }

 

  
    
    private boolean checkIfPatchIsInstalled( File f, Set<String>allFilesName) 
    {
        String uninstallFileName = Patch.getUninstallFileName( Patch.getNameFromFileName( f) );
        return allFilesName.contains(uninstallFileName);
    }
}
