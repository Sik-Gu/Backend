package Sikgu.com.example.Web.global.apiPayload;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 오프셋 페이지네이션 응답 (docs/API.md 0.4).
 * <pre>{ "content": [], "page": 0, "size": 20, "totalElements": 24, "hasNext": true }</pre>
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.hasNext());
    }

    /** 이미 잘라둔 목록으로 만든다 */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return new PageResponse<>(content, page, size, totalElements, (long) (page + 1) * size < totalElements);
    }
}
