package org.cloudifysource.dsl;

import java.util.List;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "userInterface", clazz = UserInterface.class, allowInternalNode = true,
allowRootNode = false, parent = "service")
public class UserInterface {
	
	private List<MetricGroup> metricGroups;
	private List<WidgetGroup> widgetGroups;
	
	public List<MetricGroup> getMetricGroups() {
		return metricGroups;
	}
	public void setMetricGroups(List<MetricGroup> metricGroups) {
		this.metricGroups = metricGroups;
	}
	public List<WidgetGroup> getWidgetGroups() {
		return widgetGroups;
	}
	public void setWidgetGroups(List<WidgetGroup> widgetGroups) {
		this.widgetGroups = widgetGroups;
	}

}
