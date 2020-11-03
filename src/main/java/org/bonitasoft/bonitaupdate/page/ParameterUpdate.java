package org.bonitasoft.bonitaupdate.page;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.bonitaupdate.toolbox.TypesCast;
import org.bonitasoft.engine.session.APISession;
import org.json.simple.JSONValue;

public class ParameterUpdate {

    public File bonitaRootDirectory;
    public String bonitaVersion;
    public APISession apiSession;
    public String patchName;

    public List<String> listPatchesName;

    ParametersConfiguration parametersConfiguration;

    @SuppressWarnings("unchecked")
    public static ParameterUpdate getInstanceFromJson(String jsonSt, APISession apiSession, File bonitaRootDirectory) {
        ParameterUpdate parameter = new ParameterUpdate();
        parameter.apiSession = apiSession;

        parameter.bonitaRootDirectory = bonitaRootDirectory;

        if (jsonSt == null) {
            parameter.parametersConfiguration = new ParametersConfiguration();
            parameter.parametersConfiguration.setDefault();
            parameter.parametersConfiguration.localBonitaVersion = parameter.detectBonitaVersion(bonitaRootDirectory);
            return parameter;
        }
        try {
            Map<String, Object> param = (Map<String, Object>) JSONValue.parse(jsonSt);
            parameter.listPatchesName = (List<String>) TypesCast.getList(param.get(BonitaPatchJson.CST_JSON_PATCHES), new ArrayList<>());
            parameter.bonitaVersion = TypesCast.getString(param.get(BonitaPatchJson.CST_JSON_BONITAVERSION), null);
            parameter.patchName = TypesCast.getString(param.get(BonitaPatchJson.CST_JSON_PATCHNAME), null);

            // Map paramServer = (Map<String, Object>) param.get(BonitaPatchJson.CST_JSON_PARAM);
            Map<String, Object> paramTango = (Map<String, Object>) param.get(BonitaPatchJson.CST_JSON_PARAMETERTANGO);
            parameter.parametersConfiguration = ParametersConfiguration.getInstanceFromJson(paramTango);

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            BonitaUpdateAPI.logger.severe("Parameter: ~~~~~~~~~~  : ERROR " + e + " at " + exceptionDetails);
        }
        return parameter;
    }

    public void setBonitaRootDirectory(File bonitaRootDirectory) {
        this.bonitaRootDirectory = bonitaRootDirectory;
    }

    public void setBonitaVersion(String bonitaVersion) {
        this.bonitaVersion = bonitaVersion;
    }

    public PatchConfiguration getPatchConfiguration() {
        return new PatchConfiguration(bonitaRootDirectory, bonitaVersion, apiSession, parametersConfiguration);
    }

    /**
     * there is no API which return the version (except the PlatformAPI, but you need to connect as the platform manager)
     * So, access the file VERSION and read the first line;
     * 
     * @param bonitaRootDirectory
     * @return
     */
    String detectBonitaVersion(File bonitaRootDirectory) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(bonitaRootDirectory.getPath() + "/webapps/bonita/VERSION"), StandardCharsets.UTF_8)) {
            this.bonitaVersion = reader.readLine();
            return this.bonitaVersion; // the version is the first line
        } catch (IOException e) {
            BonitaUpdateAPI.logger.severe("Can't read VERSION file under [" + bonitaRootDirectory.getPath() + "/webapps/bonita/VERSION" + "] : " + e.getMessage());
        }
        // return new BonitaVersion(new VersionFile()).getVersion();
        return null;

    }
    
  
}