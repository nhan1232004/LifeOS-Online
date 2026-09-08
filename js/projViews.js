let currentProjView = 'board';

function switchProjView(v) {
 currentProjView = v;
 const btnA = document.getElementById('btnViewAll');
 const btnB = document.getElementById('btnViewBoard');
 const btnL = document.getElementById('btnViewList');
 const btnG = document.getElementById('btnViewGantt');
 if(btnA) btnA.className = 'seg-btn ' + (v==='all'?'active':'');
 if(btnB) btnB.className = 'seg-btn ' + (v==='board'?'active':'');
 if(btnL) btnL.className = 'seg-btn ' + (v==='list'?'active':'');
 if(btnG) btnG.className = 'seg-btn ' + (v==='gantt'?'active':'');
 
 if (v === 'all') {
  currentProjId = '__all__';
  const sel = document.getElementById('projSelector');
  if(sel) sel.value = '__all__';
 } else if (currentProjId === '__all__' || !currentProjId) {
  const projs = window.DB.projects || [];
  if (projs.length > 0) {
   currentProjId = projs[0].id;
   const sel = document.getElementById('projSelector');
   if(sel) sel.value = currentProjId;
  }
 }
 renderKanbanBoard();
}

async function openProjNotes() {
 if (!currentProjId) return;
 const p = (window.DB.projects || []).find(x => x.id === currentProjId);
 if (!p) return;
 
 // A Notion project canvas is stored as a special note
 if (!p.noteId) {
  p.noteId = 'note_' + uid();
  // Save project
  const idx = window.DB.projects.findIndex(x => x.id === currentProjId);
  if(idx >= 0) window.DB.projects[idx] = p;
  await persist('projects', window.DB.projects);
 }
 
 // Create the note if it doesn't exist
 if (!window.DB.notes) window.DB.notes = [];
 let n = window.DB.notes.find(x => x.id === p.noteId);
 if (!n) {
  n = { id: p.noteId, title: 'Tài liệu dự án: ' + p.name, body: 'Bắt đầu viết tài liệu dự án tại đây...', color: '#7c4dff', date: today() };
  window.DB.notes.unshift(n);
  await persist('notes', window.DB.notes);
  renderNotes(); // if we are on notes page
 }
 
 // Open the note modal
 openNote(p.noteId);
}
