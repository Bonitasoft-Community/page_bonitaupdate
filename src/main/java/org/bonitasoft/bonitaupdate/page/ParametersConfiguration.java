package org.bonitasoft.bonitaupdate.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bonitasoft.bonitaupdate.toolbox.TypesCast;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.properties.BonitaProperties;
import org.bonitasoft.web.extension.page.PageResourceProvider;

public class ParametersConfiguration {

    static Logger logger = Logger.getLogger(ParametersConfiguration.class.getName());

    private final static BEvent eventSaveParametersError = new BEvent(ParametersConfiguration.class.getName(), 1, Level.ERROR,
            "Save parameters failed", "Error during save parameters", "Parameters are not saved", "Check the exception");
 
    private final static BEvent eventSaveParametersOk = new BEvent(ParametersConfiguration.class.getName(), 2, Level.SUCCESS,
            "Parameters saved", "Parameters saved with success");

    private final static BEvent eventLoadParametersError = new BEvent(ParametersConfiguration.class.getName(), 3, Level.ERROR,
            "Load parameters error", "Error when parameters are loaded", "Parameters can't be retrieved", "Check the exception");

    /**
     * the Tango server is a Bonita server
     */
    public boolean tangoExist;
    public String tangoServerProtocol;
    public String tangoServerName;
    public int tangoServerPort;
    public String tangoServerUserName;
    public String tangoServerPassword;

    /**
     * This is the BonitaVersion local, send to the server to get patches on this version
     */
    public String localBonitaVersion;

    public static ParametersConfiguration getInstanceFromJson(Map<String, Object> mapTango) {
        ParametersConfiguration parametersConfiguration = new ParametersConfiguration();
        if (mapTango==null)
            return parametersConfiguration;
        parametersConfiguration.fromMap( mapTango);
        return parametersConfiguration;
    }

    public void setDefault() {
        tangoServerProtocol = "http";
        tangoServerName = "localhost";
        tangoServerPort = 8080;
        tangoServerUserName = "helen.kelly";
        tangoServerPassword = "bpm";
    }

    
    public Map<String, Object> toMap() {
        Map<String, Object> mapTango = new HashMap<>();
        mapTango.put(BonitaPatchJson.CST_JSON_EXISTREFERENCE,tangoExist);
        mapTango.put(BonitaPatchJson.CST_JSON_SERVERPROTOCOL, tangoServerProtocol);
        mapTango.put(BonitaPatchJson.CST_JSON_SERVERNAME, tangoServerName);
        mapTango.put(BonitaPatchJson.CST_JSON_SERVERPORT, tangoServerPort);
        mapTango.put(BonitaPatchJson.CST_JSON_SERVERUSERNAME, tangoServerUserName);
        mapTango.put(BonitaPatchJson.CST_JSON_SERVERPASSWORD, tangoServerPassword);
        return mapTango;
    }
    public void fromMap(Map<String, Object> mapTango) {
        tangoExist = TypesCast.getBoolean(mapTango.get(BonitaPatchJson.CST_JSON_EXISTREFERENCE), false); 
        tangoServerProtocol = TypesCast.getString(mapTango.get(BonitaPatchJson.CST_JSON_SERVERPROTOCOL), null);
        tangoServerName = TypesCast.getString(mapTango.get(BonitaPatchJson.CST_JSON_SERVERNAME), null);
        tangoServerPort = TypesCast.getInteger(mapTango.get(BonitaPatchJson.CST_JSON_SERVERPORT), 8080);
        tangoServerUserName = TypesCast.getString(mapTango.get(BonitaPatchJson.CST_JSON_SERVERUSERNAME), null);
        tangoServerPassword = TypesCast.getString(mapTango.get(BonitaPatchJson.CST_JSON_SERVERPASSWORD), null);
   
    }
    /**
     * 
     * @param pageResourceProvider
     * @return
     */
    public List<BEvent> save(PageResourceProvider pageResourceProvider) {
        BonitaProperties bonitaProperties = new BonitaProperties( pageResourceProvider);
        bonitaProperties.setCheckDatabase(false); // already done at load
        List<BEvent> listEvents = new ArrayList<>();
        try {
            listEvents.addAll(bonitaProperties.load());
            for (Entry<String, Object> entry : toMap().entrySet()) {
                bonitaProperties.setProperty(entry.getKey(), entry.getValue().toString());
            }
            listEvents.addAll(bonitaProperties.store());
            listEvents.add(eventSaveParametersOk);
        } catch (Exception e) {
            logger.severe("Exception " + e.toString());
            listEvents.add(new BEvent(eventSaveParametersError, e, "Error :" + e.getMessage()));
        }
        return listEvents;
    }
    /**
     * 
     * @param firstLoad
     * @return
     */
    public List<BEvent> load(boolean firstLoad,PageResourceProvider pageResourceProvider) {
        BonitaProperties bonitaProperties = new BonitaProperties(pageResourceProvider);
        if (firstLoad)
            bonitaProperties.setCheckDatabase(true);
        else
            bonitaProperties.setCheckDatabase(false);
        
        /** initialise with default value */
        
        Map<String, Object> mapTango = new HashMap<>();
        List<BEvent> listEvents = new ArrayList<>();
        try {
            listEvents.addAll(bonitaProperties.load());
            for (String key : bonitaProperties.stringPropertyNames())
                mapTango.put(key, bonitaProperties.getProperty(key));
            if (mapTango.size()>0)
                fromMap( mapTango );
            else
                setDefault();        
        } catch (Exception e) {
            logger.severe("Exception " + e.toString());
            listEvents.add(new BEvent(eventLoadParametersError, e, "Error :" + e.getMessage()));
        }

        return listEvents;
    }
}