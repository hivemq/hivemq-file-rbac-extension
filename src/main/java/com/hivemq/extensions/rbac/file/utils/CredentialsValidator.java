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
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extensions.rbac.file.configuration.CredentialsConfiguration;
import com.hivemq.extensions.rbac.file.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.file.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.file.configuration.entities.PasswordType;
import com.hivemq.extensions.rbac.file.configuration.entities.Permission;
import com.hivemq.extensions.rbac.file.configuration.entities.Role;
import com.hivemq.extensions.rbac.file.configuration.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
public class CredentialsValidator {

    private final @NotNull CredentialsConfiguration credentialsConfiguration;
    private final @NotNull ExtensionConfig extensionConfig;
    private final @NotNull CredentialsHasher credentialsHasher;
    private final @NotNull ReadWriteLock usersLock = new ReentrantReadWriteLock();
    private final @NotNull ReadWriteLock rolesLock = new ReentrantReadWriteLock();
    private @NotNull Map<String, User> users = new ConcurrentHashMap<>();
    private @NotNull Map<String, Role> roles = new ConcurrentHashMap<>();

    public CredentialsValidator(
            final @NotNull CredentialsConfiguration credentialsConfiguration,
            final @NotNull ExtensionConfig extensionConfig,
            final @NotNull MetricRegistry metricRegistry) {
        this.credentialsConfiguration = credentialsConfiguration;
        this.extensionConfig = extensionConfig;
        this.credentialsHasher = new CredentialsHasher(metricRegistry);
    }

    public void init() {
        final var currentConfig = credentialsConfiguration.getCurrentConfig();
        if (currentConfig != null) {
            updateUsersMap(currentConfig);
            updateRolesMap(currentConfig);
        }
        credentialsConfiguration.addReloadCallback((oldConfig, newConfig) -> {
            updateUsersMap(newConfig);
            updateRolesMap(newConfig);
        });
    }

    /**
     * @param userName the userName
     * @param password the password
     * @return a list of the users roles or null if the credentials are not valid
     */
    public @Nullable List<String> getRoles(final @NotNull String userName, final @NotNull ByteBuffer password) {
        // if config is invalid do not allow clients to connect
        if (users.isEmpty() || roles.isEmpty()) {
            return null;
        }
        final var readLock = usersLock.readLock();
        readLock.lock();
        final User user;
        try {
            user = users.get(userName);
        } finally {
            readLock.unlock();
        }
        if (user == null || user.getPassword() == null) {
            return null;
        }
        if (extensionConfig.getPasswordType() == PasswordType.HASHED) {
            final var base64Password = encodePassword(password);
            final var passwordsEqual = credentialsHasher.checkCredentials(base64Password, user.getPassword());
            if (!passwordsEqual) {
                return null;
            }
        } else {
            if (!user.getPassword().equals(StandardCharsets.UTF_8.decode(password).toString())) {
                return null;
            }
        }
        return user.getRoles();
    }

    public @NotNull List<TopicPermission> getPermissions(
            final @NotNull String clientId,
            final @NotNull String userName,
            final @NotNull List<String> clientRoles) {
        if (clientRoles.isEmpty()) {
            return Collections.emptyList();
        }
        final var topicPermissions = new ArrayList<TopicPermission>();
        for (final var clientRole : clientRoles) {
            final var role = roles.get(clientRole);
            for (final var permission : Objects.requireNonNull(role.getPermissions())) {
                topicPermissions.add(toTopicPermission(clientId, userName, permission));
            }
        }
        return topicPermissions;
    }

    private @NotNull String encodePassword(final @NotNull ByteBuffer password) {
        final var passwordBytes = new byte[password.remaining()];
        password.get(passwordBytes);
        return Base64.getEncoder().encodeToString(passwordBytes);
    }

    private void updateUsersMap(final @NotNull FileAuthConfig config) {
        final var newUsers = config.getUsers();
        final var newUsersMap = new ConcurrentHashMap<String, User>(Objects.requireNonNull(newUsers).size());
        for (final var newUser : newUsers) {
            newUsersMap.put(Objects.requireNonNull(newUser.getName()), newUser);
        }
        final var writeLock = usersLock.writeLock();
        writeLock.lock();
        try {
            users = newUsersMap;
        } finally {
            writeLock.unlock();
        }
    }

    private void updateRolesMap(final @NotNull FileAuthConfig config) {
        final var newRoles = config.getRoles();
        final var newRolesMap = new ConcurrentHashMap<String, Role>(Objects.requireNonNull(newRoles).size());
        for (final var newRole : newRoles) {
            newRolesMap.put(Objects.requireNonNull(newRole.getId()), newRole);
        }
        final var writeLock = rolesLock.writeLock();
        writeLock.lock();
        try {
            roles = newRolesMap;
        } finally {
            writeLock.unlock();
        }
    }

    private @NotNull TopicPermission toTopicPermission(
            final @NotNull String clientId,
            final @NotNull String userName,
            final @NotNull Permission permission) {
        return Builders.topicPermission()
                .topicFilter(getTopicFilter(clientId, userName, permission))
                .activity(Objects.requireNonNull(permission.getActivity()))
                .type(TopicPermission.PermissionType.ALLOW)
                .retain(Objects.requireNonNull(permission.getRetain()))
                .qos(Objects.requireNonNull(permission.getQos()))
                .sharedSubscription(Objects.requireNonNull(permission.getSharedSubscription()))
                .sharedGroup(Objects.requireNonNull(permission.getSharedGroup()))
                .build();
    }

    private @NotNull String getTopicFilter(
            final @NotNull String clientId,
            final @NotNull String userName,
            final @NotNull Permission permission) {
        final var configTopic = permission.getTopic();
        return Substitution.substitute(Objects.requireNonNull(configTopic), clientId, userName);
    }
}
