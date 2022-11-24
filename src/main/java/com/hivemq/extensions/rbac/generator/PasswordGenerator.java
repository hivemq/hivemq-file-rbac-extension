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
package com.hivemq.extensions.rbac.generator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.rbac.utils.Hashing;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PasswordGenerator {

    @SuppressWarnings("unused")
    @Parameter(names = {"--password", "-p"},
               required = true,
               description = "The password to create a hashed representation from")
    private @Nullable String password;

    @Parameter(names = {"--salt", "-s"},
               description = "The salt to use for hashing (optional). If no salt is specified, a random salt is used")
    private @Nullable String salt;

    @SuppressWarnings({"FieldCanBeLocal", "CanBeFinal"})
    @Parameter(names = {"--iterations", "-i"}, description = "The amount of hashing iterations. Default: 100")
    private int iterations = 100;

    @Parameter(names = "--help", help = true)
    private boolean help;

    @SuppressWarnings({"FieldCanBeLocal", "CanBeFinal"})
    @Parameter(names = "-q", description = "Only outputs the hash string.")
    private boolean quiet = false;

    public static void main(final @NotNull String @NotNull [] args) {
        try {
            final PasswordGenerator generator = new PasswordGenerator();
            final JCommander jCommander = JCommander.newBuilder().addObject(generator).build();
            jCommander.parse(args);

            if (generator.help) {
                jCommander.usage();
                System.exit(0);
            }
            generator.generateHash();
        } catch (final ParameterException e) {
            System.err.println(e.getMessage());
            e.getJCommander().usage();
            System.exit(1);
        }
    }

    private void generateHash() {
        if (password == null || password.isEmpty()) {
            System.err.println("Required Parameter Password missing");
            System.exit(1);
        }
        if (iterations < 1) {
            System.err.println("Iterations must be larger than 0");
            System.exit(1);
        }
        if (salt == null) {
            salt = RandomStringUtils.randomAlphanumeric(32);
        }
        final String base64Password = Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
        final String base64Salt = Base64.getEncoder().encodeToString(salt.getBytes(StandardCharsets.UTF_8));
        final byte[] hash = Hashing.createHash(base64Password, base64Salt, iterations);
        final String passwordString = base64Salt + ":" + iterations + ":" + Base64.getEncoder().encodeToString(hash);
        if (!quiet) {
            System.out.println("Add the following string as password to your credentials configuration file:\n" +
                    "----------------------------------------------------------------------------");
        }
        System.out.println(passwordString);
    }
}
