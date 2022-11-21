/*
 *
 * Copyright 2019 dc-square GmbH
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
 */
package com.hivemq.extensions.rbac.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ExtensionConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConfigParser.class);
    public static final String EXTENSION_CONFIG_FILE_NAME = "extension-config.xml";

    private final @NotNull XmlParser xmlParser = new XmlParser();
    private final @NotNull
    ExtensionConfig extensionConfig;

    public ExtensionConfiguration(final @NotNull File extensionHomeFolder) {
        final @NotNull File extensionHomeFolder1 = extensionHomeFolder;
        extensionConfig = read(new File(extensionHomeFolder, EXTENSION_CONFIG_FILE_NAME));
    }

    @NotNull
    public ExtensionConfig getExtensionConfig() {
        return extensionConfig;
    }

    /**
     * @param file the new config file to read.
     * @return the new config based on the file contents or null if the config is invalid
     */
    @NotNull
    private ExtensionConfig read(@NotNull final File file) {

        final ExtensionConfig defaultConfig = new ExtensionConfig();
        if (!file.exists()) {
            log.warn("File auth extension configuration file {} missing, using defaults", file.getAbsolutePath());
            return defaultConfig;
        }

        if (!file.canRead()) {
            log.warn("Unable to read file auth extension configuration file {}, using defaults", file.getAbsolutePath());
            return defaultConfig;
        }

        try {
            final ExtensionConfig newExtensionConfig = xmlParser.unmarshalExtensionConfig(file);

            if (newExtensionConfig.getReloadInterval() < 1) {
                log.warn("Credentials reload interval for file auth extension must be greater than 0, using default interval " +
                        defaultConfig.getReloadInterval());
                newExtensionConfig.setReloadInterval(defaultConfig.getReloadInterval());
            }

            //noinspection ConstantConditions
            if (newExtensionConfig.getPasswordType() == null) {
                log.warn("Unknown password type file auth extension, using default type " +
                        defaultConfig.getPasswordType());
                newExtensionConfig.setPasswordType(defaultConfig.getPasswordType());
            }

            return newExtensionConfig;

        } catch (IOException e) {
            log.warn("Could not read file auth extension configuration file, reason: {}, using defaults", e.getMessage());
            return defaultConfig;
        }

    }

}
