/* ════════════════════════════════════════════════════════════
  MOCK TESTS (IELTS / TOEIC SCORE TRACKER)
════════════════════════════════════════════════════════════ */
let mockChartInstance = null;

function openMockTest(id) {
 document.getElementById('mockId').value = '';
 document.getElementById('mName').value = '';
 document.getElementById('mDate').value = today();
 document.getElementById('mList').value = '';
 document.getElementById('mRead').value = '';
 document.getElementById('mSpeak').value = '';
 document.getElementById('mWrite').value = '';
 document.getElementById('mNote').value = '';
 document.getElementById('mMockTestTitle').textContent = 'Nhập Điểm Thi Thử';
 if (id) {
  const m = (window.DB.mocktests || []).find(x => x.id === id);
  if (m) {
   document.getElementById('mockId').value = m.id;
   document.getElementById('mName').value = m.name;
   document.getElementById('mDate').value = m.date;
   document.getElementById('mList').value = m.list || '';
   document.getElementById('mRead').value = m.read || '';
   document.getElementById('mSpeak').value = m.speak || '';
   document.getElementById('mWrite').value = m.write || '';
   document.getElementById('mNote').value = m.note || '';
   document.getElementById('mMockTestTitle').textContent = 'Sửa Điểm Thi';
  }
 }
 openModal('mMockTest');
}

async function saveMockTest() {
 const name = document.getElementById('mName').value.trim();
 if (!name) { toast('Vui lòng nhập tên bài thi', 'error'); return; }
 const date = document.getElementById('mDate').value || today();
 const list = Number(document.getElementById('mList').value) || 0;
 const read = Number(document.getElementById('mRead').value) || 0;
 const speak = Number(document.getElementById('mSpeak').value) || 0;
 const write = Number(document.getElementById('mWrite').value) || 0;
 const note = document.getElementById('mNote').value.trim();
 const id = document.getElementById('mockId').value || uid();
 const total = list + read + speak + write;
 
 if (!window.DB.mocktests) window.DB.mocktests = [];
 const idx = window.DB.mocktests.findIndex(x => x.id === id);
 const m = { id, name, date, list, read, speak, write, total, note };
 if (idx >= 0) window.DB.mocktests[idx] = m; else window.DB.mocktests.push(m);
 
 // Sort by date ascending
 window.DB.mocktests.sort((a,b) => a.date.localeCompare(b.date));
 
 await persist('mocktests', window.DB.mocktests);
 closeModal('mMockTest');
 renderMockTests();
 toast('Đã lưu điểm thi!', 'success');
}

async function delMockTest(id) {
 if (!confirm('Xóa kết quả này?')) return;
 window.DB.mocktests = (window.DB.mocktests || []).filter(x => x.id !== id);
 await persist('mocktests', window.DB.mocktests);
 renderMockTests();
 toast('Đã xóa bài thi!', 'success');
}

function renderMockTests() {
 const grid = document.getElementById('mockTestsList');
 if (!grid) return;
 
 const tests = window.DB.mocktests || [];
 if (tests.length === 0) {
  grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1;">
    <div class="empty-state-icon"><i data-lucide="award" class="ic-24"></i></div>
    <div class="empty-state-title">Chưa có kết quả thi thử nào</div>
    <div class="empty-state-desc">Lưu điểm thi 4 kỹ năng (Nghe, Đọc, Nói, Viết) để theo dõi biểu đồ tiến bộ theo thời gian.</div>
    <button class="btn btn-p btn-sm" onclick="openMockTest()"><i data-lucide="plus" class="ic-14"></i> Nhập điểm bài thi</button>
  </div>`;
  if (window.lucide) window.lucide.createIcons();
  return;
 }
 
 grid.innerHTML = tests.map(m => `
  <div class="mock-card card-glass" style="position:relative;">
   <button class="btn-del" onclick="delMockTest('${m.id}')" style="position:absolute;top:12px;right:12px;background:transparent;border:none;color:var(--text-low);cursor:pointer;">✕</button>
   <div class="mock-hdr" style="margin-bottom:12px;">
    <div>
     <div class="mock-title" style="cursor:pointer;font-size:15px;font-weight:700;color:var(--text-hi);" onclick="openMockTest('${m.id}')">${m.name}</div>
     <div class="mock-date" style="font-size:12px;color:var(--text-mid);margin-top:2px;">${fmtDate(m.date)}</div>
    </div>
   </div>
   <div class="mock-scores" style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px;background:var(--surface);padding:10px;border-radius:10px;border:1px solid var(--border);text-align:center;">
    <div class="ms-item"><div class="ms-item-lbl" style="font-size:11px;color:var(--text-low);">Nghe</div><div class="ms-item-val" style="font-size:15px;font-weight:700;color:var(--success);">${m.list}</div></div>
    <div class="ms-item"><div class="ms-item-lbl" style="font-size:11px;color:var(--text-low);">Đọc</div><div class="ms-item-val" style="font-size:15px;font-weight:700;color:var(--warning);">${m.read}</div></div>
    <div class="ms-item"><div class="ms-item-lbl" style="font-size:11px;color:var(--text-low);">Nói</div><div class="ms-item-val" style="font-size:15px;font-weight:700;color:var(--pink);">${m.speak}</div></div>
    <div class="ms-item"><div class="ms-item-lbl" style="font-size:11px;color:var(--text-low);">Viết</div><div class="ms-item-val" style="font-size:15px;font-weight:700;color:var(--accent-cyan);">${m.write}</div></div>
   </div>
   <div style="margin-top:10px;display:flex;justify-content:space-between;align-items:center;">
     <div style="font-size:12px;color:var(--text-mid);">${m.note || 'Không có ghi chú'}</div>
     <div class="ms-total" style="font-size:18px;font-weight:800;color:var(--accent);">Tổng: ${m.total}</div>
   </div>
  </div>
 `).join('');
 
 renderMockTestChart(tests);
 if (window.lucide) window.lucide.createIcons();
}

function renderMockTestChart(tests) {
 const ctx = document.getElementById('mockTestChart');
 if (!ctx) return;
 if (mockChartInstance) mockChartInstance.destroy();
 if (tests.length === 0) return;
 
 const labels = tests.map(t => fmtDate(t.date));
 const dataList = tests.map(t => t.list);
 const dataRead = tests.map(t => t.read);
 const dataTotal = tests.map(t => t.total);
 
 const accent = getComputedStyle(document.body).getPropertyValue('--accent').trim() || '#7c4dff';
 const green = getComputedStyle(document.body).getPropertyValue('--success').trim() || '#00e676';
 const text = getComputedStyle(document.body).getPropertyValue('--text-low').trim() || '#64648a';
 
 mockChartInstance = new Chart(ctx, {
  type: 'line',
  data: {
   labels: labels,
   datasets: [
    { label: 'Tổng điểm', data: dataTotal, borderColor: accent, backgroundColor: accent+'33', fill: true, tension: 0.3, borderWidth: 3 },
    { label: 'Nghe', data: dataList, borderColor: green, backgroundColor: 'transparent', tension: 0.3, borderDash: [5,5] },
    { label: 'Đọc', data: dataRead, borderColor: '#ff9100', backgroundColor: 'transparent', tension: 0.3, borderDash: [5,5] }
   ]
  },
  options: {
   responsive: true,
   maintainAspectRatio: false,
   plugins: {
    legend: { position: 'top', labels: { color: text, font: {family: 'Inter, sans-serif'} } }
   },
   scales: {
    x: { ticks: { color: text }, grid: { color: 'rgba(128,128,128,0.1)' } },
    y: { ticks: { color: text }, grid: { color: 'rgba(128,128,128,0.1)' }, beginAtZero: true }
   }
  }
 });
}
