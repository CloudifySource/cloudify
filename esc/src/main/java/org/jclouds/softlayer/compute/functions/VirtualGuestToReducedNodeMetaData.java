
package org.jclouds.softlayer.compute.functions;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import org.jclouds.collect.Memoized;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.functions.GroupNamingConvention;
import org.jclouds.domain.Location;
import org.jclouds.softlayer.domain.VirtualGuest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

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

    @Inject
    VirtualGuestToReducedNodeMetaData(
            @Memoized Supplier<Set<? extends Location>> locations,
            GetHardwareForVirtualGuest hardware,
            GetImageForVirtualGuest images, GroupNamingConvention.Factory namingConvention) {
        super(locations, hardware, images, namingConvention);
    }

    @Override
    public NodeMetadata apply(final VirtualGuest from) {

        // convert the result object to a jclouds NodeMetadata
        NodeMetadataBuilder builder = new NodeMetadataBuilder();
        builder.ids(from.getId() + "");
        builder.name(from.getHostname());
        builder.hostname(from.getHostname());
        builder.status(serverStateToNodeStatus.get(from.getPowerState().getKeyName()));

        // These are null for 'bad' guest orders in the HALTED state.
        if (from.getPrimaryIpAddress() != null)
            builder.publicAddresses(ImmutableSet.<String> of(from.getPrimaryIpAddress()));
        if (from.getPrimaryBackendIpAddress() != null)
            builder.privateAddresses(ImmutableSet.<String> of(from.getPrimaryBackendIpAddress()));
        return builder.build();
    }
}
