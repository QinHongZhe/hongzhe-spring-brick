/**
 * Copyright [2019-2022] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gitee.starblues.plugin.pack;

import com.gitee.starblues.common.PackageStructure;
import com.gitee.starblues.plugin.pack.dev.DevConfig;
import com.gitee.starblues.plugin.pack.dev.DevRepackager;
import com.gitee.starblues.plugin.pack.main.MainConfig;
import com.gitee.starblues.plugin.pack.main.MainRepackager;
import com.gitee.starblues.plugin.pack.prod.ProdConfig;
import com.gitee.starblues.plugin.pack.prod.ProdRepackager;
import com.gitee.starblues.plugin.pack.utils.CommonUtils;
import lombok.Getter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * @author starBlues
 * @version 3.0.0
 */
@Mojo(name = "repackage", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Getter
public class RepackageMojo extends AbstractPackagerMojo {


    @Parameter(property = "springboot-plugin.devConfig")
    private DevConfig devConfig;

    @Parameter(property = "springboot-plugin.prodConfig")
    private ProdConfig prodConfig;

    @Parameter(property = "springboot-plugin.mainConfig")
    private MainConfig mainConfig;

    @Override
    protected void pack() throws MojoExecutionException, MojoFailureException {
        String mode = getMode();
        if(Constant.MODE_PROD.equalsIgnoreCase(mode)){
            new ProdRepackager(this).repackage();
        } else if(Constant.MODE_DEV.equalsIgnoreCase(mode)){
            new DevRepackager(this).repackage();
        } else if(Constant.MODE_MAIN.equalsIgnoreCase(mode)){
            new MainRepackager(this).repackage();
        } else {
            throw new MojoExecutionException(mode  +" model not supported, mode support : "
                    + Constant.MODE_DEV + "/" + Constant.MODE_PROD);
        }
    }

}
