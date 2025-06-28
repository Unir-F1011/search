package search.com.search.repository;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import search.com.search.model.consts.Consts;
import search.com.search.model.dto.ResponseItems;
import search.com.search.model.entities.Items;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ItemsRepository {

    private final String[] products = new String[] {
            Consts.PRODUCT,
            Consts.PRODUCT + "._2gram",
            Consts.PRODUCT + "._3gram",
            Consts.PRODUCT + ".prefix"
    };
    private final InnerItemsRepository repo;
    private final ElasticsearchOperations elasticClient;


    @SneakyThrows
    public ResponseItems findItems(
            String category,
            String manufacturer,
            String product,
            String page) {

        BoolQueryBuilder querySec = QueryBuilders.boolQuery();

        if (!StringUtils.isEmpty(category)) {
            querySec.must(QueryBuilders.termQuery(Consts.CATEGORY, category));
        }

        if (!StringUtils.isEmpty(manufacturer)) {
            querySec.must(QueryBuilders.termQuery(Consts.MANUFACTURER, manufacturer));
        }

        if (!StringUtils.isEmpty(product)) {
            querySec.must(
                    QueryBuilders.multiMatchQuery(product, products).type(MultiMatchQueryBuilder.Type.BOOL_PREFIX));
        }

        if (!querySec.hasClauses()) {
            querySec.must(QueryBuilders.matchAllQuery());
        }

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder().withQuery(querySec);

        int pageInt = Integer.parseInt(page);
        if (pageInt > 0) {
            queryBuilder.withPageable(PageRequest.of(pageInt - 1, 10));
        }

        SearchHits<Items> result = elasticClient.search(queryBuilder.build(), Items.class);
        return new ResponseItems(result.getSearchHits().stream().map(SearchHit::getContent).toList());
    }

    public Items save(Items item) {
        return repo.save(item);
    }

    public Items update(Items item) {
        return repo.save(item);
    }

    public Boolean delete(Items item) {
        repo.delete(item);
        return Boolean.TRUE;
    }
    
    public Optional<Items> findById(String id) {
        return repo.findById(id);
    }
}
