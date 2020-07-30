package org.bonitasoft.bonitaupdate.patch;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bonitasoft.bonitaupdate.page.PatchConfiguration;
import org.bonitasoft.bonitaupdate.page.PatchConfiguration.FOLDER;
import org.bonitasoft.bonitaupdate.patch.Patch.STATUS;
import org.bonitasoft.bonitaupdate.toolbox.FileTool;
import org.bonitasoft.bonitaupdate.toolbox.ZipTool;
import org.bonitasoft.bonitaupdate.toolbox.ZipTool.ResultZipOperation;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;

public class PatchInstall {

    private static final String CST_PREFIXFILE_UNINSTALL = "uninstall_";
    private static BEvent eventOperationFailed = new BEvent(PatchInstall.class.getName(), 1, Level.APPLICATIONERROR,
            "Operation failed", "The operation failed", "Operation is not correct",
            "Check exception");

    public static class ResultInstall {

        public List<BEvent> listEvents = new ArrayList<>();
        public STATUS statusPatch;

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
            ResultZipOperation resultZip = zipTool.createZipFile(patchConfiguration.getFolderPath(FOLDER.UNINSTALL) + CST_PREFIXFILE_UNINSTALL + patch.getName(),
                    patchConfiguration.getFolderPath(FOLDER.BONITASERVER),
                    listExistingFiles);
            resultInstall.listEvents.addAll(resultZip.listEvents);
            if (BEventFactory.isError(resultInstall.listEvents))
                return resultInstall;

            // -------------- Second : delete files
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
                resultInstall.listEvents.addAll(FileTool.moveFile(patch.getPatchFile().getAbsolutePath(), patchConfiguration.getFolderPath(FOLDER.INSTALL) + patch.getFileName()));
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
            String fileUnzip = patchConfiguration.getFolderPath(FOLDER.UNINSTALL) + CST_PREFIXFILE_UNINSTALL + patch.patchName + ".zip";
            ResultZipOperation resultZip = zipTool.unzipFile(new File(fileUnzip),
                    patchConfiguration.getFolderPath(FOLDER.BONITASERVER),
                    Arrays.asList(patch.getName()));
            resultInstall.listEvents.addAll(resultZip.listEvents);

            //-----------------  move the patch file to the Downloaded directory and remove the uninstall file
            if (BEventFactory.isError(resultInstall.listEvents))
                return resultInstall;
            resultInstall.listEvents.addAll(FileTool.removeFile(fileUnzip));

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
