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
package com.wl4g.shell.common.signal;

import static com.wl4g.infra.common.lang.Assert2.notNullOf;

/**
 * Post confirm interrupt message
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2020-1月4日
 * @since v1.0
 */
public class AckInterruptSignal extends Signal {
    private static final long serialVersionUID = -8574315246731906685L;

    /**
     * Current confirmed state.
     */
    private final boolean confirm;

    public AckInterruptSignal(boolean confirm) {
        notNullOf(confirm, "confirm");
        this.confirm = confirm;
    }

    public boolean getConfirm() {
        return confirm;
    }

    @Override
    public String toString() {
        return super.toString().concat("[confirm=" + confirm + "]");
    }

}