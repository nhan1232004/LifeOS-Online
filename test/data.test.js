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

test('validateBackup verifies collections and rejects invalid payloads', () => {
  const data = loadData();
  assert.equal(data.validateBackup(null).valid, false);
  assert.equal(data.validateBackup('not json').valid, false);
  assert.equal(data.validateBackup({}).valid, false);
  assert.equal(data.validateBackup({ todos: [] }).valid, false);

  const valid = data.validateBackup({
    todos: [{ id: 't1', text: 'Task 1' }, { id: ' ' }],
    projects: [{ id: 'p1', name: 'Project 1' }],
    notes: [{ id: 'n1', title: 'Note 1' }]
  });
  assert.equal(valid.valid, true);
  assert.equal(valid.counts.todos, 1);
  assert.equal(valid.counts.projects, 1);
  assert.equal(valid.counts.notes, 1);
  assert.equal(valid.totalItems, 3);
});

test('mergeBackup preserves existing items and appends new incoming items without duplicate IDs', () => {
  const data = loadData();
  const currentDb = {
    todos: [{ id: 't1', text: 'Original Task 1' }, { id: 't2', text: 'Task 2' }]
  };
  const incomingDb = {
    todos: [{ id: 't1', text: 'Overwritten Task 1' }, { id: 't3', text: 'New Task 3' }]
  };
  const merged = data.mergeBackup(currentDb, incomingDb);
  assert.equal(merged.todos.length, 3);
  assert.equal(merged.todos[0].text, 'Original Task 1');
  assert.equal(merged.todos[2].id, 't3');
});

