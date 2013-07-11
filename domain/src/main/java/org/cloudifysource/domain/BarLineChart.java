package org.cloudifysource.domain;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "barLineChart", clazz = MetricGroup.class, allowInternalNode = true,
allowRootNode = false, parent = "widgetGroup")
public class BarLineChart extends AbstractBasicWidget implements Widget {

    private Unit axisYUnit = Unit.REGULAR; 

    public Unit getAxisYUnit() {
        return axisYUnit;
    }

    public void setAxisYUnit(Unit axisYUnit) {
        this.axisYUnit = axisYUnit;
    }
}
