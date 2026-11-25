package com.recipehub.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender sender;
    private final String frontendUrl;

    public MailService(JavaMailSender sender, @Value("${app.frontend-url}") String frontendUrl) {
        this.sender = sender;
        this.frontendUrl = frontendUrl;
    }

    public void sendVerification(String to, String rawToken) {
        send(to, "Xác thực email Recipe Hub",
                "Chào bạn,\n\nBấm vào liên kết sau để xác thực email (hạn 24 giờ):\n"
                        + frontendUrl + "/verify-email?token=" + rawToken);
    }

    public void sendPasswordReset(String to, String rawToken) {
        send(to, "Đặt lại mật khẩu Recipe Hub",
                "Chào bạn,\n\nBấm vào liên kết sau để đặt lại mật khẩu (hạn 30 phút):\n"
                        + frontendUrl + "/reset-password?token=" + rawToken
                        + "\n\nNếu bạn không yêu cầu, hãy bỏ qua email này.");
    }

    private void send(String to, String subject, String text) {
        var msg = new SimpleMailMessage();
        msg.setFrom("no-reply@recipehub.local");
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        sender.send(msg);
    }
}
