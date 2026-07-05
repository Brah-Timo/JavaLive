/**
 * LiveSocket.js — WebSocket connection manager for JavaLive
 *
 * Manages the STOMP WebSocket connection lifecycle:
 * - Connection establishment and reconnection
 * - Message sending with queuing for pre-connect messages
 * - Topic subscriptions with automatic re-subscription on reconnect
 * - Connection state tracking
 *
 * @module LiveSocket
 */

import { createStompClient } from './StompClient.js';

export class LiveSocket {
  /**
   * @param {string} url - WebSocket server URL
   * @param {import('../state/StateStore.js').StateStore} store - State store for applying diffs
   * @param {Object} [config] - Optional configuration
   */
  constructor(url, store, config = {}) {
    this.url    = url;
    this.store  = store;
    this.config = config;

    this._client       = null;
    this._connected    = false;
    this._subscriptions = new Map();   // topic → { subscription, callbacks[] }
    this._pendingQueue = [];           // messages queued before connection
    this._reconnectCount = 0;
    this._maxReconnectAttempts = config.maxReconnectAttempts ?? 10;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Connection
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Establishes the WebSocket connection.
   * @returns {Promise<void>} resolves when connected
   */
  connect() {
    return new Promise((resolve, reject) => {
      this._client = createStompClient(this.url, {
        onConnect: (frame) => {
          this._connected = true;
          this._reconnectCount = 0;

          // Re-subscribe to all previously registered topics
          this._resubscribeAll();

          // Flush any messages that were queued before connection
          this._flushQueue();

          resolve(frame);
        },

        onDisconnect: () => {
          this._connected = false;
          // STOMP client will auto-reconnect via reconnectDelay
        },

        onError: (frame) => {
          this._reconnectCount++;
          if (this._reconnectCount >= this._maxReconnectAttempts) {
            reject(new Error(`JavaLive: Failed to connect after ${this._maxReconnectAttempts} attempts`));
          }
        },
      }, this.config);

      this._client.activate();
    });
  }

  /**
   * Gracefully disconnects from the server.
   */
  disconnect() {
    if (this._client) {
      this._client.deactivate();
      this._connected = false;
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Messaging
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Sends a message to the server.
   *
   * If not yet connected, the message is queued and sent when the
   * connection is established.
   *
   * @param {string} destination - STOMP destination (without /app prefix)
   * @param {Object} body - Message body (will be JSON-serialized)
   * @returns {Promise<void>}
   */
  send(destination, body) {
    const fullDestination = destination.startsWith('/app')
      ? destination
      : `/app${destination}`;

    if (!this._connected || !this._client) {
      // Queue for later delivery
      return new Promise((resolve) => {
        this._pendingQueue.push({ destination: fullDestination, body, resolve });
      });
    }

    return this._doSend(fullDestination, body);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Subscriptions
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Subscribes to state updates for a specific component.
   *
   * Subscribes to both:
   * - `/user/topic/{componentName}.state` — session-specific patches
   * - `/topic/{componentName}.state.global` — global state broadcasts
   *
   * @param {string} componentName - Component name in kebab-case
   * @param {Function} callback - Called with (diff) on state update
   * @returns {Function} unsubscribe function
   */
  subscribeToComponent(componentName, callback) {
    const sessionTopic = `/user/topic/${componentName}.state`;
    const globalTopic  = `/topic/${componentName}.state.global`;

    const unsubSession = this._subscribe(sessionTopic, (message) => {
      this._handleMessage(componentName, message, callback);
    });

    const unsubGlobal = this._subscribe(globalTopic, (message) => {
      this._handleMessage(componentName, message, callback);
    });

    // Return combined unsubscribe function
    return () => {
      unsubSession();
      unsubGlobal();
    };
  }

  /**
   * Returns whether the WebSocket is currently connected.
   */
  isConnected() {
    return this._connected;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Private helpers
  // ─────────────────────────────────────────────────────────────────────────

  _doSend(destination, body) {
    return new Promise((resolve, reject) => {
      try {
        this._client.publish({
          destination,
          body: JSON.stringify(body),
          headers: { 'content-type': 'application/json' },
        });
        resolve();
      } catch (e) {
        reject(e);
      }
    });
  }

  _subscribe(topic, messageHandler) {
    if (!this._subscriptions.has(topic)) {
      this._subscriptions.set(topic, { subscription: null, callbacks: [] });
    }

    const entry = this._subscriptions.get(topic);
    entry.callbacks.push(messageHandler);

    // If already connected, subscribe immediately
    if (this._connected && this._client) {
      if (!entry.subscription) {
        entry.subscription = this._client.subscribe(topic, (msg) => {
          entry.callbacks.forEach(cb => cb(msg));
        });
      }
    }

    // Return unsubscribe function
    return () => {
      const idx = entry.callbacks.indexOf(messageHandler);
      if (idx >= 0) entry.callbacks.splice(idx, 1);
    };
  }

  _resubscribeAll() {
    for (const [topic, entry] of this._subscriptions.entries()) {
      if (entry.callbacks.length > 0) {
        entry.subscription = this._client.subscribe(topic, (msg) => {
          entry.callbacks.forEach(cb => cb(msg));
        });
      }
    }
  }

  _flushQueue() {
    const queue = [...this._pendingQueue];
    this._pendingQueue = [];

    for (const { destination, body, resolve } of queue) {
      this._doSend(destination, body).then(resolve);
    }
  }

  _handleMessage(componentName, stompMessage, callback) {
    try {
      const diff = JSON.parse(stompMessage.body);

      // Apply to state store
      if (this.store) {
        this.store.applyDiff(componentName, diff);
      }

      // Notify the component callback
      if (callback) {
        callback(diff);
      }
    } catch (e) {
      console.error('[JavaLive] Failed to parse server message:', e, stompMessage.body);
    }
  }
}
