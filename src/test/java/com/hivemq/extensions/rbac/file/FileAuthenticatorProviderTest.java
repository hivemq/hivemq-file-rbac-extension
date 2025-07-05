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
package com.hivemq.extensions.rbac.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FileAuthenticatorProviderTest {

    @Test
    void test_return_same_authenticator() {
        final var fileAuthenticatorProvider = new FileAuthenticatorProvider(mock(), mock());
        final var authenticator1 = fileAuthenticatorProvider.getAuthenticator(mock());
        final var authenticator2 = fileAuthenticatorProvider.getAuthenticator(mock());
        assertThat(authenticator1).isSameAs(authenticator2);
    }
}
