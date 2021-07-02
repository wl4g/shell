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

import static com.wl4g.component.common.lang.Assert2.hasTextOf;
import static com.wl4g.component.common.lang.Assert2.notNullOf;
import static java.util.Objects.nonNull;

import com.wl4g.shell.common.registry.ShellHandlerRegistrar;
import com.wl4g.shell.core.cache.MemoryShellCache;
import com.wl4g.shell.core.cache.ShellCache;
import com.wl4g.shell.core.config.ServerShellProperties;
import com.wl4g.shell.core.handler.EmbeddedShellServer;

/**
 * Budiler of {@link EmbeddedShellServer}
 *
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2020-08-10
 * @since v1.0
 */
public class EmbeddedShellServerBuilder {

    /** Shell application name */
    private String appName = "defaultShellApplication";

    /** {@link ServerShellProperties} */
    private ServerShellProperties config = new ServerShellProperties();

    /** {@link ShellCache} */
    private ShellCache shellCache = new MemoryShellCache(config);

    /** {@link ShellHandlerRegistrar} */
    private ShellHandlerRegistrar registrar = new ShellHandlerRegistrar();

    private EmbeddedShellServerBuilder() {
    }

    /**
     * New instantial of {@link EmbeddedShellServer}
     * 
     * @return
     */
    public static EmbeddedShellServerBuilder newBuilder() {
        return new EmbeddedShellServerBuilder();
    }

    /**
     * Sets shell application name.
     * 
     * @param appName
     * @return
     */
    public EmbeddedShellServerBuilder withAppName(String appName) {
        this.appName = hasTextOf(appName, "appName");
        return this;
    }

    /**
     * Sets shell configuration of {@link ServerShellProperties}.
     * 
     * @param appName
     * @return
     */
    public EmbeddedShellServerBuilder withConfiguration(ServerShellProperties config) {
        this.config = notNullOf(config, "config");
        return this;
    }

    /**
     * Sets shell cache of {@link ShellCache}.
     * 
     * @param appName
     * @return
     */
    public EmbeddedShellServerBuilder withShellCache(ShellCache shellCache) {
        this.shellCache = notNullOf(shellCache, "shellCache");
        return this;
    }

    /**
     * Sets shell handler registry of {@link ShellHandlerRegistrar}.
     * 
     * @param appName
     * @return
     */
    public EmbeddedShellServerBuilder withRegistrar(ShellHandlerRegistrar registrar) {
        this.registrar = notNullOf(registrar, "registrar");
        return this;
    }

    /**
     * Registration shell component instance.
     * 
     * @param shellComponents
     * @return
     */
    public EmbeddedShellServerBuilder register(Object... shellComponents) {
        if (nonNull(shellComponents)) {
            for (Object c : shellComponents) {
                registrar.register(c);
            }
        }
        return this;
    }

    public EmbeddedShellServer build() {
        return new EmbeddedShellServer(config, appName, registrar, shellCache);
    }

}
