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
package com.alipay.sofa.runtime.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

/**
 * Repackages existing JAR archives so that they can be executed from the command
 * line using {@literal java -jar}.
 *
 * @author qilong.zql
 * @since 0.1
 * @author ruoshan
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME, requiresDependencyCollection = ResolutionScope.RUNTIME)
public class RepackageMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Directory containing the generated archive
     * @since 0.1.0
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File         outputDirectory;

    /**
     * Name of the generated archive
     * @since 0.1.0
     */
    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String       finalName;

    @Override
    public void execute() throws MojoExecutionException {
        if ("war".equals(this.project.getPackaging())) {
            getLog().debug("repackage goal could not be applied to war project.");
            return;
        }
        if ("pom".equals(this.project.getPackaging())) {
            getLog().debug("repackage goal could not be applied to pom project.");
            return;
        }

        repackage();
    }

    private void repackage() throws MojoExecutionException {
        File source = this.project.getArtifact().getFile();
        File runtimeTarget = getRuntimeTargetFile();

        Repackager repackager = new Repackager(source);
        try {
            repackager.repackage(runtimeTarget,
                new ArtifactsLibraries(project.getArtifacts(), this.getLog()));
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }

        this.project.getArtifact().setFile(runtimeTarget);
    }

    private File getRuntimeTargetFile() {
        if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }
        return new File(this.outputDirectory, this.finalName
                                              + "."
                                              + this.project.getArtifact().getArtifactHandler()
                                                  .getExtension());
    }

}