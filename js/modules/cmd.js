/* ════════════════════════════════════════════════════════════
  COMMAND PALETTE (Ctrl+K)
════════════════════════════════════════════════════════════ */
let cmdItems = [];
let cmdSelIdx = -1;

function openCmd() {
  document.getElementById('cmdInput').value = '';
  document.getElementById('cmdResults').innerHTML = '<div style="color:var(--text3); font-size:13px; text-align:center; padding:20px;">Gõ để tìm kiếm...</div>';
  cmdItems = [];
  cmdSelIdx = -1;
  openModal('mCmd');
  setTimeout(() => document.getElementById('cmdInput').focus(), 50);
}

document.addEventListener('keydown', (e) => {
  // Ctrl+K to open Command Palette
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault();
    openCmd();
  }
  
  // Shortcuts
  if (e.key === 'Escape') {
    document.querySelectorAll('.overlay').forEach(o => o.style.display = 'none');
  }
  
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'n') {
    e.preventDefault();
    // Default create based on active page
    const p = document.querySelector('.page.active')?.id;
    if(p === 'p-calendar' || p === 'p-schedule' || p === 'p-today') openEv();
    else if(p === 'p-todos') openTodoModal();
    else if(p === 'p-projects') openProjModal();
    else if(p === 'p-notes') openNote();
    else if(p === 'p-vocab') openVocab();
  }
  
  // Navigation in Command Palette
  const mCmd = document.getElementById('mCmd');
  if (mCmd && mCmd.style.display === 'flex') {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (cmdSelIdx < cmdItems.length - 1) cmdSelIdx++;
      updateCmdSelection();
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (cmdSelIdx > 0) cmdSelIdx--;
      updateCmdSelection();
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (cmdSelIdx >= 0 && cmdSelIdx < cmdItems.length) {
        executeCmd(cmdItems[cmdSelIdx]);
      }
    }
  }
});

function updateCmdSelection() {
  const els = document.querySelectorAll('.cmd-res-item');
  els.forEach((el, i) => {
    if (i === cmdSelIdx) {
      el.style.background = 'var(--surface2)';
      el.scrollIntoView({block: 'nearest'});
    } else {
      el.style.background = 'transparent';
    }
  });
}

function executeCmd(item) {
  closeModal('mCmd');
  if (item.type === 'ai') {
    closeModal('mCmd');
    if (typeof window.askAiQuick === 'function') {
      window.askAiQuick(item.query);
    }
    return;
  } else if (item.type === 'action') {
    item.action();
  } else if (item.type === 'todo') {
    nav('todos', document.querySelector('[data-page="todos"]'));
    editTodo(item.data.id);
  } else if (item.type === 'note') {
    nav('notes', document.querySelector('[data-page="notes"]'));
    openNote(item.data.id);
  } else if (item.type === 'event') {
    nav('calendar', document.querySelector('[data-page="calendar"]'));
    editEv(item.data.id);
  } else if (item.type === 'project') {
    nav('projects', document.querySelector('[data-page="projects"]'));
    currentProjId = item.data.id;
    const sel = document.getElementById('projSelector');
    if(sel) sel.value = item.data.id;
    renderKanbanBoard();
  } else if (item.type === 'ptask') {
    nav('projects', document.querySelector('[data-page="projects"]'));
    currentProjId = item.data.projId;
    const sel = document.getElementById('projSelector');
    if(sel) sel.value = item.data.projId;
    renderKanbanBoard();
    openProjTaskModal(item.data.id);
  }
}

// Bind search input
setTimeout(() => {
  const inp = document.getElementById('cmdInput');
  if (inp) inp.addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase().trim();
    const resEl = document.getElementById('cmdResults');
    if (!q) {
      resEl.innerHTML = '<div style="color:var(--text3); font-size:13px; text-align:center; padding:20px;">Gõ để tìm kiếm...</div>';
      cmdItems = [];
      cmdSelIdx = -1;
      return;
    }
    
    cmdItems = [];
    
    // Tag parsing
    let searchTag = null;
    let searchText = q;
    if (q.startsWith('#')) {
      const parts = q.split(' ');
      searchTag = parts[0].substring(1);
      searchText = parts.slice(1).join(' ').trim();
    }
    
    const matchesTag = (itemTags) => {
      if (!searchTag) return true;
      if (!itemTags || !Array.isArray(itemTags)) return false;
      return itemTags.some(t => t.toLowerCase().includes(searchTag));
    };
    
    // 1. Actions (Quick actions)
    const actions = [
      { text: 'Tạo Ghi chú mới', icon: 'sticky-note', action: openNote },
      { text: 'Tạo Việc cần làm mới (Todo)', icon: 'check-circle', action: openTodoModal },
      { text: 'Tạo Sự kiện mới', icon: 'calendar', action: openEv },
      { text: 'Tạo Dự án mới', icon: 'folder', action: openProjModal },
      { text: 'Xem Bảng Kanban (Todos)', icon: 'kanban', action: () => { nav('todos', document.querySelector('[onclick*="todos"]')); switchTodoView('kanban'); } },
      { text: 'Xem Danh sách (Todos)', icon: 'list', action: () => { nav('todos', document.querySelector('[onclick*="todos"]')); switchTodoView('list'); } }
    ];
    actions.forEach(a => {
      if (a.text.toLowerCase().includes(q)) cmdItems.push({type: 'action', text: a.text, icon: a.icon, action: a.action});
    });
    
    // 2. Todos
    (window.DB.todos || []).forEach(t => {
      if (matchesTag(t.tags) && (t.text.toLowerCase().includes(searchText) || (t.desc && t.desc.toLowerCase().includes(searchText)))) {
        cmdItems.push({type: 'todo', text: t.text, desc: 'Todo', icon: 'check-square', data: t});
      }
    });
    
    // 3. Notes
    (window.DB.notes || []).forEach(n => {
      if (matchesTag(n.tags) && (!searchText || (n.title && n.title.toLowerCase().includes(searchText)) || (n.body && n.body.toLowerCase().includes(searchText)))) {
        cmdItems.push({type: 'note', text: n.title || 'Ghi chú không tên', desc: 'Ghi chú', icon: 'file-text', data: n});
      }
    });
    
    // 4. Events
    (window.DB.events || []).forEach(ev => {
      if ((ev.title && ev.title.toLowerCase().includes(q)) || (ev.desc && ev.desc.toLowerCase().includes(q))) {
        cmdItems.push({type: 'event', text: ev.title, desc: 'Sự kiện (' + ev.dateStart + ')', icon: 'calendar-days', data: ev});
      }
    });
    
    // 5. Projects
    (window.DB.projects || []).forEach(p => {
      if (matchesTag(p.tags) && (!searchText || (p.name && p.name.toLowerCase().includes(searchText)))) {
        cmdItems.push({type: 'project', text: p.name, desc: 'Dự án', icon: 'folder-git-2', data: p});
      }
    });
    
    // 6. Project Tasks
    (window.DB.proj_tasks || []).forEach(pt => {
      if ((pt.text && pt.text.toLowerCase().includes(q)) || (pt.desc && pt.desc.toLowerCase().includes(q))) {
        const p = (window.DB.projects || []).find(x => x.id === pt.projId);
        cmdItems.push({type: 'ptask', text: pt.text, desc: 'Nhiệm vụ trong ' + (p?p.name:'Dự án'), icon: 'list-todo', data: pt});
      }
    });
    
    // Limit to 20 results
    cmdItems = cmdItems.slice(0, 20);
    
    // Always prepend or provide "Ask LifeOS AI" option
    cmdItems.unshift({
      type: 'ai',
      text: `✨ Hỏi LifeOS AI: "${q}"`,
      desc: 'Yêu cầu AI thực hiện hoặc giải đáp ngay',
      icon: 'sparkles',
      query: q
    });
    if (cmdItems.length === 0) {
      resEl.innerHTML = '<div style="color:var(--text3); font-size:13px; text-align:center; padding:20px;">Không tìm thấy kết quả.</div>';
    } else {
      resEl.innerHTML = cmdItems.map((item, i) => `
        <div class="cmd-res-item" style="padding:10px 15px; display:flex; align-items:center; gap:12px; cursor:pointer; border-radius:6px;" onmouseover="cmdSelIdx=${i}; updateCmdSelection()" onclick="executeCmd(cmdItems[${i}])">
         <i data-lucide="${item.icon}" style="width:16px;height:16px; color:var(--text2)"></i>
         <div style="flex:1;">
          <div style="font-size:14px; font-weight:500; color:var(--text1)">${item.text}</div>
          ${item.desc ? `<div style="font-size:11px; color:var(--text3); margin-top:2px;">${item.desc}</div>` : ''}
         </div>
        </div>
      `).join('');
      if (window.lucide) window.lucide.createIcons();
    }
    
    cmdSelIdx = cmdItems.length > 0 ? 0 : -1;
    updateCmdSelection();
  });
}, 1000);


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
  const text = inp.value.trim();
  if(!text) return;
  currentTodoSubtasks.push({text, done:false});
  inp.value = '';
  renderTodoSubtasks();
}
function toggleTodoSubtask(idx) {
  currentTodoSubtasks[idx].done = !currentTodoSubtasks[idx].done;
  renderTodoSubtasks();
}
function delTodoSubtask(idx) {
  currentTodoSubtasks.splice(idx, 1);
  renderTodoSubtasks();
}

