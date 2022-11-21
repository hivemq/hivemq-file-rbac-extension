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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Substitution {

    private static final Logger log = LoggerFactory.getLogger(Substitution.class);

    private static final String PREFIX = "${{";
    private static final String SUFFIX = "}}";
    private static final String ESCAPE_CHAR = "§";

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
    @NotNull
    public static String substitute(
            @NotNull final String topic, @NotNull final String clientId, @NotNull final String username) {

        final StringSubstitutor strSubstitutor = new StringSubstitutor(new StringLookup() {
            @Nullable
            @Override
            public String lookup(@NotNull final String key) {
                final String lowerCaseKey = key.toLowerCase();

                //replace clientid and username
                if ("clientid".equals(lowerCaseKey)) {
                    return clientId;
                } else if ("username".equals(lowerCaseKey)) {
                    return username;
                }

                return null;
            }
        }, PREFIX, SUFFIX, ESCAPE_CHAR.charAt(0));

        return strSubstitutor.replace(topic);
    }
}
