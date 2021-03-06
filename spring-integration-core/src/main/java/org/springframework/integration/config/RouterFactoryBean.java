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

package org.springframework.integration.config;

import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.router.AbstractChannelNameResolvingMessageRouter;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.support.channel.ChannelResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Router.
 * 
 * @author Mark Fisher
 * @author Jonas Partner
 */
public class RouterFactoryBean extends AbstractMessageHandlerFactoryBean {

	private volatile ChannelResolver channelResolver;

	private volatile MessageChannel defaultOutputChannel;

	private volatile Long timeout;

	private volatile Boolean resolutionRequired;

	private volatile Boolean ignoreChannelNameResolutionFailures;

	private volatile Boolean applySequence;

	private volatile Boolean ignoreSendFailures;


	public void setChannelResolver(ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public void setDefaultOutputChannel(MessageChannel defaultOutputChannel) {
		this.defaultOutputChannel = defaultOutputChannel;
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}

	public void setResolutionRequired(Boolean resolutionRequired) {
		this.resolutionRequired = resolutionRequired;
	}

	public void setIgnoreChannelNameResolutionFailures(Boolean ignoreChannelNameResolutionFailures) {
		this.ignoreChannelNameResolutionFailures = ignoreChannelNameResolutionFailures;
	}

	public void setApplySequence(Boolean applySequence) {
		this.applySequence = applySequence;
	}

	public void setIgnoreSendFailures(Boolean ignoreSendFailures) {
		this.ignoreSendFailures = ignoreSendFailures;
	}

	@Override
	MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		Assert.notNull(targetObject, "target object must not be null");
		AbstractMessageRouter router = this.createRouter(targetObject, targetMethodName);
		return this.configureRouter(router);
	}

	@Override
	MessageHandler createExpressionEvaluatingHandler(String expression) {
		return this.configureRouter(new ExpressionEvaluatingRouter(expression));
	}

	private AbstractMessageRouter createRouter(Object targetObject, String targetMethodName) {
		if (targetObject instanceof AbstractMessageRouter) {
			Assert.isTrue(!StringUtils.hasText(targetMethodName),
					"target method should not be provided when the target " +
					"object is an implementation of AbstractMessageRouter");
			return (AbstractMessageRouter) targetObject;
		}
		MethodInvokingRouter router = (StringUtils.hasText(targetMethodName))
				? new MethodInvokingRouter(targetObject, targetMethodName)
				: new MethodInvokingRouter(targetObject);
		return router;
	}

	private AbstractMessageRouter configureRouter(AbstractMessageRouter router) {
		if (this.channelResolver != null &&
				router instanceof AbstractChannelNameResolvingMessageRouter) {
			((AbstractChannelNameResolvingMessageRouter) router).setChannelResolver(this.channelResolver);
		}
		if (this.defaultOutputChannel != null) {
			router.setDefaultOutputChannel(this.defaultOutputChannel);
		}
		if (this.timeout != null) {
			router.setTimeout(timeout.longValue());
		}
		if (this.ignoreChannelNameResolutionFailures != null) {
			Assert.isTrue(router instanceof AbstractChannelNameResolvingMessageRouter, 
					"The 'ignoreChannelNameResolutionFailures' property can only be set on routers that extend "
					+ AbstractChannelNameResolvingMessageRouter.class.getName());
			((AbstractChannelNameResolvingMessageRouter) router).setIgnoreChannelNameResolutionFailures(ignoreChannelNameResolutionFailures);
		}
		if (this.applySequence != null) {
			router.setApplySequence(this.applySequence);
		}
		if (this.ignoreSendFailures != null) {
			router.setIgnoreSendFailures(this.ignoreSendFailures);
		}
		if (this.resolutionRequired != null) {
			router.setResolutionRequired(this.resolutionRequired);
		}
		return router;
	}

}
