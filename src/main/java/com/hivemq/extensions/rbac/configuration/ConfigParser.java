/*
 *
 * Copyright 2019 HiveMQ GmbH
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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

class ConfigParser {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(ConfigParser.class);

    private final @NotNull XmlParser xmlParser = new XmlParser();
    private final @NotNull ExtensionConfig extensionConfig;

    ConfigParser(final @NotNull ExtensionConfig extensionConfig) {
        this.extensionConfig = extensionConfig;
    }

    /**
     * @param file the new config file to read.
     * @return the new config based on the file contents or null if the config is invalid
     */
    @Nullable FileAuthConfig read(final @NotNull File file) {
        if (!file.canRead()) {
            LOG.error("Unable to read configuration file {}", file.getAbsolutePath());
            return null;
        }

        try {
            final FileAuthConfig config = xmlParser.unmarshalFileAuthConfig(file);
            final ConfigCredentialsValidator.ValidationResult validationResult =
                    ConfigCredentialsValidator.validateConfig(extensionConfig, config);
            if (validationResult.isValidationSuccessful()) {
                return config;
            }

            logConfigFileErrors(validationResult);
            return null;
        } catch (final IOException e) {
            LOG.error("Could not read configuration file, reason: {}", e.getMessage());
            return null;
        }
    }

    private void logConfigFileErrors(final ConfigCredentialsValidator.ValidationResult validationResult) {
        final StringBuilder errorMessage = new StringBuilder();
        for (final String error : validationResult.getErrors()) {
            errorMessage.append("\n").append("\t- ").append(error);
        }
        LOG.warn("Configuration for file auth extension has errors: {}", errorMessage);
    }
}
