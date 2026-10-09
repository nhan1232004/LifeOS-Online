package com.nhan.lifeos.ui.finance

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nhan.lifeos.data.local.entity.TransactionEntity
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinanceExcelExporter {

    private val moneyFmt = DecimalFormat("#,###")
    private val dateDisplayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun exportAndShare(
        context: Context,
        transactions: List<TransactionEntity>,
        label: String,
        startDate: String,
        endDate: String
    ) {
        try {
            val incomeTxs = transactions.filter { it.type == "income" }
                .sortedByDescending { it.date }
            val expenseTxs = transactions.filter { it.type == "expense" }
                .sortedByDescending { it.date }

            val totalInc = incomeTxs.sumOf { it.amount }
            val totalExp = expenseTxs.sumOf { it.amount }
            val netBal = totalInc - totalExp
            val savingsRate = if (totalInc > 0) {
                ((netBal.toDouble() / totalInc.toDouble()) * 100.0)
            } else {
                if (netBal < 0) -100.0 else 0.0
            }
            val formattedRate = String.format(Locale.US, "%.1f", savingsRate)

            // Category breakdown for expenses
            val catExpenses = expenseTxs
                .groupBy { it.categoryOrSource.ifBlank { "Khác" } }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }

            // Source breakdown for income
            val srcIncomes = incomeTxs
                .groupBy { it.categoryOrSource.ifBlank { "Khác" } }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }

            // Sorted transactions
            val allSortedTxs = transactions.sortedByDescending { it.date }

            // Financial health assessment
            val (healthText, healthColor) = when {
                totalInc == 0L && totalExp == 0L -> "Chưa có phát sinh giao dịch" to "#64748b"
                netBal < 0 -> "Bội chi (Cảnh báo: Chi tiêu vượt Thu nhập)" to "#dc2626"
                savingsRate >= 30.0 -> "Rất tốt (Tỷ lệ tiết kiệm ≥ 30% thu nhập)" to "#059669"
                savingsRate >= 15.0 -> "Ổn định (Tỷ lệ tiết kiệm 15% – 30%)" to "#2563eb"
                else -> "Cần cải thiện (Tỷ lệ tiết kiệm < 15%)" to "#d97706"
            }

            fun makeBar(pct: Double, maxLen: Int = 16): String {
                val filled = Math.round((pct / 100.0) * maxLen).toInt().coerceIn(0, maxLen)
                return "█".repeat(filled) + "░".repeat(maxLen - filled)
            }

            fun formatVnDate(d: String): String {
                if (d.isBlank()) return "—"
                val parts = d.split("-")
                return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else d
            }

            fun escapeXml(text: String): String {
                return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
            }

            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            val sb = java.lang.StringBuilder()
            sb.append("""
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
  .kpi-box-val-bal { font-size: 15pt; font-weight: bold; text-align: center; color: ${if (netBal >= 0) "#1d4ed8" else "#b91c1c"}; background-color: #eff6ff; height: 36px; }
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
  <td colspan="7" class="sub-banner">Khoảng thời gian: ${escapeXml(label)} | Ngày xuất báo cáo: ${escapeXml(nowStr)}</td>
 </tr>
 <tr>
  <td colspan="4" class="info-bar">Từ ngày: <b>${escapeXml(formatVnDate(startDate))}</b> &nbsp;→&nbsp; Đến ngày: <b>${escapeXml(formatVnDate(endDate))}</b></td>
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
  <td colspan="2" class="kpi-box-val-inc">${moneyFmt.format(totalInc)} ₫</td>
  <td colspan="2" class="kpi-box-val-exp">${moneyFmt.format(totalExp)} ₫</td>
  <td colspan="2" class="kpi-box-val-bal">${moneyFmt.format(netBal)} ₫</td>
  <td class="kpi-box-val-rate">${formattedRate}%</td>
 </tr>
 <tr>
  <td colspan="2" class="kpi-sub">${incomeTxs.size} giao dịch thu nhập</td>
  <td colspan="2" class="kpi-sub">${expenseTxs.size} giao dịch chi tiêu</td>
  <td colspan="2" class="kpi-sub">${if (netBal >= 0) "Thặng dư tài chính tích cực" else "Thâm hụt dòng tiền trong kỳ"}</td>
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
""".trimIndent())

            if (catExpenses.isEmpty()) {
                sb.append("<tr><td colspan=\"7\" class=\"text-c\" style=\"padding:14px;color:#64748b;\">Chưa có dữ liệu chi tiêu trong khoảng thời gian này</td></tr>\n")
            } else {
                catExpenses.forEachIndexed { idx, entry ->
                    val cat = entry.first
                    val amt = entry.second
                    val pct = if (totalExp > 0) (amt.toDouble() / totalExp.toDouble()) * 100.0 else 0.0
                    val pctStr = String.format(Locale.US, "%.1f", pct)
                    val bar = makeBar(pct)
                    val note = when {
                        pct >= 30.0 -> "⚠️ Chiếm tỷ trọng lớn nhất"
                        pct >= 15.0 -> "Chi tiêu đáng kể"
                        else -> "Chi tiêu nhỏ lẻ"
                    }
                    sb.append("""
 <tr>
  <td class="text-c">${idx + 1}</td>
  <td class="text-l"><b>${escapeXml(cat)}</b></td>
  <td class="curr-exp">${moneyFmt.format(amt)} ₫</td>
  <td class="text-r"><b>${pctStr}%</b></td>
  <td colspan="2" class="data-bar">${bar} ${pctStr}%</td>
  <td class="text-l" style="color:#64748b;font-size:8.5pt;">${escapeXml(note)}</td>
 </tr>
""".trimIndent()).append("\n")
                }
                sb.append("""
 <tr class="row-total">
  <td colspan="2" class="text-c">TỔNG CỘNG CHI TIÊU</td>
  <td class="curr-exp">${moneyFmt.format(totalExp)} ₫</td>
  <td class="text-r">100.0%</td>
  <td colspan="3" class="text-l" style="color:#64748b;">Tổng số danh mục: ${catExpenses.size}</td>
 </tr>
""".trimIndent()).append("\n")
            }

            sb.append("</table>\n\n")

            // PHẦN 3: NGUỒN THU
            sb.append("""
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
""".trimIndent()).append("\n")

            if (srcIncomes.isEmpty()) {
                sb.append("<tr><td colspan=\"7\" class=\"text-c\" style=\"padding:14px;color:#64748b;\">Chưa có dữ liệu thu nhập trong khoảng thời gian này</td></tr>\n")
            } else {
                srcIncomes.forEachIndexed { idx, entry ->
                    val src = entry.first
                    val amt = entry.second
                    val pct = if (totalInc > 0) (amt.toDouble() / totalInc.toDouble()) * 100.0 else 0.0
                    val pctStr = String.format(Locale.US, "%.1f", pct)
                    val bar = makeBar(pct)
                    val note = if (pct >= 50.0) "🌟 Nguồn thu nhập chủ lực" else "Nguồn thu bổ sung"
                    sb.append("""
 <tr>
  <td class="text-c">${idx + 1}</td>
  <td class="text-l"><b>${escapeXml(src)}</b></td>
  <td class="curr-inc">+${moneyFmt.format(amt)} ₫</td>
  <td class="text-r"><b>${pctStr}%</b></td>
  <td colspan="2" class="data-bar" style="color:#059669;">${bar} ${pctStr}%</td>
  <td class="text-l" style="color:#64748b;font-size:8.5pt;">${escapeXml(note)}</td>
 </tr>
""".trimIndent()).append("\n")
                }
                sb.append("""
 <tr class="row-total">
  <td colspan="2" class="text-c">TỔNG CỘNG THU NHẬP</td>
  <td class="curr-inc">+${moneyFmt.format(totalInc)} ₫</td>
  <td class="text-r">100.0%</td>
  <td colspan="3" class="text-l" style="color:#64748b;">Tổng số nguồn thu: ${srcIncomes.size}</td>
 </tr>
""".trimIndent()).append("\n")
            }

            sb.append("</table>\n\n")

            // PHẦN 4: TOÀN BỘ GIAO DỊCH
            sb.append("""
<!-- PHẦN 4: CHI TIẾT TOÀN BỘ GIAO DỊCH -->
<table>
 <tr>
  <td colspan="7" class="sec-hdr">4. NHẬT KÝ CHI TIẾT TOÀN BỘ GIAO DỊCH (${allSortedTxs.size} GIAO DỊCH)</td>
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
""".trimIndent()).append("\n")

            if (allSortedTxs.isEmpty()) {
                sb.append("<tr><td colspan=\"7\" class=\"text-c\" style=\"padding:16px;color:#64748b;\">Không có giao dịch nào trong khoảng thời gian đã chọn</td></tr>\n")
            } else {
                allSortedTxs.forEachIndexed { idx, tx ->
                    val isInc = tx.type == "income"
                    val typeLabel = if (isInc) "＋ Thu nhập" else "－ Chi tiêu"
                    val tagClass = if (isInc) "tag-inc" else "tag-exp"
                    val currClass = if (isInc) "curr-inc" else "curr-exp"
                    val sign = if (isInc) "+" else "−"
                    sb.append("""
 <tr>
  <td class="text-c" style="color:#64748b;">${idx + 1}</td>
  <td class="text-c">${escapeXml(formatVnDate(tx.date))}</td>
  <td class="$tagClass">$typeLabel</td>
  <td class="text-l"><b>${escapeXml(tx.categoryOrSource.ifBlank { "Khác" })}</b></td>
  <td class="text-c" style="color:#475569;">${escapeXml(tx.paymentMethod.ifBlank { "Tiền mặt" })}</td>
  <td class="$currClass">$sign${moneyFmt.format(tx.amount)} ₫</td>
  <td class="text-l" style="color:#475569;">${escapeXml(tx.note.ifBlank { "—" })}</td>
 </tr>
""".trimIndent()).append("\n")
                }
                sb.append("""
 <tr class="row-total">
  <td colspan="5" class="text-c">TỔNG KẾT: TỔNG THU (+${moneyFmt.format(totalInc)} ₫) — TỔNG CHI (−${moneyFmt.format(totalExp)} ₫)</td>
  <td class="${if (netBal >= 0) "curr-bal" else "curr-exp"}">${if (netBal >= 0) "+" else ""}${moneyFmt.format(netBal)} ₫</td>
  <td class="text-l"><b>Số dư ròng tiết kiệm được (${formattedRate}%)</b></td>
 </tr>
""".trimIndent()).append("\n")
            }

            sb.append("""
</table>

<br>
<table style="border:none;">
 <tr>
  <td colspan="7" style="border:none; text-align:right; font-style:italic; color:#64748b; font-size:8.5pt;">
   Báo cáo được trích xuất tự động từ Ứng dụng Quản trị Cá nhân LifeOS (Android) • Định dạng Microsoft Excel (.xls)
  </td>
 </tr>
</table>

</body>
</html>
""".trimIndent())

            // Save to reports directory
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanLabel = label.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val fileName = "LifeOS_ThuChi_${cleanLabel}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.xls"
            val file = File(reportsDir, fileName)
            file.writeText("\uFEFF" + sb.toString(), Charsets.UTF_8)

            // Share using FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Báo cáo Thu Chi LifeOS - $label")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Mở / Chia sẻ Báo cáo Excel (.xls)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "Đã xuất báo cáo Excel thành công!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi xuất Excel: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
