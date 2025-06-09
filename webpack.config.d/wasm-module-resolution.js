// Webpack configuration to handle WASM module resolution
// This ensures that WASM modules are properly loaded from resources

// Configure module resolution
config.resolve = config.resolve || {};
config.resolve.extensions = config.resolve.extensions || [];

// Add .mjs extension if not already present
if (!config.resolve.extensions.includes('.mjs')) {
    config.resolve.extensions.push('.mjs');
}

// Configure module rules for WASM files
config.module = config.module || {};
config.module.rules = config.module.rules || [];

// Add rule for WASM files
config.module.rules.push({
    test: /\.wasm$/,
    type: 'webassembly/async'
});

// Add rule for .mjs files
config.module.rules.push({
    test: /\.mjs$/,
    type: 'javascript/auto',
    resolve: {
        fullySpecified: false
    }
});

// Configure experiments for WebAssembly
config.experiments = config.experiments || {};
config.experiments.asyncWebAssembly = true;

// Add aliases for WASM modules to resolve them correctly
config.resolve.alias = config.resolve.alias || {};

// Use try-catch to handle cases where files might not exist yet
try {
    const path = require('path');
    const rootDir = path.resolve(__dirname, '..');
    
    // Try to resolve WASM modules
    ['lmdb-wrapper.mjs', 'lmdb.mjs', 'lmdb.wasm'].forEach(file => {
        const filePath = path.join(rootDir, file);
        try {
            require.resolve(filePath);
            config.resolve.alias['./' + file] = filePath;
        } catch (e) {
            // File might not exist yet during initial configuration
            console.log(`Note: ${file} not found at ${filePath}, will be copied during build`);
        }
    });
} catch (e) {
    console.log('Unable to set up WASM module aliases:', e.message);
}

// Add fallback for Node.js modules in browser
config.resolve.fallback = config.resolve.fallback || {};
config.resolve.fallback.fs = false;
config.resolve.fallback.path = false;

// Ensure WASM files are properly handled as assets
const wasmAssetRule = config.module.rules.find(rule => 
    rule.type === 'asset/resource' && 
    rule.test && rule.test.toString().includes('wasm')
);

if (!wasmAssetRule) {
    config.module.rules.push({
        test: /\.(wasm)$/,
        type: 'asset/resource',
        generator: {
            filename: '[name][ext]'
        }
    });
}

// Log webpack config for debugging (can be removed in production)
if (process.env.DEBUG_WEBPACK) {
    console.log('Webpack configuration:', JSON.stringify(config, null, 2));
}