package ai.xiaodudou.common.dto;

import java.util.List;

/** 稳定分页响应，不暴露 ORM Page 实现。 */
public record PageResponse<T>(List<T> records, long total, long page, long size, long pages) {
    public static <T> PageResponse<T> of(List<T> records, long total, long page, long size) {
        long pages = total == 0 ? 0 : (total + size - 1) / size;
        return new PageResponse<>(records, total, page, size, pages);
    }
}
