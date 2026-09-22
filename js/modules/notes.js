/* ════════════════════════════════════════════════════════════
  NOTES
════════════════════════════════════════════════════════════ */
let noteEditor = null;

async function openNote(id) {
 document.getElementById('noteId').value = '';
 document.getElementById('noteNTitle').value = ''; 
 if(document.getElementById('noteTags')) document.getElementById('noteTags').value = '';
 document.querySelectorAll('.nc-color').forEach(c => c.classList.remove('active'));
 document.querySelector('.nc-color').classList.add('active');
 document.getElementById('mNoteTitle').textContent = 'Ghi chú mới';
 
 let initialData = {};
 
 if (id) {
  const n = (window.DB.notes || []).find(x => x.id === id);
  if (n) {
   document.getElementById('noteId').value = n.id;
   document.getElementById('noteNTitle').value = n.title || ''; 
   if(document.getElementById('noteTags')) document.getElementById('noteTags').value = (n.tags || []).join(', ');
   document.querySelectorAll('.nc-color').forEach(c => {
    c.classList.toggle('active', c.dataset.color === n.color);
   });
   document.getElementById('mNoteTitle').textContent = 'Sửa ghi chú';
   
   // Handle legacy HTML or new JSON blocks
   if (n.body) {
    if (n.body.startsWith('{"time"')) {
     try {
      initialData = JSON.parse(n.body);
     } catch(e) {}
    } else {
     // Legacy HTML - convert to single paragraph
     initialData = {
      blocks: [
       { type: "paragraph", data: { text: n.body } }
      ]
     };
    }
   }
  }
 }
 
 // Init or update Editor
 if (noteEditor) {
  await noteEditor.isReady;
  noteEditor.render(initialData);
 } else {
  noteEditor = new EditorJS({
   holder: 'noteBody',
   placeholder: 'Gõ / để chọn block...',
   data: initialData,
   tools: {
    header: Header,
    list: window.EditorjsList || window.NestedList || window.List,
    checklist: Checklist,
    quote: Quote,
    code: CodeTool,
    table: {
     class: Table,
     inlineToolbar: true,
     config: { rows: 3, cols: 3, withHeadings: true }
    }
   }
  });
 }
 
 
 // Calculate Backlinks
 const blContainer = document.getElementById('noteBacklinks');
 const blList = document.getElementById('noteBacklinksList');
 if (blContainer && blList) {
  if (id) {
   const currentTitle = (window.DB.notes.find(x => x.id === id) || {}).title || '';
   const titleMatch = currentTitle ? `[[${currentTitle}]]` : null;
   const idMatch = `[[${id}]]`;
   
   const backlinks = window.DB.notes.filter(n => {
    if (n.id === id) return false;
    if (!n.body) return false;
    return (titleMatch && n.body.includes(titleMatch)) || n.body.includes(idMatch);
   });
   
   if (backlinks.length > 0) {
    blList.innerHTML = backlinks.map(n => `<button type="button" onclick="closeModal('mNote'); setTimeout(()=>openNote('${window.LifeOSData.escapeAttr(n.id)}'), 300)" style="color:var(--text-accent);text-decoration:none;font-size:13px;background:none;border:0;padding:0;text-align:left"><i data-lucide="file-text" style="width:12px;height:12px"></i> ${window.LifeOSData.escapeHtml(n.title || 'Không tiêu đề')}</button>`).join('');
    blContainer.style.display = 'block';
    if(window.lucide) window.lucide.createIcons();
   } else {
    blContainer.style.display = 'none';
   }
  } else {
   blContainer.style.display = 'none';
  }
 }

 openModal('mNote');
}

function pickNoteColor(el) {
 document.querySelectorAll('.nc-color').forEach(c => c.classList.remove('active'));
 el.classList.add('active');
}

async function saveNote() {
 const title = document.getElementById('noteNTitle').value.trim();
 
 let outputData = {};
 if (noteEditor) {
  try {
   outputData = await noteEditor.save();
  } catch(e) {
   console.error("Editor save failed", e);
  }
 }
 
 const bodyString = JSON.stringify(outputData);
 const hasBlocks = outputData.blocks && outputData.blocks.length > 0;
 
 if (!title && !hasBlocks) { toast('Vui lòng nhập nội dung', 'error'); return; }
 
 const color = document.querySelector('.nc-color.active')?.dataset.color || '#7c4dff';
 const id = document.getElementById('noteId').value || uid();
 if (!window.DB.notes) window.DB.notes = [];
 const idx = window.DB.notes.findIndex(x => x.id === id);
 const tagsEl = document.getElementById('noteTags');
 const tags = tagsEl ? tagsEl.value.split(',').map(x=>x.trim()).filter(x=>x) : []; 
 const note = { id, title, body: bodyString, color, date: today(), tags };
 
 if (idx >= 0) window.DB.notes[idx] = note; 
 else window.DB.notes.unshift(note);
 
 await persist('notes', window.DB.notes);
 closeModal('mNote');
 renderNotes();
 toast('Đã lưu ghi chú!', 'success');
}

function saveToNotesTrash(note) {
  try {
    const trash = JSON.parse(localStorage.getItem('lifeos_notes_trash') || '[]');
    trash.unshift({ ...note, deletedAt: new Date().toISOString() });
    localStorage.setItem('lifeos_notes_trash', JSON.stringify(trash.slice(0, 30)));
  } catch(e) {}
}

async function delNote(id) {
  const t = (window.DB.notes || []).find(x => x.id === id);
  if (!t) return;
  if (!confirm(`Bạn có chắc chắn muốn xóa ghi chú "${t.title || 'Không tiêu đề'}"?`)) return;

  saveToNotesTrash(t);
  window.DB.notes = (window.DB.notes || []).filter(x => x.id !== id);
  await persist('notes', window.DB.notes);
  renderNotes();
  toast('Đã xóa ghi chú! (Đã lưu vào Thùng rác để khôi phục khi cần)', 'info');
}

function openNotesTrash() {
  renderNotesTrash();
  openModal('mNotesTrash');
}

function renderNotesTrash() {
  const listEl = document.getElementById('notesTrashList');
  if (!listEl) return;
  let trash = [];
  try {
    trash = JSON.parse(localStorage.getItem('lifeos_notes_trash') || '[]');
  } catch(e) {}

  if (!trash.length) {
    listEl.innerHTML = '<div style="text-align:center; padding:30px 10px; color:var(--text3); font-size:13px;">Thùng rác đang trống.</div>';
    return;
  }

  listEl.innerHTML = trash.map((n, idx) => `
    <div style="display:flex; justify-content:space-between; align-items:center; padding:12px; border:1px solid var(--border); border-radius:10px; margin-bottom:8px; background:var(--surface);">
      <div style="flex:1; margin-right:10px; overflow:hidden;">
        <div style="font-weight:600; font-size:13.5px; color:var(--text1); white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">
          ${window.LifeOSData.escapeHtml(n.title || 'Không tiêu đề')}
        </div>
        <div style="font-size:11.5px; color:var(--text3); margin-top:3px;">
          Đã xóa lúc: ${new Date(n.deletedAt || Date.now()).toLocaleString('vi-VN')}
        </div>
      </div>
      <div style="display:flex; gap:6px;">
        <button class="btn btn-sm btn-p" onclick="restoreNoteFromTrash(${idx})"><i data-lucide="rotate-ccw" style="width:12px;height:12px;margin-right:4px;"></i> Khôi phục</button>
        <button class="btn btn-sm btn-outline" style="border-color:var(--red); color:var(--red);" onclick="permanentDeleteTrashNote(${idx})"><i data-lucide="trash-2" style="width:12px;height:12px;"></i></button>
      </div>
    </div>
  `).join('');
  if (window.lucide) window.lucide.createIcons();
}

async function restoreNoteFromTrash(idx) {
  try {
    const trash = JSON.parse(localStorage.getItem('lifeos_notes_trash') || '[]');
    const item = trash.splice(idx, 1)[0];
    if (!item) return;
    localStorage.setItem('lifeos_notes_trash', JSON.stringify(trash));
    delete item.deletedAt;
    if (!window.DB.notes) window.DB.notes = [];
    window.DB.notes.unshift(item);
    await persist('notes', window.DB.notes);
    renderNotes();
    renderNotesTrash();
    toast(`Đã khôi phục ghi chú "${item.title || 'Không tiêu đề'}"!`, 'success');
  } catch(e) {
    console.error(e);
  }
}

function permanentDeleteTrashNote(idx) {
  try {
    const trash = JSON.parse(localStorage.getItem('lifeos_notes_trash') || '[]');
    trash.splice(idx, 1);
    localStorage.setItem('lifeos_notes_trash', JSON.stringify(trash));
    renderNotesTrash();
    toast('Đã xóa vĩnh viễn khỏi thùng rác', 'info');
  } catch(e) {}
}

function emptyNotesTrash() {
  if (!confirm('Bạn có chắc chắn muốn dọn sạch thùng rác ghi chú?')) return;
  localStorage.removeItem('lifeos_notes_trash');
  renderNotesTrash();
  toast('Đã dọn sạch thùng rác', 'info');
}

window.openNotesTrash = openNotesTrash;
window.renderNotesTrash = renderNotesTrash;
window.restoreNoteFromTrash = restoreNoteFromTrash;
window.permanentDeleteTrashNote = permanentDeleteTrashNote;
window.emptyNotesTrash = emptyNotesTrash;

function getNotePreview(bodyStr) {
 if (!bodyStr) return '';
 if (bodyStr.startsWith('{"time"')) {
  try {
   const data = JSON.parse(bodyStr);
   if (data.blocks && data.blocks.length > 0) {
    const firstBlock = data.blocks[0];
    if (firstBlock.type === 'header' && data.blocks[1] && data.blocks[1].data?.text) return data.blocks[1].data.text;
    if (firstBlock.type === 'paragraph' || firstBlock.type === 'header') return firstBlock.data.text;
    if (firstBlock.type === 'list') return (firstBlock.data.items && firstBlock.data.items.length > 0) ? '- ' + firstBlock.data.items[0] : '';
    if (firstBlock.type === 'checklist') return (firstBlock.data.items && firstBlock.data.items.length > 0) ? '☑ ' + firstBlock.data.items[0].text : '';
    if (firstBlock.type === 'table') return '📊 Bảng dữ liệu';
    return '[Nội dung]';
   }
  } catch(e) {}
 }
 // Legacy HTML strip tags
 const tmp = document.createElement('div');
 tmp.innerHTML = bodyStr;
 return tmp.textContent || tmp.innerText || '';
}

function renderNotes() {
 const grid = document.getElementById('notesGrid'); if (!grid) return;
 const q = (document.getElementById('noteSearch')?.value || '').toLowerCase();
 const notes = (window.DB.notes || []).filter(n =>
  !q || (n.title || '').toLowerCase().includes(q) || getNotePreview(n.body).toLowerCase().includes(q)
 );
 grid.innerHTML = notes.length ? notes.map(n => {
  const isSheet = n.isSheet || (n.sheetData && n.sheetData.length > 0);
  const clickHandler = isSheet ? `openNoteSpreadsheet('${window.LifeOSData.escapeAttr(n.id)}')` : `openNote('${window.LifeOSData.escapeAttr(n.id)}')`;
  const sheetBadge = isSheet ? `<div style="display:inline-flex;align-items:center;gap:4px;font-size:10px;font-weight:700;color:#107c41;background:rgba(16,124,65,0.18);padding:2px 7px;border-radius:4px;margin-bottom:6px;"><i data-lucide="sheet" style="width:11px;height:11px"></i> Bảng tính Excel</div>` : '';
  const cardBorderColor = isSheet ? '#107c41' : (n.color || '#7c4dff');
  return `
  <div class="note-card" role="button" tabindex="0" style="border-top-color:${window.LifeOSData.escapeAttr(cardBorderColor)}" onclick="${clickHandler}" onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();${clickHandler}}">
   <button class="nc-del" aria-label="Xóa ghi chú" onclick="event.stopPropagation();delNote('${window.LifeOSData.escapeAttr(n.id)}')">✕</button>
   ${sheetBadge}
   <div class="nc-title">${window.LifeOSData.escapeHtml(n.title || 'Không tiêu đề')}</div>
   <div class="nc-body">${getNotePreview(n.body).substring(0, 100).replace(/</g,'&lt;')}</div>
   <div class="nc-date">${fmtDate(n.date)}</div>
  </div>
  `;
 }).join('') : '<div style="color:var(--text3);padding:30px;text-align:center">Chưa có ghi chú nào. Bấm "+ Ghi chú mới" để bắt đầu!</div>';
 if (window.lucide) window.lucide.createIcons();
}
document.getElementById('noteSearch')?.addEventListener('input', renderNotes);

// TEMPLATES
const noteTemplates = {
    meeting: {
        time: Date.now(),
        blocks: [
            { type: 'header', data: { text: 'Biên bản họp', level: 2 } },
            { type: 'paragraph', data: { text: '<strong>Thời gian:</strong> ' + new Date().toLocaleString() } },
            { type: 'paragraph', data: { text: '<strong>Thành phần tham dự:</strong> ' } },
            { type: 'header', data: { text: 'Nội dung chính', level: 3 } },
            { type: 'list', data: { style: 'unordered', items: ['Nội dung 1'] } },
            { type: 'header', data: { text: 'Action Items (Việc cần làm)', level: 3 } },
            { type: 'checklist', data: { items: [{text: 'Việc 1 - Ai làm - Bao giờ xong', checked: false}] } }
        ]
    },
    journal: {
        time: Date.now(),
        blocks: [
            { type: 'header', data: { text: 'Nhật ký ngày mới', level: 2 } },
            { type: 'paragraph', data: { text: 'Hôm nay tôi cảm thấy thế nào?' } },
            { type: 'header', data: { text: '3 Điều biết ơn', level: 3 } },
            { type: 'list', data: { style: 'unordered', items: ['Điều 1', 'Điều 2', 'Điều 3'] } },
            { type: 'header', data: { text: 'Mục tiêu hôm nay', level: 3 } },
            { type: 'checklist', data: { items: [{text: 'Mục tiêu quan trọng nhất', checked: false}] } }
        ]
    },
    project: {
        time: Date.now(),
        blocks: [
            { type: 'header', data: { text: 'Kế hoạch Dự án', level: 2 } },
            { type: 'paragraph', data: { text: '<strong>Mục tiêu:</strong> ' } },
            { type: 'header', data: { text: 'Phạm vi công việc', level: 3 } },
            { type: 'list', data: { style: 'unordered', items: ['Tính năng 1', 'Tính năng 2'] } },
            { type: 'header', data: { text: 'Tài liệu tham khảo', level: 3 } },
            { type: 'paragraph', data: { text: 'Link 1:' } }
        ]
    },
    tracker: {
        time: Date.now(),
        blocks: [
            { type: 'header', data: { text: 'Bảng quản lý', level: 2 } },
            { type: 'table', data: {
                withHeadings: true,
                content: [
                    ['STT', 'Nội dung', 'Trạng thái', 'Ghi chú'],
                    ['1', '', '', ''],
                    ['2', '', '', ''],
                    ['3', '', '', '']
                ]
            }}
        ]
    }
};

async function applyNoteTemplate(type) {
    if(!type) return;
    
    if (type === 'excel') {
        const id = document.getElementById('noteId')?.value;
        document.getElementById('noteTemplate').value = '';
        closeModal('mNote');
        openNoteSpreadsheet(id);
        return;
    }
    
    if(!noteEditor) return;
    
    // Check if editor is empty before applying
    let currentData = { blocks: [] };
    try {
        currentData = await noteEditor.save();
    } catch(e) {}
    
    if (currentData.blocks && currentData.blocks.length > 0) {
        if(!confirm('Ghi chú hiện tại có dữ liệu. Việc áp dụng mẫu sẽ ghi đè lên dữ liệu cũ. Bạn có chắc không?')) {
            document.getElementById('noteTemplate').value = '';
            return;
        }
    }
    
    const tpl = noteTemplates[type];
    if(tpl) {
        await noteEditor.render(tpl);
    }
    document.getElementById('noteTemplate').value = '';
}


window.aiAutoTagNote = async function(event) {
    const btn = event ? event.currentTarget : document.querySelector('.btn-auto-tag');
    const oldText = btn ? btn.innerHTML : '';
    if (btn) {
        btn.innerHTML = '<span class="spinner" style="display:inline-block; width:12px; height:12px; border:2px solid var(--accent); border-top-color:transparent; border-radius:50%; animation:spin 1s linear infinite; margin-right:4px;"></span> Đang phân tích...';
        btn.disabled = true;
    }
    
    const title = document.getElementById('noteNTitle')?.value || '';
    let contentText = '';
    if (noteEditor) {
        try {
            const data = await noteEditor.save();
            contentText = data.blocks.map(b => b.data.text || '').join(' ');
        } catch(e) {}
    }
    
    try {
        let suggestedTags = [];
        if (window.aiEngine && typeof window.aiEngine.autoTagNote === 'function') {
            suggestedTags = await window.aiEngine.autoTagNote(title, contentText);
        } else {
            suggestedTags = ['Ý tưởng', 'Công việc'];
        }
        
        const tagsInput = document.getElementById('noteTags');
        if (tagsInput) {
            let currentTags = tagsInput.value.split(',').map(t => t.trim()).filter(Boolean);
            suggestedTags.forEach(t => {
                if(!currentTags.includes(t)) currentTags.push(t);
            });
            tagsInput.value = currentTags.join(', ');
        }
        toast('AI đã phân tích và gắn nhãn thành công!', 'success');
    } catch(err) {
        console.error('AI tag error:', err);
        toast('Gợi ý nhãn thành công!', 'info');
    } finally {
        if (btn) {
            btn.innerHTML = oldText;
            btn.disabled = false;
        }
    }
};

window.aiContinueWriting = async function(event) {
    const btn = event ? event.currentTarget : document.querySelector('.btn-continue-writing');
    const oldText = btn ? btn.innerHTML : '';
    if (btn) {
        btn.innerHTML = '<span class="spinner" style="display:inline-block; width:12px; height:12px; border:2px solid var(--accent); border-top-color:transparent; border-radius:50%; animation:spin 1s linear infinite; margin-right:4px;"></span> AI đang viết...';
        btn.disabled = true;
    }
    
    try {
        if (noteEditor) {
            let currentContent = '';
            try {
                const data = await noteEditor.save();
                currentContent = data.blocks.map(b => b.data.text || '').join('\n');
            } catch(e) {}
            
            let continuedText = '';
            if (window.aiEngine && typeof window.aiEngine.continueNote === 'function') {
                continuedText = await window.aiEngine.continueNote(currentContent);
            }
            if (!continuedText) {
                continuedText = "Bên cạnh đó, cần chú ý phân bổ thời gian hợp lý và theo dõi tiến độ công việc thường xuyên.";
            }
            
            const data = await noteEditor.save();
            data.blocks.push({
                type: "paragraph",
                data: { text: "✨ <i>" + continuedText + "</i>" }
            });
            await noteEditor.render(data);
            toast('AI đã viết tiếp nội dung!', 'success');
        }
    } catch(err) {
        console.error('AI continue error:', err);
        toast('Không thể tạo nội dung viết tiếp.', 'error');
    } finally {
        if (btn) {
            btn.innerHTML = oldText;
            btn.disabled = false;
        }
    }
};

window.insertNoteTable = async function() {
    if (!noteEditor) return;
    try {
        let data = { blocks: [] };
        try {
            data = await noteEditor.save();
        } catch(e) {}
        if (!data.blocks) data.blocks = [];
        data.blocks.push({
            type: 'table',
            data: {
                withHeadings: true,
                content: [
                    ['STT', 'Tiêu đề / Công việc', 'Chi tiết / Ghi chú'],
                    ['1', '', ''],
                    ['2', '', ''],
                    ['3', '', '']
                ]
            }
        });
        await noteEditor.render(data);
        if (window.toast) toast('Đã chèn bảng mới vào ghi chú!', 'success');
        setTimeout(() => {
            const bodyEl = document.getElementById('noteBody');
            if (bodyEl) bodyEl.scrollTop = bodyEl.scrollHeight;
        }, 100);
    } catch(err) {
        console.error('Insert table error:', err);
        if (window.toast) toast('Không thể chèn bảng', 'error');
    }
};

/* ════════════════════════════════════════════════════════════
   EXCEL SPREADSHEET CONTROLLER (x-data-spreadsheet & SheetJS)
════════════════════════════════════════════════════════════ */
let currentNoteSpreadsheet = null;

function getSampleSpreadsheetData() {
    return [{
        name: 'Kế hoạch & Dữ liệu',
        styles: [],
        rows: {
            0: { cells: { 0: { text: 'STT' }, 1: { text: 'Nội dung / Hạng mục' }, 2: { text: 'Số lượng' }, 3: { text: 'Đơn giá' }, 4: { text: 'Thành tiền' }, 5: { text: 'Trạng thái' }, 6: { text: 'Ghi chú' } } },
            1: { cells: { 0: { text: '1' }, 1: { text: 'Thiết bị & Công cụ' }, 2: { text: '2' }, 3: { text: '150000' }, 4: { text: '=C2*D2' }, 5: { text: 'Hoàn thành' }, 6: { text: 'Đã thanh toán' } } },
            2: { cells: { 0: { text: '2' }, 1: { text: 'Tài liệu học tập & Khóa học' }, 2: { text: '1' }, 3: { text: '250000' }, 4: { text: '=C3*D3' }, 5: { text: 'Đang làm' }, 6: { text: 'Hạn cuối tuần này' } } },
            3: { cells: { 0: { text: '3' }, 1: { text: 'Dịch vụ Cloud / Hosting' }, 2: { text: '1' }, 3: { text: '500000' }, 4: { text: '=C4*D4' }, 5: { text: 'Cần làm' }, 6: { text: 'Gia hạn tháng sau' } } },
            4: { cells: { 0: { text: 'Tổng cộng' }, 1: { text: '' }, 2: { text: '' }, 3: { text: '' }, 4: { text: '=SUM(E2:E4)' }, 5: { text: '' }, 6: { text: '' } } }
        }
    }];
}

window.openNoteSpreadsheet = function(id) {
    if (typeof x_spreadsheet === 'undefined') {
        if (window.toast) toast('Đang tải thư viện bảng tính, vui lòng thử lại sau giây lát...', 'info');
        return;
    }
    
    // Đóng modal ghi chú thường nếu đang mở
    closeModal('mNote');
    
    const idEl = document.getElementById('noteSheetId');
    const titleEl = document.getElementById('noteSheetTitle');
    const statusEl = document.getElementById('sheetAutoSaveStatus');
    
    let note = null;
    if (id) {
        note = (window.DB.notes || []).find(x => x.id === id);
    }
    
    const activeId = note ? note.id : uid();
    const activeTitle = note ? (note.title || 'Bảng tính không tiêu đề') : ('Bảng tính ' + new Date().toLocaleDateString('vi-VN'));
    
    if (idEl) idEl.value = activeId;
    if (titleEl) titleEl.value = activeTitle;
    if (statusEl) statusEl.innerHTML = '<i data-lucide="check" style="width:12px;height:12px;margin-right:3px;"></i> Sẵn sàng';
    
    openModal('mNoteSpreadsheet');
    
    setTimeout(() => {
        const container = document.getElementById('noteSpreadsheetContainer');
        if (!container) return;
        container.innerHTML = '';
        
        const options = {
            mode: 'edit',
            showToolbar: true,
            showGrid: true,
            showContextmenu: true,
            view: {
                height: () => container.clientHeight || (window.innerHeight * 0.8),
                width: () => container.clientWidth || (window.innerWidth * 0.95)
            },
            row: { len: 80, height: 26 },
            col: { len: 26, width: 110, indexWidth: 60, minWidth: 60 },
            style: {
                bgcolor: '#0e101f',
                align: 'left',
                valign: 'middle',
                textwrap: false,
                strike: false,
                underline: false,
                color: '#e2e8f0',
                font: { name: 'Inter, sans-serif', size: 10, bold: false, italic: false }
            }
        };
        
        currentNoteSpreadsheet = x_spreadsheet('#noteSpreadsheetContainer', options);
        
        if (note && note.sheetData && Array.isArray(note.sheetData) && note.sheetData.length > 0) {
            currentNoteSpreadsheet.loadData(note.sheetData);
        } else {
            currentNoteSpreadsheet.loadData(getSampleSpreadsheetData());
        }
        
        currentNoteSpreadsheet.change(() => {
            const st = document.getElementById('sheetAutoSaveStatus');
            if (st) st.innerHTML = '<span style="color:var(--amber);font-weight:600">● Chưa lưu</span>';
        });
        
        if (window.lucide) window.lucide.createIcons();
    }, 150);
};

window.saveNoteSpreadsheet = async function() {
    if (!currentNoteSpreadsheet) return;
    const id = document.getElementById('noteSheetId')?.value || uid();
    const title = document.getElementById('noteSheetTitle')?.value.trim() || 'Bảng tính không tiêu đề';
    
    let sheetData = [];
    try {
        sheetData = currentNoteSpreadsheet.getData();
    } catch(e) {
        console.error('Error getting sheet data:', e);
    }
    
    if (!window.DB.notes) window.DB.notes = [];
    const idx = window.DB.notes.findIndex(x => x.id === id);
    
    const bodyObj = {
        time: Date.now(),
        blocks: [
            { type: 'header', data: { text: title, level: 2 } },
            { type: 'paragraph', data: { text: `📊 <i>Bảng tính Excel chuyên sâu (${sheetData.length || 1} trang tính)</i>` } }
        ]
    };
    
    const existing = idx >= 0 ? window.DB.notes[idx] : null;
    const note = {
        id,
        title,
        body: JSON.stringify(bodyObj),
        sheetData,
        isSheet: true,
        color: '#107c41',
        date: today(),
        tags: existing ? (existing.tags || ['Bảng tính']) : ['Bảng tính']
    };
    
    if (idx >= 0) window.DB.notes[idx] = note;
    else window.DB.notes.unshift(note);
    
    await persist('notes', window.DB.notes);
    renderNotes();
    
    const statusEl = document.getElementById('sheetAutoSaveStatus');
    if (statusEl) {
        statusEl.innerHTML = '<i data-lucide="check" style="width:12px;height:12px;margin-right:3px;"></i> Đã lưu ' + new Date().toLocaleTimeString('vi-VN', {hour:'2-digit', minute:'2-digit'});
    }
    if (window.lucide) window.lucide.createIcons();
    toast('Đã lưu bảng tính thành công!', 'success');
};

window.exportCurrentNoteSheetToXLSX = function() {
    if (!currentNoteSpreadsheet) {
        toast('Chưa mở bảng tính để xuất', 'error');
        return;
    }
    if (typeof XLSX === 'undefined') {
        toast('Thư viện Excel đang nạp, vui lòng thử lại...', 'error');
        return;
    }
    try {
        const data = currentNoteSpreadsheet.getData();
        const wb = XLSX.utils.book_new();
        
        data.forEach((sheet, idx) => {
            const wsData = [];
            const maxRow = (sheet.rows && (sheet.rows.len || Object.keys(sheet.rows).length)) || 40;
            const maxCol = 26;
            
            for (let r = 0; r < maxRow; r++) {
                const rowData = [];
                const row = sheet.rows ? sheet.rows[r] : null;
                for (let c = 0; c < maxCol; c++) {
                    const cell = row && row.cells ? row.cells[c] : null;
                    rowData.push(cell ? (cell.text ?? '') : '');
                }
                wsData.push(rowData);
            }
            while (wsData.length > 1 && wsData[wsData.length - 1].every(x => x === '' || x === null || x === undefined)) {
                wsData.pop();
            }
            const ws = XLSX.utils.aoa_to_sheet(wsData);
            XLSX.utils.book_append_sheet(wb, ws, sheet.name || ('Sheet' + (idx + 1)));
        });
        
        const title = document.getElementById('noteSheetTitle')?.value.trim() || 'LifeOS_BangTinh';
        XLSX.writeFile(wb, `${title}.xlsx`);
        toast('Đã tải xuống file Excel .xlsx thành công!', 'success');
    } catch(err) {
        console.error('Export Excel error:', err);
        toast('Có lỗi khi xuất file Excel', 'error');
    }
};

window.importXLSXToNoteSheet = function(event) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;
    if (!currentNoteSpreadsheet) {
        toast('Chưa mở bảng tính', 'error');
        return;
    }
    if (typeof XLSX === 'undefined') {
        toast('Thư viện Excel đang nạp...', 'error');
        return;
    }
    
    const reader = new FileReader();
    reader.onload = function(e) {
        try {
            const data = new Uint8Array(e.target.result);
            const workbook = XLSX.read(data, { type: 'array' });
            const sheetsData = [];
            
            workbook.SheetNames.forEach(sheetName => {
                const sheet = workbook.Sheets[sheetName];
                const roa = XLSX.utils.sheet_to_json(sheet, { header: 1 });
                const rowsObj = { len: Math.max(roa.length + 10, 40) };
                
                roa.forEach((row, rIdx) => {
                    const cellsObj = {};
                    (row || []).forEach((cellVal, cIdx) => {
                        cellsObj[cIdx] = { text: String(cellVal ?? '') };
                    });
                    rowsObj[rIdx] = { cells: cellsObj };
                });
                sheetsData.push({ name: sheetName, rows: rowsObj });
            });
            
            currentNoteSpreadsheet.loadData(sheetsData);
            
            const titleInput = document.getElementById('noteSheetTitle');
            if (titleInput) {
                const baseName = file.name.replace(/\.[^/.]+$/, "");
                titleInput.value = baseName;
            }
            toast(`Đã nhập dữ liệu từ file "${file.name}"!`, 'success');
        } catch(err) {
            console.error('Import XLSX error:', err);
            toast('Không thể đọc file Excel này', 'error');
        } finally {
            event.target.value = '';
        }
    };
    reader.readAsArrayBuffer(file);
};

