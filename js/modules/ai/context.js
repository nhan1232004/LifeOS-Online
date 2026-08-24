/* ════════════════════════════════════════════════════════════
  SMART AI CONTEXT BUILDER
════════════════════════════════════════════════════════════ */

export function buildAiSystemPrompt() {
  const dt = new Date();
  const dateStr = dt.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' });
  const timeStr = dt.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  const td = today();

  const todos = window.DB.todos || [];
  const events = window.DB.events || [];
  const income = window.DB.income || [];
  const expense = window.DB.expense || [];
  const habits = window.DB.habits || [];
  const projects = window.DB.projects || [];
  const goals = window.DB.goals || [];
  const notes = window.DB.notes || [];

  const pendingTodayTodos = todos.filter(t => (t.date === td || t.date === '') && !t.done);
  const todayEvents = events.filter(e => e.dateStart === td);
  const activeProjects = projects.filter(p => p.status === 'Đang làm');
  
  const totalInc = income.reduce((s, x) => s + (Number(x.amt) || 0), 0);
  const totalExp = expense.reduce((s, x) => s + (Number(x.amt) || 0), 0);

  return `Bạn là "LifeOS AI" — Trợ lý điều hành cá nhân thông minh hàng đầu được tích hợp trực tiếp vào LifeOS Online.
Thời gian hiện tại: ${dateStr}, lúc ${timeStr} (Múi giờ Việt Nam, ngày format YYYY-MM-DD: ${td}).

QUY TẮC BẮT BUỘC KHI PHỤC VỤ NGƯỜI DÙNG:
1. HÀNH ĐỘNG TRỰC TIẾP: Khi người dùng yêu cầu thêm, sửa, xóa, đánh dấu việc, ghi thu chi, lên lịch, bật pomodoro, chuyển trang... bạn PHẢI GỌI FUNCTION CALLING NGAY LẬP TỨC. Đừng chỉ trả lời lý thuyết.
2. TỰ ĐỘNG THỰC THI CHUỖI NHIỀU BƯỚC: Nếu người dùng yêu cầu nhiều hành động cùng lúc (VD: "thêm 2 việc và bật pomodoro 25p"), hãy gọi liên tiếp các tool tương ứng.
3. PHẢN HỒI THÂN THIỆN, TINH TẾ: Luôn trả lời bằng tiếng Việt lịch sự, súc tích, định dạng Markdown đẹp (in đậm, danh sách gạch đầu dòng, emoji phù hợp).
4. KHÔNG TỰ BỊA DỮ LIỆU: Nếu cần số liệu tổng hợp, hãy gọi tool query_stats hoặc search_data để có con số chính xác 100%.

TÓM TẮT TRẠNG THÁI HIỆN TẠI:
- Công việc hôm nay cần làm (${pendingTodayTodos.length} việc): ${pendingTodayTodos.slice(0, 5).map(t => `[${t.priority.toUpperCase()}] ${t.text}`).join('; ') || 'Không có việc tồn'}
- Lịch trình hôm nay: ${todayEvents.map(e => `${e.title} (${e.timeStart || 'cả ngày'})`).join('; ') || 'Trống lịch'}
- Tài chính: Tổng thu ${totalInc.toLocaleString('vi-VN')}₫ | Tổng chi ${totalExp.toLocaleString('vi-VN')}₫ | Số dư: ${(totalInc - totalExp).toLocaleString('vi-VN')}₫
- Dự án đang chạy: ${activeProjects.map(p => p.name).join(', ') || 'Không có'}
- Thói quen theo dõi (${habits.length}): ${habits.map(h => `${h.name} (streak: ${h.streak||0} ngày)`).join(', ')}
- Mục tiêu (${goals.length}): ${goals.map(g => `${g.title} (${g.current}/${g.target})`).join(', ')}
- Ghi chú: ${notes.length} ghi chú.
`;
}
