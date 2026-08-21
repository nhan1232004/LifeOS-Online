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
  document.getElementById('pomStartBtn').textContent = '▶ Bắt đầu';
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
 document.getElementById('pomodoroTimer').textContent = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
 const stats = pomLoadStats();
 document.getElementById('pomCount').textContent = stats.count;
 document.getElementById('pomMins').textContent = stats.mins;
 // Update document title if running
 if (pomRunning) {
  const isWork = pomTime > 15 * 60 || pomTime < 5 * 60;
  document.title = `(${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}) ${isWork ? 'Tập trung' : 'Nghỉ ngơi'} - LifeOS`;
 } else {
  document.title = 'LifeOS - Dashboard Quản Lý';
 }
}

function pomToggle() {
 if (pomRunning) {
  pomWorker.postMessage('stop');
  pomRunning = false;
  document.getElementById('pomStartBtn').textContent = '▶ Tiếp tục';
  document.getElementById('pomodoroStatus').textContent = 'Đã tạm dừng';
 } else {
  pomRunning = true;
  pomEndTime = Date.now() + pomLeft * 1000;
  document.getElementById('pomStartBtn').textContent = '⏸ Tạm dừng';
  document.getElementById('pomodoroStatus').textContent = pomTime <= 15 * 60 && pomTime >= 5 * 60 ? 'Đang nghỉ ngơi...' : 'Đang tập trung...';
  pomWorker.postMessage('start');
 }
 pomUpdateDisplay();
}

function pomReset() {
 pomWorker.postMessage('stop');
 pomRunning = false;
 pomLeft = pomTime;
 document.getElementById('pomStartBtn').textContent = '▶ Bắt đầu';
 document.getElementById('pomodoroStatus').textContent = 'Tập trung làm việc';
 pomUpdateDisplay();
}

function pomSet(mins, el) {
 pomWorker.postMessage('stop');
 pomRunning = false;
 pomTime = mins * 60;
 pomLeft = pomTime;
 document.getElementById('pomStartBtn').textContent = '▶ Bắt đầu';
 document.getElementById('pomodoroStatus').textContent = mins <= 15 ? 'Nghỉ ngơi' : 'Tập trung làm việc';
 document.querySelectorAll('#p-pomodoro .ftab').forEach(b => b.classList.remove('active'));
 if (el) el.classList.add('active');
 pomUpdateDisplay();
}
function renderPomodoro() { pomUpdateDisplay(); }
