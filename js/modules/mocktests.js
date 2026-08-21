/* ════════════════════════════════════════════════════════════
  MOCK TESTS
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
 toast('Đã xóa', 'info');
}

function renderMockTests() {
 const c = document.getElementById('mockTestsGrid'); if (!c) return;
 const tests = window.DB.mocktests || [];
 
 if (!tests.length) {
  c.innerHTML = '<div style="color:var(--text3);padding:30px;text-align:center">Chưa có bài test nào. Bấm "+ Nhập điểm thi" để bắt đầu!</div>';
  if(mockChartInstance) { mockChartInstance.destroy(); mockChartInstance=null; }
  return;
 }
 
 // Render cards (reverse order for grid)
 const revTests = [...tests].reverse();
 c.innerHTML = revTests.map(m => `
  <div class="mock-card">
   <button class="btn-del" onclick="delMockTest('${m.id}')">✕</button>
   <div class="mock-hdr">
    <div>
     <div class="mock-title" style="cursor:pointer" onclick="openMockTest('${m.id}')">${m.name} </div>
     <div class="mock-date">${fmtDate(m.date)}</div>
    </div>
   </div>
   <div class="mock-scores">
    <div class="ms-item"><div class="ms-item-lbl">Nghe</div><div class="ms-item-val">${m.list}</div></div>
    <div class="ms-item"><div class="ms-item-lbl">Đọc</div><div class="ms-item-val">${m.read}</div></div>
    <div class="ms-item"><div class="ms-item-lbl">Nói</div><div class="ms-item-val">${m.speak}</div></div>
    <div class="ms-item"><div class="ms-item-lbl">Viết</div><div class="ms-item-val">${m.write}</div></div>
    <div class="ms-total">${m.total}</div>
   </div>
   ${m.note ? `<div class="mock-note">${m.note}</div>` : ''}
  </div>
 `).join('');
 
 renderMockTestChart(tests);
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
 
 const accent = getComputedStyle(document.body).getPropertyValue('--accent').trim();
 const green = getComputedStyle(document.body).getPropertyValue('--green').trim();
 const text = getComputedStyle(document.body).getPropertyValue('--text3').trim();
 
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


// ==========================================
// AI ASSISTANT LOGIC
// ==========================================
let aiChatHistory = [];
const DEFAULT_KEY = "";


function saveAiHistory() {
  localStorage.setItem('lifeos_ai_history', JSON.stringify(aiChatHistory));
}

function loadAiHistory() {
  try {
    const saved = localStorage.getItem('lifeos_ai_history');
    if (saved) {
      aiChatHistory = JSON.parse(saved);
      const c = document.getElementById('aiMessages');
      // Keep only the first child (greeting)
      while (c.children.length > 1) { c.removeChild(c.lastChild); }
      aiChatHistory.forEach(msg => {
        if (msg.role === 'user') appendAiMsg(msg.parts[0].text, true);
        else if (msg.role === 'model') appendAiMsg(msg.parts[0].text, false);
      });
      c.scrollTop = c.scrollHeight;
    }
  } catch(e) {}
}

function clearAiHistory() {
  if(confirm('Bạn muốn xóa toàn bộ lịch sử trò chuyện với AI?')) {
    aiChatHistory = [];
    saveAiHistory();
    const c = document.getElementById('aiMessages');
    while (c.children.length > 1) { c.removeChild(c.lastChild); }
    toast('Đã xóa lịch sử AI', 'info');
  }
}
function toggleAiChat() {
  const panel = document.getElementById('aiChatPanel');
  if (panel.style.display === 'none') {
    panel.style.display = 'flex';
    document.getElementById('aiApiKey').value = localStorage.getItem('lifeos_ai_key') || DEFAULT_KEY;
    document.getElementById('aiModelSelect').value = localStorage.getItem('lifeos_ai_model') || 'gemini-2.5-flash-lite';
    loadAiHistory();
    if(window.lucide) window.lucide.createIcons();
    setTimeout(() => document.getElementById('aiChatInput').focus(), 100);
  } else {
    panel.style.display = 'none';
  }
}

function toggleAiSettings() {
  const setDiv = document.getElementById('aiSettings');
  setDiv.style.display = setDiv.style.display === 'none' ? 'block' : 'none';
}

function saveAiKey() {
  const k = document.getElementById('aiApiKey').value.trim();
  const m = document.getElementById('aiModelSelect').value;
  localStorage.setItem('lifeos_ai_key', k);
  localStorage.setItem('lifeos_ai_model', m);
  toast('Đã lưu cấu hình AI', 'success');
  toggleAiSettings();
}

function generateAIContext() {
  const db = window.DB;
  if (!db) return "Không có dữ liệu.";
  const td = (db.todos || []);
  const ev = (db.events || []);
  const pj = (db.projects || []);
  const inc = (db.income || []);
  const exp = (db.expense || []);
  const nts = (db.notes || []);
  
  const dToday = toLocalDateStr();
  
  let ctx = `Hệ thống Data (Kèm ID để bạn DÙNG TOOL Sửa/Xóa khi user yêu cầu). Hôm nay là ngày ${dToday}:
`;
  ctx += `[Todos]:
` + td.map(x=>`- ID:${x.id} | ${x.text} | ${x.done?'Xong':'Chưa'} | Ngày:${x.date}`).join('\n') + '\n';
  ctx += `[Lịch hẹn]:
` + ev.map(x=>`- ID:${x.id} | ${x.title} | Ngày:${x.dateStart} ${x.timeStart||''}`).join('\n') + '\n';
  ctx += `[Dự án]:
` + pj.map(x=>`- ID:${x.id} | ${x.name} | Hạn:${x.deadline||''}`).join('\n') + '\n';
  ctx += `[Ghi chú]:
` + nts.map(x=>`- ID:${x.id} | ${x.title}`).join('\n') + '\n';
  ctx += `[Thu nhập]:
` + inc.map(x=>`- ID:${x.id} | +${fmt(x.amt)} | ${x.desc}`).join('\n') + '\n';
  ctx += `[Chi phí]:
` + exp.map(x=>`- ID:${x.id} | -${fmt(x.amt)} | ${x.desc}`).join('\n') + '\n';
  
  ctx += `\nBẠN LÀ QUẢN GIA KỸ THUẬT SỐ TOÀN NĂNG TÍCH HỢP SÂU VÀO HỆ THỐNG LIFEOS. 
- Bạn ĐƯỢC CẤP QUYỀN dùng tools để quản lý (Thêm/Sửa/Xóa/Đánh dấu xong) MỌI THỨ: Lịch, Todo, Dự án, Tài chính, Ghi chú.
- KHI NGƯỜI DÙNG YÊU CẦU LÀM GÌ, HÃY GỌI TOOL NGAY LẬP TỨC. ĐỪNG CHỈ TRẢ LỜI BẰNG TEXT.
- Tự động điền các trường bắt buộc (ví dụ: ngày hôm nay ${dToday} nếu user nói "hôm nay", "bây giờ").
- Khi user nhờ xóa/sửa, hãy dùng ID tương ứng trong danh sách Data ở trên.
- Bạn phải hành động dứt khoát, chủ động giúp người dùng tối đa hóa năng suất.`;
  return ctx;
}

function appendAiMsg(text, isUser) {
  const c = document.getElementById('aiMessages');
  const d = document.createElement('div');
  d.style.padding = '10px 14px';
  d.style.borderRadius = '12px';
  d.style.maxWidth = '85%';
  d.style.whiteSpace = 'pre-wrap';
  
  if (isUser) {
    d.style.alignSelf = 'flex-end';
    d.style.background = 'var(--pri)';
    d.style.color = '#fff';
    d.style.borderBottomRightRadius = '2px';
  } else {
    d.style.alignSelf = 'flex-start';
    d.style.background = 'var(--bg)';
    d.style.border = '1px solid var(--border)';
    d.style.borderBottomLeftRadius = '2px';
  }
  
  let formatted = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  d.innerHTML = formatted;
  
  c.appendChild(d);
  c.scrollTop = c.scrollHeight;
}

function showAiTyping() {
  const c = document.getElementById('aiMessages');
  const d = document.createElement('div');
  d.id = 'aiTypingInd';
  d.style.alignSelf = 'flex-start';
  d.style.color = 'var(--text3)';
  d.style.fontSize = '12px';
  d.style.fontStyle = 'italic';
  d.textContent = 'AI đang suy nghĩ...';
  c.appendChild(d);
  c.scrollTop = c.scrollHeight;
}

function removeAiTyping() {
  const el = document.getElementById('aiTypingInd');
  if (el) el.remove();
}

async function sendAiMessage() {
  const inp = document.getElementById('aiChatInput');
  const text = inp.value.trim();
  if (!text) return;
  
  let key = localStorage.getItem('lifeos_ai_key') || document.getElementById('aiApiKey').value.trim();
  let baseModel = localStorage.getItem('lifeos_ai_model') || document.getElementById('aiModelSelect').value || 'gemini-2.5-flash-lite';
  
  if (!key) {
    toast('Vui lòng nhập API Key để sử dụng AI', 'error');
    toggleAiSettings();
    return;
  }
  
  inp.value = '';
  appendAiMsg(text, true);
  
  aiChatHistory.push({ role: 'user', parts: [{ text }] });
  const contextText = generateAIContext();
  
  const tools = [{
    functionDeclarations: [
      {
        name: "manage_calendar",
        description: "Thêm, sửa, hoặc xóa một sự kiện lịch hẹn.",
        parameters: {
          type: "OBJECT",
          properties: {
            action: { type: "STRING", description: "Hành động: 'add', 'edit', 'delete'" },
            id: { type: "STRING", description: "ID sự kiện (bắt buộc khi edit/delete)" },
            title: { type: "STRING", description: "Tên sự kiện" },
            dateStart: { type: "STRING", description: "Ngày (YYYY-MM-DD)" },
            timeStart: { type: "STRING", description: "Giờ (HH:MM)" }
          },
          required: ["action"]
        }
      },
      {
        name: "manage_finance",
        description: "Thêm, xóa thu nhập hoặc chi phí.",
        parameters: {
          type: "OBJECT",
          properties: {
            action: { type: "STRING", description: "Hành động: 'add', 'delete'" },
            id: { type: "STRING", description: "ID giao dịch (khi delete)" },
            type: { type: "STRING", description: "Loại: 'income' hoặc 'expense'" },
            amount: { type: "NUMBER", description: "Số tiền (số nguyên dương)" },
            title: { type: "STRING", description: "Mô tả / Tên giao dịch" },
            date: { type: "STRING", description: "Ngày (YYYY-MM-DD)" },
            cat: { type: "STRING", description: "Danh mục (ví dụ: luong, an-uong, di-lai, khac)" }
          },
          required: ["action"]
        }
      },
      {
        name: "manage_todo",
        description: "Thêm, xóa, hoặc đánh dấu hoàn thành công việc (todo).",
        parameters: {
          type: "OBJECT",
          properties: {
            action: { type: "STRING", description: "Hành động: 'add', 'delete', 'toggle' (toggle là đánh dấu hoàn thành/chưa hoàn thành)" },
            id: { type: "STRING", description: "ID công việc (khi delete/toggle)" },
            title: { type: "STRING", description: "Tên công việc" },
            date: { type: "STRING", description: "Ngày hạn chót (YYYY-MM-DD)" }
          },
          required: ["action"]
        }
      },
      {
        name: "manage_project",
        description: "Thêm, sửa, xóa dự án.",
        parameters: {
          type: "OBJECT",
          properties: {
            action: { type: "STRING", description: "Hành động: 'add', 'edit', 'delete'" },
            id: { type: "STRING", description: "ID dự án (khi edit/delete)" },
            name: { type: "STRING", description: "Tên dự án" },
            desc: { type: "STRING", description: "Mô tả dự án" },
            deadline: { type: "STRING", description: "Hạn chót (YYYY-MM-DD)" }
          },
          required: ["action"]
        }
      },
      {
        name: "manage_note",
        description: "Thêm, sửa, xóa ghi chú.",
        parameters: {
          type: "OBJECT",
          properties: {
            action: { type: "STRING", description: "Hành động: 'add', 'edit', 'delete'" },
            id: { type: "STRING", description: "ID ghi chú (khi edit/delete)" },
            title: { type: "STRING", description: "Tiêu đề ghi chú" },
            content: { type: "STRING", description: "Nội dung ghi chú" }
          },
          required: ["action"]
        }
      }
    ]
  }];
  
  const payload = {
    systemInstruction: { parts: [{ text: contextText }] },
    tools: tools,
    contents: aiChatHistory
  };
  
  showAiTyping();
  
  const fallbackModels = [baseModel, 'gemini-2.5-flash-lite', 'gemini-2.5-flash', 'gemini-2.0-flash-lite', 'gemini-2.0-flash', 'gemini-1.5-flash', 'gemini-3.5-flash'];
  const uniqueModels = [...new Set(fallbackModels)];
  
  let success = false;
  let finalErr = "Lỗi không xác định";
  
  for (let i = 0; i < uniqueModels.length; i++) {
    const currentModel = uniqueModels[i];
    try {
      const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${currentModel}:generateContent?key=${key}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      
      if (!res.ok) {
        const err = await res.json();
        const msg = err.error?.message || '';
        if (res.status === 503 || (msg.includes('Quota') && msg.includes('limit: 0')) || res.status === 404) {
          finalErr = msg;
          continue; 
        }
        throw new Error(msg || 'Lỗi kết nối API');
      }
      
      const data = await res.json();
      const parts = data.candidates[0].content.parts;
      removeAiTyping();
      
      let replyText = "";
      
      for (const part of parts) {
        if (part.functionCall) {
          const fn = part.functionCall.name;
          const args = part.functionCall.args;
          
          if (fn === 'manage_calendar') {
             if (args.action === 'add') {
                const ev = { id: uid(), title: args.title||'Sự kiện', dateStart: args.dateStart, dateEnd: args.dateStart, timeStart: args.timeStart || '', timeEnd: '', type: 'work', desc: 'AI tạo' };
                window.DB.events = window.DB.events || [];
                window.DB.events.push(ev);
                await persist('events', window.DB.events);
                replyText += `Đã thêm lịch: **${args.title}**\n`;
             } else if (args.action === 'delete' && args.id) {
                window.DB.events = (window.DB.events || []).filter(e => e.id !== args.id);
                await persist('events', window.DB.events);
                replyText += `Đã xóa lịch.\n`;
             } else if (args.action === 'edit' && args.id) {
                const idx = (window.DB.events || []).findIndex(e => e.id === args.id);
                if (idx >= 0) {
                    if (args.title) window.DB.events[idx].title = args.title;
                    if (args.dateStart) { window.DB.events[idx].dateStart = args.dateStart; window.DB.events[idx].dateEnd = args.dateStart; }
                    if (args.timeStart) window.DB.events[idx].timeStart = args.timeStart;
                    await persist('events', window.DB.events);
                    replyText += `Đã cập nhật lịch: **${window.DB.events[idx].title}**\n`;
                }
             }
          }
          else if (fn === 'manage_finance') {
             if (args.action === 'add') {
                const dDate = args.date || toLocalDateStr();
                const r = { id: uid(), amt: Number(args.amount)||0, cat: args.cat||'khac', desc: args.title||'', date: dDate };
                if (args.type === 'income') {
                    window.DB.income = window.DB.income || [];
                    window.DB.income.push(r);
                    await persist('income', window.DB.income);
                    replyText += `Đã thêm thu nhập: **+${fmt(r.amt)}đ** (${r.desc})\n`;
                } else {
                    window.DB.expense = window.DB.expense || [];
                    window.DB.expense.push(r);
                    await persist('expense', window.DB.expense);
                    replyText += `Đã thêm chi phí: **-${fmt(r.amt)}đ** (${r.desc})\n`;
                }
             } else if (args.action === 'delete' && args.id) {
                if (args.type === 'income') {
                    window.DB.income = (window.DB.income || []).filter(e => e.id !== args.id);
                    await persist('income', window.DB.income);
                } else {
                    window.DB.expense = (window.DB.expense || []).filter(e => e.id !== args.id);
                    await persist('expense', window.DB.expense);
                }
                replyText += `Đã xóa giao dịch.\n`;
             }
          }
          else if (fn === 'manage_todo') {
             if (args.action === 'add') {
                const dDate = args.date || toLocalDateStr();
                const t = { id: uid(), text: args.title, date: dDate, done: false, projId: '' };
                window.DB.todos = window.DB.todos || [];
                window.DB.todos.push(t);
                await persist('todos', window.DB.todos);
                replyText += `Đã thêm việc: **${args.title}**\n`;
             } else if (args.action === 'delete' && args.id) {
                window.DB.todos = (window.DB.todos || []).filter(e => e.id !== args.id);
                await persist('todos', window.DB.todos);
                replyText += `Đã xóa việc.\n`;
             } else if (args.action === 'toggle' && args.id) {
                const idx = (window.DB.todos || []).findIndex(e => e.id === args.id);
                if (idx >= 0) {
                    window.DB.todos[idx].done = !window.DB.todos[idx].done;
                    await persist('todos', window.DB.todos);
                    replyText += window.DB.todos[idx].done ? `Đã check xong việc!\n` : `Đã bỏ check việc.\n`;
                }
             }
          }
          else if (fn === 'manage_project') {
             if (args.action === 'add') {
                const p = { id: uid(), name: args.name, desc: args.desc||'', deadline: args.deadline||'', status: 'active', color: '#3b82f6' };
                window.DB.projects = window.DB.projects || [];
                window.DB.projects.push(p);
                await persist('projects', window.DB.projects);
                replyText += `Đã tạo dự án: **${args.name}**\n`;
             } else if (args.action === 'delete' && args.id) {
                window.DB.projects = (window.DB.projects || []).filter(e => e.id !== args.id);
                await persist('projects', window.DB.projects);
                replyText += `Đã xóa dự án.\n`;
             } else if (args.action === 'edit' && args.id) {
                const idx = (window.DB.projects || []).findIndex(e => e.id === args.id);
                if (idx >= 0) {
                    if (args.name) window.DB.projects[idx].name = args.name;
                    if (args.desc) window.DB.projects[idx].desc = args.desc;
                    await persist('projects', window.DB.projects);
                    replyText += `Đã sửa dự án.\n`;
                }
             }
          }
          else if (fn === 'manage_note') {
             if (args.action === 'add') {
                const n = { id: uid(), title: args.title||'Không tên', content: args.content||'', created: new Date().toISOString() };
                window.DB.notes = window.DB.notes || [];
                window.DB.notes.push(n);
                await persist('notes', window.DB.notes);
                replyText += `Đã lưu ghi chú: **${args.title}**\n`;
             } else if (args.action === 'delete' && args.id) {
                window.DB.notes = (window.DB.notes || []).filter(e => e.id !== args.id);
                await persist('notes', window.DB.notes);
                replyText += `Đã xóa ghi chú.\n`;
             } else if (args.action === 'edit' && args.id) {
                const idx = (window.DB.notes || []).findIndex(e => e.id === args.id);
                if (idx >= 0) {
                    if (args.title) window.DB.notes[idx].title = args.title;
                    if (args.content) window.DB.notes[idx].content = args.content;
                    await persist('notes', window.DB.notes);
                    replyText += `Đã sửa ghi chú.\n`;
                }
             }
          }
          if (typeof renderAll === 'function') renderAll();
        }
        if (part.text) {
          replyText += part.text + "\n";
        }
      }
      
      replyText = replyText.trim();
      if (!replyText) replyText = "Đã thực hiện xong yêu cầu!";
      
      aiChatHistory.push({ role: 'model', parts: [{ text: replyText }] });
      appendAiMsg(replyText, false);
      saveAiHistory();
      success = true;
      break; 
      
    } catch (err) {
      if (err.message.includes('fetch')) {
         continue; 
      }
      finalErr = err.message;
      continue;
    }
  }
  
  if (!success) {
      removeAiTyping();
      let errMsg = finalErr;
      appendAiMsg('Hệ thống AI Google đang bị quá tải hoàn toàn trên tất cả các kênh (Hoặc API Key không hợp lệ). Lỗi: ' + errMsg, false);
      aiChatHistory.pop();
  }
}



