package org.bonitasoft.bonitaupdate.toolbox;

// -----------------------------------------------------------------------------
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

public class FileTool {

    private final static BEvent eventNotADirectory = new BEvent(FileTool.class.getName(), 1,
            Level.APPLICATIONERROR,
            "Not a directory", "This name already exists, and this is not a directory", "Path can't be created",
            "Remove the file name, to create a subdirectory");

    private final static BEvent eventException = new BEvent(FileTool.class.getName(), 2,
            Level.APPLICATIONERROR,
            "Exception", "An exception occure during the execution", "Operation failed",
            "Check the exception");

    private final static BEvent eventBadValue = new BEvent(FileTool.class.getName(), 3,
            Level.APPLICATIONERROR,
            "Bad value", "Value are not correct", "Operation failed",
            "Check the value");

    private final static BEvent eventRenameImpossible = new BEvent(FileTool.class.getName(), 4,
            Level.APPLICATIONERROR,
            "Rename impossible", "Rename a file is not possible", "Operation failed",
            "Check source and destination file name");
    
    private final static BEvent eventDeletionFailed = new BEvent(FileTool.class.getName(), 5,
            Level.APPLICATIONERROR,
            "Deletion failed", "Delete a file is not possible", "Operation failed",
            "Check file name");
    
    private final static BEvent eventFileAlreadyExist = new BEvent(FileTool.class.getName(), 6,
            Level.APPLICATIONERROR,
            "File already exist", "Copy is impossible, destination file already exist", "Operation failed",
            "Delete the destination file first");
    
    private final static BEvent eventDirectoryChecked = new BEvent(FileTool.class.getName(), 7,
            Level.SUCCESS,
            "Directory checked", "The existence of a directory is checked with sucess");
    
    
    public FileTool() {
    }

    // ---------------------------------------------------------------------------
    /**
     * load the file, and return the content of in a String. When an error occure,
     * the result is null.
     */
    public static String loadFile(String fileName) {
        StringBuffer result = new StringBuffer();
        try (FileReader fileReader = new FileReader(fileName);){
            int nbRead;
            char[] buffer = new char[50000];
            while ((nbRead = fileReader.read(buffer, 0, 50000)) > 0)
                result.append(new String(buffer).substring(0, nbRead));
            return result.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * check that the path exist
     * 
     * @param completePathToCheck
     * @param caller
     * @param theLog
     * @return
     */
    public static boolean checkDir(String completePathToCheck) {
        // Check that the path exist

        if (completePathToCheck == null)
            return false;
        try {
            File path = new File(completePathToCheck);
            return path.isDirectory();
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------------------------------------------------
    /**
     * create the complete path Return true in case of success, false else.
     */
    public static List<BEvent> checkAndCreateDir(String completePathToCheck) {
        List<BEvent> listEvents = new ArrayList<>();
        completePathToCheck = completePathToCheck.replace('/', File.separatorChar);
        completePathToCheck = completePathToCheck.replace('\\', File.separatorChar);

        // Check that the path exist
        if (completePathToCheck == null) {
            listEvents.add(new BEvent(eventBadValue, "Check And Create Directory: No path to check"));

            return listEvents;
        }
        // do nothing if the directory exists
        if (isDirectoryExist(completePathToCheck)) {
            return listEvents;
        }
        
        StringBuffer logOperation= new StringBuffer();
        logOperation.append("Check directory ["+completePathToCheck+"]");
        try {
            // create all subdir
            File path = new File(completePathToCheck);
            path.mkdirs();

            StringTokenizer st= new StringTokenizer(completePathToCheck, String.valueOf(File.separatorChar));

       
            // then, check now they exist
            StringBuilder relativePath = new StringBuilder();
            // in Unix, the first character is the separator char.
            if (completePathToCheck.startsWith( String.valueOf(File.separatorChar )))
                relativePath.append( File.separatorChar );

            while (st.hasMoreTokens()) {
                relativePath.append( st.nextToken() );

                // theLog.log( this,LoggerLevel.Debug,"Check relative path
                // ["+relativePath+"]");
                // check that the path exist in the relative path
                File directory = new File(relativePath.toString());
                if ( !directory.isDirectory()) {
                    listEvents.add(new BEvent(eventNotADirectory, "Name[" + relativePath + "]"));
                    return listEvents;
                }

                relativePath.append( File.separatorChar );
            }
            logOperation.append("; Verified.");
            listEvents.add(new BEvent(eventDirectoryChecked, "Check And Create Directory[" + logOperation.toString() + "]"));
            
        } catch (Exception e) {
            listEvents.add(new BEvent(eventException, e, "Check And Create Directory: Complete Path[" + completePathToCheck + "]"));
            return listEvents;
        }
        return listEvents;
    }

    
    public static boolean isDirectoryExist(String completePathToCheck) {
        completePathToCheck = completePathToCheck.replace('/', File.separatorChar);
        completePathToCheck = completePathToCheck.replace('\\', File.separatorChar);

        if (completePathToCheck==null)
            return false;
        
        File file = new File(completePathToCheck);
        return file.exists();

    }
    // ------------------------------------------------------------------------
    /**
     * rename the current file in a backup file Then, for a prefix=ThisIsMyMap,
     * and a suffix .csv, the method will try to rename the currentFile in
     * ThisIsMyMap~1.bak, and, if this file exist, in ThisIsMyMap~2.bak and so on,
     * stopping when index==1000.
     * 
     * @param compleeFileName
     * @param prefix
     *        file, for example ThisIsMayMapName
     */
    public List<BEvent> nameToBackup(
            String completeFileName,
            String prefix) {
        List<BEvent> listEvents = new ArrayList<>();
        File currentFile = new File(completeFileName);
        if (!currentFile.exists()) {
            listEvents.add(new BEvent(eventBadValue, "nameToBackup: File[" + completeFileName + "] does not exists"));
            return listEvents;
        }
        File folderFile = new File(currentFile.getParent());

        prefix += "~";
        // retrieve the max name to save
        long max = 1;
        // retrieve the list of path
        String[] list = folderFile.list();
        for (int i = 0; i < list.length; i++) {
            if (list[i].startsWith(prefix)) {
                // expected: Prefix~Number.xxx
                int pos = list[i].indexOf('.', prefix.length());
                long number = 0;
                if (pos == -1)
                    number = new Long(list[i].substring(prefix.length())).longValue();
                else
                    number = new Long(list[i].substring(prefix.length(), pos)).longValue();
                if (number >= max)
                    max = number + 1;
            }
        }

        // rename to prefix~max.bak
        String destFile = prefix + max + ".bak";
        try {
            File source = currentFile;
            File dest = new File(folderFile.getAbsolutePath() + destFile);
            if (!source.renameTo(dest)) {
                listEvents.add(new BEvent(eventRenameImpossible, "Rename [" + source.getAbsolutePath() + "] to [" + dest.getAbsolutePath() + "] failed"));
                return listEvents;
            }
            return listEvents;
        } catch (Exception e) {
            listEvents.add(new BEvent(eventException, e, "Rename [" + currentFile.getAbsolutePath() + "] to [" + folderFile.getAbsolutePath() + destFile + "]"));
        }
        return listEvents;
    }

    // ---------------------------------------------------------------------------
    /**
     * isFileChanged
     * 
     * @return true if the file changed since the reference time
     */
    public static boolean isFileChanged(String fileName,
            long timeReference) {
        File file = new File(fileName);
        if (file.lastModified() > timeReference)
            return true;
        return false;
    }

    // ---------------------------------------------------------------
    /**
     * return the extension of the file
     * 
     * @param fileName
     * @param theLog
     * @return the extension or "" if no extension
     */
    public static String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }

        int index = fileName.lastIndexOf('.');
        if (index == -1) {
            return "";
        }

        return fileName.substring(index + 1);
    }

    /**
     * check that the file is correct according the upper and lower mechanism. In
     * Windows, it is possible to ask a file MYFILE for a physical file "myfile",
     * but not on linux.
     * 
     * @param completePath
     * @param theLog
     * @return
     */
    public static String checkUpperCaseFile(String pathName,
            String fileName) {
        // filename can contains somethings like '../../path/MyFile
        // then detect the last / and set the founded string to the pathname
        fileName = fileName.replace("/", File.separator);
        fileName = fileName.replace("\\", File.separator);

        if (fileName.lastIndexOf(File.separator) != -1) {
            int pos = fileName.lastIndexOf(File.separator);
            pathName += fileName.substring(0, pos + 1); /// keep the last separator
            fileName = fileName.substring(pos + 1);
        }

        File path = new File(pathName);

        if (!path.isDirectory()) {
            return null;
        }
        // retrieve the list of path
        boolean find = false;
        String[] list = path.list();
        for (int i = 0; i < list.length; i++) {
            if (list[i].compareTo(fileName) == 0)
                find = true;
        }
        if (!find) {
            // theLog.log(caller, LoggerLevel.Error, "Impossible to find the file[" + fileName + "] by the same case in pathName[" + pathName + "]");
            return null;
        }
        if (pathName.endsWith("/") || pathName.endsWith("\\"))
            return pathName + fileName;

        return pathName + File.separator + fileName;
    }

    /**
     * copy a file
     */
    public static List<BEvent> copyFile(String completeSourceFileName,
            String completeDestinationFileName) {
        List<BEvent> listEvents = new ArrayList<BEvent>();

        File destinationFile = new File(completeDestinationFileName);
        if (destinationFile.exists()) {
            // theLog.log(caller, LoggerLevel.Info, "File destination already exist[" + completeDestinationFile + "]");
            listEvents.add( new BEvent( eventFileAlreadyExist, "Destination file["+completeDestinationFileName+"]"));
            return listEvents;
        }

        try {
            FileOutputStream out = new FileOutputStream(completeDestinationFileName);
            FileInputStream in = new FileInputStream(completeSourceFileName);
            byte[] buffer = new byte[10000];
            int size = 0;
            while ((size = in.read(buffer)) != -1) {
                out.write(buffer, 0, size);
            }
            out.flush();
            out.close();
            in.close();
            return listEvents;
        }

        catch (Exception e) {
            // theLog.log(caller, "during copy file from[" + completeSourceFile + "} to[" + completeDestinationFile + "]", e);
            listEvents.add( new BEvent( eventException, "Copy File["+completeSourceFileName+"] to ["+completeDestinationFileName+"]"));
        }
        return listEvents;
        }

    /**
       * move a file
       * true if OK, false else
       */
      public static List<BEvent> moveFile(String completeSourceFile,
                                     String completeDestinationFile)
      {
          List<BEvent> listEvents= new ArrayList<>();
          listEvents.addAll( copyFile(completeSourceFile, completeDestinationFile));
          if ( ! BEventFactory.isError( listEvents))
            // delete the current one
              listEvents.addAll( removeFile(completeSourceFile));
          
          return listEvents;
      }

    /**
     * remove a file
     */
    public static List<BEvent> removeFile(String completeFileName) {
        List<BEvent> listEvents = new ArrayList<>();

        try {
            // delete
            File f = new File(completeFileName);

            boolean statusDeletion= f.delete();
            if (! statusDeletion)
                listEvents.add( new BEvent( eventDeletionFailed, "File["+completeFileName+"]"));
        } catch (Exception e) {
            listEvents.add( new BEvent( eventException, e, "removeFile ["+completeFileName+"]"));
        }
        return listEvents;
    }

    /**
     * get the content of a dir
     * 
     * @param suffix if set, then only file containing the suffix are return. Example of mask : ".zip" (* or ? are not reconized)
     * @param logError
     */
    public static ArrayList<String> getListInDirectory(String path,
            String suffix,
            boolean logError) {
        File pathFile = new File(path);
        if (!pathFile.isDirectory()) {
            return null;
        }
        ArrayList<String> listName = new ArrayList<String>();
        String[] list = pathFile.list();
        for (String name : list) {
            if (suffix == null)
                listName.add(name);
            else if (name.endsWith(suffix))
                listName.add(name);
        }
        return listName;
    }

    public static ArrayList<String> getSubdirInDirectory(String path,
            boolean logError) {
        File pathFile = new File(path);
        if (!pathFile.isDirectory()) {
            // theLog.log(caller, logError ? LoggerLevel.Error : LoggerLevel.Debug, "Path[" + path + "] is not a path !");
            return null;
        }
        ArrayList<String> listName = new ArrayList<String>();
        String[] list = pathFile.list();
        for (String name : list) {
            File subPathFile = new File(path + File.separator + name);
            if (subPathFile.isDirectory())
                listName.add(name);
        }
        return listName;
    }

    /**
     * return the simple file name. Example : c:\tmp\myfile.txt return myfile.txt
     * 
     * @param fileName
     * @param theLog
     * @param caller
     * @return
     */
    public static String getFileName(String fileName) {
        try {
            File f = new File(fileName);

            return f.getName();
        } catch (Exception e) {
            //  theLog.log(caller, "getFileName [" + fileName + "]", e);
            return null;
        }
    }

    /**
     * return the list of file detected in the path and subpath
     * 
     * @param pathName
     * @param maxDepth
     * @return
     */
    public static ArrayList<String> getRecursiveFile(String pathName,
            int maxDepth) {
        ArrayList<String> listFileName = new ArrayList<String>();
        if (maxDepth == 0)
            return listFileName;

        File path = new File(pathName);
        //  Check all file in the path
        String[] list = path.list();
        for (String fileName : list) {
            File subFile = new File(pathName + fileName);
            if (subFile.isFile())
                listFileName.add(pathName + fileName);
            if (subFile.isDirectory())
                listFileName.addAll(getRecursiveFile(pathName + fileName + File.separator, maxDepth - 1));
        }
        return listFileName;
    }

    /**
     * return a correct file name for Windows
     * 
     * @param s
     * @return
     */
    public static String getCorrectFileName(String s) {
        int i;
        char c;
        String result = "";
        for (i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            if (c == '/' || c == ':' || c == '\\' || c == '?' || c == '"' || c == '*' || c == '<' || c == '>' || c == '|') {
                result += "_";
            } else {
                result += c;
            }
        }
        return result;
    }

    /**
     * set the first letter in capital and the others in minuscule
     * 
     * @param s
     * @return
     */
    public static String putTheFirstCharUpperCase(String s) {
        String result = null;
        if (s != null) {
            // first, set all string in lower case
            s.toLowerCase();
            // then, change the first letter to upper case
            result = ("" + s.charAt(0)).toUpperCase() + s.substring(1);
        }
        return result;
    }

}
