package com.gitee.starblues.extension.log;

import com.gitee.starblues.extension.log.log4j.Log4jLogRegistry;
import com.gitee.starblues.extension.log.logback.LogbackLogRegistry;
import com.gitee.starblues.factory.PluginRegistryInfo;
import com.gitee.starblues.factory.process.pipe.PluginPipeProcessorExtend;
import com.gitee.starblues.utils.OrderPriority;
import com.gitee.starblues.utils.ResourceUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 接口处理者
 * @author sousouki
 * @version 2.4.3
 */
class PluginLogConfigProcessor implements PluginPipeProcessorExtend {

    private final LogRegistry logRegistry;

    public PluginLogConfigProcessor(SpringBootLogExtension.Type type){
        if(type == SpringBootLogExtension.Type.LOG4J){
            logRegistry = new Log4jLogRegistry();
        } else if(type == SpringBootLogExtension.Type.LOGBACK){
            logRegistry = new LogbackLogRegistry();
        } else {
            logRegistry = null;
        }
    }

    @Override
    public String key() {
        return "SpringBootLogConfigProcessor";
    }

    @Override
    public OrderPriority order() {
        return OrderPriority.getLowPriority();
    }

    @Override
    public void initialize() throws Exception {

    }

    @Override
    public void registry(PluginRegistryInfo pluginRegistryInfo) throws Exception {
        if (logRegistry == null) {
            return;
        }
        List<Resource> pluginResources = getLogConfigFile(pluginRegistryInfo);
        if (pluginResources == null || pluginResources.isEmpty()) {
            return;
        }
        logRegistry.registry(pluginResources, pluginRegistryInfo);
    }

    @Override
    public void unRegistry(PluginRegistryInfo pluginRegistryInfo) throws Exception {
        if (logRegistry == null) {
            return;
        }
        logRegistry.unRegistry(pluginRegistryInfo);
    }

    /**
     * 加载日志配置文件资源
     *      文件路径配置为 <p>file:D://log.xml<p> <br>
     *      resources路径配置为 <p>classpath:log.xml<p> <br>
     * @param pluginRegistryInfo 当前插件注册的信息
     * @throws IOException 获取不到配置文件异常
     **/
    private List<Resource> getLogConfigFile(PluginRegistryInfo pluginRegistryInfo) throws IOException {
        GenericApplicationContext pluginApplicationContext = pluginRegistryInfo.getPluginApplicationContext();
        String logConfigLocation = pluginApplicationContext.getEnvironment().getProperty(PropertyKey.LOG_CONFIG_LOCATION);
        if (ObjectUtils.isEmpty(logConfigLocation)) {
            return null;
        }

        ResourcePatternResolver resourcePatternResolver =
                new PathMatchingResourcePatternResolver(pluginRegistryInfo.getPluginClassLoader());
        List<Resource> resources = new ArrayList<>();
        String matchLocation = ResourceUtils.getMatchLocation(logConfigLocation);
        if (matchLocation == null || "".equals(matchLocation)) {
            return null;
        }
        Resource[] logConfigResources = resourcePatternResolver.getResources(matchLocation);
        if (logConfigResources.length != 0) {
            resources.addAll(Arrays.asList(logConfigResources));
        }
        return resources;
    }

}
