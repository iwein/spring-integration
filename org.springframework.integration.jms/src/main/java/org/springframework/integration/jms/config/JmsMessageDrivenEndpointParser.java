/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.jms.config;

import javax.jms.Session;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;message-driven-channel-adapter&gt; element and the
 * &lt;inbound-gateway&gt; element of the 'jms' namespace.
 * 
 * @author Mark Fisher
 */
public class JmsMessageDrivenEndpointParser extends AbstractSingleBeanDefinitionParser {

	private static String[] containerAttributes = new String[] {
		JmsAdapterParserUtils.CONNECTION_FACTORY_PROPERTY,
		JmsAdapterParserUtils.DESTINATION_ATTRIBUTE,
		JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE,
		"transaction-manager", "pub-sub-domain",
		"concurrent-consumers", "max-concurrent-consumers",
		"max-messages-per-task", "idle-task-execution-limit"
	};


	private final boolean expectReply;


	public JmsMessageDrivenEndpointParser(boolean expectReply) {
		this.expectReply = expectReply;
	}


	@Override
	protected Class<?> getBeanClass(Element element) {
		return JmsMessageDrivenEndpoint.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String containerBeanName = this.parseMessageListenerContainer(element, parserContext);
		String listenerBeanName = this.parseMessageListener(element, parserContext);
		builder.addConstructorArgReference(containerBeanName);
		builder.addConstructorArgReference(listenerBeanName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
	}

	private String parseMessageListenerContainer(Element element, ParserContext parserContext) {
		if (element.hasAttribute("container")) {
			for (String containerAttribute : containerAttributes) {
				Assert.isTrue(!element.hasAttribute(containerAttribute), "The '" + containerAttribute +
						"' attribute should not be provided when specifying a 'container' reference.");
			}
			return element.getAttribute("container");
		}
		// otherwise, we build a DefaultMessageListenerContainer instance
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DefaultMessageListenerContainer.class);
		String destination = element.getAttribute(JmsAdapterParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE);
		Assert.isTrue(StringUtils.hasText(destination) ^ StringUtils.hasText(destinationName),
				"Exactly one of '" + JmsAdapterParserUtils.DESTINATION_ATTRIBUTE +
					"' or '" + JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE + "' is required.");
		builder.addPropertyReference(JmsAdapterParserUtils.CONNECTION_FACTORY_PROPERTY,
				JmsAdapterParserUtils.determineConnectionFactoryBeanName(element));
		if (StringUtils.hasText(destination)) {
			builder.addPropertyReference(JmsAdapterParserUtils.DESTINATION_PROPERTY, destination);
		}
		else {
			builder.addPropertyValue(JmsAdapterParserUtils.DESTINATION_NAME_PROPERTY, destinationName);
		}
		Integer acknowledgeMode = JmsAdapterParserUtils.parseAcknowledgeMode(element);
		if (acknowledgeMode != null) {
			if (acknowledgeMode.intValue() == Session.SESSION_TRANSACTED) {
				builder.addPropertyValue("sessionTransacted", Boolean.TRUE);
			}
			else {
				builder.addPropertyValue("sessionAcknowledgeMode", acknowledgeMode);
			}
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "transaction-manager");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "pub-sub-domain");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-messages-per-task");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-task-execution-limit");
		builder.addPropertyValue("autoStartup", false);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	private String parseMessageListener(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ChannelPublishingJmsMessageListener.class);
		builder.addPropertyValue("expectReply", this.expectReply);
		if (this.expectReply) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-request-payload");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-reply-payload");
		}
		else {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "channel", "requestChannel");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout", "requestTimeout");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload", "extractRequestPayload");
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "header-mapper");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}