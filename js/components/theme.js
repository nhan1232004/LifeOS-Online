/**
 * LifeOS Online - Theme Manager
 * Standards: Dark, Light, System themes; LocalStorage persistence; Event dispatching
 */

(function () {
  const THEME_KEY = 'lifeos_theme';

  function getStoredTheme() {
    try {
      return localStorage.getItem(THEME_KEY) || 'dark';
    } catch (e) {
      return 'dark';
    }
  }

  function applyTheme(theme) {
    const root = document.documentElement;
    if (theme === 'system') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      root.setAttribute('data-theme', prefersDark ? 'dark' : 'light');
    } else {
      root.setAttribute('data-theme', theme);
    }

    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch (e) {}

    // Update meta theme-color for mobile browser header
    const metaThemeColor = document.getElementById('meta-theme-color');
    if (metaThemeColor) {
      const isLight = root.getAttribute('data-theme') === 'light';
      metaThemeColor.setAttribute('content', isLight ? '#F5F6FA' : '#0B0D14');
    }

    // Update theme toggle button icon if exists
    const btn = document.getElementById('themeBtn');
    if (btn) {
      const isLight = root.getAttribute('data-theme') === 'light';
      btn.innerHTML = isLight ? "<i data-lucide='moon' style='width:18px;height:18px'></i>" : "<i data-lucide='sun' style='width:18px;height:18px'></i>";
      if (window.lucide) window.lucide.createIcons();
    }

    if (typeof window.hapticTap === 'function') window.hapticTap();
    if (window.LifeOSMobile?.initNativeSystemBars) window.LifeOSMobile.initNativeSystemBars();

    // Dispatch event so charts/canvas can redraw with new CSS variable colors
    window.dispatchEvent(new CustomEvent('lifeos:themechange', { detail: { theme } }));
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme') || 'dark';
    const next = current === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    if (window.renderAll) window.renderAll();
    return next;
  }

  // System media query change listener
  if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
      const stored = getStoredTheme();
      if (stored === 'system') {
        applyTheme('system');
      }
    });
  }

  // Initial load
  const initialTheme = getStoredTheme();
  applyTheme(initialTheme);

  // Global APIs
  window.LifeOSTheme = {
    get: getStoredTheme,
    set: applyTheme,
    toggle: toggleTheme
  };
  window.setTheme = applyTheme;
  window.toggleTheme = toggleTheme;
})();
