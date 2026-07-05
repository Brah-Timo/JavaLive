/**
 * StateStore.js — Client-side reactive state store for JavaLive
 *
 * Manages the reactive state for all mounted Vue components.
 * Acts as the single source of truth on the client side,
 * synchronized with the server via WebSocket diffs.
 *
 * This is intentionally a lightweight Pinia-alternative —
 * no external store library needed.
 *
 * @module StateStore
 */

export class StateStore {
  constructor() {
    /**
     * Component states and listener sets.
     * componentName → { state: Object, listeners: Set<Function> }
     * @private
     */
    this._components = new Map();

    /**
     * History of all received diffs (for DevTools).
     * @private
     */
    this._history = [];
    this._historyMaxSize = 100;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Subscriptions
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Subscribes a listener to state changes for a specific component.
   *
   * @param {string} componentName - Component name in kebab-case
   * @param {Function} listener - Called with (diff) when state changes
   * @returns {Function} unsubscribe function — call to stop receiving updates
   */
  subscribe(componentName, listener) {
    this._ensureComponent(componentName);
    this._components.get(componentName).listeners.add(listener);

    return () => {
      const component = this._components.get(componentName);
      if (component) {
        component.listeners.delete(listener);
      }
    };
  }

  // ─────────────────────────────────────────────────────────────────────────
  // State application
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Applies a state diff received from the server.
   *
   * @param {string} componentName - Target component
   * @param {Object} diff - The diff object: { type, changed, removed }
   */
  applyDiff(componentName, diff) {
    this._ensureComponent(componentName);
    const component = this._components.get(componentName);

    if (diff.type === 'full') {
      // Replace entire state
      Object.keys(component.state).forEach(k => delete component.state[k]);
      Object.assign(component.state, diff.changed || {});
    } else if (diff.type === 'patch') {
      // Apply only changed keys
      if (diff.changed) {
        Object.assign(component.state, diff.changed);
      }
      // Remove deleted keys
      if (diff.removed) {
        diff.removed.forEach(key => delete component.state[key]);
      }
    } else if (diff.type === 'error') {
      // Error from server — emit to all listeners for UI feedback
      console.error(`[JavaLive] Server error in ${componentName}:`, diff.error);
    }

    // Notify all listeners
    component.listeners.forEach(listener => {
      try {
        listener(diff);
      } catch (e) {
        console.error('[JavaLive] StateStore listener error:', e);
      }
    });

    // Record in history (DevTools)
    this._recordHistory(componentName, diff);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // State access
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Returns the current state for a component.
   *
   * @param {string} componentName - Component name
   * @returns {Object} the current state (mutable reference — use with care)
   */
  getState(componentName) {
    this._ensureComponent(componentName);
    return this._components.get(componentName).state;
  }

  /**
   * Sets the initial state for a component (used at hydration time).
   *
   * @param {string} componentName - Component name
   * @param {Object} initialState - Initial state values
   */
  setInitialState(componentName, initialState) {
    this._ensureComponent(componentName);
    const component = this._components.get(componentName);
    Object.assign(component.state, initialState);
  }

  /**
   * Returns the number of components currently tracked.
   */
  getComponentCount() {
    return this._components.size;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // DevTools support
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Returns the diff history for DevTools inspection.
   */
  getHistory() {
    return [...this._history];
  }

  /**
   * Returns a snapshot of all component states (for DevTools).
   */
  getSnapshot() {
    const snapshot = {};
    for (const [name, component] of this._components.entries()) {
      snapshot[name] = { ...component.state };
    }
    return snapshot;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Private helpers
  // ─────────────────────────────────────────────────────────────────────────

  _ensureComponent(componentName) {
    if (!this._components.has(componentName)) {
      this._components.set(componentName, {
        state: {},
        listeners: new Set(),
      });
    }
  }

  _recordHistory(componentName, diff) {
    this._history.push({
      ts: Date.now(),
      component: componentName,
      diff,
    });
    // Keep history bounded
    if (this._history.length > this._historyMaxSize) {
      this._history.shift();
    }
  }
}
