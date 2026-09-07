/* Shared data, formatting, and escaping utilities. Loaded before feature modules. */
(function () {
  const COLLECTIONS = ['events', 'todos', 'projects', 'proj_tasks', 'income', 'expense', 'notes', 'habits', 'goals', 'journal', 'vocab', 'mocktests'];
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

  window.LifeOSData = { COLLECTIONS, localDate, escapeHtml, escapeAttr, csvCell, normalize };
  window.toLocalDateStr = window.toLocalDateStr || localDate;
})();
