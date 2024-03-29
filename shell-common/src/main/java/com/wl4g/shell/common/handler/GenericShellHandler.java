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
package com.wl4g.shell.common.handler;

import static com.wl4g.infra.common.lang.Assert2.hasLength;
import static com.wl4g.infra.common.lang.Assert2.hasText;
import static com.wl4g.infra.common.lang.Assert2.isTrue;
import static com.wl4g.infra.common.lang.Assert2.notEmpty;
import static com.wl4g.infra.common.lang.Assert2.notNull;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.lang.Exceptions.getRootCauses;
import static com.wl4g.infra.common.lang.Exceptions.getRootCausesString;
import static com.wl4g.infra.common.lang.SystemUtils2.LOCAL_PROCESS_ID;
import static com.wl4g.infra.common.reflect.ReflectionUtils2.doFullWithFields;
import static com.wl4g.infra.common.reflect.ReflectionUtils2.isGenericModifier;
import static com.wl4g.shell.common.i18n.I18nResourceMessageBundles.getMessage;
import static com.wl4g.shell.common.utils.ShellUtils.instantiateWithInitOptionValue;
import static java.lang.String.format;
import static java.lang.System.err;
import static java.lang.System.getProperty;
import static java.lang.System.out;
import static java.util.Locale.US;
import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.CRC32;

import com.wl4g.infra.common.reflect.TypeUtils2;
import com.wl4g.shell.common.annotation.ShellOption;
import com.wl4g.shell.common.config.BaseShellProperties;
import com.wl4g.shell.common.exception.ShellException;
import com.wl4g.shell.common.registry.ShellHandlerRegistrar;
import com.wl4g.shell.common.registry.TargetMethodWrapper;
import com.wl4g.shell.common.registry.TargetMethodWrapper.TargetParameter;
import com.wl4g.shell.common.utils.LineUtils;

/**
 * Generic abstract shell component actuator handler.
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2019-4月14日
 * @since v1.0
 */
public abstract class GenericShellHandler implements ShellHandler {

    /**
     * Enable shell console debug.
     */
    public final static boolean DEBUG = getProperty("xdebug") != null;

    /**
     * Shell handler bean registry
     */
    protected final ShellHandlerRegistrar registrar;

    /**
     * Shell configuration
     */
    protected final BaseShellProperties config;

    public GenericShellHandler(BaseShellProperties config, ShellHandlerRegistrar registrar) {
        notNullOf(registrar, "registrar");
        notNullOf(config, "config");
        this.registrar = registrar;
        this.config = config;
    }

    @Override
    public Object process(String line) {
        if (isEmpty(line)) {
            return null;
        }
        try {
            // Resolving line commands.
            List<String> commands = resolveCommands(line);
            notNull(commands, "Console input commands must not be null");

            // Extract main argument option.
            String mainArg = commands.remove(0);

            // Check target method existing.
            isTrue(registrar.contains(mainArg), getMessage("label.command.notfount", mainArg));
            TargetMethodWrapper tm = registrar.getTargetMethods().get(mainArg);

            // Call prepare to resolve parameters.
            preHandleCommands(commands, tm);

            // Resolving method parameters.
            List<Object> parameters = resolveParameters(commands, tm);

            // Call before execution.
            beforeShellExecution(commands, tm, parameters);

            // Invoking
            Object output = doInvoke(line, commands, mainArg, tm, parameters);

            // Call after execution.
            afterShellExecution(output);

            return output;
        } catch (Exception e) {
            throw new ShellException(getRootCauses(e));
        }
    }

    /**
     * Invocation execution command method.
     * 
     * @param line
     *            input command line string.
     * @param commands
     *            resolved input commands
     * @param mainArg
     *            execution main command name.
     * @param tm
     *            Execution target method wrapper.
     * @param args
     *            Execution target method args.
     * @return Invoked result output.
     * @throws Exception
     *             Trigger when exception occurs
     */
    protected Object doInvoke(String line, List<String> commands, String mainArg, TargetMethodWrapper tm, List<Object> args)
            throws Exception {
        return tm.getMethod().invoke(tm.getTarget(), args.toArray());
    }

    /**
     * Resolve arguments to method parameters
     * 
     * @param commands
     *            Input commands
     * @param tm
     *            Execution target method wrapper.
     * @return resolved commands parameters array.
     * @throws IllegalArgumentException
     *             Trigger when invoking exception occurs.
     * @throws IllegalAccessException
     *             Trigger when invoking exception occurs.
     * @throws InstantiationException
     *             Trigger when invoking exception occurs.
     */
    protected List<Object> resolveParameters(List<String> commands, TargetMethodWrapper tm)
            throws IllegalArgumentException, IllegalAccessException, InstantiationException {
        notNull(tm, "Error, Should targetMethodWrapper not be null?");

        /*
         * Commands to javaBean map and validate protected. (javaBean.fieldName
         * or params.index(native type))->value
         */
        final Map<String, String> beanMap = new HashMap<>();
        if (commands != null && !commands.isEmpty()) {
            for (int i = 0; i < commands.size() - 1; i++) {
                if (i % 2 == 0) {
                    // Input opt
                    String argname = commands.get(i);
                    hasText(argname, format("Unable to get parameter name, i:%s", i));
                    // Value(May be empty) See:[MARK3]
                    String value = commands.get(i + 1);

                    // Convert and save
                    beanMap.put(convertIfNecessary(argname, tm), value);
                }
            }
        }

        // Method arguments.
        final List<Object> args = new ArrayList<>();
        for (TargetParameter parameter : tm.getParameters()) {
            // [MARK1]: To native parameter, See:[TargetParameter.MARK7]
            if (parameter.simpleType()) {
                ShellOption shOpt = parameter.getShellOption();
                // Matching argument value
                Optional<Entry<String, String>> val = beanMap.entrySet().stream()
                        .filter(arg -> equalsAny(arg.getKey(), shOpt.opt(), shOpt.lopt())).findFirst();

                // Default value
                String value = shOpt.defaultValue();
                if (val.isPresent()) {
                    value = val.get().getValue();
                }

                // Validate argument(if required)
                if (shOpt.required() && !beanMap.containsKey(shOpt.opt()) && !beanMap.containsKey(shOpt.lopt())
                        && isBlank(shOpt.defaultValue())) {
                    throw new IllegalArgumentException(format("option: '-%s', '--%s' is required", shOpt.opt(), shOpt.lopt()));
                }
                args.add(TypeUtils2.instantiate(value, parameter.getParamType()));
            }
            // Convert javaBean parameter.
            // See: TargetMethodWrapper#initialize
            else {
                Object paramBean = parameter.getParamType().newInstance();

                // Recursive full traversal De-serialization.
                doFullWithFields(paramBean, field -> {
                    // [MARK4],See:[ShellUtils.MARK0][TargetParameter.MARK1]
                    return isGenericModifier(field.getModifiers());
                }, (field, objOfField) -> {
                    if (Objects.isNull(objOfField)) {
                        objOfField = TypeUtils2.instantiate(null, field.getType());
                    }

                    ShellOption shOpt = field.getDeclaredAnnotation(ShellOption.class);
                    notNull(shOpt, "Error, Should shellOption not be null?");
                    Object value = beanMap.get(field.getName());
                    if (Objects.isNull(value)) {
                        value = shOpt.defaultValue();
                    }
                    // Validate argument(if required)
                    if (shOpt.required() && !beanMap.containsKey(field.getName()) && isBlank(shOpt.defaultValue())) {
                        throw new IllegalArgumentException(
                                format("option: '-%s', '--%s' is required", shOpt.opt(), shOpt.lopt()));
                    }

                    value = instantiateWithInitOptionValue((String) value, field.getType());
                    field.setAccessible(true);
                    field.set(objOfField, value);
                });
                args.add(paramBean);
            }
        }

        return args;
    }

    /**
     * Prepared resolving parameters before handle.
     * 
     * @param commands
     *            input commands array.
     * @param tm
     *            Execution target method wrapper.
     */
    protected void preHandleCommands(List<String> commands, TargetMethodWrapper tm) {

    }

    /**
     * It the resolving parameters before handle.
     * 
     * @param commands
     *            input commands array.
     * @param tm
     *            Execution target method wrapper.
     * @param parameters
     *            Execution mehtod args.
     */
    protected void beforeShellExecution(List<String> commands, TargetMethodWrapper tm, List<Object> parameters) {
    }

    /**
     * Post invocation standard output message.
     * 
     * @param output
     *            Execution result output.
     * @throws Exception
     *             Trigger when exception occurs
     */
    protected void afterShellExecution(Object output) throws Exception {
    }

    /**
     * Convert argument to java bean actual param field name
     * 
     * @param argname
     *            main command name.
     * @param tm
     *            target command execution method wrapper.
     * @return java bean actual param field name or index(if native type)
     */
    protected String convertIfNecessary(String argname, TargetMethodWrapper tm) {
        return tm.getSureParamName(LineUtils.clean(argname));
    }

    /**
     * Resolve source commands
     * 
     * @param line
     *            input command line.
     * @return resolved commands.
     */
    protected List<String> resolveCommands(String line) {
        List<String> commands = LineUtils.parse(line);
        notEmpty(commands, "Commands must not be empty");
        return commands;
    }

    /**
     * Ensure resolve server listen port.
     * 
     * @param appName
     *            application name.
     * @return final determine server listen port.
     */
    protected int ensureDetermineServPort(String appName) {
        hasLength(appName, "appName must not be empty");
        String origin = trimToEmpty(appName).toUpperCase(US);

        CRC32 crc32 = new CRC32();
        crc32.update(origin.getBytes(Charset.forName("UTF-8")));
        int mod = config.getEndPort() - config.getBeginPort();
        int servport = (int) (config.getBeginPort() + (crc32.getValue() % mod & (mod - 1)));

        if (DEBUG) {
            out.println(format("Shell servports (%s ~ %s), origin(%s), sign(%s), determine(%s)", config.getBeginPort(),
                    config.getEndPort(), origin, crc32.getValue(), servport));
        }
        return servport;
    }

    // --- Function's ---

    /**
     * Print errors info.
     * 
     * @param abnormal
     *            abnormal string
     * @param th
     *            execution exception
     */
    protected void printError(String abnormal, Throwable th) {
        if (DEBUG) {
            th.printStackTrace();
        } else {
            err.println(format("%s %s", abnormal, getRootCausesString(th, true)));
        }
    }

    /**
     * Print debug info.
     * 
     * @param msg
     *            message string.
     */
    protected void printDebug(String msg) {
        if (DEBUG) {
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            out.println(format("%s %s DEBUG - %s", date, LOCAL_PROCESS_ID, msg));
        }
    }

}