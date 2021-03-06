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

package org.springframework.integration.jdbc.config;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for {@link org.springframework.integration.jdbc.JdbcPollingChannelAdapter}.
 * 
 * @author Jonas Partner
 * @since 2.0
 */
public class JdbcPollingChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected String parseSource(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition("org.springframework.integration.jdbc.JdbcPollingChannelAdapter");
		String dataSourceRef = element.getAttribute("data-source");
		String jdbcOperationsRef = element.getAttribute("jdbc-operations");
		boolean refToDataSourceSet = StringUtils.hasText(dataSourceRef);
		boolean refToJdbcOperationsSet = StringUtils.hasText(jdbcOperationsRef);
		if ((refToDataSourceSet && refToJdbcOperationsSet)
				|| (!refToDataSourceSet && !refToJdbcOperationsSet)) {
			parserContext.getReaderContext().error("Exactly one of the attributes data-source or " +
					"simple-jdbc-operations should be set for the JDBC inbound-channel-adapter", source);
		}
		String query = IntegrationNamespaceUtils.getTextFromAttributeOrNestedElement(element, "query", parserContext);
		if (!StringUtils.hasText(query)) {
			throw new BeanCreationException("The query attrbitue is required");
		}
		String update = IntegrationNamespaceUtils.getTextFromAttributeOrNestedElement(element, "update", parserContext);
		if (refToDataSourceSet) {
			builder.addConstructorArgReference(dataSourceRef);
		}
		else {
			builder.addConstructorArgReference(jdbcOperationsRef);
		}
		builder.addConstructorArgValue(query);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "row-mapper");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "update-sql-parameter-source-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "select-sql-parameter-source");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-rows-per-poll");
		if (update!=null) {
			builder.addPropertyValue("updateSql", update);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "update-per-row");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
