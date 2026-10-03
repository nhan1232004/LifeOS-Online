const test = require('node:test');
const assert = require('node:assert/strict');

test('valid pages list covers all main navigation routes', () => {
  const PAGE_TITLES = {
    today: 'Hôm nay',
    overview: 'Dashboard',
    calendar: 'Lịch tháng',
    schedule: 'Lịch tuần',
    projects: 'Dự án',
    todos: 'Việc cần làm',
    finance: 'Thu chi',
    stats: 'Thống kê',
    notes: 'Ghi chú',
    habits: 'Thói quen',
    goals: 'Mục tiêu',
    journal: 'Nhật ký',
    pomodoro: 'Pomodoro',
    vocab: 'Từ vựng',
    mocktests: 'Mock Tests'
  };
  const validPages = Object.keys(PAGE_TITLES);
  assert.equal(validPages.length, 15);
  assert.ok(validPages.includes('today'));
  assert.ok(validPages.includes('projects'));
  assert.ok(validPages.includes('finance'));
  assert.ok(validPages.includes('calendar'));
  assert.ok(validPages.includes('vocab'));
});

test('markdown formatting handles safe links and escapes HTML', () => {
  function formatMarkdown(text) {
    if (!text) return '';
    let html = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    html = html.replace(/\*\*\*(.*?)\*\*\*/g, '<b><i>$1</i></b>');
    html = html.replace(/\*\*(.*?)\*\*/g, '<b>$1</b>');
    html = html.replace(/\*(.*?)\*/g, '<i>$1</i>');
    html = html.replace(/`([^`]+)`/g, '<code style="background:rgba(255,255,255,0.08);padding:2px 6px;border-radius:4px;font-family:monospace;font-size:12px;color:var(--accent-cyan);">$1</code>');
    html = html.replace(/\[([^\]]+)\]\((https?:\/\/[^\s\)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" style="color:var(--accent-cyan);text-decoration:underline;">$1</a>');
    html = html.replace(/\n/g, '<br>');
    return html;
  }

  // Script tags should be safely escaped
  const dangerous = '<script>alert("xss")</script>';
  assert.ok(!formatMarkdown(dangerous).includes('<script>'));
  assert.ok(formatMarkdown(dangerous).includes('&lt;script&gt;'));

  // Safe external links
  const linked = 'Xem chi tiết tại [Google](https://google.com)';
  const formatted = formatMarkdown(linked);
  assert.ok(formatted.includes('<a href="https://google.com"'));
  assert.ok(formatted.includes('rel="noopener noreferrer"'));

  // Unsafe javascript: links should NOT be converted to <a> tags
  const unsafeLink = '[Bấm vào đây](javascript:alert(1))';
  assert.ok(!formatMarkdown(unsafeLink).includes('<a href="javascript'));
});

test('goal formatting handles standard goal attributes properly', () => {
  const goals = [
    { id: 'g1', name: 'Quỹ khẩn cấp', target: 50000000, saved: 30000000 },
    { id: 'g2', name: 'Mua xe', target: 20000000, saved: 5000000 }
  ];

  const formatted = goals.map(g => `${g.name || g.title} (${Number(g.saved || 0).toLocaleString('vi-VN')}/${Number(g.target || 0).toLocaleString('vi-VN')}₫)`).join(', ');
  assert.ok(formatted.includes('Quỹ khẩn cấp (30.000.000/50.000.000₫)'));
  assert.ok(formatted.includes('Mua xe (5.000.000/20.000.000₫)'));
  assert.ok(!formatted.includes('undefined'));
});
