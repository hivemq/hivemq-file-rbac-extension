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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

import static org.junit.Assert.*;


public class ConfigurationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Test
    public void test_create_file_before_it_does_not_exit() throws Exception {

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setReloadInterval(1);
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        final Configuration configuration = new Configuration(temporaryFolder.getRoot(), scheduledExecutorService,
                extensionConfig);

        configuration.init();

        final CountDownLatch latch = new CountDownLatch(1);
        configuration.addReloadCallback(new Configuration.ReloadCallback() {
            @Override
            public void onReload(@Nullable final FileAuthConfig oldConfig, @NotNull final FileAuthConfig newConfig) {
                latch.countDown();
            }
        });

        //Create a new file
        createCredentialsConfig();


        //Check if reload was called
        assertTrue(latch.await(30, TimeUnit.SECONDS));

        assertNotNull(configuration.getCurrentConfig());

        scheduledExecutorService.shutdown();
    }

    @Test
    public void test_reload_invalid_config() throws Exception {

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setReloadInterval(1);
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        final Configuration configuration = new Configuration(temporaryFolder.getRoot(), scheduledExecutorService,
                extensionConfig);

        //Create a new file
        createCredentialsConfig();

        configuration.init();

        final File configFile = new File(temporaryFolder.getRoot(), Configuration.CONFIG_NAME);
        configFile.delete();

        final CountDownLatch latch = new CountDownLatch(1);
        configuration.addReloadCallback(new Configuration.ReloadCallback() {
            @Override
            public void onReload(@Nullable final FileAuthConfig oldConfig, @NotNull final FileAuthConfig newConfig) {
                latch.countDown();
            }
        });

        //Check if reload was called
        assertFalse(latch.await(5, TimeUnit.SECONDS));

        assertNotNull(configuration.getCurrentConfig());

        scheduledExecutorService.shutdown();
    }

    @Test
    public void test_init() throws Exception {

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setReloadInterval(1);
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        final Configuration configuration = new Configuration(temporaryFolder.getRoot(), scheduledExecutorService,
                extensionConfig);

        createCredentialsConfig();

        configuration.init();

        assertNotNull(configuration.getCurrentConfig());

        scheduledExecutorService.shutdown();
    }

    private void createCredentialsConfig() throws URISyntaxException, IOException {
        //Create a new file
        final File configFile = new File(temporaryFolder.getRoot(), Configuration.CONFIG_NAME);

        //copy config
        final URL resource = this.getClass().getClassLoader().getResource("credentials.xml");
        final File file = new File(resource.toURI());
        Files.copy(file.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }


}