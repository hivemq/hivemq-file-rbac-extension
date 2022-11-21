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
package com.hivemq.extensions.rbac;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.rbac.configuration.Configuration;
import com.hivemq.extensions.rbac.configuration.ExtensionConfiguration;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.utils.CredentialsValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FileAuthMain implements ExtensionMain {

    private static final @NotNull Logger log = LoggerFactory.getLogger(FileAuthMain.class);

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {

        log.info("Starting File RBAC extension.");

        try {
            final File extensionHomeFolder = extensionStartInput.getExtensionInformation().getExtensionHomeFolder();

            final ExtensionConfiguration extensionConfiguration = new ExtensionConfiguration(extensionHomeFolder);

            final Configuration configuration = new Configuration(extensionHomeFolder,
                    Services.extensionExecutorService(),
                    extensionConfiguration.getExtensionConfig());
            configuration.init();

            final CredentialsValidator credentialsValidator = new CredentialsValidator(configuration,
                    extensionConfiguration.getExtensionConfig(),
                    Services.metricRegistry());
            credentialsValidator.init();

            final ExtensionConfig extensionConfig = extensionConfiguration.getExtensionConfig();

            Services.securityRegistry()
                    .setAuthenticatorProvider(new FileAuthenticatorProvider(credentialsValidator, extensionConfig));

        } catch (Exception e) {
            log.error("Exception thrown at extension start: ", e);
        }

    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {
        log.info("Stopping File RBAC extension.");
    }

}
