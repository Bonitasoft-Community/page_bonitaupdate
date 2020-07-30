package org.bonitasoft.bonitaupdate.toolbox;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bonitasoft.bonitaupdate.patch.Patch;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

import com.google.common.io.Files;

public class ZipTool {
    
    private static BEvent eventFileNotExist = new BEvent(ZipTool.class.getName(), 1, Level.APPLICATIONERROR,
            "File not exist", "The given file does not exist, and can't be saved in the ZIP", "Zip is not complete",
            "Check the file");
    private static BEvent eventFileError = new BEvent(ZipTool.class.getName(), 2, Level.APPLICATIONERROR,
            "File Error", "File error during the read", "Zip is not complete",
            "Check the file");


    public class ResultZipOperation {

        public List<BEvent> listEvents = new ArrayList<>();
        public int nbFilesTreated = 0;
        public List<String> listFilesTreated = new ArrayList<>();
    }

    public ResultZipOperation createZipFile(String completeZipFileName, String rootPathForListOfFileToSave, List<String> listFileName) {
        ResultZipOperation resultZipOperation = new ResultZipOperation();
        String completeFileName="" ;
        try (FileOutputStream theZipOutput = new FileOutputStream(completeZipFileName+".zip")) {

            ZipOutputStream outZipStream = new ZipOutputStream(theZipOutput);
            for (String fileName : listFileName) {

                completeFileName = rootPathForListOfFileToSave + fileName;
                File fileCompleteName = new File(completeFileName);
                if (!fileCompleteName.exists() || fileCompleteName.isDirectory())
                    continue;
                try {
                    byte[] buffer = new byte[10000];

                    InputStream in = null;
                    // if the listOfInputStream has a inputStream for this number, use it, else read the file
                    ZipEntry ze = new ZipEntry(fileName);
                    outZipStream.putNextEntry(ze);

                    in = new FileInputStream(completeFileName);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        outZipStream.write(buffer, 0, len);
                    }
                    resultZipOperation.nbFilesTreated++;
                    resultZipOperation.listFilesTreated.add(fileName);
                    in.close();
                    outZipStream.closeEntry();
                } catch (Exception f) {
                    resultZipOperation.listEvents.add( new BEvent( eventFileError, f, "File ["+completeFileName+"]"));
                }

            } // end loop for each file
            outZipStream.flush();
            outZipStream.close();
        } catch (FileNotFoundException e) {
            resultZipOperation.listEvents.add( new BEvent( eventFileNotExist, e, "File ["+completeFileName+"]"));

        } catch (IOException e) {
            resultZipOperation.listEvents.add( new BEvent( eventFileError, e, "File ["+completeZipFileName+"]"));

        }
        return resultZipOperation;

    }

    /**
     * 
     * @param zipFile : a .zip is added automaticaly
     * @param destinationFile
     * @param ignoreFiles
     * @return
     */
    public ResultZipOperation unzipFile(File zipFile, String destinationFile, List<String> ignoreFiles) {
        ResultZipOperation resultZipOperation = new ResultZipOperation();

        String fileName = "";
        try (FileInputStream patchFile = new FileInputStream(zipFile)){
            // get the content of patchfile
            

            ZipInputStream inStream = new ZipInputStream(patchFile);
            ZipEntry zipEntry = null;
            while ((zipEntry = inStream.getNextEntry()) != null) {
                if (ignoreFiles.contains(zipEntry.getName())) {
                    continue;
                }

                // long size = zipEntry.getSize();

                // check then create the directory, which may new
                fileName = zipEntry.getName();
                File file = new File(destinationFile + File.separator + fileName);
                if (zipEntry.isDirectory()) {
                    FileTool.checkAndCreateDir(file.getParentFile().getPath());
                    continue;
                }
                FileTool.checkAndCreateDir(file.getParentFile().getPath());

                FileOutputStream fileExtracted = new FileOutputStream(destinationFile + File.separator + fileName);

                try (BufferedOutputStream foutStream = new BufferedOutputStream(fileExtracted)) {
                
                byte[] buffer = new byte[10000];
                int len;
                while ((len = inStream.read(buffer)) > 0) {
                    foutStream.write(buffer, 0, len);
                }
                resultZipOperation.listFilesTreated.add(fileName);
                }catch(Exception e) {
                    resultZipOperation.listEvents.add( new BEvent( eventFileError, e , "FileName["+fileName+"]"));
                    
                }
                fileExtracted.close();
            }
            inStream.close();
            
            return resultZipOperation;
        } catch (Exception e) {
            resultZipOperation.listEvents.add( new BEvent( eventFileError, e , ""));
            // Event
        }
        return resultZipOperation;
    }

}
