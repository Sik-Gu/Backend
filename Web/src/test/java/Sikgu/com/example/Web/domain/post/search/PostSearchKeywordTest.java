package Sikgu.com.example.Web.domain.post.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Sikgu.com.example.Web.domain.post.search.PostSearchKeyword.Mode;
import Sikgu.com.example.Web.domain.post.search.PostSearchKeyword.Parsed;
import org.junit.jupiter.api.Test;

class PostSearchKeywordTest {

    @Test
    void 초성은_공백을_빼고_영문_숫자는_그대로_둔다() {
        assertThat(PostSearchKeyword.chosung("생수 공구해요")).isEqualTo("ㅅㅅㄱㄱㅎㅇ");
        assertThat(PostSearchKeyword.chosung("코카콜라 1.5L")).isEqualTo("ㅋㅋㅋㄹ1.5l");
        assertThat(PostSearchKeyword.chosung("까치 떡")).isEqualTo("ㄲㅊㄸ");
    }

    @Test
    void 단어_색인은_소문자_단어를_공백으로_감싼다() {
        assertThat(PostSearchKeyword.wordIndex("생수 2L 공구!", "삼다수 2L")).isEqualTo(" 생수 2l 공구 삼다수 ");
        assertThat(PostSearchKeyword.wordIndex("코스트코/생수", null)).isEqualTo(" 코스트코 생수 ");
    }

    @Test
    void 초성만_입력하면_초성_검색() {
        Parsed parsed = PostSearchKeyword.parse("ㅅㅅ ㄱㄱ");

        assertThat(parsed.mode()).isEqualTo(Mode.CHOSUNG);
        assertThat(parsed.chosung()).isEqualTo("ㅅㅅㄱㄱ");
    }

    @Test
    void 완성된_단어는_정확한_단어_검색() {
        Parsed parsed = PostSearchKeyword.parse("  생수 WATER 생수 ");

        assertThat(parsed.mode()).isEqualTo(Mode.EXACT_WORD);
        assertThat(parsed.words()).containsExactly("생수", "water");
    }

    @Test
    void 완성형_글자와_자모가_섞이면_거절한다() {
        assertThatThrownBy(() -> PostSearchKeyword.parse("생ㅅ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PostSearchKeyword.parse("ㅏㅏ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 검색할_단어가_없으면_거절하고_빈_검색어는_조건_없음() {
        assertThatThrownBy(() -> PostSearchKeyword.parse("!!%")).isInstanceOf(IllegalArgumentException.class);
        assertThat(PostSearchKeyword.parse("   ")).isNull();
        assertThat(PostSearchKeyword.parse(null)).isNull();
    }
}
