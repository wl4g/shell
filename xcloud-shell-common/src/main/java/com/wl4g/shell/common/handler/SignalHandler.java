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
package com.wl4g.shell.common.handler;

import com.wl4g.shell.common.exception.ChannelShellException;
import com.wl4g.shell.common.registry.ShellHandlerRegistrar;

import java.io.*;
import java.util.function.Function;

import static com.wl4g.components.common.lang.Assert2.*;

/**
 * Shell signal handler
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年5月2日
 * @since
 */
public abstract class SignalHandler implements Runnable, Closeable {

	/**
	 * Local shell component registry.
	 */
	final protected ShellHandlerRegistrar registrar;

	/**
	 * Callback function
	 */
	final protected Function<String, Object> function;

	public SignalHandler(ShellHandlerRegistrar registrar, Function<String, Object> function) {
		notNull(function, "Function is null, please check configure");
		notNull(registrar, "Registry must not be null");
		this.registrar = registrar;
		this.function = function;
	}

	/**
	 * Write and flush echo to client
	 * 
	 * @param message
	 * @throws IOException
	 */
	public abstract void writeFlush(Object message) throws ChannelShellException, IOException;

	/**
	 * Is connect active
	 * 
	 * @return
	 */
	public abstract boolean isActive();

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract String toString();

}