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

import static com.wl4g.component.common.lang.Assert2.isInstanceOf;
import static com.wl4g.component.common.lang.Assert2.notNull;
import static com.wl4g.component.common.lang.Assert2.notNullOf;
import static com.wl4g.component.common.lang.Assert2.state;
import static com.wl4g.shell.common.signal.ChannelState.RUNNING;
import static com.wl4g.shell.core.utils.AuthUtils.genSessionID;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.wl4g.shell.common.exception.InternalShellException;
import com.wl4g.shell.common.exception.UnauthenticationShellException;
import com.wl4g.shell.common.exception.UnauthorizedShellException;
import com.wl4g.shell.common.handler.BaseSignalHandler;
import com.wl4g.shell.common.registry.ShellHandlerRegistrar;
import com.wl4g.shell.common.registry.TargetMethodWrapper;
import com.wl4g.shell.common.signal.AckInterruptSignal;
import com.wl4g.shell.common.signal.AskInterruptSignal;
import com.wl4g.shell.common.signal.LoginSignal;
import com.wl4g.shell.common.signal.MetaSignal;
import com.wl4g.shell.common.signal.PreInterruptSignal;
import com.wl4g.shell.common.signal.PreLoginSignal;
import com.wl4g.shell.common.signal.Signal;
import com.wl4g.shell.common.signal.StdinSignal;
import com.wl4g.shell.core.config.ServerShellProperties;
import com.wl4g.shell.core.config.ServerShellProperties.AclInfo;
import com.wl4g.shell.core.config.ServerShellProperties.AclInfo.CredentialsInfo;
import com.wl4g.shell.core.utils.AuthUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Embedded shell handle server
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年5月1日
 * @since
 */
public class EmbeddedShellServer extends AbstractShellServer implements Runnable {

    /**
     * Current server shellRunning status.
     */
    protected final AtomicBoolean running = new AtomicBoolean(false);

    /** Shell signal handler workers. */
    protected final Map<ServerSignalHandler, Thread> workers;

    /** Current shell channel standard input. */
    protected final ThreadLocal<StdinCommandWrapper> currentStdin = new ThreadLocal<>();

    /**
     * Server sockets
     */
    protected ServerSocket ss;

    /**
     * Boss thread
     */
    protected Thread boss;

    public EmbeddedShellServer(ServerShellProperties config, String appName, ShellHandlerRegistrar registrar) {
        super(config, appName, registrar);
        this.workers = new ConcurrentHashMap<>(config.getMaxClients());
    }

    /**
     * Start server shell handler instance
     * 
     * @throws Exception
     */
    public void start() throws Exception {
        if (running.compareAndSet(false, true)) {
            state(isNull(ss), "server socket already listen ?");

            // Determine server port.
            int bindPort = ensureDetermineServPort(getAppName());

            ss = new ServerSocket(bindPort, getConfig().getBacklog(), getConfig().getInetBindAddr());
            ss.setSoTimeout(0); // Infinite timeout
            log.info("Shell Console started on port(s): {}", bindPort);

            boss = new Thread(this, getClass().getSimpleName() + "-boss");
            boss.setDaemon(true);
            boss.start();
        }
    }

    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            try {
                boss.interrupt();
            } catch (Exception e) {
                log.error("Interrupting boss failure", e);
            }

            if (nonNull(ss) && !ss.isClosed()) {
                try {
                    ss.close();
                } catch (IOException e) {
                    log.error("Closing server failure", e);
                }
            }

            Iterator<ServerSignalHandler> it = workers.keySet().iterator();
            while (it.hasNext()) {
                try {
                    ServerSignalHandler h = it.next();
                    Thread t = workers.get(h);
                    t.interrupt();
                    t = null;
                    it.remove();
                } catch (Exception e) {
                    log.error("Closing worker failure", e);
                }
            }
        }
    }

    @Override
    protected void preHandleCommand(List<String> commands, TargetMethodWrapper tm) {
        assertShellAclPermission(tm);
        super.preHandleCommand(commands, tm);
    }

    /**
     * Assertion shell channel ACL permission by based on roles.
     * 
     * @param tm
     */
    private void assertShellAclPermission(TargetMethodWrapper tm) {
        // Skip no enabled.
        if (!getConfig().getAcl().isEnabled()) {
            log.debug("No enabled acl of shell target method: {}", tm);
            return;
        }

        // Check shell channel authentication.
        StdinCommandWrapper stdin = currentStdin.get();
        AuthenticationInfo authInfo = stdin.getHandler().getAuthenticationInfo();
        if (!authInfo.isAuthenticated()) {
            throw new UnauthenticationShellException("This command methods must be authenticated to execute.");
        }

        // Check ACL by roles.
        String[] permissions = tm.getShellMethod().permissions();
        CredentialsInfo credentials = getConfig().getAcl().getCredentialsInfo(authInfo.getUsername());
        if (!AuthUtils.matchAclPermits(permissions, credentials.getPermissions())) {
            throw new UnauthorizedShellException("Access permission not defined.");
        }
    }

    /**
     * Accepting connect processing
     */
    @Override
    public void run() {
        while (running.get() && !boss.isInterrupted() && !ss.isClosed()) {
            try {
                // Receiving client socket(blocking)
                Socket s = ss.accept();
                log.debug("On accept socket: {}, maximum: {}, actual: {}", s, getConfig().getMaxClients(), workers.size());

                // Check many connections.
                if (workers.size() >= getConfig().getMaxClients()) {
                    log.warn(format("There are too many parallel shell connections. maximum: %s, actual: %s",
                            getConfig().getMaxClients(), workers.size()));
                    s.close();
                    continue;
                }

                // Create signal handler
                ServerSignalHandler signalHandler = new ServerSignalHandler(registrar, s, line -> process(line));

                // MARK1: The worker thread may not be the parent thread of
                // Runnable, so you need to display bind to the thread in the
                // afternoon gain.
                String taskId = getClass().getSimpleName().concat("-channel-") + workers.size();
                Thread task = new Thread(() -> bind(signalHandler).run(), taskId);
                task.setDaemon(true);
                workers.put(signalHandler, task);
                task.start();

            } catch (Throwable e) {
                // e.g. Socket is closed
                if ((!running.get() || boss.isInterrupted() || ss.isClosed()) && (e instanceof SocketException)
                        && containsIgnoreCase(e.getMessage(), "closed")) {
                    log.warn("Shutdown shell server receiver.");
                } else {
                    log.warn("Shell server receiving failure. {}", getStackTrace(e));
                }
            }
        }
    }

    /**
     * Server shell signal channel handler
     * 
     * @author Wangl.sir <983708408@qq.com>
     * @version v1.0 2019年5月2日
     * @since
     */
    class ServerSignalHandler extends BaseSignalHandler {

        /** Running process command worker */
        private final ExecutorService processWorker;

        /** Running current command {@link ShellContext} */
        private BaseShellContext currentContext;

        /** Authentication info. */
        private AuthenticationInfo authInfo = new AuthenticationInfo();

        public ServerSignalHandler(ShellHandlerRegistrar registrar, Socket client, Function<String, Object> func) {
            super(registrar, client, func);
            this.currentContext = new BaseShellContext(this) {
            };
            // Init worker
            final AtomicInteger incr = new AtomicInteger(0);
            this.processWorker = new ThreadPoolExecutor(1, 1, 0, SECONDS, new LinkedBlockingDeque<>(1), r -> {
                String processId = getClass().getSimpleName() + "-worker-" + incr.incrementAndGet();
                Thread t = new Thread(r, processId);
                t.setDaemon(true);
                return t;
            });
        }

        BaseShellContext getContext() {
            return currentContext;
        }

        void setContext(BaseShellContext context) {
            notNullOf(context, "ShellContext");
            this.currentContext = context;
        }

        public AuthenticationInfo getAuthenticationInfo() {
            return authInfo;
        }

        @Override
        public void run() {
            while (running.get() && isActive()) {
                try {
                    Object signal = new ObjectInputStream(_in).readObject();
                    isInstanceOf(Signal.class, signal);
                    log.info("<= {}", signal);

                    Object output = null;
                    // Register shell methods
                    if (signal instanceof MetaSignal) {
                        authInfo.setSessionId(genSessionID());
                        output = new MetaSignal(registrar.getTargetMethods(), authInfo.getSessionId());
                    } else {
                        notNull(getAuthenticationInfo().getSessionId(), InternalShellException.class,
                                "Internal error, shell handler sessionId required.");
                        notNull(((Signal) signal).getSessionId(), InternalShellException.class,
                                "Internal error, request shell signal sessionId required.");
                    }
                    // Pre login
                    if (signal instanceof PreLoginSignal) {
                        PreLoginSignal login = (PreLoginSignal) signal;
                        if (authInfo.isAuthenticated()) {
                            output = new LoginSignal(true, login.getSessionId()).withDesc("Authenticated.");
                        } else {
                            AclInfo acl = getConfig().getAcl();
                            if (acl.isEnabled()) {
                                if (acl.matchs(login.getUsername(), login.getPassword())) {
                                    // Sets authentication success info.
                                    authInfo.setUsername(login.getUsername());
                                    authInfo.setAuthenticated(true);
                                    authInfo.setHost(socket.getInetAddress().getHostName());
                                    authInfo.setTimestamp(currentTimeMillis());
                                    output = new LoginSignal(true, authInfo.getSessionId()).withDesc("Authentication success.");
                                } else {
                                    output = new LoginSignal(false).withDesc("Authentication failure.");
                                }
                            } else {
                                output = new LoginSignal(false).withDesc("No authentication required.");
                            }
                        }
                    }
                    // Ask interruption.
                    else if (signal instanceof PreInterruptSignal) {
                        // Call pre-interrupt events.
                        currentContext.getUnmodifiableEventListeners().forEach(l -> l.onPreInterrupt(currentContext));
                        // Ask if the client is interrupt.
                        output = new AskInterruptSignal("Are you sure you want to cancel execution? (y|n)");
                    }
                    // Confirm interruption
                    else if (signal instanceof AckInterruptSignal) {
                        AckInterruptSignal ack = (AckInterruptSignal) signal;
                        // Call interrupt events.
                        currentContext.getUnmodifiableEventListeners()
                                .forEach(l -> l.onInterrupt(currentContext, ack.getConfirm()));
                    }
                    // Stdin of commands
                    else if (signal instanceof StdinSignal) {
                        StdinSignal stdin = (StdinSignal) signal;
                        // Call command events.
                        currentContext.getUnmodifiableEventListeners().forEach(l -> l.onCommand(currentContext, stdin.getLine()));

                        // Resolve that client input cannot be received during
                        // blocking execution.
                        processWorker.execute(() -> {
                            try {
                                currentStdin.set(new StdinCommandWrapper(stdin, this));

                                /**
                                 * Only {@link ShellContext} printouts are
                                 * supported, and return value is no longer
                                 * supported (otherwise it will be ignored)
                                 */
                                function.apply(stdin.getLine());

                                /**
                                 * see:{@link EmbeddedServerShellHandler#preHandleInput()}#MARK2
                                 */
                                if (currentContext.getState() != RUNNING) {
                                    currentContext.completed();
                                }
                            } catch (Throwable e) {
                                log.error(format("Failed to handle shell command: [%s]", stdin.getLine()), e);
                                handleError(e);
                            } finally {
                                currentStdin.remove();
                            }
                        });
                    }

                    if (nonNull(output)) { // Write to console.
                        currentContext.printf0(output);
                    }
                } catch (Throwable th) {
                    handleError(th);
                } finally {
                    try {
                        sleep(100L);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {
            // Prevent threadContext memory leakage.
            cleanup();

            // Close the current socket
            super.close();

            // Clear the current channel
            Thread t = workers.remove(this);
            if (t != null) {
                t.interrupt();
                t = null;
            }
            log.debug("Remove shellHandler: {}, actual: {}", this, workers.size());
        }

        /**
         * Error handling
         * 
         * @param th
         */
        private void handleError(Throwable th) {
            if ((th instanceof SocketException) || (th instanceof EOFException) || !isActive()) {
                log.warn("Disconnect for client : {}", socket);
                try {
                    close();
                } catch (IOException e) {
                    log.error("Close failure.", e);
                }
            } else {
                currentContext.printf0(th);
            }
        }
    }

    /**
     * Shell channel authentication information.
     */
    @Getter
    @Setter
    @ToString
    static class AuthenticationInfo {
        /** Shell session ID. */
        private String sessionId;
        /** Authentication username. */
        private String username;
        /** Authentication state. */
        private boolean authenticated = false;
        /** Authentication host. */
        private String host;
        /** Authentication timestamp. */
        private long timestamp;
    }

    /**
     * Standard input command signal info wrapper.
     */
    static class StdinCommandWrapper {
        private final StdinSignal stdin;
        private final ServerSignalHandler handler;

        public StdinCommandWrapper(StdinSignal stdin, ServerSignalHandler handler) {
            this.stdin = notNullOf(stdin, "stdin");
            this.handler = notNullOf(handler, "handler");
        }

        public StdinSignal getStdin() {
            return stdin;
        }

        public ServerSignalHandler getHandler() {
            return handler;
        }
    }

}