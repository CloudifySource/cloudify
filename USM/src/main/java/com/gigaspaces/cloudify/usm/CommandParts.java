package com.gigaspaces.cloudify.usm;

import java.util.List;

public class CommandParts {

	private List<String> part;
	
	public CommandParts() {
	}

	public CommandParts(List<String> part) {
		super();
		this.part = part;
	}

	public List<String> getPart() {
		return part;
	}

	public void setPart(List<String> part) {
		this.part = part;
	}
	
}
