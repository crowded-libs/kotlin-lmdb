// Webpack configuration to handle Node.js module polyfills for browser environment
// This addresses the "Module not found: Error: Can't resolve 'module'" issue

config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};

// Provide fallbacks for Node.js modules that aren't available in the browser
config.resolve.fallback.module = false;
config.resolve.fallback.fs = false;
config.resolve.fallback.path = false;
config.resolve.fallback.url = false;
config.resolve.fallback.util = false;
config.resolve.fallback.stream = false;
config.resolve.fallback.buffer = false;
config.resolve.fallback.process = false;

// Ignore webpack warnings about these modules
config.ignoreWarnings = config.ignoreWarnings || [];
config.ignoreWarnings.push(/Module not found: Error: Can't resolve 'module'/);
config.ignoreWarnings.push(/Module not found: Error: Can't resolve 'fs'/);
config.ignoreWarnings.push(/Module not found: Error: Can't resolve 'path'/);
config.ignoreWarnings.push(/Module not found: Error: Can't resolve 'url'/);
config.ignoreWarnings.push(/Module not found: Error: Can't resolve 'util'/);