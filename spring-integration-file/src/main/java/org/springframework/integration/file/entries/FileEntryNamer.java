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

import java.io.File;


/**
 * {@link java.io.File} implementation of the {@link org.springframework.integration.file.entries.EntryNamer} strategy.
 * <p/>
 * This part feels a little over-engineered...
 *
 * @author Josh Long
 */
public class FileEntryNamer implements EntryNamer<File> {
	public String nameOf(File entry) {
		return (entry != null) ? entry.getName() : null;
	}
}
