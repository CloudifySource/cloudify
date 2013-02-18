/*******************************************************************************
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
 *******************************************************************************/

package org.cloudifysource.esc.driver.provisioning;

import java.io.File;

/**************
 * Interface for cloud drivers that support setting custom data files for specific services.
 * 
 * @author barakme
 * @since 2.2.0
 * 
 */
public interface CustomServiceDataAware {

	/*********
	 * Sets the custom data file for the cloud driver instance of a specific service.
	 * 
	 * @param customDataFile the custom data file (may be a folder).
	 */
	void setCustomDataFile(final File customDataFile);

}
