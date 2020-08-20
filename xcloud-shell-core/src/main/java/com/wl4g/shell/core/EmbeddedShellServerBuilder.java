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
package com.wl4g.shell.core;

import static com.wl4g.components.common.lang.Assert2.hasTextOf;
import static com.wl4g.components.common.lang.Assert2.notNullOf;

import com.wl4g.shell.common.registry.ShellHandlerRegistrar;
import com.wl4g.shell.core.config.ShellProperties;
import com.wl4g.shell.core.handler.EmbeddedShellHandlerServer;

/**
 * Budiler of {@link EmbeddedShellHandlerServer}
 *
 * @author Wangl.sir <wanglsir@gmail.com, 983708408@qq.com>
 * @version v1.0 2020-08-10
 * @since
 */
public class EmbeddedShellServerBuilder {

	private final ShellProperties config;
	private final String appName;
	private final ShellHandlerRegistrar registrar;

	public EmbeddedShellServerBuilder(String appName) {
		this(appName, new ShellProperties(), new ShellHandlerRegistrar());
	}

	public EmbeddedShellServerBuilder(String appName, ShellHandlerRegistrar registrar) {
		this(appName, new ShellProperties(), registrar);
	}

	public EmbeddedShellServerBuilder(String appName, ShellProperties config, ShellHandlerRegistrar registrar) {
		hasTextOf(appName, "appName");
		notNullOf(config, "config");
		notNullOf(registrar, "registrar");
		this.appName = appName;
		this.registrar = registrar;
		this.config = config;
	}

	/**
	 * Registration shell component instance.
	 * 
	 * @param bean
	 * @return
	 */
	public EmbeddedShellServerBuilder register(Object bean) {
		this.registrar.register(bean);
		return this;
	}

	public EmbeddedShellHandlerServer build() {
		return new EmbeddedShellHandlerServer(config, appName, registrar);
	}

}
