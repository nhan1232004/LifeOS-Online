/* Shared data, formatting, and escaping utilities. Loaded before feature modules. */
(function () {
  const COLLECTIONS = ['events', 'todos', 'projects', 'proj_tasks', 'income', 'expense', 'notes', 'habits', 'goals', 'journal', 'vocab', 'mocktests', 'jars'];
  const isObject = value => value !== null && typeof value === 'object' && !Array.isArray(value);

  function localDate(value = new Date()) {
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
  }

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>'"]/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[char]);
  }

  function escapeAttr(value) { return escapeHtml(value); }

  function csvCell(value) {
    let text = String(value ?? '').replace(/[\r\n]+/g, ' ');
    // Prevent spreadsheet formula execution when a CSV is opened.
    if (/^[=+\-@]/.test(text)) text = "'" + text;
    return '"' + text.replace(/"/g, '""') + '"';
  }

  function normalize(data) {
    const source = isObject(data) ? data : {};
    const result = { ...source, schemaVersion: 2 };
    COLLECTIONS.forEach(name => {
      result[name] = Array.isArray(source[name]) ? source[name].filter(isObject) : [];
    });
    return result;
  }

  function validateBackup(data) {
    if (!isObject(data)) return { valid: false, error: 'Dữ liệu không phải là đối tượng JSON hợp lệ.' };
    const counts = {};
    let totalItems = 0;
    COLLECTIONS.forEach(col => {
      if (Array.isArray(data[col])) {
        const validItems = data[col].filter(item => isObject(item) && item.id != null && String(item.id).trim() !== '');
        counts[col] = validItems.length;
        totalItems += validItems.length;
      } else {
        counts[col] = 0;
      }
    });
    if (totalItems === 0) {
      return { valid: false, error: 'File sao lưu không chứa dữ liệu của bất kỳ danh mục nào thuộc LifeOS.' };
    }
    return { valid: true, counts, totalItems };
  }

  function mergeBackup(currentDb = {}, backupDb = {}) {
    const nextDb = { ...currentDb };
    COLLECTIONS.forEach(col => {
      const existing = Array.isArray(currentDb[col]) ? currentDb[col].filter(isObject) : [];
      const incoming = Array.isArray(backupDb[col]) ? backupDb[col].filter(isObject) : [];
      const existingIds = new Set(existing.map(x => String(x.id)));
      const newItems = incoming.filter(x => !existingIds.has(String(x.id)));
      nextDb[col] = [...existing, ...newItems];
    });
    return nextDb;
  }

  window.LifeOSData = { COLLECTIONS, localDate, escapeHtml, escapeAttr, csvCell, normalize, validateBackup, mergeBackup };
  window.toLocalDateStr = window.toLocalDateStr || localDate;
})();

