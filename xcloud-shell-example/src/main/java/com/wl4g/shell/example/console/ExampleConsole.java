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
package com.wl4g.shell.example.console;

import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.wl4g.shell.common.annotation.ShellMethod;
import com.wl4g.shell.common.annotation.ShellMethod.InterruptType;
import com.wl4g.shell.common.annotation.ShellOption;
import com.wl4g.shell.core.handler.ProgressShellContext;
import com.wl4g.shell.core.handler.ProgressShellContext.UserShellContextBinders;
import com.wl4g.shell.core.handler.SimpleShellContext;
import com.wl4g.shell.example.console.args.MixedArgument;
import com.wl4g.shell.example.console.args.SumArgument;
import com.wl4g.shell.example.service.ExampleService;
import com.wl4g.shell.springboot.annotation.ShellComponent;

/**
 * Example console.</br>
 * Note: for the shell program to take effect, you must inject {@link Component}
 * or {@link Service} or {@link Bean} into the spring
 * {@link ApplicationContext}.
 * 
 * @author Wangl.sir &lt;Wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0.0 2019-04-21
 * @since
 */
@Component
@ShellComponent
public class ExampleConsole {
    final private static String GROUP_NAME = "Example commands";
    final private static Logger log = LoggerFactory.getLogger(ExampleConsole.class);

    @Autowired
    private ExampleService exampleService;

    // --------------- Testing for simple injection. ----------------

    /**
     * For example: $> testSimple -a 1 -b 123
     */
    @ShellMethod(keys = "testSimple", group = GROUP_NAME, help = "A simple shell summation method")
    public void testSimpleSum(SumArgument arg) {
        exampleService.add(arg);
    }

    /**
     * For example: $> testPrint -a 1 -b 123
     */
    @ShellMethod(keys = "testPrint", group = GROUP_NAME, help = "A simple shell summation method, the calculation process will be printed")
    public void testPrint(SimpleShellContext context, @ShellOption(opt = "a", lopt = "add1", help = "Add number") int a,
            @ShellOption(opt = "b", lopt = "add2", help = "Added number", defaultValue = "1") int b) {
        context.printf(exampleService.add(new SumArgument(a, b)).toString());
        context.completed();
    }

    /**
     * For example: $> testInjectArgs1 -s x3,x4 -l 1,2 -m a1=b1,a2=b2
     */
    @ShellMethod(keys = "testInjectArgs1", group = GROUP_NAME, help = "A simple shell printing method, will inject set/list/map various collection type parameters")
    public void testInjectArgs1(SimpleShellContext context,
            @ShellOption(opt = "s", lopt = "set", help = "Set<String> type argument field") Set<String> set,
            @ShellOption(opt = "l", lopt = "list", help = "List<Integer> type argument field") List<Integer> list,
            @ShellOption(opt = "m", lopt = "map", help = "Map<String, String> type argument field") List<Integer> map) {
        context.printf(format("输入参数: set => {}, list => {}, map => {}", set, list, map));
        context.completed();
    }

    /**
     * For example: $> testInjectArgs2 -l x1,x2 -m a1=b1,a2=b2 -p
     * aa1=bb1,aa2=bb2 -s x3,x4 -e false
     */
    @ShellMethod(keys = "testInjectArgs2", group = GROUP_NAME, help = "A simple shell printing method will inject complex + collection type parameters")
    public void testInjectArgs2(MixedArgument arg, SimpleShellContext context) {
        context.printf(format("输入参数: => {}", arg.toString()));
        context.completed();
    }

    // --------------- Testing for ShellContext. ----------------

    /**
     * For example: $> testAsyncTask -n 20
     */
    @ShellMethod(keys = "testAsyncTask", group = GROUP_NAME, help = "This is a shell method for printing logs asynchronously.(Not support interrupt)")
    public void testAsyncTask(
            @ShellOption(opt = "n", lopt = "num", required = false, defaultValue = "5", help = "Number of printed messages") int num,
            SimpleShellContext context) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                context.printf("TestAsyncTask starting ...");
                for (int i = 1; i <= num; i++) {
                    String message = "This is the " + i + "th output of TestAsyncTask ...";
                    log.info(message);

                    // Print to client console
                    context.printf(message);

                    try {
                        Thread.sleep(200L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                context.printf("TestAsyncTask finished!");
            } finally {
                // *** Note: Don't forget to execute it, or the client console
                // will pause until it timesout.
                context.completed();
            }
        });
    }

    /**
     * For example: $> testProgressTask -n 20
     */
    @ShellMethod(keys = "testProgressTask", group = GROUP_NAME, interruptible = InterruptType.ALLOW, help = "This is a shell method for printing logs asynchronously.(Support interrupt)")
    public void testProgressTask(
            @ShellOption(opt = "n", lopt = "num", required = false, defaultValue = "5", help = "Number of printed messages") int num,
            ProgressShellContext context) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                context.printf("TestProgressTask starting ...", 0.05f);
                for (int i = 1; !context.isInterrupted() && i <= num; i++) {
                    String message = "This is the " + i + "th output of TestProgressTask ...";
                    log.info(message);

                    // Print to client console
                    context.printf(message, num, i);

                    try {
                        Thread.sleep(200L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                // *** Note: Don't forget to execute it, or the client console
                // will pause until it timesout.
                context.completed("TestProgressTask finished!");
            }
        });
    }

    /**
     * For example: $> testBindContextTask -n 20
     */
    @ShellMethod(keys = "testBindContextTask", group = GROUP_NAME, interruptible = InterruptType.ALLOW, help = "This is a simple shell method for printing logs synchronously.(Support interrupt)")
    public void testBindContextTask(
            @ShellOption(opt = "n", lopt = "num", required = false, defaultValue = "5", help = "Number of printed messages") int num,
            @ShellOption(opt = "s", lopt = "sleep", required = false, defaultValue = "100", help = "Print message delay(ms)") long sleep,
            ProgressShellContext context) {

        // For testing the customization binding context
        UserShellContextBinders.bind(context);

        // Call real execution
        doTask3(num, sleep);
    }

    private void doTask3(int num, long sleep) {
        ProgressShellContext context = UserShellContextBinders.get();
        try {
            context.printf("TestBindContextTask starting ...", 0.05f);
            for (int i = 1; !context.isInterrupted() && i <= num; i++) {
                String message = "This is the " + i + "th output of testBindContextTask ...";
                log.info(message);

                // Print to client console
                context.printf(message, num, i);

                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            // *** Note: Don't forget to execute it, or the client console
            // will pause until it timesout.
            context.completed("TestBindContextTask finished!");
        }
    }

    // --------------- Testing for States switching. ----------------

    /**
     * For example: $> testError
     */
    @ShellMethod(keys = "testError", group = GROUP_NAME, help = "This is a test execution exception command methods")
    public void testError(SimpleShellContext context) {
        context.printf("Test error task starting ...");
        throw new IllegalStateException("This is a deliberate error!");
    }

    // --------------- Testing for ACL. ----------------

    /**
     * For example: $> testOnlyAuth
     */
    @ShellMethod(keys = "testOnlyAuth", group = GROUP_NAME, help = "This is a test execution ACL command methods")
    public void testOnlyAuth(SimpleShellContext context) {
        context.printf("testOnlyAuth starting ...");
        context.completed();
    }

    /**
     * For example: $> testMustAcl
     */
    @ShellMethod(keys = "testMustAcl", group = GROUP_NAME, permissions = "administrator", help = "This is a test execution ACL command methods")
    public void testMustAcl(SimpleShellContext context) {
        context.printf("testMustAcl starting ...");
    }

}