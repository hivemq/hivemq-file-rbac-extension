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
package com.hivemq.extensions.rbac.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Base64;

public class Hashing {

    @NotNull
    public static byte[] createHash(
            @NotNull final String base64Password, @NotNull final String base64Salt, final int iterations) {
        final byte[] password = Base64.getDecoder().decode(base64Password);
        final byte[] salt = Base64.getDecoder().decode(base64Salt);
        final PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA512Digest());
        gen.init(password, salt, iterations);
        return ((KeyParameter) gen.generateDerivedParameters(512)).getKey();
    }
}
