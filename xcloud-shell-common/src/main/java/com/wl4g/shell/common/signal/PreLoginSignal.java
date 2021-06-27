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

import static com.wl4g.component.common.lang.Assert2.hasTextOf;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import lombok.Getter;
import lombok.Setter;

/**
 * Pre login commands message
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年5月4日
 * @since
 */
@Getter
@Setter
public class PreLoginSignal extends Signal {
    private static final long serialVersionUID = -8574315246731906685L;

    private final String username;
    private final String password;

    public PreLoginSignal(String username, String password) {
        this.username = hasTextOf(username, "username");
        this.password = hasTextOf(password, "password");
    }

    @Override
    public String toString() {
        return "login [" + username + ":" + trimToEmpty(password).replaceAll(".", "\\*") + "]";
    }

}