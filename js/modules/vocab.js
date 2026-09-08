/* ════════════════════════════════════════════════════════════
   VOCAB FLASHCARDS & DOCUMENT EXTRACTOR (PDF / DOCX / TEXT)
   Specifically optimized for IELTS books, vocabulary lists, and reading passages
════════════════════════════════════════════════════════════ */

// ── Text-to-Speech Pronunciation ──
window.speakWord = function(word) {
 if (!word) return;
 if (!('speechSynthesis' in window)) {
  toast('Trình duyệt không hỗ trợ phát âm tự động', 'info');
  return;
 }
 try {
  window.speechSynthesis.cancel();
  // Strip trailing notes or parentheticals before pronouncing
  const cleanWord = word.replace(/\s*\([^)]*\)/g, '').replace(/\s*\+.*$/g, '').trim();
  const utter = new SpeechSynthesisUtterance(cleanWord);
  utter.lang = 'en-US';
  utter.rate = 0.88;
  window.speechSynthesis.speak(utter);
 } catch (e) {
  console.warn('[LifeOS Vocab] Speech synthesis error:', e);
 }
};

// ── State for Import Modal ──
let currentImportFile = null;
let currentPdfDoc = null;
let importedVocabList = [];

// ── Modal & Tab Handlers ──
window.openImportVocabModal = function() {
 currentImportFile = null;
 currentPdfDoc = null;
 importedVocabList = [];
 const dropName = document.getElementById('vocabFileSelectedName');
 if (dropName) { dropName.textContent = ''; dropName.style.display = 'none'; }
 const pasteArea = document.getElementById('vocabPasteText');
 if (pasteArea) pasteArea.value = '';
 const fileInp = document.getElementById('vocabFileInput');
 if (fileInp) fileInp.value = '';
 const pdfRow = document.getElementById('viPdfPageRangeRow');
 if (pdfRow) pdfRow.style.display = 'none';
 
 document.getElementById('viPreviewWrap').style.display = 'none';
 document.getElementById('viLoading').style.display = 'none';
 document.getElementById('btnConfirmImport').style.display = 'none';
 
 switchVocabImportTab('file');
 openModal('mImportVocab');
 if (window.lucide) window.lucide.createIcons();
};

window.switchVocabImportTab = function(tab) {
 const tabFile = document.getElementById('viTabFile');
 const tabPaste = document.getElementById('viTabPaste');
 const secFile = document.getElementById('viSecFile');
 const secPaste = document.getElementById('viSecPaste');
 
 if (tab === 'file') {
  if (tabFile) tabFile.classList.add('active');
  if (tabPaste) tabPaste.classList.remove('active');
  if (secFile) secFile.style.display = 'block';
  if (secPaste) secPaste.style.display = 'none';
 } else {
  if (tabPaste) tabPaste.classList.add('active');
  if (tabFile) tabFile.classList.remove('active');
  if (secPaste) secPaste.style.display = 'block';
  if (secFile) secFile.style.display = 'none';
 }
};

window.togglePdfScanAll = function(checked) {
 const startInp = document.getElementById('viPdfPageStart');
 const endInp = document.getElementById('viPdfPageEnd');
 if (startInp && endInp && currentPdfDoc) {
  if (checked) {
   startInp.value = 1;
   endInp.value = currentPdfDoc.numPages;
   startInp.disabled = true;
   endInp.disabled = true;
  } else {
   startInp.disabled = false;
   endInp.disabled = false;
   endInp.value = Math.min(currentPdfDoc.numPages, 30);
  }
 }
};

window.handleVocabFileSelect = async function(input) {
 if (input.files && input.files[0]) {
  await setImportFile(input.files[0]);
 }
};

window.handleVocabFileDrop = async function(e) {
 e.preventDefault();
 const dz = document.getElementById('vocabDropzone');
 if (dz) dz.classList.remove('drag-over');
 if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0]) {
  await setImportFile(e.dataTransfer.files[0]);
 }
};

async function setImportFile(file) {
 currentImportFile = file;
 currentPdfDoc = null;
 const nameEl = document.getElementById('vocabFileSelectedName');
 const pdfRow = document.getElementById('viPdfPageRangeRow');
 const totalPagesEl = document.getElementById('viPdfTotalPages');
 const endInp = document.getElementById('viPdfPageEnd');
 const startInp = document.getElementById('viPdfPageStart');

 const sizeKb = Math.round(file.size / 1024);
 if (nameEl) {
  nameEl.textContent = `✓ Đã chọn: ${file.name} (${sizeKb} KB)`;
  nameEl.style.display = 'block';
 }

 const ext = file.name.split('.').pop().toLowerCase();
 if (ext === 'pdf' && window.pdfjsLib) {
  try {
   const arrayBuffer = await file.arrayBuffer();
   currentPdfDoc = await window.pdfjsLib.getDocument({ data: arrayBuffer }).promise;
   if (pdfRow) pdfRow.style.display = 'flex';
   if (totalPagesEl) totalPagesEl.textContent = `(Tổng cộng ${currentPdfDoc.numPages} trang)`;
   if (startInp) startInp.value = 1;
   if (endInp) endInp.value = Math.min(currentPdfDoc.numPages, 35);
  } catch (err) {
   console.warn('[LifeOS Vocab] Pre-reading PDF metadata failed:', err);
  }
 } else {
  if (pdfRow) pdfRow.style.display = 'none';
 }

 toast('Đã tải lên: ' + file.name, 'success');
}

// ── Line-Preserving PDF Extractor ──
async function readPdfTextWithLineBreaks(pdfDoc, startPage, endPage, onProgress) {
 let fullText = '';
 
 for (let p = startPage; p <= endPage; p++) {
  if (typeof onProgress === 'function') {
   onProgress(p, startPage, endPage);
  }
  const page = await pdfDoc.getPage(p);
  const textContent = await page.getTextContent();
  
  let pageLines = [];
  let currentLine = '';
  let lastY = null;

  // Group items by vertical position to preserve true line breaks
  for (const item of textContent.items) {
   const y = item.transform[5];
   if (lastY !== null && Math.abs(y - lastY) > 4) {
    if (currentLine.trim()) pageLines.push(currentLine.trim());
    currentLine = '';
   } else if (currentLine.length > 0 && !currentLine.endsWith(' ') && !currentLine.endsWith('-')) {
    currentLine += ' ';
   }
   currentLine += item.str;
   lastY = y;
  }
  if (currentLine.trim()) pageLines.push(currentLine.trim());

  fullText += pageLines.join('\n') + '\n\n';
 }
 return fullText;
}

// ── Word .docx Reader ──
async function readDocxFileText(file) {
 if (!window.mammoth) throw new Error('Thư viện Mammoth chưa sẵn sàng.');
 const arrayBuffer = await file.arrayBuffer();
 const res = await window.mammoth.extractRawText({ arrayBuffer });
 return res.value || '';
}

// ── Map Grammar Parts of Speech to standard abbreviations ──
function mapPartOfSpeech(rawType) {
 if (!rawType) return 'n';
 const t = rawType.toLowerCase().trim();
 if (t.includes('noun') || t === 'n') return 'n';
 if (t.includes('verb') || t === 'v') return 'v';
 if (t.includes('adj') || t.includes('adjective')) return 'adj';
 if (t.includes('adv') || t.includes('adverb')) return 'adv';
 if (t.includes('idiom') || t.includes('phrase') || t.includes('clause') || t.includes('linking') || t === 'phr') return 'phr';
 return 'phr';
}

// ── Comprehensive Regex Parser for Vietnamese IELTS Books & Lists ──
function parseStructuredVocabList(content) {
 const lines = content.split(/\r?\n/);
 const items = [];
 const seenWords = new Set();
 let currentItem = null;

 for (let i = 0; i < lines.length; i++) {
  const line = lines[i].trim();
  if (!line) continue;

  // Handle English definition / example line: "ENG: ..."
  if (/^ENG\s*:\s*/i.test(line) && currentItem) {
   const engDef = line.replace(/^ENG\s*:\s*/i, '').trim();
   currentItem.ex = engDef;
   continue;
  }

  // Matches patterns:
  // - on the outskirts of something (prepositional phrase): ở ngoại ô
  // - facilitate something (verb): tạo điều kiện thuận lợi
  // - located + adv./prep. (adj): tọa lạc, ở
  // - born and bred (idiom): sinh ra và lớn lên
  // • resilient (adj) : kiên cường
  // word - meaning
  // word : meaning
  const entryMatch = line.match(/^[-•*–—]?\s*([a-zA-Z0-9\s\/'\+\.\,\(\)~…\-’‘]+?)\s*(?:\(([^)]+)\))?\s*[:–—=]\s*(.+)$/);
  if (entryMatch) {
   let rawWord = entryMatch[1].trim().replace(/^[-•*–—]\s*/, '').trim();
   const rawType = entryMatch[2] ? entryMatch[2].trim() : '';
   let mean = entryMatch[3].trim();
   
   const lowerWord = rawWord.toLowerCase();
   // Skip metadata headers
   if (
    lowerWord.startsWith('câu hỏi') ||
    lowerWord.startsWith('giải thích') ||
    lowerWord.startsWith('mục lục') ||
    lowerWord.startsWith('lời mở đầu') ||
    lowerWord.startsWith('http') ||
    lowerWord.startsWith('trang') ||
    lowerWord.startsWith('ielts thanh loan') ||
    /^\d+\./.test(rawWord)
   ) {
    continue;
   }

   if (rawWord.length >= 2 && mean.length >= 1 && !seenWords.has(lowerWord)) {
    seenWords.add(lowerWord);
    currentItem = {
     word: rawWord,
     type: mapPartOfSpeech(rawType),
     pron: '',
     mean: mean,
     ex: '',
     selected: true
    };
    items.push(currentItem);
    continue;
   }
  }

  // Multiline continuation of English definition / example
  if (currentItem && currentItem.ex && !line.startsWith('-') && !line.startsWith('•') && !line.startsWith('Giải thích') && !line.startsWith('Câu hỏi')) {
   if (!line.includes(':') || line.startsWith('(')) {
    currentItem.ex += ' ' + line;
   }
  }
 }

 return items;
}

// ── Built-in Academic Vocabulary Knowledge Base ──
const BUILTIN_VOCAB_DICT = {
 'resilient': { type: 'adj', pron: '/rɪˈzɪl.jənt/', mean: 'kiên cường, có khả năng phục hồi nhanh' },
 'streamline': { type: 'v', pron: '/ˈstriːm.laɪn/', mean: 'hợp lý hóa, tối ưu hóa quy trình' },
 'meticulous': { type: 'adj', pron: '/məˈtɪk.jə.ləs/', mean: 'tỉ mỉ, cẩn trọng trong từng chi tiết' },
 'ubiquitous': { type: 'adj', pron: '/juːˈbɪk.wɪ.təs/', mean: 'phổ biến, có mặt ở khắp mọi nơi' },
 'pragmatic': { type: 'adj', pron: '/præɡˈmæt.ɪk/', mean: 'thực tế, mang tính thực tiễn' },
 'leverage': { type: 'v', pron: '/ˈlev.ər.ɪdʒ/', mean: 'tận dụng, khai thác tối đa nguồn lực' },
 'mitigate': { type: 'v', pron: '/ˈmɪt.ɪ.ɡeɪt/', mean: 'giảm nhẹ, xoa dịu (rủi ro, thiệt hại)' },
 'comprehensive': { type: 'adj', pron: '/ˌkɒm.prɪˈhen.sɪv/', mean: 'toàn diện, bao quát mọi mặt' },
 'scrutinize': { type: 'v', pron: '/ˈskruː.tɪ.naɪz/', mean: 'xem xét kỹ lưỡng, kiểm tra tỉ mỉ' },
 'lucrative': { type: 'adj', pron: '/ˈluː.krə.tɪv/', mean: 'sinh lợi nhiều, có lãi' },
 'inevitable': { type: 'adj', pron: '/ɪnˈev.ɪ.tə.bəl/', mean: 'không thể tránh khỏi, chắc chắn xảy ra' },
 'paradigm': { type: 'n', pron: '/ˈpær.ə.daɪm/', mean: 'mô hình, hệ quy chiếu' },
 'collaborate': { type: 'v', pron: '/kəˈlæb.ə.reɪt/', mean: 'hợp tác, cộng tác' },
 'discrepancy': { type: 'n', pron: '/dɪˈskrep.ən.si/', mean: 'sự khác biệt, sự không nhất quán' },
 'feasibility': { type: 'n', pron: '/ˌfiː.zəˈbɪl.ə.ti/', mean: 'tính khả thi, tính thực tế' },
 'ambiguous': { type: 'adj', pron: '/æmˈbɪɡ.ju.əs/', mean: 'mơ hồ, đa nghĩa, không rõ ràng' },
 'coherent': { type: 'adj', pron: '/kəʊˈhɪə.rənt/', mean: 'mạch lạc, chặt chẽ' },
 'exponential': { type: 'adj', pron: '/ˌek.spəˈnen.ʃəl/', mean: 'tăng theo cấp số nhân' },
 'sustainable': { type: 'adj', pron: '/səˈsteɪ.nə.bəl/', mean: 'bền vững, duy trì lâu dài' },
 'benchmark': { type: 'n', pron: '/ˈbentʃ.mɑːk/', mean: 'tiêu chuẩn đối sánh, mốc chuẩn' },
 'unprecedented': { type: 'adj', pron: '/ʌnˈpres.ɪ.den.tɪd/', mean: 'chưa từng có tiền lệ' },
 'perceive': { type: 'v', pron: '/pəˈsiːv/', mean: 'nhận thức, cảm nhận' },
 'substantiate': { type: 'v', pron: '/səbˈstæn.ʃi.eɪt/', mean: 'chứng minh, xác minh bằng chứng' },
 'divergent': { type: 'adj', pron: '/daɪˈvɜː.dʒənt/', mean: 'khác biệt, phân kỳ' },
 'counterpart': { type: 'n', pron: '/ˈkaʊn.tə.pɑːt/', mean: 'bên tương đương, đối tác' },
 'allocate': { type: 'v', pron: '/ˈæl.ə.keɪt/', mean: 'phân bổ (ngân sách, nguồn lực)' },
 'facilitate': { type: 'v', pron: '/fəˈsɪl.ɪ.teɪt/', mean: 'tạo điều kiện thuận lợi' },
 'deteriorate': { type: 'v', pron: '/dɪˈtɪə.ri.ə.reɪt/', mean: 'suy giảm, trở nên tồi tệ hơn' },
 'predominant': { type: 'adj', pron: '/prɪˈdɒm.ɪ.nənt/', mean: 'chiếm ưu thế, chủ đạo' },
 'imperative': { type: 'adj', pron: '/ɪmˈper.ə.tɪv/', mean: 'cấp bách, bắt buộc phải có' }
};

// ── Main Extraction Pipeline ──
window.runVocabExtraction = async function() {
 const mode = document.getElementById('viMode')?.value || 'smart';
 const limit = parseInt(document.getElementById('viLimit')?.value || 25);
 const isFileTab = document.getElementById('viTabFile')?.classList.contains('active');

 let text = '';

 const loadEl = document.getElementById('viLoading');
 const loadTxt = document.getElementById('viLoadingText');
 const previewWrap = document.getElementById('viPreviewWrap');
 const btnConfirm = document.getElementById('btnConfirmImport');

 try {
  if (isFileTab) {
   if (!currentImportFile) {
    toast('Vui lòng chọn hoặc kéo thả file vào khung!', 'error');
    return;
   }
   if (loadEl) loadEl.style.display = 'block';

   const ext = currentImportFile.name.split('.').pop().toLowerCase();
   if (ext === 'pdf') {
    if (!currentPdfDoc) {
     const arrayBuffer = await currentImportFile.arrayBuffer();
     currentPdfDoc = await window.pdfjsLib.getDocument({ data: arrayBuffer }).promise;
    }
    const startP = Math.max(1, parseInt(document.getElementById('viPdfPageStart')?.value || 1));
    const endP = Math.min(currentPdfDoc.numPages, parseInt(document.getElementById('viPdfPageEnd')?.value || currentPdfDoc.numPages));

    text = await readPdfTextWithLineBreaks(currentPdfDoc, startP, endP, (cur, s, e) => {
     if (loadTxt) loadTxt.textContent = `Đang quét trang ${cur} / ${e} của file PDF...`;
    });
   } else if (ext === 'docx') {
    if (loadTxt) loadTxt.textContent = 'Đang đọc nội dung file Word (.docx)...';
    text = await readDocxFileText(currentImportFile);
   } else {
    if (loadTxt) loadTxt.textContent = 'Đang đọc file văn bản...';
    text = await currentImportFile.text();
   }
  } else {
   text = (document.getElementById('vocabPasteText')?.value || '').trim();
   if (!text) {
    toast('Vui lòng dán nội dung văn bản hoặc danh sách từ vào ô!', 'error');
    return;
   }
   if (loadEl) loadEl.style.display = 'block';
   if (loadTxt) loadTxt.textContent = 'Đang phân tích văn bản...';
  }

  if (!text || text.trim().length < 5) {
   throw new Error('Tài liệu không chứa đủ nội dung văn bản để trích xuất.');
  }

  if (loadTxt) loadTxt.textContent = 'Đang trích xuất từ vựng, phân loại và dịch nghĩa...';

  // 1. First priority: Check if text contains structured book lists like "Giải thích từ vựng:"
  let items = parseStructuredVocabList(text);

  // 2. If structured parser found words, limit to requested amount (or take all if within reason)
  if (items.length > 0) {
   if (items.length > limit) {
    items = items.slice(0, limit);
   }
  } else {
   // 3. Fallback to AI smart extraction or academic dictionary scanner
   if (mode === 'smart') {
    items = await extractVocabSmart(text, limit);
   } else {
    items = extractVocabPatternFallback(text, limit);
   }
  }

  if (!items || !items.length) {
   items = extractVocabPatternFallback(text, limit);
  }

  if (!items.length) {
   throw new Error('Không nhận diện được từ vựng nào. Nếu tài liệu là PDF dạng scan ảnh, hãy copy text dán vào tab "Dán văn bản trực tiếp".');
  }

  importedVocabList = items;
  renderImportPreview(items);
  
  if (loadEl) loadEl.style.display = 'none';
  if (previewWrap) previewWrap.style.display = 'block';
  if (btnConfirm) btnConfirm.style.display = 'inline-flex';
  toast(`🎉 Đã trích xuất thành công ${items.length} từ vựng!`, 'success');

 } catch (err) {
  if (loadEl) loadEl.style.display = 'none';
  console.error('[LifeOS Vocab Extract Error]', err);
  toast('Lỗi: ' + (err.message || 'Không thể trích xuất'), 'error');
 }
};

// ── Smart AI Extractor ──
async function extractVocabSmart(rawText, limit) {
 const apiKey = localStorage.getItem('lifeos_gemini_key') || (window.AI_CONFIG && window.AI_CONFIG.geminiKey) || '';
 const textSnippet = rawText.slice(0, 7500);

 if (apiKey && apiKey.trim().length > 10) {
  try {
   const prompt = `You are an expert English lexicographer. Analyze the following text and extract up to ${limit} key vocabulary words, idioms, collocations, or academic expressions.
For each item, provide:
- "word": English word/phrase in lower case
- "type": part of speech, strictly one of: "n", "v", "adj", "adv", "phr"
- "pron": standard IPA pronunciation, e.g. /ɪnˈvaɪ.rən.mənt/
- "mean": precise, natural Vietnamese translation in the context of the text
- "ex": a concise example sentence from or related to the text

Return ONLY a valid JSON array of objects without any markdown fences, backticks, or extra explanation. Format:
[{"word":"example","type":"n","pron":"/ɪɡˈzɑːm.pəl/","mean":"ví dụ","ex":"This is an example sentence."}]

Text to analyze:
${textSnippet}`;

   const res = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${apiKey}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
     contents: [{ parts: [{ text: prompt }] }],
     generationConfig: { temperature: 0.2, maxOutputTokens: 2048 }
    })
   });

   if (res.ok) {
    const data = await res.json();
    const txt = data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
    const cleanJson = txt.split('```json').join('').split('```').join('').trim();
    const parsed = JSON.parse(cleanJson);
    if (Array.isArray(parsed) && parsed.length > 0) {
     return parsed.map(item => ({
      word: String(item.word || '').trim(),
      type: ['n','v','adj','adv','phr'].includes(item.type) ? item.type : 'n',
      pron: String(item.pron || '').trim(),
      mean: String(item.mean || '').trim(),
      ex: String(item.ex || '').trim(),
      selected: true
     })).filter(x => x.word && x.mean);
    }
   }
  } catch (aiErr) {
   console.warn('[LifeOS Vocab AI] Gemini API call failed, falling back:', aiErr);
  }
 }

 return extractVocabPatternFallback(rawText, limit);
}

// ── Fallback Tokenizer & Dictionary Scanner ──
function extractVocabPatternFallback(text, limit = 20) {
 const results = [];
 const seen = new Set();
 const words = text.match(/\b[a-zA-Z]{4,20}\b/g) || [];

 for (const rawW of words) {
  const w = rawW.toLowerCase();
  if (!seen.has(w) && BUILTIN_VOCAB_DICT[w]) {
   seen.add(w);
   const info = BUILTIN_VOCAB_DICT[w];
   
   const sentences = text.match(/[^.!?]+[.!?]+/g) || [];
   let foundEx = '';
   for (const sent of sentences) {
    if (new RegExp(`\\b${w}\\b`, 'i').test(sent) && sent.trim().length <= 150) {
     foundEx = sent.trim();
     break;
    }
   }

   results.push({
    word: w,
    type: info.type || 'n',
    pron: info.pron || '',
    mean: info.mean || '',
    ex: foundEx || `We need to study ${w} carefully.`,
    selected: true
   });

   if (results.length >= limit) break;
  }
 }

 return results;
}

// ── Preview Table UI ──
function renderImportPreview(items) {
 const tbody = document.getElementById('viPreviewTbody');
 if (!tbody) return;

 tbody.innerHTML = items.map((item, idx) => `
  <tr id="viRow_${idx}">
   <td style="text-align:center;">
    <input type="checkbox" class="vi-chk" data-idx="${idx}" ${item.selected ? 'checked' : ''} onchange="updateSelectedImportCount()" style="cursor:pointer; width:16px; height:16px; accent-color:var(--accent);">
   </td>
   <td>
    <input type="text" class="inp" id="viWord_${idx}" value="${window.LifeOSData?.escapeAttr(item.word) || item.word}" style="padding:4px 8px; font-size:12.5px; font-weight:700; color:var(--accent-light); width:100%;">
   </td>
   <td>
    <select class="inp" id="viType_${idx}" style="padding:4px 6px; font-size:11px; width:100%;">
     <option value="n" ${item.type==='n'?'selected':''}>n</option>
     <option value="v" ${item.type==='v'?'selected':''}>v</option>
     <option value="adj" ${item.type==='adj'?'selected':''}>adj</option>
     <option value="adv" ${item.type==='adv'?'selected':''}>adv</option>
     <option value="phr" ${item.type==='phr'?'selected':''}>phr</option>
    </select>
   </td>
   <td>
    <input type="text" class="inp" id="viPron_${idx}" value="${window.LifeOSData?.escapeAttr(item.pron) || item.pron}" placeholder="/.../" style="padding:4px 8px; font-size:11.5px; width:100%; color:var(--text-mid);">
   </td>
   <td>
    <input type="text" class="inp" id="viMean_${idx}" value="${window.LifeOSData?.escapeAttr(item.mean) || item.mean}" style="padding:4px 8px; font-size:12.5px; width:100%; font-weight:500;">
   </td>
   <td>
    <input type="text" class="inp" id="viEx_${idx}" value="${window.LifeOSData?.escapeAttr(item.ex) || item.ex}" placeholder="Ví dụ..." style="padding:4px 8px; font-size:11.5px; width:100%; color:var(--text-low);">
   </td>
   <td style="text-align:center;">
    <button class="btn-audio" onclick="event.preventDefault(); speakWord(document.getElementById('viWord_${idx}').value)" title="Nghe phát âm">
     <i data-lucide="volume-2" style="width:13px;height:13px;"></i>
    </button>
   </td>
  </tr>
 `).join('');

 if (window.lucide) window.lucide.createIcons();
 updateSelectedImportCount();
}

window.toggleSelectAllImportVocab = function(checked) {
 document.querySelectorAll('.vi-chk').forEach(chk => {
  chk.checked = checked;
 });
 updateSelectedImportCount();
};

window.updateSelectedImportCount = function() {
 const chks = document.querySelectorAll('.vi-chk:checked');
 const badge = document.getElementById('viSelectedCount');
 if (badge) {
  badge.textContent = `Đã chọn: ${chks.length} / ${importedVocabList.length}`;
 }
 const allChk = document.getElementById('viCheckAll');
 if (allChk) {
  allChk.checked = chks.length === importedVocabList.length && importedVocabList.length > 0;
 }
};

// ── Save Batch to DB ──
window.confirmImportVocab = async function() {
 const selectedRows = document.querySelectorAll('.vi-chk:checked');
 if (!selectedRows.length) {
  toast('Hãy tick chọn ít nhất 1 từ để thêm!', 'error');
  return;
 }

 const toAdd = [];
 selectedRows.forEach(chk => {
  const idx = chk.getAttribute('data-idx');
  const word = document.getElementById('viWord_' + idx)?.value.trim();
  const mean = document.getElementById('viMean_' + idx)?.value.trim();
  const type = document.getElementById('viType_' + idx)?.value || 'n';
  const pron = document.getElementById('viPron_' + idx)?.value.trim();
  const ex = document.getElementById('viEx_' + idx)?.value.trim();

  if (word && mean) {
   toAdd.push({
    id: uid(),
    word,
    mean,
    type,
    pron,
    ex,
    date: today()
   });
  }
 });

 if (!toAdd.length) {
  toast('Không có từ vựng hợp lệ để lưu!', 'error');
  return;
 }

 if (!window.DB.vocab) window.DB.vocab = [];
 window.DB.vocab = [...toAdd, ...window.DB.vocab];
 await persist('vocab', window.DB.vocab);
 
 closeModal('mImportVocab');
 renderVocab();
 toast(`🎉 Đã nạp thành công ${toAdd.length} từ vựng vào bộ thẻ!`, 'success');
};

// ── CRUD For Single Vocab ──
function openVocab(id) {
 document.getElementById('vocabId').value = '';
 document.getElementById('vWord').value = '';
 document.getElementById('vPron').value = '';
 document.getElementById('vMean').value = '';
 document.getElementById('vEx').value = '';
 document.getElementById('vType').value = 'n';
 document.getElementById('mVocabTitle').textContent = 'Thêm Từ Mới';
 if (id) {
  const v = (window.DB.vocab || []).find(x => x.id === id);
  if (v) {
   document.getElementById('vocabId').value = v.id;
   document.getElementById('vWord').value = v.word;
   document.getElementById('vPron').value = v.pron || '';
   document.getElementById('vMean').value = v.mean;
   document.getElementById('vEx').value = v.ex || '';
   document.getElementById('vType').value = v.type || 'n';
   document.getElementById('mVocabTitle').textContent = 'Sửa Từ Vựng';
  }
 }
 openModal('mVocab');
}

async function saveVocab() {
 const word = document.getElementById('vWord').value.trim();
 const mean = document.getElementById('vMean').value.trim();
 if (!word || !mean) { toast('Nhập đủ Từ vựng và Nghĩa', 'error'); return; }
 const pron = document.getElementById('vPron').value.trim();
 const ex = document.getElementById('vEx').value.trim();
 const type = document.getElementById('vType').value;
 const id = document.getElementById('vocabId').value || uid();
 if (!window.DB.vocab) window.DB.vocab = [];
 const idx = window.DB.vocab.findIndex(x => x.id === id);
 const voc = { id, word, mean, pron, ex, type, date: today() };
 if (idx >= 0) window.DB.vocab[idx] = voc; else window.DB.vocab.unshift(voc);
 await persist('vocab', window.DB.vocab);
 closeModal('mVocab');
 renderVocab();
 toast('Đã lưu từ vựng!', 'success');
}

async function delVocab(id) {
 if (!confirm('Xóa từ vựng này?')) return;
 window.DB.vocab = (window.DB.vocab || []).filter(x => x.id !== id);
 await persist('vocab', window.DB.vocab);
 renderVocab();
 toast('Đã xóa', 'info');
}

function renderVocab() {
 const c = document.getElementById('vocabGrid'); if (!c) return;
 const q = (document.getElementById('vocabSearch')?.value || '').toLowerCase();
 const allVocs = window.DB.vocab || [];
 const vocs = allVocs.filter(v =>
  !q || v.word.toLowerCase().includes(q) || v.mean.toLowerCase().includes(q)
 );

 const countBadge = document.getElementById('vocabCountBadge');
 if (countBadge) {
  countBadge.textContent = `${vocs.length} / ${allVocs.length} từ`;
 }

 if (!vocs.length) {
  c.innerHTML = `<div class="empty-state" style="grid-column:1/-1; padding:40px 16px;">
   <div class="empty-state-icon"><i data-lucide="languages" class="ic-24"></i></div>
   <div class="empty-state-title">Chưa có từ vựng nào</div>
   <div class="empty-state-desc">Thêm từ mới hoặc nhập hàng loạt từ file PDF, Word để bắt đầu ôn tập!</div>
   <div style="display:flex; gap:10px; justify-content:center;">
    <button class="btn btn-outline btn-sm" onclick="openImportVocabModal()"><i data-lucide="file-up" class="ic-14"></i> Nhập từ file</button>
    <button class="btn btn-p btn-sm" onclick="openVocab()"><i data-lucide="plus" class="ic-14"></i> Thêm từ mới</button>
   </div>
  </div>`;
  if (window.lucide) window.lucide.createIcons();
  return;
 }

 c.innerHTML = vocs.map(v => `
  <div class="vocab-card" onclick="openVocab('${v.id}')">
   <div class="vc-top">
    <div class="vc-word">${v.word}</div>
    <div class="vc-type">(${v.type || 'n'})</div>
   </div>
   <div style="display:flex; align-items:center; gap:8px; margin-bottom:8px;">
    <div class="vc-pron">${v.pron || ''}</div>
    <button class="btn-audio" onclick="event.stopPropagation(); speakWord('${v.word}')" title="Nghe phát âm" style="width:24px; height:24px;">
     <i data-lucide="volume-2" style="width:12px;height:12px;"></i>
    </button>
   </div>
   <div class="vc-mean">${v.mean}</div>
   ${v.ex ? `<div style="font-size:11.5px; color:var(--text-low); margin-top:8px; font-style:italic; line-height:1.4;">"${v.ex}"</div>` : ''}
   <div class="vc-actions">
    <button class="btn btn-sm btn-r" style="padding:2px 6px; font-size:11px;" onclick="event.stopPropagation();delVocab('${v.id}')" title="Xóa">
     <i data-lucide="trash-2" style="width:12px;height:12px;"></i>
    </button>
   </div>
  </div>
 `).join('');

 if (window.lucide) window.lucide.createIcons();
}

document.getElementById('vocabSearch')?.addEventListener('input', renderVocab);

// ── Review Flashcards ──
let reviewList = [];
let reviewIdx = 0;

function reviewVocab() {
 reviewList = [...(window.DB.vocab || [])];
 if (!reviewList.length) { toast('Chưa có từ vựng để ôn tập! Hãy thêm hoặc nhập từ file trước.', 'error'); return; }
 reviewList.sort(() => Math.random() - 0.5);
 reviewIdx = 0;
 updateReviewUI();
 document.querySelector('.flashcard-container')?.classList.remove('flipped');
 openModal('mReviewVocab');
 if (window.lucide) window.lucide.createIcons();
}

function updateReviewUI() {
 if (!reviewList.length) return;
 const v = reviewList[reviewIdx];
 const cntEl = document.getElementById('rvCount');
 if (cntEl) cntEl.textContent = `Ôn tập (${reviewIdx + 1}/${reviewList.length})`;
 const wEl = document.getElementById('rvWord');
 if (wEl) wEl.textContent = v.word;
 const pEl = document.getElementById('rvPron');
 if (pEl) pEl.textContent = v.pron || '';
 const tEl = document.getElementById('rvType');
 if (tEl) tEl.textContent = '(' + (v.type || 'n') + ')';
 const mEl = document.getElementById('rvMean');
 if (mEl) mEl.textContent = v.mean;
 const exEl = document.getElementById('rvEx');
 if (exEl) exEl.textContent = v.ex || '';
 
 document.querySelector('.flashcard-container')?.classList.remove('flipped');
}

function nextVocab() {
 if (reviewIdx < reviewList.length - 1) { reviewIdx++; updateReviewUI(); }
 else { toast('🎉 Bạn đã hoàn thành phiên ôn tập!', 'success'); endReviewVocab(); }
}
function prevVocab() {
 if (reviewIdx > 0) { reviewIdx--; updateReviewUI(); }
}
function endReviewVocab() {
 closeModal('mReviewVocab');
}

window.openVocab = openVocab;
window.saveVocab = saveVocab;
window.delVocab = delVocab;
window.renderVocab = renderVocab;
window.reviewVocab = reviewVocab;
window.nextVocab = nextVocab;
window.prevVocab = prevVocab;
window.endReviewVocab = endReviewVocab;
