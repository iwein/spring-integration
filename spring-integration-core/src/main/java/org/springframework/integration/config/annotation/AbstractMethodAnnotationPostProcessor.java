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

package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.channel.ChannelResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for Method-level annotation post-processors.
 *
 * @author Mark Fisher
 */
public abstract class AbstractMethodAnnotationPostProcessor<T extends Annotation> implements MethodAnnotationPostProcessor<T> {

	private static final String INPUT_CHANNEL_ATTRIBUTE = "inputChannel";


	protected final BeanFactory beanFactory;

	protected final ChannelResolver channelResolver;


	public AbstractMethodAnnotationPostProcessor(ListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
	}


	public Object postProcess(Object bean, String beanName, Method method, T annotation) {
		MessageHandler handler = this.createHandler(bean, method, annotation);
		if (handler instanceof AbstractMessageHandler) {
			Order orderAnnotation = AnnotationUtils.findAnnotation(method, Order.class);
			if (orderAnnotation != null) {
				((AbstractMessageHandler) handler).setOrder(orderAnnotation.value());
			}
		}
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			handler = (MessageHandler) ((ConfigurableListableBeanFactory) beanFactory).initializeBean(handler, "_initHandlerFor_" + beanName);
		}
		AbstractEndpoint endpoint = this.createEndpoint(handler, annotation);
		if (endpoint != null) {
			return endpoint;
		}
		return handler;
	}

	protected boolean shouldCreateEndpoint(T annotation) {
		return (StringUtils.hasText((String) AnnotationUtils.getValue(annotation, INPUT_CHANNEL_ATTRIBUTE)));
	}

	private AbstractEndpoint createEndpoint(MessageHandler handler, T annotation) {
		AbstractEndpoint endpoint = null;
		String inputChannelName = (String) AnnotationUtils.getValue(annotation, INPUT_CHANNEL_ATTRIBUTE);
		if (StringUtils.hasText(inputChannelName)) {
			MessageChannel inputChannel = this.channelResolver.resolveChannelName(inputChannelName);
			Assert.notNull(inputChannel, "failed to resolve inputChannel '" + inputChannelName + "'");
			Assert.isTrue(inputChannel instanceof SubscribableChannel,
					"The input channel for an Annotation-based endpoint must be a SubscribableChannel.");
			endpoint = new EventDrivenConsumer((SubscribableChannel) inputChannel, handler);
			if (handler instanceof BeanFactoryAware) {
				((BeanFactoryAware) handler).setBeanFactory(this.beanFactory);
			}
		}
		return endpoint;
	}

	/**
	 * Subclasses must implement this method to create the MessageHandler.
	 */
	protected abstract MessageHandler createHandler(Object bean, Method method, T annotation);

}
