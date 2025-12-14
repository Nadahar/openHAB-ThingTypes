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
package org.openhab.io.thingtypes.internal.util;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Add-on utility methods.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class ThingTypesUtil {

    /**
     * Not to be instantiated.
     */
    private ThingTypesUtil() {
    }

    /**
     * Returns the index of the last extension separator ({@code .}) that isn't followed by a path separator
     * of the specified path.
     *
     * @param path the {@link String} to examine.
     * @return The index or {@code -1} if none was found.
     */
    public static int getExtensionIndex(@Nullable String path) {
        if (path == null || path.length() < 2) {
            return -1;
        }
        char[] filePathArray = path.toCharArray();
        for (int i = filePathArray.length - 1; i >= 0; i--) {
            switch (filePathArray[i]) {
                case '.':
                    return i == filePathArray.length - 1 ? -1 : i;
                case '/':
                case '\\':
                    return -1;
            }
        }
        return -1;
    }

    /**
     * Returns the extension of the specified path or {@code null} if none is found. The extension is the
     * string that follows the last extension separator ({@code .}) in the string that isn't followed by a
     * path separator.
     *
     * @param path the {@link String} to extract the extension from.
     * @param lowerCase {@code true} to convert the extension to lower case.
     * @return The extension or {@code null}.
     */
    @Nullable
    public static String getExtension(@Nullable String path, boolean lowerCase) {
        if (path == null || path.isBlank()) {
            return null;
        }

        int point = getExtensionIndex(path);
        if (point == -1) {
            return null;
        }

        String extension = path.substring(point + 1).trim();
        return lowerCase ? extension.toLowerCase(Locale.ROOT) : extension;
    }

    /**
     * Returns the filename part of the specified path, with or without extension.
     *
     * @param path the path from which to extract the filename.
     * @param stripExtension {@code true} to strip the extension from returned filename.
     * @return The resulting filename, possibly an empty string.
     */
    public static String getFilename(@Nullable String path, boolean stripExtension) {
        if (path == null || path.isBlank()) {
            return "";
        }
        char[] pathArray = path.toCharArray();
        int idx = -1;
        for (int i = pathArray.length - 1; i >= 0; i--) {
            if (pathArray[i] == '/' || pathArray[i] == '\\') {
                idx = i;
                break;
            }
        }
        if (idx == path.length() - 1) {
            return "";
        }
        String filename = idx < 0 ? path : path.substring(idx + 1);
        if (!stripExtension) {
            return filename;
        }
        idx = getExtensionIndex(filename);
        return idx < 0 ? filename : filename.substring(0, idx);
    }

    /**
     * Evaluates if the specified character sequence is {@code null}, empty or
     * only consists of whitespace.
     *
     * @param cs the {@link CharSequence} to evaluate.
     * @return {@code false} if {@code cs} is {@code null}, empty or only consists of
     *         whitespace, {@code true} otherwise.
     */
    public static boolean isNotBlank(@Nullable CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Evaluates if the specified character sequence is {@code null}, empty or
     * only consists of whitespace.
     *
     * @param cs the {@link CharSequence} to evaluate.
     * @return {@code true} if {@code cs} is {@code null}, empty or only
     *         consists of whitespace, {@code false} otherwise.
     */
    public static boolean isBlank(@Nullable CharSequence cs) {
        if (cs == null) {
            return true;
        }
        int strLen = cs.length();
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
