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

package com.gitee.starblues.core;

import com.gitee.starblues.core.checker.ComposePluginLauncherChecker;
import com.gitee.starblues.core.checker.DefaultPluginLauncherChecker;
import com.gitee.starblues.core.checker.DependencyPluginLauncherChecker;
import com.gitee.starblues.core.checker.PluginBasicChecker;
import com.gitee.starblues.core.descriptor.InsidePluginDescriptor;
import com.gitee.starblues.core.descriptor.PluginDescriptor;
import com.gitee.starblues.core.descriptor.PluginDescriptorLoader;
import com.gitee.starblues.core.exception.PluginDisabledException;
import com.gitee.starblues.core.exception.PluginException;
import com.gitee.starblues.core.scanner.ComposePathResolve;
import com.gitee.starblues.core.scanner.DevPathResolve;
import com.gitee.starblues.core.scanner.PathResolve;
import com.gitee.starblues.core.scanner.ProdPathResolve;
import com.gitee.starblues.integration.IntegrationConfiguration;
import com.gitee.starblues.integration.listener.DefaultPluginListenerFactory;
import com.gitee.starblues.integration.listener.PluginListenerFactory;
import com.gitee.starblues.utils.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * ????????????????????????
 * @author starBlues
 * @version 3.0.3
 */
public class DefaultPluginManager implements PluginManager{

    private final Logger log = LoggerFactory.getLogger(DefaultPluginManager.class);

    private final RealizeProvider provider;
    private final IntegrationConfiguration configuration;
    private final List<String> pluginRootDirs;

    private final PathResolve pathResolve;
    private final PluginBasicChecker basicChecker;

    protected final ComposePluginLauncherChecker launcherChecker;

    private final AtomicBoolean loaded = new AtomicBoolean(false);

    private final Map<String, PluginInsideInfo> startedPlugins = new ConcurrentHashMap<>();
    private final Map<String, PluginInsideInfo> resolvedPlugins = new ConcurrentHashMap<>();

    protected PluginListenerFactory pluginListenerFactory;


    private List<String> sortedPluginIds;

    public DefaultPluginManager(RealizeProvider realizeProvider, IntegrationConfiguration configuration) {
        this.provider = Assert.isNotNull(realizeProvider, "?????? realizeProvider ????????????");
        this.configuration = Assert.isNotNull(configuration, "?????? configuration ????????????");
        this.pluginRootDirs = resolvePath(configuration.pluginPath());
        this.pathResolve = getComposePathResolve();
        this.basicChecker = realizeProvider.getPluginBasicChecker();
        this.launcherChecker = getComposeLauncherChecker(realizeProvider);
        setSortedPluginIds(configuration.sortInitPluginIds());
    }

    protected ComposePluginLauncherChecker getComposeLauncherChecker(RealizeProvider realizeProvider){
        ComposePluginLauncherChecker checker = new ComposePluginLauncherChecker();
        checker.add(new DefaultPluginLauncherChecker(realizeProvider, configuration));
        checker.add(new DependencyPluginLauncherChecker(this));
        return checker;
    }

    protected ComposePathResolve getComposePathResolve(){
        return new ComposePathResolve(new DevPathResolve(), new ProdPathResolve());
    }

    public void setSortedPluginIds(List<String> sortedPluginIds) {
        this.sortedPluginIds = sortedPluginIds;
    }

    @Override
    public List<String> getPluginsRoots() {
        return new ArrayList<>(pluginRootDirs);
    }

    @Override
    public String getDefaultPluginRoot() {
        if(pluginRootDirs == null){
            return null;
        }
        return pluginRootDirs.stream().findFirst().orElseThrow(()->{
            return new PluginException("????????????????????????");
        });
    }

    @Override
    public synchronized List<PluginInfo> loadPlugins() {
        if(loaded.get()){
            throw new PluginException("??????????????????: loadPlugins");
        }
        try {
            pluginListenerFactory = createPluginListenerFactory();
            if(ObjectUtils.isEmpty(pluginRootDirs)){
                log.warn("?????????????????????, ??????????????????.");
                return Collections.emptyList();
            }
            List<Path> scanPluginPaths = provider.getPluginScanner().scan(pluginRootDirs);
            if(ObjectUtils.isEmpty(scanPluginPaths)){
                printOfNotFoundPlugins();
                return Collections.emptyList();
            }
            Map<String, PluginInfo> pluginInfoMap = new LinkedHashMap<>(scanPluginPaths.size());
            boolean findException = false;
            for (Path path : scanPluginPaths) {
                try {
                    PluginInsideInfo pluginInfo = loadPlugin(path, false);
                    if(pluginInfo != null){
                        pluginInfo.setFollowSystem();
                        PluginInfo pluginInfoFace = pluginInfo.toPluginInfo();
                        pluginListenerFactory.loadSuccess(pluginInfoFace);
                        pluginInfoMap.put(pluginInfo.getPluginId(), pluginInfoFace);
                    }
                } catch (Throwable e) {
                    pluginListenerFactory.loadFailure(path, e);
                    log.error("?????????????????????: {}. {}", path, e.getMessage(), e);
                    findException = true;
                }
            }
            if(!findException && pluginInfoMap.isEmpty()){
                printOfNotFoundPlugins();
            }
            return getSortPlugin(pluginInfoMap);
        } finally {
            loaded.set(true);
        }
    }

    protected PluginListenerFactory createPluginListenerFactory(){
        return new DefaultPluginListenerFactory();
    }

    @Override
    public boolean verify(Path pluginPath) {
        Assert.isNotNull(pluginPath, "??????pluginPath????????????");
        try (PluginDescriptorLoader pluginDescriptorLoader = provider.getPluginDescriptorLoader()){
            basicChecker.checkPath(pluginPath);
            PluginDescriptor pluginDescriptor = pluginDescriptorLoader.load(pluginPath);
            return pluginDescriptor != null;
        } catch (Throwable e) {
            log.error("?????????????????????: {}" , pluginPath, e);
            return false;
        }
    }

    @Override
    public PluginInfo parse(Path pluginPath) throws PluginException {
        PluginInsideInfo pluginInsideInfo = loadFromPath(pluginPath);
        if(pluginInsideInfo == null){
            throw new PluginException("???????????????: " + pluginPath);
        }
        return pluginInsideInfo.toPluginInfo();
    }

    @Override
    public synchronized PluginInfo load(Path pluginPath, boolean unpackPlugin) throws PluginException {
        Assert.isNotNull(pluginPath, "??????pluginPath????????????");
        String sourcePluginPath = pluginPath.toString();
        try {
            // ????????????
            PluginInfo pluginInfo = parse(pluginPath);
            // ??????????????????????????????
            PluginInsideInfo plugin = getPlugin(pluginInfo.getPluginId());
            if(plugin != null){
                // ?????????????????????
                throw new PluginException("???????????????[" + pluginPath + "]??????. ?????????????????????: " +
                        MsgUtils.getPluginUnique(plugin.getPluginDescriptor()));
            }
            if(configuration.isProd()){
                // ?????????????????????, ???????????????
                pluginPath = copyPlugin(pluginPath, unpackPlugin);
            }
            // ????????????
            PluginInsideInfo pluginInsideInfo = loadPlugin(pluginPath, true);
            if(pluginInsideInfo != null){
                PluginInfo pluginInfoFace = pluginInsideInfo.toPluginInfo();
                pluginListenerFactory.loadSuccess(pluginInfoFace);
                return pluginInfoFace;
            } else {
                pluginListenerFactory.loadFailure(pluginPath, new PluginException("Not found PluginInsideInfo"));
                return null;
            }
        } catch (Throwable e) {
            PluginException pluginException = PluginException.getPluginException(e, () -> {
                throw new PluginException("?????????????????????: " + sourcePluginPath, e);
            });
            pluginListenerFactory.loadFailure(pluginPath, pluginException);
            throw pluginException;
        }
    }

    @Override
    public synchronized void unLoad(String pluginId) {
        Assert.isNotNull(pluginId, "??????pluginId????????????");
        PluginInsideInfo pluginInsideInfo = resolvedPlugins.get(pluginId);
        if(!resolvedPlugins.containsKey(pluginId)){
            throw new PluginException("??????????????????: " + pluginId);
        }
        resolvedPlugins.remove(pluginId);
        pluginListenerFactory.unLoadSuccess(pluginInsideInfo.toPluginInfo());
        LogUtils.info(log, pluginInsideInfo.getPluginDescriptor(), "????????????");
    }

    @Override
    public synchronized PluginInfo install(Path pluginPath, boolean unpackPlugin) throws PluginException {
        Assert.isNotNull(pluginPath, "??????pluginPath????????????");
        PluginInfo loadPluginInfo = load(pluginPath, unpackPlugin);
        if(loadPluginInfo == null){
            throw new PluginException("?????????????????????: " + pluginPath);
        }
        PluginInsideInfo pluginInsideInfo = resolvedPlugins.get(loadPluginInfo.getPluginId());
        PluginInfo pluginInfo = pluginInsideInfo.toPluginInfo();
        try {
            start(pluginInsideInfo);
            pluginListenerFactory.startSuccess(pluginInfo);
            log.info("??????[{}]????????????", MsgUtils.getPluginUnique(pluginInsideInfo.getPluginDescriptor()));
            return pluginInsideInfo.toPluginInfo();
        } catch (Throwable e){
            if(e instanceof PluginDisabledException){
                throw (PluginDisabledException)e;
            }
            PluginException pluginException = PluginException.getPluginException(e, ()-> {
                unLoad(loadPluginInfo.getPluginId());
                throw new PluginException("?????????[ " + pluginPath + " ]??????: " + e.getMessage(), e);
            });
            pluginListenerFactory.startFailure(pluginInfo, pluginException);
            throw pluginException;
        }
    }

    @Override
    public synchronized void uninstall(String pluginId) throws PluginException {
        Assert.isNotNull(pluginId, "??????pluginId????????????");
        PluginInsideInfo wrapperInside = getPlugin(pluginId);
        if(wrapperInside == null){
            throw new PluginException("??????????????????: " + pluginId);
        }
        PluginInfo pluginInfo = wrapperInside.toPluginInfo();
        if(wrapperInside.getPluginState() == PluginState.STARTED){
            try {
                stop(wrapperInside);
                pluginListenerFactory.stopSuccess(pluginInfo);
            } catch (Throwable e) {
                PluginException pluginException = PluginException.getPluginException(e,
                        ()-> new PluginException("??????", pluginId, e));
                pluginListenerFactory.stopFailure(pluginInfo, pluginException);
                throw pluginException;
            }
        }
        startedPlugins.remove(pluginId);
        unLoad(pluginId);
        LogUtils.info(log, wrapperInside.getPluginDescriptor(), "????????????");
    }

    @Override
    public synchronized PluginInfo upgrade(Path pluginPath, boolean unpackPlugin) throws PluginException {
        Assert.isNotNull(pluginPath, "??????pluginPath????????????");
        // ???????????????
        PluginInfo upgradePlugin = parse(pluginPath);
        if(upgradePlugin == null){
            throw new PluginException("???????????????: " + pluginPath);
        }
        // ???????????????????????????
        PluginDisabledException.checkDisabled(upgradePlugin, configuration, "??????");
        String pluginId = upgradePlugin.getPluginId();
        // ???????????????
        PluginInsideInfo oldPlugin = getPlugin(pluginId);
        if(oldPlugin == null){
            // ???????????????, ????????????????????????
            return install(pluginPath, unpackPlugin);
        }
        // ??????????????????
        PluginDescriptor upgradePluginDescriptor = upgradePlugin.getPluginDescriptor();
        checkVersion(oldPlugin.getPluginDescriptor(), upgradePluginDescriptor);
        if(oldPlugin.getPluginState() == PluginState.STARTED){
            // ?????????????????????, ?????????????????????
            uninstall(pluginId);
        } else if(oldPlugin.getPluginState() == PluginState.LOADED){
            // ???????????????load
            unLoad(pluginId);
        }
        try {
            // ???????????????
            install(pluginPath, unpackPlugin);
            log.info("????????????[{}]??????", MsgUtils.getPluginUnique(upgradePluginDescriptor));
            return upgradePlugin;
        } catch (Throwable e){
            throw PluginException.getPluginException(e, ()->
                    new PluginException(upgradePluginDescriptor, "??????", e));
        }
    }

    @Override
    public synchronized PluginInfo start(String pluginId) throws PluginException {
        if(ObjectUtils.isEmpty(pluginId)){
            return null;
        }
        PluginInsideInfo pluginInsideInfo = getPlugin(pluginId);
        if(pluginInsideInfo == null){
            throw new PluginException("??????????????????: " + pluginId);
        }
        PluginInfo pluginInfo = pluginInsideInfo.toPluginInfo();
        try {
            start(pluginInsideInfo);
            log.info("??????[{}]????????????", MsgUtils.getPluginUnique(pluginInsideInfo.getPluginDescriptor()));
            pluginListenerFactory.startSuccess(pluginInfo);
            return pluginInfo;
        } catch (Throwable e){
            PluginException pluginException = PluginException.getPluginException(e,
                    ()-> new PluginException(pluginInsideInfo.getPluginDescriptor(), "??????", e));
            pluginListenerFactory.startFailure(pluginInfo, pluginException);
            throw pluginException;
        }
    }

    @Override
    public synchronized PluginInfo stop(String pluginId) throws PluginException {
        if(ObjectUtils.isEmpty(pluginId)){
            return null;
        }
        PluginInsideInfo pluginInsideInfo = startedPlugins.get(pluginId);
        if(pluginInsideInfo == null){
            throw new PluginException("??????????????????: " + pluginId);
        }
        PluginInfo pluginInfo = pluginInsideInfo.toPluginInfo();
        try {
            stop(pluginInsideInfo);
            log.info("????????????[{}]??????", MsgUtils.getPluginUnique(pluginInsideInfo.getPluginDescriptor()));
            pluginListenerFactory.stopSuccess(pluginInfo);
            return pluginInfo;
        } catch (Throwable e) {
            PluginException pluginException = PluginException.getPluginException(e,
                    () -> new PluginException(pluginInsideInfo.getPluginDescriptor(), "??????", e));
            pluginListenerFactory.stopFailure(pluginInfo, pluginException);
            throw pluginException;
        }
    }

    @Override
    public synchronized PluginInfo getPluginInfo(String pluginId) {
        if(ObjectUtils.isEmpty(pluginId)){
            return null;
        }
        PluginInsideInfo wrapperInside = startedPlugins.get(pluginId);
        if(wrapperInside == null){
            wrapperInside = resolvedPlugins.get(pluginId);
        }
        if(wrapperInside != null){
            return wrapperInside.toPluginInfo();
        } else {
            return null;
        }
    }

    @Override
    public synchronized List<PluginInfo> getPluginInfos() {
        List<PluginInfo> pluginDescriptors = new ArrayList<>(
                resolvedPlugins.size() + startedPlugins.size());
        for (PluginInsideInfo wrapperInside : startedPlugins.values()) {
            pluginDescriptors.add(wrapperInside.toPluginInfo());
        }
        for (PluginInsideInfo wrapperInside : resolvedPlugins.values()) {
            pluginDescriptors.add(wrapperInside.toPluginInfo());
        }
        return pluginDescriptors;
    }

    protected PluginInsideInfo loadPlugin(Path pluginPath, boolean resolvePath) {
        if(resolvePath){
            Path sourcePluginPath = pluginPath;
            pluginPath = pathResolve.resolve(pluginPath);
            if(pluginPath == null){
                throw new PluginException("???????????????: " + sourcePluginPath);
            }
        }
        PluginInsideInfo pluginInsideInfo = loadFromPath(pluginPath);
        if(pluginInsideInfo == null){
            return null;
        }
        String pluginId = pluginInsideInfo.getPluginId();
        if(resolvedPlugins.containsKey(pluginId)){
            throw new PluginException(pluginInsideInfo.getPluginDescriptor(), "???????????????");
        }
        // ???????????????????????????????????????
        provider.getVersionInspector().check(pluginInsideInfo.getPluginDescriptor().getPluginVersion());
        resolvedPlugins.put(pluginId, pluginInsideInfo);
        LogUtils.info(log, pluginInsideInfo.getPluginDescriptor(), "????????????");
        return pluginInsideInfo;
    }

    protected PluginInsideInfo loadFromPath(Path pluginPath) {
        try {
            basicChecker.checkPath(pluginPath);
        } catch (Throwable e) {
            throw PluginException.getPluginException(e, ()-> {
                return new PluginException("???????????????. " + e.getMessage(), e);
            });
        }

        try (PluginDescriptorLoader pluginDescriptorLoader = provider.getPluginDescriptorLoader()){
            InsidePluginDescriptor pluginDescriptor = pluginDescriptorLoader.load(pluginPath);
            if(pluginDescriptor == null){
                return null;
            }
            String pluginId = pluginDescriptor.getPluginId();
            PluginInsideInfo pluginInsideInfo = new DefaultPluginInsideInfo(pluginDescriptor);
            if(configuration.isDisabled(pluginId)){
                pluginInsideInfo.setPluginState(PluginState.DISABLED);
            } else {
                pluginInsideInfo.setPluginState(PluginState.LOADED);
            }
            return pluginInsideInfo;
        } catch (Throwable e){
            throw PluginException.getPluginException(e, ()-> new PluginException("??????????????????"));
        }
    }

    /**
     * ????????????????????????????????????
     * @param pluginPath ?????????????????????
     * @param unpackPlugin ?????????????????????. ????????????????????????????????????
     * @return ????????????????????????
     * @throws IOException IO ??????
     */
    protected Path copyPlugin(Path pluginPath, boolean unpackPlugin) throws IOException {
        if(configuration.isDev()){
            return pluginPath;
        }
        File targetFile = pluginPath.toFile();
        if(!targetFile.exists()) {
            throw new PluginException("?????????????????????: " + pluginPath);
        }
        String targetFileName = targetFile.getName();
        // ???????????????????????????????????????????????????
        File pluginRootDir = null;
        for (String dir : pluginRootDirs) {
            File rootDir = new File(dir);
            if(targetFile.getParentFile().compareTo(rootDir) == 0){
                pluginRootDir = rootDir;
                break;
            }
        }
        String resolvePluginFileName = unpackPlugin ? PluginFileUtils.getFileName(targetFile) : targetFileName;
        Path resultPath = null;
        if(pluginRootDir != null){
            // ?????????????????????
            if(targetFile.isFile() && unpackPlugin){
                // ????????????, ??????????????????????????????????????????????????????
                checkExistFile(pluginRootDir, resolvePluginFileName);
                String unpackPluginPath = FilesUtils.joiningFilePath(pluginRootDir.getPath(), resolvePluginFileName);
                PluginFileUtils.decompressZip(targetFile.getPath(), unpackPluginPath);
                resultPath = Paths.get(unpackPluginPath);
                PluginFileUtils.deleteFile(targetFile);
            } else {
                resultPath = targetFile.toPath();
            }
        } else {
            File pluginFile = pluginPath.toFile();
            pluginRootDir = new File(getDefaultPluginRoot());
            File pluginRootDirFile = new File(getDefaultPluginRoot());
            // ??????????????????????????????
            checkExistFile(pluginRootDirFile, resolvePluginFileName);
            targetFile = Paths.get(FilesUtils.joiningFilePath(pluginRootDir.getPath(), resolvePluginFileName)).toFile();
            if(pluginFile.isFile()){
                if(unpackPlugin){
                    // ????????????
                    String unpackPluginPath = FilesUtils.joiningFilePath(pluginRootDir.getPath(), resolvePluginFileName);
                    PluginFileUtils.decompressZip(pluginFile.getPath(), unpackPluginPath);
                    resultPath = Paths.get(unpackPluginPath);
                } else {
                    FileUtils.copyFile(pluginFile, targetFile);
                    resultPath = targetFile.toPath();
                }
            } else {
                FileUtils.copyDirectory(pluginFile, targetFile);
                resultPath = targetFile.toPath();
            }
        }
        return resultPath;
    }


    /**
     * ????????????????????????
     * @param pluginInsideInfo PluginInsideInfo
     * @throws Exception ????????????
     */
    protected void start(PluginInsideInfo pluginInsideInfo) throws Exception{
        Assert.isNotNull(pluginInsideInfo, "pluginInsideInfo ??????????????????");
        launcherChecker.checkCanStart(pluginInsideInfo);
        pluginInsideInfo.setPluginState(PluginState.STARTED);
        startFinish(pluginInsideInfo);
    }

    /**
     * ????????????????????????
     * @param pluginInsideInfo pluginInsideInfo
     */
    protected void startFinish(PluginInsideInfo pluginInsideInfo){
        String pluginId = pluginInsideInfo.getPluginId();
        startedPlugins.put(pluginId, pluginInsideInfo);
        resolvedPlugins.remove(pluginId);
    }


    /**
     * ????????????????????????
     * @param pluginInsideInfo PluginInsideInfo
     * @throws Exception ????????????
     */
    protected void stop(PluginInsideInfo pluginInsideInfo) throws Exception{
        launcherChecker.checkCanStop(pluginInsideInfo);
        pluginInsideInfo.setPluginState(PluginState.STOPPED);
        stopFinish(pluginInsideInfo);
    }

    /**
     * ??????????????????
     * @param pluginInsideInfo pluginInsideInfo
     */
    protected void stopFinish(PluginInsideInfo pluginInsideInfo){
        String pluginId = pluginInsideInfo.getPluginId();
        resolvedPlugins.put(pluginId, pluginInsideInfo);
        startedPlugins.remove(pluginId);
    }

    /**
     * ??????????????????????????????
     * @param pluginInfos ????????????????????????
     * @return ?????????????????????
     */
    protected List<PluginInfo> getSortPlugin(Map<String, PluginInfo> pluginInfos){
        if(ObjectUtils.isEmpty(pluginInfos)){
            return Collections.emptyList();
        }
        if (ObjectUtils.isEmpty(sortedPluginIds)) {
            return new ArrayList<>(pluginInfos.values());
        }
        List<PluginInfo> sortPluginInfos = new ArrayList<>();
        for (String sortedPluginId : sortedPluginIds) {
            PluginInfo pluginInfo = pluginInfos.get(sortedPluginId);
            if(pluginInfo != null){
                sortPluginInfos.add(pluginInfo);
                pluginInfos.remove(sortedPluginId);
            }
        }
        sortPluginInfos.addAll(pluginInfos.values());
        return sortPluginInfos;
    }


    protected PluginInsideInfo getPlugin(String pluginId){
        PluginInsideInfo wrapperInside = startedPlugins.get(pluginId);
        if(wrapperInside == null){
            wrapperInside = resolvedPlugins.get(pluginId);
        }
        return wrapperInside;
    }

    /**
     * ?????????????????????????????????????????????
     * @param dirFile ????????????
     * @param pluginFileName  ??????????????????
     */
    private void checkExistFile(File dirFile, String pluginFileName) {
        if(ResourceUtils.existFile(dirFile, pluginFileName)){
            // ???????????????????????????????????????
            throw getExistFileException(dirFile.getPath(), pluginFileName);
        }
    }

    private PluginException getExistFileException(String rootPath, String pluginFileName){
        return new PluginException("????????????[" + rootPath + "]??????????????????: " + pluginFileName);
    }

    /**
     * ????????????????????????
     * @param oldPlugin ???????????????
     * @param newPlugin ???????????????
     */
    protected void checkVersion(PluginDescriptor oldPlugin, PluginDescriptor newPlugin){
        int compareVersion = provider.getVersionInspector().compareTo(oldPlugin.getPluginVersion(),
                newPlugin.getPluginVersion());
        if(compareVersion >= 0){
            throw new PluginException("??????????????????[" + MsgUtils.getPluginUnique(newPlugin) + "]????????????" +
                    "???????????????[" + MsgUtils.getPluginUnique(oldPlugin) + "]");
        }
    }

    private List<String> resolvePath(List<String> path){
        if(ObjectUtils.isEmpty(path)){
            return Collections.emptyList();
        } else {
            File file = new File("");
            String absolutePath = file.getAbsolutePath();
            return path.stream()
                    .filter(p -> !ObjectUtils.isEmpty(p))
                    .map(p -> FilesUtils.resolveRelativePath(absolutePath, p))
                    .collect(Collectors.toList());
        }
    }

    /**
     * ???????????????????????????????????????
     */
    private void printOfNotFoundPlugins(){
        StringBuilder warn = new StringBuilder();
        warn.append("???????????????????????????: \n");
        if(pluginRootDirs.size() == 1){
            warn.append(pluginRootDirs.get(0)).append("\n");
        } else {
            for (int i = 0; i < pluginRootDirs.size(); i++) {
                warn.append(i + 1).append(". ").append(pluginRootDirs.get(i)).append("\n");
            }
        }
        warn.append("???????????????????????????.\n");
        warn.append("???????????????[plugin.runMode]????????????.\n");
        if(provider.getRuntimeMode() == RuntimeMode.DEV){
            warn.append("??????????????????????????????.\n");
        } else {
            warn.append("???????????????????????????.\n");
        }
        log.warn(warn.toString());
    }


}
