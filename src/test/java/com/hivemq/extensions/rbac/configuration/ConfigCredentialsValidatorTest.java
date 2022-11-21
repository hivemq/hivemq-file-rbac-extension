/*
 *
 * Copyright 2019 dc-square GmbH
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
import com.hivemq.extensions.rbac.configuration.ConfigCredentialsValidator.ValidationResult;
import com.hivemq.extensions.rbac.configuration.entities.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ConfigCredentialsValidatorTest {

    @Test
    public void test_no_roles() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(null);
        config.setUsers(List.of(new User("a", "b", List.of("1"))));

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());

        checkErrorString(result, "No Roles found in configuration file");
    }

    @Test
    public void test_empty_roles() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of());
        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());

        checkErrorString(result, "No Roles found in configuration file");
    }

    @Test
    public void test_no_users() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("id1", List.of(new Permission("topic")))));
        config.setUsers(null);


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "No Users found in configuration file");
    }

    @Test
    public void test_empty_users() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("id1", List.of(new Permission("topic")))));
        config.setUsers(List.of());


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "No Users found in configuration file");
    }

    @Test
    public void test_missing_id() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role(null, List.of(new Permission("topic")))));
        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "A Role is missing an ID");
    }

    @Test
    public void test_empty_id() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("", List.of(new Permission("topic")))));
        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "A Role is missing an ID");
    }

    @Test
    public void test_duplicate_id() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(
                new Role("1", List.of(new Permission("topic"))),
                new Role("1", List.of(new Permission("topic")))
        ));

        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Duplicate ID '1' for role");
    }

    @Test
    public void test_no_permissions() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(
                new Role("1", List.of(new Permission("topic"))),
                new Role("2", List.of())
        ));

        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Role '2' is missing permissions");
    }

    @Test
    public void test_null_permissions() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(
                new Role("1", List.of(new Permission("topic"))),
                new Role("2", null)
        ));

        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Role '2' is missing permissions");
    }

    @Test
    public void test_permission_no_topic() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(
                new Role("1", List.of(new Permission(null)))
        ));

        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "A Permission for role with id '1' is missing a topic filter");
    }

    @Test
    public void test_permission_no_activity() {

        final FileAuthConfig config = new FileAuthConfig();
        final Permission permission = new Permission("abc");
        permission.setActivity(null);
        config.setRoles(List.of(
                new Role("1", List.of(permission))
        ));

        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Invalid value for Activity in Permission for role with id '1'");
    }

    @Test
    public void test_permission_no_qos() {

        final FileAuthConfig config = new FileAuthConfig();
        final Permission permission = new Permission("abc");
        permission.setQos(null);
        config.setRoles(List.of(
                new Role("1", List.of(permission))
        ));

        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Invalid value for QoS in Permission for role with id '1'");
    }

    @Test
    public void test_permission_no_retain() {

        final FileAuthConfig config = new FileAuthConfig();
        final Permission permission = new Permission("abc");
        permission.setRetain(null);
        config.setRoles(List.of(
                new Role("1", List.of(permission))
        ));

        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Invalid value for Retain in Permission for role with id '1'");
    }

    @Test
    public void test_permission_no_sharedGroup() {

        final FileAuthConfig config = new FileAuthConfig();
        final Permission permission = new Permission("abc");
        permission.setSharedGroup(null);
        config.setRoles(List.of(
                new Role("1", List.of(permission))
        ));

        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Invalid value for Shared Group in Permission for role with id '1'");
    }

    @Test
    public void test_permission_no_sharedSub() {

        final FileAuthConfig config = new FileAuthConfig();
        final Permission permission = new Permission("abc");
        permission.setSharedSubscription(null);
        config.setRoles(List.of(
                new Role("1", List.of(permission))
        ));

        config.setUsers(List.of(new User("a", "b", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Invalid value for Shared Subscription in Permission for role with id '1'");
    }

    @Test
    public void test_name_missing() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("1", List.of(new Permission("topic")))));

        config.setUsers(List.of(new User(null, "pass1", List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "A User is missing a name");
    }

    @Test
    public void test_pass_missing() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("1", List.of(new Permission("topic")))));

        config.setUsers(List.of(new User("user1", null, List.of("1"))));


        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "User 'user1' is missing a password");
    }

    @Test
    public void test_duplicate_username() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("1", List.of(new Permission("topic")))));

        config.setUsers(List.of(
                new User("user1", "pass1", List.of("1")),
                new User("user1", "pass2", List.of("2"))
        ));

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Duplicate Name 'user1' for user");
    }

    @Test
    public void test_valid_hashed_pw_username() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("1", List.of(new Permission("topic")))));

        config.setUsers(List.of(
                new User("user1", "pass1:pass2", List.of("1"))
        ));

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setPasswordType(PasswordType.HASHED);

        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertTrue(result.isValidationSuccessful());
    }

    @Test
    public void test_invalid_hashed_pw_username() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("1", List.of(new Permission("topic")))));

        config.setUsers(List.of(
                new User("user1", "pass1pass2", List.of("1"))
        ));

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setPasswordType(PasswordType.HASHED);

        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "User 'user1' has invalid password");
    }

    @Test
    public void test_user_role_missing() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("1", List.of(new Permission("topic")))));

        config.setUsers(List.of(
                new User("user1", "pass1", null)
        ));

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setPasswordType(PasswordType.PLAIN);

        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "User 'user1' is missing roles");
    }

    @Test
    public void test_user_role_empty() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("1", List.of(new Permission("topic")))));

        config.setUsers(List.of(
                new User("user1", "pass1", List.of())
        ));

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setPasswordType(PasswordType.PLAIN);

        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "User 'user1' is missing roles");
    }

    @Test
    public void test_user_invalid_role() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("1", List.of(new Permission("topic")))));

        config.setUsers(List.of(
                new User("user1", "pass1", List.of(""))
        ));

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setPasswordType(PasswordType.PLAIN);

        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Invalid role for user 'user1'");
    }

    @Test
    public void test_user_unknown_role() {

        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("1", List.of(new Permission("topic")))));

        config.setUsers(List.of(
                new User("user1", "pass1", List.of("2"))
        ));

        final ExtensionConfig extensionConfig = new ExtensionConfig();
        extensionConfig.setPasswordType(PasswordType.PLAIN);

        final ValidationResult result = ConfigCredentialsValidator.validateConfig(extensionConfig, config);

        assertFalse(result.isValidationSuccessful());
        checkErrorString(result, "Unknown role '2' for user 'user1'");
    }


    private void checkErrorString(final ValidationResult result, @NotNull final String s) {
        assertTrue("Wanted error reason \"" + s + "\" not contained in: " + result.getErrors().toString(),
                result.getErrors().contains(s));
    }

}