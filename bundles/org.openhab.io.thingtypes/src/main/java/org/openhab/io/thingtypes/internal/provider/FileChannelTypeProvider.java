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
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeProvider;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.io.thingtypes.internal.copied.ChannelTypeXmlResult;
import org.osgi.service.component.annotations.Component;

/**
 * Provides {@link ChannelType}s parsed from files.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ChannelTypeProvider.class, FileChannelTypeProvider.class }, property = { "openhab.scope=core.xml.channels" })
public class FileChannelTypeProvider implements ChannelTypeProvider {

    // All access must be guarded by "this"
    private final Map<Path, Set<ChannelType>> referenceMap = new HashMap<>();

    // All access must be guarded by "this"
    private final Set<ChannelType> channelTypes = new HashSet<>();

    /**
     * Add the specified parsing result from the specified file.
     *
     * @param path the file from which the parsing result originates.
     * @param xmlResult the parsing result to add.
     */
    public void add(Path path, ChannelTypeXmlResult xmlResult) {
        ChannelType channelType = xmlResult.toChannelType();
        synchronized (this) {
            Set<ChannelType> pathSet = Objects.requireNonNull(referenceMap.computeIfAbsent(path, p -> new HashSet<ChannelType>()));
            pathSet.add(channelType);
            channelTypes.add(channelType);
        }
    }

    /**
     * Remove all parsing results associated with the specified file.
     *
     * @param path the file whose parsing results to remove.
     */
    public synchronized void remove(Path path) {
        Set<ChannelType> toRemove = referenceMap.remove(path);
        if (toRemove != null) {
            channelTypes.removeAll(toRemove);
        }
    }

    @Override
    public synchronized Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        return Set.copyOf(channelTypes);
    }

    @Override
    public synchronized @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        for (ChannelType channelType : channelTypes) {
            if (channelTypeUID.equals(channelType.getUID())) {
                return channelType;
            }
        }
        return null;
    }
}
