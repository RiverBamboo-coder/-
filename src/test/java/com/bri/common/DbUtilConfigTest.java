package com.bri.common;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 只验证 {@code db.properties} 已放在 classpath 上、关键配置项齐全，<b>不连数据库</b>
 * （连库失败会拖慢构建，真实连库验证放在控制台，见 README 第 3 节）。
 *
 * @author A
 */
public class DbUtilConfigTest {

    @Test
    public void dbPropertiesShouldBeOnClasspath() throws Exception {
        InputStream in = DbUtil.class.getClassLoader().getResourceAsStream(DbUtil.CONFIG_NAME);
        assertNotNull("src/main/resources/db.properties 必须存在，否则全组都连不上库", in);

        Properties props = new Properties();
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
        }

        String url = props.getProperty(DbUtil.KEY_URL);
        assertNotNull("db.properties 缺少 " + DbUtil.KEY_URL, url);
        assertTrue("jdbc.url 必须是 MySQL 连接串：" + url, url.startsWith("jdbc:mysql://"));

        assertNotNull("db.properties 缺少 " + DbUtil.KEY_USERNAME,
                props.getProperty(DbUtil.KEY_USERNAME));
        assertEquals("com.mysql.cj.jdbc.Driver", props.getProperty(DbUtil.KEY_DRIVER));
    }

    @Test
    public void constantsShouldMatchConvention() {
        assertEquals("db.properties", DbUtil.CONFIG_NAME);
        assertEquals("bri.db.config", DbUtil.CONFIG_PATH_PROPERTY);
    }
}
