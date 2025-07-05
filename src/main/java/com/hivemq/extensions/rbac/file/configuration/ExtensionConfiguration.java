/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.rbac.file.configuration;

import com.hivemq.extensions.rbac.file.configuration.entities.ExtensionConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.hivemq.extensions.rbac.file.ExtensionConstants.EXTENSION_CONFIG_LEGACY_LOCATION;
import static com.hivemq.extensions.rbac.file.ExtensionConstants.EXTENSION_CONFIG_LOCATION;

public class ExtensionConfiguration {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(ExtensionConfiguration.class);

    private final @NotNull XmlParser xmlParser = new XmlParser();
    private final @NotNull ExtensionConfig extensionConfig;

    public ExtensionConfiguration(final @NotNull Path extensionHome) {
        final var configResolver =
                new ConfigResolver(extensionHome, EXTENSION_CONFIG_LOCATION, EXTENSION_CONFIG_LEGACY_LOCATION);
        this.extensionConfig = read(configResolver.get());
    }

    public @NotNull ExtensionConfig getExtensionConfig() {
        return extensionConfig;
    }

    /**
     * @param file the new config file to read.
     * @return the new config based on the file contents or null if the config is invalid
     */
    private @NotNull ExtensionConfig read(final @NotNull Path file) {
        final var defaultConfig = new ExtensionConfig();
        if (!Files.exists(file)) {
            LOG.warn("File auth extension configuration file {} missing, using defaults", file);
            return defaultConfig;
        }
        if (!Files.isReadable(file)) {
            LOG.warn("Unable to read file auth extension configuration file {}, using defaults", file);
            return defaultConfig;
        }
        try {
            final var newExtensionConfig = xmlParser.unmarshalExtensionConfig(file);
            if (newExtensionConfig.getReloadInterval() < 1) {
                LOG.warn(
                        "Credentials reload interval for file auth extension must be greater than 0, using default interval " +
                                defaultConfig.getReloadInterval());
                newExtensionConfig.setReloadInterval(defaultConfig.getReloadInterval());
            }
            if (newExtensionConfig.getPasswordType() == null) {
                LOG.warn("Unknown password type file auth extension, using default type " +
                        defaultConfig.getPasswordType());
                newExtensionConfig.setPasswordType(defaultConfig.getPasswordType());
            }
            return newExtensionConfig;
        } catch (final IOException e) {
            LOG.warn("Could not read file auth extension configuration file, reason: {}, using defaults",
                    e.getMessage());
            return defaultConfig;
        }
    }
}
