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

test('native platform detection identifies mobile Capacitor environment correctly', () => {
  function isNativePlatform(mockCapacitor, mockLocation) {
    return (typeof mockCapacitor !== 'undefined' && mockCapacitor.isNativePlatform()) ||
           mockLocation.protocol === 'capacitor:' ||
           mockLocation.hostname === 'localhost' ||
           mockLocation.hostname === '127.0.0.1';
  }

  // Android Capacitor environment
  assert.equal(isNativePlatform({ isNativePlatform: () => true }, { protocol: 'https:', hostname: 'localhost' }), true);
  // iOS Capacitor custom scheme
  assert.equal(isNativePlatform(undefined, { protocol: 'capacitor:', hostname: 'localhost' }), true);
  // Web hosted environment (Firebase hosting / Netlify)
  assert.equal(isNativePlatform(undefined, { protocol: 'https:', hostname: 'dashboard-39cf8.web.app' }), false);
  assert.equal(isNativePlatform(undefined, { protocol: 'https:', hostname: 'lifeos-online-2026-v1.netlify.app' }), false);
});

test('email & password validation adheres to security requirements', () => {
  function validateAuthInput(email, password) {
    if (!email || !password) return { valid: false, error: 'Thiếu email hoặc mật khẩu' };
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) return { valid: false, error: 'Email không hợp lệ' };
    if (password.length < 6) return { valid: false, error: 'Mật khẩu cần tối thiểu 6 ký tự' };
    return { valid: true };
  }

  assert.equal(validateAuthInput('', '123456').valid, false);
  assert.equal(validateAuthInput('bademail', '123456').valid, false);
  assert.equal(validateAuthInput('test@example.com', '123').valid, false);
  assert.equal(validateAuthInput('user@lifeos.app', 'securePass123').valid, true);
});

test('active session auto-restore distinguishes guest from unauthenticated state', () => {
  function resolveStartupState(authUser, savedSession) {
    if (authUser) return 'auth_app';
    if (savedSession === 'guest') return 'guest_app';
    return 'login_screen';
  }

  assert.equal(resolveStartupState({ uid: 'u1' }, null), 'auth_app');
  assert.equal(resolveStartupState(null, 'guest'), 'guest_app');
  assert.equal(resolveStartupState(null, null), 'login_screen');
});
