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
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_LO;
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_LOGIN;
import static com.wl4g.shell.common.signal.ChannelState.RUNNING;
import static com.wl4g.shell.core.utils.AuthUtils.genSessionID;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
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

import javax.annotation.Nullable;

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
import com.wl4g.shell.core.config.ServerShellProperties.AclInfo.CredentialsInfo;
import com.wl4g.shell.core.session.ShellSession;
import com.wl4g.shell.core.session.ShellSessionDAO;
import com.wl4g.shell.core.utils.AuthUtils;

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

    public EmbeddedShellServer(ServerShellProperties config, String appName, ShellHandlerRegistrar registrar,
            ShellSessionDAO sessionDAO) {
        super(config, appName, registrar, sessionDAO);
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
        ShellSession session = stdin.getHandler().getShellSession();
        if (!session.isAuthenticated()) {
            throw new UnauthenticationShellException(format(
                    "This command method must be authenticated to execution, Please exec the command: '%s|%s' to authentication.",
                    CMD_LOGIN, CMD_LO));
        }

        // Check ACL by roles.
        String[] permissions = tm.getShellMethod().permissions();
        CredentialsInfo credentials = getConfig().getAcl().getCredentialsInfo(session.getUsername());
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

        /** Current shell commands process worker */
        private final ExecutorService processWorker;

        /** Current shell commands context of {@link ShellContext} */
        private BaseShellContext shellContext;

        /** Binding shell channel session ID. {@link ShellSession}. */
        private String bindSessionId;

        public ServerSignalHandler(ShellHandlerRegistrar registrar, Socket client, Function<String, Object> func) {
            super(registrar, client, func);
            this.shellContext = new BaseShellContext(this) {
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
            return shellContext;
        }

        void setContext(BaseShellContext context) {
            this.shellContext = notNullOf(context, "ShellContext");
        }

        ShellSession obtainShellSession(@Nullable String sessionId) {
            ShellSession session = null;
            if (!isBlank(sessionId)) {
                session = sessionDAO.get(sessionId);
            }
            if (isNull(session)) {
                session = new ShellSession(genSessionID(), null, false, null, 0, 0);
                updateSession(session);
            }
            bindSessionId = session.getSessionId();
            return notNull(session, "Cannot obtain shell session.");
        }

        ShellSession getShellSession() {
            return notNull(sessionDAO.get(bindSessionId), InternalShellException.class,
                    "Internal error, not shell signal binding sessionId.");
        }

        void updateSession(ShellSession session) {
            if (nonNull(session)) {
                session.setLatestTimestamp(currentTimeMillis());
                sessionDAO.put(session);
            }
        }

        @Override
        public void run() {
            while (running.get() && isActive()) {
                try {
                    Object input = new ObjectInputStream(_in).readObject();
                    isInstanceOf(Signal.class, input);
                    Signal signal = (Signal) input;
                    log.debug("<= {}", signal);

                    Object output = null;
                    ShellSession session = obtainShellSession(signal.getSessionId());
                    // Register shell methods
                    if (signal instanceof MetaSignal) {
                        output = new MetaSignal(registrar.getTargetMethods(), session.getSessionId());
                    } else {
                        notNull(((Signal) signal).getSessionId(), InternalShellException.class,
                                "Internal error, request shell signal sessionId required.");
                        updateSession(session);
                    }
                    // Pre login
                    if (signal instanceof PreLoginSignal) {
                        PreLoginSignal login = (PreLoginSignal) signal;
                        if (session.isAuthenticated()) {
                            output = new LoginSignal(true, session.getSessionId()).withDesc("Authenticated.");
                        } else {
                            if (getConfig().getAcl().isEnabled()) {
                                if (getConfig().getAcl().matchs(login.getUsername(), login.getPassword())) {
                                    // Sets authentication success info.
                                    session.setUsername(login.getUsername());
                                    session.setAuthenticated(true);
                                    session.setHost(socket.getInetAddress().getHostName());
                                    session.setStartTimestamp(currentTimeMillis());
                                    updateSession(session);
                                    output = new LoginSignal(true, session.getSessionId()).withDesc("Authentication success.");
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
                        shellContext.getUnmodifiableEventListeners().forEach(l -> l.onPreInterrupt(shellContext));
                        // Ask if the client is interrupt.
                        output = new AskInterruptSignal("Are you sure you want to cancel execution? (y|n)");
                    }
                    // Confirm interruption
                    else if (signal instanceof AckInterruptSignal) {
                        AckInterruptSignal ack = (AckInterruptSignal) signal;
                        // Call interrupt events.
                        shellContext.getUnmodifiableEventListeners().forEach(l -> l.onInterrupt(shellContext, ack.getConfirm()));
                    }
                    // Stdin of commands
                    else if (signal instanceof StdinSignal) {
                        StdinSignal stdin = (StdinSignal) signal;
                        // Call command events.
                        shellContext.getUnmodifiableEventListeners().forEach(l -> l.onCommand(shellContext, stdin.getLine()));

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
                                if (shellContext.getState() != RUNNING) {
                                    shellContext.completed();
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
                        shellContext.printf0(output);
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
                shellContext.printf0(th);
            }
        }
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