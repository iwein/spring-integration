/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

/**
 * A base class for {@link Transformer} implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractTransformer<OUT> implements Transformer<OUT> {

	@SuppressWarnings("unchecked")
	public final<IN> Message<OUT> transform(Message<IN> message) {
		try {
			Object result = this.doTransform(message);
			if (result == null) {
				return null;
			}
			return (result instanceof Message) ? (Message<OUT>) result
					: MessageBuilder.withPayload((OUT)result).copyHeaders(message.getHeaders()).build();
		}
		catch (MessageTransformationException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MessageTransformationException(message, "failed to transform message", e);
		}
	}

	/**
	 * Subclasses must implement this method to provide the transformation
	 * logic. If the return value is itself a Message, it will be used as the
	 * result. Otherwise, any non-null return value will be used as the payload
	 * of the result Message.
	 */
	protected abstract<IN> Object doTransform(Message<IN> message) throws Exception;

}
