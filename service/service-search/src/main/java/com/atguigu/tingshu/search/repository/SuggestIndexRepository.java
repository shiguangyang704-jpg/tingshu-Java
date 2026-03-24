package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author atguigu-mqx
 * @ClassName SuggestIndexRepository
 * @description: TODO
 * @date 2026年02月27日
 * @version: 1.0
 */
public interface SuggestIndexRepository extends ElasticsearchRepository<SuggestIndex,String> {
}
