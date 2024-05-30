package top.youzhajun.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.TimeInterval;
import info.movito.themoviedbapi.tools.TmdbException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import notion.api.v1.model.pages.Page;
import org.springframework.stereotype.Service;
import top.youzhajun.exception.ServiceException;
import top.youzhajun.notion.NotionApiUtils;

import java.util.List;


/**
 * NOTION 数据更新服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotionMovieFasterService {

    private final NotionApiUtils notionApiUtils;

    @PostConstruct
    public void init() {
        TimeInterval timeInterval = new TimeInterval();
        List<Page> pages = notionApiUtils.queryMoviePage();
        if (CollUtil.isEmpty(pages)) {
            return;
        }
        try {
            notionApiUtils.updateNotionPage(pages);
        } catch (TmdbException e) {
            throw new ServiceException("tmdb 查询异常");
        }
        long interval = timeInterval.interval();
        log.info("本次执行耗时：{} ", interval);
        log.info("项目关闭，减少使用 action 免费时长 ");
    }

}
