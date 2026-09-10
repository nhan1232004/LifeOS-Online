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
 window.DB.habits.push({ id: uid(), name, icon, log: {} });
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
 renderHabits();
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
function getStreak(log) {
 let streak = 0;
 const d = new Date();
 for (let i = 0; i < 365; i++) {
  const ds = toLocalDateStr(d);
  if (log[ds]) streak++; else if (i > 0) break;
  d.setDate(d.getDate() - 1);
 }
 return streak;
}
function renderHabits() {
 const c = document.getElementById('habitsContainer'); if (!c) return;
 const habits = window.DB.habits || [];
 if (!habits.length) { c.innerHTML = '<div style="color:var(--text3);padding:30px;text-align:center">Chưa có thói quen nào. Bấm "<i data-lucide="plus" style="width:14px;height:14px;margin-right:4px"></i> Thêm thói quen" để bắt đầu!</div>'; return; }
 const days = Array.from({ length: 21 }, (_, i) => dayOff(i - 20));
 c.innerHTML = habits.map(h => {
  const streak = getStreak(h.log || {});
  return `<div class="habit-card">
   <div class="hc-top">
    <div class="hc-name">${h.icon} ${h.name}</div>
    <div style="display:flex;align-items:center;gap:8px">
     <div class="hc-streak"> ${streak} ngày liên tiếp</div>
     <button class="habit-del" onclick="delHabit('${h.id}')"><i data-lucide="trash-2" style="width:14px;height:14px;"></i></button>
    </div>
   </div>
   <div class="habit-days">
    ${days.map(d => {
     const done = (h.log || {})[d];
     const isToday = d === today();
     const dt = new Date(d + 'T00:00:00');
     const label = dt.getDate() + '/' + (dt.getMonth() + 1);
     return `<div class="habit-day${done ? ' done' : ''}${isToday ? ' today' : ''}" onclick="toggleHabitDay('${h.id}','${d}')" title="${label}">${done ? '✓' : label}</div>`;
    }).join('')}
   </div>
  </div>`;
 }).join('');
}
