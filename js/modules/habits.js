/* ════════════════════════════════════════════════════════════
  HABITS
════════════════════════════════════════════════════════════ */
function openHabit() {
 document.getElementById('habitId').value = '';
 document.getElementById('habitName').value = '';
 document.getElementById('habitIcon').value = '';
 openModal('mHabit');
}
async function saveHabit() {
 const name = document.getElementById('habitName').value.trim();
 if (!name) { toast('Nhập tên thói quen', 'error'); return; }
 const icon = document.getElementById('habitIcon').value.trim() || '';
 if (!window.DB.habits) window.DB.habits = [];
 window.DB.habits.push({ id: uid(), name, icon, log: {}, createdAt: today() });
 await persist('habits', window.DB.habits);
 closeModal('mHabit');
 renderHabits();
 toast('Đã thêm thói quen!', 'success');
}
 async function toggleHabitDay(hid, dateStr) {
  const h = (window.DB.habits || []).find(x => x.id === hid); if (!h) return;
  if (!h.log) h.log = {};
  h.log[dateStr] = !h.log[dateStr];
  await persist('habits', window.DB.habits);
  if (typeof renderHabits === 'function') renderHabits();
  if (typeof renderToday === 'function') renderToday();
 }
async function delHabit(id) {
  const t = (window.DB.habits || []).find(x => x.id === id);
  if (!t) return;
  if (!confirm(`Bạn có chắc chắn muốn xóa thói quen "${t.name}"?`)) return;
  window.DB.habits = (window.DB.habits || []).filter(x => x.id !== id);
  await persist('habits', window.DB.habits);
  renderHabits();
  toast('Đã xóa thói quen!', 'info');
}
function getStreak(log, createdAt) {
 let streak = 0;
 const start = new Date((createdAt || today()) + 'T00:00:00');
 const end = new Date(today() + 'T00:00:00');
 const d = new Date(start);
 while (d <= end) {
  const ds = toLocalDateStr(d);
  if (log[ds]) streak++; else streak = 0;
  d.setDate(d.getDate() + 1);
 }
 return streak;
}
function renderHabits() {
 const c = document.getElementById('habitsContainer'); if (!c) return;
 const habits = window.DB.habits || [];
 if (!habits.length) {
  c.innerHTML = `
   <div class="empty-state" style="padding:48px 16px;text-align:center;">
    <div style="width:52px;height:52px;border-radius:14px;background:rgba(255,113,133,0.12);color:var(--danger);display:flex;align-items:center;justify-content:center;margin:0 auto 14px;">
     <i data-lucide="flame" class="ic-28"></i>
    </div>
    <div style="font-size:16px;font-weight:700;color:var(--text-primary);margin-bottom:6px">Chưa có thói quen nào</div>
    <div style="font-size:13px;color:var(--text-muted);margin-bottom:16px">Tạo thói quen để rèn luyện kỷ luật và duy trì tiến bộ mỗi ngày.</div>
    <button class="btn btn-p btn-sm" onclick="openHabit()"><i data-lucide="plus" class="ic-14"></i> Thêm thói quen mới</button>
   </div>`;
  if (window.lucide) lucide.createIcons();
  return;
 }
 const todayStr = today();
 const kpiEl = document.getElementById('habitKpis');
 if (kpiEl && habits.length) {
  const todayDone = habits.filter(h => (h.log || {})[todayStr]).length;
  const todayPct = Math.round((todayDone / habits.length) * 100);
  const maxStreak = habits.reduce((max, h) => Math.max(max, getStreak(h.log || {}, h.createdAt || todayStr)), 0);
  const avgStreak = Math.round(habits.reduce((sum, h) => sum + getStreak(h.log || {}, h.createdAt || todayStr), 0) / habits.length);
  
  kpiEl.innerHTML = `
    <div class="bento-kpi-card">
      <div class="bento-kpi-top">
        <div class="bento-kpi-icon mint"><i data-lucide="check-circle-2" class="ic-18"></i></div>
        <span class="bento-kpi-pill mint">${todayPct}% hôm nay</span>
      </div>
      <div class="bento-kpi-mid">
        <div class="bento-kpi-value">${todayDone}/${habits.length}</div>
        <div class="bento-kpi-label">Thói quen hoàn thành</div>
      </div>
    </div>

    <div class="bento-kpi-card">
      <div class="bento-kpi-top">
        <div class="bento-kpi-icon amber"><i data-lucide="flame" class="ic-18"></i></div>
        <span class="bento-kpi-pill amber">Cao nhất</span>
      </div>
      <div class="bento-kpi-mid">
        <div class="bento-kpi-value">${maxStreak} ngày</div>
        <div class="bento-kpi-label">Kỷ lục chuỗi</div>
      </div>
    </div>

    <div class="bento-kpi-card">
      <div class="bento-kpi-top">
        <div class="bento-kpi-icon purple"><i data-lucide="award" class="ic-18"></i></div>
        <span class="bento-kpi-pill purple">Bền bỉ</span>
      </div>
      <div class="bento-kpi-mid">
        <div class="bento-kpi-value">${avgStreak} ngày</div>
        <div class="bento-kpi-label">Chuỗi trung bình</div>
      </div>
    </div>

    <div class="bento-kpi-card">
      <div class="bento-kpi-top">
        <div class="bento-kpi-icon cyan"><i data-lucide="repeat" class="ic-18"></i></div>
        <span class="bento-kpi-pill cyan">Đang duy trì</span>
      </div>
      <div class="bento-kpi-mid">
        <div class="bento-kpi-value">${habits.length}</div>
        <div class="bento-kpi-label">Tổng số thói quen</div>
      </div>
    </div>
  `;
 } else if (kpiEl) {
  kpiEl.innerHTML = '';
 }

 const curr = new Date();
 const dayOfWeek = (curr.getDay() + 6) % 7; // 0 is Monday
 const weekDayNames = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

 c.className = 'modern-habits-container';
 c.innerHTML = habits.map(h => {
  const created = h.createdAt || todayStr;
  const streak = getStreak(h.log || {}, created);

  // Compute 7 days of current week (Mon -> Sun)
  const weekDays = [];
  let doneThisWeek = 0;
  for (let i = 0; i < 7; i++) {
   const d = new Date(curr);
   d.setDate(curr.getDate() - dayOfWeek + i);
   const ds = toLocalDateStr(d);
   const isDone = Boolean((h.log || {})[ds]);
   if (isDone) doneThisWeek++;
   weekDays.push({
    label: weekDayNames[i],
    dateStr: ds,
    dayNum: d.getDate(),
    isDone,
    isToday: ds === todayStr,
    isFuture: ds > todayStr
   });
  }

  const weekPct = Math.round((doneThisWeek / 7) * 100);

  return `
   <div class="modern-habit-card">
    <div class="modern-habit-top">
     <div class="modern-habit-info">
      <div class="modern-habit-icon-box">${h.icon || '🔥'}</div>
      <div>
       <div class="modern-habit-name">${window.LifeOSData.escapeHtml(h.name)}</div>
       <div style="font-size:12px;color:var(--text-muted)">Rèn luyện hàng ngày • Đạt ${doneThisWeek}/7 ngày tuần này</div>
      </div>
     </div>
     <div style="display:flex;align-items:center;gap:10px">
      <div class="modern-habit-streak-pill"><i data-lucide="flame" class="ic-14"></i> Chuỗi ${streak} ngày</div>
      <button class="icon-btn" style="color:var(--danger)" onclick="delHabit('${window.LifeOSData.escapeAttr(h.id)}')"><i data-lucide="trash-2" class="ic-14"></i></button>
     </div>
    </div>
    
    <div class="modern-habit-tracker-row">
     ${weekDays.map(wd => `
      <div class="modern-habit-bubble ${wd.isDone ? 'done' : ''} ${wd.isToday ? 'today' : ''} ${wd.isFuture ? 'future' : ''}" 
           ${wd.isFuture ? '' : `onclick="toggleHabitDay('${window.LifeOSData.escapeAttr(h.id)}','${wd.dateStr}')"`}
           title="${wd.label} (${wd.dateStr}): ${wd.isDone ? 'Đã hoàn thành' : 'Chưa hoàn thành'}">
       <div class="modern-habit-bubble-btn">
        ${wd.isDone ? '✓' : (wd.isFuture ? '·' : wd.dayNum)}
       </div>
       <div class="modern-habit-bubble-label">${wd.label}</div>
      </div>
     `).join('')}
    </div>

    <div style="display:flex;align-items:center;justify-content:space-between;font-size:11.5px;color:var(--text-muted);margin-top:-4px">
     <span>Tiến độ tuần: ${weekPct}%</span>
     <div style="width:120px;height:4px;background:var(--surface-2);border-radius:99px;overflow:hidden">
      <div style="width:${weekPct}%;height:100%;background:linear-gradient(90deg,var(--accent-secondary),#4DD4A2);border-radius:99px"></div>
     </div>
    </div>
   </div>
  `;
 }).join('');

 if (window.lucide) lucide.createIcons();
}
