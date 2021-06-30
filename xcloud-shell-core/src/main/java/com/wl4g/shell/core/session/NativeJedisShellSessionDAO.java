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

import static com.wl4g.component.common.collection.CollectionUtils2.safeMap;
import static com.wl4g.component.common.lang.Assert2.notNullOf;
import static com.wl4g.component.common.serialize.JacksonUtils.parseJSON;
import static com.wl4g.component.common.serialize.JacksonUtils.toJSONString;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

/**
 * {@link NativeJedisShellSessionDAO}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * @see v1.0.0
 */
public class NativeJedisShellSessionDAO extends AbstractRedisShellSessionDAO {

    protected final JedisCluster jedisCluster;
    protected final Jedis jedis;

    /**
     * @param redisObj
     *            type of {@link JedisCluster} or {@link Jedis}
     */
    public NativeJedisShellSessionDAO(Object redisObj) {
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
    public ShellSession get(String sessionId) {
        if (nonNull(jedisCluster)) {
            return parseJSON(jedisCluster.hget("", sessionId), ShellSession.class);
        }
        return parseJSON(jedis.hget("", sessionId), ShellSession.class);
    }

    @Override
    public List<ShellSession> getAll() {
        if (nonNull(jedisCluster)) {
            return safeMap(jedisCluster.hgetAll(getOpsKey())).values().stream().map(s -> parseJSON(s, ShellSession.class))
                    .collect(toList());
        }
        return safeMap(jedis.hgetAll("")).values().stream().map(s -> parseJSON(s, ShellSession.class)).collect(toList());
    }

    @Override
    public boolean put(ShellSession session) {
        if (nonNull(jedisCluster)) {
            Long ret = jedisCluster.hset(getOpsKey(), session.getSessionId(), toJSONString(session));
            return nonNull(ret) && ret > 0;
        }
        Long ret = jedis.hset(getOpsKey(), session.getSessionId(), toJSONString(session));
        return nonNull(ret) && ret > 0;
    }

    @Override
    public boolean putIfAbsent(ShellSession session) {
        if (nonNull(jedisCluster)) {
            Long ret = jedisCluster.hsetnx(getOpsKey(), session.getSessionId(), toJSONString(session));
            return nonNull(ret) && ret > 0;
        }
        Long ret = jedis.hsetnx(getOpsKey(), session.getSessionId(), toJSONString(session));
        return nonNull(ret) && ret > 0;
    }

    @Override
    public boolean remove(String sessionId) {
        if (nonNull(jedisCluster)) {
            Long ret = jedisCluster.hdel(getOpsKey(), sessionId);
            return nonNull(ret) && ret > 0;
        }
        Long ret = jedis.hdel(getOpsKey(), sessionId);
        return nonNull(ret) && ret > 0;
    }

}
