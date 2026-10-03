const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const dist = path.join(root, 'dist');

// Clean dist
if (fs.existsSync(dist)) {
  fs.rmSync(dist, { recursive: true, force: true });
}
fs.mkdirSync(dist, { recursive: true });

// Files to copy
const files = ['index.html', 'manifest.json', 'icon.svg', 'sw.js'];
files.forEach(f => {
  const src = path.join(root, f);
  if (fs.existsSync(src)) {
    fs.copyFileSync(src, path.join(dist, f));
  }
});

// Directories to copy
const dirs = ['css', 'js'];
dirs.forEach(d => {
  const src = path.join(root, d);
  if (fs.existsSync(src)) {
    fs.cpSync(src, path.join(dist, d), { recursive: true });
  }
});

console.log('✅ LifeOS web assets built successfully into dist/');
