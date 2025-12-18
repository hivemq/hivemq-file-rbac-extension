/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.rbac.file.utils;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.builder.TopicPermissionBuilder;
import com.hivemq.extensions.rbac.file.configuration.CredentialsConfiguration;
import com.hivemq.extensions.rbac.file.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.file.configuration.entities.PasswordType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class CredentialsValidatorTest {

    private static final @NotNull String ROLES = """
                <roles>
                    <role>
                        <id>role1</id>
                        <permissions>
                            <permission>
                                <topic>data/${{clientid}}/personal</topic>
                            </permission>
                        </permissions>
                    </role>
                    <role>
                        <id>role2</id>
                        <permissions>
                            <permission>
                                <topic>${{username}}/#</topic>
                            </permission>
                        </permissions>
                    </role>
                </roles>
            """;

    private static final @NotNull String HASHED_CREDENTIALS = """
            <file-rbac>
               <users>
                    <user>
                        <name>user1</name>
                        <password>c2FsdA==:100:MAK8JjJQh/c4uYbwkAm33TRXCbeuBC+meeK9ww3Mu4KTv08+8ywTKgF24MNHotOESjDmsutrEk+38PaZVX2TFA==</password>
                        <roles>
                            <id>role1</id>
                        </roles>
                    </user>
                    <user>
                        <name>user2</name>
                        <password>c2FsdA==:100:99RGrFfo+l2fQ+KTeSdM/5SZBAJlxj25jzwfAfNeqCe4+9ejGBSEue1w005Uq3+aoZKn89JXNQU8hgHKneu0Dw==</password>
                        <roles>
                            <id>role1</id>
                            <id>role2</id>
                        </roles>
                    </user>
                </users>
            """ + ROLES + """
            </file-rbac>""";

    private static final @NotNull String PLAIN_CREDENTIALS = """
            <file-rbac>
               <users>
                    <user>
                        <name>user1</name>
                        <password>pass1</password>
                        <roles>
                            <id>role1</id>
                        </roles>
                    </user>
                    <user>
                        <name>user2</name>
                        <password>pass2</password>
                        <roles>
                            <id>role1</id>
                            <id>role2</id>
                        </roles>
                    </user>
                </users>
            """ + ROLES + """
            </file-rbac>""";

    @TempDir
    private @NotNull Path extensionHome;

    private final @NotNull ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private @NotNull CredentialsValidator validator;

    @BeforeEach
    void setUp() throws Exception {
        validator = initValidator(PLAIN_CREDENTIALS, false);
    }

    @AfterEach
    void tearDown() {
        scheduledExecutorService.shutdown();
    }

    @Test
    void test_valid_roles_plain() {
        final var roles = validator.getRoles("user1", ByteBuffer.wrap("pass1".getBytes()));
        assertThat(roles).containsExactly("role1");
        final var roles2 = validator.getRoles("user2", ByteBuffer.wrap("pass2".getBytes()));
        assertThat(roles2).containsExactly("role1", "role2");
    }

    @Test
    void test_valid_roles_hashed() throws Exception {
        this.validator = initValidator(HASHED_CREDENTIALS, true);
        final var roles = validator.getRoles("user1", ByteBuffer.wrap("pass1".getBytes()));
        assertThat(roles).containsExactly("role1");
        final var roles2 = validator.getRoles("user2", ByteBuffer.wrap("pass2".getBytes()));
        assertThat(roles2).containsExactly("role1", "role2");
    }

    @Test
    void test_permissions() {
        try (final var ignored = mockStatic(Builders.class)) {
            when(Builders.topicPermission()).thenReturn(new TestTopicPermissionBuilder());
            final var permissions = validator.getPermissions("client1", "user1", List.of("role1"));
            assertThat(permissions).singleElement()
                    .satisfies(permission -> assertThat(permission.getTopicFilter()).isEqualTo("data/client1/personal"));
            final var permissions2 = validator.getPermissions("client2", "user2", List.of("role2"));
            assertThat(permissions2).singleElement()
                    .satisfies(permission -> assertThat(permission.getTopicFilter()).isEqualTo("user2/#"));
            final var permissions3 = validator.getPermissions("client3", "user3", List.of("role1", "role2"));
            assertThat(permissions3).satisfiesExactly( //
                    permission -> assertThat(permission.getTopicFilter()).isEqualTo("data/client3/personal"),
                    permission -> assertThat(permission.getTopicFilter()).isEqualTo("user3/#"));
        }
    }

    @Test
    void test_invalid_roles() {
        final var roles = validator.getRoles("user1", ByteBuffer.wrap("pass2".getBytes()));
        assertThat(roles).isNull();
        final var roles2 = validator.getRoles("user2", ByteBuffer.wrap("pass1".getBytes()));
        assertThat(roles2).isNull();
        final var roles3 = validator.getRoles("user3", ByteBuffer.wrap("pass3".getBytes()));
        assertThat(roles3).isNull();
    }

    @Test
    void test_invalid_config() throws Exception {
        validator = initValidator("", false);
        final var roles = validator.getRoles("user1", ByteBuffer.wrap("pass1".getBytes()));
        assertThat(roles).isNull();
        final var roles2 = validator.getRoles("user2", ByteBuffer.wrap("pass2".getBytes()));
        assertThat(roles2).isNull();
    }

    private @NotNull CredentialsValidator initValidator(final @NotNull String credentials, final boolean hashed)
            throws Exception {
        final var extensionConfig = new ExtensionConfig();
        if (hashed) {
            extensionConfig.setPasswordType(PasswordType.HASHED);
        } else {
            extensionConfig.setPasswordType(PasswordType.PLAIN);
        }
        Files.writeString(extensionHome.resolve("credentials.xml"), credentials);
        final var credentialsConfiguration =
                new CredentialsConfiguration(extensionHome, scheduledExecutorService, extensionConfig);
        credentialsConfiguration.init();
        final var validator = new CredentialsValidator(credentialsConfiguration, extensionConfig, new MetricRegistry());
        validator.init();
        return validator;
    }

    private static class TestTopicPermissionBuilder implements TopicPermissionBuilder {

        private @NotNull String topicFilter = "invalidFilter";

        @Override
        public @NotNull TopicPermissionBuilder topicFilter(final @NotNull String topicFilter) {
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
        public @NotNull TopicPermissionBuilder sharedGroup(final @NotNull String sharedGroup) {
            return this;
        }

        @Override
        public @NotNull TopicPermission build() {
            return new TestTopicPermission(topicFilter);
        }

    }

    private record TestTopicPermission(@NotNull String topicFilter) implements TopicPermission {

        @Override
        public @NotNull String getTopicFilter() {
            return topicFilter;
        }

        @Override
        public @NotNull PermissionType getType() {
            return mock(PermissionType.class);
        }

        @Override
        public @NotNull Qos getQos() {
            return mock(Qos.class);
        }

        @Override
        public @NotNull MqttActivity getActivity() {
            return mock(MqttActivity.class);
        }

        @Override
        public @NotNull Retain getPublishRetain() {
            return mock(Retain.class);
        }

        @Override
        public @NotNull SharedSubscription getSharedSubscription() {
            return mock(SharedSubscription.class);
        }

        @Override
        public @NotNull String getSharedGroup() {
            return mock(String.class);
        }
    }
}
