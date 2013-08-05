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
package org.cloudifysource.usm;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.activation.UnsupportedDataTypeException;

import junit.framework.Assert;

import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.usm.dsl.UserInterfaceConverter;
import org.junit.Test;
import org.openspaces.ui.BarLineChart;
import org.openspaces.ui.Unit;

public class UserInterfaceConverterTest {

	private static final int NOMBER_OF_NON_DEFAULT_WIDGET_GROUPS = 8;

	private static final int DEFAULT_NUM_OF_WIDGETS = 2;

	private static final int NUMBER_OF_METRIC_GROUPS_AS_DEFINED_IN_GROOVY = 2;
	
	private static final String OVERRIDDEN_MATRIC_AXISY_NAME = 
			"overrriddenYAxisMetric";
	
	private static final String OVERRIDDEN_MATRIC_AXISY_WIDGET_GROUP_NAME = 
			OVERRIDDEN_MATRIC_AXISY_NAME + " default name";
	
	private static final String EXISTING_WIDGET_TESTING_GROUP_NAME = 
			"TestingWidgetGroup";
	
	private static final String SERVICE_FILES_BASE_DIR = 
			"src/test/resources/userInterfaceConversionTestFiles/";

	private static final String NO_WIDGETS_SERVICE_FILE_PATH = 
			SERVICE_FILES_BASE_DIR + "service_with_metrics_no_widgets.groovy";

	private static final String SERVICE_WITH_WIDGETS_FILE_PATH = 
			SERVICE_FILES_BASE_DIR + "service_with_metrics_and_widgets.groovy";


	/**
	 * 	Since no widgets were defined in the UserInterface POJO,
	 *	default ones should be created.
	 * @throws Exception
	 */

	@Test
	public void testConversionWithDefaultWidgets()
			throws Exception {
		Service wigetlessServiceFile = ServiceReader.readService(new File(NO_WIDGETS_SERVICE_FILE_PATH));

		org.openspaces.ui.UserInterface convertedUserInterface = getConvertedObject(wigetlessServiceFile);
		

		//assert all the groups in the groovy are accounted for.
		Assert.assertEquals("Expected number of metric groups was not met." 
				+ " found " + convertedUserInterface.getMetricGroups().size()
				+ " expected " + NUMBER_OF_METRIC_GROUPS_AS_DEFINED_IN_GROOVY
				, convertedUserInterface.getMetricGroups().size(), 
				NUMBER_OF_METRIC_GROUPS_AS_DEFINED_IN_GROOVY);

		//each metric should have a widget group containing 2 widgets
		int totalNumOfMetrics = getTotalNumberOfMetrics(convertedUserInterface.getMetricGroups());
		Assert.assertEquals("Expected number of widget groups was not met."
				+ " found  " + convertedUserInterface.getWidgetGroups().size()
				+ " expected " + totalNumOfMetrics 
				, convertedUserInterface.getWidgetGroups().size(), totalNumOfMetrics);

		for (org.openspaces.ui.WidgetGroup widgetGroup : convertedUserInterface.getWidgetGroups()) {
			//each metric should have 2 widgets. assert name as-well
			Assert.assertEquals("Expected number of widgets per widget group was not met."
					+ widgetGroup.getName() + " group has " + widgetGroup.getWidgets().size()
					+ "widgets, expecting " + DEFAULT_NUM_OF_WIDGETS
					, widgetGroup.getWidgets().size(), DEFAULT_NUM_OF_WIDGETS);
		}

		//extract to method test overrides by metric def
		assertOverriddenMetricAxisYUnitAndDefaultValues(convertedUserInterface);
		
		for (org.openspaces.ui.MetricGroup metricGroup : convertedUserInterface.getMetricGroups()) {
			for (Object metric : metricGroup.getMetrics()) {
				Assert.assertTrue("Not all metrices in the converted UserInterface object are null", 
						metric instanceof String);
			}
		}
	}

	/**
	 * 	Since no widgets were defined in the UserInterface POJO,
	 *	default ones should be created.
	 * @throws Exception
	 */
	@Test
	public void testConversionWithExistingWidgets()
			throws Exception {
		Service widgetlessServiceFile = ServiceReader.readService(new File(SERVICE_WITH_WIDGETS_FILE_PATH));
		
		org.openspaces.ui.UserInterface convertedUserInterface = getConvertedObject(widgetlessServiceFile);

		//assert all the groups in the groovy are accounted for.
		Assert.assertEquals("Expected number of metrics was not met." 
				+ " Found " + convertedUserInterface.getMetricGroups().size()
				+ " expecting " + NUMBER_OF_METRIC_GROUPS_AS_DEFINED_IN_GROOVY,
				convertedUserInterface.getMetricGroups().size(), NUMBER_OF_METRIC_GROUPS_AS_DEFINED_IN_GROOVY);
		
		//assert the original number of widgets defined was not overridden by the default.
		Assert.assertEquals("Expected number of widget groups was not met. " 
				+ " Found " + convertedUserInterface.getWidgetGroups().size() 
				+ " expecting " + NOMBER_OF_NON_DEFAULT_WIDGET_GROUPS,
				convertedUserInterface.getWidgetGroups().size(), NOMBER_OF_NON_DEFAULT_WIDGET_GROUPS);
		
		//assert metric override via metrics was not used.
		assertMetricOverrideNotUsed(convertedUserInterface);
		
		for (org.openspaces.ui.MetricGroup metricGroup : convertedUserInterface.getMetricGroups()) {
			for (Object metric : metricGroup.getMetrics()) {
				Assert.assertTrue("Not all metrices in the converted UserInterface object are null", 
						metric instanceof String);
			}
		}
	}
	
	private org.openspaces.ui.UserInterface getConvertedObject(final Service wigetlessServiceFile) 
			throws IllegalAccessException, InvocationTargetException, UnsupportedDataTypeException {
//		final UserInterface ui = wigetlessServiceFile.getUserInterface();
//		final OpenspacesDomainAdapter adapter = new OpenspacesDomainAdapter();
//		//convert to openspaces ui object
//		org.openspaces.ui.UserInterface convertUserInterfaceObject = adapter.createOpenspacesUIObject(ui);
//		//convert to allow default ui option
		UserInterfaceConverter converter = new UserInterfaceConverter();
		return converter.convertUserInterface(wigetlessServiceFile.getUserInterface());
	}
	
	private void assertMetricOverrideNotUsed(
			final org.openspaces.ui.UserInterface convertedUserInterface) {
		for (org.openspaces.ui.WidgetGroup widgetGroup : convertedUserInterface.getWidgetGroups()) {
			String widgetGroupName = widgetGroup.getName();
			for (org.openspaces.ui.Widget widget : widgetGroup.getWidgets()) {

				if (widget instanceof BarLineChart) {
					if (widgetGroupName.equals(EXISTING_WIDGET_TESTING_GROUP_NAME)) {
						//assert axis was not overridden
						Assert.assertTrue("expecting axisY Unit not to be overridden",
								((BarLineChart) widget).getAxisYUnit().equals(Unit.REGULAR));
					} 
				} 
			}
		}
	}

	private void assertOverriddenMetricAxisYUnitAndDefaultValues(
			final org.openspaces.ui.UserInterface convertedUserInterface) {
		for (org.openspaces.ui.WidgetGroup widgetGroup : convertedUserInterface.getWidgetGroups()) {
			String widgetGroupName = widgetGroup.getName();
			for (org.openspaces.ui.Widget widget : widgetGroup.getWidgets()) {

				if (widget instanceof BarLineChart) {
					if (widgetGroupName.equals(OVERRIDDEN_MATRIC_AXISY_WIDGET_GROUP_NAME)) {
						//assert axis was overridden
						Assert.assertTrue("expecting axisY Unit to be overridden",
								((BarLineChart) widget).getAxisYUnit().equals(Unit.PERCENTAGE));
					} else {
						//The default axisY unit
						Assert.assertTrue("expecting axisY contain the default axisYUnit value",
								((BarLineChart) widget).getAxisYUnit().equals(Unit.REGULAR));
					}
				}
			}
		}
	}

	private int getTotalNumberOfMetrics(final List<org.openspaces.ui.MetricGroup> list) {
		int totalNumberOfMetrics = 0;
		for (org.openspaces.ui.MetricGroup metricGroup : list) {
			totalNumberOfMetrics += metricGroup.getMetrics().size();
		}
		return totalNumberOfMetrics;
	}
}
