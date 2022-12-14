/**
 * Copyright [2019-2022] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.gitee.starblues.utils;

import com.gitee.starblues.spring.ApplicationContext;
import com.gitee.starblues.spring.SpringBeanFactory;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * 自定义插件bean工具类
 * @author starBlues
 * @version 3.0.0
 */
public class SpringBeanCustomUtils {

    /**
     * 获取bean名称
     * @param applicationContext ApplicationContext
     * @return bean名称集合
     */
    public static Set<String> getBeanName(ApplicationContext applicationContext){
        SpringBeanFactory springBeanFactory = applicationContext.getSpringBeanFactory();
        String[] beanDefinitionNames = springBeanFactory.getBeanDefinitionNames();
        Set<String> set = new HashSet<>(beanDefinitionNames.length);
        set.addAll(Arrays.asList(beanDefinitionNames));
        return set;
    }

    /**
     * 得到ApplicationContext中的bean的实现
     * @param applicationContext applicationContext
     * @param aClass 接口或者抽象类型bean类型
     * @param <T> 接口或者抽象类型bean类型
     * @return 所有的实现对象
     */
    public static <T> List<T> getBeans(ApplicationContext applicationContext, Class<T> aClass) {
        SpringBeanFactory springBeanFactory = applicationContext.getSpringBeanFactory();
        Map<String, T> beansOfTypeMap = springBeanFactory.getBeansOfType(aClass);
        if(beansOfTypeMap.isEmpty()){
            return new ArrayList<>();
        }
        return new ArrayList<>(beansOfTypeMap.values());
    }

    /**
     * 得到存在的bean, 不存在则返回null
     * @param applicationContext applicationContext
     * @param aClass bean 类型
     * @param <T> bean 类型
     * @return 存在bean对象, 不存在返回null
     */
    public static <T> T getExistBean(ApplicationContext applicationContext, Class<T> aClass){
        SpringBeanFactory springBeanFactory = applicationContext.getSpringBeanFactory();
        String[] beanNamesForType = springBeanFactory.getBeanNamesForType(aClass, false, false);
        if(beanNamesForType.length > 0){
            return springBeanFactory.getBean(aClass);
        } else {
            return null;
        }
    }

    /**
     * 得到存在的bean, 不存在则返回null
     * @param applicationContext applicationContext
     * @param beanName bean 名称
     * @param <T> 返回的bean类型
     * @return 存在bean对象, 不存在返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getExistBean(ApplicationContext applicationContext, String beanName){
        SpringBeanFactory springBeanFactory = applicationContext.getSpringBeanFactory();
        if(springBeanFactory.containsBean(beanName)){
            Object bean = springBeanFactory.getBean(beanName);
            return (T) bean;
        } else {
            return null;
        }
    }

    /**
     * 通过注解获取bean
     * @param applicationContext applicationContext
     * @param annotationType 注解类型
     * @return List
     */
    public static List<Object> getBeansWithAnnotation(ApplicationContext applicationContext,
                                               Class<? extends Annotation> annotationType){
        SpringBeanFactory springBeanFactory = applicationContext.getSpringBeanFactory();
        Map<String, Object> beanMap = springBeanFactory.getBeansWithAnnotation(annotationType);
        return new ArrayList<>(beanMap.values());
    }

}
