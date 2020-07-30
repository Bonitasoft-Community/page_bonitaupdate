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
import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.bonitaupdate.patch.Patch.LoadPatchResult;
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
    
    
    private static BEvent eventPatchDoesNotExist = new BEvent(PatchDirectory.class.getName(), 2, Level.APPLICATIONERROR,
            "Patch does not exist", "The patch name given does not match any patch", "patch can't be retrieved",
            "Check the folder / patch name");
    
    File patchSourcePath;
    STATUS statusPath;
    
    public PatchDirectory( STATUS statusPath, File patchSourcePath) {
        this.patchSourcePath = patchSourcePath;
        this.statusPath = statusPath;
    }

    public static class ListPatches {

        public List<Patch> listPatch = new ArrayList<>();
        public List<BEvent> listEvents = new ArrayList<>();
        public void add(ListPatches listPatches ) {
            this.listPatch.addAll( listPatches.listPatch);
            this.listEvents.addAll( listPatches.listEvents);
        }
    }

    public LoadPatchResult getPatchByName( String patchName ) {
        File filePatch = new File(patchSourcePath.getAbsoluteFile()+File.separator+patchName+".zip" );
        if (filePatch.exists())
            {
            return  Patch.loadFromFile(statusPath, filePatch);
            }
        LoadPatchResult loadPatchResult = new LoadPatchResult();
        loadPatchResult.listEvents.add( new BEvent( eventPatchDoesNotExist, "Patch["+patchName+"] in folder["+patchSourcePath.getAbsoluteFile()+"]"));
        return loadPatchResult;
    }
    /**
     * @param patchSourcePath
     * @return
     */
    public ListPatches getListPatches() {

        ListPatches listPatch = new ListPatches();

        try {

            for (final File f : patchSourcePath.listFiles()) {

                if (f.isFile() && f.getName().endsWith(".zip")) {
                    // this is a Patch

                    LoadPatchResult loadPatchResult = Patch.loadFromFile(statusPath, f);
                    listPatch.listEvents.addAll(loadPatchResult.listEvents);
                    if (! BEventFactory.isError(loadPatchResult.listEvents))
                        listPatch.listPatch.add(loadPatchResult.patch);
                }
            }
        } catch (Exception e) {
            listPatch.listEvents.add(new BEvent(eventCantAccessFolder, e, "Folder[" + patchSourcePath + "]"));
        }
        return listPatch;

    }

}
