/*
 * Copyright (c) 2008-2020 the original author or authors.
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
package org.cometd.javascript;

import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerMessage.Mutable;
import org.cometd.bayeux.server.ServerSession;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test that in a slow network, an uninterrupted message flow due to a big subscription won't cause
 * the client to drop the connection
 */
public class CometDWebSocketSlowNetworkConnectTimeoutTest extends AbstractCometDWebSocketTest {
    private final long connectTimeout = 1000;

    /**
     * The socket will be throttled in order to send a message every 100ms
     */
    private final long messageEvery = 100;

    /**
     * 100 messages * 100ms = flow of 10 seconds
     */
    private final int messageCount = 100;

    /**
     * Test that a continue message flow through a slow socket won't cause connect abort
     * @throws Exception
     */
    @Test
    public void testConnectTimeoutIsCanceledOnSteadyMessageFlow() throws Exception {
        SlowOutgoingExtension extension = new SlowOutgoingExtension();
        bayeuxServer.addExtension(extension);

        evaluateScript("var handshakeLatch = new Latch(1);");
        Latch handshakeLatch = javaScript.get("handshakeLatch");
        evaluateScript("var connectErrLatch = new Latch(1);");
        Latch connectErrLatch = javaScript.get("connectErrLatch");

        evaluateScript("cometd.configure({" +
                "url: '" + cometdURL + "', " +
                "connectTimeout: " + connectTimeout + ", " +
                "logLevel: '" + getLogLevel() + "'" +
                "});");
        evaluateScript("cometd.addListener('/meta/handshake', function(message) {" +
                "   if (cometd.getTransport().getType() === 'websocket' && message.successful) {" +
                "       handshakeLatch.countDown();" +
                "   }" +
                "});");
        evaluateScript("cometd.addListener('/meta/connect', function(message) {" +
                "   if (!message.successful) {" +
                "       connectErrLatch.countDown();" +
                "   }" +
                "});");

        evaluateScript("cometd.handshake()");
        Assert.assertTrue(handshakeLatch.await(2 * connectTimeout));

        // Wait to be sure we're not disconnected
        Assert.assertFalse(connectErrLatch.await(2 * connectTimeout));

        // Now receive messages
        evaluateScript("var messagesLatch = new Latch(" + messageCount + ");");
        Latch messagesLatch = javaScript.get("messagesLatch");
        // Now subscribe for a lot of messages, clogging the channel
        for (int i = 0; i < messageCount; i++) {
            evaluateScript("cometd.subscribe('/echo" + i + "', function(message) { messagesLatch.countDown(); });");
        }
        for (int i = 0; i < messageCount; i++) {
            evaluateScript("cometd.publish('/echo" + i + "', { dummy: 42 });");
        }

        // Wait for messageLatch to zero
        Assert.assertTrue(messagesLatch.await(30000));
        Assert.assertEquals(messageCount * 2, extension.calls);

        // Wait to be sure we're not disconnected in the middle
        Assert.assertFalse(connectErrLatch.await(2 * connectTimeout));
        
        // Test disconnection
        evaluateScript("var disconnectLatch = new Latch(1);");
        Latch disconnectLatch = javaScript.get("disconnectLatch");
        evaluateScript("cometd.addListener('/meta/disconnect', function() { disconnectLatch.countDown(); });");
        evaluateScript("cometd.disconnect();");
        Assert.assertTrue(disconnectLatch.await(5000));
    }

    private class SlowOutgoingExtension implements BayeuxServer.Extension {
        public int calls = 0;

        @Override
        public boolean send(ServerSession from, ServerSession to, Mutable message) {
            calls++;
            sleep(messageEvery);
            return BayeuxServer.Extension.super.send(from, to, message);
        }
    }
}
