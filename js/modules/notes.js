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
    code: CodeTool
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
    blList.innerHTML = backlinks.map(n => `<a href="javascript:void(0)" onclick="closeModal('mNote'); setTimeout(()=>openNote('${n.id}'), 300)" style="color:var(--text-accent); text-decoration:none; font-size:13px"><i data-lucide="file-text" style="width:12px;height:12px"></i> ${n.title || 'Không tiêu đề'}</a>`).join('');
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

async function delNote(id) {
  const t = (window.DB.notes || []).find(x => x.id === id);
  if (!t) return;
 window.DB.notes = (window.DB.notes || []).filter(x => x.id !== id);
 await persist('notes', window.DB.notes);
 renderNotes();
 undoManager.execute('notes', t, renderNotes);
}

function getNotePreview(bodyStr) {
 if (!bodyStr) return '';
 if (bodyStr.startsWith('{"time"')) {
  try {
   const data = JSON.parse(bodyStr);
   if (data.blocks && data.blocks.length > 0) {
    const firstBlock = data.blocks[0];
    if (firstBlock.type === 'paragraph' || firstBlock.type === 'header') return firstBlock.data.text;
    if (firstBlock.type === 'list') return (firstBlock.data.items && firstBlock.data.items.length > 0) ? '- ' + firstBlock.data.items[0] : '';
    if (firstBlock.type === 'checklist') return (firstBlock.data.items && firstBlock.data.items.length > 0) ? '☑ ' + firstBlock.data.items[0].text : '';
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
 grid.innerHTML = notes.length ? notes.map(n => `
  <div class="note-card" style="border-top-color:${n.color}" onclick="openNote('${n.id}')">
   <button class="nc-del" onclick="event.stopPropagation();delNote('${n.id}')">✕</button>
   <div class="nc-title">${n.title || 'Không tiêu đề'}</div>
   <div class="nc-body">${getNotePreview(n.body).substring(0, 100).replace(/</g,'&lt;')}</div>
   <div class="nc-date">${fmtDate(n.date)}</div>
  </div>
 `).join('') : '<div style="color:var(--text3);padding:30px;text-align:center">Chưa có ghi chú nào. Bấm "+ Ghi chú mới" để bắt đầu!</div>';
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
    }
};

async function applyNoteTemplate(type) {
    if(!type || !noteEditor) return;
    
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


window.aiAutoTagNote = async function() {
    const btn = event.currentTarget;
    const oldText = btn.innerHTML;
    btn.innerHTML = '<span class="spinner" style="display:inline-block; width:12px; height:12px; border:2px solid var(--primary); border-top-color:transparent; border-radius:50%; animation:spin 1s linear infinite; margin-right:4px;"></span> Đang phân tích...';
    btn.disabled = true;
    
    // Get text to analyze
    const title = document.getElementById('noteNTitle').value;
    let contentText = '';
    if (noteEditor) {
        try {
            const data = await noteEditor.save();
            contentText = data.blocks.map(b => b.data.text || '').join(' ');
        } catch(e) {}
    }
    
    const textToAnalyze = (title + ' ' + contentText).toLowerCase();
    
    // Simulate AI delay
    setTimeout(() => {
        let suggestedTags = [];
        if(textToAnalyze.includes('họp') || textToAnalyze.includes('meeting')) suggestedTags.push('Họp hành');
        if(textToAnalyze.includes('ý tưởng') || textToAnalyze.includes('idea')) suggestedTags.push('Ý tưởng');
        if(textToAnalyze.includes('kế hoạch') || textToAnalyze.includes('plan')) suggestedTags.push('Kế hoạch');
        if(textToAnalyze.includes('bug') || textToAnalyze.includes('lỗi')) suggestedTags.push('Lỗi/Bug');
        if(textToAnalyze.includes('mua') || textToAnalyze.includes('tiền')) suggestedTags.push('Chi tiêu');
        
        if(suggestedTags.length === 0) {
            suggestedTags.push('Ghi chú chung');
        }
        
        const tagsInput = document.getElementById('noteTags');
        let currentTags = tagsInput.value.split(',').map(t => t.trim()).filter(Boolean);
        suggestedTags.forEach(t => {
            if(!currentTags.includes(t)) currentTags.push(t);
        });
        
        tagsInput.value = currentTags.join(', ');
        
        btn.innerHTML = oldText;
        btn.disabled = false;
        if(typeof toast === 'function') toast('AI đã phân tích và gắn nhãn thành công!', 'success');
    }, 1500);
};


window.aiContinueWriting = async function() {
    const btn = event.currentTarget;
    const oldText = btn.innerHTML;
    btn.innerHTML = '<span class="spinner" style="display:inline-block; width:12px; height:12px; border:2px solid var(--primary); border-top-color:transparent; border-radius:50%; animation:spin 1s linear infinite; margin-right:4px;"></span> Đang viết...';
    btn.disabled = true;
    
    setTimeout(async () => {
        if(noteEditor) {
            try {
                // Simulate AI generated text
                const aiTexts = [
                    "Bên cạnh đó, cần chú ý phân bổ thời gian hợp lý cho từng công việc.",
                    "Một điểm quan trọng khác là cần theo dõi tiến độ thường xuyên để không bị trễ hạn.",
                    "Ngoài ra, đừng quên dành thời gian nghỉ ngơi để duy trì năng lượng làm việc.",
                    "Giải pháp tối ưu nhất lúc này là chia nhỏ mục tiêu và hoàn thành từng bước một."
                ];
                const randomText = aiTexts[Math.floor(Math.random() * aiTexts.length)];
                
                // Add a new block to Editor.js
                const data = await noteEditor.save();
                data.blocks.push({
                    type: "paragraph",
                    data: {
                        text: "✨ <i>" + randomText + "</i>"
                    }
                });
                await noteEditor.render(data);
                
                if(typeof toast === 'function') toast('AI đã viết tiếp nội dung', 'success');
            } catch(e) {
                console.error("AI write failed", e);
            }
        }
        
        btn.innerHTML = oldText;
        btn.disabled = false;
    }, 1500);
};
