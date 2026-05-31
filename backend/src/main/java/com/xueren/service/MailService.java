package com.xueren.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String FROM = "1037810790@qq.com";
    private static final String PWD = "kbddmkkabyibbfcc";

    public void sendResetCode(String toEmail, String code) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.qq.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(FROM, PWD);
                    }
                });

                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(FROM));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
                msg.setSubject("轻语 - 密码重置验证码");
                msg.setText("您的验证码是：" + code + "，有效期10分钟。");

                Transport.send(msg);
                log.info("验证码邮件已发送至 {}", toEmail);
            } catch (Exception e) {
                log.error("邮件发送失败: {}", e.getMessage());
            }
        }).start();
    }
}
