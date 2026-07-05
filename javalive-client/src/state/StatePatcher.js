/**
 * StatePatcher.js — Applies server state diffs to Vue reactive objects
 *
 * Vue 3's Proxy-based reactivity requires specific operations to trigger
 * updates. This module handles applying server diffs in a reactivity-
 * compatible way.
 *
 * @module StatePatcher
 */

/**
 * Applies a state diff from the server to a Vue reactive object.
 *
 * Uses Vue's reactive mutation methods where needed to ensure
 * that all changes are properly tracked and trigger re-renders.
 *
 * @param {Object} reactiveState - The Vue reactive state object (from reactive())
 * @param {Object} diff - The diff from the server: { type, changed, removed }
 */
export function applyDiff(reactiveState, diff) {
  if (!diff || !reactiveState) return;

  if (diff.type === 'full') {
    // Full state replacement
    applyFullState(reactiveState, diff.changed || {});
  } else if (diff.type === 'patch') {
    // Partial update
    applyPatch(reactiveState, diff.changed || {}, diff.removed || []);
  }
  // Error and emit types don't affect state — handled by the component
}

/**
 * Replaces the entire reactive state with a new full state.
 *
 * @param {Object} reactiveState - Vue reactive object
 * @param {Object} newState - New complete state
 */
export function applyFullState(reactiveState, newState) {
  // Remove keys not in the new state
  const existingKeys = Object.keys(reactiveState);
  const newKeys = new Set(Object.keys(newState));

  existingKeys.forEach(key => {
    if (!newKeys.has(key)) {
      delete reactiveState[key];
    }
  });

  // Set all new values
  Object.entries(newState).forEach(([key, value]) => {
    reactiveState[key] = deepClone(value);
  });
}

/**
 * Applies a partial patch to the reactive state.
 *
 * @param {Object} reactiveState - Vue reactive object
 * @param {Object} changed - Map of changed key → new value
 * @param {string[]} removed - Keys to remove
 */
export function applyPatch(reactiveState, changed, removed) {
  // Apply changes
  Object.entries(changed).forEach(([key, value]) => {
    const existing = reactiveState[key];

    if (existing !== null && typeof existing === 'object'
        && value !== null && typeof value === 'object'
        && !Array.isArray(existing) && !Array.isArray(value)) {
      // Merge objects in place to preserve nested reactivity
      Object.assign(existing, value);
    } else {
      reactiveState[key] = deepClone(value);
    }
  });

  // Remove deleted keys
  removed.forEach(key => {
    delete reactiveState[key];
  });
}

/**
 * Deep-clones a value to prevent shared reference issues.
 * For primitives and null, returns the value as-is.
 *
 * @param {*} value - Value to clone
 * @returns {*} cloned value
 */
function deepClone(value) {
  if (value === null || typeof value !== 'object') return value;
  if (value instanceof Date) return new Date(value.getTime());
  if (Array.isArray(value)) return value.map(deepClone);
  return Object.fromEntries(
    Object.entries(value).map(([k, v]) => [k, deepClone(v)])
  );
}
