package org.bonitasoft.bonitaupdate.patch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
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
    private static BEvent eventErrorPatchInconsistent = new BEvent(Patch.class.getName(), 3, Level.APPLICATIONERROR,
            "Patch inconsistent", "The patch must respect a structure", "patch is ignored.",
            "Check the patch");
    
    
    private static final String CST_FILENAME_PATCHDESCRIPTION= "patch_description.json";
    
    public enum STATUS { INSTALLED, DOWNLOADED, SERVER };
    public enum INSTALLATIONPOLICY { STRICT, INDEPENDANT };
    
    /**
     * THis is the file name. Should finish by .zip - example Patch_7.8.4_100.zip
     */
    String fileName;
    /**
     * Patch contains a ZIP file with information. Must have the same name as the patch, example Patch_7.8.4_100.zip
     */
    String fileNameJson;
    
    /**
     * patch name must be <Patch_<BonitaVersion>_<Sequence>, so this is the BonitaVersion. Example 7.8.4 or 7.8 (patch release or patch version)
     */
    String bonitaVersion;
    
    /**
     * name of the patch, reading in the JSON. Name is normalized
     */
    String patchName;
    /**
     * Sequence of the patch, reading in the JSON.
     */
    int sequence;
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
        result.put( BonitaPatchJson.CST_JSON_SEQUENCE, sequence);
        result.put( BonitaPatchJson.CST_JSON_BONITAVERSION, bonitaVersion);

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
        sequence      = TypesCast.getInteger(map.get( BonitaPatchJson.CST_JSON_SEQUENCE),-1);
        bonitaVersion= TypesCast.getString( map.get( BonitaPatchJson.CST_JSON_BONITAVERSION),null);
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
    
    public String toString() {
        return patchName+" ["+(status==null ? null : status.toString())+"]";
    }
    
    
    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Check the consistency of the patch*/
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public String checkConsistency() {
        String consistency="";
        
        if (fileName != null && ! (fileName.equalsIgnoreCase(patchName+".zip")))
        {
            consistency+= "Incorrect filename FileName["+fileName+"] PatchName["+patchName+"];";
        }
        if (fileNameJson!=null && ! fileNameJson.equalsIgnoreCase( CST_FILENAME_PATCHDESCRIPTION )&& ! fileNameJson.equalsIgnoreCase( patchName+".json"))
        {
            consistency+= "Incorrect JSON filename FileNameJSON["+fileNameJson+"] PatchName["+patchName+"];";
        }
        if (patchName!=null)
        {
            // decompose : sequence may be 003 for 3
            StringTokenizer st = new StringTokenizer(patchName, "_");
            String head = st.hasMoreElements()? st.nextToken(): "";
            String version = st.hasMoreElements()? st.nextToken(): "";
            int seq = TypesCast.getInteger(st.hasMoreElements()? st.nextToken(): "-1", -2);
            if (!head.equalsIgnoreCase("Patch"))
                consistency+= "Patch name must start by [Patch_];";
            if (! version.equalsIgnoreCase(bonitaVersion))
                consistency+= "Version are different Version["+bonitaVersion+"] Version in PatchName["+version+"]";
            if (seq != sequence)
                consistency+= "Sequence are different["+sequence+"] Sequence in PatchName["+seq+"]";
           
        }
     
        if (patchName == null)
            consistency+="No name;";
        if (installationPolicy == null)
            consistency+="No Installation policy;";
        if (description==null)
            consistency+="No Description;";
        if (dateRelease==null)
            consistency+="No Date release;";
        if (sequence<=0)
            consistency+="No Sequence;";

        if (consistency.length()==0)
            return null;
        return consistency;
    }
    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Collect a Patch from a file */
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
        loadPatchResult.patch.fileName= filePatch.getName();
        String jsonFileExpected="";
        if (loadPatchResult.patch.fileName != null && loadPatchResult.patch.fileName.length()>4)
             jsonFileExpected = loadPatchResult.patch.fileName.substring(0, loadPatchResult.patch.fileName.length()-4)+".json";
        
        try (FileInputStream patchFile = new FileInputStream(filePatch)){
            // retrieve in the content the XML file with the same name

            ZipInputStream inStream = new ZipInputStream(patchFile);
            ZipEntry zipEntry = null;
            while ((zipEntry = inStream.getNextEntry()) != null) {
                if (zipEntry.getName().endsWith( jsonFileExpected ) || zipEntry.getName().equalsIgnoreCase(CST_FILENAME_PATCHDESCRIPTION)) {
                    loadPatchResult.patch.fileNameJson = zipEntry.getName();
                    // this is the JSON file
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

            // check the consistence now:
            //
            String consistentInformation = loadPatchResult.patch.checkConsistency();
            if (consistentInformation !=null){
                loadPatchResult.listEvents.add(new BEvent(eventErrorPatchInconsistent, consistentInformation));

            }
            return loadPatchResult;
        } catch (Exception e) {
            loadPatchResult.listEvents.add(new BEvent(eventErrorLoadingPatch, e, ""));

        }
        return loadPatchResult;

    }

}
