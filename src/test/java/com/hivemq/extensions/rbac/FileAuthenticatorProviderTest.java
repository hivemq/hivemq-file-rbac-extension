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
package com.hivemq.extensions.rbac;

import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.utils.CredentialsValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;


class FileAuthenticatorProviderTest {

    @Test
    void test_return_same_authenticator() {
        final FileAuthenticatorProvider fileAuthenticatorProvider =
                new FileAuthenticatorProvider(mock(CredentialsValidator.class), mock(ExtensionConfig.class));
        final Authenticator authenticator1 =
                fileAuthenticatorProvider.getAuthenticator(mock(AuthenticatorProviderInput.class));
        final Authenticator authenticator2 =
                fileAuthenticatorProvider.getAuthenticator(mock(AuthenticatorProviderInput.class));
        assertSame(authenticator1, authenticator2);
    }
}
