/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.http.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the 'outbound-gateway' element of the http namespace.
 * 
 * @author Mark Fisher
 */
public class HttpOutboundGatewayParser extends AbstractConsumerEndpointParser {

	private static final String PACKAGE_PATH = "org.springframework.integration.http";


	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				PACKAGE_PATH + ".HttpRequestExecutingMessageHandler");
		builder.addConstructorArgValue(element.getAttribute("url"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "http-method");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converters");
		String headerMapper = element.getAttribute("header-mapper");
		String mappedRequestHeaders = element.getAttribute("mapped-request-headers");
		String mappedResponseHeaders = element.getAttribute("mapped-response-headers");
		if (StringUtils.hasText(headerMapper)) {
			if (StringUtils.hasText(mappedRequestHeaders) || StringUtils.hasText(mappedResponseHeaders)) {
				parserContext.getReaderContext().error("Neither 'mappped-request-headers' or 'mapped-response-headers' " +
						"attributes are allowed when a 'header-mapper' has been specified.", parserContext.extractSource(element));
				return null;
			}
			builder.addPropertyReference("headerMapper", headerMapper);
		}
		else if (StringUtils.hasText(mappedRequestHeaders) || StringUtils.hasText(mappedResponseHeaders)) {
			BeanDefinitionBuilder headerMapperBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.integration.http.DefaultHttpHeaderMapper");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(headerMapperBuilder, element, "mapped-request-headers", "outboundHeaderNames");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(headerMapperBuilder, element, "mapped-response-headers", "inboundHeaderNames");
			builder.addPropertyValue("headerMapper", headerMapperBuilder.getBeanDefinition());
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-request-payload", "extractPayload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expected-response-type");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		List<Element> uriVariableElements = DomUtils.getChildElementsByTagName(element, "uri-variable");
		if (!CollectionUtils.isEmpty(uriVariableElements)) {
			Map<String, String> uriVariableExpressions = new HashMap<String, String>();
			for (Element uriVariableElement : uriVariableElements) {
				String name = uriVariableElement.getAttribute("name");
				String expression = uriVariableElement.getAttribute("expression");
				uriVariableExpressions.put(name, expression);
			}
			builder.addPropertyValue("uriVariableExpressions", uriVariableExpressions);
		}
		return builder;
	}

}
