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

package org.springframework.integration.transformer;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Transformer that removes Message headers.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class HeaderFilter implements Transformer {

	private final String[] headersToRemove;


	public HeaderFilter(String... headersToRemove) {
		Assert.notEmpty(headersToRemove, "At least one header name to remove is required.");
		this.headersToRemove = headersToRemove;
	}


	public Message transform(Message message) {
		MessageBuilder<?> builder = MessageBuilder.fromMessage(message);
		for (String headerName : headersToRemove) {
			builder.removeHeader(headerName);
		}
		return builder.build();
	}

}
