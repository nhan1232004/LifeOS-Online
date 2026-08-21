/* ════════════════════════════════════════════════════════════
  VOCAB FLASHCARDS
════════════════════════════════════════════════════════════ */
function openVocab(id) {
 document.getElementById('vocabId').value = '';
 document.getElementById('vWord').value = '';
 document.getElementById('vPron').value = '';
 document.getElementById('vMean').value = '';
 document.getElementById('vEx').value = '';
 document.getElementById('vType').value = 'n';
 document.getElementById('mVocabTitle').textContent = 'Thêm Từ Mới';
 if (id) {
  const v = (window.DB.vocab || []).find(x => x.id === id);
  if (v) {
   document.getElementById('vocabId').value = v.id;
   document.getElementById('vWord').value = v.word;
   document.getElementById('vPron').value = v.pron || '';
   document.getElementById('vMean').value = v.mean;
   document.getElementById('vEx').value = v.ex || '';
   document.getElementById('vType').value = v.type || 'n';
   document.getElementById('mVocabTitle').textContent = 'Sửa Từ Vựng';
  }
 }
 openModal('mVocab');
}

async function saveVocab() {
 const word = document.getElementById('vWord').value.trim();
 const mean = document.getElementById('vMean').value.trim();
 if (!word || !mean) { toast('Nhập đủ Từ vựng và Nghĩa', 'error'); return; }
 const pron = document.getElementById('vPron').value.trim();
 const ex = document.getElementById('vEx').value.trim();
 const type = document.getElementById('vType').value;
 const id = document.getElementById('vocabId').value || uid();
 if (!window.DB.vocab) window.DB.vocab = [];
 const idx = window.DB.vocab.findIndex(x => x.id === id);
 const voc = { id, word, mean, pron, ex, type, date: today() };
 if (idx >= 0) window.DB.vocab[idx] = voc; else window.DB.vocab.unshift(voc);
 await persist('vocab', window.DB.vocab);
 closeModal('mVocab');
 renderVocab();
 toast('Đã lưu từ vựng!', 'success');
}

async function delVocab(id) {
 if (!confirm('Xóa từ vựng này?')) return;
 window.DB.vocab = (window.DB.vocab || []).filter(x => x.id !== id);
 await persist('vocab', window.DB.vocab);
 renderVocab();
 toast('Đã xóa', 'info');
}

function renderVocab() {
 const c = document.getElementById('vocabGrid'); if (!c) return;
 const q = (document.getElementById('vocabSearch')?.value || '').toLowerCase();
 const vocs = (window.DB.vocab || []).filter(v =>
  !q || v.word.toLowerCase().includes(q) || v.mean.toLowerCase().includes(q)
 );
 if (!vocs.length) { c.innerHTML = '<div style="color:var(--text3);padding:30px;grid-column:1/-1;text-align:center">Chưa có từ vựng nào.</div>'; return; }
 c.innerHTML = vocs.map(v => `
  <div class="vocab-card" onclick="openVocab('${v.id}')">
   <div class="vc-type">(${v.type})</div>
   <div class="vc-word">${v.word}</div>
   <div class="vc-pron">${v.pron || ''}</div>
   <div class="vc-mean">${v.mean}</div>
   <button class="btn-del" onclick="event.stopPropagation();delVocab('${v.id}')"><i data-lucide="trash-2" style="width:14px;height:14px;margin-right:4px"></i> Xóa</button>
  </div>
 `).join('');
}
document.getElementById('vocabSearch')?.addEventListener('input', renderVocab);

let reviewList = [];
let reviewIdx = 0;

function reviewVocab() {
 reviewList = [...(window.DB.vocab || [])];
 if (!reviewList.length) { toast('Chưa có từ vựng để ôn tập!', 'error'); return; }
 // Shuffle list
 reviewList.sort(() => Math.random() - 0.5);
 reviewIdx = 0;
 updateReviewUI();
 document.querySelector('.flashcard-container').classList.remove('flipped');
 openModal('mReviewVocab');
}

function updateReviewUI() {
 if (!reviewList.length) return;
 const v = reviewList[reviewIdx];
 document.getElementById('rvCount').textContent = `Ôn tập (${reviewIdx + 1}/${reviewList.length})`;
 document.getElementById('rvWord').textContent = v.word;
 document.getElementById('rvPron').textContent = v.pron || '';
 document.getElementById('rvType').textContent = '(' + (v.type || 'n') + ')';
 document.getElementById('rvMean').textContent = v.mean;
 document.getElementById('rvEx').textContent = v.ex || '';
 document.querySelector('.flashcard-container').classList.remove('flipped');
}

function nextVocab() {
 if (reviewIdx < reviewList.length - 1) { reviewIdx++; updateReviewUI(); }
 else { toast('Bạn đã ôn xong!', 'success'); endReviewVocab(); }
}
function prevVocab() {
 if (reviewIdx > 0) { reviewIdx--; updateReviewUI(); }
}
function endReviewVocab() {
 closeModal('mReviewVocab');
}
