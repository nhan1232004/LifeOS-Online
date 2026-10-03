/* ════════════════════════════════════════════════════════════
   LIFEOS NATIVE MOBILE BRIDGE & TOUCH UX SYSTEM
   Supports Android & iOS via Capacitor.js runtime
════════════════════════════════════════════════════════════ */

(function() {
  const isNative = typeof window.Capacitor !== 'undefined' && window.Capacitor.isNativePlatform();
  const plugins = window.Capacitor?.Plugins || {};

  // 1. Haptics System (Tactile Feedback for Mobile)
  window.hapticTap = async function() {
    try {
      if (plugins.Haptics) {
        await plugins.Haptics.impact({ style: 'LIGHT' });
      } else if (navigator.vibrate) {
        navigator.vibrate(15);
      }
    } catch(e) {}
  };

  window.hapticSuccess = async function() {
    try {
      if (plugins.Haptics) {
        await plugins.Haptics.notification({ type: 'SUCCESS' });
      } else if (navigator.vibrate) {
        navigator.vibrate([30, 50, 30]);
      }
    } catch(e) {}
  };

  window.hapticWarning = async function() {
    try {
      if (plugins.Haptics) {
        await plugins.Haptics.notification({ type: 'WARNING' });
      } else if (navigator.vibrate) {
        navigator.vibrate(60);
      }
    } catch(e) {}
  };

  // 2. Status Bar & Splash Screen Configuration
  async function initNativeSystemBars() {
    if (!isNative) return;
    try {
      if (plugins.StatusBar) {
        const theme = localStorage.getItem('lifeos_theme') || 'dark';
        await plugins.StatusBar.setStyle({ style: theme === 'light' ? 'LIGHT' : 'DARK' });
        await plugins.StatusBar.setBackgroundColor({ color: theme === 'light' ? '#f4f5f8' : '#07070f' });
      }
      if (plugins.SplashScreen) {
        setTimeout(async () => {
          await plugins.SplashScreen.hide({ fadeOutDuration: 300 });
        }, 800);
      }
    } catch(e) {
      console.warn('[LifeOS Mobile] StatusBar/SplashScreen init:', e);
    }
  }

  // 3. Android Hardware Back Button Handling
  let lastBackPressTime = 0;
  function initHardwareBackButton() {
    if (!isNative || !plugins.App) return;

    plugins.App.addListener('backButton', ({ canGoBack }) => {
      // Priority 1: Close any active modal
      const activeModals = Array.from(document.querySelectorAll('.overlay')).filter(m => {
        return m.style.display === 'flex' || m.classList.contains('active') || m.classList.contains('open');
      });
      if (activeModals.length > 0) {
        const topModal = activeModals[activeModals.length - 1];
        if (typeof window.closeModal === 'function') {
          window.closeModal(topModal.id);
        } else {
          topModal.style.display = 'none';
        }
        window.hapticTap();
        return;
      }

      // Priority 2: Close mobile sidebar or more sheet
      const sidebar = document.getElementById('sidebar');
      if (sidebar && sidebar.classList.contains('active')) {
        if (typeof window.closeMobileSidebar === 'function') window.closeMobileSidebar();
        return;
      }

      // Priority 3: Navigate back from sub-page to 'today'
      const currentHash = (window.location.hash || '').replace(/^#/, '');
      if (currentHash && currentHash !== 'today') {
        if (typeof window.nav === 'function') {
          window.nav('today');
          window.hapticTap();
          return;
        }
      }

      // Priority 4: Double tap back button to exit app
      const now = Date.now();
      if (now - lastBackPressTime < 2000) {
        plugins.App.exitApp();
      } else {
        lastBackPressTime = now;
        if (typeof window.toast === 'function') {
          window.toast('Nhấn lần nữa để thoát LifeOS', 'info');
        }
        window.hapticTap();
      }
    });
  }

  // 4. Virtual Keyboard Handling
  function initKeyboardHandling() {
    if (isNative && plugins.Keyboard) {
      plugins.Keyboard.addListener('keyboardWillShow', info => {
        document.body.classList.add('keyboard-open');
        const activeEl = document.activeElement;
        if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
          setTimeout(() => {
            activeEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
          }, 150);
        }
      });
      plugins.Keyboard.addListener('keyboardWillHide', () => {
        document.body.classList.remove('keyboard-open');
      });
    }
  }

  // 5. Native Local Notifications Scheduler
  window.scheduleMobileNotification = async function(title, body, triggerInSeconds = 0) {
    if (isNative && plugins.LocalNotifications) {
      try {
        const perm = await plugins.LocalNotifications.requestPermissions();
        if (perm.display === 'granted') {
          await plugins.LocalNotifications.schedule({
            notifications: [
              {
                title: title,
                body: body,
                id: Math.floor(Date.now() % 100000),
                schedule: triggerInSeconds > 0 ? { at: new Date(Date.now() + triggerInSeconds * 1000) } : undefined,
                sound: 'beep.wav',
                smallIcon: 'ic_stat_name'
              }
            ]
          });
          return true;
        }
      } catch(e) {
        console.warn('[LifeOS Mobile] Local notification error:', e);
      }
    }
    return false;
  };

  // Auto-init on DOM Ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      initNativeSystemBars();
      initHardwareBackButton();
      initKeyboardHandling();
    });
  } else {
    initNativeSystemBars();
    initHardwareBackButton();
    initKeyboardHandling();
  }

  window.LifeOSMobile = {
    isNative,
    initNativeSystemBars
  };
})();
