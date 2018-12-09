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
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.junit.Test;

import java.util.Base64;

import static com.hivemq.extensions.rbac.utils.CredentialsHasher.HASH_CACHE_HITRATE;
import static org.junit.Assert.*;


public class CredentialsHasherTest {

    @Test
    public void test_check_credentials_valid_password() {
        final CredentialsHasher credentialsHasher = new CredentialsHasher(new MetricRegistry());
        final String base64Password = Base64.getEncoder().encodeToString("password".getBytes());
        final String hashedPassword = getHashedPassword("password");

        final boolean result = credentialsHasher.checkCredentials(base64Password, hashedPassword);

        assertTrue(result);
    }

    @Test
    public void test_check_credentials_invalid_password() {
        final CredentialsHasher credentialsHasher = new CredentialsHasher(new MetricRegistry());
        final String base64Password = Base64.getEncoder().encodeToString("notapassword".getBytes());
        final String hashedPassword = getHashedPassword("password");

        final boolean result = credentialsHasher.checkCredentials(base64Password, hashedPassword);

        assertFalse(result);
    }

    @Test
    public void test_check_credentials_invalid_string() {
        final CredentialsHasher credentialsHasher = new CredentialsHasher(new MetricRegistry());
        final String base64Password = Base64.getEncoder().encodeToString("password".getBytes());

        final boolean result = credentialsHasher.checkCredentials(base64Password, "invalid-string");

        assertFalse(result);
    }

    @Test
    public void test_check_credentials_valid_cached() {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final CredentialsHasher credentialsHasher = new CredentialsHasher(metricRegistry);
        final String base64Password = Base64.getEncoder().encodeToString("password".getBytes());
        final String hashedPassword = getHashedPassword("password");

        final boolean result1 = credentialsHasher.checkCredentials(base64Password, hashedPassword);
        final boolean result2 = credentialsHasher.checkCredentials(base64Password, hashedPassword);
        final boolean result3 = credentialsHasher.checkCredentials(base64Password, hashedPassword);

        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);

        assertEquals(2, metricRegistry.meter(HASH_CACHE_HITRATE).getCount());
    }

    @NotNull
    private String getHashedPassword(final String passwordString) {
        final String base64Salt = Base64.getEncoder().encodeToString("salt".getBytes());
        final byte[] password = passwordString.getBytes();
        final byte[] salt = "salt".getBytes();
        final PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA512Digest());
        gen.init(password, salt, 100);
        byte[] credentialsHash = ((KeyParameter) gen.generateDerivedParameters(512)).getKey();
        return base64Salt + ":" + 100 + ":" + Base64.getEncoder().encodeToString(credentialsHash);
    }
}