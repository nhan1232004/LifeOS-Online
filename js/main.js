
/* ────────────────────────────────────────────────────────
  UTILS
──────────────────────────────────────────────────────── */
const toLocalDateStr = (d = new Date()) => window.LifeOSData?.localDate(d) || new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().split('T')[0];
const today = () => toLocalDateStr();
const dayOff = n => { const d=new Date(); d.setDate(d.getDate()+n); return toLocalDateStr(d); };
const fmt  = n => { if(!n)return'0'; if(n>=1e9)return(n/1e9).toFixed(2)+'B'; if(n>=1e6)return(n/1e6).toFixed(1)+'M'; if(n>=1000)return Math.round(n/1000)+'K'; return n.toLocaleString('vi-VN'); };
const fmtFull= n => Number(n||0).toLocaleString('vi-VN');
const fmtDate= d => { if(!d)return''; const dt=new Date(d+'T00:00:00'); return dt.toLocaleDateString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric'}); };
const fmtDs = d => { if(!d)return''; const dt=new Date(d+'T00:00:00'); return dt.toLocaleDateString('vi-VN',{day:'2-digit',month:'2-digit'}); };
const uid  = () => Date.now().toString(36)+Math.random().toString(36).slice(2,6);
const EC   = {work:'#7c4dff',personal:'#ff6b9d',health:'#00e676',social:'#00e5ff',finance:'#ffab40',study:'#ce93d8'};
const SB   = {Backlog:'bk','Cần làm':'bp','Đang làm':'ba','Hoàn thành':'bg'};
const PRI_C = {high:'var(--red)',mid:'var(--amber)',low:'var(--green)'};

let lastToastMsg = '';
let lastToastTime = 0;
function toast(msg, type='info'){
 const now = Date.now();
 if (msg === lastToastMsg && now - lastToastTime < 2000) return;
 lastToastMsg = msg;
 lastToastTime = now;
 
 const w = document.getElementById('toastWrap');
 if (!w) return;
 
 while (w.children.length >= 3) {
   w.removeChild(w.firstChild);
 }

 const e = document.createElement('div');
 e.className = `toast t-${type}`;
 const icons = { success: '✓', error: '⚠️', info: 'ℹ️' };
 e.setAttribute('role', type === 'error' ? 'alert' : 'status');
 e.setAttribute('aria-live', type === 'error' ? 'assertive' : 'polite');
 const icon = document.createElement('span'); icon.style.cssText='margin-right:6px;font-weight:700;'; icon.textContent=icons[type]||'•';
 const text = document.createElement('span'); text.style.flex='1'; text.textContent=String(msg);
 e.append(icon, text);
 w.appendChild(e);
 
 setTimeout(() => {
   e.style.cssText = 'opacity:0; transform:translateY(-8px); transition:all .25s ease;';
   setTimeout(() => { if (e.parentNode) e.parentNode.removeChild(e); }, 250);
 }, 3000);
}


/* ────────────────────────────────────────────────────────
  NOTIFICATIONS
──────────────────────────────────────────────────────── */
const notifiedEvs = new Set();
const notifiedTodos = new Set();

function playNotifSound() {
 try {
  const ctx = new (window.AudioContext || window.webkitAudioContext)();
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();
  osc.connect(gain);
  gain.connect(ctx.destination);
  osc.frequency.value = 880;
  osc.type = 'sine';
  gain.gain.setValueAtTime(0.3, ctx.currentTime);
  gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.8);
  osc.start(ctx.currentTime);
  osc.stop(ctx.currentTime + 0.8);
  setTimeout(() => {
   const osc2 = ctx.createOscillator();
   const gain2 = ctx.createGain();
   osc2.connect(gain2);
   gain2.connect(ctx.destination);
   osc2.frequency.value = 1100;
   osc2.type = 'sine';
   gain2.gain.setValueAtTime(0.3, ctx.currentTime);
   gain2.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.6);
   osc2.start(ctx.currentTime);
   osc2.stop(ctx.currentTime + 0.6);
  }, 300);
 } catch(e) {}
}

function sendNotif(title, body, tag) {
 if (!('Notification' in window) || Notification.permission !== 'granted') return;
 try {
  const opts = {
   body: body,
   tag: tag || '',
   icon: 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><text y=".9em" font-size="90">⚡</text></svg>'
  };
  
  // Trên điện thoại (Chrome Android), new Notification() bị chặn, phải dùng Service Worker
  if (navigator.serviceWorker) {
   navigator.serviceWorker.ready.then(reg => {
    reg.showNotification(title, opts);
   }).catch(e => {
    new Notification(title, opts);
   });
  } else {
   new Notification(title, opts);
  }
 } catch(e) {
  console.error('Notif error:', e);
 }
 playNotifSound();
}

function checkNotifications() {
 if (!('Notification' in window) || Notification.permission !== 'granted') return;
 const now = new Date();
 const tDate = toLocalDateStr(now);
 const tMins = now.getHours() * 60 + now.getMinutes();

 // Event reminders: 60 min, 15 min, 5 min, and at event time
 (window.DB.events || []).forEach(e => {
  if (e.dateStart === tDate && e.timeStart) {
   const [eh, em] = e.timeStart.split(':').map(Number);
   const evMins = eh * 60 + em;
   const diff = evMins - tMins;

   const key60 = e.id + '_60';
   const key15 = e.id + '_15';
   const key5 = e.id + '_5';
   const key0 = e.id + '_0';

   if (diff > 15 && diff <= 60 && !notifiedEvs.has(key60)) {
    sendNotif('Chuẩn bị sự kiện!', e.title + ' bắt đầu sau ' + diff + ' phút nữa', key60);
    notifiedEvs.add(key60);
   }
   if (diff > 5 && diff <= 15 && !notifiedEvs.has(key15)) {
    sendNotif('⏰ Còn ' + diff + ' phút nữa!', e.title + ' bắt đầu lúc ' + e.timeStart, key15);
    notifiedEvs.add(key15);
   }
   if (diff > 0 && diff <= 5 && !notifiedEvs.has(key5)) {
    sendNotif('Sắp bắt đầu!', e.title + ' – còn ' + diff + ' phút nữa!', key5);
    notifiedEvs.add(key5);
   }
   if (diff === 0 && !notifiedEvs.has(key0)) {
    sendNotif('Bắt đầu ngay!', e.title + ' đang diễn ra!', key0);
    notifiedEvs.add(key0);
   }
  }
 });

 // Todo reminders: remind once for today's undone todos (at 8:00 AM and 14:00)
 const reminderHours = [8, 14];
 const currentHour = now.getHours();
 const currentMin = now.getMinutes();
 if (reminderHours.includes(currentHour) && currentMin < 2) {
  const todayTodos = (window.DB.todos || []).filter(t => t.date === tDate && !t.done);
  const todoKey = 'todo_' + tDate + '_' + currentHour;
  if (todayTodos.length > 0 && !notifiedTodos.has(todoKey)) {
   sendNotif(' Bạn có ' + todayTodos.length + ' việc cần làm hôm nay!',
    todayTodos.slice(0, 3).map(t => '• ' + t.text).join('\n'), todoKey);
   notifiedTodos.add(todoKey);
  }
 }
}

window.initNotifications = function() {
 if ('Notification' in window) {
  setInterval(checkNotifications, 30000);
  setTimeout(checkNotifications, 3000);
 }
};
window.requestNotif = function() {
 if ('Notification' in window) {
  try {
   Notification.requestPermission().then(p => {
    if(p==='granted') toast('Đã bật thông báo!','success');
    else toast('Từ chối thông báo (Hoặc bạn đã chặn trong cài đặt)','error');
    if (window.renderAll) window.renderAll();
   }).catch(e => toast('Lỗi cấp quyền: ' + e.message, 'error'));
  } catch (e) {
   // Fallback cho Safari cũ
   Notification.requestPermission(function(p) {
    if(p==='granted') toast('Đã bật thông báo!','success');
    else toast('Từ chối thông báo (Hoặc bạn đã chặn trong cài đặt)','error');
    if (window.renderAll) window.renderAll();
   });
  }
 }
};

/* ────────────────────────────────────────────────────────
  THEME TOGGLE
──────────────────────────────────────────────────────── */
window.toggleTheme = function() {
 const root = document.documentElement;
 const cur = root.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
 const nxt = cur === 'light' ? 'dark' : 'light';
 root.setAttribute('data-theme', nxt);
 localStorage.setItem('lifeos_theme', nxt);
 const btn = document.getElementById('themeBtn');
 if(btn) btn.innerHTML = nxt === "light" ? "<i data-lucide='moon' style='width:18px;height:18px'></i>" : "<i data-lucide='sun' style='width:18px;height:18px'></i>"; lucide.createIcons();;
 document.getElementById('meta-theme-color').setAttribute('content', nxt==='light' ? '#f4f5f8' : '#07070f');
 
 // Re-render charts to update grid/text colors
 if (window.renderAll) window.renderAll();
};
window.addEventListener('DOMContentLoaded', () => {
 const t = localStorage.getItem('lifeos_theme') || 'dark';
 const btn = document.getElementById('themeBtn');
 if(btn) btn.innerHTML = t === "light" ? "<i data-lucide='moon' style='width:18px;height:18px'></i>" : "<i data-lucide='sun' style='width:18px;height:18px'></i>"; lucide.createIcons();;
});

/* ────────────────────────────────────────────────────────
  SYNC
──────────────────────────────────────────────────────── */
async function persist(col,items){
 const lbl=document.getElementById('syncLbl');
 const dot=document.querySelector('.sync-dot');
 if(lbl) lbl.textContent='Đang lưu...';
 if(dot) dot.style.background='var(--amber)';
 try {
  const clean = Array.isArray(items) ? items.filter(x => x && typeof x === 'object') : [];
  await window.fbSaveAll(col,clean);
  if (window.DB) window.DB[col] = clean;
  if(lbl) lbl.textContent='Đã đồng bộ';
  if(dot) dot.style.background='var(--green)';
 } catch (error) {
  console.error('[LifeOS] persist error for "' + col + '":', error);
  if(lbl) lbl.textContent='Lỗi đồng bộ';
  if(dot) dot.style.background='var(--red)';
  // Don't toast for every persist error - can be spammy during offline
  // Only show once per minute
  if (!window._lastPersistErrTime || Date.now() - window._lastPersistErrTime > 60000) {
   window._lastPersistErrTime = Date.now();
   toast('Không thể lưu dữ liệu "' + col + '". Kiểm tra kết nối mạng.', 'error');
  }
 }
}
window.persist = persist;

/* ────────────────────────────────────────────────────────
  SIDEBAR / MOBILE NAV
──────────────────────────────────────────────────────── */
function toggleMobileSidebar(){
 const sb = document.getElementById('sidebar');
 const ov = document.getElementById('sbOverlay');
 const isOpen = sb.classList.contains('mobile-open');
 if(isOpen) {
  closeMobileSidebar();
 // Auto-scroll mobile nav to show active item
 const activeNav = document.querySelector('.mnav-item.active');
 if(activeNav) activeNav.scrollIntoView({behavior:'smooth',inline:'center',block:'nearest'});
 } else {
  sb.classList.add('mobile-open');
  ov.classList.add('active');
  ov.style.display = 'block';
  document.body.style.overflow = 'hidden';
 }
}
function closeMobileSidebar(){
 const sb = document.getElementById('sidebar');
 const ov = document.getElementById('sbOverlay');
 if(sb) { sb.classList.remove('mobile-open'); sb.style.transform = ''; }
 if(ov) { ov.classList.remove('active'); ov.style.display = 'none'; }
 document.body.style.overflow = '';
}

let deferredPrompt;
window.addEventListener('beforeinstallprompt', (e) => {
 e.preventDefault();
 deferredPrompt = e;
 const btn = document.getElementById('installAppBtn');
 if(btn) btn.style.display = 'block';
});

async function installApp() {
 if (!deferredPrompt) return;
 deferredPrompt.prompt();
 const { outcome } = await deferredPrompt.userChoice;
 if (outcome === 'accepted') {
  const btn = document.getElementById('installAppBtn');
  if(btn) btn.style.display = 'none';
 }
 deferredPrompt = null;
}

const PAGE_TITLES={today:'Hôm nay',overview:'Dashboard',calendar:'Lịch tháng',schedule:'Lịch tuần',projects:'Dự án',todos:'Việc cần làm',finance:'Thu chi',stats:'Thống kê',notes:'Ghi chú',habits:'Thói quen',goals:'Mục tiêu',journal:'Nhật ký',pomodoro:'Pomodoro',vocab:'Từ vựng',mocktests:'Mock Tests'};

function nav(pg, el){
 document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
 document.querySelectorAll('.sb-item, .mnav-item').forEach(i => i.classList.remove('active'));
 
 const pageEl = document.getElementById('p-' + pg);
 if (pageEl) pageEl.classList.add('active');
 
 // activate matching sidebar + mobile nav items
 document.querySelectorAll('.sb-item').forEach(i => {
   if (i.getAttribute('onclick') && i.getAttribute('onclick').includes(`'${pg}'`)) i.classList.add('active');
 });
 document.querySelectorAll('.mnav-item').forEach(i => {
   if (i.dataset.page === pg) i.classList.add('active');
 });
 
 const topTitleEl = document.getElementById('topTitle');
 if (topTitleEl) topTitleEl.textContent = PAGE_TITLES[pg] || pg;
 
 closeMobileSidebar();
 
 const renders = {
   today: typeof renderToday === 'function' ? renderToday : null,
   overview: typeof renderOverview === 'function' ? renderOverview : null,
   calendar: typeof renderCal === 'function' ? renderCal : null,
   schedule: typeof renderWeek === 'function' ? renderWeek : null,
   projects: () => { if (typeof renderProjSelector === 'function') renderProjSelector(); if (typeof renderKanbanBoard === 'function') renderKanbanBoard(); },
   todos: typeof renderTodos === 'function' ? renderTodos : null,
   finance: () => { if (typeof renderFinKpi === 'function') renderFinKpi(); if (typeof renderFinTables === 'function') renderFinTables(); },
   stats: typeof renderStats === 'function' ? renderStats : null,
   notes: typeof renderNotes === 'function' ? renderNotes : null,
   habits: typeof renderHabits === 'function' ? renderHabits : null,
   goals: typeof renderGoals === 'function' ? renderGoals : null,
   journal: typeof renderJournal === 'function' ? renderJournal : null,
   pomodoro: typeof renderPomodoro === 'function' ? renderPomodoro : null,
   vocab: typeof renderVocab === 'function' ? renderVocab : null,
   mocktests: typeof renderMockTests === 'function' ? renderMockTests : null
 };
 
 setTimeout(() => {
   if (renders[pg]) renders[pg]();
   if (window.lucide) window.lucide.createIcons();
 }, 30);
}

/* ────────────────────────────────────────────────────────
  MODALS
──────────────────────────────────────────────────────── */
function openModal(id){
 const m=document.getElementById(id);
 if (!m) return;
 m.dataset.lastFocus = document.activeElement?.id || '';
 m.setAttribute('role','dialog'); m.setAttribute('aria-modal','true');
 m.classList.remove('closing');
 m.style.display='flex';
 requestAnimationFrame(()=>{m.classList.add('open');m.querySelector('input,select,textarea,button')?.focus();});
}
function closeModal(id){
 const m=document.getElementById(id);
 if (!m) return;
 m.classList.add('closing');
 setTimeout(()=>{
  m.classList.remove('open','closing');
  m.style.display='none';
  document.getElementById(m.dataset.lastFocus || '')?.focus();
 }, 250);
}

document.addEventListener('keydown', e => {
 if (e.key !== 'Escape') return;
 const modal = [...document.querySelectorAll('.overlay')].find(m => m.style.display === 'flex' || m.classList.contains('open'));
 if (modal) closeModal(modal.id);
});

document.querySelectorAll('.overlay').forEach(m=>m.addEventListener('click',e=>{if(e.target===m)m.classList.remove('open');}));

/* ────────────────────────────────────────────────────────
  EVENTS
──────────────────────────────────────────────────────── */
function openEv(date='',hour=''){
 const t=date||today();
 document.getElementById('evId').value='';
 document.getElementById('evTitle').value='';
 document.getElementById('evDs').value=t;
 document.getElementById('evDe').value=t;
 document.getElementById('evTs').value=hour;
 document.getElementById('evTe').value='';
 document.getElementById('evDesc').value='';
 document.getElementById('evRecur').value='';
 document.getElementById('mEvTitle').textContent='Thêm sự kiện';
 openModal('mEv');
}
function editEv(id){
 const ev=(window.DB.events||[]).find(e=>e.id===id); if(!ev) return;
 ['evId','evTitle','evDs','evDe','evTs','evTe','evDesc'].forEach(k=>{
  const m={evId:'id',evTitle:'title',evDs:'dateStart',evDe:'dateEnd',evTs:'timeStart',evTe:'timeEnd',evDesc:'desc'};
  document.getElementById(k).value=ev[m[k]]||'';
 });
 document.getElementById('evType').value=ev.type||'work';
 document.getElementById('evRecur').value=ev.recurrence||'';
 document.getElementById('mEvTitle').textContent='Sửa sự kiện';
 openModal('mEv');
}
async function saveEv(){
 const title=document.getElementById('evTitle').value.trim();
 if(!title){toast('Nhập tiêu đề!','error');return;}
 const ds=document.getElementById('evDs').value;
 if(!ds){toast('Chọn ngày!','error');return;}
 const editId=document.getElementById('evId').value;
 const ev={id:editId||uid(),title,dateStart:ds,dateEnd:document.getElementById('evDe').value||ds,timeStart:document.getElementById('evTs').value,timeEnd:document.getElementById('evTe').value,type:document.getElementById('evType').value,desc:document.getElementById('evDesc').value};
 const list=[...(window.DB.events||[])];
 if(editId){const i=list.findIndex(x=>x.id===editId);if(i>=0)list[i]=ev;else list.push(ev);}
 else list.push(ev);
 await persist('events',list);
 closeModal('mEv'); toast('Đã lưu sự kiện!','success');
}
async function delEv(id){
 await persist('events',(window.DB.events||[]).filter(e=>e.id!==id));
 toast('Đã xoá','info');
}

/* ────────────────────────────────────────────────────────
  PROJECTS
──────────────────────────────────────────────────────── */
let kanbanFilter='all';
let currentProjMembers = [];
let currentProjMemberNames = {};

window.renderProjMembersUI = function() {
 const list = document.getElementById('projMembersList');
 if(!list) return;
 const userIdentifier = window.currentUser?.email || window.currentUser?.uid || "unknown";
 
 if (currentProjMembers.length === 0) {
  list.innerHTML = '<div style="color:var(--text3); font-size:13px;">Chưa có thành viên nào.</div>';
  return;
 }
 
 list.innerHTML = currentProjMembers.map(email => {
  const isMe = email === userIdentifier;
  const name = currentProjMemberNames[email] || '';
  const safeEmail = window.LifeOSData.escapeHtml(email);
  return `
   <div style="display:flex; gap:8px; align-items:center; background:var(--bg3); padding:8px; border-radius:6px;">
    <div style="flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:13px; color:var(--text2);" title="${window.LifeOSData.escapeAttr(email)}">
     ${safeEmail} ${isMe ? '(Bạn)' : ''}
    </div>
    <input type="text" placeholder="Biệt danh..." value="${window.LifeOSData.escapeAttr(name)}" data-member-email="${window.LifeOSData.escapeAttr(email)}"
        onchange="window.updateProjMemberName(this.dataset.memberEmail, this.value)"
        style="width:100px; padding:4px 8px; font-size:13px;">
    ${!isMe ? `<button type="button" data-member-email="${window.LifeOSData.escapeAttr(email)}" onclick="window.removeProjMemberUI(this.dataset.memberEmail)" aria-label="Xóa thành viên" style="background:transparent; color:var(--red); border:none; cursor:pointer;">✖</button>` : `<div style="width:20px;"></div>`}
   </div>
  `;
 }).join('');
};

window.addProjMemberUI = function() {
 const input = document.getElementById('projNewMemberEmail');
 const email = input.value.trim().toLowerCase();
 if(!email || !email.includes('@')) { toast('Vui lòng nhập Email hợp lệ', 'warning'); return; }
 if(!currentProjMembers.includes(email)) {
  currentProjMembers.push(email);
 }
 input.value = '';
 window.renderProjMembersUI();
};

window.removeProjMemberUI = function(email) {
 currentProjMembers = currentProjMembers.filter(e => e !== email);
 window.renderProjMembersUI();
};

window.updateProjMemberName = function(email, name) {
 if (name.trim()) {
  currentProjMemberNames[email] = name.trim();
 } else {
  delete currentProjMemberNames[email];
 }
};

function openProjModal(status=''){
 ['projId','projName','projBudget','projTags','projDesc','projProgress','projTimeline','projEval'].forEach(k=>document.getElementById(k).value='');
 document.getElementById('projStatus').value=status||'Cần làm';
 document.getElementById('projPri').value='mid';
 document.getElementById('projDue').value='';
 document.getElementById('mProjTitle').textContent='Tạo dự án mới';
 document.getElementById('btnDelProj').style.display='none';
 window.currentProjSubtasks=[];
 if(window.renderProjSubtasksUI) window.renderProjSubtasksUI();
 openModal('mProj');
}
function editProj(id){
 const p=(window.DB.projects||[]).find(x=>x.id===id); if(!p)return;
 closeModal('mProjDetail');
 document.getElementById('projId').value=id;
 document.getElementById('projName').value=p.name;
 document.getElementById('projStatus').value=p.status;
 document.getElementById('projPri').value=p.priority||'mid';
 document.getElementById('projBudget').value=p.budget||'';
 document.getElementById('projDue').value=p.due||'';
 document.getElementById('projTags').value=(p.tags||[]).join(', ');
 document.getElementById('projDesc').value=p.desc||'';
 document.getElementById('projProgress').value=p.progress||'';
 document.getElementById('projTimeline').value=p.timeline||'';
 document.getElementById('projEval').value=p.evaluation||'';
 document.getElementById('mProjTitle').textContent='Sửa dự án';
 document.getElementById('btnDelProj').style.display='inline-block';
 
 currentProjMembers = Array.isArray(p.members) ? [...p.members] : (p.members ? p.members.split(',').map(s=>s.trim()).filter(Boolean) : []);
 currentProjMemberNames = p.memberNames ? JSON.parse(JSON.stringify(p.memberNames)) : {};
 window.renderProjMembersUI();
 
 openModal('mProj');
}
function openProjDetail(id){
 const p=(window.DB.projects||[]).find(x=>x.id===id); if(!p)return;
 document.getElementById('mdTitle').textContent=p.name;
 document.getElementById('mdSubtitle').textContent=(p.tags||[]).join(' • ');
 
 const sb = {'Backlog':'bk','Cần làm':'ba','Đang làm':'bw','Hoàn thành':'bs'}[p.status]||'bk';
 document.getElementById('mdStatus').innerHTML=`<span class="badge ${sb}">${p.status}</span>`;
 document.getElementById('mdDue').textContent=p.due?fmtDate(p.due):'—';
 
 let prog = p.progress !== undefined ? p.progress : (p.status==='Hoàn thành'?100:p.status==='Đang làm'?50:0);
 document.getElementById('mdProgFill').style.width = prog+'%';
 document.getElementById('mdProgFill').style.background = prog===100?'var(--green)':prog>0?'var(--accent)':'var(--text3)';
 document.getElementById('mdProgTxt').textContent = prog+'%';
 
 let mems = p.members || [];
 if (typeof mems === 'string') mems = mems.split(',').map(x=>x.trim()).filter(Boolean);
 const names = mems.map(email => p.memberNames && p.memberNames[email] ? p.memberNames[email] : email);
 document.getElementById('mdMembers').textContent = names.length > 0 ? names.join(', ') : 'Chưa có thành viên';
 
 document.getElementById('mdBudget').textContent=p.budget?fmt(p.budget)+' ₫':'0 ₫';
 document.getElementById('mdDesc').textContent=p.desc||'Chưa có mô tả.';
 document.getElementById('mdTimeline').textContent=p.timeline||'Chưa có hoạt động nào.';
 document.getElementById('mdEval').textContent=p.evaluation||'Chưa có đánh giá.';
 
 const bDetail = document.getElementById('btnEditProjDetail');
 if (bDetail) bDetail.setAttribute('onclick', `editProj('${p.id}')`);
 const bTool = document.getElementById('btnEditProj');
 if (bTool) bTool.setAttribute('onclick', `editProj('${p.id}')`);
 openModal('mProjDetail');
}
async function saveProj(){
 try {
  const name = document.getElementById('projName').value.trim();
  if(!name){ toast('Nhập tên dự án!', 'error'); return; }
  const editId = document.getElementById('projId').value;
  const userIdentifier = window.currentUser?.email || window.currentUser?.uid || "unknown";
  if (!currentProjMembers.includes(userIdentifier)) currentProjMembers.push(userIdentifier);
  
  const p = {
   id: editId || uid(),
   ownerUid: editId ? ((window.DB.projects || []).find(x => x.id === editId)?.ownerUid || window.currentUser?.uid) : window.currentUser?.uid,
   memberUids: editId ? ((window.DB.projects || []).find(x => x.id === editId)?.memberUids || [window.currentUser?.uid].filter(Boolean)) : [window.currentUser?.uid].filter(Boolean),
   name,
   status: document.getElementById('projStatus').value,
   priority: document.getElementById('projPri').value || 'mid',
   budget: parseFloat(document.getElementById('projBudget').value) || 0,
   due: document.getElementById('projDue').value || '',
   tags: document.getElementById('projTags').value.split(',').map(t=>t.trim()).filter(Boolean),
   desc: document.getElementById('projDesc').value || '',
   progress: parseInt(document.getElementById('projProgress').value) || 0,
   members: currentProjMembers,
   memberNames: currentProjMemberNames,
   timeline: document.getElementById('projTimeline').value || '',
   evaluation: document.getElementById('projEval').value || ''
  };
  
  // 1. Immediately update in-memory DB
  const list = [...(window.DB.projects || [])];
  if(editId) {
   const i = list.findIndex(x => x.id === editId);
   if(i >= 0) list[i] = p; else list.unshift(p);
  } else {
   list.unshift(p);
  }
  window.DB.projects = list;
  currentProjId = p.id;
  
  // 2. Persist to primary storage (localStorage in Demo, users/{uid}/data/projects in Firebase)
  await persist('projects', list);
  
  // 3. Sync to shared_projects if Firebase is available
  if (!window.DEMO_MODE && window.db && window.setDoc && window.doc) {
   try {
    await window.setDoc(window.doc(window.db, 'shared_projects', p.id), p);
    await Promise.all(currentProjMembers.filter(email => email !== userIdentifier).map(async email => {
      try { await window.sendProjectInvite?.(p.id, email); }
      catch (inviteError) { console.warn('Invite could not be sent', inviteError); }
    }));
   } catch(syncErr) {
    console.warn('[LifeOS] Firestore shared_projects sync warning (saved locally & personal db):', syncErr.message);
   }
  }
  
  closeModal('mProj');
  toast('Đã lưu dự án thành công!', 'success');
  
  // 4. Update UI immediately
  if (typeof currentProjView !== 'undefined' && currentProjView === 'all') {
   currentProjView = 'board';
   const btnA = document.getElementById('btnViewAll');
   const btnB = document.getElementById('btnViewBoard');
   if(btnA) btnA.className = 'seg-btn';
   if(btnB) btnB.className = 'seg-btn active';
  }
  renderProjSelector();
  const sel = document.getElementById('projSelector');
  if(sel) sel.value = p.id;
  renderKanbanBoard();
  if (typeof updateBadges === 'function') updateBadges();
 } catch(globalErr) {
  console.error("LỖI saveProj:", globalErr);
  toast("Lỗi khi lưu dự án: " + globalErr.message, "error");
 }
}

function editCurrentProj() {
 if (currentProjId && currentProjId !== '__all__') editProj(currentProjId);
}

async function moveProj(id, status){
 const list = [...(window.DB.projects || [])];
 const i = list.findIndex(x => x.id === id);
 if(i < 0) return;
 list[i] = { ...list[i], status };
 window.DB.projects = list;
 await persist('projects', list);
 renderProjSelector();
 renderKanbanBoard();

 if (!window.DEMO_MODE && window.db && window.setDoc && window.doc) {
  try {
   await window.setDoc(window.doc(window.db, 'shared_projects', id), list[i]);
  } catch(e) {
   console.warn('[LifeOS] Firestore shared_projects move warning:', e.message);
  }
 }
}

async function delProj(id){
 const newProjs = (window.DB.projects || []).filter(x => x.id !== id);
 window.DB.projects = newProjs;
 await persist('projects', newProjs);
 const remainingTasks = (window.DB.proj_tasks || []).filter(t => t.projId !== id);
 window.DB.proj_tasks = remainingTasks;
 await persist('proj_tasks', remainingTasks);

 if (!window.DEMO_MODE && window.db && window.deleteDoc && window.doc) {
  try {
   await window.deleteDoc(window.doc(window.db, 'shared_projects', id));
   const pTasks = (window.DB.proj_tasks || []).filter(t => t.projId === id);
   for(const t of pTasks) {
    try { await window.deleteDoc(window.doc(window.db, 'shared_tasks', t.id)); } catch(e){}
   }
  } catch(e) {
   console.warn('[LifeOS] Firestore shared_projects delete warning:', e.message);
  }
 }

 if (currentProjId === id) {
  currentProjId = newProjs.length > 0 ? newProjs[0].id : '__all__';
  const sel = document.getElementById('projSelector');
  if(sel) sel.value = currentProjId;
 }
 renderProjSelector();
 renderKanbanBoard();
 if (typeof updateBadges === 'function') updateBadges();
 toast('Đã xoá dự án!', 'info');
}

async function delProjAction() {
 if(!confirm('Bạn có chắc chắn muốn xóa dự án này? Mọi nhiệm vụ bên trong cũng sẽ bị xóa.')) return;
 const id = document.getElementById('projId').value;
 if(id) {
  await delProj(id);
  closeModal('mProj');
 }
}

var currentProjId = window.currentProjId = '';

window.handleProjSelectorChange = function(val) {
 if (!val || val === '__all__') {
  currentProjId = '__all__';
  if (typeof switchProjView === 'function') switchProjView('all');
  else renderKanbanBoard();
 } else {
  currentProjId = val;
  if (typeof currentProjView !== 'undefined' && currentProjView === 'all') {
   if (typeof switchProjView === 'function') switchProjView('board');
   else renderKanbanBoard();
  } else {
   renderKanbanBoard();
  }
 }
};

function renderProjSelector() {
 const sel = document.getElementById('projSelector');
 if(!sel) return;
 const projs = window.DB.projects || [];
 let html = `<option value="__all__">📁 Tất cả dự án (${projs.length})</option>`;
 if (projs.length > 0) {
  html += '<option disabled>───────────────</option>';
  html += projs.map(p => `<option value="${p.id}">${window.LifeOSData?.escapeAttr(p.name) || p.name} (${p.status || 'Chưa làm'})</option>`).join('');
 }
 sel.innerHTML = html;

 if (currentProjId && projs.find(x => x.id === currentProjId)) {
  sel.value = currentProjId;
 } else if (typeof currentProjView !== 'undefined' && currentProjView === 'all') {
  currentProjId = '__all__';
  sel.value = '__all__';
 } else if (projs.length > 0) {
  currentProjId = projs[0].id;
  sel.value = currentProjId;
 } else {
  currentProjId = '__all__';
  sel.value = '__all__';
 }
}

function renderProjHeaderBanner(p) {
 const banner = document.getElementById('projHeaderBanner');
 if(!banner) return;
 if(!p) { banner.style.display = 'none'; banner.innerHTML = ''; return; }
 banner.style.display = 'block';
 
 const prog = p.progress !== undefined ? p.progress : (p.status==='Hoàn thành'?100:p.status==='Đang làm'?50:0);
 const progColor = prog === 100 ? 'var(--green)' : prog > 0 ? 'var(--accent)' : 'var(--text3)';
 const sb = {'Backlog':'bk','Cần làm':'ba','Đang làm':'bw','Hoàn thành':'bs'}[p.status]||'bk';
 const priText = {'high':'Ưu tiên: Cao','mid':'Ưu tiên: TB','low':'Ưu tiên: Thấp'}[p.priority||'mid']||'Ưu tiên: TB';
 const priClass = (p.priority||'mid') === 'high' ? 'b-red' : (p.priority||'mid') === 'low' ? 'b-green' : 'b-orange';
 
 const allTasks = window.DB.proj_tasks || [];
 const pTasks = allTasks.filter(t => t.projId === p.id);
 const doneTasks = pTasks.filter(t => t.status === 'Hoàn thành').length;
 const tagsHtml = (p.tags||[]).map(t => `<span class="proj-banner-tag">${window.LifeOSData?.escapeAttr(t)||t}</span>`).join('');
 
 banner.innerHTML = `
  <div class="proj-banner-card">
   <div class="proj-banner-top">
    <div class="proj-banner-title-wrap">
     <div class="proj-banner-badges">
      <span class="badge ${sb}">${p.status||'Chưa làm'}</span>
      <span class="badge ${priClass}">${priText}</span>
      ${tagsHtml}
     </div>
     <h2 class="proj-banner-title">${window.LifeOSData?.escapeAttr(p.name)||p.name}</h2>
     ${p.desc ? `<p class="proj-banner-desc">${window.LifeOSData?.escapeAttr(p.desc)||p.desc}</p>` : ''}
    </div>
    <div class="proj-banner-actions">
     <button class="btn btn-sm btn-outline" onclick="openProjDetail('${p.id}')" title="Xem đầy đủ hồ sơ dự án">
      <i data-lucide="info" class="ic-14"></i> Chi tiết
     </button>
     <button class="btn btn-sm btn-outline" onclick="editProj('${p.id}')" title="Chỉnh sửa thông tin">
      <i data-lucide="edit-3" class="ic-14"></i> Sửa
     </button>
     <button class="btn btn-sm btn-p" onclick="openProjTaskModal()" title="Thêm việc mới">
      <i data-lucide="plus" class="ic-14"></i> Thêm việc
     </button>
    </div>
   </div>
   
   <div class="proj-banner-metrics">
    <div class="proj-metric-item">
     <span class="proj-metric-lbl">Tiến độ</span>
     <div class="proj-metric-val">
      <div class="prog" style="width:120px;height:8px;background:var(--glass-b);border-radius:99px;overflow:hidden;margin-right:8px;">
       <div class="prog-fill" style="width:${prog}%;background:${progColor};height:100%;border-radius:99px;transition:width 0.3s"></div>
      </div>
      <span style="font-weight:700;color:var(--text-hi);font-size:13px;">${prog}%</span>
     </div>
    </div>
    
    <div class="proj-metric-item">
     <span class="proj-metric-lbl">Nhiệm vụ</span>
     <span class="proj-metric-val" style="font-weight:600;font-size:13px;color:var(--text-hi);">
      ${doneTasks}/${pTasks.length} việc (${pTasks.length ? Math.round((doneTasks/pTasks.length)*100) : 0}%)
     </span>
    </div>

    ${p.due ? `
    <div class="proj-metric-item">
     <span class="proj-metric-lbl">Hạn chót</span>
     <span class="proj-metric-val" style="font-size:13px;color:${p.due < today() ? 'var(--red)' : 'var(--text-hi)'};font-weight:600;">
      <i data-lucide="calendar" class="ic-14" style="margin-right:4px;vertical-align:-2px"></i> ${fmtDate(p.due)}
     </span>
    </div>` : ''}

    ${p.budget ? `
    <div class="proj-metric-item">
     <span class="proj-metric-lbl">Ngân sách</span>
     <span class="proj-metric-val" style="font-size:13px;color:var(--text-hi);font-weight:600;">
      <i data-lucide="wallet" class="ic-14" style="margin-right:4px;vertical-align:-2px"></i> ${fmt(p.budget)} ₫
     </span>
    </div>` : ''}
   </div>
  </div>
 `;
 if (window.lucide) window.lucide.createIcons();
}

function renderProjGrid() {
 const container = document.getElementById('projGridView');
 if(!container) return;
 const projs = window.DB.projects || [];
 const allTasks = window.DB.proj_tasks || [];
 
 let cardsHtml = `
  <div class="proj-card proj-card-create" onclick="openProjModal()">
   <div class="proj-card-create-icon">
    <i data-lucide="folder-plus" style="width:28px;height:28px"></i>
   </div>
   <div style="font-weight:700;font-size:15px;margin-top:10px;color:var(--text-hi)">Tạo dự án mới</div>
   <div style="font-size:12px;color:var(--text-mid);margin-top:4px">Thêm dự án để theo dõi tiến độ & công việc</div>
  </div>
 `;
 
 projs.forEach(p => {
  const pTasks = allTasks.filter(t => t.projId === p.id);
  const doneTasks = pTasks.filter(t => t.status === 'Hoàn thành').length;
  const prog = p.progress !== undefined ? p.progress : (p.status==='Hoàn thành'?100:p.status==='Đang làm'?50:0);
  const progColor = prog === 100 ? 'var(--green)' : prog > 0 ? 'var(--accent)' : 'var(--text3)';
  const sb = {'Backlog':'bk','Cần làm':'ba','Đang làm':'bw','Hoàn thành':'bs'}[p.status]||'bk';
  const tagsHtml = (p.tags||[]).slice(0, 3).map(t => `<span class="badge bp" style="font-size:10px;padding:2px 6px;">${window.LifeOSData?.escapeAttr(t)||t}</span>`).join('');
  
  cardsHtml += `
   <div class="proj-card" onclick="selectProjAndOpen('${p.id}')">
    <div class="proj-card-header">
     <span class="badge ${sb}">${p.status||'Chưa làm'}</span>
     <div style="display:flex;gap:4px;" onclick="event.stopPropagation()">
      <button class="btn btn-sm icon-btn-tool" onclick="editProj('${p.id}')" title="Sửa dự án" style="padding:4px"><i data-lucide="edit-3" class="ic-14"></i></button>
      <button class="btn btn-sm icon-btn-tool" onclick="openProjDetail('${p.id}')" title="Xem chi tiết" style="padding:4px"><i data-lucide="eye" class="ic-14"></i></button>
     </div>
    </div>
    <div class="proj-card-body">
     <h3 class="proj-card-title">${window.LifeOSData?.escapeAttr(p.name)||p.name}</h3>
     ${p.desc ? `<p class="proj-card-desc">${window.LifeOSData?.escapeAttr(p.desc)||p.desc}</p>` : ''}
     ${tagsHtml ? `<div style="display:flex;flex-wrap:wrap;gap:4px;margin-top:8px">${tagsHtml}</div>` : ''}
    </div>
    <div class="proj-card-footer">
     <div style="margin-bottom:8px;">
      <div style="display:flex;justify-content:space-between;font-size:11px;color:var(--text-mid);margin-bottom:4px">
       <span>Tiến độ</span>
       <span style="font-weight:700;color:var(--text-hi)">${prog}%</span>
      </div>
      <div class="prog" style="height:6px;background:var(--glass-b);border-radius:99px;overflow:hidden">
       <div class="prog-fill" style="width:${prog}%;background:${progColor};height:100%;border-radius:99px"></div>
      </div>
     </div>
     <div class="proj-card-meta">
      <span style="font-size:11.5px;color:var(--text-mid)"><i data-lucide="check-square" class="ic-12" style="margin-right:2px"></i> ${doneTasks}/${pTasks.length} việc</span>
      ${p.due ? `<span style="font-size:11.5px;color:${p.due<today()?'var(--red)':'var(--text-mid)'}"><i data-lucide="calendar" class="ic-12" style="margin-right:2px"></i> ${fmtDate(p.due)}</span>` : ''}
     </div>
    </div>
   </div>
  `;
 });
 
 container.innerHTML = `<div class="proj-cards-grid">${cardsHtml}</div>`;
 if (window.lucide) window.lucide.createIcons();
}

window.selectProjAndOpen = function(id) {
 currentProjId = id;
 const sel = document.getElementById('projSelector');
 if(sel) sel.value = id;
 if(typeof switchProjView === 'function') {
  switchProjView('board');
 } else {
  renderKanbanBoard();
 }
};

function renderKanbanBoard() {
 const sel = document.getElementById('projSelector');
 if(sel && sel.value) currentProjId = sel.value;
 const btnAdd = document.getElementById('btnAddPTask');
 const btnInvite = document.getElementById('btnInvite');
 const btnChat = document.getElementById('btnProjChat');
 const btnEdit = document.getElementById('btnEditProj');
 const board = document.getElementById('kanbanBoard');
 const listView = document.getElementById('projListView');
 const empty = document.getElementById('kanbanEmpty');
 const chatPanel = document.getElementById('projChatPanel');
 const viewSwitcher = document.getElementById('projViewSwitcher');
 const btnNotes = document.getElementById('btnProjNotes');
 const gridView = document.getElementById('projGridView');
 const banner = document.getElementById('projHeaderBanner');

 const projs = window.DB.projects || [];
 const isAllView = (typeof currentProjView !== 'undefined' && currentProjView === 'all') || currentProjId === '__all__' || (!currentProjId && projs.length === 0);

 if (isAllView) {
  if(btnAdd) btnAdd.style.display = 'none';
  if(btnInvite) btnInvite.style.display = 'none';
  if(btnChat) btnChat.style.display = 'none';
  if(btnEdit) btnEdit.style.display = 'none';
  if(board) board.style.display = 'none';
  if(listView) listView.style.display = 'none';
  const ganttView = document.getElementById('projGanttView');
  if(ganttView) ganttView.style.display = 'none';
  if(empty) empty.style.display = 'none';
  if(chatPanel) chatPanel.style.display = 'none';
  if(viewSwitcher) viewSwitcher.style.display = 'flex';
  if(btnNotes) btnNotes.style.display = 'none';
  if(banner) { banner.style.display = 'none'; banner.innerHTML = ''; }
  if(gridView) gridView.style.display = 'block';
  if(window.stopChatListener) window.stopChatListener();
  renderProjGrid();
  return;
 }

 // If a project ID is selected or defaulting to first project
 if (!currentProjId && projs.length > 0) {
  currentProjId = projs[0].id;
  if(sel) sel.value = currentProjId;
 }

 const currentProj = projs.find(x => x.id === currentProjId);
 if (!currentProj) {
  currentProjId = '__all__';
  if(sel) sel.value = '__all__';
  if(typeof switchProjView === 'function') switchProjView('all');
  else renderKanbanBoard();
  return;
 }

 if(gridView) gridView.style.display = 'none';
 renderProjHeaderBanner(currentProj);

 if(btnAdd) btnAdd.style.display = 'inline-block';
 if(btnInvite) btnInvite.style.display = 'inline-block';
 if(btnChat) btnChat.style.display = 'inline-block';
 if(btnEdit) btnEdit.style.display = 'inline-block';
 if(viewSwitcher) viewSwitcher.style.display = 'flex';
 if(btnNotes) btnNotes.style.display = 'inline-block';
 
 if (typeof currentProjView !== 'undefined' && currentProjView === 'list') {
  if(board) board.style.display = 'none';
  if(listView) listView.style.display = 'block';
  const ganttView = document.getElementById('projGanttView');
  if(ganttView) ganttView.style.display = 'none';
 } else if (typeof currentProjView !== 'undefined' && currentProjView === 'gantt') {
  if(board) board.style.display = 'none';
  if(listView) listView.style.display = 'none';
  const ganttView = document.getElementById('projGanttView');
  if(ganttView) ganttView.style.display = 'block';
  if (typeof renderProjGantt === 'function') renderProjGantt(currentProjId);
 } else {
  if(board) board.style.display = 'grid';
  if(listView) listView.style.display = 'none';
  const ganttView = document.getElementById('projGanttView');
  if(ganttView) ganttView.style.display = 'none';
 }
 if(empty) empty.style.display = 'none';
 if(chatPanel && chatPanel.style.display !== 'none' && !window.DEMO_MODE) window.startChatListener();

 const allTasks = window.DB.proj_tasks || [];
 const pTasks = allTasks.filter(t => t.projId === currentProjId);

 const statuses = ['Khởi tạo', 'Cần làm', 'Đang làm', 'Hoàn thành'];
 const ids = ['init', 'todo', 'doing', 'done'];

 statuses.forEach((st, i) => {
  const tasks = pTasks.filter(t => t.status === st);
  const cnt = document.getElementById('kcnt_' + ids[i]);
  const col = document.getElementById('kcol_' + ids[i]);
  if(cnt) cnt.textContent = tasks.length;
  if(col) {
   col.innerHTML = tasks.map(t => {
    let memHtml = '';
    if (t.members && t.members.length) {
     const pNames = currentProj?.memberNames || {};
     memHtml = t.members.map(m => {
      const n = pNames[m] || m.split('@')[0];
      return `<span style="background:var(--bg3); padding:2px 6px; border-radius:12px; font-size:10px; color:var(--text2);">${n}</span>`;
     }).join('');
    }
    return `
    <div class="kcard" draggable="true" ondragstart="kDragStart(event, '${t.id}')" onclick="openProjTaskModal('${t.id}')">
     <div class="kpri pri-${(t.priority||'m')[0]}"></div>
     <div class="kcard-title">${t.text}</div>
     <div style="display:flex; justify-content:space-between; align-items:center; margin-top:6px;">
      <div style="display:flex; flex-wrap:wrap; gap:4px;">${memHtml}</div>
      ${t.comments && t.comments.length ? `<div style="font-size:10px;color:var(--text3);"><i data-lucide="message-circle" style="width:12px;height:12px"></i> ${t.comments.length}</div>` : ''}
     </div>
    </div>
   `;}).join('');
  }
 });
 
 const tbody = document.getElementById('projListTbody');
 if (tbody) {
  let listHtml = '';
  pTasks.forEach(t => {
   let memHtml = '';
   if (t.members && t.members.length) {
    const pNames = currentProj?.memberNames || {};
    memHtml = t.members.map(m => {
     const n = pNames[m] || m.split('@')[0];
     return `<span style="background:var(--bg3); padding:2px 6px; border-radius:12px; font-size:10px; color:var(--text2); margin-right:4px;">${n}</span>`;
    }).join('');
   }
   listHtml += `<tr style="border-bottom:1px solid var(--border); cursor:pointer;" onclick="openProjTaskModal('${t.id}')">
    <td style="padding:10px; font-weight:500;">${t.text}</td>
    <td style="padding:10px;">${t.status}</td>
    <td style="padding:10px;"><span class="badge ${t.priority==='high'?'b-red':t.priority==='low'?'b-green':'b-orange'}">${t.priority==='high'?'Cao':t.priority==='low'?'Thấp':'TB'}</span></td>
    <td style="padding:10px;">${memHtml}</td>
   </tr>`;
  });
  if(!pTasks.length) listHtml = '<tr><td colspan="4" style="padding:20px; text-align:center; color:var(--text2)">Chưa có nhiệm vụ nào</td></tr>';
  tbody.innerHTML = listHtml;
 }
}

function kDragStart(ev, id) {
 ev.dataTransfer.setData("text/plain", id);
}
function kDragOver(ev) {
 ev.preventDefault();
}
async function kDrop(ev, status) {
 ev.preventDefault();
 const id = ev.dataTransfer.getData("text/plain");
 if(!id) return;

 const list = [...(window.DB.proj_tasks || [])];
 const idx = list.findIndex(x => x.id === id);
 if (idx < 0) return;
 const t = { ...list[idx], status };
 list[idx] = t;
 window.DB.proj_tasks = list;
 await persist('proj_tasks', list);
 renderKanbanBoard();

 if (!window.DEMO_MODE && window.db && window.setDoc && window.doc) {
  try {
   await window.setDoc(window.doc(window.db, 'shared_tasks', t.id), t);
  } catch(e) {
   console.warn('[LifeOS] Firestore shared_tasks kDrop warning:', e.message);
  }
 }
}

let chatUnsubscribe = null; window.stopChatListener = function() { if(chatUnsubscribe) { chatUnsubscribe(); chatUnsubscribe = null; } };
window.toggleProjChat = function() {
 const panel = document.getElementById('projChatPanel');
 if(!panel) return;
 if(panel.style.display === 'none') {
  panel.style.display = 'flex';
  document.getElementById('chatUnread').style.display = 'none';
  if(!window.DEMO_MODE) window.startChatListener();
 } else {
  panel.style.display = 'none';
  if(chatUnsubscribe) { chatUnsubscribe(); chatUnsubscribe = null; }
 }
};

window.startChatListener = function() {
 if(chatUnsubscribe) chatUnsubscribe();
 if(!currentProjId || !window.db) return;
 
 const q = window.query(window.collection(window.db, 'shared_projects', currentProjId, 'messages'), window.orderBy('timestamp', 'asc'), window.limit(50));
 chatUnsubscribe = window.onSnapshot(q, snap => {
  const messages = [];
  snap.forEach(doc => messages.push({id: doc.id, ...doc.data()}));
  window.renderChatMessages(messages);
 });
};

window.renderChatMessages = function(msgs) {
 const container = document.getElementById('projChatMessages');
 if(!container) return;
 const currentProj = (window.DB.projects||[]).find(x=>x.id===currentProjId);
 const pNames = currentProj?.memberNames || {};
 const userIdentifier = window.currentUser?.email || window.currentUser?.uid || "unknown";
 
 container.innerHTML = msgs.map(m => {
  const isMe = m.sender === userIdentifier;
  const name = isMe ? 'Bạn' : (pNames[m.sender] || m.sender.split('@')[0]);
  return `
   <div style="display:flex; flex-direction:column; align-items: ${isMe?'flex-end':'flex-start'};">
    <span style="font-size:10px; color:var(--text3); margin-bottom:2px;">${name}</span>
    <div style="background:${isMe?'var(--accent)':'var(--bg3)'}; padding:6px 10px; border-radius:8px; max-width:85%; word-wrap:break-word;">
     ${m.text}
    </div>
   </div>
  `;
 }).join('');
 container.scrollTop = container.scrollHeight;
 
 const panel = document.getElementById('projChatPanel');
 if(panel && panel.style.display === 'none' && msgs.length > 0) {
  document.getElementById('chatUnread').style.display = 'block';
 }
};

window.sendProjMessage = async function() {
 const input = document.getElementById('projChatInput');
 const text = input.value.trim();
 if(!text || !currentProjId || window.DEMO_MODE) return;
 
 const userIdentifier = window.currentUser?.email || window.currentUser?.uid || "unknown";
 input.value = '';
 try {
 await window.addDoc(window.collection(window.db, 'shared_projects', currentProjId, 'messages'), {
   text,
   sender: userIdentifier,
   userId: window.currentUser.uid,
   timestamp: window.serverTimestamp()
  });
 } catch(e) {
  console.error(e);
  toast('Lỗi gửi tin nhắn: ' + e.message, 'error');
 }
};

function inviteMemberModal() {
 document.getElementById('inviteEmail').value = '';
 openModal('mProjInvite');
}
async function sendInvite() {
 const email = document.getElementById('inviteEmail').value.trim();
 if(!email) { toast('Vui lòng nhập email!','error'); return; }
 
 if (window.DEMO_MODE) {
  toast(`Đã gửi lời mời tới ${email}! (Tính năng Demo)`, 'info');
  closeModal('mProjInvite');
  return;
 }

 const p = (window.DB.projects || []).find(x => x.id === currentProjId);
 if(p) {
  try {
   await window.sendProjectInvite(p.id, email);
   toast(`Đã thêm ${email} vào dự án!`, 'success');
  } catch(e) {
   toast('Lỗi khi mời thành viên: ' + e.message, 'error');
  }
 }
 closeModal('mProjInvite');
}

function openProjTaskModal(id='') {
 document.getElementById('ptaskId').value = id;
 document.getElementById('ptaskProjId').value = currentProjId;
 const list = window.DB.proj_tasks || [];
 const t = list.find(x => x.id === id);
 document.getElementById('mPTaskTitle').textContent = id ? 'Cập nhật nhiệm vụ' : 'Thêm nhiệm vụ mới';
 document.getElementById('btnDelPTask').style.display = id ? 'inline-block' : 'none';
 
 if(t) {
  document.getElementById('ptaskName').value = t.text || '';
  document.getElementById('ptaskStatus').value = t.status || 'Khởi tạo';
  document.getElementById('ptaskPri').value = t.priority || 'mid';
  document.getElementById('ptaskDesc').innerHTML = t.desc || '';
  document.getElementById('ptaskStart').value = t.start || '';
  document.getElementById('ptaskDue').value = t.due || '';
  
  // Comments
  document.getElementById('ptaskCommentsArea').style.display = 'block';
  renderPTaskComments(t.comments || []);
 } else {
  document.getElementById('ptaskName').value = '';
  document.getElementById('ptaskStatus').value = 'Khởi tạo';
  document.getElementById('ptaskPri').value = 'mid';
  document.getElementById('ptaskDesc').innerHTML = '';
 document.getElementById('ptaskStart').value = '';
 document.getElementById('ptaskDue').value = '';
  document.getElementById('ptaskCommentsArea').style.display = 'none';
 }
 openModal('mProjTask');
}

function renderPTaskComments(comments) {
 const el = document.getElementById('ptaskCommentsList');
 if(!comments.length) {
  el.innerHTML = '<i>Chưa có bình luận nào.</i>';
  return;
 }
 el.innerHTML = comments.map(c => `<div style="margin-bottom:8px;"><b>${window.LifeOSData.escapeHtml(c.user)}:</b> ${window.LifeOSData.escapeHtml(c.text)} <span style="font-size:10px;opacity:0.6">(${window.LifeOSData.escapeHtml(c.date)})</span></div>`).join('');
 el.scrollTop = el.scrollHeight;
}

async function saveProjTask() {
 const text = document.getElementById('ptaskName').value.trim();
 if(!text) { toast('Vui lòng nhập tên!','error'); return; }
 const id = document.getElementById('ptaskId').value;
 const pId = document.getElementById('ptaskProjId').value || currentProjId;
 const status = document.getElementById('ptaskStatus').value;
 const pri = document.getElementById('ptaskPri').value;
 const desc = document.getElementById('ptaskDesc').innerHTML;
 const start = document.getElementById('ptaskStart').value;
 const due = document.getElementById('ptaskDue').value;
 
 const list = [...(window.DB.proj_tasks || [])];
 let t;
 if(id) {
  const idx = list.findIndex(x => x.id === id);
  if(idx >= 0) t = {...list[idx], text, status, priority: pri, desc, start, due};
  else t = {id: uid(), projId: pId, text, status, priority: pri, desc, start, due, comments: []};
  if(idx >= 0) list[idx] = t; else list.push(t);
 } else {
  t = {id: uid(), projId: pId, text, status, priority: pri, desc, start, due, comments: []};
  list.push(t);
 }

 const p = (window.DB.projects || []).find(x => x.id === pId);
 const uidUser = window.currentUser?.uid || 'user';
 t.memberUids = p?.memberUids || [uidUser];

 window.DB.proj_tasks = list;
 await persist('proj_tasks', list);
 closeModal('mProjTask');
 toast('Đã lưu nhiệm vụ!','success');
 renderKanbanBoard();

 if (!window.DEMO_MODE && window.db && window.setDoc && window.doc) {
  const btn = document.getElementById('btnSavePTask');
  if(btn) btn.disabled = true;
  try {
   await window.setDoc(window.doc(window.db, 'shared_tasks', t.id), t);
  } catch(e) {
   console.warn('[LifeOS] Firestore shared_tasks sync warning:', e.message);
  } finally {
   if(btn) btn.disabled = false;
  }
 }
}

async function deleteProjTask() {
 if(!confirm('Bạn có chắc chắn muốn xóa nhiệm vụ này?')) return;
 const id = document.getElementById('ptaskId').value;
 if(!id) return;

 const list = (window.DB.proj_tasks || []).filter(x => x.id !== id);
 window.DB.proj_tasks = list;
 await persist('proj_tasks', list);
 closeModal('mProjTask');
 toast('Đã xóa nhiệm vụ!','info');
 renderKanbanBoard();

 if (!window.DEMO_MODE && window.db && window.deleteDoc && window.doc) {
  try {
   await window.deleteDoc(window.doc(window.db, 'shared_tasks', id));
  } catch(e) {
   console.warn('[LifeOS] Firestore shared_tasks delete warning:', e.message);
  }
 }
}

async function addProjTaskComment() {
 const inp = document.getElementById('ptaskCommentInput');
 const text = inp.value.trim();
 if(!text) return;
 const id = document.getElementById('ptaskId').value;
 if(!id) return;
 
 const list = [...(window.DB.proj_tasks || [])];
 const idx = list.findIndex(x => x.id === id);
 if(idx < 0) return;
 
 const t = {...list[idx]};
 const cArr = t.comments || [];
 const uName = window.currentUser?.displayName || window.currentUser?.email?.split('@')[0] || 'Bạn';
 cArr.push({user: uName, text, date: new Date().toLocaleTimeString('vi-VN') + ' ' + new Date().toLocaleDateString('vi-VN')});
 t.comments = cArr;
 
 if (window.DEMO_MODE) {
  list[idx] = t;
  window.DB.proj_tasks = list;
  await persist('proj_tasks', list);
  inp.value = '';
  renderPTaskComments(cArr);
  return;
 }

 try {
  await window.setDoc(window.doc(window.db, 'shared_tasks', t.id), t);
  inp.value = '';
  renderPTaskComments(cArr);
 } catch(e) {
  toast('Lỗi gửi bình luận: ' + e.message, 'error');
 }
}

/* ────────────────────────────────────────────────────────
  FINANCE
──────────────────────────────────────────────────────── */
async function addIncome(){
 const amt=parseFloat(document.getElementById('incAmt').value);
 if(!amt||amt<=0){toast('Nhập số tiền hợp lệ!','error');return;}
 const list=[...(window.DB.income||[])];
 list.unshift({id:uid(),src:document.getElementById('incSrc').value,amt,date:document.getElementById('incDate').value||today(),note:document.getElementById('incNote').value});
 await persist('income',list);
 document.getElementById('incAmt').value=''; document.getElementById('incNote').value='';
 toast('Đã thêm thu nhập!','success');
}
async function addExpense(){
 const amt=parseFloat(document.getElementById('expAmt').value);
 if(!amt||amt<=0){toast('Nhập số tiền hợp lệ!','error');return;}
 const list=[...(window.DB.expense||[])];
 list.unshift({id:uid(),cat:document.getElementById('expCat').value,amt,date:document.getElementById('expDate').value||today(),pay:document.getElementById('expPay').value,note:document.getElementById('expNote').value});
 await persist('expense',list);
 document.getElementById('expAmt').value=''; document.getElementById('expNote').value='';
 toast('Đã thêm chi tiêu!','success');
}
async function delIncome(id){await persist('income',(window.DB.income||[]).filter(x=>x.id!==id));}
async function delExpense(id){await persist('expense',(window.DB.expense||[]).filter(x=>x.id!==id));}


function getFinMonth() {
  const m = document.getElementById('finMonth');
  if (m && !m.value) m.value = today().substring(0, 7);
  return m ? m.value : today().substring(0, 7);
}

function computeFin(){
 const fm = getFinMonth();
 const totalInc=(window.DB.income||[]).filter(x => (x.date||'').startsWith(fm)).reduce((a,x)=>a+x.amt,0);
 const totalExp=(window.DB.expense||[]).filter(x => (x.date||'').startsWith(fm)).reduce((a,x)=>a+x.amt,0);
 const balance=totalInc-totalExp;
 const savRate=totalInc>0?((balance/totalInc)*100).toFixed(1):0;
 return{totalInc,totalExp,balance,savRate};
}
function renderFinKpi(){
 const k=computeFin();
 const fm = getFinMonth();
 const incCount = (window.DB.income||[]).filter(x => (x.date||'').startsWith(fm)).length;
 const expCount = (window.DB.expense||[]).filter(x => (x.date||'').startsWith(fm)).length;
 const expMax = expCount ? Math.max(...(window.DB.expense||[]).filter(x => (x.date||'').startsWith(fm)).map(e=>e.amt)) : 0;
 document.getElementById('finKpi').innerHTML=[
  {l:'Tổng thu nhập',v:fmt(k.totalInc)+' ₫',c:'cg',i:'',s:incCount+' giao dịch'},
  {l:'Tổng chi tiêu',v:fmt(k.totalExp)+' ₫',c:'cr',i:'<i data-lucide="heart" style="width:20px;height:20px"></i>',s:expCount+' giao dịch'},
  {l:'Số dư',v:fmt(Math.abs(k.balance))+(k.balance<0?' âm':'')+' ₫',c:k.balance>=0?'cc':'cr',i:'',s:k.savRate+'% tiết kiệm'},
  {l:'Tỷ lệ tiết kiệm',v:k.savRate+'%',c:'cp',i:'<i data-lucide="gem" style="width:20px;height:20px"></i>',s:'Mục tiêu 30%'},
  {l:'Chi tiêu cao nhất',v:expCount?fmt(expMax)+' ₫':'—',c:'ca',i:'',s:''},
  {l:'Tổng giao dịch',v:(incCount+expCount).toString(),c:'ck',i:'<i data-lucide="refresh-cw" style="width:20px;height:20px"></i>',s:''}
 ].map(k=>`<div class="kpi ${k.c}"><div class="kpi-ico">${k.i}</div><div class="kpi-lbl">${k.l}</div><div class="kpi-val">${k.v}</div>${k.s?`<div class="kpi-sub">${k.s}</div>`:''}</div>`).join('');
 if(window.lucide) window.lucide.createIcons();
}
function renderFinTables(){
 const fm = getFinMonth();
 const it=document.getElementById('incTb'); const et=document.getElementById('expTb');
 if(!it||!et)return;
 
 const incomes = (window.DB.income||[]).filter(x => (x.date||'').startsWith(fm)).sort((a,b)=>b.date.localeCompare(a.date));
 const expenses = (window.DB.expense||[]).filter(x => (x.date||'').startsWith(fm)).sort((a,b)=>b.date.localeCompare(a.date));
 
 it.innerHTML=incomes.slice(0,30).map(x=>`<tr><td style="font-weight:600">${x.src}</td><td class="money mg">+${fmt(x.amt)} ₫</td><td style="color:var(--text3);font-size:11.5px">${fmtDate(x.date)}</td><td style="color:var(--text3);font-size:11.5px">${x.note||'—'}</td><td><button class="btn btn-sm btn-r" style="padding:2px 6px" onclick="delIncome('${x.id}')"><i data-lucide="trash-2" style="width:14px;height:14px;"></i></button></td></tr>`).join('')||'<tr><td colspan="5"><div class="empty">Chưa có dữ liệu</div></td></tr>';
 et.innerHTML=expenses.slice(0,30).map(x=>`<tr><td style="font-weight:600">${x.cat}</td><td class="money mr">−${fmt(x.amt)} ₫</td><td style="color:var(--text3);font-size:11.5px">${fmtDate(x.date)}</td><td style="color:var(--text3);font-size:11.5px">${x.pay}</td><td><button class="btn btn-sm btn-r" style="padding:2px 6px" onclick="delExpense('${x.id}')"><i data-lucide="trash-2" style="width:14px;height:14px;"></i></button></td></tr>`).join('')||'<tr><td colspan="5"><div class="empty">Chưa có dữ liệu</div></td></tr>';
 
 // Default date inputs to the selected month
 const tm = today().substring(0,7);
 const defaultDate = (fm === tm) ? today() : fm + '-01';
 document.getElementById('incDate').value = defaultDate;
 document.getElementById('expDate').value = defaultDate;
 
 if(window.lucide) window.lucide.createIcons();
}
/* ────────────────────────────────────────────────────────
  CALENDAR
──────────────────────────────────────────────────────── */
let calY=new Date().getFullYear(), calM=new Date().getMonth();
function calNav(d){calM+=d;if(calM>11){calM=0;calY++;}if(calM<0){calM=11;calY--;}renderCal();}
function calToday(){calY=new Date().getFullYear();calM=new Date().getMonth();renderCal();}

function renderCal(){
 const t=today();
 const MN=['Tháng 1','Tháng 2','Tháng 3','Tháng 4','Tháng 5','Tháng 6','Tháng 7','Tháng 8','Tháng 9','Tháng 10','Tháng 11','Tháng 12'];
 document.getElementById('calLabel').textContent=`${MN[calM]} ${calY}`;
 const DN=['T.2','T.3','T.4','T.5','T.6','T.7','CN'];
 document.getElementById('calHdr').innerHTML=DN.map(d=>`<div class="bcd-name">${d}</div>`).join('');
 const first=new Date(calY,calM,1), last=new Date(calY,calM+1,0);
 const startDow=(first.getDay()+6)%7;
 let cells='';
 for(let i=0;i<startDow;i++){const dd=new Date(calY,calM,1-startDow+i);cells+=`<div class="bcd"><div class="bcd-num om">${dd.getDate()}</div></div>`;}
 for(let d=1;d<=last.getDate();d++){
  const ds=`${calY}-${String(calM+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
  const evs=(window.DB.events||[]).filter(e=>e.dateStart<=ds&&(e.dateEnd||e.dateStart)>=ds);
  const pills=evs.slice(0,3).map(e=>`<span class="ev-pill" style="background:${EC[e.type]||'#7c4dff'}22;color:${EC[e.type]||'#7c4dff'}" onclick="event.stopPropagation();editEv('${e.id}')">${e.timeStart?e.timeStart+' ':''}${e.title}</span>`).join('');
  const more=evs.length>3?`<span class="ev-pill" style="background:var(--glass);color:var(--text3)">+${evs.length-3}</span>`:'';
  
  // Responsive dots on mobile (<640px)
  const dots=evs.slice(0,3).map(e=>`<span class="cal-dot" style="background:${EC[e.type]||'#7c4dff'}"></span>`).join('');
  const dotHtml=evs.length>0?`<div class="cal-dots">${dots}${evs.length>3?'<span style="font-size:8px;color:var(--text-low);line-height:1;">+</span>':''}</div>`:'';
  
  cells+=`<div class="bcd" onclick="openEv('${ds}')"><div class="bcd-num${ds===t?' today':''}">${d}</div>${pills}${more}${dotHtml}</div>`;
 }
 const total=startDow+last.getDate(), rem=7-(total%7===0?7:total%7);
 for(let i=1;i<=rem&&rem<7;i++)cells+=`<div class="bcd"><div class="bcd-num om">${i}</div></div>`;
 document.getElementById('calGrid').innerHTML=cells;
}

/* ────────────────────────────────────────────────────────
  MINI CALENDAR
──────────────────────────────────────────────────────── */
let mcY=new Date().getFullYear(), mcM=new Date().getMonth();
function renderMiniCal(){
 const t=today();
 const MN=['Th.1','Th.2','Th.3','Th.4','Th.5','Th.6','Th.7','Th.8','Th.9','Th.10','Th.11','Th.12'];
 const DN=['T2','T3','T4','T5','T6','T7','CN'];
 const first=new Date(mcY,mcM,1), last=new Date(mcY,mcM+1,0);
 const startDow=(first.getDay()+6)%7;
 let html=`<div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
  <button class="btn btn-sm" onclick="mcNav(-1)" style="padding:3px 8px">‹</button>
  <span style="font-size:12px;font-weight:700">${MN[mcM]} ${mcY}</span>
  <button class="btn btn-sm" onclick="mcNav(1)" style="padding:3px 8px">›</button>
 </div>
 <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:2px;text-align:center">
 ${DN.map(d=>`<div style="font-size:9px;font-weight:700;color:var(--text3);padding:3px">${d}</div>`).join('')}`;
 for(let i=0;i<startDow;i++)html+=`<div></div>`;
 for(let d=1;d<=last.getDate();d++){
  const ds=`${mcY}-${String(mcM+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
  const hasEv=(window.DB.events||[]).some(e=>e.dateStart===ds);
  const isToday=ds===t;
  html+=`<div onclick="openEv('${ds}')" style="padding:5px 2px;border-radius:7px;font-size:11.5px;cursor:pointer;font-weight:500;transition:background .15s;position:relative;${isToday?'background:linear-gradient(135deg,var(--accent),#5533cc);color:#fff':'color:var(--text2)'}" onmouseover="if('${ds}'!=='${t}')this.style.background='var(--glass)'" onmouseout="if('${ds}'!=='${t}')this.style.background='transparent'">${d}${hasEv&&!isToday?`<span style="position:absolute;bottom:1px;left:50%;transform:translateX(-50%);width:3px;height:3px;border-radius:50%;background:var(--cyan)"></span>`:''}</div>`;
 }
 html+='</div>';
 const el=document.getElementById('miniCal'); if(el)el.innerHTML=html;
}
function mcNav(d){mcM+=d;if(mcM>11){mcM=0;mcY++;}if(mcM<0){mcM=11;mcY--;}renderMiniCal();}

/* ────────────────────────────────────────────────────────
  WEEK VIEW
──────────────────────────────────────────────────────── */
let wkOffset=0;
const HOURS=['06','07','08','09','10','11','12','13','14','15','16','17','18','19','20','21','22'];
const WDAYS=['T.2','T.3','T.4','T.5','T.6','T.7','CN'];

function getWeekDates(off){
 const now=new Date(), dow=(now.getDay()+6)%7;
 const mon=new Date(now); mon.setDate(now.getDate()-dow+off*7);
 return Array.from({length:7},(_,i)=>{const d=new Date(mon);d.setDate(mon.getDate()+i);return d;});
}
function wkNav(d){wkOffset+=d;renderWeek();}
function wkToday(){wkOffset=0;renderWeek();}

function renderWeek(){
 const dates=getWeekDates(wkOffset), t=today();
 const lbl=document.getElementById('wkLabel');
 if(lbl)lbl.textContent=`${dates[0].toLocaleDateString('vi-VN',{day:'2-digit',month:'2-digit'})} – ${dates[6].toLocaleDateString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric'})}`;
 const grid=document.getElementById('wkGrid'); if(!grid)return;
 let html='<div class="wk-cell wk-hdr"></div>';
 dates.forEach((d,i)=>{
  const ds=toLocalDateStr(d);
  html+=`<div class="wk-cell wk-hdr"><div class="wk-day-name">${WDAYS[i]}</div><div class="wk-day-num${ds===t?' today':''}">${d.getDate()}</div></div>`;
 });
 HOURS.forEach(h=>{
  html+=`<div class="wk-cell wk-time">${h}:00</div>`;
  dates.forEach(d=>{
   const ds=toLocalDateStr(d);
   const evs=(window.DB.events||[]).filter(e=>e.dateStart<=ds&&(e.dateEnd||e.dateStart)>=ds&&e.timeStart&&e.timeStart.split(':')[0]===h);
   html+=`<div class="wk-slot" onclick="openEv('${ds}','${h}:00')">${evs.map(e=>{
    const [sh, sm] = e.timeStart.split(':').map(Number);
    let [eh, em] = e.timeEnd ? e.timeEnd.split(':').map(Number) : [sh + 1, sm];
    let dur = (eh - sh) * 60 + (em - sm);
    if(dur <= 0) dur = 60;
    const topPx = (sm / 60) * 49;
    const heightPx = (dur / 60) * 49 - 1;
    return `<div class="wk-ev" style="top:${topPx}px; height:${heightPx}px; background:${EC[e.type]||'#7c4dff'}e6;color:#fff;border-left:3px solid ${EC[e.type]||'#7c4dff'}" onclick="event.stopPropagation();editEv('${e.id}')"><div style="font-weight:700;margin-bottom:2px">${e.title}</div>${e.timeEnd?`<div style="font-size:8.5px;opacity:0.9">${e.timeStart} - ${e.timeEnd}</div>`:''}</div>`;
   }).join('')}</div>`;
  });
 });
 grid.innerHTML=html;
}

/* ────────────────────────────────────────────────────────
  OVERVIEW
──────────────────────────────────────────────────────── */
function renderOverview(){
 const range = parseInt(document.getElementById('ovFinanceRange')?.value || 14);
 const k=computeFin(), t=today();
 const allTodos = window.DB.todos||[];
 const todayTodos = allTodos.filter(x=>!x.date||x.date===t);
 const done = todayTodos.filter(x=>x.done).length;
 
 let pomStats = {mins: 0};
 try { pomStats = JSON.parse(localStorage.getItem('lifeos_pom_'+t)) || {mins:0}; } catch{}

 const mocks = window.DB.mocktests||[];
 const avgEng = mocks.length ? Math.round(mocks.reduce((sum,m)=>sum+m.score,0)/mocks.length) : 0;

 // Banner
 const bn=document.getElementById('bannerTxt');
 if(bn){
  const pending=todayTodos.filter(x=>!x.done).length;
  let notifBtn = '';
  if ('Notification' in window && Notification.permission === 'default') {
   notifBtn = ` <button class="btn btn-sm btn-p" style="margin-left:8px;padding:3px 8px;font-size:11px" onclick="requestNotif()"><i data-lucide="bell" style="width:14px;height:14px;margin-right:4px"></i> Bật thông báo</button>`;
  }
  bn.innerHTML=`Hôm nay là <b>${new Date().toLocaleDateString('vi-VN',{weekday:'long',day:'2-digit',month:'2-digit',year:'numeric'})}</b>. Còn <b style="color:var(--cyan)">${pending} việc cần làm</b> hôm nay.${notifBtn}`;
 }

 // KPIs
 const ovKpi=document.getElementById('ovKpi');
 if(ovKpi){
  ovKpi.innerHTML=`
   <div class="kpi cc"><div class="kpi-ico" style="display:flex;align-items:center;justify-content:center"><i data-lucide="wallet"></i></div><div class="kpi-lbl">Tổng tài sản</div><div class="kpi-val">${fmt(k.balance)} ₫</div><div class="kpi-sub">${k.savRate}% Tiết kiệm</div><div class="sparkline"><canvas id="spFin"></canvas></div></div>
   <div class="kpi ca"><div class="kpi-ico"></div><div class="kpi-lbl">Năng suất Todo</div><div class="kpi-val">${done}/${todayTodos.length}</div><div class="kpi-sub">Hôm nay</div><div class="sparkline"><canvas id="spTodo"></canvas></div></div>
   <div class="kpi cg"><div class="kpi-ico">⏱️</div><div class="kpi-lbl">Thời gian Focus</div><div class="kpi-val">${pomStats.mins}p</div><div class="kpi-sub">Hôm nay</div><div class="sparkline"><canvas id="spPom"></canvas></div></div>
   <div class="kpi cp"><div class="kpi-ico" style="display:flex;align-items:center;justify-content:center"><i data-lucide="languages"></i></div><div class="kpi-lbl">Tiếng Anh</div><div class="kpi-val">${avgEng} đ</div><div class="kpi-sub">Trung bình Mock Test</div><div class="sparkline"><canvas id="spEng"></canvas></div></div>
  `;
  setTimeout(() => drawSparklines(), 50);
 }

 // Activity Heatmap (Last 28 days)
 const hm = document.getElementById('ovActivityHeatmap');
 if(hm) {
  let hmHtml = '';
  const now = new Date();
  const days = 28;
  for(let i=days-1; i>=0; i--) {
   const d = new Date(now); d.setDate(d.getDate()-i);
   const ds = toLocalDateStr(d);
   const dTodos = allTodos.filter(x=>x.done && x.date===ds).length;
   let lvl = 0;
   if(dTodos > 0) lvl = 1;
   if(dTodos > 2) lvl = 2;
   if(dTodos > 4) lvl = 3;
   if(dTodos > 7) lvl = 4;
   if(dTodos > 10) lvl = 5;
   hmHtml += `<div class="heatmap-cell" data-level="${lvl}" title="${ds}: ${dTodos} tasks"></div>`;
  }
  hm.innerHTML = hmHtml;
 }

 // Mini cal
 renderMiniCal();

 // Upcoming events
 const upcoming=(window.DB.events||[]).filter(e=>e.dateStart>=t).sort((a,b)=>a.dateStart.localeCompare(b.dateStart)).slice(0,5);
 const ovEv=document.getElementById('ovEvents');
 if(ovEv)ovEv.innerHTML=upcoming.length?upcoming.map(e=>`
 <div style="display:flex;gap:10px;padding:9px 0;border-bottom:1px solid rgba(255,255,255,.04)">
  <div style="width:8px;height:8px;border-radius:50%;background:${EC[e.type]||'#7c4dff'};flex-shrink:0;margin-top:4px"></div>
  <div style="flex:1;min-width:0">
   <div style="font-size:12.5px;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${e.title}</div>
   <div style="font-size:10.5px;color:var(--text3)">${fmtDate(e.dateStart)}${e.timeStart?' • '+e.timeStart:''}</div>
  </div>
  <button class="btn btn-sm" style="padding:2px 7px;flex-shrink:0" onclick="editEv('${e.id}')"><i data-lucide="pencil" style="width:14px;height:14px;"></i></button>
 </div>`).join(''):`<div class="empty"><br>Không có sự kiện sắp tới</div>`;
 
 // Today todos
 const tb=document.getElementById('todayBadge'); if(tb)tb.textContent=`${done}/${todayTodos.length}`;
 const ot=document.getElementById('ovTodos');
 if(ot)ot.innerHTML=todayTodos.length?todayTodos.slice(0,5).map(todo=>`
 <div style="display:flex;align-items:center;gap:10px;padding:8px 0;border-bottom:1px solid rgba(255,255,255,.04)">
  <div onclick="toggleTodo('${todo.id}')" style="width:18px;height:18px;border-radius:5px;border:2px solid ${todo.done?'var(--green)':'var(--text3)'};background:${todo.done?'var(--green)':'transparent'};cursor:pointer;display:flex;align-items:center;justify-content:center;flex-shrink:0">
   ${todo.done?'<span style="color:#000;font-size:10px;font-weight:800">✓</span>':''}
  </div>
  <span style="flex:1;font-size:12.5px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;${todo.done?'text-decoration:line-through;color:var(--text3)':''}">${todo.text}</span>
  <span style="width:6px;height:6px;border-radius:50%;background:${PRI_C[todo.priority]};flex-shrink:0"></span>
 </div>`).join(''):`<div class="empty">Không có công việc nào cần xử lý.</div>`;
 
 // Active projects
 const op=document.getElementById('ovProjs');
 if(op){
  const projs=(window.DB.projects||[]).filter(p=>['Cần làm','Đang làm'].includes(p.status)).slice(0,5);
  if(!projs.length)op.innerHTML=`<tr><td colspan="4"><div class="empty">Chưa có dự án hoạt động</div></td></tr>`;
  else op.innerHTML=projs.map(p=>`<tr>
   <td style="font-weight:600">${p.name}${p.tags&&p.tags.length?`<span class="badge bp" style="margin-left:6px;font-size:10px">${p.tags[0]}</span>`:''}</td>
   <td><span class="badge ${SB[p.status]||'bk'}">${p.status}</span></td>
   <td><div class="prog" style="width:80px"><div class="prog-fill" style="width:${p.status==='Hoàn thành'?100:p.status==='Đang làm'?55:15}%;background:${p.status==='Hoàn thành'?'var(--green)':p.status==='Đang làm'?'var(--amber)':'var(--accent)'}"></div></div></td>
   <td style="font-size:11px;color:${p.due&&p.due<today()?'var(--red)':'var(--text3)'}">${p.due?fmtDate(p.due):'—'}</td>
  </tr>`).join('');
 }
 
 renderOverviewChart(range);
 updateBadges();
}

/* ────────────────────────────────────────────────────────
  BADGES
──────────────────────────────────────────────────────── */
function updateBadges(){
 const pending=(window.DB.todos||[]).filter(t=>!t.done).length;
 const doing=(window.DB.projects||[]).filter(p=>p.status==='Đang làm').length;
 ['badgeProj'].forEach(id=>{const e=document.getElementById(id);if(e)e.textContent=doing;});
 ['badgeTodo'].forEach(id=>{const e=document.getElementById(id);if(e)e.textContent=pending;});
 // Mobile badges
 const mb1=document.getElementById('mBadgeProj'); if(mb1){mb1.textContent=doing;mb1.style.display=doing>0?'flex':'none';}
 const mb2=document.getElementById('mBadgeTodo'); if(mb2){mb2.textContent=pending;mb2.style.display=pending>0?'flex':'none';}
}

/* ────────────────────────────────────────────────────────
  CHARTS
──────────────────────────────────────────────────────── */
const charts={};
function dc(id){if(charts[id]){charts[id].destroy();delete charts[id];}}
function getCO() {
 const isLight = document.documentElement.getAttribute('data-theme') === 'light';
 const t1 = isLight ? '#4a5568' : '#9090b0';
 const t2 = isLight ? '#718096' : '#50507a';
 const g = isLight ? 'rgba(0,0,0,0.06)' : 'rgba(255,255,255,.04)';
 const isMobile = window.innerWidth < 640;
 return {
  responsive: true,
  maintainAspectRatio: false,
  plugins: { legend: { labels: { color: t1, font: { size: 10 }, padding: 8 } } },
  scales: {
   x: {
     ticks: {
       color: t2,
       font: { size: 9.5 },
       maxTicksLimit: isMobile ? 5 : 12,
       maxRotation: 0,
       minRotation: 0
     },
     grid: { color: g }
   },
   y: { ticks: { color: t2, font: { size: 9.5 } }, grid: { color: g } }
  }
 };
}

function renderOverviewChart(range=14){
 dc('ovC'); const ctx=document.getElementById('ovChart'); if(!ctx)return;
 const days=Array.from({length:range},(_,i)=>dayOff(i-(range-1)));
 const labels=days.map(d=>{const dt=new Date(d+'T00:00:00');return `${dt.getDate()}/${dt.getMonth()+1}`;});
 const opt = getCO();
 opt.scales.x.ticks.font.size = 9;
 opt.scales.y.ticks.font.size = 9;
 opt.scales.y.ticks.callback = v=>v+'M';
 charts['ovC']=new Chart(ctx,{
  type:'bar',
  data:{labels,datasets:[
   {label:'Thu nhập (M₫)',data:days.map(d=>(window.DB.income||[]).filter(x=>x.date===d).reduce((a,x)=>a+x.amt,0)/1e6),backgroundColor:'rgba(0,230,118,.5)',borderColor:'rgba(0,230,118,.8)',borderRadius:3,borderWidth:1},
   {label:'Chi tiêu (M₫)',data:days.map(d=>(window.DB.expense||[]).filter(x=>x.date===d).reduce((a,x)=>a+x.amt,0)/1e6),backgroundColor:'rgba(255,82,82,.5)',borderColor:'rgba(255,82,82,.8)',borderRadius:3,borderWidth:1},
  ]},
  options: Object.assign({}, opt, { interaction: { mode: 'index', intersect: false } })
 });
}

function drawSparklines() {
 const days = 7;
 const dArr = Array.from({length:days},(_,i)=>dayOff(i-(days-1)));
 
 const ctxFin = document.getElementById('spFin');
 if(ctxFin) {
  dc('sp1');
  const bArr = dArr.map(d=>{
   const inc = (window.DB.income||[]).filter(x=>x.date===d).reduce((a,x)=>a+x.amt,0);
   const exp = (window.DB.expense||[]).filter(x=>x.date===d).reduce((a,x)=>a+x.amt,0);
   return inc - exp;
  });
  charts['sp1'] = new Chart(ctxFin, {
   type: 'line', data: { labels: dArr, datasets: [{ data: bArr, borderColor: '#00e676', borderWidth: 2, tension: 0.4, pointRadius: 0 }] },
   options: { responsive: true, maintainAspectRatio: false, plugins: {legend:{display:false},tooltip:{enabled:false}}, scales: {x:{display:false},y:{display:false}} }
  });
 }

 const ctxTodo = document.getElementById('spTodo');
 if(ctxTodo) {
  dc('sp2');
  const tArr = dArr.map(d=>(window.DB.todos||[]).filter(x=>x.date===d&&x.done).length);
  charts['sp2'] = new Chart(ctxTodo, {
   type: 'bar', data: { labels: dArr, datasets: [{ data: tArr, backgroundColor: '#7c4dff', borderRadius: 2 }] },
   options: { responsive: true, maintainAspectRatio: false, plugins: {legend:{display:false},tooltip:{enabled:false}}, scales: {x:{display:false},y:{display:false}} }
  });
 }

 const ctxPom = document.getElementById('spPom');
 if(ctxPom) {
  dc('sp3');
  const pArr = dArr.map(d=>{ try { return (JSON.parse(localStorage.getItem('lifeos_pom_'+d))||{mins:0}).mins; } catch{return 0;} });
  charts['sp3'] = new Chart(ctxPom, {
   type: 'line', data: { labels: dArr, datasets: [{ data: pArr, borderColor: '#00e5ff', backgroundColor:'rgba(0,229,255,0.1)', fill:true, borderWidth: 2, tension: 0.4, pointRadius: 0 }] },
   options: { responsive: true, maintainAspectRatio: false, plugins: {legend:{display:false},tooltip:{enabled:false}}, scales: {x:{display:false},y:{display:false}} }
  });
 }

 const ctxEng = document.getElementById('spEng');
 if(ctxEng) {
  dc('sp4');
  const mArr = dArr.map(d=>{
   const t = (window.DB.mocktests||[]).filter(x=>x.date===d);
   return t.length ? Math.round(t.reduce((s,m)=>s+m.score,0)/t.length) : 0;
  });
  charts['sp4'] = new Chart(ctxEng, {
   type: 'line', data: { labels: dArr, datasets: [{ data: mArr, borderColor: '#ffab40', borderWidth: 2, tension: 0.4, pointRadius: 0 }] },
   options: { responsive: true, maintainAspectRatio: false, plugins: {legend:{display:false},tooltip:{enabled:false}}, scales: {x:{display:false},y:{display:false}} }
  });
 }
}

function exportReport() {
 const days=parseInt(document.getElementById('statsRange')?.value||30);
 const cutoff=dayOff(-days);
 const tDate=today();
 
 const inc=(window.DB.income||[]).filter(x=>x.date>=cutoff);
 const exp=(window.DB.expense||[]).filter(x=>x.date>=cutoff);
 const totInc=inc.reduce((a,x)=>a+x.amt,0);
 const totExp=exp.reduce((a,x)=>a+x.amt,0);
 const bal=totInc-totExp;
 const savRate=totInc>0?Math.round((bal/totInc)*100):0;
 
 const cats={}; exp.forEach(e=>{cats[e.cat]=(cats[e.cat]||0)+e.amt;});
 const topCats=Object.entries(cats).sort((a,b)=>b[1]-a[1]).slice(0,10);

 const todos=(window.DB.todos||[]).filter(x=>!x.date||(x.date>=cutoff && x.date<=tDate));
 const doneTodos=todos.filter(x=>x.done).length;
 const recentTodos=todos.filter(x=>x.done).slice(0,10);
 
 const projs=window.DB.projects||[];
 const doneProjs=projs.filter(p=>p.status==='Hoàn thành').length;
 const doingProjs=projs.filter(p=>p.status==='Đang làm').length;
 
 const evs=(window.DB.events||[]).filter(e=>e.dateStart>=cutoff && e.dateStart<=tDate);

 const html = `
<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
<head>
 <meta charset='utf-8'>
 <title>Báo Cáo LifeOS</title>
 <link rel="stylesheet" href="css/style.css">
</head>
<body>
 <h1>BÁO CÁO TỔNG HỢP LIFEOS</h1>
 <div class='subtitle'>Thời gian: ${days} ngày qua (từ ${fmtDate(cutoff)} đến ${fmtDate(tDate)})</div>

 <h2>1. TỔNG QUAN TÀI CHÍNH</h2>
 <table>
  <tr>
   <th>Tổng thu nhập</th>
   <th>Tổng chi tiêu</th>
   <th>Số dư</th>
   <th>Tỉ lệ tiết kiệm</th>
  </tr>
  <tr>
   <td class='highlight'>${fmtFull(totInc)} ₫</td>
   <td class='highlight-red'>${fmtFull(totExp)} ₫</td>
   <td style='font-weight:bold'>${fmtFull(bal)} ₫</td>
   <td>${savRate}%</td>
  </tr>
 </table>

 <h3>Chi tiết các khoản chi lớn nhất</h3>
 <table>
  <tr><th width='70%'>Danh mục</th><th>Số tiền</th></tr>
  ${topCats.map(c=>`<tr><td>${c[0]}</td><td class='highlight-red'>${fmtFull(c[1])} ₫</td></tr>`).join('')}
 </table>

 <h2>2. HIỆU SUẤT DỰ ÁN</h2>
 <p><b>Tổng quan:</b> Đang hoạt động (${doingProjs}) | Đã hoàn thành (${doneProjs})</p>
 <table>
  <tr>
   <th width='25%'>Tên dự án</th>
   <th width='15%'>Trạng thái</th>
   <th width='15%'>Tiến độ</th>
   <th width='20%'>Ngân sách</th>
   <th width='25%'>Đánh giá</th>
  </tr>
  ${projs.length ? projs.map(p=>{
   let prog = p.progress !== undefined ? p.progress : (p.status==='Hoàn thành'?100:p.status==='Đang làm'?50:0);
   return `<tr>
    <td><b>${p.name}</b></td>
    <td>${p.status}</td>
    <td>${prog}%</td>
    <td>${p.budget ? fmtFull(p.budget)+' ₫' : '-'}</td>
    <td>${p.evaluation || '-'}</td>
   </tr>`
  }).join('') : `<tr><td colspan='5'>Không có dự án nào</td></tr>`}
 </table>

 <h2>3. CÔNG VIỆC (TODO) VÀ SỰ KIỆN</h2>
 <p><b>Công việc (Todo):</b> Đã hoàn thành <b>${doneTodos}</b> / ${todos.length} việc.</p>
 ${recentTodos.length ? `<ul>${recentTodos.map(t=>`<li>${t.text}</li>`).join('')}</ul>` : ''}
 
 <p><b>Sự kiện tham gia:</b> <b>${evs.length}</b> sự kiện.</p>
 ${evs.length ? `<ul>${evs.map(e=>`<li><b>${fmtDate(e.dateStart)}</b>: ${e.title}</li>`).join('')}</ul>` : ''}

 <br><br>
 <p style='text-align:right; font-style:italic; color:#888'>Tạo bởi phần mềm quản lý LifeOS.</p>
  
</body>

</html>
 `;

 const blob = new Blob(['\ufeff', html], { type: 'application/msword' });
 const url = URL.createObjectURL(blob);
 const a = document.createElement('a');
 a.href = url;
 a.download = `LifeOS_BaoCao_${days}ngay.doc`;
 document.body.appendChild(a);
 a.click();
 document.body.removeChild(a);
 URL.revokeObjectURL(url);
 toast('Đã tải báo cáo Word xuống!','success');
}


/* ────────────────────────────────────────────────────────
  FINANCIAL STATS (MULTI-PERIOD: DAY / WEEK / MONTH / YEAR / ALL)
──────────────────────────────────────────────────────── */
let currentStatsPeriod = 'month';
let currentStatsSub = 'thisMonth';

window.setStatsPeriod = function(period, el) {
 currentStatsPeriod = period;
 document.querySelectorAll('#statsPeriodTabs .seg-btn').forEach(b => b.classList.remove('active'));
 if (el) el.classList.add('active');

 const subEl = document.getElementById('statsSubRange');
 if (!subEl) return;

 const y = new Date().getFullYear();
 const m = new Date().getMonth() + 1;

 const optionsMap = {
  day: [
   { v: 'today', t: 'Hôm nay' },
   { v: 'yesterday', t: 'Hôm qua' },
   { v: 'last7days', t: '7 ngày qua' },
   { v: 'last14days', t: '14 ngày qua' },
   { v: 'last30days', t: '30 ngày qua' }
  ],
  week: [
   { v: 'thisWeek', t: 'Tuần này' },
   { v: 'lastWeek', t: 'Tuần trước' },
   { v: 'last4weeks', t: '4 tuần qua' },
   { v: 'last8weeks', t: '8 tuần qua' }
  ],
  month: [
   { v: 'thisMonth', t: 'Tháng này' },
   { v: 'lastMonth', t: 'Tháng trước' },
   { v: 'last3months', t: '3 tháng qua (Quý)' },
   { v: 'last6months', t: '6 tháng qua' },
   { v: 'last12months', t: '12 tháng qua' }
  ],
  year: [
   { v: 'thisYear', t: 'Năm nay (' + y + ')' },
   { v: 'lastYear', t: 'Năm trước (' + (y - 1) + ')' }
  ],
  all: [
   { v: 'all', t: 'Toàn bộ thời gian' }
  ]
 };

 const list = optionsMap[period] || optionsMap.month;
 subEl.innerHTML = list.map((opt, i) => `<option value="${opt.v}" ${i === 0 ? 'selected' : ''}>${opt.t}</option>`).join('');
 renderStats();
};

function getStatsDateInterval() {
 const period = currentStatsPeriod || 'month';
 const sub = document.getElementById('statsSubRange')?.value || 'thisMonth';
 const now = new Date();
 const localStr = d => toLocalDateStr(d);
 
 let start, end, label, step;
 
 if (period === 'day') {
  if (sub === 'today') {
   start = end = localStr(now);
   label = 'Hôm nay (' + fmtDate(start) + ')';
   step = 'day';
  } else if (sub === 'yesterday') {
   const y = new Date(now); y.setDate(y.getDate() - 1);
   start = end = localStr(y);
   label = 'Hôm qua (' + fmtDate(start) + ')';
   step = 'day';
  } else if (sub === 'last7days') {
   const d = new Date(now); d.setDate(d.getDate() - 6);
   start = localStr(d); end = localStr(now);
   label = '7 ngày qua (' + fmtDs(start) + ' – ' + fmtDs(end) + ')';
   step = 'day';
  } else if (sub === 'last14days') {
   const d = new Date(now); d.setDate(d.getDate() - 13);
   start = localStr(d); end = localStr(now);
   label = '14 ngày qua (' + fmtDs(start) + ' – ' + fmtDs(end) + ')';
   step = 'day';
  } else {
   const d = new Date(now); d.setDate(d.getDate() - 29);
   start = localStr(d); end = localStr(now);
   label = '30 ngày qua (' + fmtDs(start) + ' – ' + fmtDs(end) + ')';
   step = 'day';
  }
 } else if (period === 'week') {
  const dayOfWeek = now.getDay() === 0 ? 7 : now.getDay();
  const monday = new Date(now);
  monday.setDate(now.getDate() - (dayOfWeek - 1));
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);

  if (sub === 'thisWeek') {
   start = localStr(monday); end = localStr(sunday);
   label = 'Tuần này (' + fmtDs(start) + ' – ' + fmtDs(end) + ')';
   step = 'day';
  } else if (sub === 'lastWeek') {
   const lastMon = new Date(monday); lastMon.setDate(lastMon.getDate() - 7);
   const lastSun = new Date(lastMon); lastSun.setDate(lastMon.getDate() + 6);
   start = localStr(lastMon); end = localStr(lastSun);
   label = 'Tuần trước (' + fmtDs(start) + ' – ' + fmtDs(end) + ')';
   step = 'day';
  } else if (sub === 'last8weeks') {
   const d = new Date(now); d.setDate(d.getDate() - 55);
   start = localStr(d); end = localStr(now);
   label = '8 tuần qua (' + fmtDs(start) + ' – ' + fmtDs(end) + ')';
   step = 'week';
  } else {
   const d = new Date(now); d.setDate(d.getDate() - 27);
   start = localStr(d); end = localStr(now);
   label = '4 tuần qua (' + fmtDs(start) + ' – ' + fmtDs(end) + ')';
   step = 'week';
  }
 } else if (period === 'month') {
  const y = now.getFullYear(), m = now.getMonth();
  if (sub === 'thisMonth') {
   start = localStr(new Date(y, m, 1));
   end = localStr(new Date(y, m + 1, 0));
   label = 'Tháng ' + (m + 1) + '/' + y;
   step = 'day';
  } else if (sub === 'lastMonth') {
   start = localStr(new Date(y, m - 1, 1));
   end = localStr(new Date(y, m, 0));
   const lastMDate = new Date(y, m - 1, 1);
   label = 'Tháng ' + (lastMDate.getMonth() + 1) + '/' + lastMDate.getFullYear();
   step = 'day';
  } else if (sub === 'last3months') {
   start = localStr(new Date(y, m - 2, 1));
   end = localStr(new Date(y, m + 1, 0));
   label = '3 tháng qua (Quý)';
   step = 'month';
  } else if (sub === 'last6months') {
   start = localStr(new Date(y, m - 5, 1));
   end = localStr(new Date(y, m + 1, 0));
   label = '6 tháng qua';
   step = 'month';
  } else {
   start = localStr(new Date(y - 1, m + 1, 1));
   end = localStr(new Date(y, m + 1, 0));
   label = '12 tháng qua';
   step = 'month';
  }
 } else if (period === 'year') {
  const y = now.getFullYear();
  if (sub === 'lastYear') {
   start = (y - 1) + '-01-01'; end = (y - 1) + '-12-31';
   label = 'Năm ' + (y - 1);
   step = 'month';
  } else {
   start = y + '-01-01'; end = y + '-12-31';
   label = 'Năm ' + y;
   step = 'month';
  }
 } else {
  start = '1970-01-01'; end = '2099-12-31';
  label = 'Toàn bộ thời gian';
  step = 'month';
 }
 
 return { start, end, label, step, period };
}

function renderStats(){
 const interval = getStatsDateInterval();
 const { start, end, label, step } = interval;

 // Update labels in UI
 const rangeLbl = document.getElementById('statsDateRangeLabel');
 if (rangeLbl) rangeLbl.textContent = label;
 const badgeEl = document.getElementById('stLineRangeBadge');
 if (badgeEl) badgeEl.textContent = `${fmtDate(start)} – ${fmtDate(end)}`;

 dc('stL'); dc('stD'); dc('stC');
 const isLight = document.documentElement.getAttribute('data-theme') === 'light';
 const t1 = isLight ? '#4a5568' : '#9090b0';
 const t2 = isLight ? '#718096' : '#50507a';
 
 // Filter datasets strictly by selected interval
 const filteredInc = (window.DB.income || []).filter(x => x.date >= start && x.date <= end);
 const filteredExp = (window.DB.expense || []).filter(x => x.date >= start && x.date <= end);

 const totalInc = filteredInc.reduce((a, b) => a + (b.amt || 0), 0);
 const totalExp = filteredExp.reduce((a, b) => a + (b.amt || 0), 0);
 const balance = totalInc - totalExp;
 const savRate = totalInc > 0 ? ((balance / totalInc) * 100).toFixed(1) : 0;

 // 1. Line Chart: Trend of Income & Expense
 const ctxL = document.getElementById('stLineC');
 if (ctxL) {
  let labels = [], incD = [], expD = [];

  if (step === 'day') {
   // Generate day-by-day buckets
   const sDate = new Date(start + 'T00:00:00');
   const eDate = new Date(end + 'T00:00:00');
   const diffDays = Math.max(1, Math.round((eDate - sDate) / (1000 * 60 * 60 * 24)) + 1);
   
   // If too many days, sample or step
   const pts = Math.min(diffDays, 14);
   const dayStep = Math.max(1, Math.ceil(diffDays / pts));
   
   for (let i = 0; i < diffDays; i += dayStep) {
    const cur = new Date(sDate);
    cur.setDate(cur.getDate() + i);
    const dStr = toLocalDateStr(cur);
    labels.push(`${cur.getDate()}/${cur.getMonth() + 1}`);
    
    // Sum for this sub-bucket
    let ia = 0, ea = 0;
    for (let s = 0; s < dayStep; s++) {
     const subD = new Date(cur); subD.setDate(subD.getDate() + s);
     const subStr = toLocalDateStr(subD);
     ia += filteredInc.filter(x => x.date === subStr).reduce((a, x) => a + x.amt, 0);
     ea += filteredExp.filter(x => x.date === subStr).reduce((a, x) => a + x.amt, 0);
    }
    incD.push(+(ia / 1e6).toFixed(2));
    expD.push(+(ea / 1e6).toFixed(2));
   }
  } else if (step === 'week') {
   // 4 or 8 weeks
   const nWeeks = interval.period === 'week' && document.getElementById('statsSubRange')?.value === 'last8weeks' ? 8 : 4;
   for (let w = nWeeks - 1; w >= 0; w--) {
    labels.push('Tuần -' + w);
    const wStart = dayOff(-w * 7 - 6);
    const wEnd = dayOff(-w * 7);
    const ia = filteredInc.filter(x => x.date >= wStart && x.date <= wEnd).reduce((a, x) => a + x.amt, 0);
    const ea = filteredExp.filter(x => x.date >= wStart && x.date <= wEnd).reduce((a, x) => a + x.amt, 0);
    incD.push(+(ia / 1e6).toFixed(2));
    expD.push(+(ea / 1e6).toFixed(2));
   }
  } else {
   // Monthly buckets
   const mMap = {};
   filteredInc.forEach(x => { const m = (x.date || '').slice(0, 7); mMap[m] = true; });
   filteredExp.forEach(x => { const m = (x.date || '').slice(0, 7); mMap[m] = true; });
   const mKeys = Object.keys(mMap).sort();
   
   if (mKeys.length === 0) {
    labels = ['Kỳ này']; incD = [0]; expD = [0];
   } else {
    labels = mKeys.map(k => {
     const parts = k.split('-');
     return 'T' + parseInt(parts[1]) + (parts[0] !== new Date().getFullYear().toString() ? '/' + parts[0].slice(2) : '');
    });
    incD = mKeys.map(k => +(filteredInc.filter(x => (x.date || '').startsWith(k)).reduce((a, x) => a + x.amt, 0) / 1e6).toFixed(2));
    expD = mKeys.map(k => +(filteredExp.filter(x => (x.date || '').startsWith(k)).reduce((a, x) => a + x.amt, 0) / 1e6).toFixed(2));
   }
  }

  const optL = getCO();
  optL.scales.y.ticks.callback = v => v + 'M';
  charts['stL'] = new Chart(ctxL, {
   type: 'line',
   data: {
    labels,
    datasets: [
     { label: 'Thu nhập', data: incD, borderColor: '#00e676', backgroundColor: 'rgba(0,230,118,.1)', tension: .4, fill: true, pointRadius: 3 },
     { label: 'Chi tiêu', data: expD, borderColor: '#ff5252', backgroundColor: 'rgba(255,82,82,.1)', tension: .4, fill: true, pointRadius: 3 }
    ]
   },
   options: optL
  });
 }

 // 2. Donut Chart: Expense Breakdown (Strictly Filtered)
 const ctxD = document.getElementById('stDonutC');
 const totalBadge = document.getElementById('stDonutTotalBadge');
 if (totalBadge) totalBadge.textContent = 'Tổng: ' + fmt(totalExp) + ' ₫';

 if (ctxD) {
  const cm = {};
  filteredExp.forEach(e => {
   const c = (e.cat || 'Khác').replace(/^\S+\s/, '');
   cm[c] = (cm[c] || 0) + (e.amt || 0);
  });
  const cats = Object.keys(cm);
  const vals = cats.map(c => cm[c]);
  const cols = ['#7c4dff', '#00e5ff', '#ff6b9d', '#00e676', '#ffab40', '#ff5252', '#ce93d8', '#80cbc4', '#ffd740'];
  const bdColor = isLight ? '#ffffff' : '#0e0e1c';

  if (vals.length === 0) {
   charts['stD'] = new Chart(ctxD, {
    type: 'doughnut',
    data: { labels: ['Chưa có chi tiêu'], datasets: [{ data: [1], backgroundColor: ['var(--surface2)'], borderColor: bdColor, borderWidth: 1 }] },
    options: { responsive: true, maintainAspectRatio: false, cutout: '70%', plugins: { legend: { display: false } } }
   });
  } else {
   charts['stD'] = new Chart(ctxD, {
    type: 'doughnut',
    data: { labels: cats, datasets: [{ data: vals, backgroundColor: cols, borderColor: bdColor, borderWidth: 2, hoverOffset: 8 }] },
    options: {
     responsive: true,
     maintainAspectRatio: false,
     cutout: '65%',
     plugins: { legend: { position: 'right', labels: { color: t1, font: { size: 11 }, padding: 8, boxWidth: 12 } } }
    }
   });
  }
 }

 // 3. Bar Chart: Categories by Spend (Strictly Filtered)
 const ctxC = document.getElementById('stCatC');
 if (ctxC) {
  const cm = {};
  filteredExp.forEach(e => { cm[e.cat] = (cm[e.cat] || 0) + (e.amt || 0); });
  const sorted = Object.entries(cm).sort((a, b) => b[1] - a[1]).slice(0, 8);
  const optC = getCO();
  optC.indexAxis = 'y';
  optC.plugins.legend.display = false;
  optC.scales.x.ticks.callback = v => v + 'M';
  optC.scales.y.grid.display = false;

  charts['stC'] = new Chart(ctxC, {
   type: 'bar',
   data: {
    labels: sorted.length ? sorted.map(x => x[0]) : ['Không có chi tiêu'],
    datasets: [{
     data: sorted.length ? sorted.map(x => +(x[1] / 1e6).toFixed(2)) : [0],
     backgroundColor: ['#7c4dff', '#00e5ff', '#ff6b9d', '#00e676', '#ffab40', '#ff5252', '#ce93d8', '#80cbc4'].map(c => c + 'cc'),
     borderRadius: 4
    }]
   },
   options: optC
  });
 }

 // 4. Financial Summary Card (Strictly Filtered)
 const fs = document.getElementById('finSum');
 if (fs) {
  const sDate = new Date(start + 'T00:00:00');
  const eDate = new Date(end + 'T00:00:00');
  const nDays = Math.max(1, Math.round((eDate - sDate) / (1000 * 60 * 60 * 24)) + 1);
  const avgExpPerDay = Math.round(totalExp / nDays);
  const maxExp = filteredExp.length ? Math.max(...filteredExp.map(x => x.amt || 0)) : 0;

  fs.innerHTML = `<div style="display:flex;flex-direction:column;gap:12px">
  ${[
   { l: 'Tổng thu nhập kỳ này', v: fmtFull(totalInc) + ' ₫', c: 'var(--green)' },
   { l: 'Tổng chi tiêu kỳ này', v: fmtFull(totalExp) + ' ₫', c: 'var(--red)' },
   { l: 'Số dư ròng trong kỳ', v: (balance < 0 ? '−' : '+') + fmtFull(Math.abs(balance)) + ' ₫', c: balance >= 0 ? 'var(--cyan)' : 'var(--red)' },
   { l: 'Tỷ lệ tiết kiệm', v: savRate + '%', c: 'var(--purple)' },
   { l: 'Chi tiêu trung bình/ngày', v: fmt(avgExpPerDay) + ' ₫', c: 'var(--amber)' },
   { l: 'Giao dịch chi cao nhất', v: maxExp > 0 ? fmt(maxExp) + ' ₫' : '—', c: 'var(--text1)' },
   { l: 'Số giao dịch ghi nhận', v: `Thu: ${filteredInc.length} • Chi: ${filteredExp.length}`, c: 'var(--text1)' },
  ].map(r => `<div style="display:flex;justify-content:space-between;align-items:center;padding-bottom:10px;border-bottom:1px solid var(--glass-b)">
   <span style="font-size:13px;color:var(--text2)">${r.l}</span>
   <span style="font-size:13.5px;font-weight:700;color:${r.c}">${r.v}</span>
  </div>`).join('')}
  </div>`;
 }
}
window.renderStats = renderStats;

/* ────────────────────────────────────────────────────────
  renderAll – called by Firebase listeners
──────────────────────────────────────────────────────── */

function renderToday() {
  const dt = new Date();
  const dateStr = dt.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' });
  const hr = dt.getHours();
  let greeting = 'Chào buổi sáng ☀️';
  if (hr >= 12 && hr < 18) greeting = 'Chào buổi chiều 🌤️';
  else if (hr >= 18 || hr < 5) greeting = 'Chào buổi tối 🌙';

  const td = today();
  // Unified Today Filter: due today, no date specified, or overdue pending
  const allTodayTodos = (window.DB.todos || []).filter(x => 
    x.date === td || (!x.date && !x.done) || (!x.done && x.date && x.date < td)
  );
  // Sort: overdue first, then high priority, then undone first
  allTodayTodos.sort((a, b) => {
    if (a.done !== b.done) return a.done ? 1 : -1;
    const aOverdue = !a.done && a.date && a.date < td;
    const bOverdue = !b.done && b.date && b.date < td;
    if (aOverdue !== bOverdue) return aOverdue ? -1 : 1;
    const priWeight = { high: 3, mid: 2, low: 1 };
    return (priWeight[b.priority] || 2) - (priWeight[a.priority] || 2);
  });
  const doneCount = allTodayTodos.filter(x => x.done).length;
  const totalCount = allTodayTodos.length;
  const pct = totalCount > 0 ? Math.round((doneCount / totalCount) * 100) : 0;

  const elDate = document.getElementById('todayDateStr');
  if (elDate) {
    elDate.innerHTML = `<div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:10px;">
      <div>
        <div style="font-size:13px;font-weight:600;color:var(--accent-light);margin-bottom:2px;">${greeting}</div>
        <div style="font-size:22px;font-weight:800;letter-spacing:-0.5px;">Hôm nay, ${dateStr}</div>
      </div>
      <div style="display:flex;align-items:center;gap:8px;background:var(--surface);border:1px solid var(--border);padding:6px 14px;border-radius:20px;">
        <i data-lucide="check-circle-2" class="ic-16" style="color:var(--success)"></i>
        <span style="font-size:12px;font-weight:600;color:var(--text-hi);">${doneCount}/${totalCount} việc (${pct}%)</span>
      </div>
    </div>`;
  }

  // 1. Render Todos (Unified & Interactive)
  const todoEl = document.getElementById('todayTodoList');
  if (todoEl) {
    if (!allTodayTodos.length) {
      todoEl.innerHTML = `<div class="empty-state" style="padding:24px 16px; margin:0;">
        <div class="empty-state-icon" style="width:38px;height:38px;"><i data-lucide="check-circle" class="ic-18"></i></div>
        <div class="empty-state-title" style="font-size:13.5px;">Không có việc nào hôm nay!</div>
        <div class="empty-state-desc" style="font-size:11.5px; margin-bottom:10px;">Thêm công việc cần xử lý để không bỏ lỡ.</div>
        <button class="btn btn-p btn-sm" onclick="openTodoModal()"><i data-lucide="plus" class="ic-14"></i> Thêm việc mới</button>
      </div>`;
    } else {
      todoEl.innerHTML = allTodayTodos.map(t => {
        const isOverdue = !t.done && t.date && t.date < td;
        const pl = { high: 'Cao', mid: 'TB', low: 'Thấp' };
        return `<div class="todo-item" style="padding:10px 14px; background:var(--surface); border:1px solid var(--border); border-radius:10px; display:flex; align-items:center; gap:12px; transition:var(--t); margin-bottom:8px; ${t.done ? 'opacity:0.6;' : ''}">
          <div class="todo-cb" role="checkbox" aria-checked="${Boolean(t.done)}" onclick="toggleTodo('${t.id}')" style="width:20px; height:20px; border-radius:6px; border:2px solid ${t.done ? 'var(--green)' : 'var(--text3)'}; background:${t.done ? 'var(--green)' : 'transparent'}; display:flex; align-items:center; justify-content:center; cursor:pointer; flex-shrink:0;">
            ${t.done ? '<span style="color:#000;font-size:11px;font-weight:900">✓</span>' : ''}
          </div>
          <div style="flex:1; min-width:0;">
            <div style="font-size:13.5px; font-weight:500; ${t.done ? 'text-decoration:line-through; color:var(--text-low);' : 'color:var(--text-hi);'} ${isOverdue ? 'color:var(--danger);font-weight:600;' : ''}">${t.text}</div>
            <div style="display:flex; align-items:center; gap:6px; margin-top:2px;">
              ${isOverdue ? '<span style="color:var(--danger); font-size:10.5px; font-weight:700; background:rgba(255,82,82,0.12); padding:1px 5px; border-radius:3px;">⚠️ Quá hạn</span>' : ''}
              ${t.priority === 'high' ? '<span style="color:var(--danger); font-size:10.5px; font-weight:600;">Ưu tiên cao</span>' : ''}
              ${t.note ? '<span style="color:var(--text-low); font-size:10.5px; max-width:180px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">💬 ' + t.note + '</span>' : ''}
            </div>
          </div>
          <button class="icon-btn" onclick="editTodo('${t.id}')" title="Chỉnh sửa"><i data-lucide="edit-2" class="ic-14"></i></button>
        </div>`;
      }).join('');
    }
  }

  // 2. Render Habits
  const habits = window.DB.habits || [];
  const habitEl = document.getElementById('todayHabitList');
  if (habitEl) {
    if (!habits.length) {
      habitEl.innerHTML = `<div class="empty-state" style="padding:24px 16px; margin:0;">
        <div class="empty-state-icon" style="width:38px;height:38px;"><i data-lucide="flame" class="ic-18"></i></div>
        <div class="empty-state-title" style="font-size:13.5px;">Chưa có thói quen</div>
        <div class="empty-state-desc" style="font-size:11.5px; margin-bottom:10px;">Thêm thói quen để rèn luyện mỗi ngày.</div>
        <button class="btn btn-p btn-sm" onclick="openHabit()"><i data-lucide="plus" class="ic-14"></i> Thêm thói quen</button>
      </div>`;
    } else {
      habitEl.innerHTML = habits.map(h => {
        const isDone = (h.history && h.history.includes(td)) || (h.logs && h.logs.includes(td));
        return `<div style="padding:10px 14px; background:var(--surface); border:1px solid var(--border); border-radius:10px; display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-size:13.5px; font-weight:600; ${isDone ? 'text-decoration:line-through;color:var(--text-low)' : 'color:var(--text-hi)'}">${h.name}</div>
            <div style="font-size:11px; color:var(--warning); margin-top:2px;">🔥 Chuỗi ${h.streak || 0} ngày</div>
          </div>
          <button class="btn btn-sm ${isDone ? 'btn-ghost' : 'btn-p'}" onclick="toggleHabitDay('${h.id}', '${td}')" style="font-size:12px; padding:4px 12px;">
            ${isDone ? '✓ Đã xong' : 'Hoàn thành'}
          </button>
        </div>`;
      }).join('');
    }
  }

  // 3. Render Events
  const events = (window.DB.events || []).filter(e => e.dateStart === td);
  const eventEl = document.getElementById('todayEventList');
  if (eventEl) {
    if (!events.length) {
      eventEl.innerHTML = `<div class="empty-state" style="padding:24px 16px; margin:0;">
        <div class="empty-state-icon" style="width:38px;height:38px;"><i data-lucide="calendar" class="ic-18"></i></div>
        <div class="empty-state-title" style="font-size:13.5px;">Không có sự kiện hôm nay</div>
        <div class="empty-state-desc" style="font-size:11.5px; margin-bottom:10px;">Lịch trình hôm nay trống rỗng.</div>
        <button class="btn btn-p btn-sm" onclick="openEv()"><i data-lucide="plus" class="ic-14"></i> Thêm sự kiện</button>
      </div>`;
    } else {
      eventEl.innerHTML = events.map(e => `<div style="padding:10px 14px; background:var(--surface); border:1px solid var(--border); border-left:3px solid var(--accent); border-radius:10px; display:flex; justify-content:space-between; align-items:center;">
        <div>
          <div style="font-size:13.5px; font-weight:600; color:var(--text-hi);">${e.title}</div>
          <div style="font-size:11.5px; color:var(--text-mid); margin-top:2px;">${e.timeStart ? e.timeStart + (e.timeEnd ? ' - ' + e.timeEnd : '') : 'Cả ngày'}</div>
        </div>
        <button class="icon-btn" onclick="editEv('${e.id}')"><i data-lucide="edit-2" class="ic-14"></i></button>
      </div>`).join('');
    }
  }

  if (window.lucide) window.lucide.createIcons();
}

window.renderAll = function(){
 try { if(typeof updateBadges==='function') updateBadges(); } catch(e) { console.warn('[LifeOS] renderAll: updateBadges error:', e.message); }
 const active=document.querySelector('.page.active'); if(!active)return;
 const pg=active.id.replace('p-','');
 const map={today:renderToday,overview:renderOverview,calendar:renderCal,schedule:renderWeek,projects:()=>{renderProjSelector();renderKanbanBoard();},todos:renderTodos,finance:()=>{renderFinKpi();renderFinTables();},stats:renderStats,notes:renderNotes,habits:renderHabits,goals:renderGoals,journal:renderJournal,pomodoro:renderPomodoro,vocab:renderVocab,mocktests:typeof renderMockTests==='function'?renderMockTests:null};
 try { if(map[pg]) map[pg](); } catch(e) { console.warn('[LifeOS] renderAll: error rendering "' + pg + '":', e.message); }
 try { if(window.lucide) window.lucide.createIcons(); } catch(e) {}
};

/* ────────────────────────────────────────────────────────
  TOPBAR DATE
──────────────────────────────────────────────────────── */
function setDate(){
 const e=document.getElementById('topDate');
 if(e)e.textContent=new Date().toLocaleDateString('vi-VN',{weekday:'long',day:'2-digit',month:'2-digit',year:'numeric'});
}
setDate(); setInterval(setDate,60000);
// Set default dates
['incDate','expDate'].forEach(id=>{const e=document.getElementById(id);if(e)e.value=today();});
// Resize charts on window resize
window.addEventListener('resize',()=>Object.values(charts).forEach(c=>{try{c.resize();}catch(e){}}));

// Register Service Worker for PWA
if ('serviceWorker' in navigator) {
 window.addEventListener('load', () => navigator.serviceWorker.register('sw.js').catch(() => {}));
}

/* ════════════════════════════════════════════════════════════
  RIPPLE EFFECT
════════════════════════════════════════════════════════════ */
document.addEventListener('click', function(e) {
 const el = e.target.closest('.btn, .sb-item, .mnav-item, .k-card, .bcd, .todo-item, .nc, .habit-card');
 if (!el) return;
 el.classList.add('ripple-host');
 const r = document.createElement('span');
 r.className = 'ripple';
 const rect = el.getBoundingClientRect();
 const sz = Math.max(rect.width, rect.height);
 r.style.width = r.style.height = sz + 'px';
 r.style.left = (e.clientX - rect.left - sz/2) + 'px';
 r.style.top = (e.clientY - rect.top - sz/2) + 'px';
 el.appendChild(r);
 setTimeout(() => r.remove(), 550);
});

/* ════════════════════════════════════════════════════════════
  STAGGER CLASS ON RENDER
════════════════════════════════════════════════════════════ */
const _origRenderTodos = typeof renderTodos === 'function' ? renderTodos : null;
const _origRenderNotes = typeof renderNotes === 'function' ? renderNotes : null;
const _origRenderHabits = typeof renderHabits === 'function' ? renderHabits : null;

function addStagger(containerId) {
 setTimeout(() => {
  const el = document.getElementById(containerId);
  if (el) { el.classList.remove('stagger-in'); void el.offsetWidth; el.classList.add('stagger-in'); }
 }, 50);
}

/* ================= SIDEBAR COLLAPSE LOGIC ================= */
window.toggleSbGroup = function(id) {
    const grp = document.getElementById(id);
    const ic = document.getElementById(id + '-ic');
    if (!grp) return;
    
    let isCollapsed = grp.classList.contains('collapsed');
    
    if (isCollapsed) {
        grp.classList.remove('collapsed');
        grp.style.maxHeight = grp.scrollHeight + 'px';
        if(ic) ic.classList.remove('sg-ic-rotated');
        localStorage.setItem('sb_' + id, 'open');
        
        setTimeout(() => { grp.style.maxHeight = 'none'; }, 300);
    } else {
        grp.style.maxHeight = grp.scrollHeight + 'px';
        void grp.offsetWidth; // force reflow
        grp.classList.add('collapsed');
        grp.style.maxHeight = '0';
        if(ic) ic.classList.add('sg-ic-rotated');
        localStorage.setItem('sb_' + id, 'closed');
    }
}

function initSidebarState() {
    for (let i = 1; i <= 6; i++) {
        const id = 'sg' + i;
        const state = localStorage.getItem('sb_' + id);
        const grp = document.getElementById(id);
        const ic = document.getElementById(id + '-ic');
        if (grp) {
            if (state === 'closed') {
                grp.classList.add('collapsed');
                grp.style.maxHeight = '0';
                if(ic) ic.classList.add('sg-ic-rotated');
            } else {
                grp.classList.remove('collapsed');
                grp.style.maxHeight = 'none';
                if(ic) ic.classList.remove('sg-ic-rotated');
            }
        }
    }
}
window.addEventListener('DOMContentLoaded', () => { setTimeout(initSidebarState, 100); });



window.currentProjSubtasks = [];

window.addProjSubtaskUI = function() {
    const input = document.getElementById('projNewSubtask');
    if(!input) return;
    const text = input.value.trim();
    if (!text) return;
    window.currentProjSubtasks.push({ id: 'st_' + Date.now(), text, done: false });
    input.value = '';
    renderProjSubtasksUI();
};

window.toggleProjSubtask = function(idx) {
    window.currentProjSubtasks[idx].done = !window.currentProjSubtasks[idx].done;
    renderProjSubtasksUI();
};

window.delProjSubtask = function(idx) {
    window.currentProjSubtasks.splice(idx, 1);
    renderProjSubtasksUI();
};

window.renderProjSubtasksUI = function() {
    const list = document.getElementById('projSubtaskList');
    if(!list) return;
    list.innerHTML = window.currentProjSubtasks.map((st, i) => `
        <div style="display:flex; align-items:center; gap:8px; background:var(--surface2); padding:6px 10px; border-radius:6px;">
            <input type="checkbox" ${st.done ? 'checked' : ''} onchange="toggleProjSubtask(${i})">
            <span style="flex:1; font-size:13px; ${st.done ? 'text-decoration:line-through; color:var(--text3)' : ''}">${st.text}</span>
            <button type="button" style="background:none; border:none; color:var(--red); cursor:pointer; padding:0 4px;" onclick="delProjSubtask(${i})">✕</button>
        </div>
    `).join('');
    
    // Auto-update progress
    const total = window.currentProjSubtasks.length;
    if(total > 0) {
        const done = window.currentProjSubtasks.filter(x => x.done).length;
        document.getElementById('projProgress').value = Math.round((done / total) * 100);
    }
};


window.renderProjGantt = function(projId) {
    const tasks = (window.DB.proj_tasks || []).filter(t => t.projId === projId);
    const container = document.getElementById('projGanttView');
    if(!container) return;
    
    if(tasks.length === 0) {
        document.getElementById('gantt').innerHTML = '';
        container.innerHTML = '<div style="text-align:center; padding:40px; color:var(--text3);">Không có nhiệm vụ nào để hiển thị biểu đồ Gantt.</div>';
        return;
    }
    
    // Ensure we have an svg element
    if(container.querySelector('svg') == null) {
        container.innerHTML = '<svg id="gantt"></svg>';
    } else {
        document.getElementById('gantt').innerHTML = '';
    }

    const ganttTasks = tasks.map(t => {
        let s = t.start ? new Date(t.start) : null;
        let e = t.due ? new Date(t.due) : null;
        
        if(s && e && s > e) { let temp = s; s = e; e = temp; }
        
        if(!s && !e) {
            s = new Date();
            e = new Date();
            e.setDate(s.getDate() + 1);
        } else if(!s && e) {
            s = new Date(e);
            s.setDate(s.getDate() - 1);
        } else if(s && !e) {
            e = new Date(s);
            e.setDate(e.getDate() + 1);
        }
        
        // Progress based on status
        let progress = 0;
        if(t.status === 'Đang làm') progress = 50;
        else if(t.status === 'Hoàn thành') progress = 100;
        
        return {
            id: t.id,
            name: t.text || 'Nhiệm vụ',
            start: toLocalDateStr(s),
            end: toLocalDateStr(e),
            progress: progress,
            custom_class: 'gantt-' + (t.status === 'Hoàn thành' ? 'done' : (t.status === 'Đang làm' ? 'doing' : 'todo'))
        };
    });

    if(window.Gantt) {
        new Gantt("#gantt", ganttTasks, {
            view_modes: ['Quarter Day', 'Half Day', 'Day', 'Week', 'Month'],
            view_mode: 'Day',
            date_format: 'YYYY-MM-DD',
            on_click: function (task) {
                openProjTaskModal(task.id);
            }
        });
    }
};


window.generateAIDigest = async function() {
    openModal('mAIDigest');
    const loading = document.getElementById('aiDigestLoading');
    const content = document.getElementById('aiDigestContent');
    if (loading) loading.style.display = 'block';
    if (content) content.style.display = 'none';

    try {
        let digestData = null;
        if (window.aiEngine && typeof window.aiEngine.generateWeeklyDigest === 'function') {
            digestData = await window.aiEngine.generateWeeklyDigest();
        }
        if (!digestData) {
            const now = new Date();
            const sevenDaysAgo = toLocalDateStr(new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000));
            const todos = (window.DB.todos || []).filter(t => t.done && t.date >= sevenDaysAgo);
            const exps = (window.DB.expense || []).filter(e => e.date >= sevenDaysAgo);
            const totalExp = exps.reduce((sum, e) => sum + (Number(e.amt) || 0), 0);
            digestData = {
                todosCount: todos.length,
                totalExp,
                insight: "Tuần qua bạn đã duy trì tiến độ làm việc ổn định và kiểm soát chi tiêu tốt. Hãy tiếp tục giữ vững phong độ trong tuần mới! 🚀"
            };
        }

        const html = `
<div style="display:flex; flex-direction:column; gap:20px; animation: pageFadeIn 0.3s ease;">
  <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(180px, 1fr)); gap:14px;">
    <!-- Tasks Card -->
    <div class="card-glass" style="padding:16px; display:flex; flex-direction:column; gap:8px;">
       <div style="display:flex; align-items:center; gap:8px; color:var(--text-mid); font-size:12.5px; font-weight:600; text-transform:uppercase;">
          <i data-lucide="check-circle" class="ic-16" style="color:var(--success)"></i> Công việc
       </div>
       <div style="font-size:26px; font-weight:800; color:var(--text-hi);">${digestData.todosCount}</div>
       <div style="font-size:11.5px; color:var(--text-low);">nhiệm vụ hoàn thành tuần qua</div>
    </div>
    
    <!-- Finance Card -->
    <div class="card-glass" style="padding:16px; display:flex; flex-direction:column; gap:8px;">
       <div style="display:flex; align-items:center; gap:8px; color:var(--text-mid); font-size:12.5px; font-weight:600; text-transform:uppercase;">
          <i data-lucide="wallet" class="ic-16" style="color:var(--danger)"></i> Chi tiêu
       </div>
       <div style="font-size:26px; font-weight:800; color:var(--text-hi); font-variant-numeric:tabular-nums;">${digestData.totalExp.toLocaleString('vi-VN')}₫</div>
       <div style="font-size:11.5px; color:var(--text-low);">tổng chi 7 ngày gần nhất</div>
    </div>
  </div>

  <!-- AI Coach Insight -->
  <div style="background:linear-gradient(135deg, rgba(124,77,255,0.12), rgba(0,229,255,0.06)); border:1px solid rgba(124,77,255,0.25); border-radius:14px; padding:20px;">
    <div style="display:flex; align-items:center; gap:8px; font-size:14px; font-weight:700; color:var(--accent-light); margin-bottom:10px;">
      <i data-lucide="sparkles" class="ic-18"></i> Nhận xét từ LifeOS AI
    </div>
    <div style="font-size:13.5px; line-height:1.6; color:var(--text-hi);">${digestData.insight.replace(/\n/g, '<br>')}</div>
  </div>
</div>`;

        if (content) {
            content.innerHTML = html;
            content.style.display = 'block';
        }
        if (loading) loading.style.display = 'none';
        if (window.lucide) window.lucide.createIcons();

    } catch(err) {
        console.error('Digest error:', err);
        if (loading) loading.style.display = 'none';
        if (content) {
            content.innerHTML = '<div style="color:var(--danger);padding:20px;text-align:center;">Không thể tạo báo cáo lúc này. Vui lòng thử lại sau.</div>';
            content.style.display = 'block';
        }
    }
};

// ── Global Keyboard Shortcuts ──
document.addEventListener('keydown', (e) => {
  // ESC: Close open modals / bottom sheets
  if (e.key === 'Escape') {
    document.querySelectorAll('.overlay.open').forEach(m => {
      m.classList.remove('open');
      m.style.display = 'none';
    });
    if (typeof closeMobileSidebar === 'function') closeMobileSidebar();
  }
  // Ctrl + K / Cmd + K: Open Command Palette
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault();
    if (typeof toggleCmdPalette === 'function') toggleCmdPalette();
  }
});


/* ────────────────────────────────────────────────────────
  QUICK FINANCE MODAL HANDLERS
──────────────────────────────────────────────────────── */
window.openQuickFinanceModal = function(type = 'exp') {
  const d = document.getElementById('qfExpDate');
  if (d) d.value = today();
  const di = document.getElementById('qfIncDate');
  if (di) di.value = today();
  switchQuickFinType(type);
  openModal('mQuickFin');
};

window.switchQuickFinType = function(type) {
  const tabExp = document.getElementById('qfTabExp');
  const tabInc = document.getElementById('qfTabInc');
  const fExp = document.getElementById('qfExpForm');
  const fInc = document.getElementById('qfIncForm');
  
  if (type === 'exp') {
    if (tabExp) tabExp.classList.add('active');
    if (tabInc) tabInc.classList.remove('active');
    if (fExp) fExp.style.display = 'block';
    if (fInc) fInc.style.display = 'none';
  } else {
    if (tabExp) tabExp.classList.remove('active');
    if (tabInc) tabInc.classList.add('active');
    if (fExp) fExp.style.display = 'none';
    if (fInc) fInc.style.display = 'block';
  }
};

window.saveQuickExpense = async function() {
  const amt = parseFloat(document.getElementById('qfExpAmt')?.value);
  if (!amt || amt <= 0) { toast('Nhập số tiền hợp lệ!', 'error'); return; }
  const cat = document.getElementById('qfExpCat')?.value || 'Khác';
  const date = document.getElementById('qfExpDate')?.value || today();
  const pay = document.getElementById('qfExpPay')?.value || 'Tiền mặt';
  const note = document.getElementById('qfExpNote')?.value || '';
  
  const list = [...(window.DB.expense || [])];
  list.unshift({ id: uid(), cat, amt, date, pay, note });
  await window.persist('expense', list);
  
  document.getElementById('qfExpAmt').value = '';
  document.getElementById('qfExpNote').value = '';
  closeModal('mQuickFin');
  toast('Đã ghi nhận khoản chi ' + fmt(amt) + ' ₫!', 'success');
  if (window.renderAll) window.renderAll();
};

window.saveQuickIncome = async function() {
  const amt = parseFloat(document.getElementById('qfIncAmt')?.value);
  if (!amt || amt <= 0) { toast('Nhập số tiền hợp lệ!', 'error'); return; }
  const src = document.getElementById('qfIncSrc')?.value || 'Khác';
  const date = document.getElementById('qfIncDate')?.value || today();
  const note = document.getElementById('qfIncNote')?.value || '';
  
  const list = [...(window.DB.income || [])];
  list.unshift({ id: uid(), src, amt, date, note });
  await window.persist('income', list);
  
  document.getElementById('qfIncAmt').value = '';
  document.getElementById('qfIncNote').value = '';
  closeModal('mQuickFin');
  toast('Đã ghi nhận khoản thu ' + fmt(amt) + ' ₫!', 'success');
  if (window.renderAll) window.renderAll();
};
