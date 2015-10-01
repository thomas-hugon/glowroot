/*
 * Copyright 2015 the original author or authors.
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
 */
package org.glowroot.agent.plugin.servlet;

import org.junit.After;
import org.junit.Test;

import org.glowroot.agent.harness.impl.SpyingLogbackFilter;
import org.glowroot.agent.plugin.servlet.ResponseInvoker;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseInvokerTest {

    @After
    public void afterEachTest() {
        if (SpyingLogbackFilter.active()) {
            SpyingLogbackFilter.clearMessages();
        }
    }

    @Test
    public void shouldNotFindServletResponseClass() {
        assertThat(ResponseInvoker.getServletResponseClass(Object.class)).isNull();
    }

    @Test
    public void shouldNotFindGetContentTypeMethod() {
        assertThat(new ResponseInvoker(Object.class).hasGetContentTypeMethod()).isFalse();
    }
}