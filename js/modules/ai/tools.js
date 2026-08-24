/* ════════════════════════════════════════════════════════════
  LIFEOS AI TOOL REGISTRY — 100% DOMAIN COVERAGE + UI CONTROLS
════════════════════════════════════════════════════════════ */

window.AI_UNDO_BUFFER = null;
window.AI_UNDO_TIMER = null;

function setAiUndo(description, snapshotFn, restoreFn) {
  if (window.AI_UNDO_TIMER) clearTimeout(window.AI_UNDO_TIMER);
  window.AI_UNDO_BUFFER = { description, restoreFn };
  window.AI_UNDO_TIMER = setTimeout(() => {
    window.AI_UNDO_BUFFER = null;
    const undoPill = document.getElementById('aiUndoPill');
    if (undoPill) undoPill.style.display = 'none';
  }, 10000); // 10s undo window

  const undoPill = document.getElementById('aiUndoPill');
  if (undoPill) {
    undoPill.style.display = 'inline-flex';
    undoPill.innerHTML = `<span>${description}</span> <button class="btn btn-sm" onclick="executeAiUndo()" style="padding:2px 8px;font-size:11px;margin-left:6px;background:var(--accent);color:#fff;">Hoàn tác (10s)</button>`;
  }
}

window.executeAiUndo = function() {
  if (window.AI_UNDO_BUFFER && typeof window.AI_UNDO_BUFFER.restoreFn === 'function') {
    window.AI_UNDO_BUFFER.restoreFn();
    toast('Đã hoàn tác thao tác AI!', 'success');
    window.AI_UNDO_BUFFER = null;
    const undoPill = document.getElementById('aiUndoPill');
    if (undoPill) undoPill.style.display = 'none';
    if (window.renderAll) window.renderAll();
  }
};

export const AI_TOOL_DECLARATIONS = [
  // 1. TODO
  {
    name: "manage_todo",
    description: "Quản lý danh sách việc cần làm (Todo). Thêm, sửa, xóa, đánh dấu hoàn thành.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "edit", "delete", "toggle"], description: "Hành động thực hiện" },
        id: { type: "STRING", description: "ID của todo (khi edit, delete, toggle)" },
        text: { type: "STRING", description: "Nội dung công việc" },
        priority: { type: "STRING", enum: ["high", "mid", "low"], description: "Mức độ ưu tiên" },
        date: { type: "STRING", description: "Ngày thực hiện (YYYY-MM-DD), rỗng là không hạn" },
        note: { type: "STRING", description: "Ghi chú thêm" },
        done: { type: "BOOLEAN", description: "Trạng thái hoàn thành" }
      },
      required: ["action"]
    }
  },
  // 2. CALENDAR
  {
    name: "manage_calendar",
    description: "Quản lý sự kiện lịch. Thêm, sửa, xóa sự kiện, cuộc họp, lịch hẹn.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "edit", "delete"], description: "Hành động thực hiện" },
        id: { type: "STRING", description: "ID của sự kiện (khi edit, delete)" },
        title: { type: "STRING", description: "Tiêu đề sự kiện" },
        dateStart: { type: "STRING", description: "Ngày bắt đầu (YYYY-MM-DD)" },
        dateEnd: { type: "STRING", description: "Ngày kết thúc (YYYY-MM-DD)" },
        timeStart: { type: "STRING", description: "Giờ bắt đầu (HH:MM)" },
        timeEnd: { type: "STRING", description: "Giờ kết thúc (HH:MM)" },
        type: { type: "STRING", enum: ["work", "personal", "health", "social", "study"], description: "Loại sự kiện" },
        desc: { type: "STRING", description: "Mô tả chi tiết" }
      },
      required: ["action"]
    }
  },
  // 3. FINANCE
  {
    name: "manage_finance",
    description: "Quản lý thu chi cá nhân. Ghi nhận khoản thu, khoản chi, sửa, xóa giao dịch.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "edit", "delete"], description: "Hành động thực hiện" },
        type: { type: "STRING", enum: ["income", "expense"], description: "Loại giao dịch: thu hoặc chi" },
        id: { type: "STRING", description: "ID giao dịch (khi edit, delete)" },
        amt: { type: "NUMBER", description: "Số tiền giao dịch (VNĐ)" },
        src: { type: "STRING", description: "Nguồn thu (Lương, Freelance, Đầu tư, v.v.)" },
        cat: { type: "STRING", description: "Danh mục chi (Ăn uống, Đi lại, Nhà ở, Mua sắm, v.v.)" },
        date: { type: "STRING", description: "Ngày giao dịch (YYYY-MM-DD)" },
        pay: { type: "STRING", enum: ["Tiền mặt", "Thẻ ngân hàng", "Chuyển khoản", "Ví điện tử"], description: "Hình thức thanh toán" },
        note: { type: "STRING", description: "Ghi chú giao dịch" }
      },
      required: ["action", "type"]
    }
  },
  // 4. PROJECTS
  {
    name: "manage_project",
    description: "Quản lý dự án. Tạo mới, sửa trạng thái, hạn chót, ngân sách hoặc xóa dự án.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "edit", "delete"], description: "Hành động" },
        id: { type: "STRING", description: "ID dự án" },
        name: { type: "STRING", description: "Tên dự án" },
        status: { type: "STRING", enum: ["Cần làm", "Đang làm", "Hoàn thành", "Tạm dừng"], description: "Trạng thái dự án" },
        priority: { type: "STRING", enum: ["high", "mid", "low"], description: "Độ ưu tiên" },
        budget: { type: "NUMBER", description: "Ngân sách (VNĐ)" },
        due: { type: "STRING", description: "Hạn chót (YYYY-MM-DD)" },
        tags: { type: "ARRAY", items: { type: "STRING" }, description: "Thẻ phân loại" },
        desc: { type: "STRING", description: "Mô tả dự án" }
      },
      required: ["action"]
    }
  },
  // 5. PROJECT TASKS (KANBAN)
  {
    name: "manage_project_task",
    description: "Quản lý nhiệm vụ (task) bên trong dự án Kanban. Thêm, sửa, di chuyển trạng thái, xóa task.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "move", "edit", "delete"], description: "Hành động" },
        id: { type: "STRING", description: "ID của task" },
        projId: { type: "STRING", description: "ID của dự án chứa task" },
        title: { type: "STRING", description: "Tiêu đề nhiệm vụ" },
        status: { type: "STRING", enum: ["Khởi tạo", "Cần làm", "Đang làm", "Hoàn thành"], description: "Cột trạng thái Kanban" },
        priority: { type: "STRING", enum: ["high", "mid", "low"], description: "Độ ưu tiên" },
        due: { type: "STRING", description: "Hạn chót task (YYYY-MM-DD)" }
      },
      required: ["action"]
    }
  },
  // 6. NOTES
  {
    name: "manage_note",
    description: "Quản lý ghi chú cá nhân. Thêm ghi chú mới, sửa nội dung, gắn thẻ, ghim hoặc xóa.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "edit", "delete", "pin"], description: "Hành động" },
        id: { type: "STRING", description: "ID ghi chú" },
        title: { type: "STRING", description: "Tiêu đề ghi chú" },
        body: { type: "STRING", description: "Nội dung ghi chú" },
        tags: { type: "ARRAY", items: { type: "STRING" }, description: "Danh sách tags" },
        color: { type: "STRING", description: "Mã màu viền (#7c4dff, #00e5ff, #00e676, v.v.)" },
        pinned: { type: "BOOLEAN", description: "Ghim lên đầu trang" }
      },
      required: ["action"]
    }
  },
  // 7. HABITS
  {
    name: "manage_habit",
    description: "Quản lý thói quen. Thêm thói quen mới, check-in hoàn thành hôm nay, theo dõi chuỗi ngày streak.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "edit", "delete", "checkin"], description: "Hành động" },
        id: { type: "STRING", description: "ID thói quen" },
        name: { type: "STRING", description: "Tên thói quen (VD: Đọc sách 30p, Uống 2L nước)" },
        target: { type: "STRING", description: "Mục tiêu (Mỗi ngày, 3 lần/tuần, v.v.)" },
        date: { type: "STRING", description: "Ngày check-in (YYYY-MM-DD), mặc định hôm nay" }
      },
      required: ["action"]
    }
  },
  // 8. GOALS
  {
    name: "manage_goal",
    description: "Quản lý mục tiêu cá nhân (tài chính, sự nghiệp, học tập). Thêm, cập nhật tiến độ, xóa mục tiêu.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "edit", "delete", "update_progress"], description: "Hành động" },
        id: { type: "STRING", description: "ID mục tiêu" },
        title: { type: "STRING", description: "Tên mục tiêu" },
        target: { type: "NUMBER", description: "Mục tiêu cần đạt (tiền VNĐ hoặc con số %)" },
        current: { type: "NUMBER", description: "Tiến độ hiện tại" },
        category: { type: "STRING", description: "Phân loại (Tài chính, Sự nghiệp, Sức khỏe, Học tập)" },
        deadline: { type: "STRING", description: "Hạn chót hoàn thành (YYYY-MM-DD)" }
      },
      required: ["action"]
    }
  },
  // 9. VOCABULARY
  {
    name: "manage_vocab",
    description: "Quản lý từ vựng tiếng Anh (Flashcards). Thêm từ mới, phiên âm, nghĩa tiếng Việt, ví dụ.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "delete"], description: "Hành động" },
        id: { type: "STRING", description: "ID từ vựng" },
        word: { type: "STRING", description: "Từ tiếng Anh" },
        ipa: { type: "STRING", description: "Phiên âm IPA" },
        meaning: { type: "STRING", description: "Nghĩa tiếng Việt" },
        example: { type: "STRING", description: "Câu ví dụ thực tế" }
      },
      required: ["action"]
    }
  },
  // 10. MOCK TESTS
  {
    name: "manage_mocktest",
    description: "Quản lý kết quả thi thử tiếng Anh (IELTS/TOEIC). Nhập điểm 4 kỹ năng Nghe, Đọc, Nói, Viết.",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["add", "delete"], description: "Hành động" },
        id: { type: "STRING", description: "ID bài thi" },
        name: { type: "STRING", description: "Tên bài thi (VD: Cambridge IELTS 18 Test 1)" },
        date: { type: "STRING", description: "Ngày thi (YYYY-MM-DD)" },
        list: { type: "NUMBER", description: "Điểm Nghe" },
        read: { type: "NUMBER", description: "Điểm Đọc" },
        speak: { type: "NUMBER", description: "Điểm Nói" },
        write: { type: "NUMBER", description: "Điểm Viết" },
        note: { type: "STRING", description: "Ghi chú nhận xét" }
      },
      required: ["action"]
    }
  },
  // 11. STATS & ANALYTICS QUERY
  {
    name: "query_stats",
    description: "Truy vấn số liệu thống kê tổng hợp (tài chính, năng suất làm việc, thói quen, tiến độ mục tiêu) để phân tích chính xác.",
    parameters: {
      type: "OBJECT",
      properties: {
        domain: { type: "STRING", enum: ["all", "finance", "productivity", "habits", "goals"], description: "Phạm vi cần phân tích" },
        period: { type: "STRING", enum: ["7d", "30d", "this_month", "all_time"], description: "Khung thời gian" }
      },
      required: ["domain"]
    }
  },
  // 12. UI & APP CONTROLS
  {
    name: "navigate_page",
    description: "Điều hướng và chuyển màn hình hiển thị trong ứng dụng.",
    parameters: {
      type: "OBJECT",
      properties: {
        page: { 
          type: "STRING", 
          enum: ["today", "overview", "calendar", "schedule", "projects", "finance", "stats", "notes", "habits", "goals", "pomodoro", "vocab", "mocktests"],
          description: "Tên trang cần chuyển đến"
        }
      },
      required: ["page"]
    }
  },
  {
    name: "control_pomodoro",
    description: "Điều khiển đồng hồ tập trung Pomodoro (bắt đầu, tạm dừng, đặt lại, chỉnh thời gian).",
    parameters: {
      type: "OBJECT",
      properties: {
        action: { type: "STRING", enum: ["start", "pause", "reset", "set"], description: "Hành động" },
        minutes: { type: "NUMBER", description: "Số phút (khi action là set, VD: 25, 45, 5, 15)" }
      },
      required: ["action"]
    }
  },
  {
    name: "set_theme",
    description: "Chuyển đổi giao diện Sáng (light) hoặc Tối (dark).",
    parameters: {
      type: "OBJECT",
      properties: {
        theme: { type: "STRING", enum: ["dark", "light"], description: "Chủ đề mong muốn" }
      },
      required: ["theme"]
    }
  },
  {
    name: "export_data",
    description: "Xuất dữ liệu dự phòng (JSON) hoặc xuất bảng kê thu chi (CSV).",
    parameters: {
      type: "OBJECT",
      properties: {
        type: { type: "STRING", enum: ["all_json", "finance_csv"], description: "Định dạng xuất" }
      },
      required: ["type"]
    }
  },
  {
    name: "search_data",
    description: "Tìm kiếm chi tiết từ khóa trên toàn bộ hệ thống (Ghi chú, Việc cần làm, Lịch trình, Giao dịch, Dự án).",
    parameters: {
      type: "OBJECT",
      properties: {
        domain: { type: "STRING", enum: ["all", "notes", "todos", "events", "finance", "projects"], description: "Miền tìm kiếm" },
        query: { type: "STRING", description: "Từ khóa tìm kiếm" }
      },
      required: ["query"]
    }
  }
];

// ==========================================
// TOOL EXECUTION HANDLERS
// ==========================================
export async function executeAiTool(name, args) {
  try {
    switch(name) {
      // 1. TODO
      case "manage_todo": {
        if (!window.DB.todos) window.DB.todos = [];
        const snapshot = JSON.stringify(window.DB.todos);
        
        if (args.action === "add") {
          const item = {
            id: uid(),
            text: args.text || "Nhiệm vụ mới",
            priority: args.priority || "mid",
            date: args.date || today(),
            note: args.note || "",
            done: false
          };
          window.DB.todos.push(item);
          await persist('todos', window.DB.todos);
          setAiUndo(`Thêm việc "${item.text}"`, null, async () => {
            window.DB.todos = JSON.parse(snapshot);
            await persist('todos', window.DB.todos);
          });
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã thêm việc: "${item.text}" (${item.priority.toUpperCase()}) hạn ${item.date}`, item };
        }
        if (args.action === "toggle") {
          const item = window.DB.todos.find(t => t.id === args.id || (args.text && t.text.toLowerCase().includes(args.text.toLowerCase())));
          if (!item) return { success: false, message: "Không tìm thấy nhiệm vụ phù hợp." };
          item.done = args.done !== undefined ? args.done : !item.done;
          await persist('todos', window.DB.todos);
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã ${item.done ? 'hoàn thành' : 'mở lại'} việc: "${item.text}"`, item };
        }
        if (args.action === "edit") {
          const item = window.DB.todos.find(t => t.id === args.id || (args.text && t.text.toLowerCase().includes(args.text.toLowerCase())));
          if (!item) return { success: false, message: "Không tìm thấy nhiệm vụ để sửa." };
          if (args.text) item.text = args.text;
          if (args.priority) item.priority = args.priority;
          if (args.date !== undefined) item.date = args.date;
          if (args.note !== undefined) item.note = args.note;
          if (args.done !== undefined) item.done = args.done;
          await persist('todos', window.DB.todos);
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã cập nhật việc: "${item.text}"`, item };
        }
        if (args.action === "delete") {
          const idx = window.DB.todos.findIndex(t => t.id === args.id || (args.text && t.text.toLowerCase().includes(args.text.toLowerCase())));
          if (idx === -1) return { success: false, message: "Không tìm thấy việc cần xóa." };
          const deleted = window.DB.todos.splice(idx, 1)[0];
          await persist('todos', window.DB.todos);
          setAiUndo(`Xóa việc "${deleted.text}"`, null, async () => {
            window.DB.todos = JSON.parse(snapshot);
            await persist('todos', window.DB.todos);
          });
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã xóa việc: "${deleted.text}"`, deleted };
        }
        break;
      }

      // 2. CALENDAR
      case "manage_calendar": {
        if (!window.DB.events) window.DB.events = [];
        const snapshot = JSON.stringify(window.DB.events);

        if (args.action === "add") {
          const item = {
            id: uid(),
            title: args.title || "Sự kiện mới",
            dateStart: args.dateStart || today(),
            dateEnd: args.dateEnd || args.dateStart || today(),
            timeStart: args.timeStart || "",
            timeEnd: args.timeEnd || "",
            type: args.type || "work",
            desc: args.desc || ""
          };
          window.DB.events.push(item);
          await persist('events', window.DB.events);
          setAiUndo(`Thêm sự kiện "${item.title}"`, null, async () => {
            window.DB.events = JSON.parse(snapshot);
            await persist('events', window.DB.events);
          });
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã lên lịch sự kiện: "${item.title}" vào ngày ${item.dateStart} ${item.timeStart ? 'lúc ' + item.timeStart : ''}`, item };
        }
        if (args.action === "delete") {
          const idx = window.DB.events.findIndex(e => e.id === args.id || (args.title && e.title.toLowerCase().includes(args.title.toLowerCase())));
          if (idx === -1) return { success: false, message: "Không tìm thấy sự kiện cần xóa." };
          const deleted = window.DB.events.splice(idx, 1)[0];
          await persist('events', window.DB.events);
          setAiUndo(`Xóa sự kiện "${deleted.title}"`, null, async () => {
            window.DB.events = JSON.parse(snapshot);
            await persist('events', window.DB.events);
          });
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã xóa sự kiện: "${deleted.title}"`, deleted };
        }
        break;
      }

      // 3. FINANCE
      case "manage_finance": {
        const col = args.type === "income" ? "income" : "expense";
        if (!window.DB[col]) window.DB[col] = [];
        const snapshot = JSON.stringify(window.DB[col]);

        if (args.action === "add") {
          const item = {
            id: uid(),
            amt: Number(args.amt) || 0,
            date: args.date || today(),
            note: args.note || ""
          };
          if (args.type === "income") item.src = args.src || "Khác";
          else {
            item.cat = args.cat || "Khác";
            item.pay = args.pay || "Tiền mặt";
          }
          window.DB[col].push(item);
          await persist(col, window.DB[col]);
          setAiUndo(`Thêm ${col === 'income' ? 'thu' : 'chi'} ${item.amt.toLocaleString('vi-VN')}₫`, null, async () => {
            window.DB[col] = JSON.parse(snapshot);
            await persist(col, window.DB[col]);
          });
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã ghi nhận ${args.type === 'income' ? 'khoản thu' : 'khoản chi'}: ${item.amt.toLocaleString('vi-VN')} ₫ (${item.src || item.cat})`, item };
        }
        if (args.action === "delete") {
          const idx = window.DB[col].findIndex(x => x.id === args.id);
          if (idx === -1) return { success: false, message: "Không tìm thấy giao dịch để xóa." };
          const deleted = window.DB[col].splice(idx, 1)[0];
          await persist(col, window.DB[col]);
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã xóa giao dịch ${deleted.amt.toLocaleString('vi-VN')} ₫` };
        }
        break;
      }

      // 4. HABITS
      case "manage_habit": {
        if (!window.DB.habits) window.DB.habits = [];
        const snapshot = JSON.stringify(window.DB.habits);

        if (args.action === "add") {
          const item = {
            id: uid(),
            name: args.name || "Thói quen mới",
            target: args.target || "Mỗi ngày",
            streak: 0,
            history: []
          };
          window.DB.habits.push(item);
          await persist('habits', window.DB.habits);
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã thêm thói quen mới: "${item.name}"`, item };
        }
        if (args.action === "checkin") {
          const targetDate = args.date || today();
          const habit = window.DB.habits.find(h => h.id === args.id || (args.name && h.name.toLowerCase().includes(args.name.toLowerCase())));
          if (!habit) return { success: false, message: "Không tìm thấy thói quen." };
          if (!habit.history) habit.history = [];
          if (!habit.history.includes(targetDate)) {
            habit.history.push(targetDate);
            habit.streak = (habit.streak || 0) + 1;
            await persist('habits', window.DB.habits);
            if (window.renderAll) window.renderAll();
            return { success: true, message: `✓ Đã hoàn thành thói quen "${habit.name}" hôm nay! Chuỗi: ${habit.streak} ngày 🔥`, habit };
          } else {
            return { success: true, message: `Thói quen "${habit.name}" đã được hoàn thành trước đó rồi!`, habit };
          }
        }
        break;
      }

      // 5. GOALS
      case "manage_goal": {
        if (!window.DB.goals) window.DB.goals = [];
        if (args.action === "add") {
          const item = {
            id: uid(),
            title: args.title || "Mục tiêu mới",
            target: Number(args.target) || 100,
            current: Number(args.current) || 0,
            category: args.category || "Tài chính",
            deadline: args.deadline || ""
          };
          window.DB.goals.push(item);
          await persist('goals', window.DB.goals);
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã thiết lập mục tiêu: "${item.title}" (${item.current}/${item.target})`, item };
        }
        if (args.action === "update_progress") {
          const goal = window.DB.goals.find(g => g.id === args.id || (args.title && g.title.toLowerCase().includes(args.title.toLowerCase())));
          if (!goal) return { success: false, message: "Không tìm thấy mục tiêu." };
          if (args.current !== undefined) goal.current = Number(args.current);
          await persist('goals', window.DB.goals);
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã cập nhật tiến độ mục tiêu "${goal.title}": ${goal.current}/${goal.target} (${Math.round((goal.current/goal.target)*100)}%)`, goal };
        }
        break;
      }

      // 6. VOCABULARY
      case "manage_vocab": {
        if (!window.DB.vocab) window.DB.vocab = [];
        if (args.action === "add") {
          const item = {
            id: uid(),
            word: args.word || "",
            ipa: args.ipa || "",
            meaning: args.meaning || "",
            example: args.example || ""
          };
          window.DB.vocab.push(item);
          await persist('vocab', window.DB.vocab);
          if (window.renderAll) window.renderAll();
          return { success: true, message: `Đã thêm từ vựng: "${item.word}" (${item.ipa}): ${item.meaning}`, item };
        }
        break;
      }

      // 7. STATS QUERY
      case "query_stats": {
        const td = today();
        const todos = window.DB.todos || [];
        const inc = window.DB.income || [];
        const exp = window.DB.expense || [];
        const habits = window.DB.habits || [];
        const projs = window.DB.projects || [];

        const totalInc = inc.reduce((s, x) => s + (Number(x.amt) || 0), 0);
        const totalExp = exp.reduce((s, x) => s + (Number(x.amt) || 0), 0);
        const net = totalInc - totalExp;
        const doneTodos = todos.filter(t => t.done).length;

        return {
          success: true,
          data: {
            finance: { totalIncome: totalInc, totalExpense: totalExp, netBalance: net, transactionCount: inc.length + exp.length },
            productivity: { totalTodos: todos.length, doneTodos, pendingTodos: todos.length - doneTodos, completionRate: todos.length ? Math.round((doneTodos / todos.length) * 100) + '%' : '0%' },
            habits: habits.map(h => ({ name: h.name, streak: h.streak || 0, completedToday: (h.history || []).includes(td) })),
            projects: { total: projs.length, active: projs.filter(p => p.status === 'Đang làm').length, completed: projs.filter(p => p.status === 'Hoàn thành').length }
          }
        };
      }

      // 8. UI CONTROLS
      case "navigate_page": {
        if (typeof window.nav === 'function') {
          window.nav(args.page);
          return { success: true, message: `Đã chuyển sang trang ${args.page.toUpperCase()}` };
        }
        return { success: false, message: "Không thể chuyển trang." };
      }

      case "control_pomodoro": {
        if (args.action === "start") {
          if (!window.pomRunning && typeof window.pomToggle === 'function') window.pomToggle();
          return { success: true, message: "Đã bắt đầu phiên Pomodoro ⏱️" };
        }
        if (args.action === "pause") {
          if (window.pomRunning && typeof window.pomToggle === 'function') window.pomToggle();
          return { success: true, message: "Đã tạm dừng Pomodoro ⏸️" };
        }
        if (args.action === "reset") {
          if (typeof window.pomReset === 'function') window.pomReset();
          return { success: true, message: "Đã đặt lại đồng hồ Pomodoro." };
        }
        if (args.action === "set" && args.minutes) {
          if (typeof window.pomSet === 'function') window.pomSet(args.minutes);
          return { success: true, message: `Đã cài đặt thời gian Pomodoro thành ${args.minutes} phút.` };
        }
        break;
      }

      case "set_theme": {
        const cur = document.documentElement.getAttribute('data-theme') || 'dark';
        if (cur !== args.theme && typeof window.toggleTheme === 'function') {
          window.toggleTheme();
          return { success: true, message: `Đã đổi sang chủ đề ${args.theme === 'light' ? 'Sáng ☀️' : 'Tối 🌙'}` };
        }
        return { success: true, message: `Giao diện hiện tại đã là ${args.theme}.` };
      }

      case "export_data": {
        if (args.type === "all_json" && typeof window.exportAllData === 'function') {
          window.exportAllData();
          return { success: true, message: "Đã tải xuống file sao lưu JSON an toàn!" };
        }
        if (args.type === "finance_csv" && typeof window.exportFinanceCSV === 'function') {
          window.exportFinanceCSV();
          return { success: true, message: "Đã tải xuống bảng kê thu chi CSV!" };
        }
        break;
      }

      case "search_data": {
        const q = (args.query || "").toLowerCase();
        const results = [];
        (window.DB.todos || []).forEach(t => { if ((t.text||"").toLowerCase().includes(q)) results.push({ type: "Todo", text: t.text, date: t.date, done: t.done }); });
        (window.DB.notes || []).forEach(n => { if ((n.title||"").toLowerCase().includes(q)) results.push({ type: "Ghi chú", title: n.title, date: n.date }); });
        (window.DB.events || []).forEach(e => { if ((e.title||"").toLowerCase().includes(q)) results.push({ type: "Sự kiện", title: e.title, date: e.dateStart }); });
        (window.DB.projects || []).forEach(p => { if ((p.name||"").toLowerCase().includes(q)) results.push({ type: "Dự án", name: p.name, status: p.status }); });
        return { success: true, count: results.length, results: results.slice(0, 10) };
      }

      default:
        return { success: false, message: `Tool ${name} chưa được hỗ trợ.` };
    }
  } catch (err) {
    console.error("AI Tool Execution Error:", err);
    return { success: false, error: err.message };
  }
}
