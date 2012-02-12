package com.gigaspaces.azure.server;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AzureProject {

	private final IProject project;

	public AzureProject(IProject project) {
		this.project = project;
	}

	public IProject getProject() {
		return project;
	}

	public List<AzureRole> getRoles() {

		IFile defFile = project.getFile("ServiceDefinition.csdef");

		if (!defFile.exists())
			return null;

		File rolesDefFile = defFile.getLocation().toFile();
		return parseServiceDefinitionFile(rolesDefFile);
	}

	private List<AzureRole> parseServiceDefinitionFile(File file) {
		
		List<AzureRole> roles = new LinkedList<AzureRole>();
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(file);

			// normalize text representation
			doc.getDocumentElement().normalize();

			NodeList listOfRoles = doc.getElementsByTagName("WorkerRole");

			for (int i = 0; i < listOfRoles.getLength(); i++) {

				Node roleNode = listOfRoles.item(i);
				if (roleNode.getNodeType() == Node.ELEMENT_NODE) {

					Node roleName = roleNode.getAttributes().getNamedItem("name");
					
					if(roleName != null)
						roles.add(new AzureRole(roleName.getNodeValue(),this));
				
				}// end of if clause

			}// end of for loop with s var

		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return roles;
	}
	
//	public String getFullFileName(String relativePath)
//	{
//		String[] splitPath = relativePath.split("/");
//		IFolder currentfolder = project.getFolder("/");
//		
//		IFile file = currentfolder;
//		for (int i = 0; i < splitPath.length; i++) {
//			
//			if( i < splitPath.length -1)
//			{
//				currentfolder = currentfolder.getFolder(splitPath[i]);
//				if(!currentfolder.exists())
//			}
//			else
//			{
//				file = currentfolder.getFile(splitPath[i]);
//			}
//		}
//	}
}
