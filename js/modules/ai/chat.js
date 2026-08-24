/* ════════════════════════════════════════════════════════════
  LIFEOS AI ASSISTANT & CHAT ENGINE (GEMINI PRO / FLASH)
════════════════════════════════════════════════════════════ */

import { AI_TOOL_DECLARATIONS, executeAiTool } from './tools.js';
import { buildAiSystemPrompt } from './context.js';

let aiChatHistory = [];
const DEFAULT_GEMINI_KEY = ""; // Users can enter their personal key or use demo

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
  const key = prompt('Nhập Gemini API Key của bạn (hoàn toàn miễn phí tại https://aistudio.google.com):', cur);
  if (key !== null) {
    localStorage.setItem('lifeos_gemini_key', key.trim());
    toast('Đã lưu Gemini API Key!', 'success');
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

// ── Multi-turn Gemini API Core ──
const MODELS = [
  'gemini-2.0-flash',
  'gemini-1.5-flash',
  'gemini-1.5-pro'
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

      if (res.status === 429) {
        console.warn(`Model ${model} rate limited, trying next...`);
        continue;
      }
      if (res.status === 400 || res.status === 403) {
        const errJson = await res.json().catch(() => ({}));
        if (errJson.error?.message?.includes('API key')) {
          throw new Error('KEY_KHONG_HOP_LE');
        }
      }
      if (!res.ok) {
        const txt = await res.text();
        throw new Error(`API ${res.status}: ${txt}`);
      }

      const data = await res.json();
      return data;
    } catch (err) {
      lastError = err;
      if (err.message === 'KEY_KHONG_HOP_LE' || err.message === 'CHUA_CO_KEY') throw err;
    }
  }
  throw lastError || new Error('Không thể kết nối đến máy chủ AI.');
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

      // Save model turn in history
      aiChatHistory.push({
        role: 'model',
        parts: parts
      });

      if (functionCalls.length > 0) {
        // Execute each tool and collect responses
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

        // Send function responses back to Gemini for final response synthesis
        aiChatHistory.push({
          role: 'user',
          parts: responseParts
        });
      } else {
        // No more tool calls, model provided final natural text
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

    if (err.message === 'CHUA_CO_KEY') {
      appendMessage('ai', '⚠️ Bạn chưa cài đặt **Gemini API Key**.\n\nVui lòng bấm nút **"Cài đặt Key"** bên dưới (hoàn toàn miễn phí từ Google) để kích hoạt toàn bộ tính năng Trợ lý AI!');
      const keyBtn = document.createElement('button');
      keyBtn.className = 'btn btn-p btn-sm';
      keyBtn.style.cssText = 'margin:6px 0 12px;';
      keyBtn.textContent = '🔑 Cài đặt Gemini API Key';
      keyBtn.onclick = window.openAiKeyModal;
      if (c) c.appendChild(keyBtn);
    } else if (err.message === 'KEY_KHONG_HOP_LE') {
      appendMessage('ai', '❌ Gemini API Key không hợp lệ hoặc đã hết hạn. Vui lòng kiểm tra và nhập lại Key trong phần cài đặt.');
    } else {
      appendMessage('ai', `⚠️ Không thể kết nối AI: ${err.message}. Vui lòng thử lại!`);
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
    return ['Ý tưởng', 'Công việc'];
  },

  async continueNote(currentContent) {
    const prompt = `Dưới đây là phần nội dung ghi chú đang viết dở:\n"${currentContent}"\n\nHãy viết tiếp 1 đến 2 đoạn văn ngắn mạch lạc, tự nhiên và chuyên nghiệp để bổ sung ý tưởng cho ghi chú này.`;
    return await this.generateText(prompt, 'Bạn là trợ lý soạn thảo văn bản sáng tạo.');
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
      // Fallback
      let fallbackInsight = `Tuần qua bạn đã hoàn thành ${todos.length} nhiệm vụ và kiểm soát chi tiêu ở mức ${totalExp.toLocaleString('vi-VN')}₫. Hãy tiếp tục giữ vững phong độ trong tuần mới! 🚀`;
      return { todosCount: todos.length, totalExp, totalInc, notesCount: notes.length, insight: fallbackInsight };
    }
  }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  loadAiHistory();
});
