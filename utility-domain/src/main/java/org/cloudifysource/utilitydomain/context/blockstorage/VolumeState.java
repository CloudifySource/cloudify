/*******************************************************************************
 ' * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.utilitydomain.context.blockstorage;

/**
 * Created with IntelliJ IDEA.
 *
 * Enum indicating the state of a service volume.
 *
 * User: elip
 * Date: 4/7/13
 * Time: 6:39 PM
 * To change this template use File | Settings | File Templates.
 */
public enum VolumeState {

    CREATED,
    ATTACHED,
    PARTITIONED,
    FORMATTED,
    MOUNTED
}
