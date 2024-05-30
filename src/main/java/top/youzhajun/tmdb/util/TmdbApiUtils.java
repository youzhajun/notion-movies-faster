package top.youzhajun.tmdb.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbConfiguration;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.TmdbTvSeries;
import info.movito.themoviedbapi.model.configuration.Configuration;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.core.TvSeriesResultsPage;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.model.tv.series.TvSeriesDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse;
import info.movito.themoviedbapi.tools.appendtoresponse.TvSeriesAppendToResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.youzhajun.constant.NotionMovieFasterConstant;

import java.util.Collection;

/**
 * tmdb api 请求工具类
 */
@Slf4j
@Component
public class TmdbApiUtils {


    // 单例模式共享TmdbApi实例
    private TmdbApi tmdbApi;

    // 确保tmdbApiKey参数初始化后
    @PostConstruct
    public void init() {
        tmdbApi = new TmdbApi(System.getenv(NotionMovieFasterConstant.TMDB_KEY));
    }


    /**
     * 查询 tv 信息
     *
     * @param tvName
     * @param year
     * @return
     * @throws TmdbException
     */
    public TvSeriesDb queryTv(String tvName, String year) throws TmdbException {
        TvSeriesResultsPage tvSeriesResultsPage = tmdbApi.getSearch().searchTv(tvName, null, true, "zh-cn", 1,
                StrUtil.isEmpty(year) ? null : Integer.parseInt(year));
        if (CollUtil.isEmpty(tvSeriesResultsPage)) {
            return null;
        }
        int id = tvSeriesResultsPage.getResults().get(0).getId();
        TmdbTvSeries tvSeries = tmdbApi.getTvSeries();
        TvSeriesDb details = tvSeries.getDetails(id, NotionMovieFasterConstant.ZH_CN, TvSeriesAppendToResponse.CREDITS);
        log.info("tv name: {}, year: {}, detail: {}", tvName, year, JSONUtil.toJsonStr(details));
        return details;
    }

    /**
     * 查询电影信息
     *
     * @param movieName
     * @param year
     * @return
     * @throws TmdbException
     */
    public MovieDb queryMovie(String movieName, String year) throws TmdbException {
        MovieResultsPage result = tmdbApi.getSearch().searchMovie(movieName, true, NotionMovieFasterConstant.ZH_CN, null, 1, null, year);
        if (CollUtil.isEmpty(result)) {
            return null;
        }
        int id = result.getResults().get(0).getId();
        TmdbMovies tmdbMovies = tmdbApi.getMovies();
        MovieDb movieDb = tmdbMovies.getDetails(id, NotionMovieFasterConstant.ZH_CN, MovieAppendToResponse.CREDITS);
        log.info("movie name: {}, year: {}, detail: {}", movieName, year, JSONUtil.toJsonStr(movieDb));
        return movieDb;
    }


    /**
     * 获取 tmdb 配置信息
     *
     * @return
     */
    public Configuration getTmdbConfig() {
        try {
            return tmdbApi.getConfiguration().getDetails();
        } catch (TmdbException e) {
            throw new RuntimeException(e);
        }
    }


}
