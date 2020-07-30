import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.logging.Logger;
import java.io.File
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;



import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.Clob;
import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils

import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;

import org.bonitasoft.web.extension.page.PageContext;
import org.bonitasoft.web.extension.page.PageController;
import org.bonitasoft.web.extension.page.PageResourceProvider;

import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo;
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfoSearchDescriptor;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ActivationState
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceSearchDescriptor;
import org.bonitasoft.engine.identity.UserSearchDescriptor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.IdentityAPI;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;


import org.bonitasoft.bonitaupdate.page.BonitaUpdateAPI;
import org.bonitasoft.bonitaupdate.page.BonitaUpdateAPI.ParameterUpdate;



public class Actions {

    private static Logger logger= Logger.getLogger("org.bonitasoft.custompage.truckmilk.groovy");




    // 2018-03-08T00:19:15.04Z
    public final static SimpleDateFormat sdfJson = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public final static SimpleDateFormat sdfHuman = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* doAction */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public static Index.ActionAnswer doAction(HttpServletRequest request, String paramJsonSt, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {

        // logger.fine("#### cockpit:Actions start");
        Index.ActionAnswer actionAnswer = new Index.ActionAnswer();
        List<BEvent> listEvents=new ArrayList<BEvent>();


        try {
            String action=request.getParameter("action");

            if (action==null || action.length()==0 )
            {
                actionAnswer.isManaged=false;
                // logger.fine("#### log:Actions END No Actions");
                return actionAnswer;
            }
            actionAnswer.isManaged=true;

            APISession apiSession = pageContext.getApiSession();
            HttpSession httpSession = request.getSession();
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            IdentityAPI identityAPI = TenantAPIAccessor.getIdentityAPI(apiSession);

            ParameterUpdate parameter = ParameterUpdate.getInstanceFromJson(paramJsonSt, apiSession);
            
            File pageDirectory = pageResourceProvider.getPageDirectory();
            File bonitaServerDirectory= new File( pageResourceProvider.getPageDirectory().getAbsolutePath()+"/../../../../../../");
            parameter.setBonitaRootDirectory( new File( bonitaServerDirectory.getCanonicalPath()) );
            
            
            BonitaUpdateAPI bonitaUpdateAPI = new BonitaUpdateAPI();
     

            // logger.fine("#### log:Actions_2 ["+action+"]");
           if ("init".equals(action)) {
                actionAnswer.responseMap = bonitaUpdateAPI.init( parameter );
                
           } else if ("refresh".equals(action)) {
                actionAnswer.responseMap = bonitaUpdateAPI.refresh( parameter );
                
           } else if ("refreshserver".equals(action)) {
                actionAnswer.responseMap = bonitaUpdateAPI.serverListPatches( parameter );
                
           } else if ("download".equals(action)) {
                actionAnswer.responseMap = bonitaUpdateAPI.download( parameter );
                
           } else if ("install".equals(action))
           {
                actionAnswer.responseMap = bonitaUpdateAPI.install( parameter );
           } else if ("uninstall".equals(action))
           {
                actionAnswer.responseMap = bonitaUpdateAPI.uninstall( parameter );           
           } else if ("tangoserverlistpatches".equals(action))
            {
                 actionAnswer.responseMap = bonitaUpdateAPI.tangoserverListPatches( parameter );
            }
           
            logger.info("#### TruckMilk:Actions END responseMap.size()="+actionAnswer.responseMap.size());
            return actionAnswer;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("#### TruckMilk:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            actionAnswer.isResponseMap=true;
            actionAnswer.responseMap.put("Error", "log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);



            return actionAnswer;
        }
    }





}
