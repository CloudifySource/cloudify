/*******************************************************************************
 *
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.cloudifysource.dsl.utils;

import java.util.ArrayList;
import java.util.List;

import org.openspaces.ui.BalanceGauge;
import org.openspaces.ui.BarLineChart;
import org.openspaces.ui.MetricGroup;
import org.openspaces.ui.Unit;
import org.openspaces.ui.UserInterface;
import org.openspaces.ui.Widget;
import org.openspaces.ui.WidgetGroup;

/**
 * Converts the UserInterface POJO from the new integrated DSL form to the old UserInterface POJO form.
 *
 * The converter object will define default widgets in-case no widgets were defined
 * in the case that axisYUnit was overridden the overridden unit should be used.
 *
 * The converter will return a new backward compatible UserInterface object with the default values set
 * having the old metric list form.
 *
 * @author adaml
 * @since 2.5.0
 *
 */
public class UserInterfaceConverter {

	private static final Unit DEFAULT_AXIS_Y_UNIT = Unit.REGULAR;
	private static final String DEFAULT_WIDGET_GROUP_NAME = " default name";
	private static final String DEFAULT_WIDGET_TITLE_NAME = " default title";

	/**
	 * Converts the UserInterface POJO from the new integrated
	 * DSL form to the old UserInterface POJO form.
	 *
	 * @param userInterface
	 * 		integrated UserInterface POJO.
	 * @return
	 * 		a backward compatible UserInterface object.
	 */
	public UserInterface convertUserInterface(final UserInterface userInterface) {
		UserInterface userInterfaceObject = new UserInterface();

		List<MetricGroup> metricGroups = userInterface.getMetricGroups();
		List<WidgetGroup> widgetGroups = userInterface.getWidgetGroups();

		if (metricGroups != null) {
			//metrics defined but no widgets
			if (widgetGroups == null) {
				userInterfaceObject.setWidgetGroups(getDefaultWidgetGroups(metricGroups));
			} else {
				userInterfaceObject.setWidgetGroups(widgetGroups);
			}

			userInterfaceObject.setMetricGroups(getStandardMetricGroups(metricGroups));
		}

		return userInterfaceObject;
	}

	//The matrixes inside the UserInterface should be
	//of type String to support backward compatibility
	@SuppressWarnings("rawtypes")
	private List<MetricGroup> getStandardMetricGroups(
			final List<MetricGroup> metricGroups) {
		List<MetricGroup> standardMetricGroups = new ArrayList<MetricGroup>();

		for (MetricGroup metricGroup : metricGroups) {

			MetricGroup standardMetricGroup = new MetricGroup();
			standardMetricGroup.setName(metricGroup.getName());
			List<Object> standardMetricsList = new ArrayList<Object>();

			for (Object metric : metricGroup.getMetrics()) {
				if (metric instanceof List<?>) {
					standardMetricsList.add(((List) metric).get(0).toString());
				} else {
					standardMetricsList.add(metric);
				}
			}

			standardMetricGroup.setMetrics(standardMetricsList);
			standardMetricGroups.add(standardMetricGroup);
		}
		return standardMetricGroups;
	}

	//Create default widgets. in the case that axisYUnit was overridden
	//the overridden unit should be used.
	private List<WidgetGroup> getDefaultWidgetGroups(
			final List<MetricGroup> metricGroups) {
		List<WidgetGroup> widgetGroups = new ArrayList<WidgetGroup>();

		for (MetricGroup metricGroup : metricGroups) {
			for (Object metric : metricGroup.getMetrics()) {
				WidgetGroup widgetGroup = new WidgetGroup();
				List<Widget> widgets = new ArrayList<Widget>();

				String metricName;
				Unit axisYUnit;
				//axisYUnit was overridden
				if (metric instanceof List<?>) {
					@SuppressWarnings("unchecked")
					List<Object> metricDef = (List<Object>) metric;
					metricName = metricDef.get(0).toString();
					axisYUnit = (Unit) metricDef.get(1);
				} else {
					metricName = metric.toString();
					axisYUnit = DEFAULT_AXIS_Y_UNIT;
				}

				//The two default widgets
				BarLineChart chart = new BarLineChart();
				chart.setAxisYUnit(axisYUnit);
				chart.setMetric(metricName);
				widgets.add(chart);

				BalanceGauge gauge = new BalanceGauge();
				gauge.setMetric(metricName);
				widgets.add(gauge);

				widgetGroup.setWidgets(widgets);
				widgetGroup.setName(metricName + DEFAULT_WIDGET_GROUP_NAME);
				widgetGroup.setTtile(metricName + DEFAULT_WIDGET_TITLE_NAME);
				widgetGroups.add(widgetGroup);
			}
		}

		return widgetGroups;
	}
}
