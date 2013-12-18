/*
 * ******************************************************************************
 *  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package org.jclouds.softlayer.compute.functions.guest;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import org.jclouds.collect.Memoized;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.functions.GroupNamingConvention;
import org.jclouds.domain.Location;
import org.jclouds.softlayer.domain.guest.VirtualGuest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Overrides the default jclouds transformation to boost performance.
 * This transformation does not include location and image information in the returned NodeMetaData Object.
 *
 * Side effects:
 *
 * 1. Location based scaling will not work with softlayer cloud driver.
 *
 * @author Eli Polonsky
 * @since 2.7.0
 */


@Singleton
public class VirtualGuestToReducedNodeMetaData extends VirtualGuestToNodeMetadata {

    private final GroupNamingConvention nodeNamingConvention;

    @Inject
    VirtualGuestToReducedNodeMetaData(
            @Memoized Supplier<Set<? extends Location>> locations,
            GetHardwareForVirtualGuest hardware,
            GetImageForVirtualGuest images, GroupNamingConvention.Factory namingConvention) {
        super(locations, hardware, images, namingConvention);
        this.nodeNamingConvention = checkNotNull(namingConvention, "namingConvention").createWithoutPrefix();
    }

    @Override
    public NodeMetadata apply(final VirtualGuest from) {

        // convert the result object to a jclouds NodeMetadata
        NodeMetadataBuilder builder = new NodeMetadataBuilder();
        builder.ids(from.getId() + "");
        builder.name(from.getHostname());
        builder.hostname(from.getHostname());
        builder.status(serverStateToNodeStatus.get(from.getPowerState().getKeyName()));
        builder.group(nodeNamingConvention.groupInUniqueNameOrNull(from.getHostname()));

        // These are null for 'bad' guest orders in the HALTED state.
        if (from.getPrimaryIpAddress() != null)
            builder.publicAddresses(ImmutableSet.<String> of(from.getPrimaryIpAddress()));
        if (from.getPrimaryBackendIpAddress() != null)
            builder.privateAddresses(ImmutableSet.<String> of(from.getPrimaryBackendIpAddress()));
        return builder.build();
    }
}
