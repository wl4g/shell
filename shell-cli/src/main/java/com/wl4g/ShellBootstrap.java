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
package com.wl4g;

import com.wl4g.shell.cli.RunnerBuilder;
import com.wl4g.shell.cli.handler.InteractiveClientShellHandler;

/**
 * Shell bootstrap program for client.
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2019-5月2日
 * @since v1.0
 */
public class ShellBootstrap {

	/**
	 * For examples:
	 * 
	 * <pre>
	 * [Way1]:
	 * java -Dservpoint=127.0.0.1:60103 -Dprompt=my-shell -Dtimeout=5000 -jar shell-cli-master-executable.jar
	 * [Way2]:
	 * java -Dservname=shell-example -Dprompt=my-shell -Dtimeout=5000 -jar shell-cli-master-executable.jar
	 * </pre>
	 * 
	 * @param args
	 * @see <a href=
	 *      "https://github.com/wl4g/shell/blob/master/README_CN.md">Quick
	 *      start for Gitee</a>
	 * @see <a href=
	 *      "https://github.com/wl4g/shell/blob/master/README.md">Quick
	 *      start for Github</a>
	 */
	public static void main(String[] args) {
		RunnerBuilder.builder().provider(InteractiveClientShellHandler.class).build().run(args);
	}

}