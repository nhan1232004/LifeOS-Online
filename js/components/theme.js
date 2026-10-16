/**
 * LifeOS Online - Theme & Dynamic Accent Manager
 * Standards: Dark, Light, System themes; 6 Accent Presets + Custom Hue; LocalStorage persistence; Event dispatching
 */

(function () {
  const THEME_KEY = 'lifeos_theme';
  const ACCENT_KEY = 'lifeos_accent';
  const ACCENT_HUE_KEY = 'lifeos_accent_hue';

  const ACCENT_PRESETS = {
    'apple-blue': { name: 'Apple Blue', hex: '#0A84FF', hue: 215 },
    'linear-violet': { name: 'Linear Violet', hex: '#8B7CFF', hue: 264 },
    'emerald-mint': { name: 'Emerald Forest', hex: '#10B981', hue: 155 },
    'sunset-amber': { name: 'Sunset Amber', hex: '#FF9F0A', hue: 35 },
    'neon-rose': { name: 'Neon Rose', hex: '#FF375F', hue: 345 },
    'titanium-stealth': { name: 'Titanium Stealth', hex: '#98989D', hue: 240 }
  };

  function getStoredTheme() {
    try {
      return localStorage.getItem(THEME_KEY) || 'dark';
    } catch (e) {
      return 'dark';
    }
  }

  function getStoredAccent() {
    try {
      return localStorage.getItem(ACCENT_KEY) || 'apple-blue';
    } catch (e) {
      return 'apple-blue';
    }
  }

  function getStoredAccentHue() {
    try {
      return localStorage.getItem(ACCENT_HUE_KEY) || '215';
    } catch (e) {
      return '215';
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

  function applyAccent(presetKey, customHue) {
    const root = document.documentElement;
    let effectivePreset = presetKey;
    let hue = customHue;

    if (presetKey === 'custom' && hue !== undefined) {
      root.setAttribute('data-accent', 'custom');
      root.style.setProperty('--accent-hue', hue);
      root.style.setProperty('--accent', `hsl(${hue}, 95%, 60%)`);
      root.style.setProperty('--accent-hover', `hsl(${hue}, 95%, 68%)`);
      root.style.setProperty('--accent-active', `hsl(${hue}, 95%, 50%)`);
      root.style.setProperty('--accent-soft', `hsla(${hue}, 95%, 60%, 0.18)`);
      root.style.setProperty('--accent-glow', `hsla(${hue}, 95%, 60%, 0.38)`);
      root.style.setProperty('--accent-border', `hsla(${hue}, 95%, 60%, 0.45)`);
      root.style.setProperty('--accent-rim', `hsla(${hue}, 95%, 60%, 0.65)`);
      effectivePreset = 'custom';
      try {
        localStorage.setItem(ACCENT_KEY, 'custom');
        localStorage.setItem(ACCENT_HUE_KEY, String(hue));
      } catch (e) {}
    } else if (ACCENT_PRESETS[presetKey]) {
      // Clear custom properties so preset CSS definitions take over
      root.style.removeProperty('--accent-hue');
      root.style.removeProperty('--accent');
      root.style.removeProperty('--accent-hover');
      root.style.removeProperty('--accent-active');
      root.style.removeProperty('--accent-soft');
      root.style.removeProperty('--accent-glow');
      root.style.removeProperty('--accent-border');
      root.style.removeProperty('--accent-rim');
      root.setAttribute('data-accent', presetKey);
      effectivePreset = presetKey;
      hue = ACCENT_PRESETS[presetKey].hue;
      try {
        localStorage.setItem(ACCENT_KEY, presetKey);
        localStorage.setItem(ACCENT_HUE_KEY, String(hue));
      } catch (e) {}
    } else {
      effectivePreset = 'apple-blue';
      root.setAttribute('data-accent', 'apple-blue');
    }

    // Update active swatch indicator in settings or quick palette if rendered
    updateAccentSwatchesUI(effectivePreset, hue);

    if (typeof window.hapticTap === 'function') window.hapticTap();

    // Dispatch event so charts/canvas can redraw with new CSS variable colors
    window.dispatchEvent(new CustomEvent('lifeos:accentchange', {
      detail: {
        preset: effectivePreset,
        hue: hue,
        color: getComputedStyle(root).getPropertyValue('--accent').trim()
      }
    }));

    if (window.renderAll) window.renderAll();
  }

  function updateAccentSwatchesUI(activePreset, activeHue) {
    document.querySelectorAll('[data-accent-swatch]').forEach(el => {
      const swPreset = el.getAttribute('data-accent-swatch');
      if (swPreset === activePreset) {
        el.classList.add('active');
        el.setAttribute('aria-selected', 'true');
      } else {
        el.classList.remove('active');
        el.setAttribute('aria-selected', 'false');
      }
    });

    const hueSlider = document.getElementById('accentHueSlider');
    if (hueSlider && activeHue !== undefined) {
      hueSlider.value = activeHue;
    }
    const hueValLabel = document.getElementById('accentHueVal');
    if (hueValLabel && activeHue !== undefined) {
      hueValLabel.textContent = `${activeHue}°`;
    }

    const badge = document.getElementById('settingsCurrentAccentBadge');
    if (badge) {
      if (activePreset === 'custom') {
        badge.textContent = `Custom (${activeHue}°)`;
      } else if (ACCENT_PRESETS[activePreset]) {
        badge.textContent = ACCENT_PRESETS[activePreset].name;
      }
    }
  }

  window.toggleAccentPaletteMenu = function(e) {
    if (e && typeof e.stopPropagation === 'function') e.stopPropagation();
    const pop = document.getElementById('accentPalettePopover');
    if (!pop) return;
    const isShown = pop.style.display !== 'none';
    pop.style.display = isShown ? 'none' : 'block';
    if (!isShown) {
      updateAccentSwatchesUI(getStoredAccent(), getStoredAccentHue());
      if (window.lucide) window.lucide.createIcons();
    }
  };

  document.addEventListener('click', function(e) {
    const pop = document.getElementById('accentPalettePopover');
    const btn = document.getElementById('accentPaletteBtn');
    if (pop && pop.style.display !== 'none') {
      if (!pop.contains(e.target) && (!btn || !btn.contains(e.target))) {
        pop.style.display = 'none';
      }
    }
  });

  document.addEventListener('DOMContentLoaded', function() {
    updateAccentSwatchesUI(getStoredAccent(), getStoredAccentHue());
  });

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

  const initialAccent = getStoredAccent();
  const initialHue = getStoredAccentHue();
  if (initialAccent === 'custom') {
    applyAccent('custom', initialHue);
  } else {
    applyAccent(initialAccent);
  }

  // Global APIs
  window.LifeOSTheme = {
    get: getStoredTheme,
    set: applyTheme,
    toggle: toggleTheme,
    getAccent: getStoredAccent,
    getAccentHue: getStoredAccentHue,
    setAccent: applyAccent,
    presets: ACCENT_PRESETS,
    updateAccentSwatchesUI: updateAccentSwatchesUI
  };
  window.setTheme = applyTheme;
  window.toggleTheme = toggleTheme;
  window.setAccent = applyAccent;
})();
