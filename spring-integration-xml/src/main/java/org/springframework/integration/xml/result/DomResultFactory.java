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

package org.springframework.integration.xml.result;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.springframework.integration.MessagingException;

/**
 * @author Jonas Partner
 */
public class DomResultFactory implements ResultFactory {

	private final DocumentBuilderFactory docBuilderFactory;

	public DomResultFactory(DocumentBuilderFactory docBuilderFactory) {
		this.docBuilderFactory = docBuilderFactory;
	}

	public DomResultFactory() {
		this.docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setNamespaceAware(true);
	}

	public synchronized Result createResult(Object payload) {
		try {
			return new DOMResult(getNewDocumentBuilder().newDocument());
		}
		catch (ParserConfigurationException e) {
			throw new MessagingException("Failed to create Result for payload type [" + payload.getClass().getName()
					+ "]");
		}
	}

	protected DocumentBuilder getNewDocumentBuilder() throws ParserConfigurationException {
		synchronized (docBuilderFactory) {
			return docBuilderFactory.newDocumentBuilder();
		}

	}

}
