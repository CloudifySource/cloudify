package org.cloudifysource.restDoclet.docElements;

public class DocResponseStatus {
	private final int code;
	private final String codeName;
	private final String description;
	
	public DocResponseStatus(int code, String codeName, String description) {
		this.code = code;
		this.codeName = codeName;
		this.description = description;
	}

	public int getCode() {
		return code;
	}
	public String getCodeName() {
		return codeName;
	}
	public String getDescription() {
		return description;
	}	
	
	@Override
	public String toString() {
		return "\"" + code + " " + codeName + "\" " + description;
	}
}
