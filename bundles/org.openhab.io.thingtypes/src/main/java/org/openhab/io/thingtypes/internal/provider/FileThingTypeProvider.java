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
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ThingType;
import org.openhab.io.thingtypes.internal.copied.ThingTypeXmlResult;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.converters.ConversionException;

/**
 * Provides {@link ThingType}s parsed from files.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ThingTypeProvider.class, FileThingTypeProvider.class }, property = { "openhab.scope=core.xml.thing" })
public class FileThingTypeProvider implements ThingTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(FileThingTypeProvider.class);

    // All access must be guarded by "this"
    private final Map<Path, Set<ThingType>> referenceMap = new HashMap<>();

    // All access must be guarded by "this"
    private final Set<ThingType> thingTypes = new HashSet<>();

    /**
     * Add the specified parsing result from the specified file.
     *
     * @param path the file from which the parsing result originates.
     * @param xmlResult the parsing result to add.
     */
    public void add(Path path, ThingTypeXmlResult xmlResult) {
        ThingType thingType;
        try {
            thingType = xmlResult.toThingType();
        } catch (ConversionException e) {
            logger.warn("Failed to create ThingType from \"{}\": {}", path.toAbsolutePath(), e.getMessage());
            logger.trace("", e);
            return;
        }
        synchronized (this) {
            Set<ThingType> pathSet = Objects.requireNonNull(referenceMap.computeIfAbsent(path, p -> new HashSet<ThingType>()));
            pathSet.add(thingType);
            thingTypes.add(thingType);
        }
    }

    /**
     * Remove all parsing results associated with the specified file.
     *
     * @param path the file whose parsing results to remove.
     */
    public synchronized void remove(Path path) {
        Set<ThingType> toRemove = referenceMap.remove(path);
        if (toRemove != null) {
            thingTypes.removeAll(toRemove);
        }
    }

    @Override
    public synchronized Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        return Set.copyOf(thingTypes);
    }

    @Override
    public synchronized @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        for (ThingType thingType : thingTypes) {
            if (thingTypeUID.equals(thingType.getUID())) {
                return thingType;
            }
        }
        return null;
    }
}
