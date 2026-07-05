/**
 * JavaLive Client Runtime — Main Entry Point
 *
 * @version 1.0.0
 * @license Apache-2.0
 *
 * This module provides:
 *
 * 1. `initJavaLive(options)` — Bootstrap the entire JavaLive client
 * 2. `useLiveState(componentName, defaultState)` — Vue Composable for components
 * 3. `useJavaLive()` — Access the global JavaLive instance
 *
 * ─────────────────────────────────────────────────────────────
 * USAGE (in your main.js):
 * ─────────────────────────────────────────────────────────────
 * import { createApp } from 'vue';
 * import { initJavaLive } from '/javalive/runtime.js';
 *
 * initJavaLive({
 *   wsUrl: '/javalive-ws',
 *   mountEl: '#app',
 * });
 * ─────────────────────────────────────────────────────────────
 */

import { createApp, reactive, ref, onMounted, onUnmounted, computed } from 'vue';
import { LiveSocket } from './connection/LiveSocket.js';
import { StateStore } from './state/StateStore.js';
import { ComponentRegistry } from './components/ComponentRegistry.js';
import { Hydrator } from './hydration/Hydrator.js';
import { applyDiff } from './state/StatePatcher.js';

// ─── Singleton instances ───────────────────────────────────────────────────

/** @type {LiveSocket|null} */
let _socket = null;

/** @type {StateStore|null} */
let _store = null;

/** @type {ComponentRegistry|null} */
let _registry = null;

/** @type {boolean} */
let _initialized = false;

// ─────────────────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Initializes the JavaLive client runtime and mounts the Vue application.
 *
 * Call this once in your `main.js` (or `app.js`):
 *
 * @param {Object} options - Configuration options
 * @param {string}  [options.wsUrl='/javalive-ws'] - WebSocket server endpoint
 * @param {string}  [options.mountEl='#app'] - CSS selector for the Vue mount point
 * @param {Object}  [options.rootComponent] - Root Vue component (optional)
 * @param {boolean} [options.loadAllComponents=true] - Auto-load all generated components
 * @param {boolean} [options.debug=false] - Enable debug logging
 * @returns {Promise<{app, socket, store, registry}>}
 */
export async function initJavaLive(options = {}) {
  if (_initialized) {
    console.warn('[JavaLive] Already initialized. Call initJavaLive() only once.');
    return { socket: _socket, store: _store, registry: _registry };
  }

  const {
    wsUrl             = '/javalive-ws',
    mountEl           = '#app',
    rootComponent     = null,
    loadAllComponents = true,
    debug             = false,
  } = options;

  console.log('[JavaLive] 🚀 Initializing runtime v' + (__JAVALIVE_VERSION__ || '1.0.0'));

  // 1. Create the state store
  _store = new StateStore();

  // 2. Create the component registry
  _registry = new ComponentRegistry();

  // 3. Create the WebSocket connection
  _socket = new LiveSocket(wsUrl, _store, { debug });

  // 4. Read SSR hydration data (if any)
  const hydrator = new Hydrator(_socket, _store);
  const hydrationData = hydrator.hydrate();

  // 5. Create the Vue application
  const appComponent = rootComponent || {
    template: '<router-view v-if="isConnected" />' +
              '<div v-else class="javalive-connecting">Connecting...</div>',
    setup() {
      const isConnected = ref(false);
      _socket.connect().then(() => { isConnected.value = true; });
      return { isConnected };
    }
  };

  const app = createApp(appComponent);

  // 6. Load and register all generated components
  if (loadAllComponents) {
    await _registry.loadAllComponents();
  }
  _registry.registerAll(app);

  // 7. Try to load the generated router
  try {
    const routerModule = await import('/javalive/router.js');
    if (routerModule.router) {
      app.use(routerModule.router);
      console.log('[JavaLive] 🛣️  Vue Router loaded');
    }
  } catch {
    // No router — single-page app or manual routing
    console.log('[JavaLive] No auto-generated router found. Manual routing active.');
  }

  // 8. Connect to the server
  await _socket.connect();

  // 9. Mount the Vue app
  app.mount(mountEl);

  _initialized = true;
  console.log('[JavaLive] ✅ Mounted on', mountEl);

  // Expose globals for DevTools
  window.__JAVALIVE__ = { socket: _socket, store: _store, registry: _registry };

  return { app, socket: _socket, store: _store, registry: _registry };
}

/**
 * Returns the global JavaLive runtime instances.
 * Call after initJavaLive() has completed.
 *
 * @returns {{ socket: LiveSocket, store: StateStore, registry: ComponentRegistry }}
 * @throws {Error} if called before initJavaLive()
 */
export function useJavaLive() {
  if (!_initialized) {
    throw new Error('[JavaLive] Not initialized. Call initJavaLive() first.');
  }
  return { socket: _socket, store: _store, registry: _registry };
}

/**
 * Vue Composable — the heart of every generated JavaLive component.
 *
 * Used internally by every auto-generated Vue component:
 *
 * ```js
 * const { state, dispatch, isConnected, isLoading } = useLiveState('dashboard', {
 *   count: 0,
 *   title: ''
 * });
 * ```
 *
 * @param {string} componentName - The component's kebab-case name
 * @param {Object} defaultState - Default state values (used before server responds)
 * @returns {{ state, dispatch, isConnected, isLoading }}
 */
export function useLiveState(componentName, defaultState = {}) {
  // ── Reactive state ────────────────────────────────────────────────────────
  //
  // We use Vue's reactive() here so the entire state object is deeply reactive.
  // The StatePatcher applies diffs in a way that Vue's Proxy can detect.
  //
  const state = reactive({ ...defaultState });

  // If we have hydration data for this component, pre-populate
  if (_store) {
    const hydratedState = _store.getState(componentName);
    if (hydratedState && Object.keys(hydratedState).length > 0) {
      Object.assign(state, hydratedState);
    }
  }

  /** Whether the WebSocket is currently connected. */
  const isConnected = ref(_socket ? _socket.isConnected() : false);

  /** Whether a @VueMethod(loading=true) call is in progress. */
  const isLoading = ref(false);

  /** The last error received from the server (null if no error). */
  const lastError = ref(null);

  // ── Subscribe to state updates ────────────────────────────────────────────

  let unsubscribe = () => {};

  onMounted(async () => {
    // Update connection status
    isConnected.value = _socket ? _socket.isConnected() : false;

    if (_socket && _store) {
      // Subscribe to this component's state channel
      unsubscribe = _socket.subscribeToComponent(componentName, (diff) => {
        if (diff.type === 'error') {
          lastError.value = diff.error;
          return;
        }
        if (diff.type === 'emit') {
          // Vue event emission — handled by individual component wrappers
          return;
        }
        // Apply diff to our reactive state
        applyDiff(state, diff);
        lastError.value = null;
      });

      // Request initial state from the server
      await _socket.send(`/${componentName}.init`, {
        componentName,
        currentState: { ...state },
      });
    }
  });

  onUnmounted(() => {
    // Clean up subscription
    unsubscribe();

    // Notify server that this component was unmounted
    if (_socket && _socket.isConnected()) {
      _socket.send(`/${componentName}.unmount`, {}).catch(() => {});
    }
  });

  // ── Dispatch function ─────────────────────────────────────────────────────

  /**
   * Sends a @VueMethod call to the server.
   *
   * @param {string} methodName - The Java method name
   * @param {Array}  [args=[]]  - Arguments to pass
   * @returns {Promise<void>}
   */
  const dispatch = async (methodName, args = []) => {
    if (!_socket) {
      console.error('[JavaLive] Cannot dispatch: not initialized');
      return;
    }

    if (!_socket.isConnected()) {
      console.warn('[JavaLive] Cannot dispatch: WebSocket not connected');
      return;
    }

    return _socket.send(`/${componentName}.${methodName}`, {
      method: methodName,
      args,
      currentState: { ...state },
    });
  };

  // ── Debounce utility for @VueWatch and @VueMethod(debounce=N) ─────────────

  return {
    /** The reactive state object — access fields as state.count, state.users, etc. */
    state,
    /** Dispatch a @VueMethod call to the server */
    dispatch,
    /** Whether WebSocket is connected */
    isConnected,
    /** Whether a loading-indicator method is running */
    isLoading,
    /** Last error from the server (null if OK) */
    lastError,
  };
}

/**
 * Debounce utility — used by generated @VueMethod(debounce=N) wrappers.
 *
 * @param {Function} fn - Function to debounce
 * @param {number} delay - Delay in milliseconds
 * @returns {Function} debounced function
 */
export function debounce(fn, delay) {
  let timer = null;
  return function (...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// Auto-bootstrap from HTML data attributes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Auto-bootstraps JavaLive if a <div id="app" data-javalive> element is found.
 *
 * This allows zero-config usage: just include the runtime script and add
 * data-javalive to your mount element.
 *
 * <div id="app" data-javalive data-ws-url="/javalive-ws"></div>
 */
if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  document.addEventListener('DOMContentLoaded', () => {
    const mountEl = document.querySelector('[data-javalive]');
    if (mountEl) {
      const wsUrl  = mountEl.dataset.wsUrl  || '/javalive-ws';
      const debug  = mountEl.dataset.debug === 'true';
      initJavaLive({
        wsUrl,
        mountEl: '#' + (mountEl.id || 'app'),
        debug,
      }).catch(e => console.error('[JavaLive] Auto-bootstrap failed:', e));
    }
  });
}
