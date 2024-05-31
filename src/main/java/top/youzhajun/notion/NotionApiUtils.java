package top.youzhajun.notion;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.model.configuration.Configuration;
import info.movito.themoviedbapi.model.core.Genre;
import info.movito.themoviedbapi.model.core.NamedIdElement;
import info.movito.themoviedbapi.model.movies.Cast;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.model.tv.series.TvSeriesDb;
import info.movito.themoviedbapi.tools.TmdbException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.NotionClient;
import notion.api.v1.http.OkHttp4Client;
import notion.api.v1.model.common.*;
import notion.api.v1.model.databases.DatabaseProperty;
import notion.api.v1.model.databases.QueryResults;
import notion.api.v1.model.databases.query.filter.CompoundFilter;
import notion.api.v1.model.databases.query.filter.PropertyFilter;
import notion.api.v1.model.databases.query.filter.QueryTopLevelFilter;
import notion.api.v1.model.databases.query.filter.condition.SelectFilter;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.databases.QueryDatabaseRequest;
import org.springframework.stereotype.Component;
import top.youzhajun.config.NotionDataConfiguration;
import top.youzhajun.config.NotionMovieTableColumnsMappingConfiguration;
import top.youzhajun.constant.NotionMovieFasterConstant;
import top.youzhajun.tmdb.util.TmdbApiUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Notion Api 工具类
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotionApiUtils {

    private NotionDataConfiguration notionConfig;

    // 初始化 notionConfig
    @PostConstruct
    public void init() {
        notionConfig = new NotionDataConfiguration(
                System.getenv(NotionMovieFasterConstant.NOTION_TOKEN),
                System.getenv(NotionMovieFasterConstant.NOTION_DATABASE_ID));
    }

    private final NotionMovieTableColumnsMappingConfiguration tableColumnsMappingConfiguration;

    private final TmdbApiUtils tmdbApiUtils;


    /**
     * 查询notion 电影db 的数据
     *
     * @return
     */
    public List<Page> queryMoviePage() {
        NotionClient notionClient = new NotionClient(notionConfig.getNotionToken());
        QueryResults queryResults = notionClient.queryDatabase(buildMoviePageRequest());
        notionClient.close();
        if (CollUtil.isNotEmpty(queryResults.getResults())) {
            return queryResults.getResults();
        }
        return new ArrayList<>();
    }


    /**
     * 更新电影页面
     *
     * @param pageList
     */
    public void updateNotionPage(List<Page> pageList) throws TmdbException {
        NotionClient notionClient = new NotionClient(notionConfig.getNotionToken());
        notionClient.setHttpClient(new OkHttp4Client(60000, 60000, 60000));
        for (Page page : pageList) {
            String pageCategory = getMovieCategory(page);
            HashMap<String, PageProperty> updateProperty = new HashMap<>();
            if (NotionMovieFasterConstant.CATEGORY_FOR_MOVIE.equals(pageCategory)) {
                updateNotionMoviePage(page, updateProperty);
            } else if (NotionMovieFasterConstant.CATEGORY_FOR_TV.equals(pageCategory)) {
                updateNotionTvPage(page, updateProperty);
            }
            if (CollUtil.isNotEmpty(updateProperty)) {
                notionClient.updatePage(page.getId(), updateProperty, null, null, updatePageCover(updateProperty));
                log.info("成功同步：{}", getMovieName(page));
            }
        }
        notionClient.close();
    }


    /**
     * 更新 tv 信息
     *
     * @param page
     * @param updateProperty
     */
    private void updateNotionTvPage(Page page, HashMap<String, PageProperty> updateProperty) throws TmdbException {
        TvSeriesDb tvSeriesDb = tmdbApiUtils.queryTv(getMovieName(page), getPageYear(page));
        if (null == tvSeriesDb) {
            return;
        }
        setNotionPageYear(page, String.valueOf(DateUtil.year(DateUtil.parse(tvSeriesDb.getFirstAirDate()))), updateProperty);
        setNotionPageCover(page, tvSeriesDb.getPosterPath(), updateProperty);
        setNotionTags(page, tvSeriesDb.getGenres(), updateProperty);
        List<String> actorList = tvSeriesDb.getCredits().getCast().stream().map(NamedIdElement::getName).toList();
        setNotionActor(page, actorList, updateProperty);
        setNotionSummary(page, tvSeriesDb.getOverview(), updateProperty);
    }


    /**
     * 更新 电影信息
     *
     * @param page
     * @param updateProperty
     */
    private void updateNotionMoviePage(Page page, HashMap<String, PageProperty> updateProperty) throws TmdbException {
        MovieDb movieDb = tmdbApiUtils.queryMovie(getMovieName(page), getPageYear(page));
        if (null == movieDb) {
            return;
        }
        DateTime parse = DateUtil.parse(movieDb.getReleaseDate());
        setNotionPageYear(page, String.valueOf(DateUtil.year(parse)), updateProperty);
        setNotionPageCover(page, movieDb.getPosterPath(), updateProperty);
        setNotionTags(page, movieDb.getGenres(), updateProperty);
        List<String> actorList = movieDb.getCredits().getCast().stream().map(Cast::getName).toList();
        setNotionActor(page, actorList, updateProperty);
        setNotionSummary(page, movieDb.getOverview(), updateProperty);
    }


    /**
     * 创建查询电影页面构造器
     *
     * @return
     */
    private QueryDatabaseRequest buildMoviePageRequest() {
        QueryDatabaseRequest request = new QueryDatabaseRequest(notionConfig.getNotionDatabaseId());
        request.setFilter(buildMoviePageFilter());
        return request;
    }


    /**
     * 构造查询电影页面过滤器
     * 查询条件： category 类型为电影 or 剧集
     *
     * @return
     */
    private QueryTopLevelFilter buildMoviePageFilter() {
        CompoundFilter queryTopLevelFilter = new CompoundFilter();
        queryTopLevelFilter.setOr(Arrays.asList(
                createPropertyFilter(tableColumnsMappingConfiguration.getCategory(), NotionMovieFasterConstant.CATEGORY_FOR_TV),
                createPropertyFilter(tableColumnsMappingConfiguration.getCategory(), NotionMovieFasterConstant.CATEGORY_FOR_MOVIE))
        );
        return queryTopLevelFilter;
    }


    /**
     * 创建参数查询器
     *
     * @param propertyName
     * @param propertyValue
     * @return
     */
    private static PropertyFilter createPropertyFilter(String propertyName, String propertyValue) {
        PropertyFilter propertyFilter = new PropertyFilter(propertyName);
        SelectFilter selectFilter = new SelectFilter();
        selectFilter.setEquals(propertyValue);
        propertyFilter.setSelect(selectFilter);
        return propertyFilter;
    }


    /**
     * 获取电影名称
     *
     * @param moviePage
     * @return
     */
    private String getMovieName(Page moviePage) {
        return moviePage.getProperties().get(tableColumnsMappingConfiguration.getTitle()).getTitle().get(0).getText().getContent();
    }


    /**
     * 获取电影类别
     *
     * @param moviePage
     * @return
     */
    private String getMovieCategory(Page moviePage) {
        return moviePage.getProperties().get(tableColumnsMappingConfiguration.getCategory()).getSelect().getName();
    }


    /**
     * 获取年份
     *
     * @param moviePage
     * @return
     */
    private String getPageYear(Page moviePage) {
        List<PageProperty.RichText> title = moviePage.getProperties().get(tableColumnsMappingConfiguration.getYear()).getTitle();
        if (CollUtil.isEmpty(title) || null == title.get(0).getText()) {
            return null;
        }
        return title.get(0).getText().getContent();
    }


    /**
     * 设置电影简介
     *
     * @param page
     * @param overview
     * @param updateProperty
     */
    private void setNotionSummary(Page page, String overview, HashMap<String, PageProperty> updateProperty) {
        List<PageProperty.RichText> richTextList = page.getProperties().get(tableColumnsMappingConfiguration.getSummary()).getTitle();
        if (CollUtil.isEmpty(richTextList)) {
            PageProperty property = new PageProperty();
            property.setType(PropertyType.RichText);
            property.setRichText(Arrays.asList(new PageProperty.RichText(RichTextType.Text, new PageProperty.RichText.Text(StrUtil.trim(overview)))));
            updateProperty.put(tableColumnsMappingConfiguration.getSummary(), property);
        }
    }

    /**
     * 设置演员
     *
     * @param page
     * @param actorList
     * @param updateProperty
     */
    private void setNotionActor(Page page, List<String> actorList, HashMap<String, PageProperty> updateProperty) {
        List<DatabaseProperty.MultiSelect.Option> multiSelect = page.getProperties().get(tableColumnsMappingConfiguration.getActor()).getMultiSelect();
        if (CollUtil.isEmpty(actorList)) {
            return;
        }
        if (CollUtil.isNotEmpty(multiSelect)) {
            List<String> oldChecked = multiSelect.stream().map(DatabaseProperty.MultiSelect.Option::getName).collect(Collectors.toList());
            if (CollUtil.containsAll(oldChecked, actorList)) {
                return;
            }
        }
        PageProperty pageProperty = new PageProperty();
        pageProperty.setType(PropertyType.MultiSelect);
        List<DatabaseProperty.MultiSelect.Option> actors = new ArrayList<>();
        actorList = ListUtil.sub(actorList, 0, 5);
        for (String actorName : actorList) {
            actors.add(new DatabaseProperty.MultiSelect.Option(null, actorName, null, null));
        }
        pageProperty.setMultiSelect(actors);
        updateProperty.put(tableColumnsMappingConfiguration.getActor(), pageProperty);
    }

    /**
     * 设置标签
     *
     * @param page
     * @param genres
     * @param updateProperty
     */
    private void setNotionTags(Page page, List<Genre> genres, HashMap<String, PageProperty> updateProperty) {
        List<DatabaseProperty.MultiSelect.Option> multiSelect = page.getProperties().get(tableColumnsMappingConfiguration.getTags()).getMultiSelect();
        if (CollUtil.isNotEmpty(multiSelect) || CollUtil.isEmpty(genres)) {
            return;
        }
        PageProperty pageProperty = new PageProperty();
        pageProperty.setType(PropertyType.MultiSelect);
        List<DatabaseProperty.MultiSelect.Option> tags = new ArrayList<>();
        for (Genre genre : genres) {
            tags.add(new DatabaseProperty.MultiSelect.Option(null, genre.getName(), null, null));
        }
        pageProperty.setMultiSelect(tags);
        updateProperty.put(tableColumnsMappingConfiguration.getTags(), pageProperty);
    }

    /**
     * 设置封面
     *
     * @param page
     * @param posterPath
     * @param updateProperty
     */
    private void setNotionPageCover(Page page, String posterPath, HashMap<String, PageProperty> updateProperty) {
        Configuration tmdbConfig = tmdbApiUtils.getTmdbConfig();
        tmdbConfig.getImageConfig().getBaseUrl();
        String coverUrl = new StringBuilder()
                .append(tmdbConfig.getImageConfig().getSecureBaseUrl())
                .append(NotionMovieFasterConstant.ORIGINAL)
                .append(posterPath).toString();
        List<PageProperty.File> fileList = page.getProperties().get(tableColumnsMappingConfiguration.getCover()).getFiles();
        if (CollUtil.isEmpty(fileList) ||
                ObjUtil.isNull(fileList.get(0)) ||
                !StrUtil.equals(fileList.get(0).getExternal().getUrl(), coverUrl)
        ) {
            PageProperty pageProperty = new PageProperty();
            pageProperty.setType(PropertyType.Files);
            pageProperty.setFiles(Arrays.asList(new PageProperty.File(getMovieName(page), FileType.External, null, new ExternalFileDetails(coverUrl))));
            updateProperty.put(tableColumnsMappingConfiguration.getCover(), pageProperty);
        }
    }


    /**
     * 设置年份
     *
     * @param page
     * @param year
     * @param updateProperty
     */
    private void setNotionPageYear(Page page, String year, HashMap<String, PageProperty> updateProperty) {
        if (null == getPageYear(page)) {
            PageProperty property = new PageProperty();
            property.setRichText(List.of(new PageProperty.RichText(RichTextType.Text, new PageProperty.RichText.Text(year))));
            updateProperty.put(tableColumnsMappingConfiguration.getYear(), property);
        }
    }


    /**
     * 设置封面
     *
     * @param updateProperty
     * @return
     */
    private File updatePageCover(HashMap<String, PageProperty> updateProperty) {
        if (updateProperty.containsKey(tableColumnsMappingConfiguration.getCover())) {
            PageProperty.File file = updateProperty.get(tableColumnsMappingConfiguration.getCover()).getFiles().get(0);
            return new File(FileType.External, file.getExternal());
        }
        return null;
    }


}
