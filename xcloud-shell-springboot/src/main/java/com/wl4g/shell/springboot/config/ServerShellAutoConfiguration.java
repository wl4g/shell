/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.shell.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.wl4g.shell.core.config.ServerShellProperties;
import com.wl4g.shell.springboot.EmbeddedShellServerStartup;

/**
 * Shell component services auto configuration
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年4月30日
 * @since
 */
public class ServerShellAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "spring.xcloud.shell")
    public ServerShellProperties serverShellProperties() {
        return new ServerShellProperties();
    }

    @Bean
    public AnnotationShellHandlerRegistrar annotationShellHandlerRegistrar() {
        return new AnnotationShellHandlerRegistrar();
    }

    @Bean
    public EmbeddedShellServerStartup embeddedShellServerStartup(ServerShellProperties config,
            AnnotationShellHandlerRegistrar registrar) {
        return new EmbeddedShellServerStartup(config, registrar);
    }

}