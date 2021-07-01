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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link MemoryShellCache}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * @see v1.0.0
 */
@SuppressWarnings("unchecked")
public class MemoryShellCache implements ShellCache {

    private static final Map<String, Object> localStore = new ConcurrentHashMap<>(16);

    @Override
    public <V> V hget(String key, Class<V> valueClass) {
        return (V) localStore.get(key);
    }

    @Override
    public <V> List<V> hgetAll(Class<V> valueClass) {
        return (List<V>) localStore.values().stream().collect(toList());
    }

    @Override
    public <V> boolean hset(String key, V value) {
        return nonNull(localStore.put(key, value));
    }

    @Override
    public <V> boolean hsetnx(String key, V value) {
        return isNull(localStore.putIfAbsent(key, value));
    }

    @Override
    public <V> boolean hdel(String key) {
        return nonNull(localStore.remove(key));
    }

}
