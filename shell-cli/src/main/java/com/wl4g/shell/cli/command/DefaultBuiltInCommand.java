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
package com.wl4g.shell.cli.command;

import static com.wl4g.infra.common.cli.StandardFormatter.getHelpFormat;
import static com.wl4g.infra.common.lang.Assert2.hasText;
import static com.wl4g.infra.common.lang.Assert2.notNull;
import static com.wl4g.infra.common.lang.Assert2.state;
import static java.lang.System.err;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.cli.Options;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import com.wl4g.shell.cli.config.ClientShellHandlerRegistrar;
import com.wl4g.shell.cli.handler.DefaultClientShellHandler;
import com.wl4g.shell.cli.handler.InteractiveClientShellHandler;
import com.wl4g.shell.common.annotation.ShellMethod;
import com.wl4g.shell.common.cli.BuiltInCommand;
import com.wl4g.shell.common.cli.HelpOptions;
import com.wl4g.shell.common.utils.LineUtils;

/**
 * Default internal command.
 * 
 * @author wangl.sir
 * @version v1.0 2019-5月8日
 * @since v1.0
 */
public class DefaultBuiltInCommand extends BuiltInCommand {

    /**
     * Built-in Default internal commands group name.
     */
    public static final String DEFAULT_GROUP = "Built-in commands";

    /**
     * Current read line strings.
     */
    private static final ThreadLocal<String> lineCache = new InheritableThreadLocal<>();

    /**
     * Shell handler bean registry
     */
    protected final ClientShellHandlerRegistrar registry;

    /**
     * Line process runner.
     */
    protected final DefaultClientShellHandler runner;

    public DefaultBuiltInCommand(DefaultClientShellHandler runner) {
        notNull(runner, "runner is null, please check configure");
        this.runner = runner;
        this.registry = (ClientShellHandlerRegistrar) runner.getRegistrar();
        notNull(registry, "Registry must not be null");
    }

    /**
     * @see {@link InteractiveClientShellHandler#waitForPreLoginStdin()}
     */
    @ShellMethod(keys = { CMD_LOGIN, CMD_LO }, group = DEFAULT_GROUP, help = "Interactive login")
    public void login() {
    }

    @ShellMethod(keys = { CMD_STACKTRACE, CMD_ST }, group = DEFAULT_GROUP, help = "Exit current process")
    public void stacktrace() {
        err.println(runner.getLastStacktrace());
    }

    @ShellMethod(keys = { CMD_QUIT, CMD_QU, CMD_EXIT, CMD_EX }, group = DEFAULT_GROUP, help = "Exit current process")
    public void exit() {
        runner.shutdown();
    }

    /**
     * See:<a href=
     * "https://github.com/jline/jline3/issues/183">https://github.com/jline/jline3/issues/183</a>
     */
    @ShellMethod(keys = { CMD_CLEAR, CMD_CLS }, group = DEFAULT_GROUP, help = "Clean up console history")
    public void clear() {
        Terminal terminal = runner.getLineReader().getTerminal();
        terminal.puts(Capability.clear_screen);
        terminal.flush();
    }

    @ShellMethod(keys = { CMD_HISTORY, CMD_HIS }, group = DEFAULT_GROUP, help = "View commands execution history")
    public String history() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        StringBuffer history = new StringBuffer();
        runner.getLineReader().getHistory().forEach(h -> {
            history.append(formatter.format(h.time()) + " - " + h.line());
            history.append("\n");
        });

        return history.toString();
    }

    /**
     * See:[{@link DefaultClientShellHandler#submit}.MARK0]
     * 
     * @return
     */
    @ShellMethod(keys = { CMD_HELP, CMD_HE }, group = DEFAULT_GROUP, help = "View supported commands help information")
    public String help() {
        try {
            // Input line string
            String line = lineCache.get();
            List<String> commands = LineUtils.parse(line);
            if (isNull(commands) || commands.isEmpty()) {
                return EMPTY;
            }

            // For example: $> help add
            if (commands.size() > 1) {
                String argname = commands.get(1);
                HelpOptions hopts = registry.getHelpOptions().get(argname);
                return getHelpFormat(argname, hopts, hopts.getShellMethod().help());
            }

            // For example: $> add --help
            StringBuffer helpBuf = new StringBuffer();
            // Help options(group name dict sort)
            Map<String, HelpGroupWrapper> hGroups = new TreeMap<>((o1, o2) -> o1.compareTo(o2));

            // Transform to group options
            for (Entry<String, HelpOptions> ent : registry.getHelpOptions().entrySet()) { // [MARK0]
                String argname = ent.getKey();
                HelpOptions hopts = ent.getValue();
                ShellMethod sm = hopts.getShellMethod();

                HelpGroupWrapper hGroup = hGroups.get(sm.group());
                if (hGroup == null) {
                    hGroup = new HelpGroupWrapper(sm.group());
                }
                HelpMethod hm = new HelpMethod(argname, hopts, sm.help());
                if (!hGroup.getHelpMethods().contains(hm)) {
                    hGroup.getHelpMethods().add(hm);
                }
                hGroups.put(sm.group(), hGroup);
            }

            // Move default group to first.
            HelpGroupWrapper defaultGroup = hGroups.remove(DEFAULT_GROUP);
            appendHelp(DEFAULT_GROUP, defaultGroup, helpBuf);

            // Print group options.
            hGroups.forEach((group, wrap) -> appendHelp(group, wrap, helpBuf));

            return helpBuf.toString();
        } finally {
            lineCache.remove();
        }
    }

    /**
     * Append help as strings.
     * 
     * @param group
     * @param hGroup
     * @param helpString
     */
    private void appendHelp(String group, HelpGroupWrapper hGroup, StringBuffer helpString) {
        state(nonNull(hGroup), "Internal error, help group is null, please check the server's log");

        helpString.append("\n----- " + group + " -----\n\n");
        for (HelpMethod hm : hGroup.getHelpMethods()) {
            helpString.append(getHelpFormat(hm.getArgname(), hm.getOptions(), hm.getHelp()));
            // Optimize: Printing default commands does not require line feeds.
            if (equalsIgnoreCase(group, DEFAULT_GROUP)) {
                helpString.delete(helpString.length() - LINE_SEPARATOR.length(), helpString.length());
            }
            if (!hm.getOptions().getOptions().isEmpty()) {
                helpString.append("\n");
            }
        }

    }

    /**
     * Set current line
     * 
     * @param line
     */
    public static void senseLine(String line) {
        lineCache.set(line);
    }

    /**
     * Help group wrapper
     * 
     * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
     * @version v1.0 2019-5月4日
     * @since v1.0
     */
    class HelpGroupWrapper {

        final private String group;

        final private LinkedList<HelpMethod> helpMethods = new LinkedList<>();

        public HelpGroupWrapper(String group) {
            hasText(group, "Group must not be empty");
            this.group = group;
        }

        public String getGroup() {
            return group;
        }

        public LinkedList<HelpMethod> getHelpMethods() {
            return helpMethods;
        }

        @Override
        public String toString() {
            return "HelpGroupWrapper [group=" + group + ", helpMethods=" + helpMethods + "]";
        }

    }

    /**
     * Help method
     * 
     * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
     * @version v1.0 2019-5月4日
     * @since v1.0
     */
    class HelpMethod {

        final private String argname;

        final private Options options;

        final private String help;

        public HelpMethod(String argname, Options options, String help) {
            hasText(argname, "Argname must not be empty");
            notNull(options, "options must not be null");
            hasText(argname, "Arg help must not be empty");
            this.argname = argname;
            this.options = options;
            this.help = help;
        }

        public String getArgname() {
            return argname;
        }

        public Options getOptions() {
            return options;
        }

        public String getHelp() {
            return help;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((argname == null) ? 0 : argname.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            HelpMethod other = (HelpMethod) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (argname == null) {
                if (other.argname != null)
                    return false;
            } else if (!argname.equals(other.argname))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "HelpMethod [argname=" + argname + ", options=" + options + ", help=" + help + "]";
        }

        private DefaultBuiltInCommand getOuterType() {
            return DefaultBuiltInCommand.this;
        }

    }

}