package Sikgu.com.example.Web.domain.auth.dto.request;

/**
 * 비밀번호 형식 규칙. (가정: 팀 확정 필요)
 * 아이디는 따로 두지 않고 학교 이메일로 가입·로그인한다.
 */
public final class AuthPatterns {

    /** 8~20자, 영문·숫자·특수문자를 각각 1개 이상 포함 */
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,20}$";
    public static final String PASSWORD_MESSAGE = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자입니다.";

    private AuthPatterns() {
    }
}
