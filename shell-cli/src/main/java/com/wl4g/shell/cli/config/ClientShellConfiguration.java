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
package com.wl4g.shell.cli.config;

import static com.wl4g.infra.common.lang.Assert2.*;

import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.wl4g.shell.common.config.BaseShellProperties;

/**
 * Shell properties configuration
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2019-5月1日
 * @since v1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "configuration")
public class ClientShellConfiguration extends BaseShellProperties {

	final private static long serialVersionUID = -24798955162679115L;

	final public static String DEFAULT_CONFIG = "default-config.xml";

	/**
	 * Listening server socket bind address
	 */
	@XmlElement
	private String server = "127.0.0.1";

	/**
	 * Banner strings.
	 */
	private String banner;

	public String getServer() {
		hasText(server, "server is emtpy, please check configure");
		return server;
	}

	public void setServer(String server) {
		hasText(server, "server is emtpy, please check configure");
		this.server = server;
	}

	public String getBanner() {
		return banner;
	}

	public void setBanner(String banner) {
		this.banner = banner;
	}

	public static ClientShellConfiguration create() {
		return create(ClientShellConfiguration.class.getClassLoader().getResource(DEFAULT_CONFIG));
	}

	public static ClientShellConfiguration create(URL url) {
		try {
			return Util.read(url, BaseShellProperties.class, ClientShellConfiguration.class);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Internal utility tools
	 * 
	 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
	 * @version v1.0 2019-5月9日
	 * @since v1.0
	 */
	static abstract class Util {

		@SuppressWarnings("unchecked")
		public static <T> T read(URL url, Class<?>... classes) throws Exception {
			try {
				url.openStream();
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format("File path: %s does not exist", url));
			}

			JAXBContext jaxb = JAXBContext.newInstance(classes);
			Unmarshaller unmarshaller = jaxb.createUnmarshaller();
			return (T) unmarshaller.unmarshal(url);
		}

	}

}