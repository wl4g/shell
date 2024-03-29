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
package com.wl4g.shell.springboot;

import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.shell.core.cache.ShellCache.Factory.JEDIS_CLASS;
import static com.wl4g.shell.core.cache.ShellCache.Factory.JEDIS_CLIENT_CLASS;
import static com.wl4g.shell.core.cache.ShellCache.Factory.JEDIS_CLUSTER_CLASS;
import static com.wl4g.shell.core.cache.ShellCache.Factory.REDIS_TEMPLATE_CLASS;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;

import com.wl4g.shell.core.EmbeddedShellServerBuilder;
import com.wl4g.shell.core.cache.MemoryShellCache;
import com.wl4g.shell.core.cache.ShellCache;
import com.wl4g.shell.core.config.ServerShellProperties;
import com.wl4g.shell.core.handler.EmbeddedShellServer;
import com.wl4g.shell.springboot.config.AnnotationShellHandlerRegistrar;

import lombok.CustomLog;

/**
 * {@link EmbeddedShellServerStartup}
 *
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2020-08-10
 * @since v1.0
 */
@CustomLog
public class EmbeddedShellServerStartup implements ApplicationRunner, DisposableBean {

    /** {@link ApplicationContext} */
    protected @Autowired ApplicationContext applicationContext;

    /** {@link ServerShellProperties} */
    protected final ServerShellProperties config;

    /** {@link AnnotationShellHandlerRegistrar} */
    protected final AnnotationShellHandlerRegistrar registrar;

    /** {@link EmbeddedShellServer} */
    protected EmbeddedShellServer shellServer;

    public EmbeddedShellServerStartup(ServerShellProperties config, AnnotationShellHandlerRegistrar registrar) {
        this.config = notNullOf(config, "config");
        this.registrar = notNullOf(registrar, "registrar");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Shell server init starting on [{}, {}] ...", config.getBeginPort(), config.getEndPort());

        // Create shell session DAO.
        // Redis clients in CLASSPATH. (if neccssary)
        ShellCache shellCache = new MemoryShellCache(config);
        if (nonNull(JEDIS_CLUSTER_CLASS) && nonNull(JEDIS_CLASS)) {
            Object jedisCluster = obtainNullableBean(JEDIS_CLUSTER_CLASS);
            Object jedis = obtainNullableBean(JEDIS_CLASS);
            Object jedisClient = obtainNullableBean(JEDIS_CLIENT_CLASS);
            Object redisTemplate = obtainNullableBean(REDIS_TEMPLATE_CLASS);
            if (nonNull(jedisCluster)) {
                shellCache = ShellCache.Factory.build(config, jedisCluster);
            } else if (nonNull(jedis)) {
                shellCache = ShellCache.Factory.build(config, jedis);
            } else if (nonNull(jedisClient)) {
                shellCache = ShellCache.Factory.build(config, jedisClient);
            } else if (nonNull(redisTemplate)) {
                shellCache = ShellCache.Factory.build(config, redisTemplate);
            }
        }
        log.info("Using shell cache: {}", shellCache);

        // Build shell server.
        this.shellServer = EmbeddedShellServerBuilder.newBuilder()
                .withAppName(applicationContext.getEnvironment().getRequiredProperty("spring.application.name"))
                .withConfiguration(config)
                .withRegistrar(registrar)
                .withShellCache(shellCache)
                .build();
        this.shellServer.start();
    }

    @Override
    public void destroy() throws Exception {
        shellServer.close();
    }

    private Object obtainNullableBean(Class<?> beanClazz) {
        if (isNull(beanClazz)) {
            return null;
        }
        try {
            return applicationContext.getBean(beanClazz);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

}
