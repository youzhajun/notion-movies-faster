package top.youzhajun.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * notion 配置
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotionDataConfiguration {

    /**
     * notion token
     */
    private String notionToken;

    /**
     * notion db id
     */
    private String notionDatabaseId;


}
