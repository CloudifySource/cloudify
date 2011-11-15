package com.gigaspaces.cloudify.rest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.openspaces.cloud.esm.CloudDSLToCloudMachineProvisioningConfig;
import org.openspaces.cloud.esm.CloudMachineProvisioningConfig;

import com.gigaspaces.cloudify.dsl.Cloud;
import com.gigaspaces.internal.utils.Assert;

@RunWith(BlockJUnit4ClassRunner.class)
public class CloudDSLToProvisioningConfigTest {
	private Cloud cloud;
	private CloudMachineProvisioningConfig converted;
	@Before
	public void before() throws Exception {
		//init cloud object
		cloud = new Cloud();
		cloud.setApiKey("ApiKey");
		cloud.setCloudifyUrl("CloudifyUrl");
		cloud.setConnectedToPrivateIp(true);
		cloud.setDedicatedManagementMachines(true);
		cloud.setHardwareId("hardwareId");
		cloud.setImageId("imageId");
		cloud.setKeyFile("keyFile");
		cloud.setKeyPair("keyPair");
		cloud.setLocalDirectory("localDirectory");
		cloud.setMachineMemoryMB(128);
		cloud.setMachineNamePrefix("machineNamePrefix");
		cloud.setManagementOnlyFiles(new ArrayList<String>());
		cloud.setNumberOfManagementMachines(0);
		cloud.setReservedMemoryCapacityPerMachineInMB(128);
		cloud.setSecurityGroup("securityGroup");
		cloud.setManagementGroup("managementGroup");
		cloud.setProvider("provider");
		cloud.setRemoteDirectory("remoteDirectory");
		cloud.setSshLoggingLevel(Level.WARNING);
		cloud.setUser("user");
		cloud.setZones(new ArrayList<String>());
		cloud.setLocationId("locationId");
		
	}
	
	@Test
	public void testConversion() throws InterruptedException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		//convert the cloud object to machine provisioning config
		converted = CloudDSLToCloudMachineProvisioningConfig.convert(cloud);
		Class<?> cloudClass = cloud.getClass();
		Method[] cloudMethods = cloudClass.getMethods();
		Class<?> machineProvisioningClass = converted.getClass();
		Method[] machineProvisioningMethods = machineProvisioningClass.getMethods();
		//checkout all equivalent getter methods and verify that they contain the same result
		for (Method cloudMethod : cloudMethods) {
			for (Method provisioningMethod : machineProvisioningMethods) {
				if (provisioningMethod.getName().equals(cloudMethod.getName()) &&
						isValidObjectGetter(cloudMethod)){
					compareObjects(cloudMethod, provisioningMethod);
				}
			}
		}
	}
	
	private void compareObjects(Method cloudMethod, Method machineProvisioningMethod) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		
		Object cloudInvokationResult = cloudMethod.invoke(cloud, (Object[])null);
		Object provisioningInvokationResult = machineProvisioningMethod.invoke(converted, (Object[])null);
		
		if (cloudMethod.getReturnType().equals(List.class)){
			compareLists(cloudInvokationResult, provisioningInvokationResult);
		}else{
			compareRegularTypes(cloudInvokationResult, provisioningInvokationResult);
		}
	}
	
	private void compareRegularTypes(Object invoke, Object invoke2) {
		System.out.println(invoke.toString() + "  " + invoke2.toString());
		Assert.isTrue(invoke.equals(invoke2));
	}

	private void compareLists(Object cloudPropObject, Object machineProvisioningPropObject) {
		@SuppressWarnings("unchecked")
		List<String> firstList = (ArrayList<String>)cloudPropObject;
		@SuppressWarnings("unchecked")
		List<String> secondList = (List<String>)machineProvisioningPropObject;
		for (String str : firstList) {
			Assert.isTrue(secondList.contains(str));
		}
		
	}

	public static boolean isValidObjectGetter(Method method) {
		String methodName = method.getName();
		return (methodName.startsWith("get") && method.getParameterTypes().length == 0 && !methodName.equals("getClass"));
	}
}
