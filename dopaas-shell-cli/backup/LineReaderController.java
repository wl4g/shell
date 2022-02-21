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

import static com.wl4g.component.common.lang.Assert2.isTrueOf;
import static com.wl4g.component.common.lang.Assert2.notNullOf;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import org.jline.reader.LineReader;

/**
 * {@link LineReaderController}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-29 v1.0.0
 * 
 */
public class LineReaderController implements Runnable {

    private final LineReader lineReader;
    private final long timeout;

    private String prompt;
    private String stdin;

    public LineReaderController(LineReader lineReader, String prompt, long timeout) {
        this.lineReader = notNullOf(lineReader, "lineReader");
        isTrueOf(timeout >= 0, format("Shell exec timeout must >= 0, actual: %s", timeout));
        this.timeout = timeout;
        prompt(prompt);
    }

    public synchronized LineReaderController prompt(String prompt) {
        this.prompt = notNullOf(prompt, "prompt");
        return this;
    }

    @Override
    public synchronized void run() {
        while (true) {
            if (!isBlank(stdin)) {
                try {
                    wait(timeout);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            stdin = lineReader.readLine(prompt);
            notify();
        }
    }

    public synchronized String readStdin() {
        try {
            if (isBlank(stdin)) {
                wait();
            }
            notify();
            return stdin;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            stdin = null;
        }
    }

}
