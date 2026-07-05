/**
 * ComponentRegistry.js — Manages and registers generated Vue components
 *
 * Loads all JavaLive-generated Vue components and registers them
 * globally on the Vue application instance so they can be used
 * in any template without explicit imports.
 *
 * @module ComponentRegistry
 */

export class ComponentRegistry {
  constructor() {
    /** componentName → component definition */
    this._components = new Map();
  }

  /**
   * Registers a component definition manually.
   *
   * @param {string} name - Component name in kebab-case
   * @param {Object} definition - Vue component definition object
   */
  register(name, definition) {
    this._components.set(name, definition);
  }

  /**
   * Registers all components on a Vue application instance.
   * Call this during app setup, before app.mount().
   *
   * @param {Object} vueApp - Vue application instance from createApp()
   */
  registerAll(vueApp) {
    for (const [name, definition] of this._components.entries()) {
      vueApp.component(name, definition);
    }
    console.log(`[JavaLive] Registered ${this._components.size} component(s)`);
  }

  /**
   * Dynamically loads a generated component from the server's static files.
   *
   * Generated components are served at: /javalive/components/{name}.js
   * This function fetches and evaluates the module.
   *
   * @param {string} componentName - Component name in kebab-case
   * @returns {Promise<Object>} the component definition
   */
  async loadComponent(componentName) {
    if (this._components.has(componentName)) {
      return this._components.get(componentName);
    }

    try {
      // Dynamic import of generated component
      const module = await import(`/javalive/components/${componentName}.js`);
      const definition = module.default;
      this.register(componentName, definition);
      return definition;
    } catch (e) {
      console.error(`[JavaLive] Failed to load component '${componentName}':`, e);
      throw e;
    }
  }

  /**
   * Loads all components listed in the generated schemas manifest.
   *
   * @returns {Promise<void>}
   */
  async loadAllComponents() {
    try {
      // Fetch the manifest of all generated components
      const response = await fetch('/javalive/components/manifest.json');
      if (!response.ok) {
        console.warn('[JavaLive] No component manifest found. Components must be loaded manually.');
        return;
      }

      const manifest = await response.json();
      const loadPromises = manifest.components.map(name => this.loadComponent(name));
      await Promise.allSettled(loadPromises);

      console.log(`[JavaLive] Loaded ${this._components.size} component(s) from manifest`);
    } catch (e) {
      console.warn('[JavaLive] Could not load component manifest:', e.message);
    }
  }

  /**
   * Returns all registered component names.
   */
  getRegisteredNames() {
    return Array.from(this._components.keys());
  }
}
