package org.cloudifysource.rest.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * AlertsRestController provides Rest implementation for Alerts.
 * 
 * 
 * 
 * @author ahmad
 * @since 2.5.0
 */
@Controller
@RequestMapping(value = "/{version}/alerts")
public class AlertController extends BaseRestContoller {
	
	
	/******
	 * get manager alerts.
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public void getManagerAlerts() {
			throwUnsupported();
	}
	
	

}
