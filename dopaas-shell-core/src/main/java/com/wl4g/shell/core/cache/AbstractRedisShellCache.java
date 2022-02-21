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
package com.wl4g.shell.core.cache;

import static com.wl4g.component.common.lang.Assert2.notNullOf;

import com.wl4g.shell.core.config.ServerShellProperties;

/**
 * {@link AbstractRedisShellCache}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * 
 */
public abstract class AbstractRedisShellCache implements ShellCache {
    protected static final String UNLOCK_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    protected final ServerShellProperties config;

    public AbstractRedisShellCache(ServerShellProperties config) {
        this.config = notNullOf(config, "config");
    }

    protected String getOpsKey() {
        return SESSION_KEY_PREFIX;
    }

    public static final String SESSION_KEY_PREFIX = "shell:cache:";

}
