package ai.datalithix.kanon.common.service;

import ai.datalithix.kanon.common.model.PageResult;
import ai.datalithix.kanon.common.model.QuerySpec;

public interface PagedQueryPort<T> {
    PageResult<T> findPage(QuerySpec query);
}
