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

import static com.wl4g.infra.common.lang.Assert2.isTrue;
import static com.wl4g.infra.common.lang.Assert2.notNull;
import static com.wl4g.infra.common.lang.Exceptions.getRootCausesString;
import static com.wl4g.infra.common.log.SmartLoggerFactory.getLogger;
import static com.wl4g.shell.common.annotation.ShellMethod.InterruptType.ALLOW;
import static com.wl4g.shell.common.annotation.ShellMethod.InterruptType.NOT_ALLOW;
import static com.wl4g.shell.common.signal.ChannelState.COMPLETED;
import static com.wl4g.shell.common.signal.ChannelState.INTERRUPTED;
import static com.wl4g.shell.common.signal.ChannelState.NEW;
import static com.wl4g.shell.common.signal.ChannelState.RUNNING;
import static java.lang.String.format;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.equalsAny;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.wl4g.shell.common.annotation.ShellMethod;
import com.wl4g.shell.common.annotation.ShellMethod.InterruptType;
import com.wl4g.shell.common.exception.ChannelShellException;
import com.wl4g.shell.common.exception.NoSupportedInterruptShellException;
import com.wl4g.shell.common.exception.ShellException;
import com.wl4g.shell.common.registry.TargetMethodWrapper;
import com.wl4g.shell.common.signal.BOFStdoutSignal;
import com.wl4g.shell.common.signal.ChannelState;
import com.wl4g.shell.common.signal.EOFStdoutSignal;
import com.wl4g.shell.common.signal.Signal;
import com.wl4g.shell.common.signal.StderrSignal;
import com.wl4g.shell.common.signal.StdoutSignal;
import com.wl4g.shell.core.handler.EmbeddedShellServer.ServerSignalHandler;

/**
 * Base shell handler context
 *
 * @author wangl.sir
 * @version v1.0 2019-5月24日
 * @since v1.0
 */
abstract class BaseShellContext implements ShellContext {
    public static final String DEFAULT_INTERRUPT_LISTENER = "defaultInterruptEventListener";

    protected final Logger log = getLogger(getClass());

    /**
     * Event listeners
     */
    final private Map<String, ShellEventListener> eventListeners = synchronizedMap(new LinkedHashMap<>(4));

    /**
     * Shell message channel.
     */
    private ServerSignalHandler client;

    /**
     * Line result message state.
     */
    private ChannelState state = NEW;

    /**
     * Shell target wrapper object currently executing.
     * {@link TargetMethodWrapper}
     */
    private TargetMethodWrapper target;

    protected BaseShellContext() {
    }

    BaseShellContext(ShellContext context) {
        if (context instanceof BaseShellContext) {
            BaseShellContext shc = (BaseShellContext) context;
            setChannel(shc.client);
            setState(shc.state);
            setTarget(shc.target);
            // Copy event listeners.
            shc.eventListeners.forEach((name, l) -> eventListeners.putIfAbsent(name, l));
        }
    }

    BaseShellContext(ServerSignalHandler channel) {
        setChannel(channel);
    }

    public ServerSignalHandler getChannel() {
        if (isNull(client)) {
            throw new Error("The shell clientChannel should not be null???");
        }
        return client;
    }

    void setChannel(ServerSignalHandler channel) {
        notNull(channel, "Shell channel must not be null");
        this.client = channel;
        // Default listener register.
        eventListeners.putIfAbsent(DEFAULT_INTERRUPT_LISTENER, new ShellEventListener() {
            // Ignore
        });
    }

    ShellContext setState(ChannelState state) {
        notNull(state, "State must not be null");
        this.state = state;
        return this;
    }

    ChannelState getState() {
        if (isNull(state)) {
            throw new Error("The shell channelState should not be null???");
        }
        return state;
    }

    void setTarget(TargetMethodWrapper target) {
        notNull(target, "Target method must not be null");
        this.target = target;
    }

    TargetMethodWrapper getTarget() {
        if (isNull(target)) {
            throw new Error("The shell target method should not be null???");
        }
        return target;
    }

    /**
     * Open the channel of the current command line, effect: at this time, the
     * client console will wait for execution to complete (until the
     * {@link #completed()} method is called).
     */
    final synchronized ShellContext begin() {
        state = RUNNING;
        // Print begin mark
        printf0(new BOFStdoutSignal());
        return this;
    }

    /**
     * Complete processing the current command line channel, effect: the client
     * will reopen the console prompt.
     * 
     * <b><font color=red>Note: Don't forget to execute it, or the client
     * console will pause until it timeout.</font><b>
     * 
     * @throws ChannelShellException
     */
    @Override
    public synchronized void completed() throws ChannelShellException {
        state = COMPLETED;
        printf0(new EOFStdoutSignal()); // Ouput end mark
    }

    /**
     * Are you currently in an interrupt state? (if the current thread does not
     * open the shell channel, it will return false, that is, uninterrupted)
     * 
     * 
     * @return
     * @throws NoSupportedInterruptShellException
     */
    @Override
    public boolean isInterrupted() throws NoSupportedInterruptShellException {
        // Check if the current shell method supports interrupts.
        if (getTarget().getShellMethod().interruptible() == NOT_ALLOW) {
            throw new NoSupportedInterruptShellException(
                    format("Interruptible is not supported. You can set @%s(interruptible=%s.%s)",
                            ShellMethod.class.getSimpleName(), InterruptType.class.getSimpleName(), ALLOW.name()));
        }
        return nonNull(state) ? (state == INTERRUPTED) : false;
    }

    /**
     * Get unmodifiable event listeners.
     * 
     * @return
     */
    @Override
    public Collection<ShellEventListener> getUnmodifiableEventListeners() {
        return unmodifiableCollection(eventListeners.values());
    }

    /**
     * Add event listener
     * 
     * @param name
     * @param eventListener
     * @return
     */
    @Override
    public boolean addEventListener(String name, ShellEventListener eventListener) {
        notNull(eventListener, "eventListener must not be null");
        if (nonNull(eventListeners.putIfAbsent(name, eventListener))) {
            throw new ShellException(format("Add an existed event listener: %s", name));
        }
        return eventListeners.get(name) == eventListener;
    }

    /**
     * Remove event listener
     * 
     * @param name
     * @return
     */
    @Override
    public boolean removeEventListener(String name) {
        // Check built-in event listener.
        if (equalsAny(name, DEFAULT_INTERRUPT_LISTENER)) {
            throw new ShellException(format("built-in listener is not allowed to be remove, %s", name));
        }
        return nonNull(eventListeners.remove(name));
    }

    /**
     * Print message to the client console.
     *
     * @param output
     * @throws ChannelShellException
     */
    protected ShellContext printf0(Object output) throws ChannelShellException {
        notNull(output, "Printf message must not be null.");
        isTrue((output instanceof Signal || output instanceof CharSequence || output instanceof Throwable),
                format("Unsupported print message types: %s", output.getClass()));

        if (nonNull(getChannel()) && getChannel().isActive()) {
            try {
                log.info("=> {}", output.toString());
                if (output instanceof CharSequence) {
                    getChannel().writeFlush(new StdoutSignal(output.toString()));
                } else if (output instanceof Throwable) {
                    getChannel().writeFlush(new StderrSignal((Throwable) output));
                } else if (output instanceof Signal) {
                    getChannel().writeFlush(output);
                } else {
                    throw new ChannelShellException(format("Unsupported printf shell message of '%s'", output));
                }
            } catch (IOException e) {
                log.error("Failed to printf shell message", getRootCausesString(e));
            }
        } else {
            throw new ChannelShellException("The current console channel may be closed!");
        }
        return this;
    }

}