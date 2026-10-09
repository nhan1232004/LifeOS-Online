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
 if (!entries.length) {
  c.innerHTML = `
   <div class="empty-state" style="padding:48px 20px;">
    <div class="empty-state-icon"><i data-lucide="book-heart" class="ic-24"></i></div>
    <div class="empty-state-title">Chưa có nhật ký nào</div>
    <div class="empty-state-desc">Ghi lại cảm xúc, bài học hoặc suy nghĩ của bạn hôm nay.</div>
    <button class="btn btn-p btn-sm" onclick="openJournal()" style="margin-top:12px;"><i data-lucide="plus" class="ic-14"></i> Viết nhật ký</button>
   </div>`;
  if (window.lucide) window.lucide.createIcons();
  return;
 }
 c.innerHTML = entries.map(j => `
  <div class="modern-journal-card">
   <div class="mj-header">
    <div class="mj-date"><i data-lucide="calendar" class="ic-14"></i> <span>${fmtDate(j.date)}</span></div>
    <div style="display:flex;align-items:center;gap:8px">
     ${j.mood ? `<div class="mj-mood">${j.mood}</div>` : ''}
     <button class="btn btn-sm btn-subtle" onclick="openJournal('${j.id}')" title="Chỉnh sửa"><i data-lucide="pencil" class="ic-14"></i></button>
     <button class="btn btn-sm btn-subtle" onclick="delJournal('${j.id}')" title="Xóa" style="color:var(--danger)"><i data-lucide="trash-2" class="ic-14"></i></button>
    </div>
   </div>
   <div class="mj-body">${(j.body || '').replace(/</g, '&lt;')}</div>
  </div>
 `).join('');
 if (window.lucide) window.lucide.createIcons();
}

