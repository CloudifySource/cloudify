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
package org.cloudifysource.esc;
/**********************************
 * Top level package for the esm-cloud project.
 * 
 * This project implements the GigaSpaces ESM Machine Provisioning interface for 
 * various clouds, using the jclouds framework.
 * 
 * The project includes the following packages:
 * 
 * - org.cloudifysource.esc.esm: The implementation of the ESM machine provisioning interface and
 * supporting classes.
 * - org.cloudifysource.esc.jclouds: Service facade around the jclouds framework, exposing only
 * the functionality used to scale in and out as required by the ESM.
 * - org.cloudifysource.esc.jclouds: Examples of usage of this project in various clouds and
 * use cases. (This package should not be deployed with production code, as it is not used,
 * directly or indirectly, by the ESM provisioning implementation.
 * 
 */

