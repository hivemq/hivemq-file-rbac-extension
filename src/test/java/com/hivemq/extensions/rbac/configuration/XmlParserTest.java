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
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.configuration.entities.PasswordType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XmlParserTest {

    private @NotNull File extensionFolder;

    @BeforeEach
    void setUp(@TempDir final @NotNull File extensionFolder) {
        this.extensionFolder = extensionFolder;
    }


    @Test
    void test_marshal_directory() {
        final XmlParser xmlParser = new XmlParser();
        assertThrows(NotMarshallableException.class, () -> xmlParser.marshal(new FileAuthConfig(), extensionFolder));
    }

    @Test
    void test_marshal_file_exists() {
        final XmlParser xmlParser = new XmlParser();
        assertThrows(NotMarshallableException.class, () -> xmlParser.marshal(new FileAuthConfig(), extensionFolder));
    }

    @Test
    void test_marshal_unmarshal_credentials() throws Exception {
        final XmlParser xmlParser = new XmlParser();
        final URL resource = this.getClass().getClassLoader().getResource("credentials.xml");
        assertNotNull(resource);
        final File file = new File(resource.toURI());
        final FileAuthConfig config = xmlParser.unmarshalFileAuthConfig(file);
        final File testFile = new File(extensionFolder, "test.xml");
        xmlParser.marshal(config, testFile);
        final FileAuthConfig marshalledConfig = xmlParser.unmarshalFileAuthConfig(testFile);
        assertEquals(config.toString().trim(), marshalledConfig.toString().trim());
    }

    @Test
    void test_unmarshal_extension_config() throws Exception {
        final XmlParser xmlParser = new XmlParser();
        final URL resource = this.getClass().getClassLoader().getResource("test-extension-config.xml");
        assertNotNull(resource);
        final File file = new File(resource.toURI());
        final ExtensionConfig config = xmlParser.unmarshalExtensionConfig(file);
        assertEquals(PasswordType.HASHED, config.getPasswordType());
        assertEquals(120, config.getReloadInterval());
    }
}
