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

package org.springframework.integration.transformer;

import org.springframework.integration.Message;

/**
 * Strategy interface for transforming a {@link Message}. The transformer can optionally be parametrized with the type
 * of the payload outgoing messages, in case the transformer doesn't operate on the payloads these
 * parameters should be ignored.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @param <OUT> the type of the payloads of outgoing messages
 */
public interface Transformer<OUT> {

	/**
	 * Transform given message and return the result of the transformation.
	 * @param <IN> optional, the type of the payloads of incoming message
	 */
	<IN> Message<OUT> transform(Message<IN> message);

}
