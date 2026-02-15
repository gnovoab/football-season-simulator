package com.example.footballseasonsimulator.websocket;

import com.example.footballseasonsimulator.config.MetricsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listener for WebSocket connection events.
 * Tracks active connections for metrics.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final MetricsConfig metricsConfig;

    public WebSocketEventListener(MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        metricsConfig.incrementWebsocketConnections();
        log.debug("WebSocket connection established. Active connections: {}", 
                metricsConfig.getWebsocketConnectionsCount());
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        metricsConfig.decrementWebsocketConnections();
        log.debug("WebSocket connection closed. Active connections: {}", 
                metricsConfig.getWebsocketConnectionsCount());
    }
}

