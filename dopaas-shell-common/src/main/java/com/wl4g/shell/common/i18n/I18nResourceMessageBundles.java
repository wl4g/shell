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

import static com.wl4g.infra.common.collection.CollectionUtils2.safeSet;
import static com.wl4g.infra.common.lang.Assert2.notNull;
import static com.wl4g.infra.common.log.SmartLoggerFactory.getLogger;
import static java.lang.System.getProperty;
import static java.util.Locale.US;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.Locale;
import java.util.Properties;

import com.wl4g.infra.common.log.SmartLogger;
import com.wl4g.infra.common.resource.StreamResource;
import com.wl4g.infra.common.resource.resolver.ClassPathResourcePatternResolver;

/**
 * {@link I18nResourceMessageBundles}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-06-30 v1.0.0
 */
public class I18nResourceMessageBundles {
    private static final SmartLogger log = getLogger(I18nResourceMessageBundles.class);
    private static final String LOCAL_LANG = getProperty("lang", Locale.getDefault().toString());
    private static final Properties mergedResource = new Properties();

    static {
        try {
            ClassPathResourcePatternResolver resolver = new ClassPathResourcePatternResolver();
            String classpath = I18nResourceMessageBundles.class.getName()
                    .replace(I18nResourceMessageBundles.class.getSimpleName(), "").replace(".", "/");

            // load default resources.
            Properties defaultResource = new Properties();
            String defaultPattern = "classpath*:/".concat(classpath).concat("messages.properties");
            StreamResource r1 = safeSet(resolver.getResources(defaultPattern)).stream().findFirst().orElse(null);
            notNull(r1, "Can't find i18n messages default resource of pattern: %s", defaultPattern);
            defaultResource.load(r1.getInputStream());

            // load current OS language resources.
            Properties langResource = new Properties();
            if (!equalsIgnoreCase(LOCAL_LANG, US.toString())) {
                String pattern = "classpath*:/".concat(classpath).concat("messages_").concat(LOCAL_LANG).concat(".properties");
                StreamResource r2 = safeSet(resolver.getResources(pattern)).stream().findFirst().orElse(null);
                if (nonNull(r2)) {
                    langResource.load(r2.getInputStream());
                } else {
                    log.warn("Cannot find i18n messages locale resource of pattern: {}, The default i18n resource will be used!",
                            pattern);
                }
            }

            // Merge resources.
            mergedResource.putAll(defaultResource);
            mergedResource.putAll(langResource);

            if (mergedResource.isEmpty()) {
                log.warn("Cannot to use i18n resources because it is empty after merged!");
            }
        } catch (Exception e) {
            log.error("", e);
            throw new IllegalStateException(e);
        }
    }

    public static String getMessage(String key, Object... args) {
        String msg = (String) mergedResource.getOrDefault(key, key);
        return String.format(msg, args);
    }

}