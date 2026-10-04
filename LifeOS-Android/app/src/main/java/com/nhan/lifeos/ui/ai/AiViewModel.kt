package com.nhan.lifeos.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.network.GeminiApiService
import com.nhan.lifeos.data.preferences.UserPreferencesRepository
import com.nhan.lifeos.data.preferences.UserSession
import com.nhan.lifeos.data.repository.FinanceRepository
import com.nhan.lifeos.data.repository.PersonalRepository
import com.nhan.lifeos.data.repository.TaskTimeRepository
import com.nhan.lifeos.ui.finance.formatVnd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

data class AiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val time: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

data class AiUiState(
    val messages: List<AiMessage> = emptyList(),
    val isThinking: Boolean = false,
    val apiKey: String = "",
    val model: String = GeminiApiService.DEFAULT_MODEL,
    val isTestingKey: Boolean = false,
    val testKeyResult: String? = null,
    val isKeyValid: Boolean? = null
)

class AiViewModel(
    private val taskTimeRepository: TaskTimeRepository,
    private val financeRepository: FinanceRepository,
    private val personalRepository: PersonalRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AiUiState(
            apiKey = "",
            model = GeminiApiService.DEFAULT_MODEL,
            messages = listOf(
                AiMessage(
                    isUser = false,
                    text = "Xin chào! Tôi là Trợ lý AI LifeOS 🤖 (sử dụng Gemini Flash mới nhất). Tôi đã đồng bộ toàn bộ dữ liệu công việc, dự án, tài chính và thói quen của bạn. Tôi có thể giúp bạn lập kế hoạch, phân tích hiệu suất và cố vấn phát triển bản thân. Hôm nay bạn muốn tập trung vào điều gì?"
                )
            )
        )
    )
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    init {
        // Observe saved API Key and Model
        if (preferencesRepository != null) {
            viewModelScope.launch {
                preferencesRepository.userSessionFlow.collect { session ->
                    val resolvedKey = session.geminiApiKey
                    val resolvedModel = if (session.geminiModel.contains("2.0") || session.geminiModel.contains("1.5") || session.geminiModel.isBlank()) {
                        GeminiApiService.DEFAULT_MODEL
                    } else {
                        session.geminiModel
                    }
                    _uiState.value = _uiState.value.copy(
                        apiKey = resolvedKey,
                        model = resolvedModel
                    )
                }
            }
        }
    }

    fun saveGeminiConfig(key: String, model: String) {
        val clean = GeminiApiService.cleanApiKey(key)
        viewModelScope.launch {
            preferencesRepository?.setGeminiConfig(clean, model)
            _uiState.value = _uiState.value.copy(
                apiKey = clean,
                model = model,
                testKeyResult = null,
                isKeyValid = null
            )
            val confirmMsg = if (clean.isNotBlank()) {
                "✅ **Đã kết nối Gemini API Key thành công!** (Mô hình: $model). Bạn có thể hỏi bất kỳ câu hỏi nào, yêu cầu lập kế hoạch chi tiết, hoặc ra lệnh cá nhân hóa."
            } else {
                "ℹ️ Đã xóa Gemini API Key. Trợ lý chuyển sang chế độ phân tích ngoại tuyến dựa trên dữ liệu LifeOS."
            }
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + AiMessage(isUser = false, text = confirmMsg)
            )
        }
    }

    fun testGeminiKey(key: String, model: String) {
        val clean = GeminiApiService.cleanApiKey(key)
        if (clean.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isTestingKey = false,
                isKeyValid = false,
                testKeyResult = "⚠️ Vui lòng dán mã API Key trước khi kiểm tra!"
            )
            return
        }

        if (GeminiApiService.isOAuthClientId(clean)) {
            _uiState.value = _uiState.value.copy(
                isTestingKey = false,
                isKeyValid = false,
                testKeyResult = "❌ Mã bạn vừa nhập là OAuth Client ID, không phải Gemini API Key! Hãy lấy API Key (AIzaSy...) tại https://aistudio.google.com/app/apikey"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingKey = true, testKeyResult = null)
            val result = GeminiApiService.testConnection(clean, model)
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        isTestingKey = false,
                        isKeyValid = true,
                        testKeyResult = "✅ $msg"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isTestingKey = false,
                        isKeyValid = false,
                        testKeyResult = "❌ Lỗi: ${err.message}"
                    )
                }
            )
        }
    }

    fun clearTestStatus() {
        _uiState.value = _uiState.value.copy(testKeyResult = null, isKeyValid = null)
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return

        val userMsg = AiMessage(isUser = true, text = trimmed)
        val currentHistory = _uiState.value.messages
        _uiState.value = _uiState.value.copy(
            messages = currentHistory + userMsg,
            isThinking = true
        )

        viewModelScope.launch {
            val key = _uiState.value.apiKey
            val model = if (_uiState.value.model.contains("2.0") || _uiState.value.model.contains("1.5")) {
                GeminiApiService.DEFAULT_MODEL
            } else {
                _uiState.value.model
            }

            if (key.isNotBlank()) {
                // Call real Gemini API
                val systemPrompt = buildSystemPrompt()
                val historyPairs = currentHistory.map { it.isUser to it.text }
                val apiResult = GeminiApiService.generateContent(
                    apiKey = key,
                    preferredModel = model,
                    systemInstruction = systemPrompt,
                    history = historyPairs,
                    userMessage = trimmed
                )

                apiResult.fold(
                    onSuccess = { reply ->
                        val (cleanReply, actionNotice) = processAndExecuteActions(trimmed, reply)
                        val finalMsg = if (actionNotice != null) "$cleanReply\n\n$actionNotice" else cleanReply
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + AiMessage(isUser = false, text = finalMsg),
                            isThinking = false
                        )
                    },
                    onFailure = { err ->
                        // Fallback response with notice
                        val (cleanFallback, actionNotice) = processAndExecuteActions(trimmed, generateSmartFallback(trimmed))
                        val finalFallback = if (actionNotice != null) "$cleanFallback\n\n$actionNotice" else cleanFallback
                        val errNotice = "⚠️ *(Lưu ý: Không thể kết nối Gemini API [${err.message?.take(80)}], chuyển sang phân tích ngoại tuyến)*\n\n$finalFallback"
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + AiMessage(isUser = false, text = errNotice),
                            isThinking = false
                        )
                    }
                )
            } else {
                // Smart contextual offline assistant
                kotlinx.coroutines.delay(600L)
                val (cleanResponse, actionNotice) = processAndExecuteActions(trimmed, generateSmartFallback(trimmed))
                val fullResponse = buildString {
                    append(cleanResponse)
                    if (actionNotice != null) {
                        append("\n\n")
                        append(actionNotice)
                    }
                    append("\n\n---\n💡 *Mẹo: Bạn có thể bấm nút **Cài đặt Key** ở trên cùng để kết nối Google Gemini API miễn phí, mở khóa khả năng hỏi đáp và sáng tạo không giới hạn!*")
                }
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + AiMessage(isUser = false, text = fullResponse),
                    isThinking = false
                )
            }
        }
    }

    private suspend fun buildSystemPrompt(): String {
        val todos = taskTimeRepository.allTodos.first()
        val projects = taskTimeRepository.allProjects.first()
        val events = taskTimeRepository.allEvents.first()
        val txs = financeRepository.getAllTransactions().first()
        val goals = financeRepository.getAllGoals().first()
        val habits = personalRepository.allHabits.first()
        val notes = personalRepository.allNotes.first()
        val session = preferencesRepository?.userSessionFlow?.first() ?: UserSession()

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dateHuman = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN")).format(Date())
        val timeHuman = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val pendingTodos = todos.filter { !it.done }
        val doneTodos = todos.filter { it.done }
        val todayEvents = events.filter { it.dateStart == todayDate }
        val inc = txs.filter { it.type == "income" }.sumOf { it.amount }
        val exp = txs.filter { it.type == "expense" }.sumOf { it.amount }
        val net = inc - exp

        return """
        Bạn là "LifeOS AI" — Cố vấn cá nhân và Trợ lý điều hành thông minh hàng đầu được tích hợp trực tiếp trong ứng dụng LifeOS.
        Thời gian hiện tại: $dateHuman, lúc $timeHuman (Ngày format YYYY-MM-DD: $todayDate).
        Người dùng: ${session.displayName.ifBlank { "Bạn" }} ${if (session.jobTitle.isNotBlank()) "(${session.jobTitle})" else ""} ${if (session.bio.isNotBlank()) "— Bio: ${session.bio}" else ""}.

        NGUYÊN TẮC PHỤC VỤ:
        1. Phản hồi bằng tiếng Việt thân thiện, đồng cảm nhưng chuẩn xác, định dạng Markdown đẹp mắt (in đậm, danh sách gạch đầu dòng, emoji sinh động).
        2. Dựa vào dữ liệu thực tế dưới đây của người dùng để phân tích cá nhân hóa, không bịa đặt số liệu.
        3. Đưa ra lời khuyên thực tế, có tính hành động cao (Actionable).

        DỮ LIỆU ĐANG CÓ TRONG HỆ THỐNG LIFEOS:
        • Công việc cần làm (${pendingTodos.size} việc chưa xong, ${doneTodos.size} đã hoàn thành):
        ${pendingTodos.take(8).joinToString("\n") { "  - [${it.priority.uppercase()}] ${it.text} (Hạn: ${it.date.ifBlank { "Không có" }})" }}
        • Lịch trình hôm nay (${todayEvents.size} sự kiện):
        ${todayEvents.joinToString("\n") { "  - ${it.title} (${it.timeStart} - ${it.timeEnd})" }.ifBlank { "  - Trống lịch" }}
        • Dự án Kanban (${projects.size} dự án):
        ${projects.take(5).joinToString("\n") { "  - ${it.name} [${it.status}] (Tiến độ: ${it.progress}%)" }}
        • Tài chính:
          - Tổng thu nhập: +${formatVnd(inc)}
          - Tổng chi tiêu: -${formatVnd(exp)}
          - Số dư khả dụng: ${formatVnd(net)}
        • Thói quen đang rèn luyện (${habits.size} thói quen):
        ${habits.joinToString("\n") { "  - ${it.name} (Streak: ${it.streak} ngày liên tiếp 🔥)" }}
        • Mục tiêu tài chính & dài hạn (${goals.size} mục tiêu):
        ${goals.joinToString("\n") { "  - ${it.title}: ${formatVnd(it.currentValue)} / ${formatVnd(it.targetValue)}" }}
        • Ghi chú: ${notes.size} ghi chú đã lưu.

        KHẢ NĂNG THỰC THI HÀNH ĐỘNG HỆ THỐNG (SYSTEM ACTIONS):
        Bạn có khả năng thêm trực tiếp dữ liệu vào LifeOS cho người dùng!
        Khi người dùng yêu cầu ghi nhận thu chi, thêm việc cần làm hoặc tạo ghi chú:
        1. Phản hồi tự nhiên, thân thiện xác nhận đã ghi nhận.
        2. BẮT BUỘC chèn thêm 1 khối JSON ở cuối tin nhắn theo định dạng sau:
        ```lifeos-action
        {
          "action": "add_transaction",
          "type": "expense",
          "amount": 50000,
          "category": "Ăn uống",
          "note": "Tiền ăn sáng",
          "date": "$todayDate"
        }
        ```
        hoặc Todo:
        ```lifeos-action
        {
          "action": "add_todo",
          "text": "Nội dung công việc",
          "priority": "high",
          "date": "$todayDate"
        }
        ```
        hoặc Ghi chú:
        ```lifeos-action
        {
          "action": "add_note",
          "title": "Tiêu đề ghi chú",
          "content": "Nội dung ghi chú..."
        }
        ```
        """.trimIndent()
    }

    private suspend fun processAndExecuteActions(userText: String, replyText: String): Pair<String, String?> {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var cleanedReply = replyText
        var actionNotice: String? = null

        // 1. First attempt: Parse structured ```lifeos-action ... ``` block from reply
        val actionRegex = Regex("""```lifeos-action\s*(\{[\s\S]*?\})\s*```""", RegexOption.IGNORE_CASE)
        val match = actionRegex.find(replyText)
        if (match != null) {
            val jsonStr = match.groupValues[1]
            cleanedReply = replyText.replace(match.value, "").trim()
            try {
                val json = JSONObject(jsonStr)
                val action = json.optString("action")
                when (action) {
                    "add_transaction" -> {
                        val type = json.optString("type", "expense").lowercase()
                        val amt = json.optLong("amount", 0L)
                        val cat = json.optString("category", if (type == "expense") "Chi tiêu khác" else "Thu nhập khác")
                        val note = json.optString("note", "")
                        val date = json.optString("date", todayDate).ifBlank { todayDate }
                        if (amt > 0L) {
                            financeRepository.addTransaction(
                                type = type,
                                categoryOrSource = cat,
                                amount = amt,
                                date = date,
                                paymentMethod = "Tiền mặt",
                                note = note
                            )
                            val sign = if (type == "expense") "-" else "+"
                            actionNotice = "✅ **Đã ghi nhận vào Sổ Thu Chi:** $sign${formatVnd(amt)} ($cat${if (note.isNotBlank()) ": $note" else ""})"
                        }
                    }
                    "add_todo" -> {
                        val text = json.optString("text")
                        val priority = json.optString("priority", "mid")
                        val date = json.optString("date", todayDate).ifBlank { todayDate }
                        if (text.isNotBlank()) {
                            taskTimeRepository.insertTodo(text = text, priority = priority, date = date)
                            actionNotice = "✅ **Đã tạo việc cần làm:** \"$text\""
                        }
                    }
                    "add_note" -> {
                        val title = json.optString("title", "Ghi chú từ AI")
                        val content = json.optString("content", "")
                        if (content.isNotBlank() || title.isNotBlank()) {
                            personalRepository.insertNote(title = title, content = content, tags = listOf("AI"), pinned = false)
                            actionNotice = "✅ **Đã lưu vào Ghi chú:** \"$title\""
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Second attempt: If no action was executed from JSON, check user's text with Natural Language Intent Detector
        if (actionNotice == null) {
            actionNotice = detectAndExecuteVietnameseIntent(userText, todayDate)
        }

        return Pair(cleanedReply, actionNotice)
    }

    private suspend fun detectAndExecuteVietnameseIntent(userText: String, todayDate: String): String? {
        val lower = userText.trim().lowercase()

        // 1. Transaction intent
        val isExpense = lower.contains("chi") || lower.contains("tiêu") || lower.contains("mua") ||
                lower.contains("trả tiền") || lower.contains("đổ xăng") || lower.contains("thanh toán") ||
                lower.contains("ăn sáng") || lower.contains("ăn trưa") || lower.contains("ăn tối")
        val isIncome = lower.contains("thu") || lower.contains("nhận") || lower.contains("lương") ||
                lower.contains("thưởng") || lower.contains("tiền vào") || lower.contains("freelance") ||
                lower.contains("bán được") || lower.contains("được cho")

        if (isExpense || isIncome) {
            val amount = parseVndAmount(userText)
            if (amount != null && amount > 0L) {
                val type = if (isIncome && !lower.contains("chi") && !lower.contains("tiêu")) "income" else "expense"
                val category = detectCategory(lower, type)
                val note = extractNoteDescription(userText)

                financeRepository.addTransaction(
                    type = type,
                    categoryOrSource = category,
                    amount = amount,
                    date = todayDate,
                    paymentMethod = "Tiền mặt",
                    note = note
                )
                val sign = if (type == "expense") "-" else "+"
                return "✅ **Đã tự động lưu vào Sổ Thu Chi:** $sign${formatVnd(amount)} ($category${if (note.isNotBlank()) ": $note" else ""})"
            }
        }

        // 2. Todo intent
        if (lower.startsWith("thêm việc") || lower.startsWith("tạo việc") || lower.contains("việc cần làm") ||
            lower.startsWith("nhắc tôi") || lower.startsWith("nhắc việc") || lower.startsWith("todo:")) {
            val todoText = userText
                .replace(Regex("""^(thêm việc cần làm|tạo việc cần làm|việc cần làm|nhắc tôi|nhắc việc|thêm việc|tạo việc|todo:)\s*[:\-]?\s*""", RegexOption.IGNORE_CASE), "")
                .trim()
            if (todoText.isNotBlank()) {
                val priority = if (lower.contains("gấp") || lower.contains("quan trọng") || lower.contains("ưu tiên")) "high" else "mid"
                taskTimeRepository.insertTodo(text = todoText, priority = priority, date = todayDate)
                return "✅ **Đã tự động thêm vào Việc Cần Làm:** \"$todoText\""
            }
        }

        // 3. Note intent
        if (lower.startsWith("tạo ghi chú") || lower.startsWith("thêm ghi chú") || lower.startsWith("lưu ghi chú") || lower.startsWith("ghi chú:")) {
            val content = userText
                .replace(Regex("""^(tạo ghi chú|thêm ghi chú|lưu ghi chú|ghi chú:)\s*[:\-]?\s*""", RegexOption.IGNORE_CASE), "")
                .trim()
            if (content.isNotBlank()) {
                val title = content.take(30).trim() + if (content.length > 30) "..." else ""
                personalRepository.insertNote(title = title, content = content, tags = listOf("AI"), pinned = false)
                return "✅ **Đã tự động lưu vào Ghi chú:** \"$title\""
            }
        }

        return null
    }

    private fun parseVndAmount(text: String): Long? {
        val regex = Regex("""(\d+(?:[.,]\d+)?)\s*(k|nghìn|ngàn|tr|triệu|m|đ|vnd)?""", RegexOption.IGNORE_CASE)
        val matches = regex.findAll(text).toList()
        for (m in matches) {
            val numStr = m.groupValues[1].replace(",", ".")
            val unit = m.groupValues[2].lowercase()
            val num = numStr.toDoubleOrNull() ?: continue
            var total = num
            when (unit) {
                "k", "nghìn", "ngàn" -> total *= 1_000.0
                "tr", "triệu", "m" -> total *= 1_000_000.0
                else -> {
                    if (total < 1000.0 && total > 0.0) {
                        total *= 1_000.0
                    }
                }
            }
            if (total >= 1000.0) {
                return total.toLong()
            }
        }
        return null
    }

    private fun detectCategory(lower: String, type: String): String {
        if (type == "income") {
            return when {
                lower.contains("lương") -> "Lương"
                lower.contains("thưởng") || lower.contains("bonus") -> "Thưởng"
                lower.contains("freelance") || lower.contains("dự án") -> "Freelance"
                lower.contains("bán") -> "Bán hàng"
                lower.contains("quà") || lower.contains("tặng") || lower.contains("cho") -> "Được tặng/Cho"
                lower.contains("đầu tư") || lower.contains("lãi") -> "Đầu tư"
                else -> "Thu nhập khác"
            }
        } else {
            return when {
                lower.contains("ăn") || lower.contains("uống") || lower.contains("cơm") || lower.contains("phở") ||
                lower.contains("bún") || lower.contains("bánh") || lower.contains("cà phê") || lower.contains("cafe") ||
                lower.contains("trà") || lower.contains("thức ăn") -> "Ăn uống"

                lower.contains("xăng") || lower.contains("xe") || lower.contains("grab") || lower.contains("taxi") ||
                lower.contains("đi lại") || lower.contains("gửi xe") || lower.contains("bus") -> "Đi lại"

                lower.contains("mua") || lower.contains("quần áo") || lower.contains("giày") || lower.contains("shopee") ||
                lower.contains("tiki") || lower.contains("lazada") || lower.contains("đồ") -> "Mua sắm"

                lower.contains("học") || lower.contains("sách") || lower.contains("khóa học") || lower.contains("học phí") -> "Học tập"

                lower.contains("thuốc") || lower.contains("bệnh") || lower.contains("khám") || lower.contains("gym") ||
                lower.contains("thể thao") || lower.contains("nha khoa") -> "Sức khỏe"

                lower.contains("nhà") || lower.contains("phòng") || lower.contains("điện") || lower.contains("nước") ||
                lower.contains("wifi") || lower.contains("mạng") -> "Nhà ở & Tiện ích"

                lower.contains("nhậu") || lower.contains("xem phim") || lower.contains("chơi") || lower.contains("du lịch") -> "Giải trí"

                else -> "Chi tiêu khác"
            }
        }
    }

    private fun extractNoteDescription(userText: String): String {
        val cleaned = userText
            .replace(Regex("""(?i)\b(thêm|ghi nhận|ghi|nhập|khoản chi|khoản thu|chi|tiêu|thu|nhận)\b"""), "")
            .replace(Regex("""(?i)\b(\d+(?:[.,]\d+)?\s*(?:k|nghìn|ngàn|tr|triệu|m|đ|vnd)?)\b"""), "")
            .replace(Regex("""(?i)\b(tiền|cho|vào|từ|ngày hôm nay|hôm nay)\b"""), "")
            .trim()
        return cleaned.ifBlank { userText.take(40) }
    }

    private suspend fun generateSmartFallback(query: String): String {
        val q = query.lowercase()

        val todos = taskTimeRepository.allTodos.first()
        val projects = taskTimeRepository.allProjects.first()
        val events = taskTimeRepository.allEvents.first()
        val txs = financeRepository.getAllTransactions().first()
        val goals = financeRepository.getAllGoals().first()
        val habits = personalRepository.allHabits.first()

        val pendingTodos = todos.filter { !it.done }
        val doneTodos = todos.filter { it.done }
        val inc = txs.filter { it.type == "income" }.sumOf { it.amount }
        val exp = txs.filter { it.type == "expense" }.sumOf { it.amount }
        val net = inc - exp

        return when {
            q.contains("tài chính") || q.contains("thu chi") || q.contains("tiền") || q.contains("tiết kiệm") -> {
                val savRate = if (inc > 0) ((net.toDouble() / inc.toDouble()) * 100).toInt() else 0
                """
                💰 **Báo cáo Tài chính Cá nhân Toàn diện:**
                • Tổng thu nhập: **+${formatVnd(inc)}**
                • Tổng chi tiêu: **-${formatVnd(exp)}**
                • Số dư khả dụng hiện tại: **${formatVnd(net)}**
                • Tỷ lệ tích lũy / tiết kiệm: **$savRate%** (Khuyến nghị tài chính bền vững: ≥ 20%)

                💡 **Gợi ý tối ưu dòng tiền:**
                ${if (savRate >= 20) "• Phong độ tích lũy rất tốt! Bạn nên phân bổ 50% số dư dôi dư vào các Mục tiêu dài hạn." else "• Tỷ lệ tiết kiệm đang ở mức cần lưu ý. Hãy rà soát lại các khoản chi không cấp thiết trong mục Tài chính."}
                • Thiết lập quỹ dự phòng khẩn cấp tương đương 3-6 tháng chi phí sinh hoạt cơ bản.
                """.trimIndent()
            }

            q.contains("hiệu suất") || q.contains("tuần") || q.contains("tổng quan") || q.contains("năng suất") -> {
                val rate = if (todos.isNotEmpty()) ((doneTodos.size.toDouble() / todos.size.toDouble()) * 100).toInt() else 0
                val streakMax = habits.maxOfOrNull { it.streak } ?: 0
                val highPriority = pendingTodos.count { it.priority == "high" }

                """
                📊 **Đánh giá Năng suất Toàn diện:**
                • Tỷ lệ hoàn thành công việc: **$rate%** (${doneTodos.size}/${todos.size} việc)
                • Công việc ưu tiên cao đang chờ: **$highPriority việc ⚡**
                • Dự án đang vận hành: **${projects.size} dự án Kanban**
                • Chuỗi kỷ luật thói quen cao nhất: **$streakMax ngày liên tục 🔥**
                • Mục tiêu tài chính đạt: **${goals.count { it.isCompleted }}/${goals.size}**

                🚀 **Định hướng tuần này:**
                • Tập trung giải quyết dứt điểm các việc ưu tiên cao (High Priority) trước 12h trưa mỗi ngày.
                • Duy trì chuỗi thói quen để củng cố tính kỷ luật cá nhân.
                """.trimIndent()
            }

            q.contains("hôm nay") || q.contains("kế hoạch") || q.contains("việc") || q.contains("làm gì") -> {
                if (pendingTodos.isEmpty()) {
                    """
                    🎉 **Tuyệt vời! Bạn không còn việc tồn đọng nào.**
                    • Bạn đã dọn sạch danh sách Todos.
                    • Gợi ý: Hãy dành thời gian đọc thêm một cuốn sách, ôn tập Flashcard Từ vựng, hoặc thư giãn để nạp lại năng lượng!
                    """.trimIndent()
                } else {
                    val top3 = pendingTodos
                        .sortedByDescending { it.priority == "high" }
                        .take(4)
                        .mapIndexed { idx, t -> "${idx + 1}. [${t.priority.uppercase()}] ${t.text}${if (t.date.isNotBlank()) " (Hạn: ${t.date})" else ""}" }
                        .joinToString("\n")

                    """
                    🎯 **Chiến lược Thực thi Hôm nay:**
                    Hiện tại bạn có **${pendingTodos.size} việc cần xử lý**. Hãy áp dụng ma trận Eisenhower và hoàn thành trước:
                    
                    $top3

                    ⏱ **Hành động đề xuất:** Hãy bật một phiên **Pomodoro 25 phút** trong mục Pomodoro để bắt đầu xử lý ngay việc số 1!
                    """.trimIndent()
                }
            }

            q.contains("thói quen") || q.contains("habit") || q.contains("streak") -> {
                if (habits.isEmpty()) {
                    "🌿 Bạn chưa tạo thói quen nào trong LifeOS. Hãy vào mục Thói quen để bắt đầu rèn luyện từ những thói quen nhỏ như: Đọc sách 15 phút, Uống 2L nước, hoặc Tập thể dục!"
                } else {
                    val habitDetails = habits.joinToString("\n") { "• **${it.name}**: chuỗi ${it.streak} ngày 🔥" }
                    """
                    🌿 **Tiến độ Rèn luyện Thói quen:**
                    $habitDetails

                    💡 **Nguyên tắc "Không bao giờ bỏ 2 ngày liên tiếp":** Ngay cả khi bận rộn, hãy thực hiện phiên bản thu nhỏ (2 phút) của thói quen để bảo vệ chuỗi streak của bạn!
                    """.trimIndent()
                }
            }

            q.contains("dự án") || q.contains("project") || q.contains("kanban") -> {
                if (projects.isEmpty()) {
                    "📁 Bạn chưa có dự án nào. Hãy tạo một dự án mới trong mục 'Dự án Kanban' để phân rã mục tiêu lớn thành các đầu việc nhỏ."
                } else {
                    val projSummary = projects.take(5).joinToString("\n") { "• **${it.name}**: Tiến độ ${it.progress}% [${it.status}]" }
                    """
                    📁 **Tổng quan Dự án của Bạn:**
                    $projSummary

                    💡 Gợi ý: Hãy phân nhỏ các đầu việc lớn trong dự án thành các việc nhỏ dưới 30 phút để không bị áp lực trì hoãn!
                    """.trimIndent()
                }
            }

            q.contains("mục tiêu") || q.contains("goal") -> {
                if (goals.isEmpty()) {
                    "🎯 Bạn chưa đặt mục tiêu tài chính nào. Hãy vào mục Mục tiêu để đặt mục tiêu mua sắm, tiết kiệm hoặc học tập với hạn định rõ ràng!"
                } else {
                    val goalList = goals.joinToString("\n") { "• **${it.title}**: ${formatVnd(it.currentValue)} / ${formatVnd(it.targetValue)} (${it.progressPercentage}%)" }
                    """
                    🎯 **Tiến độ Mục tiêu Cá nhân:**
                    $goalList

                    💡 Tự động trích 10-20% thu nhập ngay khi nhận tiền vào các quỹ mục tiêu này trước khi chi tiêu.
                    """.trimIndent()
                }
            }

            else -> {
                """
                Tôi đã ghi nhận câu hỏi của bạn: *"$query"*.

                📋 **Bối cảnh hiện tại của bạn trong LifeOS:**
                • Việc cần làm: **${pendingTodos.size} việc** đang chờ xử lý.
                • Dòng tiền khả dụng: **${formatVnd(net)}**.
                • Dự án đang chạy: **${projects.size} dự án**.
                • Thói quen: **${habits.size} thói quen** đang duy trì.

                💬 Để tôi có thể trả lời chi tiết và sáng tạo mọi chủ đề theo yêu cầu riêng của bạn, bạn hãy kết nối **Gemini API Key** nhé!
                """.trimIndent()
            }
        }
    }
}
