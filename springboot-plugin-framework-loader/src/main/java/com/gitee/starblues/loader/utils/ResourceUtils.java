package com.gitee.starblues.loader.utils;

import java.net.URL;

/**
 * 资源工具
 * @author starBlues
 * @version 3.0.0
 */
public class ResourceUtils {

    public static final String URL_PROTOCOL_FILE = "file";
    public static final String URL_PROTOCOL_JAR_FILE = "jar";
    public static final String JAR_FILE_EXTENSION = ".jar";

    public static final String URL_PROTOCOL_VFSFILE = "vfsfile";
    public static final String URL_PROTOCOL_VFS = "vfs";

    private ResourceUtils(){}

    /**
     * 是否为jar文件
     * @param url url
     * @return boolean
     */
    public static boolean isJarFileUrl(URL url) {
        String protocol = url.getProtocol();
        boolean extensionIsJar = url.getPath().toLowerCase().endsWith(JAR_FILE_EXTENSION);
        return (URL_PROTOCOL_FILE.equals(protocol) && extensionIsJar)
                || (URL_PROTOCOL_JAR_FILE.equals(protocol) || extensionIsJar);
    }

    /**
     * 是否为普通文件
     * @param url url
     * @return boolean
     */
    public static boolean isFileUrl(URL url) {
        String protocol = url.getProtocol();
        return (URL_PROTOCOL_FILE.equals(protocol) || URL_PROTOCOL_VFSFILE.equals(protocol) ||
                URL_PROTOCOL_VFS.equals(protocol));
    }
}
