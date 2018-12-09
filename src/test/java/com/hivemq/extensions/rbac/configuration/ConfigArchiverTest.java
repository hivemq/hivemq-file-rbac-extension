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

import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.configuration.entities.Permission;
import com.hivemq.extensions.rbac.configuration.entities.Role;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigArchiverTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File folder;
    private ConfigArchiver configArchiver;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        folder = temporaryFolder.newFolder();

        configArchiver = new ConfigArchiver(folder, new XmlParser());
    }

    @Test
    public void test_archive() throws Exception {

        final FileAuthConfig config = new FileAuthConfig();

        config.setRoles(List.of(new Role("id1", List.of(new Permission("topic1")))));

        configArchiver.archive(config);

        final File[] files = folder.listFiles();
        assertEquals(1, files.length);

        final File archiveFolder = files[0];
        assertEquals("credentials-archive", archiveFolder.getName());
        assertEquals(true, archiveFolder.isDirectory());

        assertEquals(1, archiveFolder.listFiles().length);
        System.out.println(archiveFolder.listFiles()[0].getName());
        assertTrue(archiveFolder.listFiles()[0].getName().startsWith("20"));
        assertTrue(archiveFolder.listFiles()[0].getName().endsWith("credentials.xml"));

    }

}