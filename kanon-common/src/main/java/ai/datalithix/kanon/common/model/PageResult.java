package ai.datalithix.kanon.common.model;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        int pageNumber,
        int pageSize,
        long totalItems
) {
    public int totalPages() {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / pageSize);
    }
}
