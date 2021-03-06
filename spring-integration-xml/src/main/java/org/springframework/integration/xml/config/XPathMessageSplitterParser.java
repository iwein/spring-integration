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

package org.springframework.integration.xml.config;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.xml.splitter.XPathMessageSplitter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Jonas Partner
 */
public class XPathMessageSplitterParser extends AbstractConsumerEndpointParser {

	private final XPathExpressionParser xpathParser = new XPathExpressionParser();


	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(XPathMessageSplitter.class);
		String xPathExpressionRef = element.getAttribute("xpath-expression-ref");
		String documentBuilderFactoryRef = element.getAttribute("doc-builder-factory");
		String createDocuments = element.getAttribute("create-documents");
		
		NodeList xPathExpressionNodes = element.getElementsByTagNameNS(element.getNamespaceURI(), "xpath-expression");
		Assert.isTrue(xPathExpressionNodes.getLength() <= 1, "only one xpath-expression child can be specified");
		boolean hasChild = xPathExpressionNodes.getLength() == 1;
		boolean hasReference = StringUtils.hasText(xPathExpressionRef);
		
		Assert.isTrue(hasChild ^ hasReference, "Exactly one of 'xpath-expression' or 'xpath-expression-ref' is required.");
		if (hasChild) {
			BeanDefinition beanDefinition = this.xpathParser.parse((Element) xPathExpressionNodes.item(0), parserContext);
			builder.addConstructorArgValue(beanDefinition);
		}
		else {
			builder.addConstructorArgReference(xPathExpressionRef);
		}
		
		if(StringUtils.hasText(documentBuilderFactoryRef)){
			builder.addPropertyReference("documentBuilder", documentBuilderFactoryRef);
		}
		if(StringUtils.hasText("create-documents")){
			builder.addPropertyValue("createDocuments", Boolean.valueOf(createDocuments));
		}
		
		return builder;
	}

}
