package org.bonitasoft.bonitaupdate.page;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.bonitaupdate.toolbox.FileTool;
import org.bonitasoft.log.event.BEvent;

/** contains everything is necessary for the configuration 
 * 
 * @author Firstname Lastname
 *
 */
public class PatchConfiguration {

    File serverFolder;
    File patchFolder;
    String version;
    
    public enum FOLDER { BONITASERVER, INSTALL, UNINSTALL, DOWNLOAD, TANGOSERVER };
    
    public String serverProtocol;
    public String serverName; 
    public int serverPort;
    public String serverUserName; 
    public String serverPassword;
    
    public String userName;
    
    public PatchConfiguration( File bonitaServerPath, String version,  String serverProtocol,
    String serverName, 
    int serverPort,
    String serverUserName, 
    String serverPassword,
    String userName) {
        this.serverFolder = bonitaServerPath;
        this.version = version;
        this.patchFolder = new File( bonitaServerPath.getAbsolutePath()+File.separator+"patches");
        this.serverProtocol= serverProtocol;
        this.serverName = serverName; 
        this.serverPort = serverPort;
        this.serverUserName = serverUserName; 
        this.serverPassword= serverPassword;
        this.userName = userName;
     
    }
    
    
    /**
     * This method should be call to validate the configuration
     * @return
     */
    public List<BEvent> validateConfiguration() {
        List<BEvent> listEvents = new ArrayList<>();
        listEvents.addAll( FileTool.checkAndCreateDir( this.getFolderPath( FOLDER.INSTALL)));
        listEvents.addAll( FileTool.checkAndCreateDir( this.getFolderPath( FOLDER.DOWNLOAD)));
        listEvents.addAll( FileTool.checkAndCreateDir( this.getFolderPath( FOLDER.UNINSTALL)));
        listEvents.addAll( FileTool.checkAndCreateDir( this.getFolderPath( FOLDER.TANGOSERVER)));
        return listEvents;
    }
    
    public File getFolder( FOLDER  folder) {
        if (FOLDER.BONITASERVER.equals( folder ))
            return serverFolder;
        if (FOLDER.INSTALL.equals( folder ))
            return new File( patchFolder.getAbsolutePath()+File.separator+"installed");
        if (FOLDER.UNINSTALL.equals( folder ))
            return new File( patchFolder.getAbsolutePath()+File.separator+"uninstalled");
        if (FOLDER.DOWNLOAD.equals( folder ))
            return new File( patchFolder.getAbsolutePath()+File.separator+"downloaded");
        if (FOLDER.TANGOSERVER.equals( folder ))
            return new File( patchFolder.getAbsolutePath()+File.separator+"reference");
        return null;
    }
    public String getFolderPath( FOLDER  folder) {
        File file = getFolder(folder);
        if (file==null)
            return null;
        return file.getAbsolutePath()+File.separator;
    }
        
    
    
    
}
