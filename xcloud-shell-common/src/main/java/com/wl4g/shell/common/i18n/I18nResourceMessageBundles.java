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
package com.wl4g.shell.common.i18n;

import static com.wl4g.component.common.collection.CollectionUtils2.safeSet;
import static com.wl4g.component.common.lang.Assert2.notNull;
import static com.wl4g.component.common.log.SmartLoggerFactory.getLogger;
import static java.lang.System.getProperty;

import java.util.Locale;
import java.util.Properties;

import com.wl4g.component.common.log.SmartLogger;
import com.wl4g.component.common.resource.StreamResource;
import com.wl4g.component.common.resource.resolver.ClassPathResourcePatternResolver;

/**
 * {@link I18nResourceMessageBundles}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 * @see v1.0.0
 */
public class I18nResourceMessageBundles {
    private static final SmartLogger log = getLogger(I18nResourceMessageBundles.class);
    private static final String LANG = getProperty("lang", Locale.getDefault().toString());
    private static final Properties localeResource = new Properties();

    static {
        try {
            ClassPathResourcePatternResolver resolver = new ClassPathResourcePatternResolver();
            String classpath = I18nResourceMessageBundles.class.getName()
                    .replace(I18nResourceMessageBundles.class.getSimpleName(), "").replace(".", "/");

            // Default resources.
            Properties defaultResource = new Properties();
            String defaultPattern = "classpath*:/".concat(classpath).concat("messages.properties");
            StreamResource r1 = safeSet(resolver.getResources(defaultPattern)).stream().findFirst().orElse(null);
            notNull(r1, "Not found i18n messages resource: %s", r1);
            defaultResource.load(r1.getInputStream());

            // Configuration Language resources.
            Properties langResource = new Properties();
            if (!LANG.equalsIgnoreCase(Locale.US.toString())) {
                String langPattern = "classpath*:/".concat(classpath).concat("messages_").concat(LANG).concat(".properties");
                StreamResource r2 = safeSet(resolver.getResources(langPattern)).stream().findFirst().orElse(null);
                notNull(r1, "Not found i18n messages resource: %s", r2);
                langResource.load(r2.getInputStream());
            }

            // Merge resources.
            localeResource.putAll(defaultResource);
            localeResource.putAll(langResource);

            if (localeResource.isEmpty()) {
                log.warn("Unable initialzation read i18n resources. scan pattern: {}", defaultPattern);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String getMessage(String key, Object... args) {
        String msg = (String) localeResource.getOrDefault(key, key);
        return String.format(msg, args);
    }

}