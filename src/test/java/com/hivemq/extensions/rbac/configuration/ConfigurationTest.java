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
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationTest {

    private @NotNull File extensionFolder;

    @BeforeEach
    void setUp(@TempDir final @NotNull File extensionFolder) {
        this.extensionFolder = extensionFolder;
    }

    @Test
    void test_create_file_before_it_does_not_exit() throws Exception {
        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setReloadInterval(1);
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        final CredentialsConfiguration credentialsConfiguration =
                new CredentialsConfiguration(extensionFolder, scheduledExecutorService, extensionConfig);
        credentialsConfiguration.init();
        final CountDownLatch latch = new CountDownLatch(1);
        credentialsConfiguration.addReloadCallback((oldConfig, newConfig) -> latch.countDown());
        //Create a new file
        createCredentialsConfig();
        //Check if reload was called
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertNotNull(credentialsConfiguration.getCurrentConfig());
        scheduledExecutorService.shutdown();
    }

    @Test
    void test_reload_invalid_config() throws Exception {
        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setReloadInterval(1);
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        final CredentialsConfiguration credentialsConfiguration =
                new CredentialsConfiguration(extensionFolder, scheduledExecutorService, extensionConfig);
        //Create a new file
        createCredentialsConfig();
        credentialsConfiguration.init();
        final File configFile = new File(extensionFolder, CredentialsConfiguration.CONFIG_NAME);
        assertTrue(configFile.delete());
        final CountDownLatch latch = new CountDownLatch(1);
        credentialsConfiguration.addReloadCallback((oldConfig, newConfig) -> latch.countDown());
        //Check if reload was called
        assertFalse(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(credentialsConfiguration.getCurrentConfig());
        scheduledExecutorService.shutdown();
    }

    @Test
    void test_init() throws Exception {
        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setReloadInterval(1);
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        final CredentialsConfiguration credentialsConfiguration =
                new CredentialsConfiguration(extensionFolder, scheduledExecutorService, extensionConfig);
        createCredentialsConfig();
        credentialsConfiguration.init();
        assertNotNull(credentialsConfiguration.getCurrentConfig());
        scheduledExecutorService.shutdown();
    }

    private void createCredentialsConfig() throws URISyntaxException, IOException {
        //Create a new file
        final File configFile = new File(extensionFolder, CredentialsConfiguration.CONFIG_NAME);
        //Copy config
        final URL resource = this.getClass().getClassLoader().getResource("credentials.xml");
        assertNotNull(resource);
        final File file = new File(resource.toURI());
        Files.copy(file.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
