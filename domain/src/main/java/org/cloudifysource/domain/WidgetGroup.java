package org.cloudifysource.domain;

import java.util.List;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "widgetGroup", clazz = WidgetGroup.class, allowInternalNode = true,
allowRootNode = false, parent = "userInterface")
public class WidgetGroup {

    private String name;
    private String title;
	private List<Widget> widgets;
	public String getName() {
		return name;
	}
	public String getTitle(){
	    return title == null ? getName() : title;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setTtile(String title){
	    this.title = title;
	}
	public List<Widget> getWidgets() {
		return (List<Widget>) widgets;
	}
	public void setWidgets(List<Widget> widgets) {
		this.widgets = widgets;
	}
}
