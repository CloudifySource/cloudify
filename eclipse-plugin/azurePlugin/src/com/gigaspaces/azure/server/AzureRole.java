package com.gigaspaces.azure.server;

public class AzureRole {

	private final String role;
	private final AzureProject project;

	public AzureRole(String role,AzureProject project) {
		this.role = role;
		this.project = project;
	}

	public String getRole() {
		return role;
	}

	public AzureProject getProject() {
		return project;
	}

}
