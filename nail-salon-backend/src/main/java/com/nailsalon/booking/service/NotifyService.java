package com.nailsalon.booking.service;

import com.nailsalon.booking.entity.Reservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 通知服务 —— 当前支持 Telegram Bot 推送
 *
 * 📖 知识点：
 * 1. @Service — 声明这是一个 Spring Bean，自动被依赖注入管理
 * 2. @Value("${...}") — 从 application.yml / 环境变量读取配置
 * 3. RestTemplate — Spring 提供的 HTTP 客户端，简化 REST API 调用
 * 4. Telegram Bot API — 通过 HTTP POST 发送消息，无需 SDK
 */
@Slf4j
@Service
public class NotifyService {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Telegram Bot API 地址模板
     * 格式: https://api.telegram.org/bot<TOKEN>/sendMessage
     *
     * 📖 知识点：Telegram Bot API 的工作原理
     * - Bot 通过 HTTP 接口与 Telegram 服务器通信，不需要 WebSocket
     * - sendMessage 是最基本的 API，只需要 chat_id + text
     * - 支持 MarkdownV2 / HTML 格式的消息
     * - 完全免费，没有调用次数限制
     */
    private static final String TELEGRAM_API = "https://api.telegram.org/bot%s/sendMessage";

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.admin-chat-id:}")
    private String adminChatId;

    /**
     * 发送新预约通知
     *
     * @param reservation 刚创建的预约记录
     */
    public void notifyNewReservation(Reservation reservation) {
        sendTelegramMessage(
            formatReservationMessage(reservation),
            "✅ 新预约通知发送成功: {}",
            "❌ 新预约通知发送失败: {}"
        );
    }

    /**
     * 发送取消预约通知
     *
     * @param reservation 被取消的预约记录
     */
    public void notifyCancelledReservation(Reservation reservation) {
        sendTelegramMessage(
            formatCancelledReservationMessage(reservation),
            "✅ 取消预约通知发送成功: {}",
            "❌ 取消预约通知发送失败: {}"
        );
    }

    /**
     * 发送恢复预约通知
     *
     * @param reservation 被恢复的预约记录
     */
    public void notifyRestoredReservation(Reservation reservation) {
        sendTelegramMessage(
            formatRestoredReservationMessage(reservation),
            "✅ 恢复预约通知发送成功: {}",
            "❌ 恢复预约通知发送失败: {}"
        );
    }

    /**
     * 发送改期通知
     */
    public void notifyRescheduledReservation(Reservation reservation) {
        String dateStr = reservation.getReserveDate() != null
                ? reservation.getReserveDate().format(DateTimeFormatter.ofPattern("MM月dd日"))
                : "未知";

        String customerLabel = "NEW".equals(reservation.getCustomerType()) ? "🆕 新客" : "🔄 老客";

        StringBuilder sb = new StringBuilder();
        sb.append("🔄 <b>预约改期通知</b>\n");
        sb.append("━━━━━━━━━━━━━━\n");
        sb.append("👤 ").append(customerLabel).append(" | <b>").append(escapeHtml(reservation.getName())).append("</b>\n");
        sb.append("📅 新时间：").append(dateStr).append(" ").append(reservation.getTimeSlot()).append("\n");
        sb.append("📞 ").append(escapeHtml(reservation.getContactId())).append("\n");

        sendTelegramMessage(sb.toString(),
                "✅ 改期通知发送成功: {}",
                "❌ 改期通知发送失败: {}");
    }

    /**
     * 发送用户自行取消预约的通知（定金不退提醒）
     */
    public void notifyUserCancelledReservation(Reservation reservation) {
        String dateStr = reservation.getReserveDate() != null
                ? reservation.getReserveDate().format(DateTimeFormatter.ofPattern("MM月dd日"))
                : "未知";

        String customerLabel = "NEW".equals(reservation.getCustomerType()) ? "🆕 新客" : "🔄 老客";

        StringBuilder sb = new StringBuilder();
        sb.append("🙋 <b>用户自行取消预约</b>\n");
        sb.append("━━━━━━━━━━━━━━\n");
        sb.append("👤 ").append(customerLabel).append(" | <b>").append(escapeHtml(reservation.getName())).append("</b>\n");
        sb.append("📅 ").append(dateStr).append(" ").append(reservation.getTimeSlot()).append("\n");
        sb.append("📞 ").append(escapeHtml(reservation.getContactId())).append("\n");

        if ("FORFEITED".equals(reservation.getDepositStatus())) {
            sb.append("\n💰 <b>定金已没收</b> ¥").append(reservation.getDepositAmount()).append("\n");
            sb.append("客户取消，定金不退，已标记为没收。（CUSTOMER_PAID 状态同样按不退处理）\n");
        } else {
            sb.append("\n💰 该预约未付定金，无需处理。\n");
        }
        sb.append("\n⚠️ 如客户要求退款，请告知政策：取消预约定金恕不退还，如有疑问请联系店主微信。\n");

        sendTelegramMessage(sb.toString(),
                "✅ 用户取消通知发送成功: {}",
                "❌ 用户取消通知发送失败: {}");
    }

    /**
     * 发送退款通知
     */
    public void notifyDepositRefunded(Reservation reservation) {
        String dateStr = reservation.getReserveDate() != null
                ? reservation.getReserveDate().format(DateTimeFormatter.ofPattern("MM月dd日"))
                : "未知";

        StringBuilder sb = new StringBuilder();
        sb.append("↩️ <b>定金已退款</b>\n");
        sb.append("━━━━━━━━━━━━━━\n");
        sb.append("👤 <b>").append(escapeHtml(reservation.getName())).append("</b>\n");
        sb.append("📅 ").append(dateStr).append(" ").append(reservation.getTimeSlot()).append("\n");
        sb.append("💰 已退款: ¥").append(reservation.getDepositAmount()).append("\n");

        sendTelegramMessage(sb.toString(),
                "✅ 退款通知发送成功: {}",
                "❌ 退款通知发送失败: {}");
    }

    /**
     * 发送客户声称已付款通知
     * 店主收到后需检查微信/支付宝，确认收款后手动标记定金为已收款
     */
    public void notifyCustomerClaimedPaid(Reservation reservation) {
        String dateStr = reservation.getReserveDate() != null
                ? reservation.getReserveDate().format(DateTimeFormatter.ofPattern("MM月dd日"))
                : "未知";

        String customerLabel = "NEW".equals(reservation.getCustomerType()) ? "🆕 新客" : "🔄 老客";

        StringBuilder sb = new StringBuilder();
        sb.append("💰 <b>客户声称已付款，请确认收款</b>\n");
        sb.append("━━━━━━━━━━━━━━\n");
        sb.append("👤 ").append(customerLabel).append(" | <b>").append(escapeHtml(reservation.getName())).append("</b>\n");
        sb.append("📅 ").append(dateStr).append(" ").append(reservation.getTimeSlot()).append("\n");
        sb.append("📞 ").append(escapeHtml(reservation.getContactId())).append("\n");
        sb.append("💵 定金: ¥").append(reservation.getDepositAmount()).append("\n");

        if (reservation.getRemarks() != null && !reservation.getRemarks().isEmpty()) {
            sb.append("📝 备注: ").append(escapeHtml(reservation.getRemarks())).append("\n");
        }

        sb.append("\n⚠️ 请检查微信/支付宝到账记录，确认后在管理后台标记为「已收款」。\n");

        sendTelegramMessage(sb.toString(),
                "✅ 客户已付款通知发送成功: {}",
                "❌ 客户已付款通知发送失败: {}");
    }

    /**
     * 统一的 Telegram 消息发送逻辑
     * 抽出来避免在两个通知方法里重复写一样的 HTTP 调用代码
     */
    private void sendTelegramMessage(String message, String successLog, String failLog) {
        if (botToken.isEmpty() || adminChatId.isEmpty()) {
            log.warn("Telegram 通知未配置，跳过通知");
            return;
        }
        try {
            String url = String.format(TELEGRAM_API, botToken);
            Map<String, Object> body = Map.of(
                    "chat_id", adminChatId,
                    "text", message,
                    "parse_mode", "HTML"
            );
            var response = restTemplate.postForEntity(url, body, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info(successLog, message.hashCode());
            } else {
                log.warn("⚠️ Telegram 返回异常: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error(failLog, e.getMessage());
        }
    }

    /**
     * 格式化取消预约消息
     */
    private String formatCancelledReservationMessage(Reservation reservation) {
        String dateStr = reservation.getReserveDate() != null
                ? reservation.getReserveDate().format(DateTimeFormatter.ofPattern("MM月dd日"))
                : "未知";

        String customerLabel = "NEW".equals(reservation.getCustomerType()) ? "🆕 新客" : "🔄 老客";

        StringBuilder sb = new StringBuilder();
        sb.append("❌ <b>预约已取消</b>\n");
        sb.append("━━━━━━━━━━━━━━\n");
        sb.append("👤 ").append(customerLabel).append(" | <b>").append(escapeHtml(reservation.getName())).append("</b>\n");
        sb.append("📅 ").append(dateStr).append(" ").append(reservation.getTimeSlot()).append("\n");
        sb.append("📞 ").append(escapeHtml(reservation.getContactId())).append("\n");
        sb.append("🔧 卸甲: ").append(reservation.getRemovalType()).append("\n");

        if (reservation.getRemarks() != null && !reservation.getRemarks().isEmpty()) {
            sb.append("📝 备注: ").append(escapeHtml(reservation.getRemarks())).append("\n");
        }

        return sb.toString();
    }

    /**
     * 格式化恢复预约消息（✅ 绿色图标，告知老客的预约已恢复）
     */
    private String formatRestoredReservationMessage(Reservation reservation) {
        String dateStr = reservation.getReserveDate() != null
                ? reservation.getReserveDate().format(DateTimeFormatter.ofPattern("MM月dd日"))
                : "未知";

        String customerLabel = "NEW".equals(reservation.getCustomerType()) ? "🆕 新客" : "🔄 老客";

        StringBuilder sb = new StringBuilder();
        sb.append("✅ <b>预约已恢复</b>\n");
        sb.append("━━━━━━━━━━━━━━\n");
        sb.append("👤 ").append(customerLabel).append(" | <b>").append(escapeHtml(reservation.getName())).append("</b>\n");
        sb.append("📅 ").append(dateStr).append(" ").append(reservation.getTimeSlot()).append("\n");
        sb.append("📞 ").append(escapeHtml(reservation.getContactId())).append("\n");
        sb.append("🔧 卸甲: ").append(reservation.getRemovalType()).append("\n");

        if (reservation.getRemarks() != null && !reservation.getRemarks().isEmpty()) {
            sb.append("📝 备注: ").append(escapeHtml(reservation.getRemarks())).append("\n");
        }

        sb.append("\n🔄 该预约已由店主恢复，时间段重新开放给该客户。\n");
        return sb.toString();
    }

    /**
     * 格式化新预约消息（HTML 格式，Telegram 原生支持）
     *
     * 📖 知识点：HTML vs MarkdownV2
     * - Telegram 支持 HTML 和 MarkdownV2 两种格式化方式
     * - MarkdownV2 有很多转义规则（_ * [ ] ( ) ~ ` > # + - = | { } . ! 都要转义）
     * - HTML 更简单稳定，推荐使用
     */
    private String formatReservationMessage(Reservation reservation) {
        String dateStr = reservation.getReserveDate() != null
                ? reservation.getReserveDate().format(DateTimeFormatter.ofPattern("MM月dd日"))
                : "未知";

        String customerLabel = "NEW".equals(reservation.getCustomerType()) ? "🆕 新客" : "🔄 老客";
        boolean isToday = reservation.getReserveDate().equals(java.time.LocalDate.now());

        StringBuilder sb = new StringBuilder();
        sb.append("💅 <b>新预约通知</b>\n");
        sb.append("━━━━━━━━━━━━━━\n");
        sb.append("👤 ").append(customerLabel).append(" | <b>").append(escapeHtml(reservation.getName())).append("</b>\n");
        sb.append("📅 ").append(dateStr).append(" ").append(reservation.getTimeSlot()).append("\n");
        sb.append("📞 ").append(escapeHtml(reservation.getContactId())).append("\n");
        sb.append("🔧 卸甲: ").append(reservation.getRemovalType()).append("\n");

        if (reservation.getRemarks() != null && !reservation.getRemarks().isEmpty()) {
            sb.append("📝 备注: ").append(escapeHtml(reservation.getRemarks())).append("\n");
        }

        sb.append("\n⏰ ");

        // 添加创建时间
        if (reservation.getCreatedAt() != null) {
            sb.append("下单时间: ")
              .append(reservation.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        if (isToday) {
            sb.append("⚡ 今天预约的客户！\n");
        }
        return sb.toString();
    }

    /**
     * 转义 HTML 特殊字符，防止注入
     *
     * 📖 知识点：HTML 转义
     * - 用户输入的姓名/备注可能包含 < > & 等特殊字符
     * - 如果不转义，Telegram 的 HTML 解析会出错
     * - 这是所有 Web 开发都要注意的基本安全实践
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
