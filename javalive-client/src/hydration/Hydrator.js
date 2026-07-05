/**
 * Hydrator.js — Client-side hydration for JavaLive SSR
 *
 * When a page is served with embedded state (via SSR), this module:
 * 1. Reads the JSON hydration data from the <script id="javalive-state"> tag
 * 2. Pre-populates the StateStore with the server's state
 * 3. Initiates the WebSocket connection to the same server
 *
 * This prevents any visual "flash" — Vue mounts with state already present,
 * and the WebSocket connection takes over seamlessly.
 *
 * @module Hydrator
 */

export class Hydrator {
  /**
   * @param {import('../connection/LiveSocket.js').LiveSocket} socket
   * @param {import('../state/StateStore.js').StateStore} store
   */
  constructor(socket, store) {
    this.socket = socket;
    this.store  = store;
  }

  /**
   * Reads the hydration data embedded in the page and applies it.
   *
   * @returns {HydrationResult|null} the hydration data, or null if not found
   */
  hydrate() {
    const scriptEl = document.getElementById('javalive-state');
    if (!scriptEl) {
      // No SSR data — fresh mount, state will be loaded via WebSocket init
      return null;
    }

    let data;
    try {
      data = JSON.parse(scriptEl.textContent || scriptEl.innerHTML);
    } catch (e) {
      console.error('[JavaLive] Failed to parse hydration data:', e);
      return null;
    }

    const { component, state, sessionId, wsEndpoint } = data;

    if (component && state) {
      // Pre-populate the store with the server's initial state
      this.store.setInitialState(component, state);
      console.log(`[JavaLive] 💧 Hydrated component '${component}' with ${Object.keys(state).length} field(s)`);
    }

    // Expose session info globally for the router's auth guard
    if (sessionId) {
      window.__JAVALIVE_SESSION__ = sessionId;
    }

    return { component, state, sessionId, wsEndpoint };
  }

  /**
   * Checks whether SSR hydration data is available on this page.
   *
   * @returns {boolean}
   */
  hasHydrationData() {
    return document.getElementById('javalive-state') !== null;
  }
}
