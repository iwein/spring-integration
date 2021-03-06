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

package org.springframework.integration.file.entries;

import org.springframework.util.Assert;


/**
 * this simply takes an {@link org.springframework.integration.file.entries.EntryListFilter}
 * and produces an object that can field just <b>one</b> argument instea of an array
 *
 * @author Josh Long
 */
public class SingleEntryAdaptingEntryListFilter<T> extends AbstractEntryListFilter<T> {

	/**
	 * the {@link org.springframework.integration.file.entries.EntryListFilter} that you'd like to delegate to
	 */
	private volatile EntryListFilter<T> entryFilter;

	public SingleEntryAdaptingEntryListFilter(EntryListFilter<T> ef) {
		this.entryFilter = ef;
		Assert.notNull(this.entryFilter, "the entryFilter can't be null");
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean accept(T t) {
		T[] ts = (T[]) new Object[]{t};
		return this.entryFilter.filterEntries(ts).size() == 1;
	}
}
