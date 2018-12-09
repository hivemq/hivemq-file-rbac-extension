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

package com.hivemq.extensions.rbac.utils;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.builder.TopicPermissionBuilder;
import com.hivemq.extensions.rbac.configuration.Configuration;
import com.hivemq.extensions.rbac.configuration.entities.PasswordType;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({Builders.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*"})
public class CredentialsValidatorTest {


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CredentialsValidator validator;
    private ScheduledExecutorService scheduledExecutorService;
    private ExtensionConfig extensionConfig;
    private File homeFolder;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        initValidator(PLAIN_CREDENTIALS, false);

        mockStatic(Builders.class);
        when(Builders.topicPermission()).thenReturn(new TestTopicPermissionBuilder());
    }

    @After
    public void after() {
        scheduledExecutorService.shutdown();
    }

    @Test
    public void test_valid_roles_plain() throws Exception {
        final List<String> roles = validator.getRoles("user1", ByteBuffer.wrap("pass1".getBytes()));
        assertEquals("role1", roles.get(0));

        final List<String> roles2 = validator.getRoles("user2", ByteBuffer.wrap("pass2".getBytes()));
        assertEquals("role1", roles2.get(0));
        assertEquals("role2", roles2.get(1));
    }

    @Test
    public void test_valid_roles_hashed() throws Exception {

        initValidator(HASHED_CREDENTIALS, true);

        final List<String> roles = validator.getRoles("user1", ByteBuffer.wrap("pass1".getBytes()));
        assertNotNull(roles);
        assertEquals("role1", roles.get(0));

        final List<String> roles2 = validator.getRoles("user2", ByteBuffer.wrap("pass2".getBytes()));
        assertNotNull(roles);
        assertEquals("role1", roles2.get(0));
        assertEquals("role2", roles2.get(1));
    }

    @Test
    public void test_permissions() throws Exception {
        final List<TopicPermission> permissions = validator.getPermissions("client1", "user1", List.of("role1"));
        assertEquals(1, permissions.size());
        assertEquals("data/client1/personal", permissions.get(0).getTopicFilter());

        final List<TopicPermission> permissions2 = validator.getPermissions("client2", "user2", List.of("role2"));
        assertEquals(1, permissions2.size());
        assertEquals("user2/#", permissions2.get(0).getTopicFilter());

        final List<TopicPermission> permissions3 = validator.getPermissions("client3", "user3", List.of("role1", "role2"));
        assertEquals(2, permissions3.size());
        assertEquals("data/client3/personal", permissions3.get(0).getTopicFilter());
        assertEquals("user3/#", permissions3.get(1).getTopicFilter());
    }

    @Test
    public void test_invalid_roles() throws Exception {
        final List<String> roles = validator.getRoles("user1", ByteBuffer.wrap("pass2".getBytes()));
        assertNull(roles);

        final List<String> roles2 = validator.getRoles("user2", ByteBuffer.wrap("pass1".getBytes()));
        assertNull(roles2);

        final List<String> roles3 = validator.getRoles("user3", ByteBuffer.wrap("pass3".getBytes()));
        assertNull(roles3);
    }

    @Test
    public void test_invalid_config() throws Exception {

        initValidator("", false);

        final List<String> roles = validator.getRoles("user1", ByteBuffer.wrap("pass1".getBytes()));
        assertNull(roles);

        final List<String> roles2 = validator.getRoles("user2", ByteBuffer.wrap("pass2".getBytes()));
        assertNull(roles2);
    }

    private void initValidator(@NotNull final String credentials, final boolean hashed) throws Exception {
        extensionConfig = new ExtensionConfig();
        if (hashed) {
            extensionConfig.setPasswordType(PasswordType.HASHED);
        }else{
            extensionConfig.setPasswordType(PasswordType.PLAIN);
        }
        homeFolder = temporaryFolder.getRoot();

        Files.writeString(new File(homeFolder, "credentials.xml").toPath(), credentials);

        final Configuration configuration = new Configuration(homeFolder, scheduledExecutorService, extensionConfig);
        configuration.init();

        validator = new CredentialsValidator(configuration, extensionConfig, new MetricRegistry());
        validator.init();
    }

    public static final String ROLES = "    <roles>\n" +
            "        <role>\n" +
            "            <id>role1</id>\n" +
            "            <permissions>\n" +
            "                <permission>\n" +
            "                    <topic>data/${{clientid}}/personal</topic>\n" +
            "                </permission>\n" +
            "            </permissions>\n" +
            "        </role>\n" +
            "        <role>\n" +
            "            <id>role2</id>\n" +
            "            <permissions>\n" +
            "                <permission>\n" +
            "                    <topic>${{username}}/#</topic>\n" +
            "                </permission>\n" +
            "            </permissions>\n" +
            "        </role>\n" +
            "    </roles>\n";

    private static final String HASHED_CREDENTIALS = "<file-rbac>" +
            "   <users>\n" +
            "        <user>\n" +
            "            <name>user1</name>\n" +
            "            <password>c2FsdA==:100:MAK8JjJQh/c4uYbwkAm33TRXCbeuBC+meeK9ww3Mu4KTv08+8ywTKgF24MNHotOESjDmsutrEk+38PaZVX2TFA==</password>\n" +
            "            <roles>\n" +
            "                <id>role1</id>\n" +
            "            </roles>\n" +
            "        </user>\n" +
            "        <user>\n" +
            "            <name>user2</name>\n" +
            "            <password>c2FsdA==:100:99RGrFfo+l2fQ+KTeSdM/5SZBAJlxj25jzwfAfNeqCe4+9ejGBSEue1w005Uq3+aoZKn89JXNQU8hgHKneu0Dw==</password>\n" +
            "            <roles>\n" +
            "                <id>role1</id>\n" +
            "                <id>role2</id>\n" +
            "            </roles>\n" +
            "        </user>\n" +
            "    </users>\n" +
            ROLES +
            "</file-rbac>";

    private static final String PLAIN_CREDENTIALS = "<file-rbac>" +
            "   <users>\n" +
            "        <user>\n" +
            "            <name>user1</name>\n" +
            "            <password>pass1</password>\n" +
            "            <roles>\n" +
            "                <id>role1</id>\n" +
            "            </roles>\n" +
            "        </user>\n" +
            "        <user>\n" +
            "            <name>user2</name>\n" +
            "            <password>pass2</password>\n" +
            "            <roles>\n" +
            "                <id>role1</id>\n" +
            "                <id>role2</id>\n" +
            "            </roles>\n" +
            "        </user>\n" +
            "    </users>\n" +
            ROLES +
            "</file-rbac>";

    public static class TestTopicPermissionBuilder implements TopicPermissionBuilder {

        private String topicFilter = null;

        @Override
        public @NotNull TopicPermissionBuilder topicFilter(@NotNull final String topicFilter) {
            this.topicFilter = topicFilter;
            return this;
        }

        @Override
        public @NotNull TopicPermissionBuilder type(final TopicPermission.@NotNull PermissionType type) {
            return this;
        }

        @Override
        public @NotNull TopicPermissionBuilder qos(final TopicPermission.@NotNull Qos qos) {
            return this;
        }

        @Override
        public @NotNull TopicPermissionBuilder activity(final TopicPermission.@NotNull MqttActivity activity) {
            return this;
        }

        @Override
        public @NotNull TopicPermissionBuilder retain(final TopicPermission.@NotNull Retain retain) {
            return this;
        }

        @Override
        public @NotNull TopicPermissionBuilder sharedSubscription(final TopicPermission.@NotNull SharedSubscription sharedSubscription) {
            return this;
        }

        @Override
        public @NotNull TopicPermissionBuilder sharedGroup(@NotNull final String sharedGroup) {
            return this;
        }

        @Override
        public @NotNull TopicPermission build() {
            return new TestTopicPermission(topicFilter);
        }

    }

    public static class TestTopicPermission implements TopicPermission {

        private final String topicFilter;

        public TestTopicPermission(@NotNull final String topicFilter) {
            this.topicFilter = topicFilter;
        }

        @Override
        public @NotNull String getTopicFilter() {
            return topicFilter;
        }

        @Override
        public @NotNull PermissionType getType() {
            return null;
        }

        @Override
        public @NotNull Qos getQos() {
            return null;
        }

        @Override
        public @NotNull MqttActivity getActivity() {
            return null;
        }

        @Override
        public @NotNull Retain getPublishRetain() {
            return null;
        }

        @Override
        public @NotNull SharedSubscription getSharedSubscription() {
            return null;
        }

        @Override
        public @NotNull String getSharedGroup() {
            return null;
        }
    }

}
