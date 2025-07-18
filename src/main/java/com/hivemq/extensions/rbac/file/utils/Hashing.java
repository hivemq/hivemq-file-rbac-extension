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

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

public class Hashing {

    public static byte @NotNull [] createHash(
            final @NotNull String base64Password,
            final @NotNull String base64Salt,
            final int iterations) {
        final var password = Base64.getDecoder().decode(base64Password);
        final var salt = Base64.getDecoder().decode(base64Salt);
        final var generator = new PKCS5S2ParametersGenerator(new SHA512Digest());
        generator.init(password, salt, iterations);
        return ((KeyParameter) generator.generateDerivedParameters(512)).getKey();
    }
}
