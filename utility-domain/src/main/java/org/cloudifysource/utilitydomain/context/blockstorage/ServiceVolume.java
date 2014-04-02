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

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

/**
 * Created with IntelliJ IDEA.
 *
 * Holds some required information for working with block storage volumes.
 * Since each service instance can have only one volume attached to it, we use it as the SpaceId
 *
 * User: elip
 * Date: 4/7/13
 * Time: 6:37 PM
 */
@SpaceClass
public class ServiceVolume {

    private String applicationName;
    private String serviceName;
    private VolumeState state;
    private String id;
    private String device;
    private Boolean dynamic;
    private String ip;

    public ServiceVolume() {

    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Boolean getDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public VolumeState getState() {
        return state;
    }

    public void setState(final VolumeState state) {
        this.state = state;
    }

    @SpaceId(autoGenerate = false)
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ServiceVolume{" +
                "applicationName='" + applicationName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", state=" + state +
                ", id='" + id + '\'' +
                ", ip='" + ip + '\'' +
                ", device='" + device + '\'' +
                ", dynamic=" + dynamic +
                '}';
    }
}