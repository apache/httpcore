/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.hc.core5.testing.nio;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.nio.AsyncServerExchangeHandler;
import org.apache.hc.core5.reactor.ExceptionEvent;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.testing.SSLTestContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class InternalConnectChannelTimeoutTest extends InternalHttp1ServerTestBase {

    private final Logger log = LogManager.getLogger(getClass());

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> protocols() {
        return Arrays.asList(new Object[][]{
                { URIScheme.HTTP },
                { URIScheme.HTTPS }
        });
    }

    public InternalConnectChannelTimeoutTest(final URIScheme scheme) {
        super(scheme);
    }

    private static final TimeValue TIMEOUT = TimeValue.ofSeconds(1);
    private static final TimeValue LONG_TIMEOUT = TimeValue.ofSeconds(60);

    private Http1TestClient client;

    @Before
    public void setup() throws Exception {
        log.debug("Starting up test client");
        IOReactorConfig io = IOReactorConfig.custom().setIoThreadCount(1).build();
        
        client = new Http1TestClient(
                io,
                scheme == URIScheme.HTTPS ? SSLTestContexts.createClientSSLContext() : null);
    }

    @After
    public void cleanup() throws Exception {
        log.debug("Shutting down test client");
        if (client != null) {
            client.shutdown(TimeValue.ofSeconds(5));
            final List<ExceptionEvent> exceptionLog = client.getExceptionLog();
            if (!exceptionLog.isEmpty()) {
                for (final ExceptionEvent event: exceptionLog) {
                    final Throwable cause = event.getCause();
                    log.error("Unexpected " + cause.getClass() + " at " + event.getTimestamp(), cause);
                }
            }
        }
    }

    private URI createRequestURI(final InetSocketAddress serverEndpoint, final String path) {
        try {
            return new URI(scheme.id, null, "localhost", serverEndpoint.getPort(), path, null, null);
        } catch (final URISyntaxException e) {
            throw new IllegalStateException();
        }
    }

    @Test
    public void testSimpleGet() throws Exception {
        server.register("/hello", new Supplier<AsyncServerExchangeHandler>() {

            @Override
            public AsyncServerExchangeHandler get() {
                return new SingleLineResponseHandler("Hi there");
            }

        });
        final InetSocketAddress serverEndpoint = server.start();

        client.start();
        final Future<ClientSessionEndpoint> connectFuture = client.connect(
                "localhost", serverEndpoint.getPort(), TIMEOUT);
        //这里需要让
        ClientSessionEndpoint streamEndpoint = null;
        try {
            streamEndpoint = connectFuture.get();
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
    }



}
