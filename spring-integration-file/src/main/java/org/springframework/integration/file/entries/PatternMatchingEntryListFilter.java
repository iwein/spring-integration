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
package org.springframework.integration.file.entries;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;


/**
 * <emphasis>experimental</emphasis>
 * <p/>
 * Filters a listing of entries (T) by qualifying their 'name' (as determined by {@link org.springframework.integration.file.entries.EntryNamer})
 * against a regular expression (an instance of {@link java.util.regex.Pattern})
 *
 * @author Josh Long
 * @param <T>   the type of entry
 */
public class PatternMatchingEntryListFilter<T> extends AbstractEntryListFilter<T> implements InitializingBean {
	private Pattern pattern;
	private String patternExpression;
	private EntryNamer<T> entryNamer;

	public PatternMatchingEntryListFilter(EntryNamer<T> en, String p) {
		this.entryNamer = en;
		this.patternExpression = p;
	}

	public PatternMatchingEntryListFilter(EntryNamer<T> en, Pattern p) {
		this.entryNamer = en;
		this.pattern = p;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public void setPatternExpression(String patternExpression) {
		this.patternExpression = patternExpression;
	}

	public void afterPropertiesSet() throws Exception {
		if (StringUtils.hasText(this.patternExpression) && (pattern == null)) {
			this.pattern = Pattern.compile(this.patternExpression);
		}
		Assert.notNull(this.entryNamer, "'entryNamer' must not be null!");
		Assert.notNull(this.pattern, "'pattern' mustn't be null!");
	}

	@Override
	public boolean accept(T t) {
		return (t != null) && this.pattern.matcher(this.entryNamer.nameOf(t)).matches();
	}

	public void setEntryNamer(EntryNamer<T> entryNamer) {
		this.entryNamer = entryNamer;
	}
}
