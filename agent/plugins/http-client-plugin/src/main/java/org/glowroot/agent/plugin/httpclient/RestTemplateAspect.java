/*
 * Copyright 2016-2017 the original author or authors.
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
package org.glowroot.agent.plugin.httpclient;

import java.net.URI;

import javax.annotation.Nullable;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.MessageSupplier;
import org.glowroot.agent.plugin.api.ThreadContext;
import org.glowroot.agent.plugin.api.TimerName;
import org.glowroot.agent.plugin.api.TraceEntry;
import org.glowroot.agent.plugin.api.util.FastThreadLocal;
import org.glowroot.agent.plugin.api.weaving.BindParameter;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.OnAfter;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;
import org.glowroot.agent.plugin.api.weaving.Shim;

public class RestTemplateAspect {

    private static final FastThreadLocal</*@Nullable*/ String> templateUrlHolder =
            new FastThreadLocal</*@Nullable*/ String>();

    @Shim("org.springframework.http.HttpMethod")
    public interface HttpMethod {
        String name();
    }

    @Pointcut(className = "org.springframework.web.client.RestTemplate", methodName = "execute",
            methodParameterTypes = {"java.lang.String", "org.springframework.http.HttpMethod",
                    ".."})
    public static class ExecuteAdvice {
        @OnBefore
        public static void onBefore(@BindParameter String url) {
            templateUrlHolder.set(url);
        }
        @OnAfter
        public static void onAfter() {
            templateUrlHolder.set(null);
        }
    }

    @Pointcut(className = "org.springframework.web.client.RestTemplate", methodName = "doExecute",
            methodParameterTypes = {"java.net.URI", "org.springframework.http.HttpMethod", ".."},
            nestingGroup = "http-client", timerName = "http client request")
    public static class DoExecuteAdvice {
        private static final TimerName timerName = Agent.getTimerName(DoExecuteAdvice.class);
        @OnBefore
        public static @Nullable TraceEntry onBefore(ThreadContext context, @BindParameter URI uri,
                @BindParameter HttpMethod method) {
            String templateUrl = templateUrlHolder.get();
            String url = uri.toString();
            if (templateUrl == null) {
                templateUrl = Uris.stripQueryString(url);
            }
            return context.startServiceCallEntry("HTTP", method + " " + templateUrl,
                    MessageSupplier.create("http client request: {} {}", method.name(), url),
                    timerName);
        }
        @OnReturn
        public static void onReturn(@BindTraveler @Nullable TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.end();
            }
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable t,
                @BindTraveler @Nullable TraceEntry traceEntry) {
            if (traceEntry != null) {
                traceEntry.endWithError(t);
            }
        }
    }
}
