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
package com.wl4g.shell.common.cli;

import static com.wl4g.component.common.lang.Assert2.hasText;
import static com.wl4g.shell.common.utils.LineUtils.clean;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.wl4g.shell.common.annotation.ShellOption;

/**
 * Internal built-in commands
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2019-5月4日
 * @since v1.0
 */
public abstract class BuiltInCommand {

    public final static String CMD_LOGIN = "login";
    public final static String CMD_LO = "lo";

    public final static String CMD_HELP = "help";
    public final static String CMD_HE = "he";

    public final static String CMD_EXIT = "exit";
    public final static String CMD_QUIT = "quit";

    public final static String CMD_QU = "qu";
    public final static String CMD_EX = "ex";

    public final static String CMD_HISTORY = "history";
    public final static String CMD_HIS = "his";

    public final static String CMD_CLEAR = "clear";
    public final static String CMD_CLS = "cls";

    public final static String CMD_STACKTRACE = "stacktrace";
    public final static String CMD_ST = "st";

    final private static List<String> CMDS = new ArrayList<>();

    static {
        Field[] fields = BuiltInCommand.class.getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            if (startsWithAny(f.getName(), "INTERNAL")) {
                try {
                    CMDS.add((String) f.get(null));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Check contains defineKey whether it is naming conflict with built-in
     * commands
     * 
     * @param defineKey
     * @return
     */
    public final static boolean contains(String... defineKeys) {
        for (String key : defineKeys) {
            if (CMDS.contains(key) || CMDS.contains(clean(key))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get GNU long option
     * 
     * @param defineKey
     * @return
     */
    public final static String getGNULong(String defineKey) {
        hasText(defineKey, "defineKey must not be emtpy");
        return ShellOption.GNU_CMD_LONG + defineKey;
    }

    /**
     * Get GNU short option
     * 
     * @param defineKey
     * @return
     */
    public final static String getGNUShort(String defineKey) {
        hasText(defineKey, "defineKey must not be emtpy");
        return ShellOption.GNU_CMD_SHORT + defineKey;
    }

    /**
     * To internal command option all.
     * 
     * @param defineKey
     * @return
     */
    public final static String asCmdsString() {
        StringBuffer cmds = new StringBuffer();
        for (String cmd : CMDS) {
            cmds.append(cmd);
            cmds.append(", ");
        }
        return cmds.toString();
    }

}