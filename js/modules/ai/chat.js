/* ════════════════════════════════════════════════════════════
  LIFEOS AI ASSISTANT & CHAT ENGINE (GEMINI PRO / FLASH)
════════════════════════════════════════════════════════════ */

import { AI_TOOL_DECLARATIONS, executeAiTool } from './tools.js';
import { buildAiSystemPrompt } from './context.js';

let aiChatHistory = [];
const DEFAULT_GEMINI_KEY = "";

// ── Markdown Formatter ──
function formatMarkdown(text) {
  if (!text) return '';
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  // Bold & Italic
  html = html.replace(/\*\*\*(.*?)\*\*\*/g, '<b><i>$1</i></b>');
  html = html.replace(/\*\*(.*?)\*\*/g, '<b>$1</b>');
  html = html.replace(/\*(.*?)\*/g, '<i>$1</i>');
  
  // Inline Code
  html = html.replace(/`([^`]+)`/g, '<code style="background:rgba(255,255,255,0.08);padding:2px 6px;border-radius:4px;font-family:monospace;font-size:12px;color:var(--accent-cyan);">$1</code>');

  // Bullet Lists
  html = html.replace(/^\s*[\-\*]\s+(.*)$/gim, '<li style="margin-left:18px;margin-bottom:4px;">$1</li>');
  
  // Numbered Lists
  html = html.replace(/^\s*(\d+)\.\s+(.*)$/gim, '<li style="margin-left:18px;margin-bottom:4px;" value="$1">$2</li>');

  // Paragraphs / Line Breaks
  html = html.replace(/\n\n/g, '<div style="margin-bottom:10px;"></div>');
  html = html.replace(/\n/g, '<br>');

  return html;
}

// ── Local Storage History ──
function saveAiHistory() {
  localStorage.setItem('lifeos_ai_history', JSON.stringify(aiChatHistory.slice(-30)));
}

function loadAiHistory() {
  try {
    const saved = localStorage.getItem('lifeos_ai_history');
    if (saved) {
      aiChatHistory = JSON.parse(saved);
      const c = document.getElementById('aiMessages');
      if (c) {
        while (c.children.length > 1) { c.removeChild(c.lastChild); }
        aiChatHistory.forEach(msg => {
          if (msg.role === 'user' && msg.parts && msg.parts[0]?.text) {
            appendMessage('user', msg.parts[0].text, false);
          } else if (msg.role === 'model' && msg.parts && msg.parts[0]?.text) {
            appendMessage('ai', msg.parts[0].text, false);
          }
        });
      }
    }
  } catch(e) {}
}

window.clearAiHistory = function() {
  aiChatHistory = [];
  localStorage.removeItem('lifeos_ai_history');
  const c = document.getElementById('aiMessages');
  if (c) {
    while (c.children.length > 1) { c.removeChild(c.lastChild); }
  }
  toast('Đã xóa lịch sử trò chuyện AI', 'info');
};

// ── UI Modal Controls ──
window.toggleAiChat = function() {
  const m = document.getElementById('aiModal');
  if (!m) return;
  if (m.style.display === 'flex') {
    m.style.display = 'none';
  } else {
    m.style.display = 'flex';
    setTimeout(() => {
      const inp = document.getElementById('aiInput');
      if (inp) inp.focus();
    }, 100);
  }
};

window.closeAiModal = function() {
  const m = document.getElementById('aiModal');
  if (m) m.style.display = 'none';
};

window.openAiKeyModal = function() {
  const cur = localStorage.getItem('lifeos_gemini_key') || '';
  const key = prompt('Nhập Gemini API Key của bạn (miễn phí tại https://aistudio.google.com):', cur);
  if (key !== null) {
    const trimmed = key.trim();
    localStorage.setItem('lifeos_gemini_key', trimmed);
    if (trimmed) {
      toast('Đã lưu Gemini API Key!', 'success');
      appendMessage('ai', '✅ **Đã kết nối Gemini API Key thành công!** Bạn có thể ra lệnh và trò chuyện tự nhiên ngay bây giờ.');
    } else {
      toast('Đã xóa Gemini API Key', 'info');
    }
  }
};

function getApiKey() {
  return localStorage.getItem('lifeos_gemini_key') || window.DEMO_GEMINI_KEY || DEFAULT_GEMINI_KEY;
}

// ── Append Message to UI ──
function appendMessage(sender, text, isActionChip = false) {
  const c = document.getElementById('aiMessages');
  if (!c) return;

  const msgDiv = document.createElement('div');
  msgDiv.className = `ai-msg ${sender === 'user' ? 'ai-msg-user' : 'ai-msg-bot'}`;
  
  if (sender === 'user') {
    msgDiv.style.cssText = 'align-self:flex-end; background:var(--accent); color:#fff; padding:10px 14px; border-radius:14px 14px 2px 14px; max-width:85%; font-size:13.5px; line-height:1.5; word-break:break-word; margin-bottom:8px; box-shadow:0 2px 8px rgba(124,77,255,0.3);';
    msgDiv.textContent = text;
  } else {
    msgDiv.style.cssText = 'align-self:flex-start; background:var(--surface); border:1px solid var(--border); color:var(--text-hi); padding:12px 16px; border-radius:14px 14px 14px 2px; max-width:90%; font-size:13.5px; line-height:1.6; word-break:break-word; margin-bottom:8px;';
    msgDiv.innerHTML = formatMarkdown(text);
  }

  c.appendChild(msgDiv);
  c.scrollTop = c.scrollHeight;
  return msgDiv;
}

function appendActionChip(title) {
  const c = document.getElementById('aiMessages');
  if (!c) return;
  const chip = document.createElement('div');
  chip.style.cssText = 'display:inline-flex; align-items:center; gap:6px; background:rgba(0,230,118,0.12); border:1px solid rgba(0,230,118,0.3); color:var(--success); padding:4px 10px; border-radius:20px; font-size:11.5px; font-weight:600; margin:4px 0 8px;';
  chip.innerHTML = `<i data-lucide="check" class="ic-14"></i> ${title}`;
  c.appendChild(chip);
  if (window.lucide) window.lucide.createIcons();
  c.scrollTop = c.scrollHeight;
}

function appendApiKeyPromptCard() {
  const c = document.getElementById('aiMessages');
  if (!c) return;

  const card = document.createElement('div');
  card.style.cssText = 'background:linear-gradient(135deg, rgba(124,77,255,0.12), rgba(0,229,255,0.06)); border:1px solid rgba(124,77,255,0.3); border-radius:12px; padding:14px; margin-top:8px;';
  card.innerHTML = `
    <div style="font-weight:700; font-size:13.5px; color:var(--accent-light); margin-bottom:6px; display:flex; align-items:center; gap:6px;">
      <i data-lucide="key" class="ic-16"></i> Kích hoạt Google Gemini AI
    </div>
    <div style="font-size:12.5px; color:var(--text-mid); line-height:1.5; margin-bottom:10px;">
      Để kích hoạt khả năng suy luận ngôn ngữ tự nhiên không giới hạn, bạn chỉ cần nhập <b>Gemini API Key</b> cá nhân (hoàn toàn miễn phí).
    </div>
    <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
      <button class="btn btn-p btn-sm" onclick="openAiKeyModal()"><i data-lucide="key" class="ic-14"></i> Nhập Gemini API Key</button>
      <a href="https://aistudio.google.com/app/apikey" target="_blank" class="btn btn-outline btn-sm" style="text-decoration:none;"><i data-lucide="external-link" class="ic-14"></i> Lấy Key Miễn Phí</a>
    </div>
  `;
  c.appendChild(card);
  if (window.lucide) window.lucide.createIcons();
  c.scrollTop = c.scrollHeight;
}

// ── Multi-turn Gemini API Core ──
const MODELS = [
  'gemini-1.5-flash-latest',
  'gemini-1.5-flash',
  'gemini-2.0-flash',
  'gemini-2.5-flash',
  'gemini-1.5-pro-latest'
];

async function callGeminiRaw(contents, systemInstruction, tools) {
  const apiKey = getApiKey();
  if (!apiKey) {
    throw new Error('CHUA_CO_KEY');
  }

  let lastError = null;
  for (const model of MODELS) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
    const payload = {
      contents,
      systemInstruction: { parts: [{ text: systemInstruction }] }
    };
    if (tools && tools.length > 0) {
      payload.tools = [{ functionDeclarations: tools }];
    }

    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.status === 400 || res.status === 403) {
        const errJson = await res.json().catch(() => ({}));
        const msg = errJson.error?.message || '';
        if (msg.toLowerCase().includes('api key') || msg.toLowerCase().includes('api_key') || msg.toLowerCase().includes('invalid') || msg.toLowerCase().includes('permission_denied') || msg.toLowerCase().includes('has not been used in project')) {
          throw new Error('KEY_KHONG_HOP_LE');
        }
        console.warn(`Model ${model} returned 400 (${msg}), trying next...`);
        lastError = new Error(`Model ${model}: ${msg}`);
        continue;
      }

      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        const msg = errJson.error?.message || (await res.text().catch(() => `HTTP ${res.status}`));
        if (msg.includes('not found for API version') || msg.includes('NOT_FOUND') || msg.includes('PERMISSION_DENIED')) {
          throw new Error('KEY_KHONG_HOP_LE');
        }
        console.warn(`Model ${model} returned ${res.status} (${msg}), trying next model...`);
        lastError = new Error(msg);
        continue;
      }

      const data = await res.json();
      return data;
    } catch (err) {
      lastError = err;
      if (err.message === 'KEY_KHONG_HOP_LE' || err.message === 'CHUA_CO_KEY') throw err;
      console.warn(`Error calling model ${model}:`, err.message);
    }
  }
  throw lastError || new Error('CHUA_CO_KEY');
}

// ── Smart Local Heuristic Engine (Offline / No Key Fallback) ──
async function executeLocalHeuristicAi(text) {
  const t = text.toLowerCase().trim();
  
  // 1. Pomodoro
  if (t.includes('pomodoro') || t.includes('tập trung') || t.includes('hẹn giờ')) {
    if (t.includes('nghỉ') || t.includes('dừng') || t.includes('tạm dừng')) {
      const res = await executeAiTool('control_pomodoro', { action: 'pause' });
      appendActionChip(res.message);
      return '⏱️ Đã tạm dừng phiên Pomodoro cho bạn!';
    } else {
      let mins = 25;
      const mMatch = t.match(/(\d+)\s*(phút|p|m)/);
      if (mMatch) mins = Number(mMatch[1]);
      const res = await executeAiTool('control_pomodoro', { action: 'set', minutes: mins });
      await executeAiTool('control_pomodoro', { action: 'start' });
      appendActionChip(res.message);
      return `⏱️ Đã bắt đầu phiên Pomodoro **${mins} phút** tập trung cao độ! Chúc bạn làm việc hiệu quả.`;
    }
  }

  // 2. Navigation
  if (t.startsWith('chuyển') || t.startsWith('mở') || t.startsWith('xem trang')) {
    if (t.includes('lịch')) { await executeAiTool('navigate_page', { page: 'calendar' }); return '📅 Đã chuyển sang trang **Lịch**.'; }
    if (t.includes('todo') || t.includes('công việc')) { await executeAiTool('navigate_page', { page: 'todos' }); return '📝 Đã chuyển sang trang **Việc cần làm**.'; }
    if (t.includes('dự án')) { await executeAiTool('navigate_page', { page: 'projects' }); return '📁 Đã chuyển sang trang **Dự án**.'; }
    if (t.includes('thu chi') || t.includes('tiền')) { await executeAiTool('navigate_page', { page: 'finance' }); return '💰 Đã chuyển sang trang **Thu chi**.'; }
    if (t.includes('ghi chú')) { await executeAiTool('navigate_page', { page: 'notes' }); return '📋 Đã chuyển sang trang **Ghi chú**.'; }
    if (t.includes('thói quen')) { await executeAiTool('navigate_page', { page: 'habits' }); return '🔥 Đã chuyển sang trang **Thói quen**.'; }
    if (t.includes('mục tiêu')) { await executeAiTool('navigate_page', { page: 'goals' }); return '🎯 Đã chuyển sang trang **Mục tiêu**.'; }
    if (t.includes('hôm nay')) { await executeAiTool('navigate_page', { page: 'today' }); return '☀️ Đã chuyển sang màn hình **Hôm nay**.'; }
  }

  // 3. Finance expense (chi / tiêu / mua)
  if (t.startsWith('chi ') || t.startsWith('tiêu ') || t.startsWith('mua ') || t.includes('đã chi') || t.includes('hết')) {
    let amt = 0;
    const numMatch = t.match(/(\d+[\d\.,]*)\s*(k|nghìn|ngàn|tr|triệu|đ|vnd)?/i);
    if (numMatch) {
      let raw = parseFloat(numMatch[1].replace(/,/g, ''));
      const unit = (numMatch[2] || '').toLowerCase();
      if (unit === 'k' || unit === 'nghìn' || unit === 'ngàn') raw *= 1000;
      else if (unit === 'tr' || unit === 'triệu') raw *= 1000000;
      else if (raw < 1000) raw *= 1000; // default e.g. "50" -> 50k
      amt = raw;
    }
    if (amt > 0) {
      let cat = 'Ăn uống';
      if (t.includes('xăng') || t.includes('xe') || t.includes('grab')) cat = 'Đi lại';
      else if (t.includes('mua') || t.includes('sắm') || t.includes('áo') || t.includes('quần')) cat = 'Mua sắm';
      else if (t.includes('tiền nhà') || t.includes('điện') || t.includes('nước')) cat = 'Nhà ở';
      
      const res = await executeAiTool('manage_finance', { action: 'add', type: 'expense', amt, cat, note: text });
      appendActionChip(res.message);
      return `💸 Đã ghi nhận khoản chi **${amt.toLocaleString('vi-VN')}₫** vào danh mục **${cat}**!`;
    }
  }

  // 4. Finance income (thu / nhận / lương)
  if (t.startsWith('thu ') || t.startsWith('nhận ') || t.includes('lương') || t.includes('thưởng')) {
    let amt = 0;
    const numMatch = t.match(/(\d+[\d\.,]*)\s*(k|nghìn|ngàn|tr|triệu|đ|vnd)?/i);
    if (numMatch) {
      let raw = parseFloat(numMatch[1].replace(/,/g, ''));
      const unit = (numMatch[2] || '').toLowerCase();
      if (unit === 'k' || unit === 'nghìn' || unit === 'ngàn') raw *= 1000;
      else if (unit === 'tr' || unit === 'triệu') raw *= 1000000;
      else if (raw < 1000) raw *= 1000;
      amt = raw;
    }
    if (amt > 0) {
      const src = t.includes('lương') ? 'Lương' : (t.includes('freelance') ? 'Freelance' : 'Khác');
      const res = await executeAiTool('manage_finance', { action: 'add', type: 'income', amt, src, note: text });
      appendActionChip(res.message);
      return `💰 Đã ghi nhận khoản thu **${amt.toLocaleString('vi-VN')}₫** từ nguồn **${src}**!`;
    }
  }

  // 5. Todo (thêm việc / làm / todo)
  if (t.startsWith('thêm việc') || t.startsWith('tạo việc') || t.startsWith('todo:') || t.startsWith('nhớ ') || t.startsWith('cần làm')) {
    const taskName = text.replace(/^(thêm việc|tạo việc|todo:|nhớ|cần làm)\s*/i, '').trim() || text;
    let pri = 'mid';
    if (t.includes('gấp') || t.includes('quan trọng')) pri = 'high';
    const res = await executeAiTool('manage_todo', { action: 'add', text: taskName, priority: pri });
    appendActionChip(res.message);
    return `✓ Đã thêm nhiệm vụ: **"${taskName}"** (${pri.toUpperCase()}) vào danh sách việc hôm nay!`;
  }

  // 6. Habit checkin
  if (t.includes('thói quen') && (t.includes('xong') || t.includes('hoàn thành') || t.includes('checkin'))) {
    const db = window.DB || {};
    const habits = db.habits || [];
    if (habits.length > 0) {
      const h = habits.find(x => t.includes(x.name.toLowerCase())) || habits[0];
      const res = await executeAiTool('manage_habit', { action: 'checkin', id: h.id });
      appendActionChip(res.message);
      return `🔥 Đã check-in thói quen **"${h.name}"** hôm nay!`;
    }
  }

  // 7. Stats Query
  if (t.includes('thống kê') || t.includes('tổng quan') || t.includes('báo cáo') || t.includes('số dư')) {
    const res = await executeAiTool('query_stats', { domain: 'all' });
    const d = res.data;
    return `📊 **Tổng quan số liệu hiện tại:**\n- **Tài chính**: Tổng thu ${d.finance.totalIncome.toLocaleString('vi-VN')}₫ | Tổng chi ${d.finance.totalExpense.toLocaleString('vi-VN')}₫ | **Số dư: ${d.finance.netBalance.toLocaleString('vi-VN')}₫**\n- **Năng suất**: Hoàn thành ${d.productivity.doneTodos}/${d.productivity.totalTodos} việc (${d.productivity.completionRate})\n- **Dự án**: ${d.projects.active} dự án đang triển khai.`;
  }

  // 8. Theme Toggle
  if (t.includes('theme') || t.includes('giao diện') || t.includes('chủ đề')) {
    const theme = t.includes('sáng') || t.includes('light') ? 'light' : 'dark';
    const res = await executeAiTool('set_theme', { theme });
    return res.message;
  }

  return null;
}

// ── Multi-turn Function Calling Execution Loop ──
async function processUserAiMessage(promptText) {
  appendMessage('user', promptText);
  
  const loadingDiv = document.createElement('div');
  loadingDiv.style.cssText = 'align-self:flex-start; color:var(--text-mid); font-size:12.5px; font-style:italic; padding:6px 12px; display:flex; align-items:center; gap:6px;';
  loadingDiv.innerHTML = '<span class="sync-dot" style="animation:pulse 1s infinite;"></span> Đang suy nghĩ và xử lý...';
  const c = document.getElementById('aiMessages');
  if (c) { c.appendChild(loadingDiv); c.scrollTop = c.scrollHeight; }

  // Add user prompt to conversation
  aiChatHistory.push({
    role: 'user',
    parts: [{ text: promptText }]
  });

  const systemInstruction = buildAiSystemPrompt();

  try {
    let loopCount = 0;
    const maxLoops = 5;
    let finalModelText = '';

    while (loopCount < maxLoops) {
      loopCount++;
      const data = await callGeminiRaw(aiChatHistory, systemInstruction, AI_TOOL_DECLARATIONS);
      const candidate = data.candidates && data.candidates[0];
      if (!candidate || !candidate.content) {
        finalModelText = 'Tôi đã nhận thông tin nhưng không có phản hồi cụ thể.';
        break;
      }

      const parts = candidate.content.parts || [];
      const functionCalls = parts.filter(p => p.functionCall);
      const textParts = parts.filter(p => p.text);

      aiChatHistory.push({
        role: 'model',
        parts: parts
      });

      if (functionCalls.length > 0) {
        const responseParts = [];
        for (const fc of functionCalls) {
          const call = fc.functionCall;
          console.log(`[AI Function Call] ${call.name}:`, call.args);
          const result = await executeAiTool(call.name, call.args);
          if (result.message) {
            appendActionChip(result.message);
          }
          responseParts.push({
            functionResponse: {
              name: call.name,
              response: result
            }
          });
        }

        aiChatHistory.push({
          role: 'user',
          parts: responseParts
        });
      } else {
        finalModelText = textParts.map(p => p.text).join('\n');
        break;
      }
    }

    if (loadingDiv.parentNode) loadingDiv.parentNode.removeChild(loadingDiv);
    if (finalModelText) {
      appendMessage('ai', finalModelText);
    }
    saveAiHistory();

  } catch (err) {
    if (loadingDiv.parentNode) loadingDiv.parentNode.removeChild(loadingDiv);
    console.error('AI Processing Error:', err);

    // Try local heuristic execution first so actions work offline
    const localResult = await executeLocalHeuristicAi(promptText);
    if (localResult) {
      appendMessage('ai', localResult);
    } else {
      appendMessage('ai', 'Chào bạn! Để trò chuyện và ra lệnh ngôn ngữ tự nhiên không giới hạn cùng **LifeOS AI**, bạn chỉ cần kết nối **Gemini API Key** cá nhân (miễn phí từ Google AI Studio).');
      appendApiKeyPromptCard();
    }
  }
}

window.sendAiChat = function() {
  const inp = document.getElementById('aiInput');
  if (!inp) return;
  const txt = inp.value.trim();
  if (!txt) return;
  inp.value = '';
  processUserAiMessage(txt);
};

window.askAiQuick = function(promptText) {
  window.toggleAiChat();
  processUserAiMessage(promptText);
};

// ── Voice Input (Web Speech API) ──
window.startVoiceInput = function() {
  const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRec) {
    toast('Trình duyệt của bạn chưa hỗ trợ nhận diện giọng nói Web Speech API.', 'error');
    return;
  }

  const rec = new SpeechRec();
  rec.lang = 'vi-VN';
  rec.continuous = false;
  rec.interimResults = false;

  const micBtn = document.getElementById('aiMicBtn');
  if (micBtn) {
    micBtn.style.color = 'var(--red)';
    micBtn.classList.add('pulse');
  }
  toast('Đang lắng nghe... Hãy nói yêu cầu của bạn.', 'info');

  rec.onresult = (e) => {
    const transcript = e.results[0][0].transcript;
    const inp = document.getElementById('aiInput');
    if (inp) {
      inp.value = transcript;
      window.sendAiChat();
    }
  };

  rec.onerror = (e) => {
    console.error('Speech error:', e);
    toast('Không nhận diện được giọng nói. Vui lòng thử lại!', 'error');
  };

  rec.onend = () => {
    if (micBtn) {
      micBtn.style.color = '';
      micBtn.classList.remove('pulse');
    }
  };

  rec.start();
};

// ── Global AI Engine for Features (Weekly Digest, Note Tagging, Continue Writing) ──
window.aiEngine = {
  async generateText(promptText, systemPrompt = '') {
    const contents = [{ role: 'user', parts: [{ text: promptText }] }];
    const res = await callGeminiRaw(contents, systemPrompt || 'Bạn là trợ lý AI thông minh.', []);
    return res.candidates?.[0]?.content?.parts?.[0]?.text || '';
  },

  async autoTagNote(title, content) {
    const prompt = `Ghi chú tiêu đề: "${title}". Nội dung: "${content}". Hãy gợi ý 2 đến 3 thẻ (tags) ngắn gọn, phù hợp nhất dưới dạng JSON array dạng ["Tag1", "Tag2"]. Chỉ trả về JSON array, không kèm giải thích.`;
    try {
      const res = await this.generateText(prompt, 'Bạn là chuyên gia phân loại tài liệu.');
      const match = res.match(/\[.*\]/s);
      if (match) return JSON.parse(match[0]);
    } catch(e) {}
    
    // Heuristic Fallback
    const combined = (title + ' ' + content).toLowerCase();
    const tags = [];
    if (combined.includes('họp') || combined.includes('meeting')) tags.push('Họp hành');
    if (combined.includes('ý tưởng') || combined.includes('idea')) tags.push('Ý tưởng');
    if (combined.includes('kế hoạch') || combined.includes('plan')) tags.push('Kế hoạch');
    if (combined.includes('bug') || combined.includes('lỗi')) tags.push('Lỗi/Bug');
    if (combined.includes('mua') || combined.includes('tiền')) tags.push('Tài chính');
    return tags.length > 0 ? tags : ['Ý tưởng', 'Công việc'];
  },

  async continueNote(currentContent) {
    const prompt = `Dưới đây là phần nội dung ghi chú đang viết dở:\n"${currentContent}"\n\nHãy viết tiếp 1 đến 2 đoạn văn ngắn mạch lạc, tự nhiên và chuyên nghiệp để bổ sung ý tưởng cho ghi chú này.`;
    try {
      const res = await this.generateText(prompt, 'Bạn là trợ lý soạn thảo văn bản sáng tạo.');
      if (res) return res;
    } catch(e) {}
    return "Bên cạnh đó, cần chú ý phân bổ thời gian hợp lý và theo dõi tiến độ các đầu việc thường xuyên.";
  },

  async generateWeeklyDigest() {
    const now = new Date();
    const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    const todos = (window.DB.todos || []).filter(t => t.done && t.date >= sevenDaysAgo);
    const exps = (window.DB.expense || []).filter(e => e.date >= sevenDaysAgo);
    const incs = (window.DB.income || []).filter(i => i.date >= sevenDaysAgo);
    const notes = (window.DB.notes || []).filter(n => n.date >= sevenDaysAgo);
    const habits = (window.DB.habits || []);

    const totalExp = exps.reduce((sum, e) => sum + (Number(e.amt) || 0), 0);
    const totalInc = incs.reduce((sum, i) => sum + (Number(i.amt) || 0), 0);

    const summaryPrompt = `Hãy đóng vai LifeOS Coach, viết nhận xét tổng kết tuần 7 ngày qua cho người dùng dựa trên số liệu thực tế:
- Công việc hoàn thành: ${todos.length} nhiệm vụ (${todos.map(t=>t.text).slice(0,4).join(', ')})
- Tổng chi tiêu: ${totalExp.toLocaleString('vi-VN')}₫ (trong ${exps.length} giao dịch)
- Tổng thu nhập: ${totalInc.toLocaleString('vi-VN')}₫
- Ghi chú mới: ${notes.length} ghi chú
- Thói quen kiên trì: ${habits.map(h=>`${h.name} streak ${h.streak||0} ngày`).join(', ')}

Viết một đoạn nhận xét truyền cảm hứng, ngắn gọn (3-4 câu), đánh giá khách quan điểm mạnh và nhắc nhở điểm cần cải thiện tuần tới. Kèm emoji sinh động.`;

    try {
      const insight = await this.generateText(summaryPrompt, 'Bạn là huấn luyện viên phong cách sống và năng suất cá nhân hàng đầu.');
      return { todosCount: todos.length, totalExp, totalInc, notesCount: notes.length, insight };
    } catch(e) {
      let fallbackInsight = `Tuần qua bạn đã hoàn thành ${todos.length} nhiệm vụ và kiểm soát chi tiêu ở mức ${totalExp.toLocaleString('vi-VN')}₫. Hãy tiếp tục giữ vững phong độ trong tuần mới! 🚀`;
      return { todosCount: todos.length, totalExp, totalInc, notesCount: notes.length, insight: fallbackInsight };
    }
  }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  loadAiHistory();
});
