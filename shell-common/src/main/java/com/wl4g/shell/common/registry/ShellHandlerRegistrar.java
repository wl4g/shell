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
package com.wl4g.shell.common.registry;

import static com.wl4g.infra.common.lang.Assert2.notNull;
import static com.wl4g.infra.common.lang.Assert2.state;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isInterface;
import static java.lang.reflect.Modifier.isNative;
import static java.lang.reflect.Modifier.isStatic;
import static java.lang.reflect.Modifier.isTransient;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.wl4g.shell.common.annotation.ShellMethod;

/**
 * Shell command handler registry
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2019-4月14日
 * @since v1.0
 */
public class ShellHandlerRegistrar implements Serializable {
    private static final long serialVersionUID = -8763772555086222131L;

    /**
     * Local registed shell targetMethodWrappers
     */
    private final Map<String, TargetMethodWrapper> registry = new ConcurrentHashMap<>(16);

    public Map<String, TargetMethodWrapper> getTargetMethods() {
        return registry;
    }

    public TargetMethodWrapper getTargetMethod(String argname) {
        return registry.get(argname);
    }

    public boolean contains(String argname) {
        return getTargetMethods().containsKey(argname);
    }

    /**
     * Registion shell component of bean
     * 
     * @param bean
     *            Target method object
     * @return This handler registrar object.
     */
    public ShellHandlerRegistrar register(Object bean) {
        notNull(bean, "bean is null, please check configure");

        for (Method m : bean.getClass().getDeclaredMethods()) {
            int mod = m.getModifiers();
            if (isStatic(mod) || isAbstract(mod) || isTransient(mod) || isAbstract(mod) || isNative(mod) || isInterface(mod)) {
                continue;
            }

            // Shell method?
            ShellMethod sm = m.getAnnotation(ShellMethod.class);
            if (sm != null) {
                notNull(sm.keys(), "Shell method key must not be null");
                for (String k : sm.keys()) {
                    register0(k, new TargetMethodWrapper(sm, m, bean));
                }
            }
        }

        return this;
    }

    /**
     * Internal registion shell bean stored
     * 
     * @param mainOpt
     *            main shell option
     * @param tm
     *            Target method wrapper
     */
    private void register0(String mainOpt, TargetMethodWrapper tm) {
        state(registry.putIfAbsent(mainOpt, tm) == null, String.format("Repeatedly defined shell method: '%s'", mainOpt));
    }

}