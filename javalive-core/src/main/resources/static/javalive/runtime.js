/**
 * JavaLive Runtime v0.1.0-alpha
 *
 * Self-contained STOMP + Vue 3 composable.
 * Served at: /javalive/runtime.js
 *
 * Every auto-generated component does:
 *   import { useLiveState } from '/javalive/runtime.js';
 *
 * This file has zero npm dependencies — it uses Vue 3 and SockJS from the
 * same CDN that the SPA shell imports. The global `StompJs` object is injected
 * by the shell's <script> tag before any component module is loaded.
 *
 * @license Apache-2.0
 */

// ─── Vue 3 — resolve from the global already loaded by the SPA shell ─────────
import { reactive, ref, onMounted, onUnmounted } from 'vue';

// ─── @stomp/stompjs — resolved from globalThis (loaded by shell via CDN) ─────
// The SPA shell does:  <script src="https://cdn.jsdelivr.net/...stomp.umd.min.js">
// which puts `StompJs` on window.
const StompJs = globalThis.StompJs;

// ─────────────────────────────────────────────────────────────────────────────
// Module-level singleton socket shared by ALL components on the page.
// This way a single WebSocket connection serves every component.
// ─────────────────────────────────────────────────────────────────────────────

/** @type {import('@stomp/stompjs').Client|null} */
let _client = null;

/** @type {boolean} */
let _connected = false;

/** @type {boolean} */
let _connecting = false;

/** Re-subscription callbacks registered while connecting */
const _subscriptionQueue = [];

/** callbacks registered before connection: [{topic, callback}] */
const _pendingSubscriptions = [];

/** STOMP subscription objects keyed by topic */
const _subscriptions = new Map();

/** Callbacks for each topic: topic → Set<Function> */
const _callbacks = new Map();

/** Messages queued while not yet connected */
const _sendQueue = [];

/** Reactive connection status — components can watch this */
export const connectionStatus = ref('disconnected');

// ─────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

function _getOrCreateCallbacks(topic) {
  if (!_callbacks.has(topic)) _callbacks.set(topic, new Set());
  return _callbacks.get(topic);
}

function _doSubscribe(topic) {
  if (_subscriptions.has(topic)) return; // already subscribed
  const sub = _client.subscribe(topic, (frame) => {
    const cbs = _callbacks.get(topic);
    if (!cbs) return;
    let msg;
    try { msg = JSON.parse(frame.body); } catch { return; }
    cbs.forEach(cb => { try { cb(msg); } catch (e) { console.error('[JavaLive]', e); } });
  });
  _subscriptions.set(topic, sub);
}

function _resubscribeAll() {
  for (const topic of _callbacks.keys()) {
    _doSubscribe(topic);
  }
}

function _flushSendQueue() {
  const queued = [..._sendQueue];
  _sendQueue.length = 0;
  queued.forEach(({ destination, body, resolve, reject }) => {
    _rawSend(destination, body).then(resolve).catch(reject);
  });
}

function _rawSend(destination, body) {
  return new Promise((resolve, reject) => {
    try {
      _client.publish({
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

// ─────────────────────────────────────────────────────────────────────────────
// Connection bootstrap
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Initialises (or returns already-active) STOMP connection.
 * Safe to call multiple times — only one connection is ever created.
 *
 * @param {string} [wsUrl='/javalive-ws'] - SockJS endpoint path
 * @returns {Promise<void>}
 */
export function initSocket(wsUrl) {
  wsUrl = wsUrl || '/javalive-ws';
  if (_client && (_connected || _connecting)) return Promise.resolve();

  _connecting = true;
  connectionStatus.value = 'connecting';

  return new Promise((resolve, reject) => {
    const clientConfig = {
      // Reconnect automatically
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect() {
        _connected   = true;
        _connecting  = false;
        connectionStatus.value = 'connected';
        console.log('[JavaLive] ✅ WebSocket connected');
        _resubscribeAll();
        _flushSendQueue();
        resolve();
      },

      onDisconnect() {
        _connected  = false;
        connectionStatus.value = 'reconnecting';
        console.warn('[JavaLive] ⚡ WebSocket disconnected — will retry');
      },

      onStompError(frame) {
        _connecting = false;
        connectionStatus.value = 'error';
        console.error('[JavaLive] STOMP error:', frame.headers?.message);
        reject(new Error(frame.headers?.message || 'STOMP error'));
      },

      debug: () => {},
    };

    // Use SockJS if available (server uses .withSockJS()), else fall back to
    // native WebSocket (useful in integration tests / non-browser environments).
    if (typeof globalThis.SockJS !== 'undefined') {
      clientConfig.webSocketFactory = () => new globalThis.SockJS(wsUrl);
    } else {
      const wsProto = wsUrl.startsWith('https') ? 'wss' : 'ws';
      const wsWsUrl = wsUrl.replace(/^https?/, wsProto);
      clientConfig.brokerURL = wsWsUrl;
    }

    _client = new StompJs.Client(clientConfig);
    _client.activate();
  });
}

/**
 * Send a STOMP message (queued if not yet connected).
 *
 * @param {string} destination - e.g. '/counter-widget.increment' (prefix /app is added)
 * @param {Object} body
 * @returns {Promise<void>}
 */
export function send(destination, body) {
  const fullDest = destination.startsWith('/app') ? destination : `/app${destination}`;
  if (_connected && _client) {
    return _rawSend(fullDest, body);
  }
  return new Promise((resolve, reject) => {
    _sendQueue.push({ destination: fullDest, body, resolve, reject });
  });
}

/**
 * Subscribe to a STOMP topic.
 *
 * @param {string} topic
 * @param {Function} callback - called with parsed JSON message
 * @returns {Function} unsubscribe
 */
export function subscribe(topic, callback) {
  const cbs = _getOrCreateCallbacks(topic);
  cbs.add(callback);

  if (_connected && _client) {
    _doSubscribe(topic);
  }
  // If not yet connected, _resubscribeAll() will handle it onConnect

  return function unsubscribe() {
    cbs.delete(callback);
    if (cbs.size === 0) {
      const sub = _subscriptions.get(topic);
      if (sub) { try { sub.unsubscribe(); } catch {} }
      _subscriptions.delete(topic);
      _callbacks.delete(topic);
    }
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// Debounce utility (exported so generated components can use it directly)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns a debounced version of `fn`.
 *
 * @param {Function} fn
 * @param {number} delay - milliseconds
 * @returns {Function}
 */
export function debounce(fn, delay) {
  let timer = null;
  return function (...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// applyDiff — applies a ServerMessage to a Vue reactive() object
// ─────────────────────────────────────────────────────────────────────────────

function _deepClone(v) {
  if (v === null || typeof v !== 'object') return v;
  if (Array.isArray(v)) return v.map(_deepClone);
  if (v instanceof Date) return new Date(v.getTime());
  return Object.fromEntries(Object.entries(v).map(([k, val]) => [k, _deepClone(val)]));
}

function applyDiff(state, diff) {
  if (!diff) return;
  const { type, changed = {}, removed = [] } = diff;

  if (type === 'full') {
    // Replace everything
    for (const key of Object.keys(state)) {
      if (!(key in changed)) delete state[key];
    }
    for (const [k, v] of Object.entries(changed)) {
      state[k] = _deepClone(v);
    }
  } else if (type === 'patch') {
    for (const [k, v] of Object.entries(changed)) {
      const cur = state[k];
      if (cur !== null && typeof cur === 'object' && !Array.isArray(cur)
          && v  !== null && typeof v  === 'object' && !Array.isArray(v)) {
        Object.assign(cur, v);
      } else {
        state[k] = _deepClone(v);
      }
    }
    for (const k of removed) delete state[k];
  }
  // 'error', 'emit', 'reload' are handled by the component callback
}

// ─────────────────────────────────────────────────────────────────────────────
// useLiveState — the heart of every generated JavaLive component
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Vue 3 Composable — binds a component to its Java server-side state.
 *
 * Usage (auto-generated by the processor):
 * ```js
 * const { state, dispatch, isConnected, isLoading } = useLiveState('counter-widget', {
 *   count: 0,
 *   step: 1,
 * });
 * ```
 *
 * @param {string} componentName - kebab-case name matching @VueComponent value
 * @param {Object} [defaultState={}] - initial client-side defaults
 * @returns {{ state, dispatch, isConnected, isLoading, lastError }}
 */
export function useLiveState(componentName, defaultState) {
  defaultState = defaultState || {};

  // ── Reactive state ─────────────────────────────────────────────────────────
  const state       = reactive({ ...defaultState });
  const isConnected = ref(_connected);
  const isLoading   = ref(false);
  const lastError   = ref(null);

  // ── Subscription topics ────────────────────────────────────────────────────
  const sessionTopic = `/user/topic/${componentName}.state`;
  const globalTopic  = `/topic/${componentName}.state.global`;

  // ── Lifecycle ──────────────────────────────────────────────────────────────
  let unsubSession = () => {};
  let unsubGlobal  = () => {};

  // Track connection status changes
  let unsubStatus = () => {};

  onMounted(async () => {
    // Mirror global connection status into per-component ref
    isConnected.value = _connected;

    // Watch global status
    const stopWatch = (() => {
      const interval = setInterval(() => {
        isConnected.value = _connected;
      }, 500);
      return () => clearInterval(interval);
    })();
    unsubStatus = stopWatch;

    // Subscribe for this component's state patches
    unsubSession = subscribe(sessionTopic, (diff) => {
      if (diff.type === 'error') {
        lastError.value = diff.error || 'Unknown error';
        return;
      }
      if (diff.type === 'reload') {
        window.location.reload();
        return;
      }
      applyDiff(state, diff);
      lastError.value = null;
    });

    unsubGlobal = subscribe(globalTopic, (diff) => {
      applyDiff(state, diff);
    });

    // Ensure socket is connected, then request initial state
    try {
      await initSocket();
      isConnected.value = true;
      await send(`/${componentName}.init`, {
        componentName,
        currentState: { ...state },
      });
    } catch (e) {
      console.error(`[JavaLive] Failed to init component '${componentName}':`, e);
      lastError.value = e.message;
    }
  });

  onUnmounted(() => {
    unsubSession();
    unsubGlobal();
    unsubStatus();

    // Politely tell the server we unmounted
    if (_connected) {
      send(`/${componentName}.unmount`, {}).catch(() => {});
    }
  });

  // ── dispatch ───────────────────────────────────────────────────────────────

  /**
   * Dispatches a @VueMethod call to the server.
   *
   * @param {string} methodName - Java method name (as declared on @VueMethod)
   * @param {Array}  [args=[]]
   * @returns {Promise<void>}
   */
  async function dispatch(methodName, args) {
    args = args || [];
    if (!_connected) {
      console.warn(`[JavaLive] dispatch('${methodName}') called while disconnected — queued`);
    }
    return send(`/${componentName}.${methodName}`, {
      method: methodName,
      args,
      currentState: { ...state },
    });
  }

  return { state, dispatch, isConnected, isLoading, lastError };
}

// ─────────────────────────────────────────────────────────────────────────────
// Named re-exports so the generated components can tree-shake what they need
// ─────────────────────────────────────────────────────────────────────────────
export { applyDiff };
