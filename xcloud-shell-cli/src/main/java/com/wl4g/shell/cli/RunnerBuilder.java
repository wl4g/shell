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
package com.wl4g.shell.cli;

import java.lang.reflect.Constructor;
import java.net.URL;

import static com.wl4g.component.common.lang.Assert2.*;
import static org.apache.commons.lang3.StringUtils.*;

import com.wl4g.shell.cli.config.ClientShellConfiguration;
import com.wl4g.shell.cli.handler.ClientShellHandler;

/**
 * Runner builder
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2019-5月2日
 * @since v1.0
 */
public abstract class RunnerBuilder {

	private String conf;

	private Class<? extends ClientShellHandler> provider;

	private RunnerBuilder() {
	}

	public final static RunnerBuilder builder() {
		return new RunnerBuilder() {
		};
	}

	public RunnerBuilder config(String conf) {
		hasText(conf, "conf is empty, please check configure");
		this.conf = conf;
		return this;
	}

	public RunnerBuilder provider(Class<? extends ClientShellHandler> provider) {
		notNull(provider, "provider is null, please check configure");
		this.provider = provider;
		return this;
	}

	public ClientShellHandler build() {
		try {
			ClientShellConfiguration config = ClientShellConfiguration.create();
			if (isNotBlank(conf)) {
				config = ClientShellConfiguration.create(new URL("file://" + conf));
			}
			notNull(provider, "provider is null, please check configure");
			notNull(config, "config is null, please check configure");

			Constructor<? extends ClientShellHandler> constr = provider.getConstructor(ClientShellConfiguration.class);
			return constr.newInstance(new Object[] { config });
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}