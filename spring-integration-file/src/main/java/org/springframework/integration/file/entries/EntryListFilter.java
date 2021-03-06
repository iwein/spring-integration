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

import java.util.List;


/**
 * Strategy interface for filtering a group of entries / files.
 * <p/>
 * {@link EntryListFilter} that passes file entries only one time. This can
 * conveniently be used to prevent duplication of files, as is done in
 * {@link org.springframework.integration.file.FileReadingMessageSource}.
 * <p/>
 * This implementation is thread safe.
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @since 1.0.0
 */
public interface EntryListFilter<T> {
	List<T> filterEntries(T[] entries);
}
