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
package org.openhab.io.thingtypes.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.xml.util.XmlDocumentReader;
import org.openhab.core.service.WatchService;
import org.openhab.core.service.WatchService.Kind;
import org.openhab.core.service.WatchService.WatchEventListener;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ThingType;
import org.openhab.io.thingtypes.internal.copied.ChannelGroupTypeXmlResult;
import org.openhab.io.thingtypes.internal.copied.ChannelTypeXmlResult;
import org.openhab.io.thingtypes.internal.copied.ThingDescriptionReader;
import org.openhab.io.thingtypes.internal.copied.ThingTypeXmlResult;
import org.openhab.io.thingtypes.internal.provider.FileChannelGroupTypeProvider;
import org.openhab.io.thingtypes.internal.provider.FileChannelTypeProvider;
import org.openhab.io.thingtypes.internal.provider.FileThingTypeProvider;
import org.openhab.io.thingtypes.internal.util.ThingTypesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses files from the dedicated configuration folder into {@link ThingType}s, {@link ChannelType}s and
 * {@link ChannelGroupType}s.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class FileProcessor implements WatchEventListener {

    /** The configuration subfolder name */
    public static final String FOLDER_NAME = "thingtypes";

    /** The supported file extensions in lower case */
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of("xml");
    private final Logger logger = LoggerFactory.getLogger(FileProcessor.class);
    private final XmlDocumentReader<List<?>> thingTypeReader;
    private final WatchService watchService;

    private final FileThingTypeProvider fileThingTypeProvider;
    private final FileChannelTypeProvider fileChannelTypeProvider;
    private final FileChannelGroupTypeProvider fileChannelGroupTypeProvider;

    @Activate
    public FileProcessor(
        @Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService,
        @Reference FileThingTypeProvider fileThingTypeProvider,
        @Reference FileChannelTypeProvider fileChannelTypeProvider,
        @Reference FileChannelGroupTypeProvider fileChannelGroupTypeProvider
    ) {
        this.watchService = watchService;
        this.fileThingTypeProvider = fileThingTypeProvider;
        this.fileChannelTypeProvider = fileChannelTypeProvider;
        this.fileChannelGroupTypeProvider = fileChannelGroupTypeProvider;
        this.thingTypeReader = new ThingDescriptionReader();
    }

    @Activate
    public void activate() {
      watchService.registerListener(this, Path.of(FOLDER_NAME));
      new Thread(() -> {
          Path watchPath;
          try {
              watchPath = watchService.getWatchPath().resolve(FOLDER_NAME);
          } catch (InvalidPathException e) {
              logger.error("Can't parse thing types - invalid config path", e);
              return;
          }
          if (Files.isDirectory(watchPath)) {
              try {
                  Files.walkFileTree(watchPath, new SimpleFileVisitor<>() {

                      @Override
                      public FileVisitResult visitFile(@NonNullByDefault({}) Path file,
                              @NonNullByDefault({}) BasicFileAttributes attrs) throws IOException {
                          if (attrs.isRegularFile()) {
                              Path filename = file.getFileName();
                              String extension;
                              if (
                                  filename != null &&
                                  (extension = ThingTypesUtil.getExtension(filename.toString(), true)) != null &&
                                  SUPPORTED_EXTENSIONS.contains(extension)
                              ) {
                                  add(file);
                              }
                          }
                          return FileVisitResult.CONTINUE;
                      }

                      @Override
                      public FileVisitResult visitFileFailed(@NonNullByDefault({}) Path file,
                              @NonNullByDefault({}) IOException exc) throws IOException {
                          logger.warn("Failed to process \"{}\": {} - {}", file.toAbsolutePath(), exc.getClass().getSimpleName(), exc.getMessage());
                          return FileVisitResult.CONTINUE;
                      }
                  });
            } catch (IOException e) {
                logger.warn("Could not process ThingType files in \"{}\": {}", watchPath.toAbsolutePath(), e.getMessage());
                logger.trace("", e);
            }
          }
      }, "OH-file-thing-types-initializer").start();
    }

    @Deactivate
    public void deactivate() {
        this.watchService.unregisterListener(this);
    }

    @Override
    public void processWatchEvent(Kind kind, Path path) {
        Path filename = path.getFileName();
        String extension;
        if (
            filename == null ||
            (extension = ThingTypesUtil.getExtension(filename.toString(), true)) == null ||
            !SUPPORTED_EXTENSIONS.contains(extension)
        ) {
            return;
        }

        Path filePath;
        if (path.isAbsolute()) {
            filePath = path;
        } else {
            try {
                filePath = watchService.getWatchPath().resolve(FOLDER_NAME).resolve(path);
            } catch (InvalidPathException e) {
                logger.error("Can't parse thing types - invalid config path", e);
                return;
            }
        }

        switch (kind) {
            case CREATE:
                logger.debug("Discovered new file: {}", filePath.toAbsolutePath());
                add(filePath);
                break;
            case DELETE:
                logger.debug("Discovered deleted file: {}", filePath.toAbsolutePath());
                remove(filePath);
                break;
            case MODIFY:
                logger.debug("Discovered modified file: {}", filePath.toAbsolutePath());
                remove(filePath);
                add(filePath);
                break;
            case OVERFLOW:
                logger.warn("An overflow occurred, some changes might have been missed: {}", filePath.toAbsolutePath());
                remove(filePath);
                add(filePath);
                break;
            default:
                break;
        }
    }

    /**
     * Parse a file and add the results to accompanying providers.
     *
     * @param path the file to parse.
     */
    protected void add(Path path) {
        try {
            List<?> results = thingTypeReader.readFromXML(path.toUri().toURL());
            if (results != null) {
                for (Object result : results) {
                    if (result instanceof ThingTypeXmlResult xmlResult) {
                        fileThingTypeProvider.add(path, xmlResult);
                    } else if (result instanceof ChannelTypeXmlResult xmlResult) {
                        fileChannelTypeProvider.add(path, xmlResult);
                    } else if (result instanceof ChannelGroupTypeXmlResult xmlResult) {
                        fileChannelGroupTypeProvider.add(path, xmlResult);
                    } else {
                        logger.warn("Ignoring unexpected result type {}: {}", result.getClass().getSimpleName(), result);
                    }
                }
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Parsed \"{}\": {}", path, results);
            } else {
                logger.debug("Parsed \"{}\"", path);
            }
        } catch (MalformedURLException e) {
            logger.warn("Malformed path URL: {}", path, e);
        } catch (RuntimeException e) {
            logger.warn("Failed to parse \"{}\": {}", path, e.getMessage());
            logger.debug("", e);
        }
    }

    /**
     * Remove all previously parsed results from the specified file from the accompanying providers.
     *
     * @param path the file whose previous parsing results to remove.
     */
    protected void remove(Path path) {
        fileChannelTypeProvider.remove(path);
        fileChannelGroupTypeProvider.remove(path);
        fileThingTypeProvider.remove(path);
    }
}
