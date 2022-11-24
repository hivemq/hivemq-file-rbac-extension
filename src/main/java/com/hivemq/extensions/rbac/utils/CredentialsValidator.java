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
package com.hivemq.extensions.rbac.utils;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extensions.rbac.configuration.CredentialsConfiguration;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.configuration.entities.PasswordType;
import com.hivemq.extensions.rbac.configuration.entities.Permission;
import com.hivemq.extensions.rbac.configuration.entities.Role;
import com.hivemq.extensions.rbac.configuration.entities.User;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
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
        final FileAuthConfig currentConfig = credentialsConfiguration.getCurrentConfig();
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
        //If Config is invalid do not allow clients to connect
        if (users.isEmpty() || roles.isEmpty()) {
            return null;
        }

        final Lock readLock = usersLock.readLock();
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
            final String base64Password = encodePassword(password);
            final boolean passwordsEqual = credentialsHasher.checkCredentials(base64Password, user.getPassword());

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
            final @NotNull String clientId, final @NotNull String userName, final @NotNull List<String> clientRoles) {
        if (clientRoles.isEmpty()) {
            return Collections.emptyList();
        }

        final ArrayList<TopicPermission> topicPermissions = new ArrayList<>();
        for (final String clientRole : clientRoles) {
            final Role role = roles.get(clientRole);
            for (final Permission permission : Objects.requireNonNull(role.getPermissions())) {
                topicPermissions.add(toTopicPermission(clientId, userName, permission));
            }
        }
        return topicPermissions;
    }


    private @NotNull String encodePassword(final @NotNull ByteBuffer password) {
        final byte[] passwordBytes = new byte[password.remaining()];
        password.get(passwordBytes);
        return Base64.getEncoder().encodeToString(passwordBytes);
    }

    private void updateUsersMap(final @NotNull FileAuthConfig config) {
        final List<User> newUsers = config.getUsers();
        final ConcurrentHashMap<String, User> newUsersMap =
                new ConcurrentHashMap<>(Objects.requireNonNull(newUsers).size());
        for (final User newUser : newUsers) {
            newUsersMap.put(Objects.requireNonNull(newUser.getName()), newUser);
        }

        final Lock writeLock = usersLock.writeLock();
        writeLock.lock();
        try {
            users = newUsersMap;
        } finally {
            writeLock.unlock();
        }
    }

    private void updateRolesMap(final @NotNull FileAuthConfig config) {
        final List<Role> newRoles = config.getRoles();
        final ConcurrentHashMap<String, Role> newRolesMap =
                new ConcurrentHashMap<>(Objects.requireNonNull(newRoles).size());
        for (final Role newRole : newRoles) {
            newRolesMap.put(Objects.requireNonNull(newRole.getId()), newRole);
        }

        final Lock writeLock = rolesLock.writeLock();
        writeLock.lock();
        try {
            roles = newRolesMap;
        } finally {
            writeLock.unlock();
        }
    }

    private @NotNull TopicPermission toTopicPermission(
            final @NotNull String clientId, final @NotNull String userName, final @NotNull Permission permission) {
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
            final @NotNull String clientId, final @NotNull String userName, final @NotNull Permission permission) {
        final String configTopic = permission.getTopic();
        return Substitution.substitute(Objects.requireNonNull(configTopic), clientId, userName);
    }
}
