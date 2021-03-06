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

package org.springframework.integration.file;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.file.entries.EntryListFilter;
import org.springframework.integration.file.entries.FileEntryNamer;
import org.springframework.integration.file.entries.PatternMatchingEntryListFilter;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mark Fisher
 */
public class PatternMatchingFileListFilterTests {

    private FileEntryNamer fileEntryNamer = new FileEntryNamer();

    @Test
    public void matchSingleFile() {
        File[] files = new File[]{new File("/some/path/test.txt")};
        Pattern pattern = Pattern.compile("[a-z]+\\.txt");
        PatternMatchingEntryListFilter<File> filter = new PatternMatchingEntryListFilter<File>(fileEntryNamer, pattern);
        List<File> accepted = filter.filterEntries(files);
        assertEquals(1, accepted.size());
    }

    @Test
    public void noMatchWithSingleFile() {
        File[] files = new File[]{new File("/some/path/Test.txt")};
        Pattern pattern = Pattern.compile("[a-z]+\\.txt");
        PatternMatchingEntryListFilter<File> filter = new PatternMatchingEntryListFilter<File>(fileEntryNamer, pattern);
        List<File> accepted = filter.filterEntries(files);
        assertEquals(0, accepted.size());
    }

    @Test
    public void matchSubset() {
        File[] files = new File[]{
                new File("/some/path/foo.txt"),
                new File("/some/path/foo.not"),
                new File("/some/path/bar.txt"),
                new File("/some/path/bar.not")
        };
        Pattern pattern = Pattern.compile("[a-z]+\\.txt");
        PatternMatchingEntryListFilter<File> filter = new PatternMatchingEntryListFilter<File>(this.fileEntryNamer, pattern);
        List<File> accepted = filter.filterEntries(files);
        assertEquals(2, accepted.size());
        assertTrue(accepted.contains(new File("/some/path/foo.txt")));
        assertTrue(accepted.contains(new File("/some/path/bar.txt")));
    }


    @Test               @SuppressWarnings("unchecked")
    public void patternEditorInContext() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "patternMatchingFileListFilterTests.xml", this.getClass());
        EntryListFilter<File> filter = (EntryListFilter<File>) context.getBean("filter");
        File[] files = new File[]{new File("/some/path/foo.txt")};
        List<File> accepted = filter.filterEntries(files);
        assertEquals(1, accepted.size());
    }

    @Test(expected = BeanCreationException.class)
    public void invalidPatternSyntax() throws Throwable {
        new ClassPathXmlApplicationContext("invalidPatternMatchingFileListFilterTests.xml", this.getClass());
    }

}
