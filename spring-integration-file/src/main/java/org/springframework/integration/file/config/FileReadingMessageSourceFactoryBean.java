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
package org.springframework.integration.file.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.FactoryBean;

import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.entries.CompositeEntryListFilter;
import org.springframework.integration.file.entries.EntryListFilter;
import org.springframework.integration.file.locking.AbstractFileLockerFilter;

import java.io.File;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;


/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @since 1.0.3
 */
public class FileReadingMessageSourceFactoryBean implements FactoryBean<FileReadingMessageSource> {
    private static Log logger = LogFactory.getLog(FileReadingMessageSourceFactoryBean.class);
    private volatile FileReadingMessageSource source;
    private volatile File directory;
    private volatile EntryListFilter<File> filter;
    private volatile AbstractFileLockerFilter locker;
    private volatile Comparator<File> comparator;
    private volatile DirectoryScanner scanner;
    private volatile Boolean scanEachPoll;
    private volatile Boolean autoCreateDirectory;
    private volatile Integer queueSize;
    private final Object initializationMonitor = new Object();

    @SuppressWarnings("unused")
    public void setDirectory(File directory) {
        this.directory = directory;
    }

    @SuppressWarnings("unused")
    public void setComparator(Comparator<File> comparator) {
        this.comparator = comparator;
    }

    @SuppressWarnings("unused")
    public void setScanner(DirectoryScanner scanner) {
        this.scanner = scanner;
    }

    @SuppressWarnings("unused")
    public void setFilter(EntryListFilter<File> filter) {
        if (filter instanceof AbstractFileLockerFilter && (this.locker == null)) {
            this.setLocker((AbstractFileLockerFilter) filter);
        }

        this.filter = filter;
    }

    @SuppressWarnings("unused")
    public void setScanEachPoll(Boolean scanEachPoll) {
        this.scanEachPoll = scanEachPoll;
    }

    @SuppressWarnings("unused")
    public void setAutoCreateDirectory(Boolean autoCreateDirectory) {
        this.autoCreateDirectory = autoCreateDirectory;
    }

    @SuppressWarnings("unused")
    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

    public void setLocker(AbstractFileLockerFilter locker) {
        this.locker = locker;
    }

    public FileReadingMessageSource getObject() throws Exception {
        if (this.source == null) {
            initSource();
        }

        return this.source;
    }

    public Class<?> getObjectType() {
        return FileReadingMessageSource.class;
    }

    public boolean isSingleton() {
        return true;
    }

    private void initSource() {
        synchronized (this.initializationMonitor) {
            if (this.source != null) {
                return;
            }

            boolean comparatorSet = this.comparator != null;
            boolean queueSizeSet = this.queueSize != null;

            if (comparatorSet) {
                if (queueSizeSet) {
                    logger.warn("'comparator' and 'queueSize' are mutually exclusive. Ignoring 'queueSize'");
                }

                this.source = new FileReadingMessageSource(this.comparator);
            } else if (queueSizeSet) {
                this.source = new FileReadingMessageSource(queueSize);
            } else {
                this.source = new FileReadingMessageSource();
            }

            this.source.setDirectory(this.directory);

            if (this.scanner != null) {
                this.source.setScanner(this.scanner);
            }

            if (this.filter != null) {
                if (this.locker == null) {
                    this.source.setFilter(this.filter);
                } else {
                    CompositeEntryListFilter<File> fileCompositeEntryListFilter = new CompositeEntryListFilter<File>();

                    for (EntryListFilter<File> filter : Arrays.asList(this.filter, this.locker))
                        fileCompositeEntryListFilter.addFilter(filter);

                    this.source.setFilter(fileCompositeEntryListFilter);
                    this.source.setLocker(locker);
                }
            }

            if (this.scanEachPoll != null) {
                this.source.setScanEachPoll(this.scanEachPoll);
            }

            if (this.autoCreateDirectory != null) {
                this.source.setAutoCreateDirectory(this.autoCreateDirectory);
            }

            this.source.afterPropertiesSet();
        }
    }
}
