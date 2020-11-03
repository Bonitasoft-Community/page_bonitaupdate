package org.bonitasoft.bonitaupdate.patch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.bonitaupdate.page.BonitaLocalServer;
import org.bonitasoft.bonitaupdate.page.BonitaPatchJson;
import org.bonitasoft.bonitaupdate.page.PatchConfiguration;
import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.patch.Patch.LoadPatchResult;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.patch.PatchInstall.ResultInstall;
import org.bonitasoft.bonitaupdate.toolbox.FileTool;
import org.bonitasoft.bonitaupdate.toolbox.ZipTool;
import org.bonitasoft.bonitaupdate.toolbox.ZipTool.ResultZipOperation;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;

public class PatchInstall {


    private static BEvent eventOperationFailed = new BEvent(PatchInstall.class.getName(), 1, Level.APPLICATIONERROR,
            "Operation failed", "The operation failed", "Operation is not correct",
            "Check exception");

    private static final String CST_STATUS_SUCCESS = "SUCCESS";
    private static final String CST_STATUS_FAILED = "FAILED";

    
    public static class ResultInstall {

        public List<BEvent> listEvents = new ArrayList<>();
        public STATUS statusPatch;
        public List<Map<String, Object>> listStatusPatches = new ArrayList<>();

    }

    
    /**
     * do the management to install patch.
     * 1/ if the patch is PUBLIC:
     *      then all PRIVATE *installed* patch will be uninstalled
     *      All PUBLIC patch BEFORE this one will be installed
     *      The patch is installed
     *      then all PRIVATE *installed* patch will be re-installed
     * 2/ if the patch is PRIVATE 
     *      All PRIVATE patch before this one is installed
     * 
     * @param listPatchesName
     * @param bonitaClientPatchServer
     * @param patchConfiguration
     * @return
     */
    public ResultInstall installManagement(  List<String> listPatchesName,  BonitaLocalServer bonitaClientPatchServer,PatchConfiguration patchConfiguration) {
        ResultInstall resultInstall = new ResultInstall();
    
        for (String patchName : listPatchesName) {
            Map<String, Object> statusPatch = new HashMap<>();
            resultInstall.listStatusPatches.add(statusPatch);
            statusPatch.put(BonitaPatchJson.CST_JSON_PATCHNAME, patchName);
            LoadPatchResult loadedPatch = bonitaClientPatchServer.getPatchByName(FOLDER.DOWNLOAD, patchName);
            if (loadedPatch.patch == null) {
                resultInstall.listEvents.addAll(loadedPatch.listEvents);
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSLISTEVENTS, BEventFactory.getSyntheticHtml(loadedPatch.listEvents));
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, CST_STATUS_FAILED);

            } else {
                ResultInstall resultInstallOnePatch = installPatch(patchConfiguration, loadedPatch.patch);
                resultInstall.listEvents.addAll(resultInstallOnePatch.listEvents);
                statusPatch.put(BonitaPatchJson.CST_JSON_PATCHSTATUS, resultInstallOnePatch.statusPatch.toString());

                if (!resultInstall.listEvents.isEmpty())
                    statusPatch.put(BonitaPatchJson.CST_JSON_STATUSLISTEVENTS, BEventFactory.getSyntheticHtml(resultInstall.listEvents));
                statusPatch.put(BonitaPatchJson.CST_JSON_STATUSOPERATION, BEventFactory.isError(resultInstallOnePatch.listEvents) ? CST_STATUS_FAILED : CST_STATUS_SUCCESS);
            }
        }
        return resultInstall;
    }
    
    
    
    public ResultInstall installPatch(PatchConfiguration patchConfiguration, Patch patch) {

        ResultInstall resultInstall = new ResultInstall();
        resultInstall.statusPatch = patch.status;
        try {
            //------------------ first, create the Desintaller
            List<String> listExistingFiles = new ArrayList<>();
            for (String fileName : patch.getListFilesInPatch()) {
                // if the file exist in the current version? If yes, then add it in the Uninstall file
                File fileCurrent = new File(patchConfiguration.getFolderPath(FOLDER.BONITASERVER) + fileName);
                if (fileCurrent.exists() && !fileCurrent.isDirectory())
                    listExistingFiles.add(fileName);
            }
            for (String fileToDelete : patch.getFilesToDelete()) {
                String completeFileName = patchConfiguration.getFolderPath(FOLDER.BONITASERVER) + fileToDelete;
                File file = new File(completeFileName);
                if (file.exists() && file.isFile()) {
                    listExistingFiles.add(fileToDelete);
                }
            }
            ZipTool zipTool = new ZipTool();
            String uninstallFile = patchConfiguration.getFolderPath( FOLDER.UNINSTALL) + Patch.getUninstallFileName(  patch.getName()); 
            ResultZipOperation resultZip = zipTool.createZipFile( uninstallFile,
                    patchConfiguration.getFolderPath(FOLDER.BONITASERVER),
                    listExistingFiles);
            resultInstall.listEvents.addAll(resultZip.listEvents);
            if (BEventFactory.isError(resultInstall.listEvents))
                return resultInstall;
           
            //------------- second, reinstall the deinstaller : then, we can check all the files can be updated correctly 
            resultZip = zipTool.unzipFile(new File(uninstallFile),
                    patchConfiguration.getFolderPath(FOLDER.BONITASERVER),
                    Arrays.asList(patch.getFileNameDescription()));
            if (BEventFactory.isError(resultZip.listEvents)) {
                resultInstall.listEvents.addAll( resultZip.listEvents);
                return resultInstall;
            }
            
            // -------------- Third : delete files
            for (String fileToDelete : patch.getFilesToDelete()) {
                String completeFileName = patchConfiguration.getFolderPath(FOLDER.BONITASERVER) + fileToDelete;
                File file = new File(completeFileName);
                if (file.exists() && file.isFile()) {
                    resultInstall.listEvents.addAll(FileTool.removeFile(file.getAbsolutePath()));
                }
            }

            // --------------- third install : unzip
            resultZip = zipTool.unzipFile(patch.patchFile,
                    patchConfiguration.getFolderPath(FOLDER.BONITASERVER),
                    Arrays.asList(patch.getFileNameDescription()));
            resultInstall.listEvents.addAll(resultZip.listEvents);

            //-------------- move the patch file to the installed directory
            if (!BEventFactory.isError(resultInstall.listEvents)) {
                if (! patchConfiguration.getFolderPath(FOLDER.INSTALL).equals( patchConfiguration.getFolderPath(FOLDER.DOWNLOAD))) {
                    resultInstall.listEvents.addAll(FileTool.moveFile(patch.getPatchFile().getAbsolutePath(), patchConfiguration.getFolderPath(FOLDER.INSTALL) + patch.getFileName()));
                }
                resultInstall.statusPatch = STATUS.INSTALLED;
            }

        } catch (Exception e) {
            resultInstall.listEvents.add(new BEvent(eventOperationFailed, e, ""));
        }
        return resultInstall;
    }

    /**
     * @param patchConfiguration
     * @param patch
     * @return
     */
    public ResultInstall unInstallPatch(PatchConfiguration patchConfiguration, Patch patch) {

        ResultInstall resultInstall = new ResultInstall();
        resultInstall.statusPatch = patch.status;
        try {
            //-------------- first, access the patch and delete all theses files
            for (String fileName : patch.getListFilesInPatch()) {
                // skip the patch description file
                if (fileName.equals( patch.getFileNameDescription()))
                        continue;
                // if the file exist in the current version? If yes, then add it in the Uninstall file
                FileTool.removeFile(patchConfiguration.getFolderPath(FOLDER.BONITASERVER)+fileName);
            }
 
            //------------------ second, unzip the uninstall
            if (BEventFactory.isError(resultInstall.listEvents))
                return resultInstall;

            ZipTool zipTool = new ZipTool();
            // Now install : unzip
            String fileUnzip = patchConfiguration.getFolderPath(FOLDER.UNINSTALL) + Patch.getUninstallFileName(patch.getName());
            ResultZipOperation resultZip = zipTool.unzipFile(new File(fileUnzip),
                    patchConfiguration.getFolderPath(FOLDER.BONITASERVER),
                    Arrays.asList(patch.getName()));
            resultInstall.listEvents.addAll(resultZip.listEvents);

            //-----------------  move the patch file to the Downloaded directory and remove the uninstall file
            if (BEventFactory.isError(resultInstall.listEvents))
                return resultInstall;
            
            // remove the unzip file
            resultInstall.listEvents.addAll(FileTool.removeFile(fileUnzip));

            if (! patchConfiguration.getFolderPath(FOLDER.DOWNLOAD).equals(patchConfiguration.getFolderPath(FOLDER.INSTALL)))
                resultInstall.listEvents.addAll(FileTool.moveFile(patch.getPatchFile().getAbsolutePath(), patchConfiguration.getFolderPath(FOLDER.DOWNLOAD)+patch.getFileName()));
            
            if (BEventFactory.isError(resultInstall.listEvents))
                return resultInstall;
            resultInstall.statusPatch = STATUS.DOWNLOADED;
        } catch (Exception e) {
            resultInstall.listEvents.add(new BEvent(eventOperationFailed, e, ""));
        }
        return resultInstall;
    }
}
