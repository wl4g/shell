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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wl4g.shell.core.config.ServerShellProperties;

/**
 * {@link MemoryShellCache}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * 
 */
@SuppressWarnings("unchecked")
public class MemoryShellCache extends AbstractRedisShellCache {

    private final Cache<Object, Object> localCache;

    public MemoryShellCache(ServerShellProperties config) {
        super(config);
        this.localCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMillis(config.getSharedLockTimeoutMs())).build();
    }

    @Override
    public <V> V hget(String key, Class<V> valueClass) {
        return (V) localCache.asMap().get(getOpsKey().concat(key));
    }

    @Override
    public <V> List<V> hgetAll(Class<V> valueClass) {
        return localCache.asMap().values().stream().map(e -> (V) e).collect(toList());
    }

    @Override
    public <V> boolean hset(String key, V value) {
        localCache.put(getOpsKey().concat(key), value);
        return true;
    }

    @Override
    public <V> boolean hsetnx(String key, V value) {
        return isNull(localCache.asMap().putIfAbsent(key, value));
    }

    @Override
    public <V> boolean hdel(String key) {
        return nonNull(localCache.asMap().remove(key));
    }

    @Override
    public <V> V get(String key, Class<V> valueClass) {
        return (V) localCache.asMap().get(key);
    }

    @Override
    public <V> boolean set(String key, V value, long expireMs) {
        return nonNull(localCache.asMap().put(key, value));
    }

    @Override
    public <V> boolean setnx(String key, V value, @Deprecated long expireMs) {
        return isNull(localCache.asMap().putIfAbsent(key, value));
    }

    @Override
    public <V> boolean del(String key) {
        localCache.invalidate(key);
        return true;
    }

    @Override
    public synchronized Object deleq(String key, String arg) {
        String value = get(key, String.class);
        if (StringUtils.equals(value, arg)) {
            return del(key) ? "OK" : null;
        }
        return null;
    }

}
