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
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static com.hivemq.extensions.rbac.file.utils.CredentialsHasher.HASH_CACHE_HITRATE;
import static org.assertj.core.api.Assertions.assertThat;

class CredentialsHasherTest {

    @Test
    void test_check_credentials_valid_password() {
        final var credentialsHasher = new CredentialsHasher(new MetricRegistry());
        final var base64Password = Base64.getEncoder().encodeToString("password".getBytes());
        final var hashedPassword = getHashedPassword();
        final var result = credentialsHasher.checkCredentials(base64Password, hashedPassword);
        assertThat(result).isTrue();
    }

    @Test
    void test_check_credentials_invalid_password() {
        final var credentialsHasher = new CredentialsHasher(new MetricRegistry());
        final var base64Password = Base64.getEncoder().encodeToString("notapassword".getBytes());
        final var hashedPassword = getHashedPassword();
        final var result = credentialsHasher.checkCredentials(base64Password, hashedPassword);
        assertThat(result).isFalse();
    }

    @Test
    void test_check_credentials_invalid_string() {
        final var credentialsHasher = new CredentialsHasher(new MetricRegistry());
        final var base64Password = Base64.getEncoder().encodeToString("password".getBytes());
        final var result = credentialsHasher.checkCredentials(base64Password, "invalid-string");
        assertThat(result).isFalse();
    }

    @Test
    void test_check_credentials_valid_cached() {
        final var metricRegistry = new MetricRegistry();
        final var credentialsHasher = new CredentialsHasher(metricRegistry);
        final var base64Password = Base64.getEncoder().encodeToString("password".getBytes());
        final var hashedPassword = getHashedPassword();
        final var result1 = credentialsHasher.checkCredentials(base64Password, hashedPassword);
        final var result2 = credentialsHasher.checkCredentials(base64Password, hashedPassword);
        final var result3 = credentialsHasher.checkCredentials(base64Password, hashedPassword);
        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
        assertThat(result3).isTrue();
        assertThat(metricRegistry.meter(HASH_CACHE_HITRATE).getCount()).isEqualTo(2);
    }

    private static @NotNull String getHashedPassword() {
        final var base64Salt = Base64.getEncoder().encodeToString("salt".getBytes());
        final var password = "password".getBytes();
        final var salt = "salt".getBytes();
        final var gen = new PKCS5S2ParametersGenerator(new SHA512Digest());
        gen.init(password, salt, 100);
        final var credentialsHash = ((KeyParameter) gen.generateDerivedParameters(512)).getKey();
        return base64Salt + ":" + 100 + ":" + Base64.getEncoder().encodeToString(credentialsHash);
    }
}
