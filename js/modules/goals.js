/* ════════════════════════════════════════════════════════════
  GOALS
════════════════════════════════════════════════════════════ */
function openGoal(id) {
 document.getElementById('goalId').value = '';
 document.getElementById('goalName').value = '';
 document.getElementById('goalTarget').value = '';
 document.getElementById('goalSaved').value = '';
 document.getElementById('goalDue').value = '';
 document.getElementById('mGoalTitle').textContent = 'Thêm mục tiêu';
 if (id) {
  const g = (window.DB.goals || []).find(x => x.id === id);
  if (g) {
   document.getElementById('goalId').value = g.id;
   document.getElementById('goalName').value = g.name;
   document.getElementById('goalTarget').value = g.target;
   document.getElementById('goalSaved').value = g.saved;
   document.getElementById('goalDue').value = g.due || '';
   document.getElementById('mGoalTitle').textContent = 'Sửa mục tiêu';
  }
 }
 openModal('mGoal');
}
async function saveGoal() {
 const name = document.getElementById('goalName').value.trim();
 const target = Number(document.getElementById('goalTarget').value) || 0;
 if (!name || !target) { toast('Nhập tên và số tiền mục tiêu', 'error'); return; }
 const saved = Number(document.getElementById('goalSaved').value) || 0;
 const due = document.getElementById('goalDue').value;
 const id = document.getElementById('goalId').value || uid();
 if (!window.DB.goals) window.DB.goals = [];
 const idx = window.DB.goals.findIndex(x => x.id === id);
 const goal = { id, name, target, saved, due };
 if (idx >= 0) window.DB.goals[idx] = goal; else window.DB.goals.push(goal);
 await persist('goals', window.DB.goals);
 closeModal('mGoal');
 renderGoals();
 toast('Đã lưu mục tiêu!', 'success');
}
async function addToGoal(id, amt) {
 const g = (window.DB.goals || []).find(x => x.id === id); if (!g) return;
 const add = prompt('Nhập số tiền muốn thêm (₫):', '');
 if (!add || isNaN(add)) return;
 g.saved = (g.saved || 0) + Number(add);
 if (g.saved > g.target) g.saved = g.target;
 await persist('goals', window.DB.goals);
 renderGoals();
 toast('Đã cập nhật!', 'success');
}
async function delGoal(id) {
 if (!confirm('Xóa mục tiêu này?')) return;
 window.DB.goals = (window.DB.goals || []).filter(x => x.id !== id);
 await persist('goals', window.DB.goals);
 renderGoals();
 toast('Đã xóa mục tiêu', 'info');
}
function renderGoals() {
 const c = document.getElementById('goalsContainer'); if (!c) return;
 const goals = window.DB.goals || [];
 if (!goals.length) { c.innerHTML = '<div style="color:var(--text3);padding:30px;text-align:center">Chưa có mục tiêu nào. Bấm "<i data-lucide="plus" style="width:14px;height:14px;margin-right:4px"></i> Thêm mục tiêu" để bắt đầu!</div>'; return; }
 c.innerHTML = goals.map(g => {
  const pct = g.target > 0 ? Math.min(100, Math.round((g.saved / g.target) * 100)) : 0;
  const done = pct >= 100;
  return `<div class="goal-card">
   <div class="gc-top">
    <div class="gc-name">${done ? ' ' : ' '}${g.name}</div>
    <div class="gc-pct">${pct}%</div>
   </div>
   <div class="gc-amt">${fmtFull(g.saved)} / ${fmtFull(g.target)} ₫${g.due ? ' · Hạn: ' + fmtDate(g.due) : ''}</div>
   <div class="goal-bar"><div class="goal-bar-fill" style="width:${pct}%;${done ? 'background:linear-gradient(90deg,var(--green),#00e676)' : ''}"></div></div>
   <div class="gc-actions">
    <button class="btn btn-sm btn-g" onclick="addToGoal('${g.id}')"><i data-lucide="plus" style="width:14px;height:14px;margin-right:4px"></i> Thêm tiền</button>
    <button class="btn btn-sm" onclick="openGoal('${g.id}')"><i data-lucide="pencil" style="width:14px;height:14px;margin-right:4px"></i> Sửa</button>
    <button class="btn btn-sm btn-r" onclick="delGoal('${g.id}')"><i data-lucide="trash-2" style="width:14px;height:14px;"></i></button>
   </div>
  </div>`;
 }).join('');
}
