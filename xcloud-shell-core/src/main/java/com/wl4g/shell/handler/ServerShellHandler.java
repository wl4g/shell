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
package com.wl4g.shell.handler;

import static com.wl4g.components.common.lang.Assert2.hasTextOf;
import static com.wl4g.components.common.log.SmartLoggerFactory.getLogger;

import java.io.Closeable;

import org.slf4j.Logger;

import com.wl4g.shell.config.ShellProperties;
import com.wl4g.shell.handler.GenericShellHandler;
import com.wl4g.shell.handler.EmbeddedShellHandlerServer.ServerSignalChannelHandler;
import com.wl4g.shell.registry.ShellHandlerRegistrar;

/**
 * Server abstract shell component handler
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年4月14日
 * @since
 */
abstract class ServerShellHandler extends GenericShellHandler implements Closeable {
	final protected Logger log = getLogger(getClass());

	/**
	 * Accept socket client handlers.
	 */
	final private ThreadLocal<ServerSignalChannelHandler> clientContext = new InheritableThreadLocal<>();

	/**
	 * Spring application name.
	 */
	final private String appName;

	public ServerShellHandler(ShellProperties config, String appName, ShellHandlerRegistrar registrar) {
		super(config, registrar);
		hasTextOf(appName, "appName");
		this.appName = appName;
	}

	/**
	 * Gets {@link ShellProperties} configuration
	 * 
	 * @return
	 */
	protected ShellProperties getConfig() {
		return (ShellProperties) config;
	}

	protected String getAppName() {
		return appName;
	}

	/**
	 * Register current client handler.
	 * 
	 * @param client
	 * @return
	 */
	protected ServerSignalChannelHandler bind(ServerSignalChannelHandler client) {
		clientContext.set(client);
		return client;
	}

	/**
	 * Get current client handler
	 * 
	 * @return
	 */
	protected ServerSignalChannelHandler getClient() {
		return clientContext.get();
	}

	/**
	 * Cleanup current client handler.
	 * 
	 * @param client
	 * @return
	 */
	protected void cleanup() {
		clientContext.remove();
	}

}