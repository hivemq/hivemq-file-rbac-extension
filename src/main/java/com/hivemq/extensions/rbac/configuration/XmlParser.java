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
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.configuration.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;


@ThreadSafe
class XmlParser {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(XmlParser.class);

    //jaxb context is thread safe
    private final @NotNull JAXBContext jaxb;

    XmlParser() {
        try {
            jaxb = JAXBContext.newInstance(FileAuthConfig.class, User.class, ExtensionConfig.class);
        } catch (final JAXBException e) {
            LOG.error("Error in the File Auth Extension. Could not initialize XML parser", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Marshals a Config to a XML file.
     * <p>
     * The file must not exist, otherwise this method will fail
     *
     * @param config the config to write to XML
     * @param file   the file where the XML should be written to
     * @throws NotMarshallableException if the XML cannot be marshalled
     */
    void marshal(final @NotNull FileAuthConfig config, final @NotNull File file)
            throws NotMarshallableException {
        if (file.isDirectory()) {
            throw new NotMarshallableException("Could not write config to file " +
                    file.getAbsolutePath() +
                    " because it's a directory");
        }
        if (file.exists()) {
            throw new NotMarshallableException("File " + file.getAbsolutePath() + " already exists");
        }
        if (file.canWrite()) {
            throw new NotMarshallableException("Could not write config to file " +
                    file.getAbsolutePath() +
                    " because it's not writable");
        }

        try {
            final Marshaller marshaller = jaxb.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(config, file);
        } catch (final JAXBException e) {
            throw new NotMarshallableException("Could not write config to file " + file.getAbsolutePath(), e);
        }
    }

    @NotNull FileAuthConfig unmarshalFileAuthConfig(final @NotNull File file) throws IOException {
        try {
            final Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            return (FileAuthConfig) unmarshaller.unmarshal(file);
        } catch (final JAXBException e) {
            throw new IOException(e);
        }
    }

    @NotNull ExtensionConfig unmarshalExtensionConfig(final @NotNull File file) throws IOException {
        try {
            final Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            return (ExtensionConfig) unmarshaller.unmarshal(file);
        } catch (final JAXBException e) {
            throw new IOException(e);
        }
    }
}
