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

package org.springframework.integration.mail.transformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.integration.transformer.Transformer;
import org.springframework.util.Assert;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for Transformers that convert from a JavaMail Message to a
 * Spring Integration Message.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMailMessageTransformer<T> implements Transformer<T> {

	protected final Log logger = LogFactory.getLog(this.getClass());


	public <IN> Message<T> transform(Message<IN> message) {
		Object payload = message.getPayload();
		if (!(payload instanceof javax.mail.Message)) {
			throw new MessageTransformationException(message, this.getClass().getSimpleName()
					+ " requires a javax.mail.Message payload");
		}
		javax.mail.Message mailMessage = (javax.mail.Message) payload;
		MessageBuilder<T> builder = null;
		try {
			builder = this.doTransform(mailMessage);
		}
		catch (Exception e) {
			throw new MessageTransformationException(message, "failed to transform mail message", e);
		}
		if (builder == null) {
			throw new MessageTransformationException(message, "failed to transform mail message");
		}
		builder.copyHeaders(this.extractHeaderMapFromMailMessage(mailMessage));
		return builder.build();
	}

	protected abstract MessageBuilder<T> doTransform(javax.mail.Message mailMessage) throws Exception;


	private Map<String, Object> extractHeaderMapFromMailMessage(javax.mail.Message mailMessage) {
		try {
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put(MailHeaders.FROM, this.convertToString(mailMessage.getFrom()));
			headers.put(MailHeaders.BCC, this.convertToStringArray(mailMessage.getRecipients(RecipientType.BCC)));
			headers.put(MailHeaders.CC, this.convertToStringArray(mailMessage.getRecipients(RecipientType.CC)));
			headers.put(MailHeaders.TO, this.convertToStringArray(mailMessage.getRecipients(RecipientType.TO)));
			headers.put(MailHeaders.REPLY_TO, this.convertToString(mailMessage.getReplyTo()));
			headers.put(MailHeaders.SUBJECT, mailMessage.getSubject());
			return headers;
		}
		catch (Exception e) {
			throw new MessagingException("conversion of MailMessage headers failed", e);
		}
	}

	private String convertToString(Address[] addresses) {
		if (addresses == null || addresses.length == 0) {
			return null;
		}
		Assert.state(addresses.length == 1, "expected a single value but received an Array");
		return addresses[0].toString();
	}

	private String[] convertToStringArray(Address[] addresses) {
		if (addresses != null) {
			String[] addressStrings = new String[addresses.length];
			for (int i = 0; i < addresses.length; i++) {
				addressStrings[i] = addresses[i].toString();
			}
			return addressStrings;
		}
		return new String[0];
	}

}
