package org.webtide.demo.auction;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.WebSocketContainer;

import org.cometd.bayeux.Message;
import org.cometd.client.websocket.javax.WebSocketTransport;

public class SlowJSONTransport extends WebSocketTransport {

    public SlowJSONTransport(String url, Map<String, Object> options, ScheduledExecutorService scheduler, WebSocketContainer webSocketContainer) {
        super(url, options, scheduler, webSocketContainer);
    }

    public SlowJSONTransport(Map<String, Object> options, ScheduledExecutorService scheduler,
            WebSocketContainer webSocketContainer) {
        super(options, scheduler, webSocketContainer);
    }

    @Override
    protected String generateJSON(List<Message.Mutable> messages) {
        System.console().printf("Waiting...");
        try {
            TimeUnit.MILLISECONDS.sleep(6000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return super.generateJSON(messages);
    }
}
