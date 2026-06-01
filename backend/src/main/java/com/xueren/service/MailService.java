package com.xueren.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Value("${xueren.mail.username:}")
    private String mailUsername;

    @Value("${xueren.mail.password:}")
    private String mailPassword;

    @Value("${xueren.mail.host:smtp.qq.com}")
    private String mailHost;

    @Value("${xueren.mail.port:587}")
    private int mailPort;

    @Async
    public void sendResetCode(String toEmail, String code) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", mailHost);
            props.put("mail.smtp.port", String.valueOf(mailPort));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUsername, mailPassword);
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(mailUsername));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            msg.setSubject("轻语 - 密码重置验证码");
            msg.setText("您的验证码是：" + code + "，有效期10分钟。");

            Transport.send(msg);
            log.info("验证码邮件已发送至 {}", toEmail);
        } catch (Exception e) {
            log.error("邮件发送失败: {}", e.getMessage());
        }
    }
}
