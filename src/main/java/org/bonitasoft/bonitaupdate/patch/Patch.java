package org.bonitasoft.bonitaupdate.patch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bonitasoft.bonitaupdate.page.BonitaPatchJson;
import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.toolbox.TypesCast;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.json.simple.JSONValue;


public class Patch {

 
    static Logger logger = Logger.getLogger(Patch.class.getName());

    private static BEvent eventIncorrectDescriptionFile = new BEvent(Patch.class.getName(), 1, Level.APPLICATIONERROR,
            "Incorrect Description file", "Patch Zip must contains a description file", "The description file is not correct, it's an empty file.",
            "Check the patch");

    private static BEvent eventErrorLoadingPatch = new BEvent(Patch.class.getName(), 2, Level.APPLICATIONERROR,
            "incorrect Patch Structure", "An error occures when the patch is parsing", "The description file is not correct.",
            "Check the patch");

    public enum STATUS { INSTALLED, DOWNLOADED, SERVER };
    public enum INSTALLATIONPOLICY { STRICT, INDEPENDANT };
    
    String patchName;
    String description;
    String dateRelease;
    STATUS status;
    INSTALLATIONPOLICY installationPolicy ;
    
    File patchFile;
    List<String> listFilesToDelete= new ArrayList<>();
    
    List<String> listFilesinPatch = new ArrayList<>();
    /**
     * load the patch form the file
     * Update return a list of Event. Not
     * 
     * @param f
     * @return
     */
    public static class LoadPatchResult {

        public List<BEvent> listEvents = new ArrayList<>();
        public Patch patch;
    }

    public Map<String,Object> getMap() {
        Map<String,Object> result = new HashMap<>();
        result.put( BonitaPatchJson.CST_JSON_PATCHNAME, patchName);
        result.put( BonitaPatchJson.CST_JSON_PATCHDESCRIPTION, description);
        result.put( BonitaPatchJson.CST_JSON_PATCHDATEREALEASE, dateRelease);
        result.put( BonitaPatchJson.CST_JSON_PATCHSTATUS, status.toString());
        result.put( BonitaPatchJson.CST_JSON_PATCHINSTALLATIONPOLICY, installationPolicy.toString());
        result.put( BonitaPatchJson.CST_JSON_PATCHFILESCONTENT, listFilesinPatch);
        result.put( BonitaPatchJson.CST_JSON_PATCHFILESTODELETE, listFilesToDelete);
        return result;
    }
    
    public static Patch getFromMap(Map<String,Object> map) {
        Patch patch = new Patch();
        patch.completeFromMap(map);
        return patch;
    }
    
    public void completeFromMap(Map<String,Object> map) {
        patchName     = TypesCast.getString( map.get( BonitaPatchJson.CST_JSON_PATCHNAME), null);
        description   = TypesCast.getString( map.get( BonitaPatchJson.CST_JSON_PATCHDESCRIPTION) ,null);
        dateRelease   = TypesCast.getString(map.get( BonitaPatchJson.CST_JSON_PATCHDATEREALEASE),null);
        installationPolicy = INSTALLATIONPOLICY.STRICT;
        if (map.get( BonitaPatchJson.CST_JSON_PATCHINSTALLATIONPOLICY) !=null)
            installationPolicy   = INSTALLATIONPOLICY.valueOf( TypesCast.getString(map.get( BonitaPatchJson.CST_JSON_PATCHINSTALLATIONPOLICY),INSTALLATIONPOLICY.STRICT.toString()));
        if (map.get( BonitaPatchJson.CST_JSON_PATCHSTATUS)!=null)
            status        = STATUS.valueOf( (String) map.get( BonitaPatchJson.CST_JSON_PATCHSTATUS));
        
        listFilesToDelete = (List<String>) TypesCast.getList( map.get(BonitaPatchJson.CST_JSON_PATCHFILESTODELETE), new ArrayList<>());
    }
    public File getPatchFile() {
        return patchFile;
    }
    /**
     * get the filename, like "Patch_001.zip"
     * @return
     */
    public String getFileName() {
        if (patchFile!=null)
            return patchFile.getName();
        return null;
    }
    
    public List<String> getListFilesInPatch() {
        return listFilesinPatch; 
    }

    public String getName() {
        return patchName;
    }
    public String getFileNameDescription() {
        return patchName+".json";
    }
    
    public List<String> getFilesToDelete() {
        return listFilesToDelete;
    }
    
    public void setStatus( STATUS status ) {
        this.status = status;
    }
    
    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Collect a Path from a file */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public static STATUS getStatusFromFolder( FOLDER folder ) 
    {
        if (FOLDER.INSTALL.equals( folder))
            return STATUS.INSTALLED;
        if (FOLDER.DOWNLOAD.equals( folder))
            return STATUS.DOWNLOADED;
        if (FOLDER.TANGOSERVER.equals( folder))
            return STATUS.SERVER;
        // not in this path? Then we need to download it againt
        return STATUS.SERVER;
    }

    public static LoadPatchResult loadFromFile(STATUS status, File filePatch) {

        LoadPatchResult loadPatchResult = new LoadPatchResult();
        loadPatchResult.patch = new Patch();
        loadPatchResult.patch.patchFile = filePatch;
        loadPatchResult.patch.status = status;
        loadPatchResult.patch.patchName = filePatch.getName();
        if (loadPatchResult.patch.patchName.endsWith(".zip"))
            loadPatchResult.patch.patchName = loadPatchResult.patch.patchName.substring(0, loadPatchResult.patch.patchName.length() - 4);

         
        
        try (FileInputStream patchFile = new FileInputStream(filePatch)){
            // retrieve in the content the XML file with the same name
            ;

            ZipInputStream inStream = new ZipInputStream(patchFile);
            ZipEntry zipEntry = null;
            while ((zipEntry = inStream.getNextEntry()) != null) {
                if (zipEntry.getName().endsWith(loadPatchResult.patch.getFileNameDescription())) {
                    // this is the XML file
                    long size = zipEntry.getSize();
                    if (size == -1) {
                        // incorrect xml file

                        loadPatchResult.listEvents.add(eventIncorrectDescriptionFile);
                        return loadPatchResult;
                    }
                    ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[5000];

                    int sizeReaded = 0;
                    while (size > sizeReaded) {
                        int bufToRead = (int) (size - sizeReaded);
                        if (bufToRead > buffer.length)
                            bufToRead = buffer.length;
                        inStream.read(buffer, 0, bufToRead);
                        bufferStream.write(buffer, 0, bufToRead);
                        sizeReaded += bufToRead;
                    }

                    // JSON structure
                    Map param = (Map<String, Object>) JSONValue.parse(bufferStream.toString());
                    loadPatchResult.patch.completeFromMap( param );

                    loadPatchResult.patch.listFilesinPatch.add( zipEntry.getName() );

                }
                else if (! zipEntry.isDirectory())
                    loadPatchResult.patch.listFilesinPatch.add( zipEntry.getName() );
            }
            inStream.close();

            return loadPatchResult;
        } catch (Exception e) {
            loadPatchResult.listEvents.add(new BEvent(eventErrorLoadingPatch, e, ""));

        }
        return loadPatchResult;

    }

}
