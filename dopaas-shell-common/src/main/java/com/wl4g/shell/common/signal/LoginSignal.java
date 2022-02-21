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

/**
 * Login commands message
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2019-5月4日
 * @since v1.0
 */
public class LoginSignal extends Signal {
    private static final long serialVersionUID = -8574315246731906685L;

    private boolean authenticated;
    private String desc;

    public LoginSignal() {
    }

    public LoginSignal(boolean authenticated) {
        setAuthenticated(authenticated);
    }

    public LoginSignal(boolean authenticated, String sessionId) {
        setAuthenticated(authenticated);
        setSessionId(sessionId);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public LoginSignal withAuthenticated(boolean authenticated) {
        setAuthenticated(authenticated);
        return this;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public LoginSignal withDesc(String desc) {
        setDesc(desc);
        return this;
    }

}