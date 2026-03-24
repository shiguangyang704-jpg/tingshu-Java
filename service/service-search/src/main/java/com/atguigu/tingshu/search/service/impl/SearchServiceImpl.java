package com.atguigu.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.PinYinUtils;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    //  注入一个自定义的接口;
    @Autowired
    private AlbumIndexRepository albumIndexRepository;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private CategoryFeignClient categoryFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private SuggestIndexRepository suggestIndexRepository;

    @Value("${ranking.dimension.array}")
    private String rankingDimensionArray;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void upperAlbum(Long albumId) {
        //  创建对象赋值：
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        //  赋值数据; -- 远程调用获取数据 本质：service-album 服务提供者 即获取到了album_info 数据又获取到了 album_attribute_value数据;
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
                    //  远程调用
                    Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
                    //  判断结果集：
                    Assert.notNull(albumInfoResult, "远程调用专辑数据不存在！");
                    AlbumInfo albumInfo = albumInfoResult.getData();
                    //  判断:
                    Assert.notNull(albumInfo, "远程调用专辑数据结果不存在！");
                    //  属性拷贝：
                    BeanUtils.copyProperties(albumInfo, albumInfoIndex);
                    //  属性与属性值;
                    List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
                    List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList.stream().map(albumAttributeValue -> {
                        //  创建这个对象；
                        AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                        attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
                        attributeValueIndex.setValueId(albumAttributeValue.getValueId());
                        //  返回对象;
                        return attributeValueIndex;
                    }).collect(Collectors.toList());
                    //  List<AttributeValueIndex> attributeValueIndexList private Long attributeId; private Long valueId;
                    albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
                    //  返回数据
                    return albumInfo;
                }, threadPoolExecutor)
                .exceptionally(throwable -> {
                    log.error("远程调用专辑失败！", throwable);
                    return null;
                });

//        albumInfoCompletableFuture.thenApplyAsync()
        //  分类数据获取！三级分类Id成为了已知条件;作为主键查询分类视图;
//        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
//            Result<BaseCategoryView> baseCategoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
//            //  判断
//            Assert.notNull(baseCategoryViewResult, "远程调用分类数据不存在！");
//            BaseCategoryView baseCategoryView = baseCategoryViewResult.getData();
//            Assert.notNull(baseCategoryView, "远程调用分类数据结果不存在！");
//            albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
//            albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
//        }, threadPoolExecutor);
        CompletableFuture<Integer> categoryCompletableFuture = albumInfoCompletableFuture.thenApplyAsync(albumInfo -> {
            Result<BaseCategoryView> baseCategoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            //  判断
            Assert.notNull(baseCategoryViewResult, "远程调用分类数据不存在！");
            BaseCategoryView baseCategoryView = baseCategoryViewResult.getData();
            Assert.notNull(baseCategoryView, "远程调用分类数据结果不存在！");
            albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
            albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
            //  返回数据；
            return 1;
        }, threadPoolExecutor).exceptionally(throwable -> {
            log.error("远程调用分类失败！", throwable);
            return 0;
        });

//        CompletableFuture<Void> nameCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
//            //  专辑作者;
//            Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
//            Assert.notNull(userInfoResult, "远程调用用户数据不存在！");
//            UserInfoVo userInfoVo = userInfoResult.getData();
//            Assert.notNull(userInfoVo, "远程调用用户数据结果不存在！");
//            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
//        }, threadPoolExecutor);

        CompletableFuture<Integer> nameCompletableFuture = albumInfoCompletableFuture.thenApplyAsync(albumInfo -> {
            //  专辑作者;
            Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
            Assert.notNull(userInfoResult, "远程调用用户数据不存在！");
            UserInfoVo userInfoVo = userInfoResult.getData();
            Assert.notNull(userInfoVo, "远程调用用户数据结果不存在！");
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
            //  返回数据；
            return 1;
        }, threadPoolExecutor).exceptionally(throwable -> {
            log.error("远程调用用户微服务失败！", throwable);
            return 0;
        });

        CompletableFuture<Integer> statCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //  播放量;
            Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStat(albumId);
            Assert.notNull(albumStatVoResult, "远程调用专辑统计数据不存在！");
            AlbumStatVo albumStatVo = albumStatVoResult.getData();
            Assert.notNull(albumStatVo, "远程调用专辑统计数据结果不存在！");
            albumInfoIndex.setPlayStatNum(albumStatVo.getPlayStatNum());
            albumInfoIndex.setSubscribeStatNum(albumStatVo.getSubscribeStatNum());
            albumInfoIndex.setBuyStatNum(albumStatVo.getBuyStatNum());
            albumInfoIndex.setCommentStatNum(albumStatVo.getCommentStatNum());
            //  返回数据；
            return 1;
        }, threadPoolExecutor).exceptionally(throwable -> {
            log.error("远程调用专辑统计失败！", throwable);
            return 0;
        });

//        CompletableFuture<Void> statCompletableFuture = CompletableFuture.runAsync(() -> {
//            //  播放量;
//            Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStat(albumId);
//            Assert.notNull(albumStatVoResult, "远程调用专辑统计数据不存在！");
//            AlbumStatVo albumStatVo = albumStatVoResult.getData();
//            Assert.notNull(albumStatVo, "远程调用专辑统计数据结果不存在！");
//            albumInfoIndex.setPlayStatNum(albumStatVo.getPlayStatNum());
//            albumInfoIndex.setSubscribeStatNum(albumStatVo.getSubscribeStatNum());
//            albumInfoIndex.setBuyStatNum(albumStatVo.getBuyStatNum());
//            albumInfoIndex.setCommentStatNum(albumStatVo.getCommentStatNum());
//        }, threadPoolExecutor);
        //  热度排名
        albumInfoIndex.setHotScore(new Random().nextDouble(1000));
        //  获取结果集albumInfo 为空！ 如果有失败的情况下，怎么处理?
        try {
            if (null == albumInfoCompletableFuture.get()) {
                throw new RuntimeException("远程调用专辑微服务失败！");
            }
            if (0 == categoryCompletableFuture.get()) {
                throw new RuntimeException("远程调用分类微服务失败！");
            }
            if (0 == nameCompletableFuture.get()) {
                throw new RuntimeException("远程调用用户微服务失败！");
            }
            if (0 == statCompletableFuture.get()) {
                throw new RuntimeException("远程调用专辑统计微服务失败！");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        //  多任务组合; 阻塞： join();可以实现阻塞;
        //  CompletableFuture.allOf(albumInfoCompletableFuture, categoryCompletableFuture, nameCompletableFuture, statCompletableFuture).join();
        //  保存数据; 需要使用静态映射！albumIndexRepository启动扫码自动完成静态映射！启动完成之后，主要看：attributeValueIndexList必须是Nested数据类型！
        albumIndexRepository.save(albumInfoIndex);

        //  保存提出库信息;
        //  创建一个对象：
        SuggestIndex suggestIndex = new SuggestIndex();
        //  赋值：标题; 三国演义；
        suggestIndex.setId(UUID.randomUUID().toString().replace("-", ""));
        suggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
        suggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));
        //  sanguoyanyi;
        suggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumTitle())}));
        //  sgyy
        suggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumTitle())}));
        suggestIndexRepository.save(suggestIndex);

        //  创建一个对象：存储简介
        SuggestIndex suggestIndexIntro = new SuggestIndex();
        //  赋值：标题; 三国演义；
        suggestIndexIntro.setId(UUID.randomUUID().toString().replace("-", ""));
        suggestIndexIntro.setTitle(albumInfoIndex.getAlbumIntro());
        suggestIndexIntro.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumIntro()}));
        //  sanguoyanyi;
        suggestIndexIntro.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumIntro())}));
        //  sgyy
        suggestIndexIntro.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumIntro())}));
        suggestIndexRepository.save(suggestIndexIntro);
        //  存储作者;
    }


    @Override
    public List<AlbumInfoIndex> findRankingList(Long category1Id, String rangking) {
        //  hget key field value;
        return (List<AlbumInfoIndex>) this.redisTemplate.opsForHash().get(RedisConstant.RANKING_KEY_PREFIX+category1Id, rangking);
    }

    @Override
    public void updateLatelyAlbumRanking() {
        //  定义排序规则;
        // String[] sort = new String[]{"playStatNum","hotScore","commentStatNum","subscribeStatNum","buyStatNum"};
        String[] sort = rankingDimensionArray.split(",");
        //  1.  先获取到所有的一级分类数据;
        Result<List<BaseCategory1>> baseCategory1Result = categoryFeignClient.findAllCategory1();
        Assert.notNull(baseCategory1Result, "远程调用分类微服务失败！");
        List<BaseCategory1> baseCategory1List = baseCategory1Result.getData();
        Assert.notNull(baseCategory1List, "远程调用分类微服务结果数据不存在！");
        //  循环遍历：
        for (BaseCategory1 baseCategory1 : baseCategory1List) {
            for (String rangking : sort) {
                //  获取每一个对象的一级分类Id;
                SearchResponse<AlbumInfoIndex> searchResponse = null;
                try {
                    searchResponse = elasticsearchClient.search(s -> s.index("albuminfo")
                                    .query(q -> q.term(t -> t.field("category1Id").value(baseCategory1.getId())))
                                    .sort(st -> st.field(f -> f.field(rangking).order(SortOrder.Desc)))
                                    .size(15)
                            ,
                            AlbumInfoIndex.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //  将数据保存到缓存中! String Hash List Set Zset;
                List<AlbumInfoIndex> albumInfoIndexList = searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
                //  key=category1Id field=rangking vlaue=List<AlbumInfoIndex>
                redisTemplate.opsForHash().put(RedisConstant.RANKING_KEY_PREFIX+baseCategory1.getId(),rangking,albumInfoIndexList);
            }
        }
    }

    @Override
    public List<String> completeSuggest(String keyword) {
        //  调用客户端生成dsl语句
        SearchResponse<SuggestIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(
                    s -> s.index("suggestinfo")
                            .suggest(sg -> sg.suggesters("suggest-keyword", su -> su.prefix(keyword)
                                            .completion(c -> c.field("keyword")
                                                    .skipDuplicates(true)
                                                    .fuzzy(f -> f.fuzziness("auto"))
                                                    .size(10)))
                                    .suggesters("suggest-keywordPinyin", su -> su.prefix(keyword)
                                            .completion(c -> c.field("keywordPinyin")
                                                    .skipDuplicates(true)
                                                    .fuzzy(f -> f.fuzziness("auto"))
                                                    .size(10)))
                                    .suggesters("suggest-keywordSequence", su -> su.prefix(keyword)
                                            .completion(c -> c.field("keywordSequence")
                                                    .skipDuplicates(true)
                                                    .fuzzy(f -> f.fuzziness("auto"))
                                                    .size(10)))
                            ),
                    SuggestIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //  创建一个集合：
        List<String> list = new ArrayList<>();
        //  获取数据了
        list.addAll(this.getSearchData(searchResponse,"suggest-keyword")); // searchResponse.suggest().get("suggest-keyword");
        list.addAll(this.getSearchData(searchResponse,"suggest-keywordPinyin"));  //searchResponse.suggest().get("suggest-keywordPinyin");
        list.addAll(this.getSearchData(searchResponse,"suggest-keywordSequence")); //searchResponse.suggest().get("suggest-keywordSequence");
        //  返回数据
        return list;
    }

    /**
     * 获取数据
     * @param searchResponse
     * @param keyword
     * @return
     */
    private Collection<String> getSearchData(SearchResponse<SuggestIndex> searchResponse, String keyword) {
        List<Suggestion<SuggestIndex>> suggestionList = searchResponse.suggest().get(keyword);
        //   suggestion.completion().options()
        if (!CollectionUtils.isEmpty(suggestionList)){
            for (Suggestion<SuggestIndex> suggestIndexSuggestion : suggestionList) {
                //  循环遍历;
                if (!CollectionUtils.isEmpty(suggestIndexSuggestion.completion().options())){
                    List<String> list = suggestIndexSuggestion.completion().options().stream().map(option -> option.source().getTitle()).collect(Collectors.toList());
                    return list;
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> channel(Long category1Id) {
        //  分析实现过程！
        //  1.考虑一级分类下有哪些三级分类Id; -- 已经实现了;
        Result<List<BaseCategory3>> baseCategory3ListResult = categoryFeignClient.findTopBaseCategory3(category1Id);
        //  判断
        Assert.notNull(baseCategory3ListResult, "远程调用分类数据不存在！");
        List<BaseCategory3> baseCategory3List = baseCategory3ListResult.getData();
        Assert.notNull(baseCategory3List, "远程调用分类数据结果不存在！");
        //  将这个集合转换为map集合;
        Map<Long, BaseCategory3> category3Map = baseCategory3List.stream().collect(Collectors.toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));
        //  获取到三级分类Id;
        List<Long> category3IdList = baseCategory3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());
        //  2.三级分类下有哪些专辑列表;
        //  通过dsl语句查询！
        //  需要将Long-->FileValue数据类型；
        List<FieldValue> category3IdFieldValueList = category3IdList.stream().map(category3Id -> FieldValue.of(category3Id)).collect(Collectors.toList());
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(s -> s.index("albuminfo")
                            .query(q -> q.terms(t -> t.field("category3Id").terms(ts -> ts.value(category3IdFieldValueList))))
                            .aggregations("agg_category3Id",
                                    a -> a.terms(t -> t.field("category3Id"))
                                            .aggregations("agg_topSix",
                                                    ag -> ag.topHits(tp -> tp.size(6)
                                                            .sort(st -> st.field(f -> f.field("hotScore").order(SortOrder.Desc))))
                                            )
                            )
                    ,
                    AlbumInfoIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //  从结果集中获取数据;
        Aggregate aggregate = searchResponse.aggregations().get("agg_category3Id");
        //  核心是获取：buckets
        List<Map<String, Object>> mapList = aggregate.lterms().buckets().array().stream().map(bucket -> {
            //  创建一个Map 集合
            Map<String, Object> map = new HashMap<>();
            //  获取数据;
            long category3Id = bucket.key();
            //  获取专辑集合列表; hit.source()==>AlbumInfoIndex
            List<AlbumInfoIndex> albumInfoIndexList = bucket.aggregations().get("agg_topSix").topHits().hits().hits().stream().map(hit -> JSON.parseObject(hit.source().toString(), AlbumInfoIndex.class)).collect(Collectors.toList());
            //  根据三级分类Id获取到三级分类对象;
            map.put("baseCategory3", category3Map.get(category3Id));
            map.put("list", albumInfoIndexList);
            //  返回数据
            return map;
        }).collect(Collectors.toList());
        // 返回值：List<Map>; map.put("baseCategory3","");map.put("list","");
        //  返回数据
        return mapList;
    }

    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        //  albumIndexQuery--表示接收用户输入的检索条件;
        //  第一步：先构建动态的查询请求对象SearcherRequest
        SearchRequest searchRequest = this.queryBuilderDsl(albumIndexQuery);
        //  第二步：调用查询方法
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //  第三步：从返回结果
        //  AlbumSearchResponseVo--封装用户检索的结果集;
        //  这个方法的本质：给albumSearchResponseVo对象的属性赋值; 主要赋值： albumSearchResponseVo.setList();
        AlbumSearchResponseVo albumSearchResponseVo = this.parseSearchResult(searchResponse);
        /*
            private List<AlbumInfoIndexVo> list = new ArrayList<>();
            private Long total;//总记录数
            private Integer pageSize;//每页显示的内容
            private Integer pageNo;//当前页面
            private Long totalPages;//总页数
         */
        albumSearchResponseVo.setTotal(searchResponse.hits().total().value());
        albumSearchResponseVo.setPageNo(albumIndexQuery.getPageNo());
        albumSearchResponseVo.setPageSize(albumIndexQuery.getPageSize());
        //  long totalPages = (albumSearchResponseVo.getTotal() % albumIndexQuery.getPageSize()) == 0 ? albumSearchResponseVo.getTotal() / albumIndexQuery.getPageSize() : albumSearchResponseVo.getTotal() / albumIndexQuery.getPageSize() + 1;
        //  10 3 4 | 9 3 3
        long totalPages = (albumSearchResponseVo.getTotal() + albumIndexQuery.getPageSize() - 1) / albumIndexQuery.getPageSize();
        albumSearchResponseVo.setTotalPages(totalPages);
        //  返回数据
        return albumSearchResponseVo;
    }

    /**
     * 解析结果集
     *
     * @param searchResponse
     * @return
     */
    private AlbumSearchResponseVo parseSearchResult(SearchResponse<AlbumInfoIndex> searchResponse) {
        //  创建对象;
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();
        //  给private List<AlbumInfoIndexVo> list = new ArrayList<>();这个属性赋值;
        List<AlbumInfoIndexVo> albumInfoIndexVoList = searchResponse.hits().hits().stream().map(hit -> {
            //  创建对象
            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            //  赋值;
            AlbumInfoIndex albumInfoIndex = hit.source();
            //  属性拷贝：
            BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
            //  判断是否有高亮字段;
            if (null != hit.highlight() && null != hit.highlight().get("albumTitle")) {
                //  说明有高亮字段;
                String albumTitle = hit.highlight().get("albumTitle").get(0);
                //  赋值高亮字段；
                albumInfoIndexVo.setAlbumTitle(albumTitle);
            }
            //  返回
            return albumInfoIndexVo;
        }).collect(Collectors.toList());
        //  赋值;
        albumSearchResponseVo.setList(albumInfoIndexVoList);
        //  返回当前对象；
        return albumSearchResponseVo;
    }

    /**
     * 构建查询请求对象
     *
     * @param albumIndexQuery
     * @return
     */
    private SearchRequest queryBuilderDsl(AlbumIndexQuery albumIndexQuery) {
        //  需要创建一个对象;
        //  SearchRequest searchRequest = new SearchRequest.Builder().build();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index("albuminfo");
        //  构建Query类型 bool
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        //  判断用户检索方式：入口只有两个：一个是关键词，一个是分类Id;
        if (!StringUtils.isEmpty(albumIndexQuery.getKeyword())) {
            boolQueryBuilder.should(s -> s.match(m -> m.field("albumTitle").query(albumIndexQuery.getKeyword())))
                    .should(s -> s.match(m -> m.field("albumIntro").query(albumIndexQuery.getKeyword())))
                    .should(s -> s.match(m -> m.field("announcerName").query(albumIndexQuery.getKeyword())));
            //  设置高亮字段;
            searchRequestBuilder.highlight(h -> h.fields("albumTitle", f -> f.preTags("<span style='color:red'>").postTags("</span>")));
        }

        //  根据分类Id检索
        if (!StringUtils.isEmpty(albumIndexQuery.getCategory1Id())) {
            //  bool--filter--term
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category1Id").value(albumIndexQuery.getCategory1Id())));
        }

        if (!StringUtils.isEmpty(albumIndexQuery.getCategory2Id())) {
            //  bool--filter--term
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category2Id").value(albumIndexQuery.getCategory2Id())));
        }

        if (!StringUtils.isEmpty(albumIndexQuery.getCategory3Id())) {
            //  bool--filter--term
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category3Id").value(albumIndexQuery.getCategory3Id())));
        }
        //  判断用户是否根据属性值过滤;
        List<String> attributeList = albumIndexQuery.getAttributeList();
        //  判断
        if (!CollectionUtils.isEmpty(attributeList)) {
            for (String attribute : attributeList) {
                //  attribute=属性id:属性值id
                String[] split = attribute.split(":");
                if (null != split && split.length == 2) {
                    //  split[0]=属性id split[1]=属性值id
                    boolQueryBuilder.filter(f -> f.nested(n -> n.path("attributeValueIndexList")
                            .query(q -> q.bool(b -> b.filter(f1 -> f1.term(t -> t.field("attributeValueIndexList.attributeId").value(split[0])))
                                            .filter(f2 -> f2.term(t -> t.field("attributeValueIndexList.valueId").value(split[1])))
                                    )
                            )
                    ));
                }
            }
        }
        //  排序：排序（综合排序[1:desc] 播放量[2:desc] 发布时间[3:desc]；asc:升序 desc:降序）
        String order = albumIndexQuery.getOrder();
        if (!StringUtils.isEmpty(order)) {
            //  对order进行分割; 1:desc
            String[] split = order.split(":");
            if (null != split && split.length == 2) {
                //  定义一个变量；
                String field = "";
                //  判断：
                switch (split[0]) {
                    case "1":
                        //  综合排序
                        field = "hotScore";
                        break;
                    case "2":
                        //  播放量
                        field = "playStatNum";
                        break;
                    case "3":
                        //  发布时间
                        field = "createTime";
                }
                //  添加排序规则;
                String finalField = field;
                searchRequestBuilder.sort(s -> s.field(f -> f.field(finalField).order("asc".equals(split[1]) ? SortOrder.Asc : SortOrder.Desc)));
            }
        }
        //  分页：
        Integer from = (albumIndexQuery.getPageNo() - 1) * albumIndexQuery.getPageSize();
        searchRequestBuilder.from(from);
        searchRequestBuilder.size(albumIndexQuery.getPageSize());
        //  {query}
        searchRequestBuilder.query(boolQueryBuilder.build()._toQuery());
        SearchRequest searchRequest = searchRequestBuilder.build();
        log.info("dsl:\t" + searchRequest.toString());
        //  返回对象;
        return searchRequest;
    }

    @Override
    public void lowerAlbum(Long albumId) {
        //  下架：
        albumIndexRepository.deleteById(albumId);
    }
}
