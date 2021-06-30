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
package com.wl4g.shell.core.config;

import static com.google.common.base.Charsets.UTF_8;
import static com.wl4g.component.common.lang.Assert2.hasText;
import static com.wl4g.component.common.lang.Assert2.isTrue;
import static java.security.MessageDigest.isEqual;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.wl4g.shell.common.config.BaseShellProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Shell properties configuration
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年5月1日
 * @since
 */
public class ServerShellProperties extends BaseShellProperties {
    private static final long serialVersionUID = -24798955162679115L;

    /**
     * listening TCP backlog
     */
    private int backlog = 16;

    /**
     * Listening server socket bind address
     */
    private String bindAddr = "127.0.0.1";

    /**
     * Maximum number of concurrent client connections.
     */
    private int maxClients = 3;

    /**
     * Authetication configuration.
     */
    private AclInfo acl = new AclInfo();

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        isTrue(backlog > 0, String.format("backlog must greater than 0, actual is %s", backlog));
        this.backlog = backlog;
    }

    public String getBindAddr() {
        return bindAddr;
    }

    public InetAddress getInetBindAddr() {
        try {
            return InetAddress.getByName(getBindAddr());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBindAddr(String bindAddr) {
        hasText(bindAddr, "binAddr is emtpy, please check configure");
        this.bindAddr = bindAddr;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        isTrue(maxClients > 0, String.format("maxClients must greater than 0, actual is %s", backlog));
        this.maxClients = maxClients;
    }

    public AclInfo getAcl() {
        return acl;
    }

    public void setAcl(AclInfo auth) {
        this.acl = auth;
    }

    @Getter
    @Setter
    public static class AclInfo {
        private boolean enabled = false;
        private long timeoutMs = 5 * 60 * 1000L;
        private List<CredentialsInfo> info = new ArrayList<>();

        public final CredentialsInfo getCredentialsInfo(final String username) {
            return info.stream()
                    .filter(ai -> isEqual(trimToEmpty(ai.getUsername()).getBytes(UTF_8), trimToEmpty(username).getBytes(UTF_8)))
                    .findFirst().orElse(null);
        }

        public final boolean matchs(final String username, final String password) {
            for (CredentialsInfo ai : info) {
                if (isEqual(trimToEmpty(ai.getUsername()).getBytes(UTF_8), trimToEmpty(username).getBytes(UTF_8))
                        && isEqual(trimToEmpty(ai.getPassword()).getBytes(UTF_8), trimToEmpty(password).getBytes(UTF_8))) {
                    return true;
                }
            }
            return false;
        }

        @Getter
        @Setter
        public static class CredentialsInfo {
            private String username;
            private String password;
            private String[] permissions;
        }

    }

}