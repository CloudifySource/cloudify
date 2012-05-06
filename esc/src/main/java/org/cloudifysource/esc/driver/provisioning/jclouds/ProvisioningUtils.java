/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.jclouds;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * @author rafi
 * @since 2.0.0
 */
public class ProvisioningUtils {


    public static Set<Integer> delimitedStringToSet(
            final String componentInstanceIDs) throws NumberFormatException {
        final String[] delimited = componentInstanceIDs.split(",");
        final Set<Integer> intSet = new HashSet<Integer>();
        for (final String str : delimited) {
            intSet.add(Integer.valueOf(str));
        }
        return intSet;
    }


    
    public static long millisUntil(String errorMessage, long end)
            throws TimeoutException {
        long millisUntilEnd = end - System.currentTimeMillis();
        if (millisUntilEnd < 0) {
            throw new TimeoutException(errorMessage);
        }
        return millisUntilEnd;
    }


    public static Map<String, String> convertBeanToMap(Object bean) {
		final Map<String, String> result = new HashMap<String, String>();
		try {
			populateMapFromBean(result, bean, "");
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to convert Bean to Map: " + e.getMessage(), e);
		}
		return result;

	}

	@SuppressWarnings("unchecked")
	private static void populateMapFromBean(Map<String, String> map, Object bean,
			String keyPrefix) throws IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {

		if(bean == null) {
			return;
		}
		if (List.class.isAssignableFrom(bean.getClass())) {
			List<Object> list = (List<Object>) bean;
			int index = 0;
			for (Object object : list) {
				populateMapFromBean(map, object, keyPrefix + "." + index);
				++index;
			}
		} else if (Map.class.isAssignableFrom(bean.getClass())) {
			Map<String, Object> propertyMap = (Map<String, Object>) bean;
			Set<Entry<String, Object>> entries = propertyMap.entrySet();
			for (Entry<String, Object> propertyEntry : entries) {
				populateMapFromBean(map, propertyEntry.getValue(), keyPrefix
						+ "." + propertyEntry.getKey());
			}
		} else {
			if (org.springframework.beans.BeanUtils.isSimpleProperty(bean
					.getClass())) {
				if(!Class.class.isAssignableFrom(bean.getClass())) {
					map.put(keyPrefix, bean.toString());
				}
			} else {
				Map<String, Object> propertyMap = PropertyUtils.describe(bean);
				Set<Entry<String, Object>> entries = propertyMap.entrySet();
				for (Entry<String, Object> entry : entries) {
					if (keyPrefix.isEmpty()) {
						populateMapFromBean(map, entry.getValue(),
								entry.getKey());
					} else {
						populateMapFromBean(map, entry.getValue(), keyPrefix
								+ "." + entry.getKey());
					}
				}

			}

		}

	}

    

}
