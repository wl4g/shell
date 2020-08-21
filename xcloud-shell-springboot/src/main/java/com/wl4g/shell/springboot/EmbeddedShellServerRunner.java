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

import static com.wl4g.components.common.lang.Assert2.notNullOf;
import static com.wl4g.components.common.log.SmartLoggerFactory.getLogger;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;

import com.wl4g.components.common.log.SmartLogger;
import com.wl4g.shell.core.EmbeddedShellServerBuilder;
import com.wl4g.shell.core.config.ShellProperties;
import com.wl4g.shell.core.handler.EmbeddedShellServer;
import com.wl4g.shell.springboot.config.AnnotationShellHandlerRegistrar;

/**
 * {@link EmbeddedShellServerRunner}
 *
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2020-08-10
 * @since
 */
public class EmbeddedShellServerRunner implements ApplicationRunner, DisposableBean {

	protected final SmartLogger log = getLogger(getClass());

	/** {@link ShellProperties} */
	protected final ShellProperties config;

	/** {@link AnnotationShellHandlerRegistrar} */
	protected final AnnotationShellHandlerRegistrar registrar;

	/** {@link Environment} */
	@Autowired
	protected Environment environment;

	/** {@link EmbeddedShellServer} */
	protected EmbeddedShellServer shellServer;

	public EmbeddedShellServerRunner(ShellProperties config, AnnotationShellHandlerRegistrar registrar) {
		notNullOf(config, "config");
		notNullOf(registrar, "registrar");
		this.config = config;
		this.registrar = registrar;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Shell server init starting on {} ...", config.getBeginPort());

		this.shellServer = EmbeddedShellServerBuilder.newBuilder()
				.withAppName(environment.getRequiredProperty("spring.application.name"))
				.withConfiguration(config)
				.withRegistrar(registrar)
				.build();
		this.shellServer.start();
	}

	@Override
	public void destroy() throws Exception {
		this.shellServer.close();
	}

}
