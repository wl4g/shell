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

import static com.wl4g.infra.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.serialize.JacksonUtils.parseJSON;
import static com.wl4g.infra.common.serialize.JacksonUtils.toJSONString;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.wl4g.shell.core.config.ServerShellProperties;

/**
 * {@link RedisTemplateShellCache}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * 
 */
public class RedisTemplateShellCache extends AbstractRedisShellCache {

    protected final RedisTemplate<String, String> redisTemplate;

    /**
     * @param redisObj
     *            type of {@link RedisTemplate}
     */
    @SuppressWarnings("unchecked")
    public RedisTemplateShellCache(ServerShellProperties config, Object redisObj) {
        super(config);
        notNullOf(redisObj, "redisObj");
        if (redisObj instanceof RedisTemplate) {
            this.redisTemplate = (RedisTemplate<String, String>) redisObj;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public <V> V hget(String key, Class<V> valueClass) {
        Object ret = redisTemplate.opsForHash().get(getOpsKey(), key);
        return parseJSON(ret.toString(), valueClass);
    }

    @Override
    public <V> List<V> hgetAll(Class<V> valueClass) {
        Map<Object, Object> ret = redisTemplate.opsForHash().entries(getOpsKey());
        return safeMap(ret).values().stream().map(s -> parseJSON((String) s, valueClass)).collect(toList());
    }

    @Override
    public <V> boolean hset(String key, V value) {
        redisTemplate.opsForHash().put(getOpsKey(), key, value);
        return true;
    }

    @Override
    public <V> boolean hsetnx(String key, V value) {
        return redisTemplate.opsForHash().putIfAbsent(getOpsKey(), key, value);
    }

    @Override
    public boolean hdel(String sessionId) {
        redisTemplate.opsForHash().delete(getOpsKey(), sessionId);
        return true;
    }

    @Override
    public <V> V get(String key, Class<V> valueClass) {
        return parseJSON(redisTemplate.opsForValue().get(key), valueClass);
    }

    @Override
    public <V> boolean set(String key, V value, long expireMs) {
        redisTemplate.opsForValue().set(key, toJSONString(value), Duration.ofMillis(expireMs));
        return true;
    }

    @Override
    public <V> boolean setnx(String key, V value, long expireMs) {
        redisTemplate.opsForValue().setIfAbsent(key, toJSONString(value), Duration.ofMillis(expireMs));
        return true;
    }

    @Override
    public <V> boolean del(String key) {
        redisTemplate.delete(key);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deleq(String key, String arg) {
        return redisTemplate.execute(RedisScript.of(UNLOCK_LUA, Object.class), singletonList(key), arg);
    }

}
