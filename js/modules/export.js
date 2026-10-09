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
            if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) throw new Error('File không hợp lệ');

            const collections = ['events', 'todos', 'projects', 'proj_tasks', 'income', 'expense', 'notes', 'habits', 'goals', 'journal', 'vocab', 'mocktests'];
            const pending = collections.filter(col => Array.isArray(parsed[col]));
            const restoredCount = pending.length;

            if (restoredCount === 0) {
                toast('File không chứa dữ liệu LifeOS hợp lệ!', 'error');
                return;
            }

            // Persist each collection before changing in-memory state. This avoids a
            // partial import that looks successful when a network write fails.
            const nextDb = { ...(window.DB || {}) };
            pending.forEach(col => { nextDb[col] = parsed[col].filter(x => x && typeof x === 'object'); });
            for (const col of pending) await window.persist(col, nextDb[col]);
            window.DB = nextDb;
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

window.openExportFinanceModal = function() {
    const modal = document.getElementById('mExportFinance');
    if (modal) {
        // Pre-fill dates based on current view
        const now = new Date();
        const y = now.getFullYear();
        const m = String(now.getMonth() + 1).padStart(2, '0');
        const d = String(now.getDate()).padStart(2, '0');
        const todayStr = `${y}-${m}-${d}`;
        
        const firstDayOfMonth = `${y}-${m}-01`;
        const lastDayOfMonth = new Date(y, now.getMonth() + 1, 0).toISOString().split('T')[0];

        const customStart = document.getElementById('expFinCustomStart');
        const customEnd = document.getElementById('expFinCustomEnd');
        if (customStart && !customStart.value) customStart.value = firstDayOfMonth;
        if (customEnd && !customEnd.value) customEnd.value = lastDayOfMonth;

        // If currently on finance or stats page, check its interval
        let activeInterval = null;
        if (typeof getFinDateInterval === 'function' && document.getElementById('p-finance')?.classList.contains('active')) {
            try { activeInterval = getFinDateInterval(); } catch(e) {}
        } else if (typeof getStatsDateInterval === 'function') {
            try { activeInterval = getStatsDateInterval(); } catch(e) {}
        }
        if (activeInterval) {
            const curLbl = document.getElementById('expFinCurLabel');
            if (curLbl) curLbl.textContent = `(${activeInterval.label})`;
        }

        modal.style.display = 'flex';
        modal.classList.add('open');
        if (window.lucide) window.lucide.createIcons();
    }
};

window.closeExportFinanceModal = function() {
    const modal = document.getElementById('mExportFinance');
    if (modal) {
        modal.classList.remove('open');
        modal.style.display = 'none';
    }
};

window.triggerFinanceExportFromModal = function() {
    const selectedRadio = document.querySelector('input[name="expFinRange"]:checked');
    const rangeType = selectedRadio ? selectedRadio.value : 'current';
    
    let start = '';
    let end = '';
    let label = '';
    const now = new Date();
    const y = now.getFullYear();
    const m = now.getMonth();

    const toLocalStr = (d) => {
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    if (rangeType === 'current') {
        let interval = null;
        if (typeof getFinDateInterval === 'function' && document.getElementById('p-finance')?.classList.contains('active')) {
            try { interval = getFinDateInterval(); } catch(e) {}
        } else if (typeof getStatsDateInterval === 'function') {
            try { interval = getStatsDateInterval(); } catch(e) {}
        }
        if (interval) {
            start = interval.start;
            end = interval.end;
            label = interval.label;
        } else {
            const fm = (document.getElementById('finMonth')?.value) || toLocalStr(now).substring(0, 7);
            const [year, month] = fm.split('-').map(Number);
            start = `${fm}-01`;
            const lastD = new Date(year, month, 0).getDate();
            end = `${fm}-${String(lastD).padStart(2, '0')}`;
            label = `Tháng ${month}/${year}`;
        }
    } else if (rangeType === 'thisMonth') {
        start = toLocalStr(new Date(y, m, 1));
        end = toLocalStr(new Date(y, m + 1, 0));
        label = `Tháng ${m + 1}/${y}`;
    } else if (rangeType === 'lastMonth') {
        start = toLocalStr(new Date(y, m - 1, 1));
        end = toLocalStr(new Date(y, m, 0));
        const prevM = new Date(y, m - 1, 1);
        label = `Tháng ${prevM.getMonth() + 1}/${prevM.getFullYear()}`;
    } else if (rangeType === 'last3months') {
        start = toLocalStr(new Date(y, m - 2, 1));
        end = toLocalStr(new Date(y, m + 1, 0));
        label = '3 tháng qua (Quý)';
    } else if (rangeType === 'thisYear') {
        start = `${y}-01-01`;
        end = `${y}-12-31`;
        label = `Năm ${y}`;
    } else if (rangeType === 'all') {
        start = '1970-01-01';
        end = '2099-12-31';
        label = 'Toàn bộ thời gian';
    } else if (rangeType === 'custom') {
        start = document.getElementById('expFinCustomStart')?.value || '';
        end = document.getElementById('expFinCustomEnd')?.value || '';
        label = `Tùy chọn (${start} đến ${end})`;
    }

    closeExportFinanceModal();
    exportFinanceExcel({ start, end, label });
};

window.exportFinanceExcel = function(opts = {}) {
    const income = (window.DB && window.DB.income) || [];
    const expense = (window.DB && window.DB.expense) || (window.DB && window.DB.expenses) || [];

    let start = opts.start || '';
    let end = opts.end || '';
    let label = opts.label || '';

    // If no start/end provided, fallback to current context
    if (!start && !end) {
        let interval = null;
        if (typeof getFinDateInterval === 'function' && document.getElementById('p-finance')?.classList.contains('active')) {
            try { interval = getFinDateInterval(); } catch(e) {}
        } else if (typeof getStatsDateInterval === 'function') {
            try { interval = getStatsDateInterval(); } catch(e) {}
        }
        if (interval) {
            start = interval.start;
            end = interval.end;
            label = interval.label;
        } else {
            const fm = (document.getElementById('finMonth')?.value) || new Date().toISOString().substring(0, 7);
            const [y, m] = fm.split('-').map(Number);
            start = `${fm}-01`;
            const lastDay = new Date(y, m, 0).getDate();
            end = `${fm}-${String(lastDay).padStart(2, '0')}`;
            label = `Tháng ${m}/${y}`;
        }
    }

    // Filter transactions strictly
    const filteredInc = income.filter(x => (!start || (x.date || '') >= start) && (!end || (x.date || '') <= end));
    const filteredExp = expense.filter(x => (!start || (x.date || '') >= start) && (!end || (x.date || '') <= end));

    // Sort by date descending
    filteredInc.sort((a, b) => (b.date || '').localeCompare(a.date || ''));
    filteredExp.sort((a, b) => (b.date || '').localeCompare(a.date || ''));

    const totInc = filteredInc.reduce((sum, x) => sum + (Number(x.amt) || 0), 0);
    const totExp = filteredExp.reduce((sum, x) => sum + (Number(x.amt) || 0), 0);
    const netBal = totInc - totExp;
    const savRate = totInc > 0 ? ((netBal / totInc) * 100).toFixed(1) : (netBal < 0 ? -100 : 0);

    // Categories breakdown
    const catMap = {};
    filteredExp.forEach(x => {
        const c = x.cat || 'Khác';
        catMap[c] = (catMap[c] || 0) + (Number(x.amt) || 0);
    });
    const sortedCats = Object.entries(catMap).sort((a, b) => b[1] - a[1]);

    // Income sources breakdown
    const srcMap = {};
    filteredInc.forEach(x => {
        const s = x.src || 'Khác';
        srcMap[s] = (srcMap[s] || 0) + (Number(x.amt) || 0);
    });
    const sortedSrcs = Object.entries(srcMap).sort((a, b) => b[1] - a[1]);

    // Combined transactions list
    const allTxs = [
        ...filteredInc.map(i => ({ ...i, txType: 'Thu nhập', typeCode: 'inc', catOrSrc: i.src || 'Khác', payMethod: '—' })),
        ...filteredExp.map(e => ({ ...e, txType: 'Chi tiêu', typeCode: 'exp', catOrSrc: e.cat || 'Khác', payMethod: e.pay || 'Tiền mặt' }))
    ].sort((a, b) => (b.date || '').localeCompare(a.date || ''));

    // Financial health assessment
    let healthText = 'Bình thường';
    let healthColor = '#4f46e5';
    if (totInc === 0 && totExp === 0) {
        healthText = 'Chưa có phát sinh giao dịch';
        healthColor = '#64748b';
    } else if (netBal < 0) {
        healthText = 'Bội chi (Cảnh báo: Chi tiêu vượt Thu nhập)';
        healthColor = '#dc2626';
    } else if (parseFloat(savRate) >= 30) {
        healthText = 'Rất tốt (Tỷ lệ tiết kiệm ≥ 30% thu nhập)';
        healthColor = '#059669';
    } else if (parseFloat(savRate) >= 15) {
        healthText = 'Ổn định (Tỷ lệ tiết kiệm 15% – 30%)';
        healthColor = '#2563eb';
    } else {
        healthText = 'Cần cải thiện (Tỷ lệ tiết kiệm < 15%)';
        healthColor = '#d97706';
    }

    // Visual ASCII/Unicode Data Bar representation for Excel
    const makeBar = (pct, maxLen = 16) => {
        const filled = Math.round((pct / 100) * maxLen);
        return '█'.repeat(Math.max(0, Math.min(maxLen, filled))) + '░'.repeat(Math.max(0, maxLen - filled));
    };

    const fmtVnDate = d => {
        if (!d) return '—';
        const parts = d.split('-');
        return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : d;
    };

    const escapeXml = s => String(s ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    const nowStr = new Date().toLocaleString('vi-VN');

    // Build the rich HTML Excel spreadsheet
    let html = `
<html xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:x="urn:schemas-microsoft-com:office:excel"
      xmlns="http://www.w3.org/TR/REC-html40">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<!--[if gte mso 9]>
<xml>
 <x:ExcelWorkbook>
  <x:ExcelWorksheets>
   <x:ExcelWorksheet>
    <x:Name>Bao Cao Thu Chi</x:Name>
    <x:WorksheetOptions>
     <x:DisplayGridlines/>
     <x:Print>
      <x:ValidPrinterInfo/>
     </x:Print>
    </x:WorksheetOptions>
   </x:ExcelWorksheet>
  </x:ExcelWorksheets>
 </x:ExcelWorkbook>
</xml>
<![endif]-->
<style>
  body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 10pt; color: #1e293b; background: #ffffff; }
  table { border-collapse: collapse; table-layout: fixed; width: 100%; margin-bottom: 24px; }
  th, td { border: 1px solid #cbd5e1; padding: 7px 10px; vertical-align: middle; }
  .title-banner { background-color: #1e1b4b; color: #ffffff; font-size: 16pt; font-weight: bold; text-align: center; height: 44px; border: 1px solid #1e1b4b; }
  .sub-banner { background-color: #312e81; color: #c7d2fe; font-size: 10pt; text-align: center; height: 26px; border: 1px solid #312e81; }
  .info-bar { background-color: #e0e7ff; color: #3730a3; font-size: 9.5pt; font-weight: bold; text-align: left; padding-left: 12px; }
  
  .sec-hdr { background-color: #0f172a; color: #ffffff; font-size: 11pt; font-weight: bold; text-align: left; padding: 8px 12px; height: 32px; border: 1px solid #0f172a; }
  
  .kpi-box-title { font-size: 9pt; font-weight: bold; text-align: center; text-transform: uppercase; color: #475569; background-color: #f8fafc; height: 24px; }
  .kpi-box-val-inc { font-size: 15pt; font-weight: bold; text-align: center; color: #047857; background-color: #ecfdf5; height: 36px; }
  .kpi-box-val-exp { font-size: 15pt; font-weight: bold; text-align: center; color: #b91c1c; background-color: #fef2f2; height: 36px; }
  .kpi-box-val-bal { font-size: 15pt; font-weight: bold; text-align: center; color: ${netBal >= 0 ? '#1d4ed8' : '#b91c1c'}; background-color: #eff6ff; height: 36px; }
  .kpi-box-val-rate { font-size: 15pt; font-weight: bold; text-align: center; color: #6d28d9; background-color: #f5f3ff; height: 36px; }
  .kpi-sub { font-size: 8.5pt; color: #64748b; text-align: center; background-color: #f8fafc; height: 20px; }
  
  .tbl-hdr { background-color: #1e293b; color: #ffffff; font-weight: bold; font-size: 9.5pt; text-align: center; height: 28px; }
  .tbl-hdr-inc { background-color: #065f46; color: #ffffff; font-weight: bold; font-size: 9.5pt; text-align: center; height: 28px; }
  .tbl-hdr-exp { background-color: #991b1b; color: #ffffff; font-weight: bold; font-size: 9.5pt; text-align: center; height: 28px; }
  
  .curr-inc { mso-number-format: '#,##0\\ "₫"'; text-align: right; color: #047857; font-weight: bold; }
  .curr-exp { mso-number-format: '#,##0\\ "₫"'; text-align: right; color: #b91c1c; font-weight: bold; }
  .curr-bal { mso-number-format: '#,##0\\ "₫"'; text-align: right; color: #1d4ed8; font-weight: bold; }
  
  .text-c { text-align: center; }
  .text-l { text-align: left; }
  .text-r { text-align: right; }
  
  .tag-inc { background-color: #d1fae5; color: #065f46; font-weight: bold; text-align: center; }
  .tag-exp { background-color: #fee2e2; color: #991b1b; font-weight: bold; text-align: center; }
  
  .row-total { background-color: #f1f5f9; font-weight: bold; font-size: 10pt; height: 30px; }
  .data-bar { font-family: 'Consolas', 'Courier New', monospace; color: #4338ca; font-weight: bold; font-size: 9.5pt; }
</style>
</head>
<body>

<!-- BANNER -->
<table>
 <tr>
  <td colspan="7" class="title-banner">💎 BÁO CÁO TỔNG HỢP THU CHI &amp; DÒNG TIỀN LIFEOS</td>
 </tr>
 <tr>
  <td colspan="7" class="sub-banner">Khoảng thời gian: ${escapeXml(label || (start + ' đến ' + end))} | Ngày xuất báo cáo: ${escapeXml(nowStr)}</td>
 </tr>
 <tr>
  <td colspan="4" class="info-bar">Từ ngày: <b>${escapeXml(fmtVnDate(start) || 'Toàn thời gian')}</b> &nbsp;→&nbsp; Đến ngày: <b>${escapeXml(fmtVnDate(end) || 'Hiện tại')}</b></td>
  <td colspan="3" class="info-bar" style="text-align:right;">Sức khỏe tài chính: <b style="color:${healthColor};">${escapeXml(healthText)}</b></td>
 </tr>
</table>

<!-- PHẦN 1: TỔNG QUAN CHỈ SỐ KPI -->
<table>
 <tr>
  <td colspan="7" class="sec-hdr">1. TỔNG QUAN CHỈ SỐ TÀI CHÍNH (EXECUTIVE FINANCIAL KPIS)</td>
 </tr>
 <tr>
  <td colspan="2" class="kpi-box-title">💰 TỔNG THU NHẬP</td>
  <td colspan="2" class="kpi-box-title">💸 TỔNG CHI TIÊU</td>
  <td colspan="2" class="kpi-box-title">💎 TIẾT KIỆM ĐƯỢC (SỐ DƯ RÒNG)</td>
  <td class="kpi-box-title">📈 TỶ LỆ TIẾT KIỆM</td>
 </tr>
 <tr>
  <td colspan="2" class="kpi-box-val-inc">${totInc.toLocaleString('vi-VN')} ₫</td>
  <td colspan="2" class="kpi-box-val-exp">${totExp.toLocaleString('vi-VN')} ₫</td>
  <td colspan="2" class="kpi-box-val-bal">${netBal.toLocaleString('vi-VN')} ₫</td>
  <td class="kpi-box-val-rate">${savRate}%</td>
 </tr>
 <tr>
  <td colspan="2" class="kpi-sub">${filteredInc.length} giao dịch thu nhập</td>
  <td colspan="2" class="kpi-sub">${filteredExp.length} giao dịch chi tiêu</td>
  <td colspan="2" class="kpi-sub">${netBal >= 0 ? 'Thặng dư tài chính tích cực' : 'Thâm hụt dòng tiền trong kỳ'}</td>
  <td class="kpi-sub">Mục tiêu an toàn: ≥ 20.0%</td>
 </tr>
</table>

<!-- PHẦN 2: CƠ CẤU CHI TIÊU & BIỂU ĐỒ THANH TIẾN ĐỘ -->
<table>
 <tr>
  <td colspan="7" class="sec-hdr">2. PHÂN TÍCH CƠ CẤU CHI TIÊU &amp; BIỂU ĐỒ TRỰC QUAN (EXPENSE BREAKDOWN &amp; CHART)</td>
 </tr>
 <tr>
  <th style="width:50px;" class="tbl-hdr-exp">STT</th>
  <th style="width:160px;" class="tbl-hdr-exp">Danh mục chi tiêu</th>
  <th style="width:140px;" class="tbl-hdr-exp">Số tiền (₫)</th>
  <th style="width:90px;" class="tbl-hdr-exp">Tỷ trọng (%)</th>
  <th colspan="2" style="width:280px;" class="tbl-hdr-exp">Biểu đồ tỷ trọng (Data Bar Chart)</th>
  <th style="width:180px;" class="tbl-hdr-exp">Ghi chú phân bổ</th>
 </tr>
`;

    if (sortedCats.length === 0) {
        html += `<tr><td colspan="7" class="text-c" style="padding:14px;color:#64748b;">Chưa có dữ liệu chi tiêu trong khoảng thời gian này</td></tr>`;
    } else {
        sortedCats.forEach(([cat, amt], idx) => {
            const pct = totExp > 0 ? (amt / totExp) * 100 : 0;
            const bar = makeBar(pct);
            html += `
 <tr>
  <td class="text-c">${idx + 1}</td>
  <td class="text-l"><b>${escapeXml(cat)}</b></td>
  <td class="curr-exp">${amt.toLocaleString('vi-VN')} ₫</td>
  <td class="text-r"><b>${pct.toFixed(1)}%</b></td>
  <td colspan="2" class="data-bar">${bar} ${pct.toFixed(1)}%</td>
  <td class="text-l" style="color:#64748b;font-size:8.5pt;">${pct >= 30 ? '⚠️ Chiếm tỷ trọng lớn nhất' : (pct >= 15 ? 'Chi tiêu đáng kể' : 'Chi tiêu nhỏ lẻ')}</td>
 </tr>`;
        });
        html += `
 <tr class="row-total">
  <td colspan="2" class="text-c">TỔNG CỘNG CHI TIÊU</td>
  <td class="curr-exp">${totExp.toLocaleString('vi-VN')} ₫</td>
  <td class="text-r">100.0%</td>
  <td colspan="3" class="text-l" style="color:#64748b;">Tổng số danh mục: ${sortedCats.length}</td>
 </tr>`;
    }

    html += `
</table>

<!-- PHẦN 3: CƠ CẤU NGUỒN THU NHẬP -->
<table>
 <tr>
  <td colspan="7" class="sec-hdr">3. PHÂN TÍCH NGUỒN THU NHẬP (INCOME SOURCES)</td>
 </tr>
 <tr>
  <th style="width:50px;" class="tbl-hdr-inc">STT</th>
  <th style="width:160px;" class="tbl-hdr-inc">Nguồn thu nhập</th>
  <th style="width:140px;" class="tbl-hdr-inc">Số tiền (₫)</th>
  <th style="width:90px;" class="tbl-hdr-inc">Tỷ trọng (%)</th>
  <th colspan="2" style="width:280px;" class="tbl-hdr-inc">Biểu đồ tỷ trọng (Data Bar Chart)</th>
  <th style="width:180px;" class="tbl-hdr-inc">Đánh giá</th>
 </tr>
`;

    if (sortedSrcs.length === 0) {
        html += `<tr><td colspan="7" class="text-c" style="padding:14px;color:#64748b;">Chưa có dữ liệu thu nhập trong khoảng thời gian này</td></tr>`;
    } else {
        sortedSrcs.forEach(([src, amt], idx) => {
            const pct = totInc > 0 ? (amt / totInc) * 100 : 0;
            const bar = makeBar(pct);
            html += `
 <tr>
  <td class="text-c">${idx + 1}</td>
  <td class="text-l"><b>${escapeXml(src)}</b></td>
  <td class="curr-inc">+${amt.toLocaleString('vi-VN')} ₫</td>
  <td class="text-r"><b>${pct.toFixed(1)}%</b></td>
  <td colspan="2" class="data-bar" style="color:#059669;">${bar} ${pct.toFixed(1)}%</td>
  <td class="text-l" style="color:#64748b;font-size:8.5pt;">${pct >= 50 ? '🌟 Nguồn thu nhập chủ lực' : 'Nguồn thu bổ sung'}</td>
 </tr>`;
        });
        html += `
 <tr class="row-total">
  <td colspan="2" class="text-c">TỔNG CỘNG THU NHẬP</td>
  <td class="curr-inc">+${totInc.toLocaleString('vi-VN')} ₫</td>
  <td class="text-r">100.0%</td>
  <td colspan="3" class="text-l" style="color:#64748b;">Tổng số nguồn thu: ${sortedSrcs.length}</td>
 </tr>`;
    }

    html += `
</table>

<!-- PHẦN 4: CHI TIẾT TOÀN BỘ GIAO DỊCH -->
<table>
 <tr>
  <td colspan="7" class="sec-hdr">4. NHẬT KÝ CHI TIẾT TOÀN BỘ GIAO DỊCH (${allTxs.length} GIAO DỊCH)</td>
 </tr>
 <tr>
  <th style="width:45px;" class="tbl-hdr">STT</th>
  <th style="width:95px;" class="tbl-hdr">Ngày GD</th>
  <th style="width:90px;" class="tbl-hdr">Phân loại</th>
  <th style="width:150px;" class="tbl-hdr">Danh mục / Nguồn</th>
  <th style="width:120px;" class="tbl-hdr">Phương thức</th>
  <th style="width:140px;" class="tbl-hdr">Số tiền (₫)</th>
  <th class="tbl-hdr">Ghi chú / Chi tiết</th>
 </tr>
`;

    if (allTxs.length === 0) {
        html += `<tr><td colspan="7" class="text-c" style="padding:16px;color:#64748b;">Không có giao dịch nào trong khoảng thời gian đã chọn</td></tr>`;
    } else {
        allTxs.forEach((tx, idx) => {
            const isInc = tx.typeCode === 'inc';
            html += `
 <tr>
  <td class="text-c" style="color:#64748b;">${idx + 1}</td>
  <td class="text-c">${escapeXml(fmtVnDate(tx.date))}</td>
  <td class="${isInc ? 'tag-inc' : 'tag-exp'}">${isInc ? '＋ Thu nhập' : '－ Chi tiêu'}</td>
  <td class="text-l"><b>${escapeXml(tx.catOrSrc)}</b></td>
  <td class="text-c" style="color:#475569;">${escapeXml(tx.payMethod)}</td>
  <td class="${isInc ? 'curr-inc' : 'curr-exp'}">${isInc ? '+' : '−'}${Number(tx.amt || 0).toLocaleString('vi-VN')} ₫</td>
  <td class="text-l" style="color:#475569;">${escapeXml(tx.note || '—')}</td>
 </tr>`;
        });
        html += `
 <tr class="row-total">
  <td colspan="5" class="text-c">TỔNG KẾT: TỔNG THU (+${totInc.toLocaleString('vi-VN')} ₫) — TỔNG CHI (−${totExp.toLocaleString('vi-VN')} ₫)</td>
  <td class="${netBal >= 0 ? 'curr-bal' : 'curr-exp'}">${(netBal >= 0 ? '+' : '') + netBal.toLocaleString('vi-VN')} ₫</td>
  <td class="text-l"><b>Số dư ròng tiết kiệm được (${savRate}%)</b></td>
 </tr>`;
    }

    html += `
</table>

<br>
<table style="border:none;">
 <tr>
  <td colspan="7" style="border:none; text-align:right; font-style:italic; color:#64748b; font-size:8.5pt;">
   Báo cáo được trích xuất tự động từ Hệ thống Quản trị Cá nhân LifeOS • Định dạng bảng tính Microsoft Excel (.xls)
  </td>
 </tr>
</table>

</body>
</html>
`;

    // Download as .xls
    const blob = new Blob(['\ufeff' + html], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const dStr = (start || 'all') + '_' + (end || 'all');
    const safeFilename = `LifeOS_ThuChi_${dStr.replace(/[^a-zA-Z0-9_-]/g, '_')}.xls`;

    const a = document.createElement('a');
    a.href = url;
    a.download = safeFilename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    if (window.toast) window.toast(`Đã tải xuống báo cáo Excel: ${safeFilename}`, 'success');
};

window.exportFinanceCSV = function() {
    const income = (window.DB && window.DB.income) || [];
    const expense = (window.DB && window.DB.expense) || (window.DB && window.DB.expenses) || [];
    
    const csv = window.LifeOSData?.csvCell || (value => `"${String(value ?? '').replace(/"/g, '""')}"`);
    let csvContent = "Type,Date,Amount,Category/Source,Payment Method,Note\n";
    
    income.forEach(i => {
        csvContent += [csv('Income'), csv(i.date), csv(Number(i.amt) || 0), csv(i.src), csv(''), csv(i.note)].join(',') + '\n';
    });
    
    expense.forEach(e => {
        csvContent += [csv('Expense'), csv(e.date), csv(Number(e.amt) || 0), csv(e.cat), csv(e.pay), csv(e.note)].join(',') + '\n';
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
                else if (b.type === 'table' && b.data && b.data.content && b.data.content.length > 0) {
                    const rows = b.data.content;
                    rows.forEach((row, rIdx) => {
                        md += '| ' + row.map(cell => (cell || '').replace(/\|/g, '\\|').replace(/\n/g, ' ')).join(' | ') + ' |\n';
                        if (rIdx === 0 && b.data.withHeadings) {
                            md += '| ' + row.map(() => '---').join(' | ') + ' |\n';
                        }
                    });
                    md += '\n';
                }
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
