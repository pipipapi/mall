package com.macro.mall.portal;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootApplication
@MapperScan({"com.macro.mall.mapper","com.macro.mall.portal.dao"})
public class MallPortalApplication {
    @Value("${http.port}")
    private Integer port;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("classpath:dao/*.xml")
    private String mapperLocation;

    private DruidDataSource dataSource;

    private DataSourceTransactionManager transactionManager;

    private SqlSessionFactory sqlSessionFactory;
    @Autowired
    private ResourcePatternResolver resourceResolver;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMapperLocation() {
        return mapperLocation;
    }

    public void setMapperLocation(String mapperLocation) {
        this.mapperLocation = mapperLocation;
    }

    public String[] getMapperLocations() {
        String[] mapperLocations = new String[1];
        mapperLocations[0] = getMapperLocation();
        return mapperLocations;
    }

    @PostConstruct
    public void init() {
        try {
            log.info("Init datasource: url: {}", url);
            dataSource = new DruidDataSource();
            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setTestWhileIdle(true);
            dataSource.setTestOnReturn(false);
            dataSource.init();
            transactionManager = new DataSourceTransactionManager();
            transactionManager.setDataSource(dataSource);
            log.info("Init done");
        } catch (Throwable t) {
            log.error("Init error", t);        }
    }

    @PreDestroy
    public void destroy() {
        try {
            log.info("Close {}", url);
            dataSource.close();
            log.info("Close {} done", url);
        } catch (Throwable t) {
            log.error("Destroy error", t);
        }
    }

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactoryBean() throws Exception {
        SqlSessionFactory sqlSessionFactory=null;
        if (sqlSessionFactory == null) {
            SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
            Configuration config = new org.apache.ibatis.session.Configuration();
            config.setMapUnderscoreToCamelCase(true);            sqlSessionFactoryBean.setConfiguration(config);
            sqlSessionFactoryBean.setDataSource(dataSource);
            List<Resource> resources = new ArrayList<>();
            if (this.getMapperLocations() != null) {
                for (String mapperLocation : this.getMapperLocations()) {
                    try {
                        Resource[] mappers = resourceResolver.getResources(mapperLocation);
                        resources.addAll(Arrays.asList(mappers));
                    } catch (IOException e) {
                        //log.error("IOException", e);
                        return null;
                    }
                }
            }
            Resource[] arr = resources.toArray(new Resource[resources.size()]);
            sqlSessionFactoryBean.setMapperLocations(arr);
            sqlSessionFactory = sqlSessionFactoryBean.getObject();
        }
        return sqlSessionFactory;
    }

    public static void main(String[] args) {
        SpringApplication.run(MallPortalApplication.class, args);
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
        tomcat.addAdditionalTomcatConnectors(createStandardConnector()); // 添加http
        return tomcat;
    }

    //配置http
    private Connector createStandardConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(port);
        return connector;
    }
}