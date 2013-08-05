package org.cloudifysource.domain;

import java.util.List;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "metricGroup", clazz = MetricGroup.class, allowInternalNode = true,
allowRootNode = false, parent = "userInterface")
public class MetricGroup {
	private String name;
	private List<?> metrics;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public <T> List<T> getMetrics() {
		return (List<T>) metrics;
	}
	public <T> void setMetrics(List<T> metrics) {
		this.metrics = metrics;
	}
}
