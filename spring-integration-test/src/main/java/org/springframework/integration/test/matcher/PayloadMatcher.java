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

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.springframework.integration.Message;

/**
 * <h2>Is the payload of a {@link Message} equal to a given value or is matching
 * a given matcher?</h2>
 * 
 * <h3>
 * A Junit example using {@link Assert#assertThat(Object, Matcher)} could look
 * like this to test a payload value:</h3>
 * 
 * <pre>
 * ANY_PAYLOAD = new BigDecimal(&quot;1.123&quot;);
 * Message&lt;BigDecimal message = MessageBuilder.withPayload(ANY_PAYLOAD).build();
 * assertThat(message, hasPayload(ANY_PAYLOjAD));
 * </pre>
 * 
 * <h3>
 * An example using {@link Assert#assertThat(Object, Matcher)} delegating to
 * another {@link Matcher}.</h3>
 * 
 * <pre>
 * ANY_PAYLOAD = new BigDecimal(&quot;1.123&quot;);
 * assertThat(message, PayloadMatcher.hasPayload(is(BigDecimal.class)));
 * assertThat(message, PayloadMatcher.hasPayload(notNullValue()));
 * assertThat(message, not((PayloadMatcher.hasPayload(is(String.class))))); *
 * </pre>
 * 
 * 
 * 
 * @author Alex Peters
 * @author Iwein Fuld
 * 
 */
public class PayloadMatcher<T> extends TypeSafeMatcher<Message<T>> {

	private final Matcher<? extends T> matcher;

	/**
	 * @param matcher
	 */
	PayloadMatcher(Matcher<? extends T> matcher) {
		super();
		this.matcher = matcher;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean matchesSafely(Message message) {
		return matcher.matches(message.getPayload());
	}

	/**
	 * {@inheritDoc}
	 */
	//@Override
	public void describeTo(Description description) {
		description.appendText("a Message with payload: ").appendDescriptionOf(matcher);

	}

	@Factory
	public static <T> Matcher<Message<T>> hasPayload(T payload) {
		return new PayloadMatcher(IsEqual.equalTo(payload));
	}

	@Factory
	public static <T> Matcher<Message> hasPayload(Matcher<T> payloadMatcher) {
		return new PayloadMatcher(payloadMatcher);
	}
}