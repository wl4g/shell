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
package com.wl4g.shell.core.session;

import static com.wl4g.component.common.lang.Assert2.notNullOf;

import java.util.List;

import com.wl4g.shell.core.cache.ShellCache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

/**
 * {@link CachingShellSessionDAO}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * 
 */
public class CachingShellSessionDAO implements ShellSessionDAO {

    protected final ShellCache shellCache;

    /**
     * @param redisObj
     *            type of {@link JedisCluster} or {@link Jedis}
     */
    public CachingShellSessionDAO(ShellCache shellCache) {
        this.shellCache = notNullOf(shellCache, "shellCache");
    }

    @Override
    public ShellSession get(String sessionId) {
        return shellCache.hget(sessionId, ShellSession.class);
    }

    @Override
    public List<ShellSession> getAll() {
        return shellCache.hgetAll(ShellSession.class);
    }

    @Override
    public boolean put(ShellSession session) {
        return shellCache.hset(session.getSessionId(), session);
    }

    @Override
    public boolean putIfAbsent(ShellSession session) {
        return shellCache.hset(session.getSessionId(), session);
    }

    @Override
    public boolean remove(String sessionId) {
        return shellCache.hdel(sessionId);
    }

}
