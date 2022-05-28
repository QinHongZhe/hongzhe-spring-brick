package com.gitee.starblues.bootstrap.annotation;


import java.lang.annotation.*;

/**
 * 注入类型
 *
 * @author starBlues
 * @version 3.0.3
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutowiredType {

    /**
     * 插件Bean注入类型
     * @return Type
     */
    Type value() default Type.PLUGIN_MAIN;

    enum Type{
        /**
         * Bean 注入类型(默认): 先插件后主程序
         */
        PLUGIN_MAIN,

        /**
         * Bean 注入类型: 先主程序后插件
         */
        MAIN_PLUGIN,

        /**
         * Bean 注入类型: 仅插件
         */
        PLUGIN,

        /**
         *  Bean 注入类型: 仅主程序
         */
        MAIN
    }


}
