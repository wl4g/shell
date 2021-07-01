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

import static com.wl4g.component.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.component.common.lang.Assert2.notNullOf;
import static com.wl4g.component.common.serialize.JacksonUtils.parseJSON;
import static com.wl4g.component.common.serialize.JacksonUtils.toJSONString;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import java.util.List;

import com.wl4g.shell.core.config.ServerShellProperties;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

/**
 * {@link NativeJedisShellCache}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * @see v1.0.0
 */
public class NativeJedisShellCache extends AbstractRedisShellCache {

    protected final JedisCluster jedisCluster;
    protected final Jedis jedis;

    /**
     * @param redisObj
     *            type of {@link JedisCluster} or {@link Jedis}
     */
    public NativeJedisShellCache(ServerShellProperties config, Object redisObj) {
        super(config);
        notNullOf(redisObj, "redisObj");
        if (redisObj instanceof JedisCluster) {
            this.jedisCluster = (JedisCluster) redisObj;
            notNullOf(jedisCluster, "jedisCluster");
            this.jedis = null;
        } else if (redisObj instanceof JedisCluster) {
            this.jedis = (Jedis) redisObj;
            this.jedisCluster = null;
            notNullOf(jedis, "jedis");
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public <V> V hget(String key, Class<V> valueClass) {
        if (nonNull(jedisCluster)) {
            return parseJSON(jedisCluster.hget(getOpsKey(), key), valueClass);
        }
        return parseJSON(jedis.hget(getOpsKey(), key), valueClass);
    }

    @Override
    public <V> List<V> hgetAll(Class<V> valueClass) {
        if (nonNull(jedisCluster)) {
            return safeMap(jedisCluster.hgetAll(getOpsKey())).values().stream().map(s -> parseJSON(s, valueClass))
                    .collect(toList());
        }
        return safeMap(jedis.hgetAll(getOpsKey())).values().stream().map(s -> parseJSON(s, valueClass)).collect(toList());
    }

    @Override
    public <V> boolean hset(String key, V value) {
        if (nonNull(jedisCluster)) {
            Long ret = jedisCluster.hset(getOpsKey(), key, toJSONString(value));
            return nonNull(ret) && ret > 0;
        }
        Long ret = jedis.hset(getOpsKey(), key, toJSONString(value));
        return nonNull(ret) && ret > 0;
    }

    @Override
    public <V> boolean hsetnx(String key, V value) {
        if (nonNull(jedisCluster)) {
            Long ret = jedisCluster.hsetnx(getOpsKey(), key, toJSONString(value));
            return nonNull(ret) && ret > 0;
        }
        Long ret = jedis.hsetnx(getOpsKey(), key, toJSONString(value));
        return nonNull(ret) && ret > 0;
    }

    @Override
    public boolean hdel(String key) {
        if (nonNull(jedisCluster)) {
            Long ret = jedisCluster.hdel(getOpsKey(), key);
            return nonNull(ret) && ret > 0;
        }
        Long ret = jedis.hdel(getOpsKey(), key);
        return nonNull(ret) && ret > 0;
    }

    @Override
    public <V> V get(String key, Class<V> valueClass) {
        if (nonNull(jedisCluster)) {
            return parseJSON(jedisCluster.get(key), valueClass);
        }
        return parseJSON(jedis.get(key), valueClass);
    }

    @Override
    public <V> boolean set(String key, V value, long expireMs) {
        String ret = null;
        if (nonNull(jedisCluster)) {
            ret = jedisCluster.setex(key, expireMs, toJSONString(value));
        } else {
            ret = jedis.setex(key, expireMs, toJSONString(value));
        }
        return nonNull(ret) && ("OK".equalsIgnoreCase(ret) || Integer.parseInt(ret) > 0);
    }

    @Override
    public <V> boolean setnx(String key, V value, long expireMs) {
        String ret = null;
        SetParams setParams = SetParams.setParams().nx().px(expireMs);
        if (nonNull(jedisCluster)) {
            ret = jedisCluster.set(key, toJSONString(value), setParams);
        } else {
            ret = jedis.set(key, toJSONString(value), setParams);
        }
        return nonNull(ret) && ("OK".equalsIgnoreCase(ret) || Integer.parseInt(ret) > 0);
    }

    @Override
    public <V> boolean del(String key) {
        Long ret = null;
        if (nonNull(jedisCluster)) {
            ret = jedisCluster.del(key);
        } else {
            ret = jedis.del(key);
        }
        return nonNull(ret) && ret > 0;
    }

    @Override
    public Object deleq(String key, String arg) {
        if (nonNull(jedisCluster)) {
            return jedisCluster.eval(UNLOCK_LUA, singletonList(key), singletonList(arg));
        }
        return jedis.eval(UNLOCK_LUA, singletonList(key), singletonList(arg));
    }

}
