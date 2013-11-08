package org.cloudifysource.esc.driver.provisioning.openstack.rest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.map.annotate.JsonRootName;

@JsonRootName("router")
public class Router {
	private String status;

	private RouterExternalGatewayInfo externalGatewayInfo;

	private String name;
	private Boolean adminStateUp;
	private String tenantId;
	// private String routes;
	private String id;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getAdminStateUp() {
		return adminStateUp;
	}

	public void setAdminStateUp(Boolean adminStateUp) {
		this.adminStateUp = adminStateUp;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public RouterExternalGatewayInfo getExternalGatewayInfo() {
		return externalGatewayInfo;
	}

	public void setExternalGatewayInfo(RouterExternalGatewayInfo externalGatewayInfo) {
		this.externalGatewayInfo = externalGatewayInfo;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
