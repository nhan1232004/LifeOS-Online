/* ════════════════════════════════════════════════════════════
  POMODORO
════════════════════════════════════════════════════════════ */
let pomTime = 25 * 60, pomLeft = 25 * 60, pomRunning = false, pomEndTime = 0;
function getPomKey() { return 'lifeos_pom_' + today(); }

// Sử dụng Web Worker để không bị trình duyệt làm chậm khi đổi tab
const workerCode = `
 let interval;
 self.onmessage = function(e) {
  if (e.data === 'start') {
   interval = setInterval(() => self.postMessage('tick'), 1000);
  } else if (e.data === 'stop') {
   clearInterval(interval);
  }
 };
`;
const pomWorker = new Worker(URL.createObjectURL(new Blob([workerCode], {type: 'application/javascript'})));

pomWorker.onmessage = function() {
 if (!pomRunning) return;
 pomLeft = Math.ceil((pomEndTime - Date.now()) / 1000);
 if (pomLeft <= 0) {
  pomWorker.postMessage('stop');
  pomRunning = false;
  const startBtn = document.getElementById('pomStartBtn');
  if (startBtn) {
   startBtn.innerHTML = '<i data-lucide="play" class="ic-16"></i> Bắt đầu';
   if (window.lucide) window.lucide.createIcons();
  }
  const isWork = pomTime > 15 * 60 || pomTime < 5 * 60;
  if (isWork) {
   const stats = pomLoadStats();
   stats.count++;
   stats.mins += Math.round(pomTime / 60);
   pomSaveStats(stats);
   toast('Hoàn thành phiên tập trung.', 'success');
   document.getElementById('pomodoroStatus').textContent = 'Đã hoàn thành. Chuyển sang thời gian nghỉ ngơi.';
  } else {
   toast('Thời gian nghỉ đã kết thúc. Vui lòng quay lại công việc.', 'info');
   document.getElementById('pomodoroStatus').textContent = 'Hết giờ nghỉ!';
  }
  sendNotif('LifeOS Pomodoro', isWork ? 'Hoàn thành phiên tập trung.' : 'Thời gian nghỉ đã kết thúc. Vui lòng quay lại công việc.', 'pom');
  if (typeof window.hapticSuccess === 'function') window.hapticSuccess();
  if (typeof window.scheduleMobileNotification === 'function') {
    window.scheduleMobileNotification('LifeOS Pomodoro', isWork ? 'Hoàn thành phiên tập trung!' : 'Hết giờ nghỉ! Quay lại công việc nào.');
  }
  pomLeft = pomTime;
 }
 pomUpdateDisplay();
};

function pomLoadStats() {
 try { return JSON.parse(localStorage.getItem(getPomKey())) || { count: 0, mins: 0 }; } catch { return { count: 0, mins: 0 }; }
}
function pomSaveStats(s) { localStorage.setItem(getPomKey(), JSON.stringify(s)); }

function pomUpdateDisplay() {
 const m = Math.floor(Math.max(0, pomLeft) / 60), s = Math.max(0, pomLeft) % 60;
 const timeStr = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
 
 const timerEl = document.getElementById('pomodoroTimer');
 if (timerEl) timerEl.textContent = timeStr;
 
 // Sync to Today page widget
 const todayPomEl = document.getElementById('todayPomDisplay');
 if (todayPomEl) todayPomEl.textContent = timeStr;
 const todayPomBtn = document.getElementById('btnTodayPomToggle');
 if (todayPomBtn) todayPomBtn.textContent = pomRunning ? 'Tạm dừng' : 'Bắt đầu';

 // Sync SVG ring progress
 const ringEl = document.getElementById('pomRingProgress');
 if (ringEl && pomTime > 0) {
  const totalDash = 276.46;
  const progress = Math.max(0, Math.min(1, 1 - (pomLeft / pomTime)));
  ringEl.style.strokeDashoffset = String(totalDash * (1 - progress));
 }

 const stats = pomLoadStats();
 const countEl = document.getElementById('pomCount');
 if (countEl) countEl.textContent = stats.count;
 const minsEl = document.getElementById('pomMins');
 if (minsEl) minsEl.textContent = stats.mins;

 // Update document title if running
 if (pomRunning) {
  const isWork = pomTime > 15 * 60 || pomTime < 5 * 60;
  document.title = `(${timeStr}) ${isWork ? 'Tập trung' : 'Nghỉ ngơi'} - LifeOS`;
 } else {
  document.title = 'LifeOS – Quản lý cuộc sống';
 }
}

function pomToggle() {
 if (typeof window.hapticTap === 'function') window.hapticTap();
 const btn = document.getElementById('pomStartBtn');
 if (pomRunning) {
  pomWorker.postMessage('stop');
  pomRunning = false;
  if (btn) btn.innerHTML = '<i data-lucide="play" class="ic-16"></i> Tiếp tục';
  document.getElementById('pomodoroStatus').textContent = 'Đã tạm dừng';
 } else {
  pomRunning = true;
  pomEndTime = Date.now() + pomLeft * 1000;
  if (btn) btn.innerHTML = '<i data-lucide="pause" class="ic-16"></i> Tạm dừng';
  document.getElementById('pomodoroStatus').textContent = pomTime <= 15 * 60 && pomTime >= 5 * 60 ? 'Đang nghỉ ngơi...' : 'Đang tập trung...';
  pomWorker.postMessage('start');
 }
 if (window.lucide) window.lucide.createIcons();
 pomUpdateDisplay();
}

function pomReset() {
 pomWorker.postMessage('stop');
 pomRunning = false;
 pomLeft = pomTime;
 const btn = document.getElementById('pomStartBtn');
 if (btn) {
  btn.innerHTML = '<i data-lucide="play" class="ic-16"></i> Bắt đầu';
  if (window.lucide) window.lucide.createIcons();
 }
 document.getElementById('pomodoroStatus').textContent = pomTime <= 15 * 60 ? 'Nghỉ ngơi' : 'Tập trung làm việc';
 pomUpdateDisplay();
}

function pomSet(mins, el) {
 pomWorker.postMessage('stop');
 pomRunning = false;
 pomTime = mins * 60;
 pomLeft = pomTime;
 const btn = document.getElementById('pomStartBtn');
 if (btn) {
  btn.innerHTML = '<i data-lucide="play" class="ic-16"></i> Bắt đầu';
  if (window.lucide) window.lucide.createIcons();
 }
 document.getElementById('pomodoroStatus').textContent = mins <= 15 ? 'Nghỉ ngơi' : 'Tập trung làm việc';
 
 // Remove active from all preset buttons
 document.querySelectorAll('#p-pomodoro .seg-btn, #p-pomodoro .ftab').forEach(b => b.classList.remove('active'));
 if (el) {
  el.classList.add('active');
 } else {
  const matchBtn = Array.from(document.querySelectorAll('#p-pomodoro .seg-btn')).find(b => {
    const attr = b.getAttribute('onclick') || '';
    return attr.includes(`pomSet(${mins}`) || b.textContent.includes(`${mins}p`);
  });
  if (matchBtn) matchBtn.classList.add('active');
 }
 pomUpdateDisplay();
}

let pomActiveTaskId = null;

function setPomActiveTask(taskId) {
  if (pomActiveTaskId === taskId) {
    pomActiveTaskId = null;
  } else {
    pomActiveTaskId = taskId;
  }
  renderPomodoroTasks();
  updatePomActiveTaskBadge();
}

function updatePomActiveTaskBadge() {
  const wrap = document.getElementById('pomActiveTaskWrap');
  const textEl = document.getElementById('pomActiveTaskText');
  if (!wrap || !textEl) return;
  if (!pomActiveTaskId) {
    wrap.style.display = 'none';
  } else {
    const task = (window.DB.todos || []).find(t => t.id === pomActiveTaskId);
    if (!task) {
      wrap.style.display = 'none';
      pomActiveTaskId = null;
    } else {
      wrap.style.display = 'block';
      textEl.innerHTML = `<i data-lucide="target" class="ic-14" style="color:var(--accent-secondary)"></i> <span>Đang tập trung: <b>${window.LifeOSData?.escapeHtml(task.text) || task.text}</b></span> <button onclick="event.stopPropagation();setPomActiveTask('${task.id}')" style="background:none;border:none;color:var(--text-muted);cursor:pointer;margin-left:4px;">✕</button>`;
      if (window.lucide) lucide.createIcons();
    }
  }
}

function renderPomodoroTasks() {
  const listEl = document.getElementById('pomTaskList');
  if (!listEl) return;
  const todos = (window.DB.todos || []).filter(t => !t.done);
  if (!todos.length) {
    listEl.innerHTML = `<div style="text-align:center;padding:24px 10px;font-size:12.5px;color:var(--text-muted);">Không có công việc nào đang chờ. Hãy thêm việc mới để tập trung!</div>`;
    return;
  }

  listEl.innerHTML = todos.slice(0, 10).map(t => {
    const isSelected = t.id === pomActiveTaskId;
    return `
      <div class="bento-task-row" style="background:${isSelected ? 'rgba(139,92,246,0.18)' : 'var(--surface-2)'};border:${isSelected ? '1px solid var(--accent)' : '1px solid transparent'};" onclick="setPomActiveTask('${t.id}')">
        <div class="bento-task-checkbox" onclick="event.stopPropagation();toggleTodo('${t.id}')" style="cursor:pointer;" title="Đánh dấu hoàn thành">
          ${t.done ? '✓' : ''}
        </div>
        <div class="bento-task-name" style="flex:1;font-size:13px;font-weight:${isSelected ? '700' : '500'};color:${isSelected ? 'var(--accent-light)' : 'var(--text-primary)'};">
          ${window.LifeOSData?.escapeHtml(t.text) || t.text}
        </div>
        ${isSelected ? '<span class="badge" style="background:var(--accent);color:#fff;font-size:10px;padding:2px 6px;">Đang chọn</span>' : ''}
      </div>
    `;
  }).join('');
}

function renderPomodoro() { 
  pomUpdateDisplay(); 
  renderPomodoroTasks();
  updatePomActiveTaskBadge();
}

window.setPomActiveTask = setPomActiveTask;
window.renderPomodoroTasks = renderPomodoroTasks;
