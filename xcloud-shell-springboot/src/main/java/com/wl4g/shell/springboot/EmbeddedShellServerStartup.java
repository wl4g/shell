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

import static com.wl4g.component.common.lang.Assert2.notNullOf;
import static com.wl4g.component.common.lang.ClassUtils2.resolveClassNameNullable;
import static com.wl4g.component.common.log.SmartLoggerFactory.getLogger;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;

import com.wl4g.component.common.log.SmartLogger;
import com.wl4g.shell.core.EmbeddedShellServerBuilder;
import com.wl4g.shell.core.config.ServerShellProperties;
import com.wl4g.shell.core.handler.EmbeddedShellServer;
import com.wl4g.shell.core.session.JedisClientShellSessionDAO;
import com.wl4g.shell.core.session.MemoryShellSessionDAO;
import com.wl4g.shell.core.session.ShellSessionDAO;
import com.wl4g.shell.springboot.config.AnnotationShellHandlerRegistrar;

/**
 * {@link EmbeddedShellServerStartup}
 *
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2020-08-10
 * @since
 */
public class EmbeddedShellServerStartup implements ApplicationRunner, DisposableBean {
    protected final SmartLogger log = getLogger(getClass());

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
        ShellSessionDAO sessionDAO = new MemoryShellSessionDAO();
        // Jedis in classpath.(if neccssary)
        if (nonNull(JEDIS_CLUSTER_CLASS) && nonNull(JEDIS_CLASS)) {
            Object jedisCluster = obtainNullableBean(JEDIS_CLUSTER_CLASS);
            Object jedis = obtainNullableBean(JEDIS_CLASS);
            Object jedisClient = obtainNullableBean(JEDIS_CLIENT_CLASS);
            Object redisTemplate = obtainNullableBean(REDIS_TEMPLATE_CLASS);
            if (nonNull(jedisCluster)) {
                sessionDAO = new JedisClientShellSessionDAO(jedisCluster);
            } else if (nonNull(jedis)) {
                sessionDAO = new JedisClientShellSessionDAO(jedis);
            } else if (nonNull(jedisClient)) {
                sessionDAO = new JedisClientShellSessionDAO(jedisClient);
            } else if (nonNull(redisTemplate)) {
                sessionDAO = new JedisClientShellSessionDAO(redisTemplate);
            }
        }
        log.info("Using shell session DAO: {}", sessionDAO);

        // Build shell server.
        shellServer = EmbeddedShellServerBuilder.newBuilder()
                .withAppName(applicationContext.getEnvironment().getRequiredProperty("spring.application.name"))
                .withConfiguration(config).withRegistrar(registrar).withShellSessionDAO(sessionDAO).build();
        shellServer.start();
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

    private static final Class<?> JEDIS_CLUSTER_CLASS = resolveClassNameNullable("redis.clients.jedis.JedisCluster");
    private static final Class<?> JEDIS_CLASS = resolveClassNameNullable("redis.clients.jedis.Jedis");
    private static final Class<?> JEDIS_CLIENT_CLASS = resolveClassNameNullable(
            "com.wl4g.component.support.cache.jedis.JedisClient");
    private static final Class<?> REDIS_TEMPLATE_CLASS = resolveClassNameNullable(
            "org.springframework.data.redis.core.RedisTemplate");

}
