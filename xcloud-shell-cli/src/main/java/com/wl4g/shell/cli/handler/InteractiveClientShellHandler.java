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

import static com.wl4g.component.common.cli.ProcessUtils.printProgress;
import static com.wl4g.component.common.lang.StringUtils2.isEmpty;
import static com.wl4g.shell.cli.config.ClientShellHandlerRegistrar.getSingle;
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_EX;
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_EXIT;
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_LO;
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_LOGIN;
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_QU;
import static com.wl4g.shell.common.cli.BuiltInCommand.CMD_QUIT;
import static com.wl4g.shell.common.i18n.I18nResourceMessageBundles.getMessage;
import static com.wl4g.shell.common.utils.ShellUtils.isTrue;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.err;
import static java.lang.System.out;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.reader.UserInterruptException;

import com.wl4g.shell.cli.config.ClientShellConfiguration;
import com.wl4g.shell.common.signal.AckInterruptSignal;
import com.wl4g.shell.common.signal.AskInterruptSignal;
import com.wl4g.shell.common.signal.BOFStdoutSignal;
import com.wl4g.shell.common.signal.EOFStdoutSignal;
import com.wl4g.shell.common.signal.LoginSignal;
import com.wl4g.shell.common.signal.MetaSignal;
import com.wl4g.shell.common.signal.PreInterruptSignal;
import com.wl4g.shell.common.signal.PreLoginSignal;
import com.wl4g.shell.common.signal.ProgressSignal;
import com.wl4g.shell.common.signal.Signal;
import com.wl4g.shell.common.signal.StderrSignal;
import com.wl4g.shell.common.signal.StdoutSignal;

/**
 * Interactive shell component runner
 * 
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0 2019年4月14日
 * @since
 */
@SuppressWarnings("unused")
public class InteractiveClientShellHandler extends DefaultClientShellHandler {

    /** Running status. */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** Mark the current console pending state. */
    private volatile boolean pauseState = false;

    /** Current line standard input string. */
    private Object stdin;

    /** Payload command last sent timestamp, for timeout check. */
    private long lastCmdSentTime = 0L;

    public InteractiveClientShellHandler(ClientShellConfiguration config) {
        super(config);
    }

    @Override
    public void run(String[] args) {
        if (!running.compareAndSet(false, true)) {
            err.println(format("Already running of '%s'", this));
            return;
        }

        while (true) {
            try {
                printDebug("readLine...");
                stdin = lineReader.readLine(getPrompt());
                printDebug("readLine: " + stdin);

                synchronized (this) { // MARK2
                    notifyAll(); // see:MARK3
                }

                if (!isEmpty(stdin) && equalsAny(stdin.toString(), CMD_LOGIN, CMD_LO)) {
                    waitForPreLoginStdin();
                }

                // Payload command
                if (!isEmpty(stdin) && !isPaused()) {
                    paused(); // Paused wait complete
                    lastCmdSentTime = currentTimeMillis();
                    writeStdin(stdin); // Do send command
                }

            } catch (UserInterruptException e) { // e.g: Ctrl+C
                // Last command is not completed, send interrupt signal
                // stop gracefully
                if (isPaused()) {
                    writeStdin(new PreInterruptSignal(true));
                } else {
                    // Last command completed, interrupt allowed.
                    out.println(
                            format("Command is cancelled. to exit please use: %s|%s|%s|%s", CMD_EXIT, CMD_EX, CMD_QUIT, CMD_QU));
                }
            } catch (Throwable e) {
                printError(EMPTY, e);
            }
        }
    }

    @Override
    protected void afterShellExecution(Object output) throws Exception {
        if (output instanceof Signal) { // Remote command stdout?
            // Meta
            if (output instanceof MetaSignal) {
                MetaSignal meta = (MetaSignal) output;
                getSingle().merge(meta.getRegistedMethods());
                super.sessionId = meta.getSessionId();
            }
            // Login
            else if (output instanceof LoginSignal) {
                LoginSignal login = (LoginSignal) output;
                if (login.isAuthenticated()) {
                    out.println(login.getDesc());
                } else {
                    err.println(login.getDesc());
                }
                wakeup();
            }
            // Progress
            else if (output instanceof ProgressSignal) {
                ProgressSignal pro = (ProgressSignal) output;
                printProgress(pro.getTitle(), pro.getProgress(), pro.getWhole(), '=');
            }
            // Ask interrupt
            else if (output instanceof AskInterruptSignal) {
                AskInterruptSignal ask = (AskInterruptSignal) output;
                printDebug("ask interrupt ...");

                // Print retry ask prompt
                do {
                    lineReader.printAbove(ask.getSubject());
                    synchronized (this) { // MARK3
                        wait(TIMEOUT); // see:MARK2
                    }
                } while (isEmpty(stdin));

                AckInterruptSignal confirm = new AckInterruptSignal(isTrue(trimToEmpty(stdin.toString()), false));
                if (confirm.getConfirm()) {
                    out.println("Command interrupting...");
                } else {
                    out.println(getMessage("label.interrupt.cancel"));
                }
                // Echo interrupt
                writeStdin(confirm);
            }
            // Stderr
            else if (output instanceof StderrSignal) {
                StderrSignal stderr = (StderrSignal) output;
                printError("-ERROR:", stderr.getThrowable());
                wakeup();
            }
            // BOF stdout
            else if (output instanceof BOFStdoutSignal) {
                // Ignore
            }
            // EOF stdout
            else if (output instanceof EOFStdoutSignal) {
                wakeup();
            }
            // Stdout
            else if (output instanceof StdoutSignal) {
                out.println(((StdoutSignal) output).getContent());
            }
        } else { // Local command stdout?
            wakeup();
        }

        // Print of local command stdout.
        if (output instanceof CharSequence) {
            out.println(output);
        }
    }

    /**
     * Waiting input login credentials.
     * 
     * @return
     */
    private PreLoginSignal waitForPreLoginStdin() {
        String username = null, password = null;
        do {
            username = lineReader.readLine(getMessage("label.input.username").concat("\n"));
        } while (isEmpty(username));
        do {
            password = lineReader.readLine(getMessage("label.input.password").concat("\n"), new Character('\0'));
        } while (isEmpty(password));
        return (PreLoginSignal) (stdin = new PreLoginSignal(username, password));
    }

    /**
     * Pause wait for completed. </br>
     * {@link DefaultClientShellHandler#wakeup()}
     */
    private void paused() {
        printDebug(format("waitForCompleted: %s, completed: %s", this, pauseState));
        pauseState = true;
    }

    /**
     * Wake-up for lineReader watching. </br>
     * 
     * {@link DefaultClientShellHandler#waitForComplished()}
     */
    private void wakeup() {
        printDebug(format("Wakeup: %s, completed: %s", this, pauseState));
        pauseState = false;
    }

    /**
     * Get the current status prompt.
     * 
     * @return
     */
    private String getPrompt() {
        printDebug(format("getPrompt: %s, pauseState: %s", this, pauseState));
        return isPaused() ? EMPTY : getAttributed().toAnsi(lineReader.getTerminal());
    }

    /**
     * Check whether it is currently paused? (if the last command is not
     * completed, it is paused)
     * 
     * @return
     */
    private boolean isPaused() {
        return pauseState && (currentTimeMillis() - lastCmdSentTime) < TIMEOUT;
    }

}