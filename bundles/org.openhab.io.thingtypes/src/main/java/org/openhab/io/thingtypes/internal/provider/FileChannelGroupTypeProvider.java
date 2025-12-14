/*
 * Thing Type File Provider, an add-on for openHAB for providing ThingTypes from files.
 * Copyright (c) 2025 Nadahar.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.io.thingtypes.internal.provider;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.io.thingtypes.internal.copied.ChannelGroupTypeXmlResult;
import org.osgi.service.component.annotations.Component;

/**
 * Provides {@link ChannelGroupType}s parsed from files.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ChannelGroupTypeProvider.class, FileChannelGroupTypeProvider.class }, property = { "openhab.scope=core.xml.channelGroups" })
public class FileChannelGroupTypeProvider implements ChannelGroupTypeProvider {

    // All access must be guarded by "this"
    private final Map<Path, Set<ChannelGroupType>> referenceMap = new HashMap<>();

    // All access must be guarded by "this"
    private final Set<ChannelGroupType> channelGroupTypes = new HashSet<>();

    /**
     * Add the specified parsing result from the specified file.
     *
     * @param path the file from which the parsing result originates.
     * @param xmlResult the parsing result to add.
     */
    public void add(Path path, ChannelGroupTypeXmlResult xmlResult) {
        ChannelGroupType channelGroupType = xmlResult.toChannelGroupType();
        synchronized (this) {
            Set<ChannelGroupType> pathSet = Objects.requireNonNull(referenceMap.computeIfAbsent(path, p -> new HashSet<ChannelGroupType>()));
            pathSet.add(channelGroupType);
            channelGroupTypes.add(channelGroupType);
        }
    }

    /**
     * Remove all parsing results associated with the specified file.
     *
     * @param path the file whose parsing results to remove.
     */
    public synchronized void remove(Path path) {
        Set<ChannelGroupType> toRemove = referenceMap.remove(path);
        if (toRemove != null) {
            channelGroupTypes.removeAll(toRemove);
        }
    }

    @Override
    public synchronized @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
        @Nullable Locale locale) {
        for (ChannelGroupType channelGroupType : channelGroupTypes) {
            if (channelGroupTypeUID.equals(channelGroupType.getUID())) {
                return channelGroupType;
            }
        }
        return null;
    }

    @Override
    public synchronized Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return Set.copyOf(channelGroupTypes);
    }
}
