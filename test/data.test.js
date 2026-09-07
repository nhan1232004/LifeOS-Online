const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function loadData() {
  const context = { window: {} };
  vm.runInNewContext(fs.readFileSync('js/data.js', 'utf8'), context);
  return context.window.LifeOSData;
}

test('normalization preserves valid collections and discards malformed collection values', () => {
  const data = loadData();
  const result = data.normalize({ todos: [{ id: 'a' }, null, 'bad'], custom: { keep: true } });
  assert.equal(result.schemaVersion, 2);
  assert.equal(JSON.stringify(result.todos), JSON.stringify([{ id: 'a' }]));
  assert.equal(JSON.stringify(result.events), '[]');
  assert.equal(result.custom.keep, true);
});

test('HTML and CSV values are escaped before rendering or export', () => {
  const data = loadData();
  assert.equal(data.escapeHtml('<img src=x onerror=1>'), '&lt;img src=x onerror=1&gt;');
  assert.equal(data.csvCell('=SUM(A1:A2)'), "'=SUM(A1:A2)".replace(/^/, '"').replace(/$/, '"'));
  assert.equal(data.csvCell('a"b'), '"a""b"');
});

test('local date uses the local calendar day rather than a UTC day boundary', () => {
  const data = loadData();
  const d = new Date(2026, 8, 8, 0, 30, 0);
  assert.match(data.localDate(d), /^2026-09-08$/);
});
