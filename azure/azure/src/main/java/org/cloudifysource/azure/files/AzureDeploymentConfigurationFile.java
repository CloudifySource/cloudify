/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.azure.files;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.logging.Logger;

/**
 * @see http://msdn.microsoft.com/en-us/library/ee758710.aspx
 *
 */
public class AzureDeploymentConfigurationFile extends AbstractAzureDeploymentFile {
	
	private static final Logger logger = Logger.getLogger(AzureDeploymentConfigurationFile.class.getName());
	
    public AzureDeploymentConfigurationFile(File cscfgFile)
            throws XMLXPathEditorException {
        super(cscfgFile);
    }

    public void setDeploymentName(String deploymentName) throws XMLXPathEditorException {
        getEditor().setNodeAttribute("/ServiceConfiguration", "serviceName", deploymentName);
    }
    
    public void setServices(AzureServiceDefinition[] serviceDefinitions) throws XMLXPathEditorException {
        String[] services = AzureServiceDefinition.extractServiceNames(serviceDefinitions);
        getEditor().findNodeDuplicateByAttribute("/ServiceConfiguration/Role[@name='internal']", services, "name");    
        for (AzureServiceDefinition serviceDefinition : serviceDefinitions) {
            setNumberOfInstances(serviceDefinition.getServiceName(), serviceDefinition.getNumberOfInstances());
        }
    }
    
    public void setGigaSpacesXAPDownloadUrl(URI gigaSpacesUrl) throws XMLXPathEditorException, MalformedURLException {
        String urlEncodedGigaspacesURL = null;
        try {
            urlEncodedGigaspacesURL = URLEncoder.encode(gigaSpacesUrl.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 must exist");
        }
        
        getEditor().setNodeAttribute(
                "/ServiceConfiguration/Role/ConfigurationSettings/Setting[@name='GigaSpaces.XAP.DownloadUrl']",
                "value", 
                urlEncodedGigaspacesURL);
    }

    public void setBlobStoreAccountCredentials(String accountUsername, String accountKey) throws XMLXPathEditorException {
        
        String value = new StringBuilder()
            .append("DefaultEndpointsProtocol=https;AccountName=")
            .append(accountUsername)
            .append(";AccountKey=")
            .append(accountKey).toString();
        
        getEditor().setNodeAttribute(
                "/ServiceConfiguration/Role/ConfigurationSettings/Setting[@name='Microsoft.WindowsAzure.Plugins.Diagnostics.ConnectionString']",
                "value", 
                value);
    }
    
    public void setRdpLoginCredentials(String loginUsername, String loginEncryptedPassword) throws XMLXPathEditorException {
        
        getEditor().setNodeAttribute(
                "/ServiceConfiguration/Role/ConfigurationSettings/Setting[@name='Microsoft.WindowsAzure.Plugins.RemoteAccess.AccountUsername']",
                "value", 
                loginUsername);

        getEditor().setNodeAttribute(
                "/ServiceConfiguration/Role/ConfigurationSettings/Setting[@name='Microsoft.WindowsAzure.Plugins.RemoteAccess.AccountEncryptedPassword']",
                "value", 
                loginEncryptedPassword);
        
    }
    
    public int getNumberOfInstances(String role) throws XMLXPathEditorException {
        String resultString = getEditor().getNodeValue(getXpathForInstanceCount(role));
        try {
            return Integer.parseInt(resultString);
        } catch (NumberFormatException e) {
            throw new XMLXPathEditorException(e);
        }
    }

    public void setNumberOfInstances(String role, int instances) throws XMLXPathEditorException {
    	logger.fine("Setting role " + role + " number of instances to " + instances);
        getEditor().setNodeValue(getXpathForInstanceCount(role), String.valueOf(instances));
    }

    private static String getXpathForInstanceCount(String roleName) {
        return "/ServiceConfiguration/Role[@name='" + roleName + "']/Instances/@count";
    }

	public void setRdpCertificateThumbprint(String thumbprint) throws XMLXPathEditorException {
		getEditor().setNodeAttribute(
				"/ServiceConfiguration/Role/Certificates/Certificate",
                "thumbprint", 
                thumbprint);
	}
	
    public void setUploadAgentLogs(boolean uploadAgentLogs) throws XMLXPathEditorException {
        setRoleConfigurationSettings("GigaSpaces.XAP.UploadAgentLogs", String.valueOf(uploadAgentLogs));
    }
	
	public void setUploadAgentLogs(String role, boolean uploadAgentLogs) throws XMLXPathEditorException {
	    setRoleConfigurationSettings(role, "GigaSpaces.XAP.UploadAgentLogs", String.valueOf(uploadAgentLogs));
	}
	
	public void setUploadAllLogs(String role, boolean uploadAllLogs) throws XMLXPathEditorException {
	    setRoleConfigurationSettings(role, "GigaSpaces.XAP.UploadAllLogs", String.valueOf(uploadAllLogs));
	}
	    
	public void setUploadAllLogs(boolean uploadAllLogs) throws XMLXPathEditorException {
	    setRoleConfigurationSettings("GigaSpaces.XAP.UploadAllLogs", String.valueOf(uploadAllLogs));
	}
	
	/**
	 * applies change to all roles 
	 * 
     * @param key the key the configuration setting
     * @param value the value of the configuration setting
     * @throws XMLXPathEditorException
	 * @throws XMLXPathEditorException
	 */
	private void setRoleConfigurationSettings(String key, String value) throws XMLXPathEditorException {
	    setRoleConfigurationSettings(null, key, value);
	}
	
	/**
	 * 
	 * @param role The role to apply the changes on. passing null means this change will be applied to ALL roles
	 * @param key the key the configuration setting
	 * @param value the value of the configuration setting
	 * @throws XMLXPathEditorException
	 */
	private void setRoleConfigurationSettings(String role, String key, String value) throws XMLXPathEditorException {
	    String xpath = role != null ?  
	            "/ServiceConfiguration/Role[@name='" + role + "']/ConfigurationSettings[@name='" + key + "']" :
	            "/ServiceConfiguration/Role/ConfigurationSettings[@name='" + key + "']" ;   
	    getEditor().setNodeAttribute(
	            xpath, 
	            "value", 
	            value);
	}
    
}