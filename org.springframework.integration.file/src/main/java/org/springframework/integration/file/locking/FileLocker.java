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
package org.springframework.integration.file.locking;

import java.io.File;
import java.io.IOException;

/**
 * A FileLocker is a strategy that can ensure that files are only processed a single time.
 *
 * Implementations are free implement any relation between locking and unlocking (e.g. the Noop
 *
 * @author Iwein Fuld
 */
public interface FileLocker {

    /**
     * Tries to lock the given file and returns <code>true</code> if it was successful, <code>false</code> otherwise.
     */
    boolean lock(File fileToLock);

    /**
     * Unlocks the given file.
     */
    void unlock(File fileToUnlock);
}