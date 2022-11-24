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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.commons.text.StringSubstitutor;

public class Substitution {

    private static final @NotNull String PREFIX = "${{";
    private static final @NotNull String SUFFIX = "}}";
    private static final @NotNull String ESCAPE_CHAR = "§";

    /**
     * Replaces parts enclosed in <code>${{}}</code>
     * <p>
     * Can replace <code>${{clientid}}</code> and <code>${{username}}</code>
     * <p>
     *
     * @param topic    topic pattern
     * @param clientId actual clientId
     * @param username actual username
     * @return substituted string
     */
    public static @NotNull String substitute(
            final @NotNull String topic, final @NotNull String clientId, final @NotNull String username) {
        final StringSubstitutor stringSubstitutor = new StringSubstitutor(key -> {
            final String lowerCaseKey = key.toLowerCase();

            //replace clientid and username
            if ("clientid".equals(lowerCaseKey)) {
                return clientId;
            } else if ("username".equals(lowerCaseKey)) {
                return username;
            }

            return null;
        }, PREFIX, SUFFIX, ESCAPE_CHAR.charAt(0));

        return stringSubstitutor.replace(topic);
    }
}
