/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/

package org.cloudifysource.dsl.rest.response;


/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/20/13
 * Time: 2:04 PM
 * <br></br>
 *
 * Represents a life cycle deployment event.
 */
public class DeploymentEvent {

    private String description;
    private int index;

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeploymentEvent event = (DeploymentEvent) o;

        if (index != event.index) {
            return false;
        }
        if (!description.equals(event.description)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = description.hashCode();
        result = 31 * result + index;
        return result;
    }

    @Override
    public String toString() {
        return "DeploymentEvent{" + "description='" + description + '\''
                + ", index=" + index + '}';
    }
}
