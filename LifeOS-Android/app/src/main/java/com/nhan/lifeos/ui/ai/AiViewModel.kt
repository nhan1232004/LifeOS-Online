package com.nhan.lifeos.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.repository.FinanceRepository
import com.nhan.lifeos.data.repository.PersonalRepository
import com.nhan.lifeos.data.repository.TaskTimeRepository
import com.nhan.lifeos.ui.finance.formatVnd
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

data class AiUiState(
    val messages: List<AiMessage> = emptyList(),
    val isThinking: Boolean = false
)

class AiViewModel(
    private val taskTimeRepository: TaskTimeRepository,
    private val financeRepository: FinanceRepository,
    private val personalRepository: PersonalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AiUiState(
            messages = listOf(
                AiMessage(
                    isUser = false,
                    text = "Xin chào! Tôi là Trợ lý AI LifeOS 🤖. Tôi có thể phân tích công việc, tài chính, thói quen và mục tiêu để giúp bạn đạt hiệu suất cao nhất hôm nay. Bạn cần tôi hỗ trợ điều gì?"
                )
            )
        )
    )
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = AiMessage(isUser = true, text = userText)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg,
            isThinking = true
        )

        viewModelScope.launch {
            delay(800L) // natural thinking pause

            val aiResponse = generateContextualResponse(userText)
            val assistantMsg = AiMessage(isUser = false, text = aiResponse)

            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + assistantMsg,
                isThinking = false
            )
        }
    }

    private suspend fun generateContextualResponse(query: String): String {
        val q = query.lowercase()

        // Fetch live DB snapshot
        val todos = taskTimeRepository.allTodos.first()
        val projects = taskTimeRepository.allProjects.first()
        val txs = financeRepository.getAllTransactions().first()
        val goals = financeRepository.getAllGoals().first()
        val habits = personalRepository.allHabits.first()

        val doneTodos = todos.count { it.done }
        val pendingTodos = todos.filter { !it.done }
        val inc = txs.filter { it.type == "income" }.sumOf { it.amount }
        val exp = txs.filter { it.type == "expense" }.sumOf { it.amount }
        val net = inc - exp

        return when {
            q.contains("tài chính") || q.contains("thu chi") || q.contains("tiền") -> {
                val savRate = if (inc > 0) ((net.toDouble() / inc.toDouble()) * 100).toInt() else 0
                """
                💰 **Báo cáo Dòng tiền Cá nhân:**
                • Tổng thu nhập: +${formatVnd(inc)}
                • Tổng chi tiêu: -${formatVnd(exp)}
                • Số dư khả dụng hiện tại: **${formatVnd(net)}**
                • Tỷ lệ tiết kiệm: **$savRate%** (Mức khuyến nghị: ≥ 20%)

                💡 **Lời khuyên từ LifeOS AI:**
                ${if (savRate >= 20) "Dòng tiền của bạn đang rất lành mạnh! Hãy trích 50% số dư này vào các quỹ Mục tiêu dài hạn." else "Chi tiêu đang chiếm tỷ lệ cao trong tháng. Hãy cân nhắc cắt giảm các khoản chi không cấp thiết."}
                """.trimIndent()
            }

            q.contains("hiệu suất") || q.contains("tuần") || q.contains("tổng quan") -> {
                val rate = if (todos.isNotEmpty()) ((doneTodos.toDouble() / todos.size.toDouble()) * 100).toInt() else 0
                val streakMax = habits.maxOfOrNull { it.streak } ?: 0
                """
                📊 **Đánh giá Năng suất Toàn diện:**
                • Tỷ lệ hoàn thành công việc: **$rate%** ($doneTodos/${todos.size} việc)
                • Dự án đang vận hành: **${projects.size}** dự án Kanban
                • Chuỗi thói quen cao nhất: **$streakMax ngày liên tiếp 🔥**
                • Mục tiêu đạt 100%: **${goals.count { it.isCompleted }}/${goals.size}**

                🚀 **Đánh giá:** Phong độ của bạn đang ở mức rất cao! Tiếp tục giữ nhịp độ và không trì hoãn các việc ưu tiên cao nhé.
                """.trimIndent()
            }

            q.contains("hôm nay") || q.contains("kế hoạch") || q.contains("việc") -> {
                if (pendingTodos.isEmpty()) {
                    "🎉 Chúc mừng bạn! Bạn đã hoàn thành toàn bộ công việc trong danh sách việc cần làm. Hãy dành thời gian đọc sách hoặc giải lao Pomodoro."
                } else {
                    val top3 = pendingTodos.take(3).mapIndexed { idx, t -> "${idx + 1}. [${t.priority.uppercase()}] ${t.text}" }.joinToString("\n")
                    """
                    🎯 **Gợi ý Kế hoạch Hành động:**
                    Bạn còn **${pendingTodos.size} công việc** cần xử lý. Dưới đây là 3 việc nên tập trung hoàn thành trước:
                    
                    $top3
                    
                    ⏱ Bạn có thể bật **Pomodoro 25 phút** để bắt đầu xử lý ngay việc đầu tiên!
                    """.trimIndent()
                }
            }

            q.contains("thói quen") || q.contains("habit") -> {
                val habitsList = habits.joinToString("\n") { "• ${it.name}: chuỗi ${it.streak} ngày" }
                """
                🌿 **Tiến độ Thói quen:**
                $habitsList
                
                💡 Để không ngắt chuỗi streak, hãy hoàn thành các thói quen này vào cùng một khung giờ cố định mỗi ngày!
                """.trimIndent()
            }

            else -> {
                """
                Tôi đã ghi nhận câu hỏi của bạn. Dựa trên dữ liệu LifeOS của bạn:
                • Bạn có ${pendingTodos.size} công việc đang chờ.
                • Số dư tài chính ròng hiện tại là ${formatVnd(net)}.
                • Có ${goals.size} mục tiêu dài hạn đang theo đuổi.
                
                Bạn có thể bấm các gợi ý bên dưới để nhận phân tích chi tiết hơn!
                """.trimIndent()
            }
        }
    }
}
