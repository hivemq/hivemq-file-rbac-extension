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
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;

@ThreadSafe
class ConfigArchiver {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(ConfigArchiver.class);

    private final @NotNull File archiveFolder;
    private final @NotNull XmlParser xmlParser;

    ConfigArchiver(
            final @NotNull File extensionHomeFolder, final @NotNull XmlParser xmlParser) {
        this.xmlParser = xmlParser;
        archiveFolder = new File(extensionHomeFolder, "credentials-archive");
    }

    /**
     * A config to the archive folder as XML
     * <p>
     * If archiving was not successful, this method will throw an IOException.
     * <p>
     * This method is thread safe
     *
     * @param config the config to archive
     * @throws IOException if something bad happened and archival was not successful
     */
    synchronized void archive(final @Nullable FileAuthConfig config) throws IOException {
        if (config == null) {
            LOG.debug("Configuration is invalid, archiving is not possible");
            return;
        }

        //If someone's nasty and creates a file that looks like a folder
        if (archiveFolder.isFile()) {
            LOG.warn("The credentials archive folder is a file, trying to delete");
            if (archiveFolder.delete()) {
                throw new IOException("Could not delete file " + archiveFolder.getAbsolutePath());
            }
        }

        //If someone deleted the folder in the meantime
        if (!archiveFolder.exists()) {
            LOG.debug("Creating credentials archive Folder");

            if (!archiveFolder.mkdir()) {
                throw new IOException("Could not create credentials archive folder " + archiveFolder.getAbsolutePath());
            } else {
                LOG.info("Created credentials Archive folder {}." + archiveFolder.getAbsolutePath());
            }
        }

        final FastDateFormat formatter = FastDateFormat.getInstance("yyyyMMdd-hh-mm-ss");
        try {
            final String dateString = formatter.format(new Date());
            final File file = new File(archiveFolder, dateString + "-credentials.xml");
            xmlParser.marshal(config, file);
            LOG.info("Archived current credentials config to {}.", file.getAbsolutePath());
        } catch (final NotMarshallableException e) {
            throw new IOException(e);
        }
    }
}
