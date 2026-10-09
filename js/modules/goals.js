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
 if (!goals.length) {
  c.className = '';
  c.innerHTML = `
   <div class="empty-state" style="padding:48px 16px;text-align:center;">
    <div style="width:52px;height:52px;border-radius:14px;background:rgba(93,214,192,0.12);color:var(--accent-secondary);display:flex;align-items:center;justify-content:center;margin:0 auto 14px;">
     <i data-lucide="target" class="ic-28"></i>
    </div>
    <div style="font-size:16px;font-weight:700;color:var(--text-primary);margin-bottom:6px">Chưa có mục tiêu nào</div>
    <div style="font-size:13px;color:var(--text-muted);margin-bottom:16px">Đặt ra mục tiêu tài chính cụ thể để theo dõi số tiền tiết kiệm và ngày hoàn thành.</div>
    <button class="btn btn-p btn-sm" onclick="openGoal()"><i data-lucide="plus" class="ic-14"></i> Thêm mục tiêu</button>
   </div>`;
  if (window.lucide) lucide.createIcons();
  return;
 }

 const kpiEl = document.getElementById('goalKpis');
 if (kpiEl && goals.length) {
  const totalSaved = goals.reduce((sum, g) => sum + (Number(g.saved) || 0), 0);
  const totalTarget = goals.reduce((sum, g) => sum + (Number(g.target) || 0), 0);
  const totalPct = totalTarget > 0 ? Math.min(100, Math.round((totalSaved / totalTarget) * 100)) : 0;
  const completedGoals = goals.filter(g => (Number(g.saved) || 0) >= (Number(g.target) || 0)).length;

  kpiEl.innerHTML = `
    <div class="bento-kpi-card">
      <div class="bento-kpi-top">
        <div class="bento-kpi-icon mint"><i data-lucide="piggy-bank" class="ic-18"></i></div>
        <span class="bento-kpi-pill mint">Đã tiết kiệm</span>
      </div>
      <div class="bento-kpi-mid">
        <div class="bento-kpi-value">${fmt(totalSaved)} ₫</div>
        <div class="bento-kpi-label">Tổng tích lũy</div>
      </div>
    </div>

    <div class="bento-kpi-card">
      <div class="bento-kpi-top">
        <div class="bento-kpi-icon purple"><i data-lucide="target" class="ic-18"></i></div>
        <span class="bento-kpi-pill purple">${totalPct}% toàn bộ</span>
      </div>
      <div class="bento-kpi-mid">
        <div class="bento-kpi-value">${fmt(totalTarget)} ₫</div>
        <div class="bento-kpi-label">Mục tiêu cần đạt</div>
      </div>
    </div>

    <div class="bento-kpi-card">
      <div class="bento-kpi-top">
        <div class="bento-kpi-icon amber"><i data-lucide="check-circle-2" class="ic-18"></i></div>
        <span class="bento-kpi-pill amber">${completedGoals}/${goals.length}</span>
      </div>
      <div class="bento-kpi-mid">
        <div class="bento-kpi-value">${completedGoals}</div>
        <div class="bento-kpi-label">Mục tiêu hoàn thành</div>
      </div>
    </div>

    <div class="bento-kpi-card">
      <div class="bento-kpi-top">
        <div class="bento-kpi-icon cyan"><i data-lucide="trending-up" class="ic-18"></i></div>
        <span class="bento-kpi-pill cyan">Tiến độ</span>
      </div>
      <div class="bento-kpi-mid">
        <div class="bento-kpi-value">${totalPct}%</div>
        <div class="bento-kpi-label">Tỷ lệ tích lũy chung</div>
      </div>
    </div>
  `;
 } else if (kpiEl) {
  kpiEl.innerHTML = '';
 }

 c.className = 'goals-grid';
 c.innerHTML = goals.map(g => {
  const pct = g.target > 0 ? Math.min(100, Math.round((g.saved / g.target) * 100)) : 0;
  const done = pct >= 100;
  return `
   <div class="goal-card">
    <div class="gc-top">
     <div class="gc-name" style="display:flex;align-items:center;gap:8px;">
      <i data-lucide="${done ? 'check-circle-2' : 'target'}" class="ic-18" style="color:${done ? 'var(--success)' : 'var(--accent)'}"></i>
      <span>${window.LifeOSData.escapeHtml(g.name)}</span>
     </div>
     <span class="bento-kpi-pill ${done ? 'mint' : 'purple'}">${pct}%</span>
    </div>

    <div class="gc-amt" style="display:flex;justify-content:space-between;align-items:center;">
     <span style="font-weight:700;color:var(--text-primary);font-size:13.5px">${fmtFull(g.saved)} <span style="font-size:11px;color:var(--text-muted)">/ ${fmtFull(g.target)} ₫</span></span>
     ${g.due ? `<span style="font-size:11.5px;color:var(--text-muted)"><i data-lucide="calendar" class="ic-12"></i> ${fmtDate(g.due)}</span>` : ''}
    </div>

    <div class="goal-bar">
     <div class="goal-bar-fill" style="width:${pct}%;${done ? 'background:linear-gradient(90deg,var(--success),#5DD6C0)' : ''}"></div>
    </div>

    <div class="gc-actions" style="margin-top:4px;">
     <button class="btn btn-sm btn-p" onclick="addToGoal('${window.LifeOSData.escapeAttr(g.id)}')" style="font-size:11.5px;padding:5px 12px;background:var(--accent-secondary);color:#0B0D14;font-weight:700"><i data-lucide="plus" class="ic-12"></i> Nạp thêm</button>
     <button class="btn btn-sm btn-outline" onclick="openGoal('${window.LifeOSData.escapeAttr(g.id)}')" style="font-size:11.5px;padding:5px 10px"><i data-lucide="pencil" class="ic-12"></i> Sửa</button>
     <button class="icon-btn" style="color:var(--danger);margin-left:auto" onclick="delGoal('${window.LifeOSData.escapeAttr(g.id)}')"><i data-lucide="trash-2" class="ic-14"></i></button>
    </div>
   </div>
  `;
 }).join('');

 if (window.lucide) lucide.createIcons();
}
