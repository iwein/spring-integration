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

/**
 * A base class for {@link Transformer} implementations that modify the payload
 * of a {@link Message}. If the return value is itself a Message, it will be
 * used as the result. Otherwise, the return value will be used as the payload
 * of the result Message.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public abstract class AbstractPayloadTransformer<IN, OUT> extends AbstractTransformer<OUT> {

	public final <T> OUT doTransform(Message<T> message) throws Exception {
		return this.transformPayload((IN) message.getPayload());
	}

	protected abstract OUT transformPayload(IN payload) throws Exception;

}
