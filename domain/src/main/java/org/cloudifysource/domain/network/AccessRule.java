package org.cloudifysource.domain.network;

public class AccessRule {

	private AccessRuleType type = null;
	private String target = null;

	public AccessRule() {

	}

	public AccessRuleType getType() {
		return type;
	}

	public void setType(final AccessRuleType type) {
		this.type = type;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(final String target) {
		this.target = target;
	}

}
