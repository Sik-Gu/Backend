package Sikgu.com.example.Web.global.infra.mail;

/**
 * 메일 발송. 서비스는 이 인터페이스에만 의존해서 테스트에서 쉽게 바꿀 수 있게 한다.
 */
public interface EmailSender {

    void send(String to, String subject, String text);
}
