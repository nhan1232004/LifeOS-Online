/* ────────────────────────────────────────────────────────
  WORKSPACE MANAGER
──────────────────────────────────────────────────────── */
async function loadWorkspaces() {
  if (window.DEMO_MODE) {
    return [{id:'default', name:'Personal'}];
  }
  const uDoc = await window.getDoc(window.doc(window.db, 'users', currentUser.uid));
  let wss = [{id:'default', name:'Personal'}];
  if (uDoc.exists() && uDoc.data().workspaces) {
    wss = [{id:'default', name:'Personal'}, ...uDoc.data().workspaces];
  }
  return wss;
}

async function renderWorkspaceSwitcher() {
  const wss = await loadWorkspaces();
  const list = document.getElementById('wsList');
  if(!list) return;
  const cw = localStorage.getItem('currentWorkspace') || 'default';
  
  let html = '';
  wss.forEach(w => {
    const isAct = w.id === cw;
    const initial = w.name.charAt(0).toUpperCase();
    html += `<div class="ws-item ${isAct?'active':''}" onclick="switchWorkspace('${w.id}', '${w.name}')">
      <div class="ws-icon">${initial}</div>
      <div style="font-weight:500">${w.name}</div>
    </div>`;
  });
  list.innerHTML = html;
  
  // Update sidebar icon
  const curWs = wss.find(x => x.id === cw) || wss[0];
  const si = document.getElementById('currentWsIcon');
  const sn = document.getElementById('currentWsName');
  if(si) si.innerText = curWs.name.charAt(0).toUpperCase();
  if(sn) sn.innerText = curWs.name;
}

function openWorkspaceSwitcher() {
  openModal('mWorkspaceSwitcher');
  renderWorkspaceSwitcher();
}

async function createWorkspace() {
  const name = document.getElementById('newWsName').value.trim();
  if(!name) return toast('Nhập tên Workspace','error');
  if(window.DEMO_MODE) return toast('Không khả dụng trong bản Demo','error');
  
  const wss = await loadWorkspaces();
  const newId = 'ws_' + uid();
  const custom = wss.filter(x => x.id !== 'default');
  custom.push({id: newId, name});
  
  await window.setDoc(window.doc(window.db, 'users', currentUser.uid), {workspaces: custom}, {merge: true});
  
  document.getElementById('newWsName').value = '';
  renderWorkspaceSwitcher();
  toast('Đã tạo Workspace mới','success');
}

function switchWorkspace(id, name) {
  if (id === (localStorage.getItem('currentWorkspace') || 'default')) {
    closeModal('mWorkspaceSwitcher');
    return;
  }
  localStorage.setItem('currentWorkspace', id);
  // Reload the entire page to let fbListen re-attach
  window.location.reload();
}
