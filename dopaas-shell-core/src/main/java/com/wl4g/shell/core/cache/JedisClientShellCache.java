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
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import java.util.List;

import com.wl4g.infra.support.cache.jedis.JedisClient;
import com.wl4g.shell.core.config.ServerShellProperties;

import redis.clients.jedis.params.SetParams;

/**
 * {@link JedisClientShellCache}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * 
 */
public class JedisClientShellCache extends AbstractRedisShellCache {

    protected final JedisClient jedisClient;

    /**
     * @param redisObj
     *            type of {@link JedisClient}
     */
    public JedisClientShellCache(ServerShellProperties config, Object redisObj) {
        super(config);
        notNullOf(redisObj, "redisObj");
        if (redisObj instanceof JedisClient) {
            this.jedisClient = (JedisClient) redisObj;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public <V> V hget(String key, Class<V> valueClass) {
        return parseJSON(jedisClient.hget(getOpsKey(), key), valueClass);
    }

    @Override
    public <V> List<V> hgetAll(Class<V> valueClass) {
        return safeMap(jedisClient.hgetAll(getOpsKey())).values().stream().map(s -> parseJSON(s, valueClass)).collect(toList());
    }

    @Override
    public <V> boolean hset(String key, V value) {
        Long ret = jedisClient.hset(getOpsKey(), key, toJSONString(value));
        return nonNull(ret) && ret > 0;
    }

    @Override
    public <V> boolean hsetnx(String key, V value) {
        Long ret = jedisClient.hsetnx(getOpsKey(), key, toJSONString(value));
        return nonNull(ret) && ret > 0;
    }

    @Override
    public boolean hdel(String key) {
        Long ret = jedisClient.hdel(getOpsKey(), key);
        return nonNull(ret) && ret > 0;
    }

    @Override
    public <V> V get(String key, Class<V> valueClass) {
        return parseJSON(jedisClient.get(key), valueClass);
    }

    @Override
    public <V> boolean set(String key, V value, long expireMs) {
        String ret = jedisClient.setex(key, expireMs, toJSONString(value));
        return nonNull(ret) && ("OK".equalsIgnoreCase(ret) || Integer.parseInt(ret) > 0);
    }

    @Override
    public <V> boolean setnx(String key, V value, long expireMs) {
        SetParams setParams = SetParams.setParams().nx().px(expireMs);
        String ret = jedisClient.set(key, toJSONString(value), setParams);
        return nonNull(ret) && ("OK".equalsIgnoreCase(ret) || Integer.parseInt(ret) > 0);
    }

    @Override
    public <V> boolean del(String key) {
        Long ret = jedisClient.del(key);
        return nonNull(ret) && ret > 0;
    }

    @Override
    public Object deleq(String key, String arg) {
        return jedisClient.eval(UNLOCK_LUA, singletonList(key), singletonList(arg));
    }

}
