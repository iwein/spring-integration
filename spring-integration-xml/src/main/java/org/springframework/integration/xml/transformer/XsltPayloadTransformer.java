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
package org.springframework.integration.xml.transformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.integration.xml.source.DomSourceFactory;
import org.springframework.integration.xml.source.SourceFactory;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.Map;

/**
 * Thread safe XSLT transformer implementation which returns a transformed
 * {@link Source}, {@link Document}, or {@link String}. If
 * alwaysUseSourceResultFactories is false (default) the following logic occurs
 * <p/>
 * {@link String} payload in results in {@link String} payload out
 * <p/>
 * {@link Document} payload in results in {@link Document} payload out
 * <p/>
 * {@link Source} payload in results in {@link Result} payload out, type will be
 * determined by the {@link ResultFactory}, {@link DomResultFactory} by default.
 * If an instance of {@link ResultTransformer} is registered this will be used
 * to convert the result.
 * <p/>
 * If alwaysUseSourceResultFactories is true then the ResultFactory and
 * {@link SourceFactory} will be used to create the {@link Source} from the
 * payload and the {@link Result} to pass into the transformer. An instance of
 * {@link ResultTransformer} can also be provided to convert the Result prior to
 * returning.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class XsltPayloadTransformer extends AbstractTransformer {

	private final Log logger = LogFactory.getLog(this.getClass());
	private final Templates templates;
	private final StandardEvaluationContext context = new StandardEvaluationContext();
	private Map<String, Expression> xslParameterMappings;

	private final ResultTransformer resultTransformer;

	private volatile SourceFactory sourceFactory = new DomSourceFactory();

	private volatile ResultFactory resultFactory = new DomResultFactory();

	private volatile boolean alwaysUseSourceResultFactories = false;

	private String[] xsltParamHeaders;

	public XsltPayloadTransformer(Templates templates) throws ParserConfigurationException {
		this(templates, null);
	}

	public XsltPayloadTransformer(Templates templates, ResultTransformer resultTransformer) throws ParserConfigurationException {
		this.templates = templates;
		this.resultTransformer = resultTransformer;
	}

	public XsltPayloadTransformer(Resource xslResource) throws Exception {
		this(TransformerFactory.newInstance().newTemplates(createStreamSourceOnResource(xslResource)), null);
	}

	public XsltPayloadTransformer(Resource xslResource, ResultTransformer resultTransformer) throws Exception {
		this(TransformerFactory.newInstance().newTemplates(createStreamSourceOnResource(xslResource)), resultTransformer);
	}


	/**
	 * Compensate for the fact that a Resource <i>may</i> not be a File or even
	 * addressable through a URI. If it is, we want the created StreamSource to
	 * read other resources relative to the provided one. If it isn't, it loads
	 * from the default path.
	 */
	private static StreamSource createStreamSourceOnResource(Resource xslResource) throws IOException {
		try {
			String systemId = xslResource.getURI().toString();
			return new StreamSource(xslResource.getInputStream(), systemId);
		}
		catch (IOException e) {
			return new StreamSource(xslResource.getInputStream());
		}
	}

	/**
	 * Sets the SourceFactory.
	 */
	public void setSourceFactory(SourceFactory sourceFactory) {
		Assert.notNull(sourceFactory, "SourceFactory can not be null");
		this.sourceFactory = sourceFactory;
	}

	/**
	 * Sets the ResultFactory
	 */
	public void setResultFactory(ResultFactory resultFactory) {
		Assert.notNull(sourceFactory, "ResultFactory can not be null");
		this.resultFactory = resultFactory;
	}

	/**
	 * Specifies whether {@link ResultFactory} and {@link SourceFactory} should always
	 * be used, even for directly supported payloads such as {@link String} and {@link Document}.
	 */
	public void setAlwaysUseSourceResultFactories(boolean alwaysUserSourceResultFactories) {
		this.alwaysUseSourceResultFactories = alwaysUserSourceResultFactories;
	}

	@Override
	protected Object doTransform(Message message) throws Exception {
		Transformer transformer = buildTransformer(message);
		Object payload = message.getPayload();
		Object transformedPayload = null;
		if (this.alwaysUseSourceResultFactories) {
			transformedPayload = transformUsingFactories(payload, transformer);
		}
		else if (payload instanceof String) {
			transformedPayload = transformString((String) payload, transformer);
		}
		else if (payload instanceof Document) {
			transformedPayload = transformDocument((Document) payload, transformer);
		}
		else if (payload instanceof Source) {
			transformedPayload = transformSource((Source) payload, payload, transformer);
		}
		else {
			// fall back to trying factories
			transformedPayload = transformUsingFactories(payload, transformer);
		}
		return transformedPayload;
	}

	protected Object transformUsingFactories(Object payload, Transformer transformer) throws TransformerException {
		Source source = sourceFactory.createSource(payload);
		return transformSource(source, payload, transformer);
	}

	protected Object transformSource(Source source, Object payload, Transformer transformer) throws TransformerException {
		Result result = resultFactory.createResult(payload);
		transformer.transform(source, result);
		if (resultTransformer != null) {
			return resultTransformer.transformResult(result);
		}
		return result;
	}

	protected String transformString(String stringPayload, Transformer transformer) throws TransformerException {
		StringResult result = new StringResult();
		transformer.transform(new StringSource(stringPayload), result);
		return result.toString();
	}

	protected Document transformDocument(Document documentPayload, Transformer transformer) throws TransformerException {
		DOMSource source = new DOMSource(documentPayload);
		Result result = resultFactory.createResult(documentPayload);
		if (!DOMResult.class.isAssignableFrom(result.getClass())) {
			throw new MessagingException(
					"Document to Document conversion requires a DOMResult-producing ResultFactory implementation.");
		}
		DOMResult domResult = (DOMResult) result;
		transformer.transform(source, domResult);
		return (Document) domResult.getNode();
	}
	
	protected Transformer buildTransformer(Message<?> message) throws TransformerException {
		//process  individual mappings
		Transformer transformer = this.templates.newTransformer();
		context.setRootObject(message);
		context.addPropertyAccessor(new MapAccessor());
		if (xslParameterMappings != null){
			for (String parameterName: xslParameterMappings.keySet()) {
				Expression expression = xslParameterMappings.get(parameterName);
				Object value = null;
				try {
					value = expression.getValue(context);
					transformer.setParameter(parameterName, value);
				} catch (Exception e) {
					logger.warn("Header expression '" + expression.getExpressionString() + "' can not resolve within current message and will not be mapped to XSLT parameter");
				}		
			}
		}
		// process xslt-parameter-headers
		MessageHeaders headers =  message.getHeaders();
		if (xsltParamHeaders != null){
			for (String headerName : headers.keySet()) {
				if (PatternMatchUtils.simpleMatch(xsltParamHeaders, headerName)){
					transformer.setParameter(headerName, headers.get(headerName));
				}			
			}
		}
		return transformer;
	}

	public Map<String, Expression> getXslParameterMappings() {
		return xslParameterMappings;
	}

	public void setXslParameterMappings(Map<String, Expression> xslParameterMappings) {
		this.xslParameterMappings = xslParameterMappings;
	}
	public String[] getXsltParamHeaders() {
		return xsltParamHeaders;
	}

	public void setXsltParamHeaders(String[] xsltParamHeaders) {
		this.xsltParamHeaders = xsltParamHeaders;
	}
}
