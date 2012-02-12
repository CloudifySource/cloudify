package com.gigaspaces.azure.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

public class AzureProjectTools {

	public static List<AzureProject> getProjects() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		IProject[] projects = workspace.getRoot().getProjects();

		LinkedList<AzureProject> azureProjects = new LinkedList<AzureProject>();

		for (IProject project : projects) {

			if (project.getFile("ServiceDefinition.csdef").exists()) {
				azureProjects.add(new AzureProject(project));
			}
		}
		return azureProjects;
	}

	public static void createZipFile(String target, String sourceDirName) {
		
		try {
			
			int lastPathIndex = sourceDirName.lastIndexOf('\\');
			// Create the ZIP file
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
					target));

			zipDir(sourceDirName,out,lastPathIndex);
			
			// Complete the ZIP file
			out.close();
		} catch (IOException e) {
		}

	}

	// here is the code for the method
	public static void zipDir(String dir2zip, ZipOutputStream zos,int lastPathIndex) {
		try {
			// create a new File object based on the directory we
			// have to zip File
			File zipDir = new File(dir2zip);
			// get a listing of the directory content
			String[] dirList = zipDir.list();
			byte[] readBuffer = new byte[2156];
			int bytesIn = 0;
			// loop through dirList, and zip the files
			for (int i = 0; i < dirList.length; i++) {
				File f = new File(zipDir, dirList[i]);
				if (f.isDirectory()) {
					// if the File object is a directory, call this
					// function again to add its content recursively
					String filePath = f.getPath();
					zipDir(filePath, zos,lastPathIndex);
					// loop again
					continue;
				}
				// if we reached here, the File object f was not
				// a directory
				// create a FileInputStream on top of f
				FileInputStream fis = new FileInputStream(f);
				// create a new zip entry
				ZipEntry anEntry = new ZipEntry(f.getPath().substring(lastPathIndex+1));
				// place the zip entry in the ZipOutputStream object
				zos.putNextEntry(anEntry);
				// now write the content of the file to the ZipOutputStream
				while ((bytesIn = fis.read(readBuffer)) != -1) {
					zos.write(readBuffer, 0, bytesIn);
				}
				// close the Stream
				fis.close();
				
				
			}
		} catch (Exception e) {
			// handle exception
		}
	}
	
	public static void main(String[] args) {
		createZipFile("webserver.zip","C:\\Program Files\\Apache Software Foundation\\Tomcat 7.0");
	}
	
}
