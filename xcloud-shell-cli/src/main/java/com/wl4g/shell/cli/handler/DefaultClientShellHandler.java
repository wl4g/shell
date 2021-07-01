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
package com.wl4g.shell.cli.handler;

import static com.wl4g.component.common.lang.Assert2.isTrue;
import static com.wl4g.component.common.lang.Assert2.notNull;
import static com.wl4g.component.common.lang.Assert2.state;
import static com.wl4g.component.common.lang.Exceptions.getStackTraceAsString;
import static com.wl4g.shell.cli.config.ClientShellHandlerRegistrar.getSingle;
import static com.wl4g.shell.common.annotation.ShellOption.GNU_CMD_LONG;
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_HE;
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_HELP;
import static com.wl4g.shell.common.utils.LineUtils.clean;
import static com.wl4g.shell.common.utils.LineUtils.parse;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.err;
import static java.lang.System.exit;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.SystemUtils.USER_HOME;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.jline.reader.LineReader.HISTORY_FILE;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

import com.wl4g.shell.cli.command.DefaultBuiltInCommand;
import com.wl4g.shell.cli.config.ClientShellConfiguration;
import com.wl4g.shell.cli.config.DynamicCompleter;
import com.wl4g.shell.common.handler.BaseSignalHandler;
import com.wl4g.shell.common.handler.GenericShellHandler;
import com.wl4g.shell.common.registry.ShellHandlerRegistrar;
import com.wl4g.shell.common.signal.MetaSignal;
import com.wl4g.shell.common.signal.Signal;
import com.wl4g.shell.common.signal.StdinSignal;

/**
 * CLi shell component runner
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年4月14日
 * @since
 */
public abstract class DefaultClientShellHandler extends GenericShellHandler implements ClientShellHandler {

    /**
     * Used to get the name of the set target service (that is, the server that
     * the current shell client will connect to) (because the same computer may
     * start many different shell services)
     */
    public static final String ARG_SERV_NAME = "servname";

    /**
     * IBid, note that this priority is higher than ARG_SERV_PIDS
     */
    public static final String ARG_SERV_POINT = "servpoint";

    /**
     * Commands prompt string.
     */
    public static final String ARG_PROMPT = "prompt";

    /**
     * Enable debugging
     */
    public static final long TIMEOUT = Long.parseLong(getProperty("timeout", valueOf(60_000L)));

    /**
     * Attributed string
     */
    public static final AttributedString DEFAULT_ATTRIBUTED = new AttributedString("console");

    /**
     * Shell configuration
     */
    protected final ClientShellConfiguration config;

    /**
     * Line reader
     */
    protected final LineReader lineReader;

    /**
     * Shell client handler
     */
    private ClientSignalHandler clientChannel;

    /** Current shell channel session ID. */
    protected String sessionId;

    /**
     * Current process exception statcktrace as strings.
     */
    private String stacktraceAsString;

    public DefaultClientShellHandler(ClientShellConfiguration config) {
        super(config, getSingle());
        notNull(config, "configuration is null, please check configure");
        this.config = config;

        // Init create lineReader
        this.lineReader = createLineReader();

        // Initialization
        try {
            initialize();
        } catch (Throwable t) {
            printError(EMPTY, t);
            shutdown();
        }
    }

    public final ShellHandlerRegistrar getRegistrar() {
        return registrar;
    }

    public final LineReader getLineReader() {
        return lineReader;
    }

    @Override
    public void shutdown() {
        try {
            out.println("Shutting down, bye...");

            // Close client
            closeQuietly();
        } catch (Throwable e) {
            printError("Shutdown failure.", e);
        }

        // Gracefully halt
        exit(0);
    }

    @Override
    protected void printError(String abnormal, Throwable th) {
        stacktraceAsString = getStackTraceAsString(th);
        super.printError(abnormal, th);
    }

    /**
     * Get last abnormal stack-trace string
     * 
     * @return
     */
    public String getLastStacktrace() {
        return stacktraceAsString;
    }

    /**
     * Submission stdin message to remote
     * 
     * @param line
     * @throws IOException
     */
    protected void writeStdin(Object stdin) {
        try {
            boolean isRemoteCommand = true;
            if (stdin instanceof String) {
                String line = (String) stdin;
                List<String> cmds = parse(line);
                if (!cmds.isEmpty()) {
                    // $> [help|clear|history...]
                    if (registrar.contains(cmds.get(0))) { // Local command?
                        isRemoteCommand = false;
                        DefaultBuiltInCommand.senseLine(line);
                        process(line);
                        return;
                    }
                    // help command? [MARK0] $> add --help
                    else if (cmds.size() > 1 && equalsAny(cmds.get(1), (GNU_CMD_LONG + CMD_HELP), (GNU_CMD_LONG + CMD_HE))) {
                        isRemoteCommand = false;
                        // e.g: '$> help add'
                        line = clean(CMD_HELP) + " " + cmds.get(0);
                        // Set current line
                        DefaultBuiltInCommand.senseLine(line);
                        process(line);
                        return;
                    }
                }
                // Wrap string command
                stdin = new StdinSignal(line);
            }

            // Check connect & send to server.
            if (isRemoteCommand) {
                ensureClient();
                Signal stdin0 = (Signal) stdin;
                // Add current shell channel sessionId.
                stdin0.setSessionId(sessionId);
                clientChannel.writeFlush(stdin0);
            }
        } catch (IOException e) {
            printError(EMPTY, e);
        }
    }

    /**
     * Get line attributed.
     * 
     * @return
     */
    protected AttributedString getAttributed() {
        String prompt = getProperty(ARG_PROMPT);
        prompt = isBlank(prompt) ? getProperty(ARG_SERV_NAME) : prompt;
        return isBlank(prompt) ? DEFAULT_ATTRIBUTED : new AttributedString(format("%s> ", prompt));
    }

    /**
     * Create {@link LineReader}.
     * 
     * @return
     */
    private LineReader createLineReader() {
        try {
            return LineReaderBuilder.builder().appName("XCloud Shell").completer(new DynamicCompleter(getSingle()))
                    .terminal(TerminalBuilder.terminal()).build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Initialization runner
     * 
     * @throws IOException
     */
    private void initialize() throws IOException {
        // Register commands
        registrar.register(new DefaultBuiltInCommand(this));

        // Initialize remote register commands
        writeStdin(new MetaSignal());

        // Set history persist file
        File file = new File(USER_HOME + "/.wl4g/shell/history");
        if (!file.getParentFile().exists()) {
            state(file.getParentFile().mkdirs(),
                    format("Failed to create, for directory: '%s'", file.getParentFile().getAbsolutePath()));
        }
        if (!file.exists()) {
            String errmsg = format("Failed to create, for file: '%s'", file.getAbsolutePath());
            try {
                state(file.createNewFile(), errmsg);
            } catch (IOException e) {
                throw new IllegalStateException(errmsg);
            }
        }
        lineReader.setVariable(HISTORY_FILE, file.getAbsolutePath());

        // Print banners
        banner();
    }

    /**
     * Print banner
     */
    private void banner() {
        out.println(format("%s", config.getBanner()));
        String v = getClass().getPackage().getImplementationVersion();
        out.println(format("Version: %s", isBlank(v) ? "unknown" : v));
        out.println(format("Time: %s", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
        out.println();
    }

    /**
     * Ensure client handler
     * 
     * @throws IOException
     */
    @SuppressWarnings("resource")
    private void ensureClient() throws IOException {
        boolean create = false;
        if (clientChannel == null) {
            create = true;
        } else if (!clientChannel.isActive()) {
            create = true;
            closeQuietly();
        }

        if (create) {
            Object[] point = determineServPoint();
            printDebug(format("Connecting to %s:%s ... \n", point[0], point[1]));

            Socket s = null;
            try {
                s = new Socket((String) point[0], (int) point[1]);
            } catch (IOException e) {
                String errmsg = format("Connecting to '%s'(%s) failure! cause by: %s", getProperty(ARG_SERV_NAME), point[1],
                        getRootCauseMessage(e));
                throw new IllegalStateException(errmsg);
            }

            clientChannel = new ClientSignalHandler(this, s, result -> null).starting();
        }

    }

    /**
     * Determine the corresponding server port (identified by PID) of the
     * current client
     * 
     * @return
     */
    private Object[] determineServPoint() {
        String servName = getProperty(ARG_SERV_NAME);
        String servPoint = getProperty(ARG_SERV_POINT);

        if (isBlank(servName) && isBlank(servPoint)) {
            throw new IllegalArgumentException(format(
                    "JVM startup argument -D%s(e.g. -D%s=8080) and -D%s(e.g. -D%s=myapp1) must be one of the two, and only -D%s are adopted when both exist",
                    ARG_SERV_POINT, ARG_SERV_POINT, ARG_SERV_NAME, ARG_SERV_NAME, ARG_SERV_POINT));
        }

        //
        // Direct use of specified point.</br>
        // Can be used to connect to remote service console.
        //
        if (isNotBlank(servPoint)) {
            isTrue(contains(servPoint, ":") && servPoint.length() > 8,
                    format("Invalid server point. e.g. -D%s=10.0.0.11", ARG_SERV_POINT));
            String[] parts = servPoint.split(":");
            isTrue(isNumeric(parts[1]), format("Invalid server port is %s", servPoint));
            int port = Integer.parseInt(parts[1]);
            isTrue((port > 1024 && port < 65535), format("Server port must be between 1024 and 65535, actual is %s", servPoint));
            return new Object[] { parts[0], port };
        }

        //
        // Obtain port according to PIDS.</br>
        // Can only be used to connect to the local service console.
        //

        return new Object[] { config.getServer(), ensureDetermineServPort(servName) };
    }

    /**
     * Quietly client close
     * 
     * @throws IOException
     */
    private void closeQuietly() {
        try {
            if (clientChannel != null) {
                clientChannel.close();
            }
        } catch (IOException e) {
            printError("Failed to close channel", e);
        }
    }

    /**
     * Check client channel is active.
     * 
     * @return
     */
    protected boolean isActive() {
        return nonNull(clientChannel) && clientChannel.isActive();
    }

    /**
     * Client shell message channel handler
     * 
     * @author Wangl.sir <983708408@qq.com>
     * @version v1.0 2019年5月2日
     * @since
     */
    class ClientSignalHandler extends BaseSignalHandler {

        /**
         * Line process runner.
         */
        final private DefaultClientShellHandler shellHandler;

        /**
         * Boot boss thread
         */
        private Thread boss;

        public ClientSignalHandler(DefaultClientShellHandler shellHandler, Socket socket, Function<String, Object> function) {
            super(shellHandler.getRegistrar(), socket, function);
            this.shellHandler = shellHandler;
        }

        public ClientSignalHandler starting() {
            this.boss = new Thread(this);
            this.boss.start();
            return this;
        }

        @Override
        public void run() {
            while (!boss.isInterrupted() && isActive()) {
                try {
                    // Read a string command process result
                    Object input = new ObjectInputStream(_in).readObject();

                    // Post process
                    afterShellExecution(input);

                } catch (SocketException | EOFException e) {
                    err.println("Connection tunnel closed!");
                    boss.interrupt();
                    try {
                        close();
                    } catch (IOException e1) {
                        shellHandler.printError(EMPTY, e);
                    }
                } catch (Throwable e) {
                    shellHandler.printError(EMPTY, e);
                }
            }
        }

    }

}