package org.cloudifysource.dsl;

import java.util.List;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "widgetGroup", clazz = WidgetGroup.class, allowInternalNode = true,
allowRootNode = false, parent = "userInterface")
public class WidgetGroup {

    private String name;
    private String title;
	private List<?> widgets;
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
	public <T> List<T> getWidgets() {
		return (List<T>) widgets;
	}
	public <T> void setWidgets(List<T> widgets) {
		this.widgets = widgets;
	}
}
