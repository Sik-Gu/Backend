package Sikgu.com.example.Web.global.infra.mail;

import Sikgu.com.example.Web.global.apiPayload.code.ErrorStatus;
import Sikgu.com.example.Web.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP로 메일을 보낸다.
 * spring.mail.host 설정이 없으면(로컬 개발) 메일을 보내지 않고 콘솔 로그로 내용을 출력한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String from;

    @Override
    public void send(String to, String subject, String text) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("[메일 미설정] spring.mail.host가 없어 메일 대신 로그로 출력합니다. to={}, subject={}\n{}",
                    to, subject, text);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (!from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("메일 발송 실패 to={}", to, e);
            throw new GeneralException(ErrorStatus.EMAIL_SEND_FAILED);
        }
    }
}
