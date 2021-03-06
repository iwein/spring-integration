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
package org.springframework.integration.groovy.config;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @since 2.0
 * 
 */
public class RefreshableResourceScriptSource implements ScriptSource {

	private final long refreshDelay;

	private final ResourceScriptSource source;

	private AtomicLong lastModifiedChecked = new AtomicLong(System.currentTimeMillis());

	private String script;

	public RefreshableResourceScriptSource(Resource resource, long refreshDelay) {
		this.refreshDelay = refreshDelay;
		this.source = new ResourceScriptSource(resource);
		try {
			this.script = source.getScriptAsString();
		}
		catch (IOException e) {
			lastModifiedChecked.set(0);
		}
	}

	public String getScriptAsString() throws IOException {
		this.script = source.getScriptAsString();
		return script;
	}

	public boolean isModified() {
		if (refreshDelay < 0) {
			return false;
		}
		long time = System.currentTimeMillis();
		if (refreshDelay == 0 || (time - lastModifiedChecked.get()) > refreshDelay) {
			lastModifiedChecked.set(time);
			return source.isModified();
		}
		return false;
	}

	public String suggestedClassName() {
		return source.suggestedClassName();
	}

}
