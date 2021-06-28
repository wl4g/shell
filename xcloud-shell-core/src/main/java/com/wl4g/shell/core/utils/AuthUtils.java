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
package com.wl4g.shell.core.utils;

import static com.wl4g.component.common.collection.CollectionUtils2.isEmptyArray;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.UUID;

/**
 * {@link AuthUtils}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-28 v1.0.0
 * @see v1.0.0
 */
public abstract class AuthUtils {

    public static String generateSessionId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static boolean matchsRoles(final String[] shellMethodRoles, final String[] userRoles) {
        // Shell method roles is empty, means allowed anonymous access.
        if (!isEmptyArray(shellMethodRoles)) {
            for (String r1 : shellMethodRoles) {
                for (String r2 : userRoles) {
                    if (trimToEmpty(r1).equals(trimToEmpty(r2))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
