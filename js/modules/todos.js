
window.syncTodosUI = function() {
  if (typeof renderTodos === 'function') renderTodos();
  if (typeof renderToday === 'function') renderToday();
  if (typeof renderOverview === 'function') renderOverview();
  if (typeof updateBadges === 'function') updateBadges();
  if (window.lucide) window.lucide.createIcons();
};

/* ────────────────────────────────────────────────────────
  TODOS
──────────────────────────────────────────────────────── */
let todoFilter='all';
let currentTodoView = localStorage.getItem('todoView') || 'list';

function openTodoModal(){
 ['todoId','todoText','todoNote'].forEach(k=>document.getElementById(k).value='');
 if(document.getElementById('todoTags')) document.getElementById('todoTags').value='';
 document.getElementById('todoPri').value='mid';
 document.getElementById('todoDate').value=today();
 document.getElementById('todoRecur').value='';
 openModal('mTodo');
}
function editTodo(id){
 const t=(window.DB.todos||[]).find(x=>x.id===id); if(!t)return;
 document.getElementById('todoId').value=id;
 document.getElementById('todoText').value=t.text;
 document.getElementById('todoPri').value=t.priority||'mid';
 document.getElementById('todoDate').value=t.date||today();
 document.getElementById('todoRecur').value=t.recurrence||'';
 document.getElementById('todoNote').value=t.note||'';
 if(document.getElementById('todoTags')) document.getElementById('todoTags').value=(t.tags||[]).join(', ');
 openModal('mTodo');
}
async function saveTodo(){
 const text=document.getElementById('todoText').value.trim();
 if(!text){toast('Nhập nội dung!','error');return;}
 const editId=document.getElementById('todoId').value;
 const list=[...(window.DB.todos||[])];
 const tagsStr=document.getElementById('todoTags')?document.getElementById('todoTags').value:'';
 const tags=tagsStr.split(',').map(x=>x.trim()).filter(x=>x);
 const item={id:editId||uid(),text,priority:document.getElementById('todoPri').value,date:document.getElementById('todoDate').value||today(),recurrence:document.getElementById('todoRecur').value,note:document.getElementById('todoNote').value,tags,done:editId?(list.find(t=>t.id===editId)||{done:false}).done:false};
 if(editId){const i=list.findIndex(t=>t.id===editId);if(i>=0)list[i]=item;else list.unshift(item);}
 else list.unshift(item);
 await persist('todos',list);
 closeModal('mTodo'); toast('Đã lưu!','success'); window.syncTodosUI();
}
async function toggleTodo(id){
 const list=[...(window.DB.todos||[])];
 const i=list.findIndex(x=>x.id===id); if(i<0)return;
 
 const isDoneNow = !list[i].done;
 list[i]={...list[i],done:isDoneNow};
 if (isDoneNow && typeof window.hapticSuccess === 'function') window.hapticSuccess();
 else if (typeof window.hapticTap === 'function') window.hapticTap();
 
 // Spawn recurring task if marked done and hasn't spawned yet
 if (isDoneNow && list[i].recurrence && !list[i].spawnedNext) {
   list[i].spawnedNext = true;
   
   let nextDate = new Date(list[i].date || today());
   if (list[i].recurrence === 'daily') nextDate.setDate(nextDate.getDate() + 1);
   else if (list[i].recurrence === 'weekly') nextDate.setDate(nextDate.getDate() + 7);
   else if (list[i].recurrence === 'monthly') nextDate.setMonth(nextDate.getMonth() + 1);
   
   const nextDateStr = window.LifeOSData?.localDate(nextDate) || nextDate.toISOString().split('T')[0];
   
   const nextTodo = {
     ...list[i],
     id: uid(),
     date: nextDateStr,
     done: false,
     spawnedNext: false
   };
   list.unshift(nextTodo);
 }
 
 await persist('todos',list);
  window.syncTodosUI();
}
async function delTodo(id){
 await persist('todos',(window.DB.todos||[]).filter(x=>x.id!==id));
 toast('Đã xoá','info');
  window.syncTodosUI();
}
function setTodoFilter(f,el){
 todoFilter=f;
 document.querySelectorAll('#todoFtabs .ftab').forEach(t=>t.classList.remove('active'));
 if(el)el.classList.add('active');
 renderTodos();
}

function switchTodoView(view) {
 currentTodoView = view;
 localStorage.setItem('todoView', view);
 document.querySelectorAll('#todoViewTabs .btn').forEach(b => b.classList.remove('active'));
 if(view === 'list') document.getElementById('btnTodoViewList').classList.add('active');
 else if(view === 'kanban') document.getElementById('btnTodoViewKanban').classList.add('active');
 renderTodos();
}

let tDraggedId = null;
function todoDragStart(e, id) {
 tDraggedId = id;
 e.dataTransfer.effectAllowed = 'move';
 e.target.style.opacity = '0.5';
}
function todoDragEnd(e) {
 e.target.style.opacity = '1';
 tDraggedId = null;
 document.querySelectorAll('.kcol').forEach(c => c.classList.remove('drag-over'));
}
function todoDragOver(e) {
 e.preventDefault();
 e.dataTransfer.dropEffect = 'move';
 const col = e.target.closest('.kcol');
 if(col) col.classList.add('drag-over');
}
async function todoDrop(e, newPri) {
 e.preventDefault();
 document.querySelectorAll('.kcol').forEach(c => c.classList.remove('drag-over'));
 if(!tDraggedId) return;
 const list = [...(window.DB.todos||[])];
 const i = list.findIndex(x => x.id === tDraggedId);
 if(i >= 0 && list[i].priority !== newPri) {
  list[i].priority = newPri;
  await persist('todos', list);
 }
 tDraggedId = null;
}

async function quickAddTodoFromInput() {
  const inp = document.getElementById('quickTodoInput');
  if (!inp) return;
  const text = inp.value.trim();
  if (!text) return;
  const newTodo = {
    id: uid(),
    text,
    date: today(),
    priority: 'mid',
    done: false
  };
  const list = [newTodo, ...(window.DB.todos || [])];
  await persist('todos', list);
  inp.value = '';
  toast('Đã thêm công việc!', 'success');
  window.syncTodosUI();
}

function renderTodos(){
 const t = today();
 const all = window.DB.todos || [];
 const bAll = document.getElementById('badgeTodoAll');
 if (bAll) bAll.textContent = all.length;

 let list = [...all];
 if (todoFilter === 'today') {
  list = list.filter(x => x.date === t || (!x.done && x.date && x.date < t) || (!x.date && !x.done));
 } else if (todoFilter === 'pending') {
  list = list.filter(x => !x.done);
 } else if (todoFilter === 'done') {
  list = list.filter(x => x.done);
 }
 
 // Sort: undone first, overdue first, then high priority
 list.sort((a, b) => {
  if (a.done !== b.done) return a.done ? 1 : -1;
  const aOverdue = !a.done && a.date && a.date < t;
  const bOverdue = !b.done && b.date && b.date < t;
  if (aOverdue !== bOverdue) return aOverdue ? -1 : 1;
  const priWeight = { high: 3, mid: 2, low: 1 };
  const pa = priWeight[a.priority] || 2;
  const pb = priWeight[b.priority] || 2;
  if (pa !== pb) return pb - pa;
  return (a.date || '').localeCompare(b.date || '');
 });

 const listView = document.getElementById('todoListView');
 const kanbanView = document.getElementById('todoKanbanView');
 
 if (currentTodoView === 'kanban' && kanbanView) {
  if (listView) listView.style.display = 'none';
  kanbanView.style.display = 'block';
  renderTodoKanban(list);
 } else {
  if (kanbanView) kanbanView.style.display = 'none';
  if (listView) listView.style.display = 'block';
  renderTodoList(list);
 }
}

function renderTodoList(list) {
 const el = document.getElementById('todoList'); if(!el) return;
 if (!list.length) {
  el.innerHTML = `
   <div class="empty-state" style="padding:36px 16px;text-align:center;">
    <div style="width:48px;height:48px;border-radius:14px;background:rgba(139,124,255,0.12);color:var(--accent);display:flex;align-items:center;justify-content:center;margin:0 auto 12px;">
     <i data-lucide="check-circle-2" class="ic-24"></i>
    </div>
    <div style="font-size:15px;font-weight:700;color:var(--text-primary);margin-bottom:4px">Không có công việc nào</div>
    <div style="font-size:12.5px;color:var(--text-muted);margin-bottom:14px">Thêm công việc vào danh sách để quản lý hiệu quả hơn.</div>
    <button class="btn btn-p btn-sm" onclick="openTodoModal()"><i data-lucide="plus" class="ic-14"></i> Thêm việc mới</button>
   </div>`;
  if (window.lucide) lucide.createIcons();
  return;
 }
 const pl = { high: 'Cao', mid: 'TB', low: 'Thấp' };
 const t = today();
 el.innerHTML = list.map(item => {
  const isOverdue = !item.done && item.date && item.date < t;
  const priClass = item.priority === 'high' ? 'p-high' : item.priority === 'mid' ? 'p-med' : 'p-low';
  return `
  <div class="modern-todo-row ${item.done ? 'done' : ''}">
   <div class="modern-todo-cb" role="checkbox" aria-checked="${Boolean(item.done)}" onclick="toggleTodo('${window.LifeOSData.escapeAttr(item.id)}')">
    ${item.done ? '<span style="font-size:11px;font-weight:900;color:#fff">✓</span>' : ''}
   </div>
   <div class="modern-todo-body">
    <div class="modern-todo-text">${window.LifeOSData.escapeHtml(item.text)}</div>
    <div class="modern-todo-meta">
     <span class="bento-priority-badge ${priClass}">${pl[item.priority] || 'TB'}</span>
     ${isOverdue ? '<span class="modern-tag-chip modern-tag-overdue">⚠️ Quá hạn</span>' : ''}
     ${item.date ? `<span class="modern-tag-chip modern-tag-date"><i data-lucide="calendar" class="ic-10"></i> ${fmtDate(item.date)}</span>` : ''}
     ${item.project ? `<span class="modern-tag-chip modern-tag-project"><i data-lucide="folder" class="ic-10"></i> ${item.project}</span>` : ''}
     ${item.note ? `<span style="color:var(--text-muted);max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">💬 ${window.LifeOSData.escapeHtml(item.note)}</span>` : ''}
    </div>
   </div>
   <div class="modern-todo-actions">
    <button class="icon-btn" aria-label="Sửa việc" onclick="editTodo('${window.LifeOSData.escapeAttr(item.id)}')"><i data-lucide="pencil" class="ic-14"></i></button>
    <button class="icon-btn" aria-label="Xóa việc" style="color:var(--danger)" onclick="delTodo('${window.LifeOSData.escapeAttr(item.id)}')"><i data-lucide="trash-2" class="ic-14"></i></button>
   </div>
  </div>`;
 }).join('');
 if (window.lucide) lucide.createIcons();
}

function renderTodoKanban(list) {
 const cols = { high: [], mid: [], low: [] };
 list.forEach(t => {
  if (cols[t.priority]) cols[t.priority].push(t);
  else cols.mid.push(t);
 });
 
 ['high', 'mid', 'low'].forEach(pri => {
  const cntEl = document.getElementById('tkcnt_' + pri);
  if (cntEl) cntEl.textContent = cols[pri].length;
  const colEl = document.getElementById('tkcol_' + pri);
  if (colEl) {
   colEl.innerHTML = cols[pri].map(t => `
   <div class="kcard" draggable="true" ondragstart="todoDragStart(event, '${t.id}')" ondragend="todoDragEnd(event)" onclick="editTodo('${t.id}')" style="${t.done ? 'opacity:0.6' : ''}">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px;">
     <div style="font-weight:600; font-size:13.5px; color:var(--text-primary); line-height:1.4; ${t.done ? 'text-decoration:line-through; color:var(--text-muted)' : ''}">${window.LifeOSData.escapeHtml(t.text)}</div>
     <div class="modern-todo-cb" onclick="event.stopPropagation(); toggleTodo('${t.id}')" style="margin-left:8px; width:18px; height:18px; flex-shrink:0;">
      ${t.done ? '<span style="font-size:10px;font-weight:900;color:#fff">✓</span>' : ''}
     </div>
    </div>
    <div style="display:flex; justify-content:space-between; align-items:center; font-size:11.5px; color:var(--text-muted);">
     ${t.date ? `<span><i data-lucide="calendar" class="ic-12"></i> ${fmtDate(t.date)}</span>` : '<span></span>'}
     <button class="icon-btn" style="color:var(--danger);padding:2px" onclick="event.stopPropagation(); delTodo('${t.id}')"><i data-lucide="trash-2" class="ic-12"></i></button>
    </div>
   </div>
   `).join('');
  }
 });
 if (window.lucide) lucide.createIcons();
}

window.addEventListener('DOMContentLoaded', () => {
 setTimeout(() => {
  if (currentTodoView === 'kanban') {
   document.getElementById('btnTodoViewList')?.classList.remove('active');
   document.getElementById('btnTodoViewKanban')?.classList.add('active');
  }
 }, 300);
});

let currentTodoSubtasks = [];
function renderTodoSubtasks() {
  const el = document.getElementById('todoSubtaskList');
  if(!el) return;
  if (!currentTodoSubtasks.length) {
    el.innerHTML = '<div style="font-size:12px; color:var(--text3); font-style:italic;">Chưa có nhiệm vụ con</div>';
    return;
  }
  el.innerHTML = currentTodoSubtasks.map((st, i) => `
    <div style="display:flex; align-items:center; gap:10px; background:var(--surface); padding:6px 10px; border-radius:6px;">
      <input type="checkbox" ${st.done ? 'checked' : ''} onchange="toggleTodoSubtask(${i})">
      <span style="flex:1; font-size:14px; ${st.done ? 'text-decoration:line-through; color:var(--text3)' : ''}">${st.text}</span>
      <button class="icon-btn" onclick="delTodoSubtask(${i})"><i data-lucide="trash-2" style="width:14px;height:14px;color:var(--red)"></i></button>
    </div>
  `).join('');
  if(window.lucide) window.lucide.createIcons();
}
function addTodoSubtaskUI() {
  const inp = document.getElementById('todoNewSubtask');
  if (!inp) return;
  const text = inp.value.trim();
  if(!text) return;
  currentTodoSubtasks.push({text, done:false});
  inp.value = '';
  renderTodoSubtasks();
}
function toggleTodoSubtask(idx) {
  if (currentTodoSubtasks[idx]) {
    currentTodoSubtasks[idx].done = !currentTodoSubtasks[idx].done;
    renderTodoSubtasks();
  }
}
function delTodoSubtask(idx) {
  currentTodoSubtasks.splice(idx, 1);
  renderTodoSubtasks();
}
window.renderTodoSubtasks = renderTodoSubtasks;
window.addTodoSubtaskUI = addTodoSubtaskUI;
window.toggleTodoSubtask = toggleTodoSubtask;
window.delTodoSubtask = delTodoSubtask;

