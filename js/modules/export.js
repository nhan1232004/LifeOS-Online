// module for export & import functionality

window.openSettings = function() {
    if (typeof openModal === 'function') {
        openModal('settingsModal');
    } else {
        const modal = document.getElementById('settingsModal');
        if (modal) {
            modal.style.display = 'flex';
            modal.classList.add('open');
        }
    }
};

window.closeSettings = function() {
    if (typeof closeModal === 'function') {
        closeModal('settingsModal');
    } else {
        const modal = document.getElementById('settingsModal');
        if (modal) {
            modal.classList.remove('open');
            modal.style.display = 'none';
        }
    }
};

window.exportAllData = function() {
    const data = window.DB || {};
    const jsonStr = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    
    const d = new Date();
    const dateStr = d.toISOString().split('T')[0].replace(/-/g, '');
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `lifeos_backup_${dateStr}.json`;
    a.click();
    URL.revokeObjectURL(url);
    
    if (window.toast) window.toast('Đã tải xuống bản sao lưu (JSON)', 'success');
};

window.importAllData = function(event) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async function(e) {
        try {
            const parsed = JSON.parse(e.target.result);
            if (typeof parsed !== 'object' || parsed === null) throw new Error('File không hợp lệ');

            const collections = ['events', 'todos', 'projects', 'proj_tasks', 'income', 'expense', 'notes', 'habits', 'goals', 'journal', 'vocab', 'mocktests'];
            let restoredCount = 0;

            collections.forEach(col => {
                if (Array.isArray(parsed[col])) {
                    window.DB[col] = parsed[col];
                    if (typeof persist === 'function') persist(col, window.DB[col]);
                    restoredCount++;
                }
            });

            if (restoredCount === 0) {
                toast('File không chứa dữ liệu LifeOS hợp lệ!', 'error');
                return;
            }

            if (window.renderAll) window.renderAll();
            toast('Khôi phục dữ liệu thành công!', 'success');
            closeSettings();
        } catch (err) {
            console.error('Import error:', err);
            toast('Lỗi đọc file sao lưu: ' + err.message, 'error');
        }
    };
    reader.readAsText(file);
    event.target.value = ''; // Reset input
};

window.exportFinanceCSV = function() {
    const income = (window.DB && window.DB.income) || [];
    const expense = (window.DB && window.DB.expense) || (window.DB && window.DB.expenses) || [];
    
    let csvContent = "Type,Date,Amount,Category/Source,Payment Method,Note\n";
    
    income.forEach(i => {
        csvContent += `Income,${i.date || ''},${i.amt || 0},"${i.src || ''}","", "${i.note || ''}"\n`;
    });
    
    expense.forEach(e => {
        csvContent += `Expense,${e.date || ''},${e.amt || 0},"${e.cat || ''}","${e.pay || ''}","${e.note || ''}"\n`;
    });
    
    const blob = new Blob(["\uFEFF" + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    
    const d = new Date();
    const dateStr = d.toISOString().split('T')[0].replace(/-/g, '');
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `lifeos_finance_${dateStr}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    
    if (window.toast) window.toast('Đã tải xuống thu chi (CSV)', 'success');
};

window.exportNoteMarkdown = function() {
    const idEl = document.getElementById('noteId');
    const id = idEl ? idEl.value : null;
    if (!id || !window.DB || !window.DB.notes) {
        if(window.toast) window.toast('Không tìm thấy ghi chú', 'error');
        return;
    }
    const note = window.DB.notes.find(x => x.id === id);
    if (!note) {
        if(window.toast) window.toast('Không tìm thấy ghi chú', 'error');
        return;
    }
    let md = '# ' + (note.title || 'Untitled') + '\n\n';
    
    try {
        const bodyObj = JSON.parse(note.body);
        if (bodyObj.blocks) {
            bodyObj.blocks.forEach(b => {
                if (b.type === 'paragraph') md += b.data.text + '\n\n';
                else if (b.type === 'header') md += '#'.repeat(b.data.level) + ' ' + b.data.text + '\n\n';
                else if (b.type === 'list') {
                    b.data.items.forEach(i => {
                        md += (b.data.style === 'ordered' ? '1. ' : '- ') + i + '\n';
                    });
                    md += '\n';
                }
                else if (b.type === 'checklist') {
                    b.data.items.forEach(i => {
                        md += (i.checked ? '- [x] ' : '- [ ] ') + i.text + '\n';
                    });
                    md += '\n';
                }
                else if (b.type === 'code') md += '```\n' + b.data.code + '\n```\n\n';
                else if (b.type === 'quote') md += '> ' + b.data.text + '\n\n';
                else if (b.type === 'delimiter') md += '---\n\n';
            });
        }
    } catch (e) {
        const tmp = document.createElement('div');
        tmp.innerHTML = note.body || '';
        md += tmp.innerText || tmp.textContent;
    }
    
    const blob = new Blob([md], { type: 'text/markdown;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = (note.title || 'Note') + '.md';
    a.click();
    URL.revokeObjectURL(url);
    if(window.toast) window.toast('Đã tải xuống Markdown', 'success');
};
