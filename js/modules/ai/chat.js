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

function cleanApiKey(str) {
  if (!str) return '';
  return str.trim()
    .replace(/^[\"']|[\"']$/g, '')
    .replace(/^Bearer\s+/i, '')
    .trim();
}

window.cleanApiKey = cleanApiKey;

window.openAiKeyModal = function() {
  const modal = document.getElementById('mAiKeyConfig');
  const input = document.getElementById('aiKeyInput');
  const statusTxt = document.getElementById('aiKeyStatusText');
  const feedback = document.getElementById('aiKeyTestFeedback');
  const modelSel = document.getElementById('aiModelSelect');
  
  if (feedback) {
    feedback.style.display = 'none';
    feedback.innerHTML = '';
  }

  const curKey = localStorage.getItem('lifeos_gemini_key') || '';
  let curModel = localStorage.getItem('lifeos_ai_preferred_model') || localStorage.getItem('lifeos_ai_working_model') || 'gemini-2.0-flash';
  if (curModel.includes('3.') || curModel.includes('pro')) curModel = 'gemini-2.0-flash';
  
  if (input) {
    input.value = curKey;
    input.type = 'password';
  }
  if (modelSel) {
    modelSel.value = curModel;
  }
  const eye = document.getElementById('btnToggleAiKeyEye');
  if (eye) eye.innerHTML = '<i data-lucide="eye" class="ic-14"></i>';

  if (statusTxt) {
    statusTxt.textContent = curKey ? `Đã lưu (${curKey.length} ký tự)` : 'Chưa cài đặt';
    statusTxt.style.color = curKey ? 'var(--green)' : 'var(--text3)';
  }

  if (modal) {
    modal.style.display = 'flex';
    setTimeout(() => { input?.focus(); }, 100);
  } else {
    const key = prompt('Nhập Gemini API Key của bạn (miễn phí tại https://aistudio.google.com):', curKey);
    if (key !== null) {
      const clean = cleanApiKey(key);
      if (clean) {
        localStorage.setItem('lifeos_gemini_key', clean);
        toast('Đã lưu Gemini API Key!', 'success');
        appendMessage('ai', '✅ **Đã kết nối Gemini API Key thành công!** Bạn có thể ra lệnh và trò chuyện tự nhiên ngay bây giờ.');
      } else {
        localStorage.removeItem('lifeos_gemini_key');
        toast('Đã xóa Gemini API Key', 'info');
      }
    }
  }
  if (window.lucide) window.lucide.createIcons();
};

window.toggleAiKeyVisibility = function() {
  const input = document.getElementById('aiKeyInput');
  const eye = document.getElementById('btnToggleAiKeyEye');
  if (!input) return;
  if (input.type === 'password') {
    input.type = 'text';
    if (eye) eye.innerHTML = '<i data-lucide="eye-off" class="ic-14"></i>';
  } else {
    input.type = 'password';
    if (eye) eye.innerHTML = '<i data-lucide="eye" class="ic-14"></i>';
  }
  if (window.lucide) window.lucide.createIcons();
};

window.pasteAiKeyFromClipboard = async function() {
  try {
    const text = await navigator.clipboard.readText();
    const input = document.getElementById('aiKeyInput');
    if (input && text) {
      input.value = cleanApiKey(text);
      toast('Đã dán mã từ Clipboard', 'info');
    }
  } catch(e) {
    toast('Vui lòng dùng phím Ctrl+V để dán', 'info');
  }
};

window.saveGeminiApiKey = function() {
  const input = document.getElementById('aiKeyInput');
  const modelSel = document.getElementById('aiModelSelect');
  const raw = input ? input.value : '';
  const clean = cleanApiKey(raw);

  if (modelSel) {
    localStorage.setItem('lifeos_ai_preferred_model', modelSel.value);
    localStorage.setItem('lifeos_ai_working_model', modelSel.value);
  }

  if (!clean) {
    localStorage.removeItem('lifeos_gemini_key');
    toast('Đã xóa Gemini API Key', 'info');
    closeModal('mAiKeyConfig');
    return;
  }

  if (clean.includes('.apps.googleusercontent.com') || clean.startsWith('GOCSPX-')) {
    const fb = document.getElementById('aiKeyTestFeedback');
    if (fb) {
      fb.style.display = 'block';
      fb.style.background = 'rgba(255,71,87,0.12)';
      fb.style.border = '1px solid rgba(255,71,87,0.4)';
      fb.style.color = '#ff4757';
      fb.innerHTML = '❌ <b>Lỗi nhận diện mã:</b> Bạn đang dán <b>OAuth 2.0 Client ID</b> (hoặc Web Client) chứ không phải API Key!<br><br>👉 Vui lòng vào <a href="https://aistudio.google.com/app/apikey" target="_blank" style="color:var(--accent-cyan); font-weight:700; text-decoration:underline;">Google AI Studio</a>, bấm <b>Create API key</b> để nhận mã bắt đầu bằng <b>AIzaSy...</b>';
    }
    return;
  }

  localStorage.setItem('lifeos_gemini_key', clean);
  toast('Đã lưu cấu hình AI thành công!', 'success');
  appendMessage('ai', `✅ **Đã kết nối Gemini API Key thành công!** (Mô hình ưu tiên: ${modelSel ? modelSel.value : 'Gemini 2.0 Flash'}). Bạn có thể trò chuyện hoặc ra lệnh ngay bây giờ.`);
  closeModal('mAiKeyConfig');
};

window.clearAiKey = function() {
  const input = document.getElementById('aiKeyInput');
  if (input) input.value = '';
  localStorage.removeItem('lifeos_gemini_key');
  const statusTxt = document.getElementById('aiKeyStatusText');
  if (statusTxt) {
    statusTxt.textContent = 'Chưa cài đặt';
    statusTxt.style.color = 'var(--text3)';
  }
  const fb = document.getElementById('aiKeyTestFeedback');
  if (fb) {
    fb.style.display = 'block';
    fb.style.background = 'rgba(255,255,255,0.06)';
    fb.style.border = '1px solid var(--border)';
    fb.style.color = 'var(--text2)';
    fb.innerHTML = 'ℹ️ Đã xóa Key. Hãy nhập Key mới và bấm "Lưu Key".';
  }
  toast('Đã xóa Key', 'info');
};

window.testGeminiApiKey = async function() {
  const input = document.getElementById('aiKeyInput');
  const modelSel = document.getElementById('aiModelSelect');
  const raw = input ? input.value : '';
  const clean = cleanApiKey(raw);
  const fb = document.getElementById('aiKeyTestFeedback');
  const btn = document.getElementById('btnTestAiKey');
  const txt = document.getElementById('txtTestAiKey');

  if (!fb) return;
  fb.style.display = 'block';

  if (!clean) {
    fb.style.background = 'rgba(255,183,3,0.12)';
    fb.style.border = '1px solid rgba(255,183,3,0.4)';
    fb.style.color = '#ffb703';
    fb.innerHTML = '⚠️ Vui lòng dán mã API Key trước khi kiểm tra kết nối!';
    return;
  }

  if (clean.includes('.apps.googleusercontent.com') || clean.startsWith('GOCSPX-')) {
    fb.style.background = 'rgba(255,71,87,0.12)';
    fb.style.border = '1px solid rgba(255,71,87,0.4)';
    fb.style.color = '#ff4757';
    fb.innerHTML = '❌ <b>Lỗi xác thực:</b> Mã bạn vừa nhập là <b>OAuth 2.0 Client ID</b>!<br>Google Gemini API yêu cầu <b>API Key</b> (dạng <code>AIzaSy...</code>).<br>👉 Hãy tạo key tại: <a href="https://aistudio.google.com/app/apikey" target="_blank" style="color:var(--accent-cyan); font-weight:700; text-decoration:underline;">aistudio.google.com/app/apikey</a>';
    return;
  }

  fb.style.background = 'rgba(124,77,255,0.12)';
  fb.style.border = '1px solid rgba(124,77,255,0.3)';
  fb.style.color = 'var(--accent-light)';
  fb.innerHTML = '⏳ Đang gửi tín hiệu kiểm tra tốc độ đến Google Gemini API...';
  if (btn) btn.disabled = true;
  if (txt) txt.textContent = 'Đang kiểm tra...';

  const startTime = Date.now();
  try {
    const res = await callGeminiRaw(
      [{ role: 'user', parts: [{ text: 'Trả lời đúng 1 chữ: OK' }] }],
      'Bạn là trợ lý AI.',
      [],
      clean
    );

    const latency = Date.now() - startTime;
    const modelReply = res.candidates?.[0]?.content?.parts?.[0]?.text || 'OK';
    const usedModel = localStorage.getItem('lifeos_ai_working_model') || (modelSel ? modelSel.value : 'gemini-2.0-flash');
    
    fb.style.background = 'rgba(46,213,115,0.12)';
    fb.style.border = '1px solid rgba(46,213,115,0.4)';
    fb.style.color = '#2ed573';
    fb.innerHTML = `✅ <b>Kết nối siêu tốc thành công! (${latency}ms)</b><br>
      • Model hoạt động: <b>${usedModel}</b><br>
      • Phản hồi: "${modelReply.trim()}"<br><br>
      👉 Hãy bấm <b>"Lưu Key"</b> để lưu cấu hình này.`;
  } catch (err) {
    fb.style.background = 'rgba(255,71,87,0.12)';
    fb.style.border = '1px solid rgba(255,71,87,0.4)';
    fb.style.color = '#ff4757';

    const msg = err.message || '';
    if (msg.includes('OAuth') || msg.includes('authentication credentials') || msg.includes('KEY_OAUTH_CLIENT_ERROR')) {
      fb.innerHTML = '❌ <b>Lỗi xác thực OAuth:</b> Mã bạn dùng bị Google từ chối vì là OAuth Client chứ không phải API Key.<br><br>👉 <b>Cách khắc phục:</b> Vào <a href="https://aistudio.google.com/app/apikey" target="_blank" style="color:var(--accent-cyan); font-weight:700; text-decoration:underline;">Google AI Studio</a>, bấm <b>Create API key</b> để nhận mã <code>AIzaSy...</code>';
    } else if (msg.includes('API key not valid') || msg.includes('KEY_KHONG_HOP_LE')) {
      fb.innerHTML = '❌ <b>API Key không hợp lệ:</b> Mã bị sai ký tự hoặc không tồn tại trong hệ thống Google.<br>Vui lòng sao chép lại chính xác từ <a href="https://aistudio.google.com/app/apikey" target="_blank" style="color:var(--accent-cyan); font-weight:700; text-decoration:underline;">Google AI Studio</a>.';
    } else if (msg.includes('PERMISSION_DENIED') || msg.includes('has not been used in project')) {
      fb.innerHTML = '❌ <b>Chưa bật Generative Language API:</b> Dự án Google Cloud của bạn chưa kích hoạt API này. Hãy tạo một key mới trong dự án mặc định tại <a href="https://aistudio.google.com/app/apikey" target="_blank" style="color:var(--accent-cyan); font-weight:700; text-decoration:underline;">Google AI Studio</a>.';
    } else {
      fb.innerHTML = `❌ <b>Không thể kết nối:</b> ${msg}<br>Vui lòng kiểm tra lại mạng hoặc thử tạo một API Key mới.`;
    }
  } finally {
    if (btn) btn.disabled = false;
    if (txt) txt.textContent = 'Kiểm tra kết nối';
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
// ── Multi-turn Gemini API Core (Speed-optimized: Gemini 2.0 Flash First) ──
// Clean up any deprecated/problematic cached models
try {
  const curSaved = localStorage.getItem('lifeos_ai_working_model') || localStorage.getItem('lifeos_ai_preferred_model');
  if (curSaved && (curSaved.includes('3.') || curSaved.includes('2.5') || curSaved.includes('pro'))) {
    localStorage.setItem('lifeos_ai_working_model', 'gemini-2.0-flash');
    localStorage.setItem('lifeos_ai_preferred_model', 'gemini-2.0-flash');
  }
} catch(e) {}

const ALL_MODELS = [
  'gemini-2.0-flash',     // Fastest (< 1s, highly intelligent)
  'gemini-1.5-flash',     // Standard, 100% reliable across all keys
  'gemini-1.5-flash-8b'   // Ultra lightweight fallback
];

function getWindowedHistory(history, maxTurns = 6) {
  if (!history || history.length <= maxTurns) return history;
  let slice = history.slice(-maxTurns);
  while (slice.length > 0 && slice[0].role !== 'user') {
    slice.shift();
  }
  return slice.length > 0 ? slice : history.slice(-2);
}

async function callGeminiRaw(contents, systemInstruction, tools, customKey = null) {
  const rawKey = customKey || getApiKey();
  const apiKey = cleanApiKey(rawKey);
  if (!apiKey) {
    throw new Error('CHUA_CO_KEY');
  }

  let preferredModel = localStorage.getItem('lifeos_ai_preferred_model') || localStorage.getItem('lifeos_ai_working_model') || 'gemini-2.0-flash';
  if (preferredModel.includes('3.') || preferredModel.includes('pro')) {
    preferredModel = 'gemini-2.0-flash';
  }

  const modelsToTry = [
    preferredModel,
    ...ALL_MODELS.filter(m => m !== preferredModel)
  ];

  let lastError = null;
  for (const model of modelsToTry) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(apiKey)}`;
    const payload = {
      contents,
      systemInstruction: { parts: [{ text: systemInstruction }] },
      generationConfig: {
        temperature: 0.7,
        maxOutputTokens: 1024,
        topP: 0.95
      }
    };
    if (tools && tools.length > 0) {
      payload.tools = [{ functionDeclarations: tools }];
    }

    // 25-second realistic timeout to allow model to finish reasoning without premature aborts
    const controller = new AbortController();
    const timeoutTimer = setTimeout(() => controller.abort(), 25000);

    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-goog-api-key': apiKey
        },
        body: JSON.stringify(payload),
        signal: controller.signal
      });
      clearTimeout(timeoutTimer);

      if (res.status === 400 || res.status === 401 || res.status === 403) {
        const errJson = await res.json().catch(() => ({}));
        const msg = errJson.error?.message || '';
        const msgLower = msg.toLowerCase();
        
        if (msgLower.includes('oauth') || msgLower.includes('authentication credentials') || msgLower.includes('access token')) {
          throw new Error('KEY_OAUTH_CLIENT_ERROR: ' + msg);
        }
        if (msgLower.includes('api key not valid') || msgLower.includes('api_key_invalid') || msgLower.includes('invalid api key')) {
          throw new Error('KEY_KHONG_HOP_LE: ' + msg);
        }
        if (msgLower.includes('permission_denied') || msgLower.includes('has not been used in project')) {
          throw new Error('KEY_PERMISSION_DENIED: ' + msg);
        }

        console.warn(`Model ${model} returned ${res.status} (${msg}), trying next model...`);
        lastError = new Error(`Model ${model}: ${msg}`);
        continue;
      }

      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        const msg = errJson.error?.message || `HTTP ${res.status}`;
        console.warn(`Model ${model} returned ${res.status} (${msg}), trying next model...`);
        lastError = new Error(msg);
        continue;
      }

      const data = await res.json();
      localStorage.setItem('lifeos_ai_working_model', model);
      return data;
    } catch (err) {
      clearTimeout(timeoutTimer);
      lastError = err;
      if (err.name === 'AbortError') {
        console.warn(`Model ${model} timed out after 25s, switching to next model...`);
        lastError = new Error(`Model ${model} quá hạn phản hồi (timeout)`);
        continue;
      }
      if (err.message && err.message.startsWith('KEY_')) throw err;
      console.warn(`Error calling model ${model}:`, err.message);
    }
  }

  throw lastError || new Error('Tất cả các model Gemini đều không phản hồi. Vui lòng thử lại sau.');
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
  loadingDiv.innerHTML = '<span class="sync-dot" style="animation:pulse 0.8s infinite; background:var(--accent-cyan);"></span> <span id="aiThinkingStatus">⚡ Đang xử lý...</span>';
  const c = document.getElementById('aiMessages');
  if (c) { c.appendChild(loadingDiv); c.scrollTop = c.scrollHeight; }

  function updateThinkingText(text) {
    const st = loadingDiv.querySelector('#aiThinkingStatus');
    if (st) st.textContent = text;
  }

  // Add user prompt to conversation history
  aiChatHistory.push({
    role: 'user',
    parts: [{ text: promptText }]
  });

  const systemInstruction = buildAiSystemPrompt();

  try {
    let loopCount = 0;
    const maxLoops = 4;
    let finalModelText = '';

    while (loopCount < maxLoops) {
      loopCount++;
      if (loopCount > 1) {
        updateThinkingText('⚡ Đang hoàn tất phản hồi...');
      } else {
        updateThinkingText('⚡ Đang suy nghĩ...');
      }

      // Window history to keep payload lightweight and fast
      const windowedHistory = getWindowedHistory(aiChatHistory, 6);
      const data = await callGeminiRaw(windowedHistory, systemInstruction, AI_TOOL_DECLARATIONS);
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
        updateThinkingText('⚙️ Đang thực hiện hành động...');
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

    const msg = err.message || '';
    const hasKey = !!getApiKey();

    // Try local heuristic execution first so actions work offline
    const localResult = await executeLocalHeuristicAi(promptText);
    if (localResult) {
      appendMessage('ai', localResult);
    } else if (msg === 'CHUA_CO_KEY' || !hasKey) {
      appendMessage('ai', 'Chào bạn! Để trò chuyện và ra lệnh ngôn ngữ tự nhiên không giới hạn cùng **LifeOS AI**, bạn chỉ cần kết nối **Gemini API Key** cá nhân (hoàn toàn miễn phí từ Google AI Studio).');
      appendApiKeyPromptCard();
    } else if (msg.includes('KEY_OAUTH_CLIENT_ERROR') || msg.includes('authentication credentials') || msg.includes('OAuth')) {
      appendMessage('ai', '⚠️ **Mã bạn nhập không phải là Gemini API Key hợp lệ.**\n\n' +
        'Google báo lỗi: *Request had invalid authentication credentials. Expected OAuth 2 access token...*\n\n' +
        '👉 **Nguyên nhân**: Bạn đã tạo nhầm mã **OAuth 2.0 Client ID** (hoặc Web Client) thay vì **API Key**.\n\n' +
        '👉 **Cách lấy đúng API Key trong 30 giây (Miễn phí 100%):**\n' +
        '1. Truy cập [Google AI Studio (aistudio.google.com/app/apikey)](https://aistudio.google.com/app/apikey)\n' +
        '2. Bấm nút xanh **"Create API key"**\n' +
        '3. Sao chép mã có dạng **`AIzaSy...`** (khoảng 39 ký tự)\n' +
        '4. Bấm nút bên dưới để dán vào và kiểm tra kết nối ngay.');
      appendApiKeyPromptCard();
    } else if (msg.includes('KEY_KHONG_HOP_LE') || msg.includes('API key not valid')) {
      appendMessage('ai', '⚠️ **API Key không hợp lệ hoặc đã hết hạn.**\n\n' +
        'Mã bạn nhập không đúng hoặc thiếu ký tự. Key chuẩn từ Google AI Studio luôn có dạng **`AIzaSy...`**.\n\n' +
        '👉 Bạn hãy vào [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey) tạo key mới và dán lại nhé.');
      appendApiKeyPromptCard();
    } else if (msg.includes('KEY_PERMISSION_DENIED')) {
      appendMessage('ai', '⚠️ **Quyền truy cập API bị từ chối:** Key của bạn chưa được kích hoạt "Generative Language API" hoặc bị giới hạn trong Google Cloud Console. Hãy tạo một key mới tại [aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey).');
      appendApiKeyPromptCard();
    } else {
      appendMessage('ai', `❌ **Không thể kết nối Gemini AI lúc này.**\n\n- Lỗi: *${msg}*\n- Có thể do mạng không ổn định hoặc dịch vụ Google đang quá tải. Hãy thử lại sau ít phút.`);
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
