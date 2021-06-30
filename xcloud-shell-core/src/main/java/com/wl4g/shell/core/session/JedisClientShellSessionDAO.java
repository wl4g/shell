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

import com.wl4g.component.support.cache.jedis.JedisClient;

/**
 * {@link JedisClientShellSessionDAO}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * @see v1.0.0
 */
public class JedisClientShellSessionDAO extends AbstractRedisShellSessionDAO {

    protected final JedisClient jedisClient;

    /**
     * @param redisObj
     *            type of {@link JedisClient}
     */
    public JedisClientShellSessionDAO(Object redisObj) {
        notNullOf(redisObj, "redisObj");
        if (redisObj instanceof JedisClient) {
            this.jedisClient = (JedisClient) redisObj;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public ShellSession get(String sessionId) {
        return parseJSON(jedisClient.hget(getOpsKey(), sessionId), ShellSession.class);
    }

    @Override
    public List<ShellSession> getAll() {
        return safeMap(jedisClient.hgetAll(getOpsKey())).values().stream().map(s -> parseJSON(s, ShellSession.class))
                .collect(toList());
    }

    @Override
    public boolean put(ShellSession session) {
        Long ret = jedisClient.hset(getOpsKey(), session.getSessionId(), toJSONString(session));
        return nonNull(ret) && ret > 0;
    }

    @Override
    public boolean putIfAbsent(ShellSession session) {
        Long ret = jedisClient.hsetnx(getOpsKey(), session.getSessionId(), toJSONString(session));
        return nonNull(ret) && ret > 0;
    }

    @Override
    public boolean remove(String sessionId) {
        Long ret = jedisClient.hdel(getOpsKey(), sessionId);
        return nonNull(ret) && ret > 0;
    }

}
