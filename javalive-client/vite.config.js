import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    lib: {
      entry: 'src/index.js',
      name: 'JavaLive',
      formats: ['es', 'umd'],
      fileName: (format) => format === 'es' ? 'javalive.esm.js' : 'javalive.js',
    },
    rollupOptions: {
      // Vue is a peer dependency — don't bundle it
      external: ['vue'],
      output: {
        globals: { vue: 'Vue' },
        // Ensure clean exports
        exports: 'named',
      },
    },
    // Target: all modern browsers + edge runtimes
    target: 'es2020',
    // Minify for production
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: false, // Keep console.* for debugging
        passes: 2,
      },
    },
    // Output to dist/ for Spring Boot static serving
    outDir: 'dist',
  },
  define: {
    __JAVALIVE_VERSION__: JSON.stringify('1.0.0'),
  },
  test: {
    environment: 'jsdom',
    globals: true,
  },
});
