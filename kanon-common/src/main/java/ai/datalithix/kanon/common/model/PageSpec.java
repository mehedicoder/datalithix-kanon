package ai.datalithix.kanon.common.model;

public record PageSpec(
        int pageNumber,
        int pageSize,
        String sortBy,
        SortDirection sortDirection
) {
    public PageSpec {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must be zero or greater");
        }
        if (pageSize < 1 || pageSize > 500) {
            throw new IllegalArgumentException("pageSize must be between 1 and 500");
        }
        if (sortDirection == null) {
            sortDirection = SortDirection.ASC;
        }
    }
}
