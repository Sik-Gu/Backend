package Sikgu.com.example.Web.domain.image.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ImageFormatTest {

    @Test
    void 확장자로_형식을_찾는다_대소문자_무시() {
        assertThat(ImageFormat.fromExtension("JPEG")).contains(ImageFormat.JPEG);
        assertThat(ImageFormat.fromExtension("jpg")).contains(ImageFormat.JPEG);
        assertThat(ImageFormat.fromExtension("png")).contains(ImageFormat.PNG);
        assertThat(ImageFormat.fromExtension("webp")).contains(ImageFormat.WEBP);
        assertThat(ImageFormat.fromExtension("gif")).isEmpty();
        assertThat(ImageFormat.fromExtension(null)).isEmpty();
    }

    @Test
    void WEBP는_RIFF_뒤_8바이트_위치에_WEBP가_있어야_한다() {
        byte[] webp = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
        byte[] wav = "RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1);

        assertThat(ImageFormat.WEBP.matchesSignature(webp)).isTrue();
        assertThat(ImageFormat.WEBP.matchesSignature(wav)).isFalse();
    }

    @Test
    void 시그니처보다_짧은_파일은_거부한다() {
        assertThat(ImageFormat.PNG.matchesSignature(new byte[] {(byte) 0x89, 'P'})).isFalse();
        assertThat(ImageFormat.JPEG.matchesSignature(new byte[0])).isFalse();
    }
}
