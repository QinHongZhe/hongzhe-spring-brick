package com.gitee.starblues.core.launcher.jar;

import java.util.zip.ZipEntry;

/**
 * @author starBlues
 * @version 1.0
 */
public interface FileHeader {

    /**
     * Returns {@code true} if the header has the given name.
     * @param name the name to test
     * @param suffix an additional suffix (or {@code 0})
     * @return {@code true} if the header has the given name
     */
    boolean hasName(CharSequence name, char suffix);

    /**
     * Return the offset of the load file header within the archive data.
     * @return the local header offset
     */
    long getLocalHeaderOffset();

    /**
     * Return the compressed size of the entry.
     * @return the compressed size.
     */
    long getCompressedSize();

    /**
     * Return the uncompressed size of the entry.
     * @return the uncompressed size.
     */
    long getSize();

    /**
     * Return the method used to compress the data.
     * @return the zip compression method
     * @see ZipEntry#STORED
     * @see ZipEntry#DEFLATED
     */
    int getMethod();


}
