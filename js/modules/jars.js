/* ────────────────────────────────────────────────────────
  HŨ CHI TIÊU & NGÂN SÁCH (EXPENSE JARS / ENVELOPE BUDGETING)
  Midnight Aurora Edition - LifeOS Online
──────────────────────────────────────────────────────── */

const DEFAULT_JARS = [
  { id: 'jar_food', name: 'Ăn uống', icon: 'utensils', emoji: '🍜', color: '#10B981', desc: 'Nhu yếu phẩm & Ăn uống hàng ngày', defaultPercent: 30 },
  { id: 'jar_fuel', name: 'Xăng xe', icon: 'fuel', emoji: '⛽', color: '#06B6D4', desc: 'Xăng xe, đi lại, bảo dưỡng & gửi xe', defaultPercent: 10 },
  { id: 'jar_study', name: 'Học tập', icon: 'graduation-cap', emoji: '🎓', color: '#8B5CF6', desc: 'Sách vở, khóa học & phát triển bản thân', defaultPercent: 10 },
  { id: 'jar_save', name: 'Tiết kiệm', icon: 'piggy-bank', emoji: '💰', color: '#F59E0B', desc: 'Tích lũy, đầu tư & quỹ khẩn cấp', defaultPercent: 20 },
  { id: 'jar_travel', name: 'Du lịch', icon: 'plane', emoji: '✈️', color: '#3B82F6', desc: 'Du lịch, nghỉ dưỡng & trải nghiệm', defaultPercent: 10 },
  { id: 'jar_party', name: 'Đám tiệc', icon: 'gift', emoji: '🎁', color: '#EC4899', desc: 'Hiếu hỷ, liên hoan, ngoại giao & quà biếu', defaultPercent: 5 },
  { id: 'jar_health', name: 'Sức khỏe', icon: 'heart-pulse', emoji: '🏥', color: '#14B8A6', desc: 'Thuốc men, khám bệnh, thể thao & vitamin', defaultPercent: 10 },
  { id: 'jar_emerg', name: 'Phát sinh', icon: 'zap', emoji: '⚡', color: '#F97316', desc: 'Chi phí đột xuất, sửa chữa & dự phòng', defaultPercent: 5 }
];

// Mapping category to recommended jar
const CAT_TO_JAR = {
  'Ăn uống': 'Ăn uống',
  'Đi lại': 'Xăng xe',
  'Học tập': 'Học tập',
  'Sức khoẻ': 'Sức khỏe',
  'Sức khỏe': 'Sức khỏe',
  'Giải trí': 'Du lịch',
  'Du lịch': 'Du lịch',
  'Quà tặng': 'Đám tiệc',
  'Tiệc tùng': 'Đám tiệc',
  'Nhà ở': 'Phát sinh',
  'Điện thoại': 'Xăng xe',
  'Điện nước': 'Phát sinh',
  'Quần áo': 'Ăn uống',
  'Mua sắm': 'Phát sinh',
  'Khác': 'Phát sinh'
};

function getAllJars() {
  const custom = (window.DB && Array.isArray(window.DB.jars)) ? window.DB.jars : [];
  if (custom.length === 0) return DEFAULT_JARS;
  // Merge default jars with user custom jars, avoiding ID duplicates
  const map = new Map();
  DEFAULT_JARS.forEach(j => map.set(j.id, { ...j }));
  custom.forEach(j => map.set(j.id || j.name, { ...j }));
  return Array.from(map.values());
}

function getJarByName(name) {
  if (!name) return null;
  const jars = getAllJars();
  const lower = String(name).trim().toLowerCase();
  return jars.find(j => j.name.toLowerCase() === lower || j.id.toLowerCase() === lower) || null;
}

function calculateJarStats(jarName, interval) {
  const getAmt = x => Number(x.amt ?? x.amount ?? 0) || 0;
  const jarObj = getJarByName(jarName);
  const matchJar = (item) => {
    if (!item.jar) return false;
    const j = String(item.jar).trim().toLowerCase();
    const target = String(jarName).trim().toLowerCase();
    return j === target || (jarObj && j === jarObj.id.toLowerCase());
  };

  const allIncomes = (window.DB.income || []).filter(matchJar);
  const allExpenses = (window.DB.expense || []).filter(matchJar);

  const totalInflow = allIncomes.reduce((s, x) => s + getAmt(x), 0);
  const totalOutflow = allExpenses.reduce((s, x) => s + getAmt(x), 0);
  const balance = totalInflow - totalOutflow;

  // Period stats if interval provided
  let periodInflow = totalInflow;
  let periodOutflow = totalOutflow;
  if (interval && interval.start && interval.end) {
    const pIncomes = allIncomes.filter(x => (!interval.start || x.date >= interval.start) && (!interval.end || x.date <= interval.end));
    const pExpenses = allExpenses.filter(x => (!interval.start || x.date >= interval.start) && (!interval.end || x.date <= interval.end));
    periodInflow = pIncomes.reduce((s, x) => s + getAmt(x), 0);
    periodOutflow = pExpenses.reduce((s, x) => s + getAmt(x), 0);
  }

  const spendPct = totalInflow > 0 ? Math.min(100, Math.round((totalOutflow / totalInflow) * 100)) : (totalOutflow > 0 ? 100 : 0);

  return {
    totalInflow,
    totalOutflow,
    balance,
    periodInflow,
    periodOutflow,
    spendPct
  };
}

let activeJarFilter = null;

function renderJarsGrid() {
  const grid = document.getElementById('finJarsGrid');
  if (!grid) return;

  const interval = typeof window.getFinDateInterval === 'function' ? window.getFinDateInterval() : null;
  const jars = getAllJars();

  grid.innerHTML = jars.map(j => {
    const stats = calculateJarStats(j.name, interval);
    const isOverbudget = stats.balance < 0;
    const isZero = stats.balance === 0;
    const isSelected = activeJarFilter === j.name;

    const statusBadge = isOverbudget
      ? `<span class="jar-status-pill danger">⚠️ Bội chi -${window.fmt ? window.fmt(Math.abs(stats.balance)) : Math.abs(stats.balance)}₫</span>`
      : isZero
      ? `<span class="jar-status-pill neutral">Hết tiền</span>`
      : `<span class="jar-status-pill success">Còn ${stats.spendPct}% hạn mức</span>`;

    const balanceColor = isOverbudget ? 'var(--danger)' : (isZero ? 'var(--text-muted)' : 'var(--text-primary)');

    return `
      <div class="jar-card ${isSelected ? 'active-filter' : ''}" style="--jar-color:${j.color};" onclick="filterByJar('${j.name}')">
        <div class="jar-card-top">
          <div class="jar-icon-wrap" style="background:${j.color}22; color:${j.color}; border: 1px solid ${j.color}44;">
            <span style="font-size:18px;">${j.emoji || '🫙'}</span>
          </div>
          <div style="flex:1; min-width:0; margin-left:10px;">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <h4 class="jar-title">${j.name}</h4>
              ${statusBadge}
            </div>
            <p class="jar-desc">${j.desc}</p>
          </div>
        </div>

        <div class="jar-card-balance">
          <div class="jar-bal-val" style="color:${balanceColor};">
            ${stats.balance >= 0 ? '' : '−'}${window.fmt ? window.fmt(Math.abs(stats.balance)) : Math.abs(stats.balance)} ₫
          </div>
          <div class="jar-bal-lbl">Số dư khả dụng</div>
        </div>

        <div class="jar-progress-track">
          <div class="jar-progress-fill" style="width:${Math.min(100, stats.spendPct)}%; background:linear-gradient(90deg, ${j.color}, ${isOverbudget ? 'var(--danger)' : j.color});"></div>
        </div>

        <div class="jar-card-stats">
          <div class="jar-stat-item">
            <span class="jar-stat-lbl">Đã nạp</span>
            <span class="jar-stat-val text-inc">+${window.fmt ? window.fmt(stats.totalInflow) : stats.totalInflow}₫</span>
          </div>
          <div class="jar-stat-item" style="text-align:right;">
            <span class="jar-stat-lbl">Đã chi</span>
            <span class="jar-stat-val text-exp">−${window.fmt ? window.fmt(stats.totalOutflow) : stats.totalOutflow}₫</span>
          </div>
        </div>

        <div class="jar-card-actions">
          <button class="jar-btn-action" onclick="event.stopPropagation(); quickDepositToJar('${j.name}')" title="Nạp tiền vào hũ">
            <i data-lucide="plus-circle" class="ic-14"></i> Nạp hũ
          </button>
          <button class="jar-btn-action" onclick="event.stopPropagation(); filterByJar('${j.name}')" title="Lọc giao dịch của hũ này">
            <i data-lucide="filter" class="ic-14"></i> ${isSelected ? 'Bỏ lọc' : 'Lọc thu chi'}
          </button>
        </div>
      </div>
    `;
  }).join('');

  if (window.lucide) window.lucide.createIcons();
}

function filterByJar(jarName) {
  if (activeJarFilter === jarName) {
    activeJarFilter = null;
    if (window.toast) window.toast('Đã hiển thị toàn bộ giao dịch', 'info');
  } else {
    activeJarFilter = jarName;
    if (window.toast) window.toast(`Đang lọc giao dịch theo Hũ: ${jarName}`, 'info');
  }
  renderJarsGrid();
  if (typeof window.renderFinTables === 'function') window.renderFinTables();
}
window.filterByJar = filterByJar;

function populateJarSelects() {
  const jars = getAllJars();
  const optionsHtml = `
    <option value="">-- Chọn Hũ chi tiêu --</option>
    ${jars.map(j => `<option value="${j.name}">${j.emoji || '🫙'} ${j.name} (${j.desc})</option>`).join('')}
    <option value="Chung">🌐 Ngoài hũ / Thu chi chung</option>
  `;

  ['incJar', 'expJar', 'qfIncJar', 'qfExpJar'].forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      const curVal = el.value;
      el.innerHTML = optionsHtml;
      if (curVal) el.value = curVal;
    }
  });
}

function onExpCatChange() {
  const cat = document.getElementById('expCat')?.value;
  const jarSel = document.getElementById('expJar');
  if (cat && jarSel && CAT_TO_JAR[cat]) {
    jarSel.value = CAT_TO_JAR[cat];
  }
}
window.onExpCatChange = onExpCatChange;

function onQfExpCatChange() {
  const cat = document.getElementById('qfExpCat')?.value;
  const jarSel = document.getElementById('qfExpJar');
  if (cat && jarSel && CAT_TO_JAR[cat]) {
    jarSel.value = CAT_TO_JAR[cat];
  }
}
window.onQfExpCatChange = onQfExpCatChange;

function quickDepositToJar(jarName) {
  const incJar = document.getElementById('incJar');
  if (incJar) incJar.value = jarName;
  const qfIncJar = document.getElementById('qfIncJar');
  if (qfIncJar) qfIncJar.value = jarName;

  if (typeof window.openQuickFinanceModal === 'function') {
    window.openQuickFinanceModal('inc');
    if (qfIncJar) qfIncJar.value = jarName;
  } else {
    const incAmt = document.getElementById('incAmt');
    if (incAmt) {
      incAmt.scrollIntoView({ behavior: 'smooth', block: 'center' });
      incAmt.focus();
    }
  }
  if (window.toast) window.toast(`Nhập số tiền để nạp vào hũ "${jarName}"`, 'info');
}
window.quickDepositToJar = quickDepositToJar;

// ── Modal Thêm Hũ Mới ──
function openAddJarModal() {
  const m = document.getElementById('mAddJar');
  if (!m) return;
  m.classList.add('active');
  document.getElementById('newJarName').value = '';
  document.getElementById('newJarDesc').value = '';
  document.getElementById('newJarEmoji').value = '🫙';
  document.getElementById('newJarColor').value = '#8B5CF6';
}
window.openAddJarModal = openAddJarModal;

function closeAddJarModal() {
  const m = document.getElementById('mAddJar');
  if (m) m.classList.remove('active');
}
window.closeAddJarModal = closeAddJarModal;

async function saveNewJar() {
  const name = document.getElementById('newJarName')?.value.trim();
  if (!name) {
    if (window.toast) window.toast('Vui lòng nhập tên hũ chi tiêu!', 'error');
    return;
  }
  const desc = document.getElementById('newJarDesc')?.value.trim() || 'Hũ chi tiêu cá nhân';
  const emoji = document.getElementById('newJarEmoji')?.value.trim() || '🫙';
  const color = document.getElementById('newJarColor')?.value || '#8B5CF6';

  const jars = (window.DB && Array.isArray(window.DB.jars)) ? [...window.DB.jars] : [...DEFAULT_JARS];
  const newId = 'jar_' + Date.now();
  jars.push({ id: newId, name, desc, emoji, color, defaultPercent: 10, updatedAt: Date.now() });

  window.DB.jars = jars;
  if (typeof window.persist === 'function') {
    await window.persist('jars', jars);
  }
  closeAddJarModal();
  populateJarSelects();
  renderJarsGrid();
  if (window.toast) window.toast(`Đã thêm hũ "${name}" thành công!`, 'success');
}
window.saveNewJar = saveNewJar;

// ── Modal Phân Bổ Thu Nhập Tự Động ──
function openAllocateIncomeModal(defaultAmt) {
  const m = document.getElementById('mAllocJars');
  if (!m) return;
  m.classList.add('active');
  const amtInp = document.getElementById('allocTotalAmt');
  if (amtInp && defaultAmt) amtInp.value = defaultAmt;
  renderAllocateRows();
}
window.openAllocateIncomeModal = openAllocateIncomeModal;

function closeAllocateIncomeModal() {
  const m = document.getElementById('mAllocJars');
  if (m) m.classList.remove('active');
}
window.closeAllocateIncomeModal = closeAllocateIncomeModal;

function renderAllocateRows() {
  const container = document.getElementById('allocJarsList');
  if (!container) return;
  const jars = getAllJars();
  const totalAmt = parseFloat(document.getElementById('allocTotalAmt')?.value) || 0;

  container.innerHTML = jars.map(j => {
    const pct = j.defaultPercent || 10;
    const calculated = totalAmt > 0 ? Math.round((totalAmt * pct) / 100) : 0;
    return `
      <div style="display:flex; align-items:center; justify-content:space-between; gap:10px; padding:8px 12px; background:var(--surface-2); border-radius:8px; margin-bottom:8px;">
        <div style="display:flex; align-items:center; gap:8px; width:140px;">
          <span>${j.emoji || '🫙'}</span>
          <span style="font-size:13px; font-weight:600; color:var(--text-primary); white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${j.name}</span>
        </div>
        <div style="display:flex; align-items:center; gap:6px;">
          <input type="number" class="inp alloc-pct-inp" data-jar="${j.name}" value="${pct}" style="width:60px; padding:4px 6px; font-size:12px; text-align:center;" oninput="recalcAllocAmounts()">
          <span style="font-size:12px; color:var(--text-muted);">%</span>
        </div>
        <div style="font-size:13px; font-weight:700; color:var(--accent-light); min-width:100px; text-align:right;" id="allocAmt_${j.id}">
          ${window.fmt ? window.fmt(calculated) : calculated} ₫
        </div>
      </div>
    `;
  }).join('');
}
window.renderAllocateRows = renderAllocateRows;

function recalcAllocAmounts() {
  const totalAmt = parseFloat(document.getElementById('allocTotalAmt')?.value) || 0;
  const jars = getAllJars();
  jars.forEach(j => {
    const pctInp = document.querySelector(`.alloc-pct-inp[data-jar="${j.name}"]`);
    const pct = pctInp ? (parseFloat(pctInp.value) || 0) : 0;
    const calc = totalAmt > 0 ? Math.round((totalAmt * pct) / 100) : 0;
    const el = document.getElementById(`allocAmt_${j.id}`);
    if (el) el.textContent = (window.fmt ? window.fmt(calc) : calc) + ' ₫';
  });
}
window.recalcAllocAmounts = recalcAllocAmounts;

async function executeIncomeAllocation() {
  const totalAmt = parseFloat(document.getElementById('allocTotalAmt')?.value);
  if (!totalAmt || totalAmt <= 0) {
    if (window.toast) window.toast('Vui lòng nhập tổng số tiền cần phân bổ!', 'error');
    return;
  }
  const date = document.getElementById('allocDate')?.value || (window.today ? window.today() : new Date().toISOString().slice(0, 10));
  const note = document.getElementById('allocNote')?.value.trim() || 'Phân bổ thu nhập vào các hũ';

  const jars = getAllJars();
  const newIncomes = [];
  let allocatedSum = 0;

  jars.forEach(j => {
    const pctInp = document.querySelector(`.alloc-pct-inp[data-jar="${j.name}"]`);
    const pct = pctInp ? (parseFloat(pctInp.value) || 0) : 0;
    if (pct > 0) {
      const amt = Math.round((totalAmt * pct) / 100);
      if (amt > 0) {
        allocatedSum += amt;
        newIncomes.push({
          id: window.uid ? window.uid() : ('inc_' + Math.random().toString(36).substr(2, 9)),
          src: 'Phân bổ thu nhập',
          amt,
          date,
          jar: j.name,
          note: `${note} (${pct}% vào ${j.name})`,
          updatedAt: Date.now()
        });
      }
    }
  });

  if (newIncomes.length === 0) {
    if (window.toast) window.toast('Tỷ lệ phân bổ phải lớn hơn 0%!', 'error');
    return;
  }

  const list = [...(window.DB.income || [])];
  newIncomes.reverse().forEach(inc => list.unshift(inc));
  window.DB.income = list;

  if (typeof window.persist === 'function') {
    await window.persist('income', list);
  }

  closeAllocateIncomeModal();
  if (typeof window.renderFinKpi === 'function') window.renderFinKpi();
  if (typeof window.renderFinTables === 'function') window.renderFinTables();
  renderJarsGrid();

  if (window.toast) window.toast(`Đã phân bổ thành công ${window.fmt ? window.fmt(allocatedSum) : allocatedSum} ₫ vào ${newIncomes.length} hũ!`, 'success');
}
window.executeIncomeAllocation = executeIncomeAllocation;

// Export symbols to window
window.LifeOSJars = {
  DEFAULT_JARS,
  CAT_TO_JAR,
  getAllJars,
  getJarByName,
  calculateJarStats,
  renderJarsGrid,
  populateJarSelects,
  getActiveJarFilter: () => activeJarFilter
};
