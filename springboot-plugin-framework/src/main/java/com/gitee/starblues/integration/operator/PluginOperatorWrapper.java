package com.gitee.starblues.integration.operator;

import com.gitee.starblues.integration.IntegrationConfiguration;
import com.gitee.starblues.integration.listener.PluginInitializerListener;
import com.gitee.starblues.integration.operator.module.PluginInfo;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 插件操作包装者
 * @author zhangzhuo
 * @version 1.0
 * @since 2020-12-30
 */
public class PluginOperatorWrapper implements PluginOperator{

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final PluginOperator pluginOperator;
    private final IntegrationConfiguration integrationConfiguration;

    public PluginOperatorWrapper(PluginOperator pluginOperator,
                                 IntegrationConfiguration integrationConfiguration) {
        this.pluginOperator = pluginOperator;
        this.integrationConfiguration = integrationConfiguration;
    }

    @Override
    public boolean initPlugins(PluginInitializerListener pluginInitializerListener) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.initPlugins(pluginInitializerListener);
    }

    @Override
    public boolean install(Path path) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.install(path);
    }

    @Override
    public boolean uninstall(String pluginId, boolean isBackup) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.uninstall(pluginId, isBackup);
    }

    @Override
    public boolean start(String pluginId) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.start(pluginId);
    }

    @Override
    public boolean stop(String pluginId) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.stop(pluginId);
    }

    @Override
    public boolean uploadPluginAndStart(MultipartFile pluginFile) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.uploadPluginAndStart(pluginFile);
    }

    @Override
    public boolean installConfigFile(Path path) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.installConfigFile(path);
    }

    @Override
    public boolean uploadConfigFile(MultipartFile configFile) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.uploadConfigFile(configFile);
    }

    @Override
    public boolean backupPlugin(Path path, String sign) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.backupPlugin(path, sign);
    }

    @Override
    public boolean backupPlugin(String pluginId, String sign) throws Exception {
        if(isDisable()){
            return false;
        }
        return pluginOperator.backupPlugin(pluginId, sign);
    }

    @Override
    public List<PluginInfo> getPluginInfo() {
        if(isDisable()){
            return Collections.emptyList();
        }
        return pluginOperator.getPluginInfo();
    }

    @Override
    public PluginInfo getPluginInfo(String pluginId) {
        if(isDisable()){
            return null;
        }
        return pluginOperator.getPluginInfo(pluginId);
    }

    @Override
    public Set<String> getPluginFilePaths() throws Exception {
        if(isDisable()){
            return Collections.emptySet();
        }
        return pluginOperator.getPluginFilePaths();
    }

    @Override
    public List<PluginWrapper> getPluginWrapper() {
        if(isDisable()){
            return Collections.emptyList();
        }
        return pluginOperator.getPluginWrapper();
    }

    @Override
    public PluginWrapper getPluginWrapper(String pluginId) {
        if(isDisable()){
            return null;
        }
        return pluginOperator.getPluginWrapper(pluginId);
    }

    /**
     * 是否被禁用
     * @return true 禁用
     */
    private boolean isDisable(){
        if(integrationConfiguration.enable()){
            return false;
        }
        // 如果禁用的话, 直接返回
        log.info("The Plugin module is disabled!");
        return true;
    }


}
