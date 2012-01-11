package org.cloudifysource.rest.controllers;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.cloudifysource.rest.command.CommandManager;
import org.cloudifysource.rest.out.OutputDispatcher;
import org.cloudifysource.rest.util.NotFoundHttpException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.os.OperatingSystem;
import org.openspaces.admin.os.OperatingSystemDetails.NetworkDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;


/**
 * Spring MVC controller for the RESTful Admin API 
 * via a reflection-based implementation of dispatcher pattern
 *
 *  - Accepts a generic uri path which denotes a specific Admin request
 *  
 *  - Parses and walks through the uri by activating getter methods
 *    to "dig into" the admin object hierarchy
 *     
 *  - Results marshaled as a generic document serialized to a JSON object
 *
 *             
 *	 Usage examples:
 *	     http://localhost:8099/admin/ElasticServiceManagers/Managers
 *	     http://localhost:8099/admin/GridServiceManagers/size
 *	     http://localhost:8099/admin/Spaces
 *	     http://localhost:8099/admin/VirtualMachines/VirtualMachines
 *	     http://localhost:8099/admin/VirtualMachines/VirtualMachines/3
 *	     http://localhost:8099/admin/VirtualMachines/VirtualMachines/3/Statistics/Machine/GridServiceAgents
 *	     DETAILS:  http://localhost:8099/admin/GridServiceManagers/Uids/49a6e2ef-5fd3-471a-94ff-c961a52ffd0f
 *	     STATIST:  http://localhost:8099/admin/GridServiceManagers/Uids/49a6e2ef-5fd3-471a-94ff-c961a52ffd0f
 *              
 *  Note that the wiring and marshaling services are provided by Spring framework
 *  
 *  Note 2: It is highly recommended that results will be viewed
 *  on FF with JsonView plugin
 *
 * @author giladh, adaml
 */


@Controller
@RequestMapping(value = "/admin/*")
public class AdminAPIController  {

	private static String ADMIN_VIEW = "index";
	
	@Autowired(required = true)
	private Admin admin;

    private final Logger logger = Logger.getLogger(getClass().getName());

	private OperatingSystem operatingSystem;

	private Map<String, NetworkDetails> networkDetails;
	
	/**
	 * redirects to index view
	 * @return
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public ModelAndView redirectToIndex(){
		return new ModelAndView("index");
	}

	/**
	 * REST GET requests handler wrapper
	 */
	@RequestMapping(value = "/**", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> get(HttpServletRequest httpServletRequest) throws Exception{
		return getImplementation(httpServletRequest);
	}		

	/**
	 * REST GET requests handler implementation
	 *    Parses uri path
	 *    activates appropriate getters
	 *    serialize results into a document object and pass for JSON marshaling
	 *
	 *		        uri type                          processing 
	 *		      ============                      ==============
	 *		    http:/../getArr/ind/...       =>  (intermed.) resolve to arr[ind] and continue processing
	 *		    http:/../getMap/key/...       =>  (intermed.) resolve to map.get(key) and continue processing
	 *		    http:/../getList/ind/...      =>  (intermed.) resolve to list(ind) and continue processing		
	 *		    http:/../getObj               =>  (final) return obj fields (by public getters)
	 *		    http:/../getArr               =>  (final) return arr.length
	 *		    http:/../getList              =>  (final) return list.size()
	 *		    http:/../getMap               =>  (final) return comma-separated list of map keys
	 *
	 *    
	 */
	private Map<String, Object> getImplementation(HttpServletRequest httpServletRequest) throws Exception {
		//admin acts as root
		CommandManager manager = new CommandManager(httpServletRequest, getAdmin());
		manager.runCommands();
		String hostAddress = getRemoteHostAddress(httpServletRequest);
		String hostContext = httpServletRequest.getContextPath();
		return OutputDispatcher.outputResultObjectToMap(manager, hostAddress, hostContext);
	}


	private String getRemoteHostAddress(HttpServletRequest httpServletRequest) {
		String host = httpServletRequest.getServerName();
		int port = httpServletRequest.getServerPort();
		return "http://" + host + ":" + port;
	}

	public Admin getAdmin() {
		return admin;
	}
	@ExceptionHandler(NotFoundHttpException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public void resolveNotFound(Writer writer, Exception e) throws IOException {
        
    }
	@ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public void resolveInternalServerError(Writer writer, Exception e) throws IOException {
        logger.log(Level.SEVERE, "caught exception", e);
        writer.write("{\"status\":\"error\", \"error\":\"" + e.getMessage() + "\"}");
    }
}

