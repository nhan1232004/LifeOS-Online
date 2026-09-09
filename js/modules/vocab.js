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
   if (endInp) endInp.value = currentPdfDoc.numPages;
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
 if (t.includes('noun') && !t.includes('phrase')) return 'n';
 if (t.includes('verb') && !t.includes('phrase')) return 'v';
 if (t.includes('adj') || t.includes('adjective')) return 'adj';
 if (t.includes('adv') || t.includes('adverb')) return 'adv';
 if (t.includes('idiom') || t.includes('phrase') || t.includes('clause') || t.includes('collocation') || t.includes('phr') || t.includes('phrasal') || t.includes('slang') || t.includes('metaphor')) return 'phr';
 if (t.includes('noun')) return 'n';
 if (t.includes('verb')) return 'v';
 return 'n';
}

// ── Comprehensive Parser for IELTS Books, Handbooks & Vocabulary Lists ──
function parseStructuredVocabList(content) {
 const lines = content.split(/\r?\n/);
 const items = [];
 const seenWords = new Set();
 let currentEntry = null;

 function finalizeEntry() {
  if (!currentEntry) return;
  let w = currentEntry.word.trim();
  const m = currentEntry.mean.trim();
  
  // Clean numbering if present e.g. "1. Dismiss" -> "Dismiss"
  w = w.replace(/^\d+\.\s*/, '').trim();
  const lower = w.toLowerCase();
  
  if (w && m && !seenWords.has(lower) && w.length >= 2) {
   seenWords.add(lower);
   items.push({
    word: w,
    type: currentEntry.type || 'n',
    pron: currentEntry.pron || '',
    mean: m,
    ex: currentEntry.ex || '',
    selected: true
   });
  }
  currentEntry = null;
 }

 for (let i = 0; i < lines.length; i++) {
  let line = lines[i].trim();
  if (!line) continue;

  // Clean bullet symbols at start: ●, •, *, ▪, ▫, -
  const cleanLine = line.replace(/^[●•*▪▫-]\s*/, '').trim();

  // 1. Attribute matchers for current entry
  const pronMatch = cleanLine.match(/^(?:Phiên âm|Pronunciation)\s*:\s*(.+)$/i);
  if (pronMatch) {
   if (currentEntry) currentEntry.pron = pronMatch[1].trim();
   continue;
  }

  const typeMatch = cleanLine.match(/^(?:Từ loại|Part of speech)\s*:\s*(.+)$/i);
  if (typeMatch) {
   if (currentEntry) currentEntry.type = mapPartOfSpeech(typeMatch[1].trim());
   continue;
  }

  const meanMatch = cleanLine.match(/^(?:Nghĩa tiếng Việt|Nghĩa|Meaning)\s*:\s*(.+)$/i);
  if (meanMatch) {
   if (currentEntry) currentEntry.mean = meanMatch[1].trim();
   continue;
  }

  const familyMatch = cleanLine.match(/^(?:Gia đình từ|Word family|Word families)\s*:\s*(.+)$/i);
  if (familyMatch) {
   continue;
  }

  const exMatch = cleanLine.match(/^(?:Ví dụ|Example|Ex)\s*:\s*(.+)$/i);
  if (exMatch) {
   if (currentEntry) currentEntry.ex = exMatch[1].trim();
   continue;
  }

  // 2. Word Header detection (Handbook Block format: "1. Dismiss", "20. Ace an exam")
  const numHeaderMatch = cleanLine.match(/^(\d+)\.\s+([A-Za-z0-9\s\/'\+\.\,\(\)~…\-’‘]+)$/);
  if (numHeaderMatch) {
   const potentialWord = numHeaderMatch[2].trim();
   const lower = potentialWord.toLowerCase();
   
   // Skip chapter/section headers like "1. Education (Giáo dục)" or "Mục lục"
   if (
    lower.includes('(giáo dục)') ||
    lower.includes('(công việc') ||
    lower.includes('(truyền thông') ||
    lower.includes('(môi trường') ||
    lower.includes('(xã hội') ||
    lower.includes('(kinh doanh') ||
    lower.includes('(sức khỏe') ||
    lower.includes('(du lịch') ||
    lower.includes('(gia đình') ||
    lower.includes('(tính cách') ||
    lower.includes('(văn hóa') ||
    lower.startsWith('mục lục') ||
    /^[A-Z\s,–—]{6,}$/.test(potentialWord)
   ) {
    finalizeEntry();
    continue;
   }

   // Peek ahead to confirm it's followed by "Phiên âm" or "Từ loại" or "Nghĩa"
   let isVocabBlock = false;
   for (let j = i + 1; j < Math.min(i + 6, lines.length); j++) {
    const nextClean = lines[j].replace(/^[●•*▪▫-]\s*/, '').trim();
    if (/^(?:Phiên âm|Từ loại|Nghĩa tiếng Việt|Nghĩa)\s*:/i.test(nextClean)) {
     isVocabBlock = true;
     break;
    }
   }

   if (isVocabBlock) {
    finalizeEntry();
    currentEntry = {
     word: potentialWord,
     type: 'n',
     pron: '',
     mean: '',
     ex: ''
    };
    continue;
   }
  }

  // Also check if line is word header without number: e.g. "Cognitive" followed immediately by "Phiên âm:"
  if (/^[A-Za-z\s\/'\+\,\(\)~…\-’‘]{3,50}$/.test(cleanLine)) {
   let isVocabBlock = false;
   for (let j = i + 1; j < Math.min(i + 4, lines.length); j++) {
    const nextClean = lines[j].replace(/^[●•*▪▫-]\s*/, '').trim();
    if (/^(?:Phiên âm|Từ loại|Nghĩa tiếng Việt|Nghĩa)\s*:/i.test(nextClean)) {
     isVocabBlock = true;
     break;
    }
   }
   if (isVocabBlock) {
    finalizeEntry();
    currentEntry = {
     word: cleanLine,
     type: 'n',
     pron: '',
     mean: '',
     ex: ''
    };
    continue;
   }
  }

  // 3. Single-line pattern match: "word (type) : meaning" or "word - meaning"
  const singleLineMatch = cleanLine.match(/^([a-zA-Z0-9\s\/'\+\.\,\(\)~…\-’‘]+?)\s*(?:\(([^)]+)\))?\s*[:–—=]\s*(.+)$/);
  if (singleLineMatch) {
   const w = singleLineMatch[1].trim();
   const lower = w.toLowerCase();
   if (
    !lower.startsWith('phiên âm') &&
    !lower.startsWith('từ loại') &&
    !lower.startsWith('nghĩa') &&
    !lower.startsWith('gia đình từ') &&
    !lower.startsWith('ví dụ') &&
    !lower.startsWith('câu hỏi') &&
    !lower.startsWith('giải thích') &&
    !lower.startsWith('mục lục') &&
    !lower.startsWith('lời mở đầu') &&
    !lower.startsWith('trang')
   ) {
    finalizeEntry();
    const cleanWord = w.replace(/^\d+\.\s*/, '').trim();
    if (cleanWord.length >= 2) {
     items.push({
      word: cleanWord,
      type: mapPartOfSpeech(singleLineMatch[2]),
      pron: '',
      mean: singleLineMatch[3].trim(),
      ex: '',
      selected: true
     });
    }
    continue;
   }
  }

  // 4. Multiline continuation of example or meaning
  if (currentEntry) {
   if (currentEntry.ex && (cleanLine.startsWith('(') || !cleanLine.includes(':'))) {
    currentEntry.ex += ' ' + cleanLine;
   } else if (currentEntry.mean && !currentEntry.ex && !cleanLine.includes(':')) {
    currentEntry.mean += ' ' + cleanLine;
   }
  }
 }

 finalizeEntry();
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

  // 2. If structured parser found words, limit to requested amount (or take all if limit is 999)
  if (items.length > 0) {
   if (limit < 999 && items.length > limit) {
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
 selectedVocabIds.delete(id);
 await persist('vocab', window.DB.vocab);
 renderVocab();
 toast('Đã xóa', 'info');
}

// ── Batch Selection State & Handlers ──
let isVocabSelectMode = false;
let selectedVocabIds = new Set();

function getVisibleVocabIds() {
 const q = (document.getElementById('vocabSearch')?.value || '').toLowerCase();
 const allVocs = window.DB.vocab || [];
 return allVocs
  .filter(v => !q || v.word.toLowerCase().includes(q) || v.mean.toLowerCase().includes(q))
  .map(v => v.id);
}

function toggleVocabSelectMode() {
 isVocabSelectMode = !isVocabSelectMode;
 if (!isVocabSelectMode) {
  selectedVocabIds.clear();
 }
 syncVocabSelectModeUI();
 renderVocab();
}

function exitVocabSelectMode() {
 isVocabSelectMode = false;
 selectedVocabIds.clear();
 syncVocabSelectModeUI();
 renderVocab();
}

function syncVocabSelectModeUI() {
 const btn = document.getElementById('btnVocabSelectMode');
 const txt = document.getElementById('txtVocabSelectMode');
 const bar = document.getElementById('vocabBatchBar');
 if (btn) {
  if (isVocabSelectMode) {
   btn.classList.add('btn-p');
   btn.classList.remove('btn-outline');
   if (txt) txt.textContent = 'Hủy chọn';
  } else {
   btn.classList.remove('btn-p');
   btn.classList.add('btn-outline');
   if (txt) txt.textContent = 'Chọn nhiều';
  }
 }
 if (bar) {
  bar.style.display = isVocabSelectMode ? 'flex' : 'none';
 }
 updateVocabBatchBar();
}

function updateVocabBatchBar() {
 const count = selectedVocabIds.size;
 const badge = document.getElementById('vocabSelectedBadge');
 if (badge) badge.textContent = `Đã chọn: ${count} từ`;

 const btnDel = document.getElementById('btnBatchDeleteVocab');
 const txtDel = document.getElementById('txtBatchDelete');
 if (btnDel) btnDel.disabled = (count === 0);
 if (txtDel) txtDel.textContent = `Xóa đã chọn (${count})`;

 const chkAll = document.getElementById('vocabSelectAllChk');
 if (chkAll) {
  const visibleIds = getVisibleVocabIds();
  if (visibleIds.length > 0 && visibleIds.every(id => selectedVocabIds.has(id))) {
   chkAll.checked = true;
   chkAll.indeterminate = false;
  } else if (visibleIds.some(id => selectedVocabIds.has(id))) {
   chkAll.checked = false;
   chkAll.indeterminate = true;
  } else {
   chkAll.checked = false;
   chkAll.indeterminate = false;
  }
 }
}

function toggleVocabCardSelection(id, forceState) {
 if (typeof forceState === 'boolean') {
  if (forceState) selectedVocabIds.add(id);
  else selectedVocabIds.delete(id);
 } else {
  if (selectedVocabIds.has(id)) selectedVocabIds.delete(id);
  else selectedVocabIds.add(id);
 }
 const card = document.querySelector(`.vocab-card[data-id="${id}"]`);
 const chk = document.querySelector(`.vc-select-chk[data-id="${id}"]`);
 const isSelected = selectedVocabIds.has(id);
 if (card) {
  if (isSelected) card.classList.add('is-selected');
  else card.classList.remove('is-selected');
 }
 if (chk) {
  chk.checked = isSelected;
 }
 updateVocabBatchBar();
}

function toggleSelectAllVocab(checked) {
 const visibleIds = getVisibleVocabIds();
 if (checked) {
  visibleIds.forEach(id => selectedVocabIds.add(id));
 } else {
  visibleIds.forEach(id => selectedVocabIds.delete(id));
 }
 renderVocab();
 updateVocabBatchBar();
}

async function batchDeleteVocab() {
 const count = selectedVocabIds.size;
 if (count === 0) {
  toast('Vui lòng chọn ít nhất một từ vựng để xóa', 'info');
  return;
 }
 if (!confirm(`Bạn có chắc chắn muốn xóa ${count} từ vựng đã chọn? Thao tác này không thể hoàn tác.`)) return;

 window.DB.vocab = (window.DB.vocab || []).filter(v => !selectedVocabIds.has(v.id));
 await persist('vocab', window.DB.vocab);
 selectedVocabIds.clear();
 toast(`Đã xóa thành công ${count} từ vựng!`, 'success');
 renderVocab();
 updateVocabBatchBar();
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
  updateVocabBatchBar();
  return;
 }

 c.innerHTML = vocs.map(v => {
  const isSelected = selectedVocabIds.has(v.id);
  const cardClass = `vocab-card${isVocabSelectMode ? ' select-mode' : ''}${isSelected ? ' is-selected' : ''}`;
  const cardClick = isVocabSelectMode ? `toggleVocabCardSelection('${v.id}')` : `openVocab('${v.id}')`;
  const chkHtml = isVocabSelectMode ? `
   <input type="checkbox" class="vc-select-chk" data-id="${v.id}" ${isSelected ? 'checked' : ''} onclick="event.stopPropagation(); toggleVocabCardSelection('${v.id}', this.checked)" title="Chọn từ">
  ` : '';

  return `
  <div class="${cardClass}" data-id="${v.id}" onclick="${cardClick}">
   ${chkHtml}
   <div class="vc-top">
    <div class="vc-word">${v.word}</div>
    <div class="vc-type">(${v.type || 'n'})</div>
   </div>
   <div style="display:flex; align-items:center; gap:8px; margin-bottom:8px;">
    <div class="vc-pron">${v.pron || ''}</div>
    <button class="btn-audio" onclick="event.stopPropagation(); speakWord('${(v.word || '').replace(/'/g, "\\'")}')" title="Nghe phát âm" style="width:24px; height:24px;">
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
 `;
 }).join('');

 if (window.lucide) window.lucide.createIcons();
 updateVocabBatchBar();
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
window.toggleVocabSelectMode = toggleVocabSelectMode;
window.exitVocabSelectMode = exitVocabSelectMode;
window.toggleVocabCardSelection = toggleVocabCardSelection;
window.toggleSelectAllVocab = toggleSelectAllVocab;
window.batchDeleteVocab = batchDeleteVocab;

// ════════════════════════════════════════════════════════════
// COMMUNITY & SHARED VOCABULARY DECKS
// ════════════════════════════════════════════════════════════

const DEFAULT_COMMUNITY_DECKS = [
 {
  id: 'deck_ielts_academic',
  title: 'IELTS Academic Writing & Speaking (Band 7.5+)',
  category: 'IELTS',
  description: 'Tuyển tập từ vựng, collocations học thuật đắt giá giúp nâng band điểm Writing Task 2 & Speaking.',
  authorName: 'LifeOS Academy',
  authorEmail: 'ielts@lifeos.io',
  ownerUid: 'system_curated',
  mode: 'public',
  allowedEmails: [],
  clones: 142,
  createdAt: '2026-09-01T08:00:00.000Z',
  updatedAt: '2026-09-01T08:00:00.000Z',
  words: [
   { id: 'w1', word: 'ubiquitous', pron: '/juːˈbɪk.wɪ.təs/', type: 'adj', mean: 'có mặt ở khắp mọi nơi, phổ biến rộng rãi', ex: 'Smartphones and digital media have become ubiquitous in daily life.' },
   { id: 'w2', word: 'resilient', pron: '/rɪˈzɪl.jənt/', type: 'adj', mean: 'kiên cường, có khả năng phục hồi nhanh sau khó khăn', ex: 'The local economy proved remarkably resilient during the crisis.' },
   { id: 'w3', word: 'streamline', pron: '/ˈstriːm.laɪn/', type: 'v', mean: 'hợp lý hóa, tinh giản quy trình để tối ưu hiệu quả', ex: 'Automated pipelines were designed to streamline software deployment.' },
   { id: 'w4', word: 'meticulous', pron: '/məˈtɪk.jə.ləs/', type: 'adj', mean: 'tỉ mỉ, cẩn trọng trong từng chi tiết nhỏ', ex: 'The architectural blueprints were drafted with meticulous precision.' },
   { id: 'w5', word: 'counterproductive', pron: '/ˌkaʊn.tə.prəˈdʌk.tɪv/', type: 'adj', mean: 'phản tác dụng, đem lại kết quả ngược mong muốn', ex: 'Working through the night is often counterproductive to quality output.' },
   { id: 'w6', word: 'substantiate', pron: '/səbˈstæn.ʃi.eɪt/', type: 'v', mean: 'chứng minh, đưa ra chứng cứ xác thực', ex: 'Researchers must provide empirical data to substantiate their hypothesis.' },
   { id: 'w7', word: 'discrepancy', pron: '/dɪˈskrep.ən.si/', type: 'n', mean: 'sự khác biệt, sự không nhất quán giữa hai số liệu', ex: 'Auditors discovered a marked discrepancy between the two financial statements.' },
   { id: 'w8', word: 'ameliorate', pron: '/əˈmiːl.jə.reɪt/', type: 'v', mean: 'cải thiện, làm cho tốt hơn (tình huống xấu)', ex: 'New urban policies helped ameliorate living standards in overcrowded districts.' }
  ]
 },
 {
  id: 'deck_oxford_essential',
  title: 'Oxford 3000 Core Vocabulary - Giao Tiếp Hàng Ngày',
  category: 'Giao tiếp',
  description: 'Tổng hợp các từ và cụm từ thông dụng nhất trong giao tiếp đời sống, du lịch và làm việc.',
  authorName: 'Cộng đồng LifeOS',
  authorEmail: 'community@lifeos.io',
  ownerUid: 'system_curated',
  mode: 'public',
  allowedEmails: [],
  clones: 98,
  createdAt: '2026-09-02T10:00:00.000Z',
  updatedAt: '2026-09-02T10:00:00.000Z',
  words: [
   { id: 'w11', word: 'collaborate', pron: '/kəˈlæb.ə.reɪt/', type: 'v', mean: 'hợp tác, phối hợp cùng làm việc', ex: 'Our team collaborates with international partners across multiple timezones.' },
   { id: 'w12', word: 'feasible', pron: '/ˈfiː.zə.bəl/', type: 'adj', mean: 'khả thi, có thể thực hiện thành công', ex: 'Management will determine if the proposed budget is financially feasible.' },
   { id: 'w13', word: 'leverage', pron: '/ˈliː.vər.ɪdʒ/', type: 'v', mean: 'tận dụng, khai thác tối đa tiềm năng/lợi thế', ex: 'Modern businesses leverage artificial intelligence to gain competitive advantage.' },
   { id: 'w14', word: 'benchmark', pron: '/ˈbentʃ.mɑːk/', type: 'n', mean: 'tiêu chuẩn chuẩn mực để đối sánh', ex: 'Customer satisfaction scores serve as an essential benchmark for service teams.' },
   { id: 'w15', word: 'proactive', pron: '/prəʊˈæk.tɪv/', type: 'adj', mean: 'chủ động, tiên phong giải quyết vấn đề', ex: 'Companies need proactive measures rather than reactive responses.' }
  ]
 }
];

let currentVocabSubView = 'personal';
let currentCommunityTab = 'public';
let communityDecksCache = [];
let selectedShareMode = 'email';
let currentViewingDeck = null;

function switchVocabSubView(view) {
 currentVocabSubView = view;
 const btnPers = document.getElementById('btnTabVocabPersonal');
 const btnComm = document.getElementById('btnTabVocabCommunity');
 const secPers = document.getElementById('vocabViewPersonal');
 const secComm = document.getElementById('vocabViewCommunity');

 if (view === 'personal') {
  if (btnPers) { btnPers.classList.add('btn-p'); btnPers.classList.remove('btn-outline'); }
  if (btnComm) { btnComm.classList.remove('btn-p'); btnComm.classList.add('btn-outline'); }
  if (secPers) secPers.style.display = 'block';
  if (secComm) secComm.style.display = 'none';
  renderVocab();
 } else {
  if (btnComm) { btnComm.classList.add('btn-p'); btnComm.classList.remove('btn-outline'); }
  if (btnPers) { btnPers.classList.remove('btn-p'); btnPers.classList.add('btn-outline'); }
  if (secPers) secPers.style.display = 'none';
  if (secComm) secComm.style.display = 'block';
  fetchAndRenderCommunityDecks();
 }
 if (window.lucide) window.lucide.createIcons();
}

function switchCommunityTab(tab) {
 currentCommunityTab = tab;
 ['Public', 'Shared', 'Mine'].forEach(t => {
  const btn = document.getElementById('btnCommTab' + t);
  if (btn) btn.classList.remove('active');
 });
 if (tab === 'public') document.getElementById('btnCommTabPublic')?.classList.add('active');
 if (tab === 'shared_with_me') document.getElementById('btnCommTabShared')?.classList.add('active');
 if (tab === 'my_decks') document.getElementById('btnCommTabMine')?.classList.add('active');

 renderCommunityDecksUI();
}

function selectShareDeckMode(mode) {
 selectedShareMode = mode;
 const cardEmail = document.getElementById('cardModeEmail');
 const cardPub = document.getElementById('cardModePublic');
 const wrapEmails = document.getElementById('wrapAllowedEmails');

 if (mode === 'email') {
  cardEmail?.classList.add('active');
  cardPub?.classList.remove('active');
  if (wrapEmails) wrapEmails.style.display = 'block';
 } else {
  cardPub?.classList.add('active');
  cardEmail?.classList.remove('active');
  if (wrapEmails) wrapEmails.style.display = 'none';
 }
}

function updateShareDeckScopeInfo() {
 const scope = document.getElementById('shareDeckScope')?.value || 'all';
 const infoEl = document.getElementById('shareDeckScopeInfo');
 const allCount = (window.DB.vocab || []).length;
 const selCount = (typeof selectedVocabIds !== 'undefined') ? selectedVocabIds.size : 0;

 if (!infoEl) return;
 if (scope === 'selected') {
  if (selCount === 0) {
   infoEl.innerHTML = `<span style="color:var(--amber);">⚠️ Bạn chưa chọn từ nào ở chế độ "Chọn nhiều". Hãy chọn ít nhất 1 từ hoặc đổi sang "Toàn bộ từ".</span>`;
  } else {
   infoEl.textContent = `Sẽ chia sẻ ${selCount} từ vựng đang được chọn.`;
  }
 } else {
  infoEl.textContent = `Sẽ chia sẻ toàn bộ ${allCount} từ vựng hiện có trong kho cá nhân.`;
 }
}

function openShareVocabModal(fromSelectedOnly = false) {
 const titleInp = document.getElementById('shareDeckTitle');
 const descInp = document.getElementById('shareDeckDesc');
 const emailsInp = document.getElementById('shareDeckEmails');
 const resBox = document.getElementById('shareResultBox');
 const scopeSel = document.getElementById('shareDeckScope');

 if (titleInp) titleInp.value = '';
 if (descInp) descInp.value = '';
 if (emailsInp) emailsInp.value = '';
 if (resBox) resBox.style.display = 'none';

 const selCount = (typeof selectedVocabIds !== 'undefined') ? selectedVocabIds.size : 0;
 if (scopeSel) {
  scopeSel.value = (fromSelectedOnly || selCount > 0) ? 'selected' : 'all';
 }

 selectShareDeckMode('email');
 updateShareDeckScopeInfo();
 openModal('mShareVocab');
 if (window.lucide) window.lucide.createIcons();
}

async function submitShareVocabDeck() {
 const title = (document.getElementById('shareDeckTitle')?.value || '').trim();
 if (!title) { toast('Vui lòng nhập tên bộ từ vựng!', 'error'); return; }

 const category = document.getElementById('shareDeckCategory')?.value || 'Khác';
 const description = (document.getElementById('shareDeckDesc')?.value || '').trim();
 const scope = document.getElementById('shareDeckScope')?.value || 'all';

 let wordsToShare = [];
 const allVocs = window.DB.vocab || [];

 if (scope === 'selected') {
  const selIds = typeof selectedVocabIds !== 'undefined' ? selectedVocabIds : new Set();
  wordsToShare = allVocs.filter(v => selIds.has(v.id));
  if (!wordsToShare.length) {
   toast('Chưa có từ vựng nào được chọn! Hãy bật "Chọn nhiều" để chọn từ hoặc đổi phạm vi sang "Toàn bộ từ".', 'error');
   return;
  }
 } else {
  wordsToShare = [...allVocs];
  if (!wordsToShare.length) {
   toast('Kho từ vựng của bạn đang trống! Hãy thêm từ trước khi chia sẻ.', 'error');
   return;
  }
 }

 let allowedEmails = [];
 if (selectedShareMode === 'email') {
  const rawEmails = (document.getElementById('shareDeckEmails')?.value || '').trim();
  allowedEmails = rawEmails.split(/[,;\s]+/)
   .map(e => e.trim().toLowerCase())
   .filter(e => e && e.includes('@'));

  if (!allowedEmails.length) {
   toast('Vui lòng nhập ít nhất một địa chỉ Gmail hợp lệ để chia sẻ!', 'error');
   return;
  }
 }

 const btnSubmit = document.getElementById('btnSubmitShareDeck');
 if (btnSubmit) { btnSubmit.disabled = true; btnSubmit.textContent = 'Đang chia sẻ...'; }

 try {
  const user = window.currentUser || {};
  const currentUid = user.uid || 'local_user';
  const authorName = user.displayName || user.email || 'Người dùng LifeOS';
  const authorEmail = (user.email || '').toLowerCase();

  const deck = {
   id: 'deck_' + (window.uid ? window.uid() : Date.now().toString(36)),
   title,
   description,
   category,
   ownerUid: currentUid,
   authorName,
   authorEmail,
   mode: selectedShareMode,
   allowedEmails,
   words: wordsToShare,
   wordCount: wordsToShare.length,
   createdAt: new Date().toISOString(),
   updatedAt: new Date().toISOString(),
   clones: 0
  };

  // Save to Firebase Firestore if online
  if (!window.DEMO_MODE && window.db && window.doc && window.setDoc) {
   try {
    await window.setDoc(window.doc(window.db, 'shared_vocab_decks', deck.id), deck);
   } catch (e) {
    console.warn('[LifeOS Vocab Share] Firestore setDoc warning:', e.message);
   }
  }

  // Always keep a copy in localStorage for instant access & offline availability
  let localDecks = [];
  try { localDecks = JSON.parse(localStorage.getItem('lifeos_shared_vocab_decks') || '[]'); } catch {}
  localDecks.unshift(deck);
  localStorage.setItem('lifeos_shared_vocab_decks', JSON.stringify(localDecks));

  // Update in-memory cache
  communityDecksCache = [deck, ...communityDecksCache.filter(d => d.id !== deck.id)];

  toast(selectedShareMode === 'email' ? '✓ Đã chia sẻ bộ từ thành công qua Gmail!' : '✓ Đã đăng tải bộ từ lên Thư viện Online!', 'success');

  // Display result box with share link
  const resBox = document.getElementById('shareResultBox');
  const linkInp = document.getElementById('shareDeckLink');
  if (resBox && linkInp) {
   const shareUrl = window.location.origin + window.location.pathname + '?deck=' + deck.id;
   linkInp.value = shareUrl;
   resBox.style.display = 'block';
  }

  // Refresh community decks UI if visible
  renderCommunityDecksUI();
 } catch (err) {
  console.error('[LifeOS Vocab Share] Error submitting deck:', err);
  toast('Lỗi khi chia sẻ bộ từ: ' + (err.message || err), 'error');
 } finally {
  if (btnSubmit) { btnSubmit.disabled = false; btnSubmit.innerHTML = '<i data-lucide="share-2" class="ic-14"></i> <span>Tạo & Chia sẻ</span>'; }
  if (window.lucide) window.lucide.createIcons();
 }
}

function copyShareDeckLink() {
 const linkInp = document.getElementById('shareDeckLink');
 if (linkInp && linkInp.value) {
  navigator.clipboard.writeText(linkInp.value).then(() => {
   toast('Đã sao chép liên kết chia sẻ vào bộ nhớ tạm!', 'success');
  }).catch(() => {
   linkInp.select();
   document.execCommand('copy');
   toast('Đã sao chép liên kết!', 'success');
  });
 }
}

async function fetchAndRenderCommunityDecks() {
 const loadingEl = document.getElementById('communityDecksLoading');
 if (loadingEl) loadingEl.style.display = 'block';

 try {
  let localDecks = [];
  try { localDecks = JSON.parse(localStorage.getItem('lifeos_shared_vocab_decks') || '[]'); } catch {}

  const user = window.currentUser || {};
  const currentEmail = (user.email || '').toLowerCase();
  const currentUid = user.uid || '';

  let remoteDecks = [];

  if (!window.DEMO_MODE && window.db && window.collection && window.getDocs && window.query) {
   try {
    // 1. Query public decks
    const qPub = window.query(window.collection(window.db, 'shared_vocab_decks'), window.where('mode', '==', 'public'), window.limit(50));
    const snapPub = await window.getDocs(qPub);
    snapPub.forEach(doc => remoteDecks.push({ id: doc.id, ...doc.data() }));

    // 2. Query decks shared with this user's email
    if (currentEmail) {
     const qShared = window.query(window.collection(window.db, 'shared_vocab_decks'), window.where('allowedEmails', 'array-contains', currentEmail), window.limit(50));
     const snapShared = await window.getDocs(qShared);
     snapShared.forEach(doc => remoteDecks.push({ id: doc.id, ...doc.data() }));
    }

    // 3. Query decks owned by current user
    if (currentUid) {
     const qMine = window.query(window.collection(window.db, 'shared_vocab_decks'), window.where('ownerUid', '==', currentUid), window.limit(50));
     const snapMine = await window.getDocs(qMine);
     snapMine.forEach(doc => remoteDecks.push({ id: doc.id, ...doc.data() }));
    }
   } catch (e) {
    console.warn('[LifeOS Vocab Share] Firestore fetch warning:', e.message);
   }
  }

  // Merge default decks, local decks, and remote decks (dedup by ID)
  const map = new Map();
  DEFAULT_COMMUNITY_DECKS.forEach(d => map.set(d.id, d));
  localDecks.forEach(d => map.set(d.id, d));
  remoteDecks.forEach(d => map.set(d.id, d));

  communityDecksCache = Array.from(map.values());
  renderCommunityDecksUI();
 } catch (err) {
  console.error('[LifeOS Vocab Share] Error fetching decks:', err);
 } finally {
  if (loadingEl) loadingEl.style.display = 'none';
 }
}

function renderCommunityDecksUI() {
 const grid = document.getElementById('communityDecksGrid');
 if (!grid) return;

 const user = window.currentUser || {};
 const currentEmail = (user.email || '').toLowerCase();
 const currentUid = user.uid || '';

 const q = (document.getElementById('communitySearch')?.value || '').toLowerCase();
 const cat = document.getElementById('communityCategoryFilter')?.value || 'all';

 let list = communityDecksCache.filter(d => {
  if (currentCommunityTab === 'public') {
   return d.mode === 'public';
  } else if (currentCommunityTab === 'shared_with_me') {
   return d.mode === 'email' && Array.isArray(d.allowedEmails) && (
    (currentEmail && d.allowedEmails.includes(currentEmail)) ||
    d.allowedEmails.length > 0 // in demo mode show local email shared decks
   );
  } else if (currentCommunityTab === 'my_decks') {
   return (currentUid && d.ownerUid === currentUid) || (currentEmail && d.authorEmail === currentEmail) || d.ownerUid === 'local_user';
  }
  return true;
 });

 if (cat !== 'all') {
  list = list.filter(d => d.category === cat);
 }

 if (q) {
  list = list.filter(d =>
   (d.title || '').toLowerCase().includes(q) ||
   (d.description || '').toLowerCase().includes(q) ||
   (d.authorName || '').toLowerCase().includes(q) ||
   (d.authorEmail || '').toLowerCase().includes(q)
  );
 }

 if (!list.length) {
  let emptyMsg = 'Chưa có bộ từ vựng nào trong mục này.';
  if (currentCommunityTab === 'shared_with_me') {
   emptyMsg = `Chưa có ai chia sẻ bộ từ với Gmail ${currentEmail || 'của bạn'}. Khi bạn bè nhập email của bạn lúc chia sẻ, bộ từ sẽ xuất hiện tại đây!`;
  } else if (currentCommunityTab === 'my_decks') {
   emptyMsg = 'Bạn chưa chia sẻ bộ từ nào. Hãy bấm nút "Tạo bộ từ chia sẻ mới" ở trên để chia sẻ với bạn bè!';
  }

  grid.innerHTML = `
   <div class="empty-state" style="grid-column:1/-1; padding:40px 16px;">
    <div class="empty-state-icon"><i data-lucide="share-2" class="ic-24"></i></div>
    <div class="empty-state-title">${emptyMsg}</div>
    <div style="margin-top:14px;">
     <button class="btn btn-p btn-sm" onclick="openShareVocabModal()"><i data-lucide="plus" class="ic-14"></i> Chia sẻ bộ từ ngay</button>
    </div>
   </div>
  `;
  if (window.lucide) window.lucide.createIcons();
  return;
 }

 grid.innerHTML = list.map(d => {
  const isMine = (currentUid && d.ownerUid === currentUid) || (currentEmail && d.authorEmail === currentEmail) || d.ownerUid === 'local_user';
  const isPublic = d.mode === 'public';
  const badgeMode = isPublic
   ? `<span class="badge" style="background:rgba(0,229,255,0.15); color:var(--accent-cyan); font-size:11px; padding:2px 8px; border-radius:10px;"><i data-lucide="globe" style="width:11px;height:11px;margin-right:3px;"></i> Công khai</span>`
   : `<span class="badge" style="background:rgba(124,77,255,0.15); color:var(--accent-light); font-size:11px; padding:2px 8px; border-radius:10px;"><i data-lucide="mail" style="width:11px;height:11px;margin-right:3px;"></i> Gmail riêng</span>`;

  return `
   <div class="shared-deck-card" onclick="openDeckDetailModal('${d.id}')">
    <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:8px; margin-bottom:8px;">
     <div style="display:flex; gap:6px; flex-wrap:wrap; align-items:center;">
      ${badgeMode}
      <span class="badge" style="background:var(--surface); border:1px solid var(--border); font-size:11px; padding:2px 7px; border-radius:10px;">${d.category || 'Chung'}</span>
     </div>
     <span style="font-size:11.5px; font-weight:700; color:var(--accent-light);">${(d.words || []).length} từ</span>
    </div>

    <div style="font-size:15px; font-weight:700; color:var(--text-hi); margin-bottom:6px; line-height:1.3;">
     ${window.LifeOSData ? window.LifeOSData.escapeHtml(d.title) : d.title}
    </div>

    <div style="font-size:12px; color:var(--text3); line-height:1.4; margin-bottom:12px; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden;">
     ${d.description ? (window.LifeOSData ? window.LifeOSData.escapeHtml(d.description) : d.description) : 'Bộ thẻ từ vựng chia sẻ chất lượng cao.'}
    </div>

    <div style="font-size:11px; color:var(--text-low); margin-bottom:14px; display:flex; justify-content:space-between; align-items:center;">
     <span>Bởi: <b>${d.authorName || d.authorEmail || 'Người dùng'}</b></span>
     <span>${d.clones ? `📥 ${d.clones} lượt lưu` : ''}</span>
    </div>

    <div class="deck-card-actions" onclick="event.stopPropagation();" style="display:flex; gap:6px; align-items:center; border-top:1px solid var(--border); padding-top:10px;">
     <button class="btn btn-sm btn-p" onclick="reviewSharedVocabDeck('${d.id}')" style="flex:1; padding:4px 8px; font-size:11.5px; display:inline-flex; align-items:center; justify-content:center; gap:4px;" title="Học lật thẻ ngay">
      <i data-lucide="play" style="width:12px;height:12px;"></i> <span>Lật thẻ</span>
     </button>
     <button class="btn btn-sm btn-outline" onclick="startVocabTest('deck', '${d.id}')" style="padding:4px 8px; font-size:11.5px; display:inline-flex; align-items:center; gap:4px; border-color:var(--accent-cyan); color:var(--accent-cyan); background:rgba(0,229,255,0.06);" title="Kiểm tra gõ từ tiếng Anh theo nghĩa tiếng Việt">
      <i data-lucide="edit-3" style="width:12px;height:12px;"></i> <span>Test</span>
     </button>
     <button class="btn btn-sm btn-outline" onclick="importSharedDeckToPersonal('${d.id}')" style="padding:4px 8px; font-size:11.5px; display:inline-flex; align-items:center; gap:4px;" title="Lưu từ vào kho cá nhân">
      <i data-lucide="download" style="width:12px;height:12px;"></i> <span>Lưu về</span>
     </button>
     ${isMine ? `
      <button class="btn btn-sm btn-r" onclick="deleteSharedDeck('${d.id}')" style="padding:4px 7px;" title="Xóa / Gỡ chia sẻ">
       <i data-lucide="trash-2" style="width:12px;height:12px;"></i>
      </button>
     ` : ''}
    </div>
   </div>
  `;
 }).join('');

 if (window.lucide) window.lucide.createIcons();
}

function openDeckDetailModal(deckId) {
 const deck = communityDecksCache.find(d => d.id === deckId);
 if (!deck) return;
 currentViewingDeck = deck;

 const titleEl = document.getElementById('ddTitle');
 const metaEl = document.getElementById('ddMeta');
 const descEl = document.getElementById('ddDesc');
 const listEl = document.getElementById('ddWordsList');

 if (titleEl) titleEl.textContent = deck.title;
 if (metaEl) metaEl.textContent = `Tác giả: ${deck.authorName || deck.authorEmail || 'Người dùng'} • ${(deck.words || []).length} từ vựng • ${deck.category || 'Chung'}`;
 if (descEl) descEl.textContent = deck.description || '';

 if (listEl) {
  const words = deck.words || [];
  if (!words.length) {
   listEl.innerHTML = '<div style="color:var(--text3); font-size:12px; text-align:center; padding:20px;">Bộ từ vựng này chưa có từ nào.</div>';
  } else {
   listEl.innerHTML = words.map(w => `
    <div style="background:var(--surface); border:1px solid var(--border); border-radius:8px; padding:10px 14px; display:flex; flex-direction:column; gap:4px;">
     <div style="display:flex; justify-content:space-between; align-items:center;">
      <div style="display:flex; align-items:center; gap:8px;">
       <span style="font-weight:700; font-size:14px; color:var(--accent);">${w.word}</span>
       <span style="font-size:11px; color:var(--text3);">(${w.type || 'n'})</span>
       ${w.pron ? `<span style="font-size:11px; color:var(--text2);">${w.pron}</span>` : ''}
      </div>
      <button class="btn-audio" onclick="speakWord('${(w.word || '').replace(/'/g, "\\'")}')" style="width:22px; height:22px;">
       <i data-lucide="volume-2" style="width:12px;height:12px;"></i>
      </button>
     </div>
     <div style="font-size:13px; color:var(--text-hi); font-weight:500;">${w.mean}</div>
     ${w.ex ? `<div style="font-size:11.5px; color:var(--text3); font-style:italic; margin-top:2px;">"${w.ex}"</div>` : ''}
    </div>
   `).join('');
  }
 }

 openModal('mDeckDetail');
 if (window.lucide) window.lucide.createIcons();
}

async function cloneCurrentDeckToPersonal() {
 if (!currentViewingDeck) return;
 await importSharedDeckToPersonal(currentViewingDeck.id);
 closeModal('mDeckDetail');
}

function studyCurrentDeck() {
 if (!currentViewingDeck) return;
 closeModal('mDeckDetail');
 reviewSharedVocabDeck(currentViewingDeck.id);
}

function testCurrentDeck() {
 if (!currentViewingDeck) return;
 closeModal('mDeckDetail');
 startVocabTest('deck', currentViewingDeck.id);
}

function reviewSharedVocabDeck(deckId) {
 const deck = communityDecksCache.find(d => d.id === deckId);
 if (!deck || !deck.words || !deck.words.length) {
  toast('Bộ từ này không có từ vựng để ôn tập!', 'error');
  return;
 }

 reviewList = [...deck.words];
 reviewList.sort(() => Math.random() - 0.5);
 reviewIdx = 0;
 updateReviewUI();
 document.querySelector('.flashcard-container')?.classList.remove('flipped');
 openModal('mReviewVocab');

 const cntEl = document.getElementById('rvCount');
 if (cntEl) cntEl.textContent = `Học chung: ${deck.title} (1/${reviewList.length})`;
 if (window.lucide) window.lucide.createIcons();
}

async function importSharedDeckToPersonal(deckId) {
 const deck = communityDecksCache.find(d => d.id === deckId);
 if (!deck || !deck.words || !deck.words.length) {
  toast('Bộ từ vựng này không có dữ liệu để lưu!', 'error');
  return;
 }

 const currentList = window.DB.vocab || [];
 const existingWords = new Set(currentList.map(v => (v.word || '').toLowerCase().trim()));

 let addedCount = 0;
 const newVocs = [];

 deck.words.forEach(w => {
  const normWord = (w.word || '').toLowerCase().trim();
  if (normWord && !existingWords.has(normWord)) {
   newVocs.push({
    id: window.uid ? window.uid() : 'v_' + Math.random().toString(36).substr(2, 9),
    word: w.word,
    mean: w.mean,
    pron: w.pron || '',
    ex: w.ex || '',
    type: w.type || 'n',
    date: window.today ? window.today() : new Date().toISOString().split('T')[0]
   });
   existingWords.add(normWord);
   addedCount++;
  }
 });

 if (addedCount === 0) {
  toast('Tất cả các từ trong bộ này đã có sẵn trong kho từ vựng của bạn!', 'info');
  return;
 }

 window.DB.vocab = [...newVocs, ...currentList];
 await persist('vocab', window.DB.vocab);
 renderVocab();

 // Increment clones count
 deck.clones = (deck.clones || 0) + 1;
 if (!window.DEMO_MODE && window.db && window.doc && window.updateDoc) {
  try {
   await window.updateDoc(window.doc(window.db, 'shared_vocab_decks', deck.id), {
    clones: deck.clones
   });
  } catch {}
 }

 toast(`🎉 Đã lưu thành công ${addedCount} từ mới vào kho cá nhân của bạn!`, 'success');
}

async function deleteSharedDeck(deckId) {
 const deck = communityDecksCache.find(d => d.id === deckId);
 if (!deck) return;

 if (!confirm(`Bạn có chắc muốn xóa và ngừng chia sẻ bộ từ "${deck.title}"?`)) return;

 communityDecksCache = communityDecksCache.filter(d => d.id !== deckId);

 // Remove from localStorage
 try {
  let localDecks = JSON.parse(localStorage.getItem('lifeos_shared_vocab_decks') || '[]');
  localDecks = localDecks.filter(d => d.id !== deckId);
  localStorage.setItem('lifeos_shared_vocab_decks', JSON.stringify(localDecks));
 } catch {}

 // Remove from Firestore
 if (!window.DEMO_MODE && window.db && window.doc && window.deleteDoc) {
  try {
   await window.deleteDoc(window.doc(window.db, 'shared_vocab_decks', deckId));
  } catch (e) {
   console.warn('[LifeOS Vocab Share] Error deleting from firestore:', e);
  }
 }

 toast('Đã xóa bộ từ vựng chia sẻ!', 'info');
 renderCommunityDecksUI();
}


// ════════════════════════════════════════════════════════════
// VOCABULARY TEST / QUIZ (TYPING TEST: VIETNAMESE -> ENGLISH)
// ════════════════════════════════════════════════════════════

let testVocabList = [];
let testOriginalList = [];
let testCurrentIdx = 0;
let testScore = 0;
let testWrongList = [];
let testHistory = [];
let testIsAnswered = false;
let testSourceTitle = '';

function normalizeVocabWord(str) {
 if (!str) return '';
 return str.toLowerCase()
   .replace(/\s*\([^)]*\)/g, '') // remove parentheticals
   .replace(/[.,\/#!$%\^&\*;:{}=\-_\`~()?]/g, ' ') // punctuation to space
   .replace(/\s+/g, ' ')
   .trim();
}

function startVocabTest(sourceType, deckIdOrList) {
 let source = [];
 let title = 'Kho từ vựng cá nhân';

 if (sourceType === 'personal') {
  source = window.DB.vocab || [];
  title = 'Kho từ vựng cá nhân';
 } else if (sourceType === 'selected') {
  const all = window.DB.vocab || [];
  source = all.filter(v => selectedVocabIds.has(v.id));
  title = `Đã chọn (${source.length} từ)`;
 } else if (sourceType === 'deck') {
  const deck = communityDecksCache.find(d => d.id === deckIdOrList);
  if (!deck || !deck.words || !deck.words.length) {
   toast('Bộ từ vựng này không có dữ liệu để kiểm tra!', 'error');
   return;
  }
  source = deck.words;
  title = deck.title;
 } else if (sourceType === 'custom') {
  source = Array.isArray(deckIdOrList) ? deckIdOrList : [];
  title = 'Ôn luyện từ sai';
 }

 if (!source || !source.length) {
  toast('Chưa có từ vựng nào để làm bài kiểm tra!', 'error');
  return;
 }

 testOriginalList = [...source];
 testVocabList = [...source].sort(() => Math.random() - 0.5);
 testCurrentIdx = 0;
 testScore = 0;
 testWrongList = [];
 testHistory = [];
 testIsAnswered = false;
 testSourceTitle = title;

 const headerSub = document.getElementById('vtHeaderSubtitle');
 if (headerSub) headerSub.textContent = title;

 const playArea = document.getElementById('vTestPlayArea');
 const sumArea = document.getElementById('vTestSummaryArea');
 if (playArea) playArea.style.display = 'block';
 if (sumArea) sumArea.style.display = 'none';

 openModal('mTestVocab');
 renderTestQuestion();
 if (window.lucide) window.lucide.createIcons();
}

function renderTestQuestion() {
 if (testCurrentIdx >= testVocabList.length) {
  finishVocabTest();
  return;
 }

 testIsAnswered = false;
 const curr = testVocabList[testCurrentIdx];
 const total = testVocabList.length;

 const scoreBadge = document.getElementById('vtScoreBadge');
 if (scoreBadge) scoreBadge.textContent = `${testScore}/${total} đúng`;

 const progBar = document.getElementById('vtProgressBar');
 if (progBar) {
  const pct = Math.round((testCurrentIdx / total) * 100);
  progBar.style.width = `${pct}%`;
 }

 const qCount = document.getElementById('vtQuestionCount');
 if (qCount) qCount.textContent = `Câu hỏi ${testCurrentIdx + 1}/${total}`;

 const typeBadge = document.getElementById('vtWordTypeBadge');
 if (typeBadge) typeBadge.textContent = '(' + (curr.type || 'n') + ')';

 const meanEl = document.getElementById('vtPromptMeaning');
 if (meanEl) meanEl.textContent = curr.mean || '(Chưa có nghĩa)';

 const contextWrap = document.getElementById('vtContextWrap');
 const contextSentence = document.getElementById('vtContextSentence');
 if (curr.ex && curr.ex.trim()) {
  let sentence = curr.ex.trim();
  const cleanWord = (curr.word || '').trim();
  const escWord = cleanWord.replace(/[.*+?^${}()|[\]\\]/g, '\\// Auto-check deep link (?deck=...) on startup');
  const regex = new RegExp('\\b' + escWord + '(\\w*)', 'gi');
  if (regex.test(sentence)) {
   sentence = sentence.replace(regex, '_______');
  } else {
   sentence = sentence.replace(new RegExp(escWord, 'gi'), '_______');
  }
  if (contextSentence) contextSentence.textContent = sentence;
  if (contextWrap) contextWrap.style.display = 'block';
 } else {
  if (contextWrap) contextWrap.style.display = 'none';
 }

 const hintBtn = document.getElementById('btnVtHint');
 const hintTxt = document.getElementById('txtVtHint');
 const letterEl = document.getElementById('vtLetterCount');
 if (hintBtn) hintBtn.disabled = false;
 if (hintTxt) hintTxt.textContent = '💡 Gợi ý chữ cái';
 if (letterEl) {
  const len = (curr.word || '').trim().length;
  letterEl.textContent = `${len} chữ cái`;
 }

 const input = document.getElementById('vocabTestInput');
 if (input) {
  input.value = '';
  input.disabled = false;
  input.className = 'vtest-input';
  setTimeout(() => {
   input.focus();
  }, 100);
 }

 const preActions = document.getElementById('vtActionsPreCheck');
 const feedbackBox = document.getElementById('vtFeedbackBox');
 if (preActions) preActions.style.display = 'flex';
 if (feedbackBox) feedbackBox.style.display = 'none';

 if (window.lucide) window.lucide.createIcons();
}

function showTestHint() {
 const curr = testVocabList[testCurrentIdx];
 if (!curr) return;
 const word = (curr.word || '').trim();
 if (!word) return;

 const firstChar = word.charAt(0).toUpperCase();
 const len = word.length;
 const hintPattern = `${firstChar} ` + `_ `.repeat(Math.max(0, len - 1)).trim() + ` (${len} chữ cái)`;

 const letterEl = document.getElementById('vtLetterCount');
 if (letterEl) letterEl.textContent = hintPattern;

 const hintBtn = document.getElementById('btnVtHint');
 const hintTxt = document.getElementById('txtVtHint');
 if (hintBtn) hintBtn.disabled = true;
 if (hintTxt) hintTxt.textContent = 'Đã mở gợi ý';

 document.getElementById('vocabTestInput')?.focus();
}

function checkTestAnswer() {
 if (testIsAnswered) return;
 const curr = testVocabList[testCurrentIdx];
 if (!curr) return;

 const inputEl = document.getElementById('vocabTestInput');
 const rawInput = inputEl ? inputEl.value.trim() : '';

 if (!rawInput) {
  toast('Vui lòng gõ từ tiếng Anh hoặc bấm "Bỏ qua"!', 'info');
  inputEl?.focus();
  return;
 }

 testIsAnswered = true;
 const normUser = normalizeVocabWord(rawInput);
 const normCorrect = normalizeVocabWord(curr.word);

 const alternatives = (curr.word || '').split(/[\/,]/).map(w => normalizeVocabWord(w));
 const isCorrect = (normUser === normCorrect) || alternatives.includes(normUser);

 testHistory.push({
  word: curr.word,
  mean: curr.mean,
  pron: curr.pron || '',
  type: curr.type || 'n',
  ex: curr.ex || '',
  userAns: rawInput,
  isCorrect: isCorrect
 });

 if (inputEl) inputEl.disabled = true;

 const preActions = document.getElementById('vtActionsPreCheck');
 const feedbackBox = document.getElementById('vtFeedbackBox');
 const fbIcon = document.getElementById('vtFeedbackIcon');
 const fbTitle = document.getElementById('vtFeedbackTitle');
 const fbDetail = document.getElementById('vtFeedbackDetail');
 const nextTxt = document.getElementById('txtVtNext');

 if (preActions) preActions.style.display = 'none';
 if (feedbackBox) feedbackBox.style.display = 'block';

 const isLast = testCurrentIdx >= testVocabList.length - 1;
 if (nextTxt) nextTxt.textContent = isLast ? 'Xem kết quả' : 'Câu tiếp theo';

 if (isCorrect) {
  testScore++;
  if (feedbackBox) feedbackBox.className = 'vtest-feedback-box correct';
  if (fbIcon) fbIcon.textContent = '🎉';
  if (fbTitle) fbTitle.textContent = 'Chính xác! Rất xuất sắc!';
  if (fbDetail) {
   fbDetail.innerHTML = `<b style="font-size:15px; color:#2ed573;">${curr.word}</b> ${curr.pron ? `<span style="color:var(--text2); font-size:12px; margin-left:6px;">${curr.pron}</span>` : ''}`;
  }
  speakWord(curr.word);
 } else {
  testWrongList.push(curr);
  if (feedbackBox) feedbackBox.className = 'vtest-feedback-box wrong';
  if (fbIcon) fbIcon.textContent = '❌';
  if (fbTitle) fbTitle.textContent = 'Chưa chính xác!';
  if (fbDetail) {
   fbDetail.innerHTML = `Bạn gõ: <s style="opacity:0.8;">${rawInput}</s> &bull; Đáp án đúng: <b style="font-size:15px; color:var(--text-hi); margin-left:4px;">${curr.word}</b> ${curr.pron ? `<span style="color:var(--text2); font-size:12px; margin-left:4px;">${curr.pron}</span>` : ''}`;
  }
 }

 const scoreBadge = document.getElementById('vtScoreBadge');
 if (scoreBadge) scoreBadge.textContent = `${testScore}/${testVocabList.length} đúng`;

 document.getElementById('btnVtNext')?.focus();
 if (window.lucide) window.lucide.createIcons();
}

function skipTestQuestion() {
 if (testIsAnswered) return;
 const curr = testVocabList[testCurrentIdx];
 if (!curr) return;

 testIsAnswered = true;
 testWrongList.push(curr);
 testHistory.push({
  word: curr.word,
  mean: curr.mean,
  pron: curr.pron || '',
  type: curr.type || 'n',
  ex: curr.ex || '',
  userAns: '(Bỏ qua)',
  isCorrect: false
 });

 const inputEl = document.getElementById('vocabTestInput');
 if (inputEl) inputEl.disabled = true;

 const preActions = document.getElementById('vtActionsPreCheck');
 const feedbackBox = document.getElementById('vtFeedbackBox');
 const fbIcon = document.getElementById('vtFeedbackIcon');
 const fbTitle = document.getElementById('vtFeedbackTitle');
 const fbDetail = document.getElementById('vtFeedbackDetail');
 const nextTxt = document.getElementById('txtVtNext');

 if (preActions) preActions.style.display = 'none';
 if (feedbackBox) {
  feedbackBox.style.display = 'block';
  feedbackBox.className = 'vtest-feedback-box wrong';
 }
 if (fbIcon) fbIcon.textContent = '💡';
 if (fbTitle) fbTitle.textContent = 'Đáp án đúng:';
 if (fbDetail) {
  fbDetail.innerHTML = `<b style="font-size:15px; color:var(--text-hi);">${curr.word}</b> ${curr.pron ? `<span style="color:var(--text2); font-size:12px; margin-left:4px;">${curr.pron}</span>` : ''}`;
 }

 const isLast = testCurrentIdx >= testVocabList.length - 1;
 if (nextTxt) nextTxt.textContent = isLast ? 'Xem kết quả' : 'Câu tiếp theo';

 document.getElementById('btnVtNext')?.focus();
 if (window.lucide) window.lucide.createIcons();
}

function nextTestQuestion() {
 testCurrentIdx++;
 renderTestQuestion();
}

function speakCurrentTestWord() {
 const curr = testVocabList[testCurrentIdx];
 if (curr && curr.word) speakWord(curr.word);
}

function handleVocabTestSubmit() {
 if (!testIsAnswered) {
  checkTestAnswer();
 } else {
  nextTestQuestion();
 }
}

function finishVocabTest() {
 const progBar = document.getElementById('vtProgressBar');
 if (progBar) progBar.style.width = '100%';

 const playArea = document.getElementById('vTestPlayArea');
 const sumArea = document.getElementById('vTestSummaryArea');
 if (playArea) playArea.style.display = 'none';
 if (sumArea) sumArea.style.display = 'block';

 const total = testVocabList.length;
 const pct = Math.round((testScore / Math.max(1, total)) * 100);

 const scoreEl = document.getElementById('vtSummaryScore');
 if (scoreEl) scoreEl.textContent = `${testScore} / ${total}`;

 const pctEl = document.getElementById('vtSummaryPercent');
 if (pctEl) pctEl.textContent = `Tỉ lệ chính xác: ${pct}%`;

 const badgeEl = document.getElementById('vtSummaryBadge');
 const titleEl = document.getElementById('vtSummaryTitle');

 if (pct >= 80) {
  if (badgeEl) badgeEl.textContent = '🏆';
  if (titleEl) titleEl.textContent = 'Xuất sắc! Bạn nhớ từ rất tốt! 🔥';
  toast(`🎉 Xuất sắc! Bạn đạt ${pct}% trong bài kiểm tra từ vựng!`, 'success');
 } else if (pct >= 50) {
  if (badgeEl) badgeEl.textContent = '👍';
  if (titleEl) titleEl.textContent = 'Khá tốt! Hãy tiếp tục rèn luyện nhé! ✨';
 } else {
  if (badgeEl) badgeEl.textContent = '💪';
  if (titleEl) titleEl.textContent = 'Cần luyện tập thêm! Hãy ôn lại các từ sai nhé! 🎯';
 }

 const listEl = document.getElementById('vtSummaryList');
 const countEl = document.getElementById('vtSummaryCount');
 if (countEl) countEl.textContent = `${testScore} đúng • ${testWrongList.length} sai`;

 if (listEl) {
  listEl.innerHTML = testHistory.map(h => `
   <div class="vtest-word-row" style="border-left:3px solid ${h.isCorrect ? '#2ed573' : '#ff4757'};">
    <div style="display:flex; flex-direction:column; gap:2px;">
     <div style="display:flex; align-items:center; gap:8px;">
      <span style="font-weight:700; font-size:14px; color:var(--text-hi);">${h.word}</span>
      <span style="font-size:11px; color:var(--text3);">(${h.type})</span>
      ${h.pron ? `<span style="font-size:11px; color:var(--text2);">${h.pron}</span>` : ''}
     </div>
     <div style="font-size:12px; color:var(--text2);">${h.mean}</div>
     ${!h.isCorrect ? `<div style="font-size:11.5px; color:#ff4757;">Bạn đã gõ: <s>${h.userAns}</s></div>` : ''}
    </div>
    <div style="display:flex; align-items:center; gap:8px;">
     <span style="font-size:16px;">${h.isCorrect ? '✅' : '❌'}</span>
     <button class="btn-audio" onclick="speakWord('${(h.word || '').replace(/'/g, "\\'")}')" style="width:24px; height:24px;">
      <i data-lucide="volume-2" style="width:13px; height:13px;"></i>
     </button>
    </div>
   </div>
  `).join('');
 }

 const retryWrongBtn = document.getElementById('btnVtRetryWrong');
 const wrongCountTxt = document.getElementById('txtVtWrongCount');
 if (retryWrongBtn && wrongCountTxt) {
  if (testWrongList.length > 0) {
   retryWrongBtn.style.display = 'inline-flex';
   wrongCountTxt.textContent = testWrongList.length;
  } else {
   retryWrongBtn.style.display = 'none';
  }
 }

 if (window.lucide) window.lucide.createIcons();
}

function retryWrongVocabTest() {
 if (!testWrongList.length) {
  toast('Bạn không có từ nào làm sai!', 'info');
  return;
 }
 startVocabTest('custom', [...testWrongList]);
}

function restartVocabTest() {
 startVocabTest('custom', [...testOriginalList]);
}

function closeVocabTestModal() {
 if (testCurrentIdx < testVocabList.length && !testIsAnswered && testCurrentIdx > 0) {
  if (!confirm('Bạn có chắc muốn dừng bài kiểm tra từ vựng đang làm dở?')) return;
 }
 closeModal('mTestVocab');
}

// Auto-check deep link (?deck=...) on startup
setTimeout(() => {
 try {
  const urlParams = new URLSearchParams(window.location.search);
  const deckId = urlParams.get('deck');
  if (deckId) {
   if (typeof nav === 'function') nav('vocab');
   switchVocabSubView('community');
   fetchAndRenderCommunityDecks().then(() => {
    openDeckDetailModal(deckId);
   });
  }
 } catch {}
}, 500);

window.switchVocabSubView = switchVocabSubView;
window.switchCommunityTab = switchCommunityTab;
window.selectShareDeckMode = selectShareDeckMode;
window.updateShareDeckScopeInfo = updateShareDeckScopeInfo;
window.openShareVocabModal = openShareVocabModal;
window.submitShareVocabDeck = submitShareVocabDeck;
window.copyShareDeckLink = copyShareDeckLink;
window.fetchAndRenderCommunityDecks = fetchAndRenderCommunityDecks;
window.renderCommunityDecksUI = renderCommunityDecksUI;
window.openDeckDetailModal = openDeckDetailModal;
window.cloneCurrentDeckToPersonal = cloneCurrentDeckToPersonal;
window.studyCurrentDeck = studyCurrentDeck;
window.reviewSharedVocabDeck = reviewSharedVocabDeck;
window.importSharedDeckToPersonal = importSharedDeckToPersonal;
window.deleteSharedDeck = deleteSharedDeck;
window.startVocabTest = startVocabTest;
window.testCurrentDeck = testCurrentDeck;
window.renderTestQuestion = renderTestQuestion;
window.showTestHint = showTestHint;
window.checkTestAnswer = checkTestAnswer;
window.skipTestQuestion = skipTestQuestion;
window.nextTestQuestion = nextTestQuestion;
window.speakCurrentTestWord = speakCurrentTestWord;
window.handleVocabTestSubmit = handleVocabTestSubmit;
window.retryWrongVocabTest = retryWrongVocabTest;
window.restartVocabTest = restartVocabTest;
window.closeVocabTestModal = closeVocabTestModal;

