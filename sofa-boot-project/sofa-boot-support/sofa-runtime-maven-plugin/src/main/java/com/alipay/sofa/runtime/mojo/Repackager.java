/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.serverless.runtime.mojo;

import com.alipay.sofa.ark.spi.constant.Constants;
import com.alipay.sofa.ark.tools.JarWriter;
import com.alipay.sofa.ark.tools.Layout;
import com.alipay.sofa.ark.tools.Layouts;
import com.alipay.sofa.ark.tools.Libraries;
import com.alipay.sofa.ark.tools.Library;
import com.alipay.sofa.ark.tools.LibraryScope;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * Utility class that can be used to repackage an archive so that it can be executed using
 * {@literal java -jar}
 *
 * @author qilong.zql
 * @author ruoshan
 * @since 0.1.0
 */
public class Repackager {

    private static final byte[] ZIP_FILE_HEADER     = new byte[] { 'P', 'K', 3, 4 };

    private File                serverlessRuntimeJar;

    private Library             arkContainerLibrary = null;

    private final List<Library> arkPluginLibraries  = new ArrayList<>();

    public Repackager(File source) {
        if (source == null) {
            throw new IllegalArgumentException("Source file must be provided");
        }
        if (!source.exists() || !source.isFile()) {
            throw new IllegalArgumentException("Source must refer to an existing file, " + "got"
                                               + source.getAbsolutePath());
        }
    }

    /**
     * Repackage to the given destination so that it can be launched using '
     * {@literal java -jar}'.
     *
     * @param appDestination the executable fat jar's destination
     * @param libraries the libraries required to run the archive
     * @throws IOException if the file cannot be repackaged
     */
    public void repackage(File appDestination, Libraries libraries)
            throws IOException {

        if (libraries == null) {
            throw new IllegalArgumentException("Libraries must not be null");
        }
        serverlessRuntimeJar = appDestination;

        libraries.doWithLibraries((library) -> {
            if (LibraryScope.PROVIDED.equals(library.getScope())) {
                return;
            }

            if (!isZip(library.getFile())) {
                return;
            }

            try (JarFile jarFile = new JarFile(library.getFile())) {
                if (isArkContainer(jarFile)) {
                    if (arkContainerLibrary != null) {
                        throw new RuntimeException("duplicate SOFAArk Container dependency");
                    }
                    library.setScope(LibraryScope.CONTAINER);
                    arkContainerLibrary = library;
                } else if (isArkPlugin(jarFile)) {
                    library.setScope(LibraryScope.PLUGIN);
                    arkPluginLibraries.add(library);
                }
            }
        });

        repackageJar();
    }

    private void repackageJar() throws IOException {
        File destination = serverlessRuntimeJar.getAbsoluteFile();
        destination.delete();

        JarFile jarFileSource = new JarFile(arkContainerLibrary.getFile().getAbsoluteFile());
        JarWriter writer = new JarWriter(destination);

        try {
            writer.writeBootstrapEntry(jarFileSource);
            writeNestedLibraries(Collections.singletonList(arkContainerLibrary), Layouts.Jar.jar(),
                writer);
            writeNestedLibraries(arkPluginLibraries, Layouts.Jar.jar(), writer);
        } finally {
            jarFileSource.close();
            try {
                writer.close();
            } catch (Exception ex) {
                // Ignore
            }
        }
    }

    private void writeNestedLibraries(List<Library> libraries, Layout layout, JarWriter writer)
                                                                                               throws IOException {
        Set<String> alreadySeen = new HashSet<>();
        for (Library library : libraries) {
            String destination = layout
                .getLibraryDestination(library.getName(), library.getScope());
            if (destination != null) {
                if (!alreadySeen.add(destination + library.getName())) {
                    throw new IllegalStateException("Duplicate library " + library.getName());
                }
                writer.writeNestedLibrary(destination, library);
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isZip(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                return isZip(fileInputStream);
            } finally {
                fileInputStream.close();
            }
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isArkContainer(JarFile jarFile) {
        return jarFile.getEntry(Constants.ARK_CONTAINER_MARK_ENTRY) != null;
    }

    private boolean isArkPlugin(JarFile jarFile) {
        return jarFile.getEntry(Constants.ARK_PLUGIN_MARK_ENTRY) != null;
    }

    public static boolean isZip(InputStream inputStream) throws IOException {
        for (int i = 0; i < ZIP_FILE_HEADER.length; i++) {
            if (inputStream.read() != ZIP_FILE_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

}