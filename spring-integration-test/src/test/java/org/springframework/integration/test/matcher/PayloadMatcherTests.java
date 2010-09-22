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
package org.springframework.integration.test.matcher;

import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

/**
 * @author Alex Peters
 * @author Iwein Fuld
 */
public class PayloadMatcherTests {

	static final BigDecimal ANY_PAYLOAD = new BigDecimal("1.123");

	Message<BigDecimal> message = MessageBuilder.withPayload(ANY_PAYLOAD).build();

	@Test
	public void hasPayload_withEqualValue_matches() throws Exception {
		assertThat(message, hasPayload(new BigDecimal("1.123")));
	}

	@Test
	public void hasPayload_withNotEqualValue_notMatching() throws Exception {
		assertThat(message, not(hasPayload(new BigDecimal("456"))));
	}

	@Test
	public void hasPayload_withMatcher_matches() throws Exception {
		assertThat(message, 
				hasPayload(is(BigDecimal.class)));
		assertThat(message, hasPayload(notNullValue()));
	}

	@Test
	public void hasPayload_withNotMatchingMatcher_notMatching()
			throws Exception {
		assertThat(message, not((hasPayload(is(String.class)))));
	}
	
	@Test
	public void readableException() throws Exception {
		try {
			assertThat(message, hasPayload(new BigDecimal("1")));
		} catch(AssertionError ae){
			assertTrue(ae.getMessage().contains("Expected: a Message with payload: "));
		}
	}
}
