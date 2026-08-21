/* ════════════════════════════════════════════════════════════
  JOURNAL
════════════════════════════════════════════════════════════ */
let selectedMood = '';
function pickMood(el) {
 document.querySelectorAll('.mood-btn').forEach(b => b.classList.remove('active'));
 el.classList.add('active');
 selectedMood = el.dataset.mood;
}
function openJournal(id) {
 document.getElementById('journalId').value = '';
 document.getElementById('journalDate').value = today();
 document.getElementById('journalBody').value = '';
 selectedMood = '';
 document.querySelectorAll('.mood-btn').forEach(b => b.classList.remove('active'));
 if (id) {
  const j = (window.DB.journal || []).find(x => x.id === id);
  if (j) {
   document.getElementById('journalId').value = j.id;
   document.getElementById('journalDate').value = j.date;
   document.getElementById('journalBody').value = j.body || '';
   selectedMood = j.mood || '';
   document.querySelectorAll('.mood-btn').forEach(b => b.classList.toggle('active', b.dataset.mood === j.mood));
  }
 }
 openModal('mJournal');
}
async function saveJournal() {
 const body = document.getElementById('journalBody').value.trim();
 if (!body) { toast('Vui lòng viết gì đó!', 'error'); return; }
 const date = document.getElementById('journalDate').value || today();
 const id = document.getElementById('journalId').value || uid();
 if (!window.DB.journal) window.DB.journal = [];
 const idx = window.DB.journal.findIndex(x => x.id === id);
 const entry = { id, date, mood: selectedMood, body };
 if (idx >= 0) window.DB.journal[idx] = entry; else window.DB.journal.unshift(entry);
 await persist('journal', window.DB.journal);
 closeModal('mJournal');
 renderJournal();
 toast('Đã lưu nhật ký!', 'success');
}
async function delJournal(id) {
 if (!confirm('Xóa nhật ký này?')) return;
 window.DB.journal = (window.DB.journal || []).filter(x => x.id !== id);
 await persist('journal', window.DB.journal);
 renderJournal();
 toast('Đã xóa nhật ký', 'info');
}
function renderJournal() {
 const c = document.getElementById('journalContainer'); if (!c) return;
 const entries = (window.DB.journal || []).sort((a, b) => b.date.localeCompare(a.date));
 if (!entries.length) { c.innerHTML = '<div style="color:var(--text3);padding:30px;text-align:center">Chưa có nhật ký nào. Bấm "+ Viết nhật ký" để bắt đầu!</div>'; return; }
 c.innerHTML = entries.map(j => `
  <div class="journal-entry">
   <div class="je-top">
    <div class="je-date">${fmtDate(j.date)}</div>
    <div style="display:flex;align-items:center;gap:8px">
     <div class="je-mood">${j.mood || ''}</div>
     <button class="btn btn-sm" onclick="openJournal('${j.id}')" style="padding:3px 6px"><i data-lucide="pencil" style="width:14px;height:14px;"></i></button>
     <button class="btn btn-sm" onclick="delJournal('${j.id}')" style="padding:3px 6px"><i data-lucide="trash-2" style="width:14px;height:14px;"></i></button>
    </div>
   </div>
   <div class="je-body">${(j.body || '').replace(/</g, '&lt;')}</div>
  </div>
 `).join('');
}
