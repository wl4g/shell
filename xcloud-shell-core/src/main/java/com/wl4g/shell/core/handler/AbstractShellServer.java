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
package com.wl4g.shell.core.handler;

import static com.wl4g.component.common.lang.Assert2.hasTextOf;
import static com.wl4g.component.common.lang.Assert2.state;
import static com.wl4g.component.common.log.SmartLoggerFactory.getLogger;
import static java.lang.String.format;

import java.io.Closeable;
import java.util.List;

import org.slf4j.Logger;

import com.wl4g.shell.common.handler.GenericShellHandler;
import com.wl4g.shell.common.registry.ShellHandlerRegistrar;
import com.wl4g.shell.common.registry.TargetMethodWrapper;
import com.wl4g.shell.core.config.ServerShellProperties;
import com.wl4g.shell.core.handler.EmbeddedShellServer.ServerSignalHandler;

/**
 * Server abstract shell component handler
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年4月14日
 * @since
 */
public abstract class AbstractShellServer extends GenericShellHandler implements Closeable {
    protected final Logger log = getLogger(getClass());

    /**
     * Accept socket client handlers.
     */
    private final ThreadLocal<ServerSignalHandler> clientStore = new InheritableThreadLocal<>();

    /**
     * Spring application name.
     */
    private final String appName;

    public AbstractShellServer(ServerShellProperties config, String appName, ShellHandlerRegistrar registrar) {
        super(config, registrar);
        hasTextOf(appName, "appName");
        this.appName = appName;
    }

    /**
     * Gets {@link ServerShellProperties} configuration
     * 
     * @return
     */
    protected ServerShellProperties getConfig() {
        return (ServerShellProperties) config;
    }

    protected String getAppName() {
        return appName;
    }

    /**
     * Register current client handler.
     * 
     * @param client
     * @return
     */
    protected ServerSignalHandler bind(ServerSignalHandler client) {
        clientStore.set(client);
        return client;
    }

    /**
     * Gets current client handler
     * 
     * @return
     */
    protected ServerSignalHandler getClient() {
        return clientStore.get();
    }

    /**
     * Cleanup current client handler.
     * 
     * @param client
     * @return
     */
    protected void cleanup() {
        clientStore.remove();
    }

    @Override
    protected void preHandleInput(List<String> commands, TargetMethodWrapper tm, List<Object> args) {
        // Get current context
        BaseShellContext context = getClient().getContext();

        // Bind target method
        context.setTarget(tm);

        // Resolving args with {@link AbstractShellContext}
        BaseShellContext updatedCtx = resolveInjectArgsForShellContextIfNecceary(context, tm, args);

        // Inject update actual context
        getClient().setContext(updatedCtx);
    }

    /**
     * If necessary, resolving whether the shell method parameters have
     * {@link BaseShellContext} instances and inject.
     * 
     * @param context
     * @param tm
     * @param args
     */
    protected BaseShellContext resolveInjectArgsForShellContextIfNecceary(BaseShellContext context, TargetMethodWrapper tm,
            List<Object> args) {

        // Find parameter: ShellContext index and class
        Object[] ret = findParameterForShellContext(tm);
        int index = (int) ret[0];
        Class<?> contextClass = (Class<?>) ret[1];

        if (index >= 0) { // have ShellContext?
            // Convert to specific shellContext
            if (SimpleShellContext.class.isAssignableFrom(contextClass)) {
                context = new SimpleShellContext(context);
            } else if (ProgressShellContext.class.isAssignableFrom(contextClass)) {
                context = new ProgressShellContext(context);
            }
            if (index < args.size()) { // Correct parameter index
                args.add(index, context);
            } else {
                args.add(context);
            }

            /**
             * When injection {@link ShellContext} is used, the auto open
             * channel status is wait.
             */
            context.begin(); // MARK2
        }

        return context;
    }

    /**
     * Gets {@link ShellContext} index by parameters classes.
     * 
     * @param tm
     * @param clazz
     * @return
     */
    private Object[] findParameterForShellContext(TargetMethodWrapper tm) {
        int index = -1, i = 0;
        Class<?> contextCls = null;
        for (Class<?> cls : tm.getMethod().getParameterTypes()) {
            if (ShellContext.class.isAssignableFrom(cls)) {
                state(index < 0, format("Multiple shellcontext type parameters are unsupported. %s", tm.getMethod()));
                index = i;
                contextCls = cls;
            }
            ++i;
        }
        return new Object[] { index, contextCls };
    }

}