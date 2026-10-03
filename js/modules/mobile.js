/* ════════════════════════════════════════════════════════════
   LIFEOS NATIVE MOBILE BRIDGE & ADVANCED TOUCH UX SYSTEM
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

  // 6. Mobile Touch Drag & Drop for Kanban (Projects & Tasks)
  function initTouchDragDrop() {
    let draggedCard = null;
    let cloneEl = null;
    let touchStartX = 0;
    let touchStartY = 0;
    let cardId = null;
    let isDragging = false;
    let dragTimer = null;

    document.addEventListener('touchstart', (e) => {
      const card = e.target.closest('.kcard');
      if (!card || e.target.closest('button, input, .todo-cb, .icon-btn')) return;

      draggedCard = card;
      const touch = e.touches[0];
      touchStartX = touch.clientX;
      touchStartY = touch.clientY;

      cardId = card.dataset.id || card.getAttribute('data-id');
      if (!cardId) {
        const match = (card.getAttribute('ondragstart') || card.getAttribute('onclick') || '').match(/['"]([a-zA-Z0-9_\-]+)['"]/);
        if (match) cardId = match[1];
      }

      dragTimer = setTimeout(() => {
        if (!draggedCard) return;
        isDragging = true;
        if (window.hapticTap) window.hapticTap();

        cloneEl = draggedCard.cloneNode(true);
        cloneEl.style.cssText = `position:fixed; z-index:9999; pointer-events:none; width:${draggedCard.offsetWidth}px; opacity:0.92; transform:scale(1.04) rotate(2deg); box-shadow:0 14px 28px rgba(0,0,0,0.6); left:${touch.clientX - 40}px; top:${touch.clientY - 30}px;`;
        document.body.appendChild(cloneEl);
        draggedCard.style.opacity = '0.35';
      }, 160);
    }, { passive: true });

    document.addEventListener('touchmove', (e) => {
      if (!draggedCard) return;
      const touch = e.touches[0];
      const dx = Math.abs(touch.clientX - touchStartX);
      const dy = Math.abs(touch.clientY - touchStartY);

      if (!isDragging && (dx > 8 || dy > 8)) {
        clearTimeout(dragTimer);
        return;
      }

      if (isDragging && cloneEl) {
        if (e.cancelable) e.preventDefault();
        cloneEl.style.left = (touch.clientX - 40) + 'px';
        cloneEl.style.top = (touch.clientY - 30) + 'px';

        document.querySelectorAll('.kcol, .todo-kcol').forEach(c => c.style.outline = '');
        const elemBelow = document.elementFromPoint(touch.clientX, touch.clientY);
        const col = elemBelow ? elemBelow.closest('.kcol, .todo-kcol') : null;
        if (col) col.style.outline = '2px dashed var(--accent)';
      }
    }, { passive: false });

    document.addEventListener('touchend', async (e) => {
      clearTimeout(dragTimer);
      if (!isDragging) {
        draggedCard = null;
        return;
      }

      isDragging = false;
      if (draggedCard) draggedCard.style.opacity = '1';
      if (cloneEl && cloneEl.parentNode) cloneEl.parentNode.removeChild(cloneEl);
      cloneEl = null;

      const touch = e.changedTouches[0];
      const elemBelow = document.elementFromPoint(touch.clientX, touch.clientY);
      document.querySelectorAll('.kcol, .todo-kcol').forEach(c => c.style.outline = '');

      const col = elemBelow ? elemBelow.closest('.kcol, .todo-kcol') : null;
      if (col && cardId) {
        const status = col.dataset.status || col.getAttribute('data-status');
        const pri = col.dataset.pri || col.getAttribute('data-pri');

        if (status && typeof window.kDrop === 'function') {
          const fakeEv = { preventDefault: () => {}, dataTransfer: { getData: () => cardId } };
          await window.kDrop(fakeEv, status);
          if (window.hapticSuccess) window.hapticSuccess();
        } else if (pri && window.DB && window.DB.todos) {
          const list = [...(window.DB.todos || [])];
          const tIdx = list.findIndex(x => x.id === cardId);
          if (tIdx >= 0) {
            list[tIdx] = { ...list[tIdx], priority: pri };
            window.DB.todos = list;
            if (typeof persist === 'function') await persist('todos', list);
            if (typeof window.syncTodosUI === 'function') window.syncTodosUI();
            if (window.hapticSuccess) window.hapticSuccess();
          }
        }
      }
      draggedCard = null;
      cardId = null;
    });
  }

  // 7. Swipe-down to dismiss mobile Bottom Sheet
  function initSwipeToDismissModal() {
    let activeModal = null;
    let startY = 0;
    let currentY = 0;
    let isSwiping = false;

    document.addEventListener('touchstart', (e) => {
      if (window.innerWidth > 640) return;
      const modal = e.target.closest('.overlay .modal');
      if (!modal) return;
      const rect = modal.getBoundingClientRect();
      if (e.touches[0].clientY - rect.top < 65) {
        activeModal = modal;
        startY = e.touches[0].clientY;
        currentY = startY;
        isSwiping = true;
      }
    }, { passive: true });

    document.addEventListener('touchmove', (e) => {
      if (!isSwiping || !activeModal) return;
      currentY = e.touches[0].clientY;
      const dy = currentY - startY;
      if (dy > 0) {
        activeModal.style.transform = `translateY(${dy}px)`;
        activeModal.style.transition = 'none';
      }
    }, { passive: true });

    document.addEventListener('touchend', () => {
      if (!isSwiping || !activeModal) return;
      const dy = currentY - startY;
      isSwiping = false;

      if (dy > 80) {
        activeModal.style.transition = 'transform 0.22s ease-out';
        activeModal.style.transform = 'translateY(100%)';
        const overlay = activeModal.closest('.overlay');
        const modalId = overlay ? overlay.id : null;
        setTimeout(() => {
          if (modalId && typeof window.closeModal === 'function') {
            window.closeModal(modalId);
          } else if (overlay) {
            overlay.style.display = 'none';
          }
          if (activeModal) {
            activeModal.style.transform = '';
            activeModal.style.transition = '';
          }
          if (window.hapticTap) window.hapticTap();
        }, 200);
      } else {
        activeModal.style.transition = 'transform 0.2s cubic-bezier(0.16, 1, 0.3, 1)';
        activeModal.style.transform = 'translateY(0)';
        setTimeout(() => {
          if (activeModal) {
            activeModal.style.transform = '';
            activeModal.style.transition = '';
          }
        }, 200);
      }
      activeModal = null;
    });
  }

  // 8. Pull-to-Refresh Gesture for Mobile
  function initPullToRefresh() {
    let pullStartY = 0;
    let isPulling = false;
    let pullEl = null;

    document.addEventListener('touchstart', (e) => {
      if (window.innerWidth > 640) return;
      const content = document.querySelector('.content');
      if (!content || content.scrollTop > 5) return;
      pullStartY = e.touches[0].clientY;
      isPulling = true;
    }, { passive: true });

    document.addEventListener('touchmove', (e) => {
      if (!isPulling) return;
      const y = e.touches[0].clientY;
      const dy = y - pullStartY;
      if (dy > 25 && dy < 120) {
        if (!pullEl) {
          pullEl = document.createElement('div');
          pullEl.id = 'pullRefreshIndicator';
          pullEl.style.cssText = 'position:fixed; top:calc(var(--sat, 0px) + 60px); left:50%; transform:translateX(-50%); background:var(--surface); border:1px solid var(--accent); color:var(--text1); padding:6px 14px; border-radius:20px; font-size:12px; font-weight:600; display:flex; align-items:center; gap:8px; z-index:1000; box-shadow:0 4px 16px rgba(0,0,0,0.4);';
          pullEl.innerHTML = '<span class="sync-dot" style="background:var(--accent-cyan); animation:pulse 0.8s infinite;"></span> Kéo để đồng bộ...';
          document.body.appendChild(pullEl);
        }
      }
    }, { passive: true });

    document.addEventListener('touchend', async (e) => {
      if (!isPulling) return;
      isPulling = false;
      if (pullEl) {
        const y = e.changedTouches[0].clientY;
        const dy = y - pullStartY;
        if (dy > 65) {
          pullEl.innerHTML = '⚡ Đang đồng bộ...';
          if (window.hapticTap) window.hapticTap();
          if (typeof window.renderAll === 'function') window.renderAll();
          setTimeout(() => {
            if (pullEl && pullEl.parentNode) pullEl.parentNode.removeChild(pullEl);
            pullEl = null;
            if (window.hapticSuccess) window.hapticSuccess();
            if (typeof window.toast === 'function') window.toast('Đã cập nhật dữ liệu mới nhất!', 'success');
          }, 450);
        } else {
          if (pullEl.parentNode) pullEl.parentNode.removeChild(pullEl);
          pullEl = null;
        }
      }
    });
  }

  // 9. IndexedDB Local Storage Adapter (Unlimited Storage)
  const IDB_NAME = 'lifeos_idb';
  const IDB_STORE = 'app_data';

  function openIDB() {
    return new Promise((resolve) => {
      if (!window.indexedDB) { resolve(null); return; }
      const req = window.indexedDB.open(IDB_NAME, 1);
      req.onupgradeneeded = (e) => {
        const db = e.target.result;
        if (!db.objectStoreNames.contains(IDB_STORE)) {
          db.createObjectStore(IDB_STORE);
        }
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => resolve(null);
    });
  }

  window.idbSet = async function(key, val) {
    try {
      const db = await openIDB();
      if (!db) return;
      const tx = db.transaction(IDB_STORE, 'readwrite');
      tx.objectStore(IDB_STORE).put(val, key);
    } catch(e) {}
  };

  window.idbGet = async function(key) {
    try {
      const db = await openIDB();
      if (!db) return null;
      return new Promise((resolve) => {
        const tx = db.transaction(IDB_STORE, 'readonly');
        const req = tx.objectStore(IDB_STORE).get(key);
        req.onsuccess = () => resolve(req.result || null);
        req.onerror = () => resolve(null);
      });
    } catch(e) {
      return null;
    }
  };

  // Auto-init on DOM Ready
  function initAllMobileFeatures() {
    initNativeSystemBars();
    initHardwareBackButton();
    initKeyboardHandling();
    initTouchDragDrop();
    initSwipeToDismissModal();
    initPullToRefresh();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAllMobileFeatures);
  } else {
    initAllMobileFeatures();
  }

  window.LifeOSMobile = {
    isNative,
    initNativeSystemBars,
    initTouchDragDrop,
    initSwipeToDismissModal,
    initPullToRefresh
  };
})();
