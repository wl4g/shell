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
package com.wl4g.shell.common.signal;

import java.io.Serializable;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

/**
 * Shell transport signal
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年5月4日
 * @since
 */
@Getter
@Setter
public abstract class Signal implements Serializable {
    private static final long serialVersionUID = 771621623117425564L;

    private @Nullable String sessionId;

    @Override
    public String toString() {
        return getClass().getSimpleName().concat(" - ");
    }

}