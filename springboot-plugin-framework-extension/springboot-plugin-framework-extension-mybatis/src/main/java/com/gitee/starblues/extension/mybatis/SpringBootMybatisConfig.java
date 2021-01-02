package com.gitee.starblues.extension.mybatis;

import org.mybatis.spring.SqlSessionFactoryBean;

/**
 * Springboot mybatis 的配置接口
 * @author starBlues
 * @version 2.3
 */
public interface SpringBootMybatisConfig extends MybatisCommonConfig{


    /**
     * 插件自主配置Mybatis的SqlSessionFactoryBean
     * SqlSessionFactoryBean 具体配置说明参考 Mybatis 官网
     * @param sqlSessionFactoryBean SqlSessionFactoryBean
     */
    default void oneselfConfig(SqlSessionFactoryBean sqlSessionFactoryBean){
    }


}
