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
package org.cometd.server.websocket.javax;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.cometd.bayeux.Promise;
import org.cometd.bayeux.server.BayeuxContext;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.websocket.common.AbstractWebSocketEndPoint;
import org.cometd.server.websocket.common.AbstractWebSocketTransport;
import org.cometd.server.websocket.common.TimeoutScheduledService;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketEndPoint extends Endpoint implements MessageHandler.Whole<String> {
    private final Logger _logger = LoggerFactory.getLogger(getClass());
    private final AbstractWebSocketEndPoint _delegate;
    private volatile Session _wsSession;
    private final TimeoutScheduledService scheduler = new TimeoutScheduledService(100);

    public WebSocketEndPoint(final AbstractWebSocketTransport transport, final BayeuxContext bayeuxContext) {
        _delegate = new Delegate(transport, bayeuxContext);
    }

    @Override
    public void onOpen(final Session wsSession, final EndpointConfig config) {
        _wsSession = wsSession;
        wsSession.addMessageHandler(this);
    }

    @Override
    public void onMessage(final String data) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("WebSocket Text message on {}@{}",
                    getClass().getSimpleName(),
                    Integer.toHexString(hashCode()));
        }
        try {
            try {
                final Promise.Completable<Void> completable = new Promise.Completable<>();
                _delegate.onMessage(data, completable);
                // Cannot return from this method until the processing is finished.
                completable.get();
            } catch (final ExecutionException x) {
                throw x.getCause();
            }
        } catch (final Throwable failure) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("", failure);
            }
            _delegate.close(1011, failure.toString());
        }
    }

    @Override
    public void onClose(final Session wsSession, final CloseReason closeReason) {
        _delegate.onClose(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
    }

    @Override
    public void onError(final Session wsSession, final Throwable failure) {
        _delegate.onError(failure);
    }

    protected void writeComplete(final AbstractWebSocketEndPoint.Context context, final List<ServerMessage> messages) {
    }

    private class Delegate extends AbstractWebSocketEndPoint {
        public Delegate(final AbstractWebSocketTransport transport, final BayeuxContext bayeuxContext) {
            super(transport, bayeuxContext);
        }

        @Override
        protected void send(final ServerSession session, final String data, final Callback callback) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("Sending Delayed {}", data);
            }

            // Simulate slow channel
            scheduler.schedule(() -> {
                _wsSession.getAsyncRemote().sendText(data, 
                    result -> {
                        final Throwable failure = result.getException();
                        if (failure == null) {
                            callback.succeeded();
                        } else {
                            callback.failed(failure);
                        }
                    }
                );
                return null;
            });
        }

        @Override
        public void close(final int code, final String reason) {
            try {
                // Limits of the WebSocket APIs, otherwise an exception is thrown.
                final String reason1 = reason.substring(0, Math.min(reason.length(), 30));
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Closing {}/{}", code, reason1);
                }
                scheduler.schedule(() -> {
                    _wsSession.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(code), reason1));
                    return null;
                });
            } catch (final Throwable x) {
                _logger.trace("Could not close WebSocket session " + _wsSession, x);
            }
        }

        @Override
        protected void writeComplete(final Context context, final List<ServerMessage> messages) {
            WebSocketEndPoint.this.writeComplete(context, messages);
        }
    }
}
