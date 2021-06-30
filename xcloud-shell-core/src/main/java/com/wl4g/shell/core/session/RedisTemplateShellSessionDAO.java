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
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;

/**
 * {@link RedisTemplateShellSessionDAO}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * @see v1.0.0
 */
public class RedisTemplateShellSessionDAO extends AbstractRedisShellSessionDAO {

    protected final RedisTemplate<String, String> redisTemplate;

    /**
     * @param redisObj
     *            type of {@link RedisTemplate}
     */
    @SuppressWarnings("unchecked")
    public RedisTemplateShellSessionDAO(Object redisObj) {
        notNullOf(redisObj, "redisObj");
        if (redisObj instanceof RedisTemplate) {
            this.redisTemplate = (RedisTemplate<String, String>) redisObj;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public ShellSession get(String sessionId) {
        Object ret = redisTemplate.opsForHash().get(getOpsKey(), sessionId);
        return parseJSON(ret.toString(), ShellSession.class);
    }

    @Override
    public List<ShellSession> getAll() {
        Map<Object, Object> ret = redisTemplate.opsForHash().entries(getOpsKey());
        return safeMap(ret).values().stream().map(s -> parseJSON((String) s, ShellSession.class)).collect(toList());
    }

    @Override
    public boolean put(ShellSession session) {
        redisTemplate.opsForHash().put(getOpsKey(), session.getSessionId(), session);
        return true;
    }

    @Override
    public boolean putIfAbsent(ShellSession session) {
        return redisTemplate.opsForHash().putIfAbsent(getOpsKey(), session.getSessionId(), session);
    }

    @Override
    public boolean remove(String sessionId) {
        redisTemplate.opsForHash().delete(getOpsKey(), sessionId);
        return true;
    }

}
