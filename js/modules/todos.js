
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

function renderTodos(){
 const t = today();
 let list = [...(window.DB.todos || [])];
 
 if (todoFilter === 'today') {
  list = list.filter(x => x.date === t || (!x.done && x.date && x.date < t) || (!x.date && !x.done));
 } else if (todoFilter === 'pending') {
  list = list.filter(x => !x.done);
 } else if (todoFilter === 'done') {
  list = list.filter(x => x.done);
 }
 
 // Sort: high priority first, then date ascending, then undone before done
 list.sort((a, b) => {
  if (a.done !== b.done) return a.done ? 1 : -1;
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
  kanbanView.style.display = 'flex';
  renderTodoKanban(list);
 } else {
  if (kanbanView) kanbanView.style.display = 'none';
  if (listView) listView.style.display = 'block';
  renderTodoList(list);
 }
}

function renderTodoList(list) {
 const el=document.getElementById('todoList'); if(!el)return;
 if (!list.length){el.innerHTML='<div class="empty">Chưa có việc cần làm.</div>';return;}
 const pl={high:'Cao',mid:'TB',low:'Thấp'};
 el.innerHTML=list.map(t=>`
 <div class="todo-item">
  <div class="todo-cb" role="checkbox" tabindex="0" aria-checked="${Boolean(t.done)}" onclick="toggleTodo('${window.LifeOSData.escapeAttr(t.id)}')" onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();toggleTodo('${window.LifeOSData.escapeAttr(t.id)}')}" style="border-color:${t.done?'var(--green)':'var(--text3)'};background:${t.done?'var(--green)':'transparent'}">
   ${t.done?'<span style="color:#000;font-size:10px;font-weight:800">✓</span>':''}
  </div>
  <div style="flex:1;min-width:0">
   <div class="todo-txt" style="${t.done?'text-decoration:line-through;color:var(--text3)':''}">${window.LifeOSData.escapeHtml(t.text)}</div>
   ${t.date?'<div class="todo-meta"> ' + fmtDate(t.date) + '</div>':''}
  </div>
  <span style="font-size:10px;padding:2px 7px;border-radius:8px;background:${PRI_C[t.priority]+'22'};color:${PRI_C[t.priority]};font-weight:700;flex-shrink:0">${pl[t.priority]||'TB'}</span>
  <button class="btn btn-sm" aria-label="Sửa việc" style="padding:3px 7px;flex-shrink:0" onclick="editTodo('${window.LifeOSData.escapeAttr(t.id)}')"><i data-lucide="pencil" style="width:14px;height:14px;"></i></button>
  <button class="btn btn-sm btn-r" aria-label="Xóa việc" style="padding:3px 7px;flex-shrink:0" onclick="delTodo('${window.LifeOSData.escapeAttr(t.id)}')"><i data-lucide="trash-2" style="width:14px;height:14px;"></i></button>
 </div>`).join('');
 if(window.lucide) lucide.createIcons();
}

function renderTodoKanban(list) {
 const cols = {high:[], mid:[], low:[]};
 list.forEach(t => {
  if(cols[t.priority]) cols[t.priority].push(t);
  else cols.mid.push(t);
 });
 
 ['high','mid','low'].forEach(pri => {
  const cntEl = document.getElementById('tkcnt_' + pri);
  if(cntEl) cntEl.textContent = cols[pri].length;
  const colEl = document.getElementById('tkcol_' + pri);
  if(colEl) {
   colEl.innerHTML = cols[pri].map(t => `
   <div class="kcard" draggable="true" ondragstart="todoDragStart(event, '${t.id}')" ondragend="todoDragEnd(event)" onclick="editTodo('${t.id}')" style="${t.done?'opacity:0.6':''}">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px;">
     <div style="font-weight:600; font-size:14px; color:var(--text1); line-height:1.4; ${t.done?'text-decoration:line-through; color:var(--text3)':''}">${t.text}</div>
     <div class="todo-cb" onclick="event.stopPropagation(); toggleTodo('${t.id}')" style="margin-left:8px; width:18px; height:18px; flex-shrink:0; border-color:${t.done?'var(--green)':'var(--text3)'};background:${t.done?'var(--green)':'transparent'}">
      ${t.done?'<span style="color:#000;font-size:10px;font-weight:800">✓</span>':''}
     </div>
    </div>
    <div style="display:flex; justify-content:space-between; align-items:center; font-size:12px; color:var(--text3);">
     ${t.date ? '<span><i data-lucide="calendar" style="width:12px;height:12px;vertical-align:middle;margin-top:-2px;"></i> ' + fmtDate(t.date) + '</span>' : '<span></span>'}
     <button class="btn btn-sm btn-r" style="padding:2px 5px; opacity:0.5;" onclick="event.stopPropagation(); delTodo('${t.id}')"><i data-lucide="trash-2" style="width:12px;height:12px;"></i></button>
    </div>
   </div>
   `).join('');
  }
 });
 if(window.lucide) lucide.createIcons();
}

window.addEventListener('DOMContentLoaded', () => {
 setTimeout(() => {
  if (currentTodoView === 'kanban') {
   document.getElementById('btnTodoViewList')?.classList.remove('active');
   document.getElementById('btnTodoViewKanban')?.classList.add('active');
  }
 }, 300);
});
