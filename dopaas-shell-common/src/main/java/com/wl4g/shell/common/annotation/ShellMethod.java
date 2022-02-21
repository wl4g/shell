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
package com.wl4g.shell.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link ShellMethod}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2019-04-17
 * @since v1.0 v1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Documented
public @interface ShellMethod {

    /**
     * Command names definition.
     * e.g:
     * 
     * <pre>
     * $ &gt; mylist
     * </pre>
     * 
     * @return Command names definition.
     */
    String[] keys();

    /**
     * @return Command group name.
     */
    String group();

    /**
     * @return Whether to allow command line execution to be interrupted.
     */
    InterruptType interruptible() default InterruptType.NOT_ALLOW;

    /**
     * @return The list of roles required by the access control mechanism takes
     *         effect only when the global ACL is enabled.
     */
    String[] permissions() default {};

    /**
     * @return If lock is true, it means that concurrent execution lock is
     *         enabled (cluster is supported if redis exists in classpath).
     *         Default: true, that is, any client executing a command must wait
     *         for the last execution (possibly other clients) to end.
     */
    boolean lock() default true;

    /**
     * @return Command help description.
     */
    String help();

    public static enum InterruptType {
        ALLOW, NOT_ALLOW
    }

}