/**
 * StompClient.js — STOMP protocol wrapper for JavaLive
 *
 * Wraps the @stomp/stompjs library with JavaLive-specific configuration
 * and automatic reconnection logic.
 *
 * @module StompClient
 */

import { Client } from '@stomp/stompjs';

/**
 * Creates and configures a STOMP client for JavaLive communication.
 *
 * @param {string} wsUrl - WebSocket server URL (e.g., '/javalive-ws')
 * @param {Object} handlers - Event handler callbacks
 * @param {Function} handlers.onConnect - Called when connected
 * @param {Function} handlers.onDisconnect - Called when disconnected
 * @param {Function} handlers.onError - Called on STOMP error
 * @param {Object} [config] - Optional configuration overrides
 * @returns {Client} configured STOMP client (not yet activated)
 */
export function createStompClient(wsUrl, handlers, config = {}) {
  const {
    onConnect    = () => {},
    onDisconnect = () => {},
    onError      = () => {},
  } = handlers;

  const client = new Client({
    // Use SockJS as transport factory for broad compatibility
    // (works even when native WebSocket is blocked by proxies)
    webSocketFactory: () => {
      // Dynamic import of SockJS — try native WS first
      if (config.useSockJS !== false) {
        try {
          return new window.SockJS(wsUrl);
        } catch {
          // SockJS not loaded — fall back to native WebSocket
          const wsWsUrl = wsUrl.replace(/^http/, 'ws');
          return new WebSocket(wsWsUrl);
        }
      }
      const wsWsUrl = wsUrl.replace(/^http/, 'ws');
      return new WebSocket(wsWsUrl);
    },

    // Reconnect automatically after disconnection
    reconnectDelay: config.reconnectDelay ?? 2000,

    // Heartbeats: keep-alive pings
    heartbeatIncoming: config.heartbeatIncoming ?? 4000,
    heartbeatOutgoing: config.heartbeatOutgoing ?? 4000,

    // Callbacks
    onConnect: (frame) => {
      console.log('[JavaLive] ✅ Connected to server');
      onConnect(frame);
    },

    onDisconnect: () => {
      console.warn('[JavaLive] ⚠️ Disconnected from server');
      onDisconnect();
    },

    onStompError: (frame) => {
      console.error('[JavaLive] ❌ STOMP error:', frame.headers?.message, frame.body);
      onError(frame);
    },

    onWebSocketClose: (event) => {
      console.warn('[JavaLive] WebSocket closed:', event.code, event.reason);
    },

    // Log only in debug mode
    debug: config.debug
      ? (msg) => console.debug('[JavaLive STOMP]', msg)
      : () => {},
  });

  return client;
}
