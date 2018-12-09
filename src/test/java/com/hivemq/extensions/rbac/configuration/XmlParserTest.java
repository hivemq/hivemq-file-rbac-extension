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

import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.configuration.entities.PasswordType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertEquals;

public class XmlParserTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = NotMarshallableException.class)
    public void test_marshal_directory() throws Exception {

        final XmlParser xmlParser = new XmlParser();
        final File file = temporaryFolder.newFolder();

        xmlParser.marshal(new FileAuthConfig(), file);
    }

    @Test(expected = NotMarshallableException.class)
    public void test_marshal_file_exists() throws Exception {

        final XmlParser xmlParser = new XmlParser();
        final File file = temporaryFolder.newFile();

        xmlParser.marshal(new FileAuthConfig(), file);
    }

    @Test
    public void test_marshal_unmarshal_credentials() throws Exception {

        final XmlParser xmlParser = new XmlParser();

        final URL resource = this.getClass().getClassLoader().getResource("credentials.xml");
        final File file = new File(resource.toURI());

        final FileAuthConfig config = xmlParser.unmarshalFileAuthConfig(file);

        final File testFile = new File(temporaryFolder.getRoot(), "test.xml");
        xmlParser.marshal(config, testFile);

        final FileAuthConfig marshalledConfig = xmlParser.unmarshalFileAuthConfig(testFile);

        assertEquals(config.toString().trim(), marshalledConfig.toString().trim());
    }

    @Test
    public void test_unmarshal_extension_config() throws Exception {

        final XmlParser xmlParser = new XmlParser();

        final URL resource = this.getClass().getClassLoader().getResource("test-extension-config.xml");
        final File file = new File(resource.toURI());

        final ExtensionConfig config = xmlParser.unmarshalExtensionConfig(file);

        assertEquals(PasswordType.HASHED, config.getPasswordType());
        assertEquals(120, config.getReloadInterval());
    }

}