package Sikgu.com.example.Web.domain.post.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 공구 검색어 처리. 허용하는 검색은 두 가지뿐이다.
 * <ul>
 *   <li><b>정확한 단어 검색</b>: 검색어의 각 단어가 제목 또는 상품명에 <i>단어 그대로</i> 있어야 한다.
 *       "생수"로 "생수 공구해요"는 찾지만 "생수병"은 찾지 않는다. 여러 단어면 모두 있어야 한다(AND).</li>
 *   <li><b>초성 검색</b>: 검색어가 전부 초성(ㄱ~ㅎ)이면 제목의 초성(공백 제거)에서 찾는다.
 *       "ㅅㅅ", "ㅅㅅㄱㄱ"처럼 띄어쓰기 없이 입력해도 된다.</li>
 * </ul>
 * 완성형 글자와 초성(또는 모음)이 섞인 검색어("생ㅅ")는 허용하지 않는다.
 *
 * <p>DB에는 게시글마다 {@link #wordIndex 단어 색인}과 {@link #chosung 제목 초성}을 미리 저장해 두고,
 * 검색은 그 컬럼에 대한 LIKE로 처리한다. (PostgreSQL·H2 모두에서 동작하는 표준 SQL만 사용)
 */
public final class PostSearchKeyword {

    /** 단어 색인에서 단어를 감싸는 구분자. "% 생수 %"로 단어 경계를 표현한다 */
    public static final String WORD_DELIMITER = " ";

    private static final char[] CHOSUNG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    private static final char HANGUL_BEGIN = 0xAC00; // 가
    private static final char HANGUL_END = 0xD7A3;   // 힣
    private static final int JUNGSUNG_X_JONGSUNG = 21 * 28;

    public enum Mode {
        /** 단어 그대로 일치 */
        EXACT_WORD,
        /** 제목 초성 일치 */
        CHOSUNG
    }

    /**
     * 해석한 검색어.
     *
     * @param mode    검색 방식
     * @param words   EXACT_WORD일 때 찾을 단어들 (소문자, 중복 제거)
     * @param chosung CHOSUNG일 때 찾을 초성 (공백 제거)
     */
    public record Parsed(Mode mode, List<String> words, String chosung) {
    }

    /**
     * 검색어를 해석한다.
     *
     * @return 검색어가 비었으면 null (검색 조건 없음)
     * @throws IllegalArgumentException 완성형 글자와 자모가 섞였거나, 검색할 단어가 없는 경우
     */
    public static Parsed parse(String rawKeyword) {
        if (rawKeyword == null || rawKeyword.isBlank()) {
            return null;
        }
        String compact = rawKeyword.replaceAll("\\s+", "");
        if (isChosungOnly(compact)) {
            return new Parsed(Mode.CHOSUNG, List.of(), compact);
        }
        if (containsJamo(compact)) {
            // "생ㅅ", "ㅏㅏ"처럼 초성만으로 이루어지지 않은 자모 입력
            throw new IllegalArgumentException("완성된 단어 또는 초성만 검색할 수 있습니다.");
        }
        List<String> words = words(rawKeyword);
        if (words.isEmpty()) {
            throw new IllegalArgumentException("검색할 단어가 없습니다.");
        }
        return new Parsed(Mode.EXACT_WORD, words, null);
    }

    /**
     * 게시글 단어 색인. 앞뒤와 단어 사이를 공백 하나로 감싼다. 예: 제목 "생수 2L 공구!" → " 생수 2l 공구 "
     * 이렇게 저장하면 {@code LIKE '% 생수 %'}로 단어 경계까지 맞는지 확인할 수 있다.
     */
    public static String wordIndex(String... texts) {
        Set<String> all = new LinkedHashSet<>();
        for (String text : texts) {
            all.addAll(words(text));
        }
        return WORD_DELIMITER + String.join(WORD_DELIMITER, all) + WORD_DELIMITER;
    }

    /**
     * 초성 추출 (공백 제거). 완성형 한글은 초성으로 바꾸고, 영문·숫자 등은 그대로 둔다.
     * 예: "생수 공구해요" → "ㅅㅅㄱㄱㅎㅇ"
     */
    public static String chosung(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= HANGUL_BEGIN && c <= HANGUL_END) {
                sb.append(CHOSUNG[(c - HANGUL_BEGIN) / JUNGSUNG_X_JONGSUNG]);
            } else if (!Character.isWhitespace(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /** 공백 없는 문자열이 전부 초성(ㄱ~ㅎ, 쌍자음 포함)인지 */
    static boolean isChosungOnly(String compact) {
        if (compact.isEmpty()) {
            return false;
        }
        for (char c : compact.toCharArray()) {
            if (!isChosungChar(c)) {
                return false;
            }
        }
        return true;
    }

    /** 문자, 숫자가 아닌 글자를 경계로 단어를 나눈다 (소문자, 순서 유지, 중복 제거) */
    static List<String> words(String text) {
        if (text == null) {
            return List.of();
        }
        Set<String> words = new LinkedHashSet<>();
        StringBuilder current = new StringBuilder();
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                current.append(c);
            } else if (!current.isEmpty()) {
                words.add(current.toString());
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            words.add(current.toString());
        }
        return new ArrayList<>(words);
    }

    private static boolean isChosungChar(char c) {
        for (char cho : CHOSUNG) {
            if (cho == c) {
                return true;
            }
        }
        return false;
    }

    /** 한글 호환 자모(ㄱ~ㅣ)가 들어 있는지 */
    private static boolean containsJamo(String text) {
        for (char c : text.toCharArray()) {
            if (c >= 0x3131 && c <= 0x3163) {
                return true;
            }
        }
        return false;
    }

    private PostSearchKeyword() {
    }
}
