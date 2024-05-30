package top.youzhajun;

import cn.hutool.json.JSONUtil;
import info.movito.themoviedbapi.tools.TmdbException;
import notion.api.v1.model.pages.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.youzhajun.exception.ServiceException;
import top.youzhajun.notion.NotionApiUtils;
import top.youzhajun.service.NotionMovieFasterService;
import top.youzhajun.tmdb.util.TmdbApiUtils;

import java.util.List;

@SpringBootTest
class NotionMovieFasterApplicationTests {

    @Autowired
    private NotionApiUtils notionApiUtils;


    @Test
    void contextLoads() {
    }

}
