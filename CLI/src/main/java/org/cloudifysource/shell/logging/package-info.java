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
******************************************************************************/

/**************************
 * @author noak
 * @since 2.0.0
 * 
 * <p>
 * This package supplies custom formatting of log records and print outs to the error stream, that can be used by the 
 * shell, as configured in {@link org.cloudifysource.shell.GigaShellMain} 
 * 
 * <p>
 * {@link org.cloudifysource.shell.logging.ShellErrorManager} - An extension of {@link java.util.logging.ErrorManager},
 * prints data about exceptions to the error stream.
 * 
 * <p>
 * {@link org.cloudifysource.shell.logging.ShellFormatter} - An extension of {@link java.util.logging.SimpleFormatter},
 * supplies custom formatting for log records that refer to thrown exceptions.
 * 
 **/
package org.cloudifysource.shell.logging;