package top.youzhajun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * notion 列名映射配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "notion.table-columns-mapping")
public class NotionMovieTableColumnsMappingConfiguration {

    /**
     * 标题
     */
    private String title;

    /**
     * 年份
     */
    private String year;

    /**
     * 封面
     */
    private String cover;

    /**
     * 简介
     */
    private String summary;

    /**
     * 类别
     */
    private String category;

    /**
     * 标签
     */
    private String tags;

    /**
     * 演员
     */
    private String actor;


}
