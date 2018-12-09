/*
 * Copyright 2018 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hivemq.extensions.rbac.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Collections.unmodifiableList;


@ThreadSafe
public class Configuration {

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);
    static final String CONFIG_NAME = "credentials.xml";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // COWAL is perfect here because the callbacks are not expected to change regularly
    //The callbacks are shared between this class and the reloadable config task. Modifications are only possible via this class")
    private final List<ReloadCallback> callbacks = new CopyOnWriteArrayList<>();

    //"This queue is thread safe and shared between this class and the reloadable config file task")
    private final Queue<ReloadCallback> newCallbacks = new LinkedBlockingQueue<>();
    private final @NotNull File extensionHomeFolder;
    private final @NotNull ConfigParser configParser;

    //guarded by lock
    private @Nullable FileAuthConfig config;


    public Configuration(@NotNull final File extensionHomeFolder,
                         @NotNull final ScheduledExecutorService extensionExecutorService,
                         @NotNull final ExtensionConfig extensionConfig) {

        configParser = new ConfigParser(extensionConfig);
        this.extensionHomeFolder = extensionHomeFolder;
        final ReloadConfigFileTask reloadableTask = new ReloadConfigFileTask(extensionHomeFolder,
                extensionExecutorService,
                unmodifiableList(callbacks) /* We don't want the task to modify the callbacks!*/,
                newCallbacks,
                configParser,
                new ConfigArchiver(extensionHomeFolder, new XmlParser()), this);
        extensionExecutorService.scheduleWithFixedDelay(reloadableTask, extensionConfig.getReloadInterval(),
                extensionConfig.getReloadInterval(), TimeUnit.SECONDS);
    }

    public void init() {

        config = configParser.read(getConfigFile(extensionHomeFolder));

        if (config == null) {
            log.warn("No credentials configuration file for file auth extension available, denying all connections.");
        }

        addReloadCallback(new ReloadCallback() {
            @Override
            public void onReload(@Nullable final FileAuthConfig oldConfig, @NotNull final FileAuthConfig newConfig) {
                final Lock writeLock = lock.writeLock();
                writeLock.lock();
                try {
                    config = newConfig;
                } finally {
                    writeLock.unlock();
                }
            }
        });
    }

    @Nullable
    public FileAuthConfig getCurrentConfig() {
        final Lock readLock = lock.readLock();
        readLock.lock();
        try {
            return config;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Adds a reload callback.
     */
    public void addReloadCallback(@NotNull final ReloadCallback callback) {
        callbacks.add(callback);
        newCallbacks.add(callback);
    }


    /**
     * A callback that gets triggered every time the config file changes.
     * <p>
     * A callback is guaranteed to not get executed concurrently.
     * <p>
     * All callbacks are executed in a dedicated executor. Callbacks are not executed concurrently,
     * so make sure they don't block for too long
     */
    public interface ReloadCallback {

        /**
         * An callback that is called when the config changes
         *
         * @param oldConfig the old config
         * @param newConfig the new config
         */
        void onReload(@Nullable FileAuthConfig oldConfig, @NotNull FileAuthConfig newConfig);

    }

    private static class ReloadConfigFileTask implements Runnable {

        private final @NotNull ExecutorService callbackExecutor;
        private final @NotNull ConfigArchiver configArchiver;
        private final @NotNull ConfigParser configParser;
        private final @NotNull File configFile;
        private final Configuration configuration;

        private final @NotNull List<ReloadCallback> callbacks;
        private final @NotNull Queue<ReloadCallback> newCallbacks;
        private @Nullable FileAuthConfig oldConfig;
        private long lastReadTimestamp = 0;

        ReloadConfigFileTask(@NotNull final File extensionHomeFolder,
                             @NotNull final ExecutorService callbackExecutor,
                             @NotNull final List<ReloadCallback> callbacks,
                             @NotNull final Queue<ReloadCallback> newCallbacks,
                             @NotNull final ConfigParser configParser,
                             @NotNull final ConfigArchiver configArchiver,
                             @NotNull final Configuration configuration) {

            this.callbacks = callbacks;
            this.newCallbacks = newCallbacks;
            this.configParser = configParser;
            this.callbackExecutor = callbackExecutor;
            this.configArchiver = configArchiver;

            configFile = getConfigFile(extensionHomeFolder);
            this.configuration = configuration;
            lastReadTimestamp = System.currentTimeMillis();
            oldConfig = configParser.read(configFile);
        }

        @Override
        public void run() {

            if (!configFile.exists()) {
                log.debug("No credentials file for file auth extension {} available, not reloading configuration for now", configFile.getAbsolutePath());
                return;
            }

            final long lastModification = configFile.lastModified();
            if (configuration.getCurrentConfig() != null && lastReadTimestamp >= lastModification) {
                log.trace("Checked for changes for file {}. No changes since {}", configFile.getAbsolutePath(), lastModification);
                return;
            }

            log.debug("Credentials for file auth extension changed, checking new credentials file.", configFile.getAbsolutePath());
            final FileAuthConfig newConfig = configParser.read(configFile);

            lastReadTimestamp = System.currentTimeMillis();

            if (newConfig == null) {
                //No changes or invalid new config
                return;
            }

            log.info("Credentials configuration for file auth extension changed, using new configuration.");
            try {
                configArchiver.archive(oldConfig);
            } catch (IOException e) {
                log.warn("Archival of the old credentials config failed. Reason: {}", e.getMessage());
            }

            oldConfig = newConfig;

            for (ReloadCallback callback : callbacks) {
                callback.onReload(oldConfig, newConfig);
            }
        }
    }

    @NotNull
    private static File getConfigFile(final @NotNull File extensionHomeFolder) {
        return new File(extensionHomeFolder, CONFIG_NAME);
    }

}
