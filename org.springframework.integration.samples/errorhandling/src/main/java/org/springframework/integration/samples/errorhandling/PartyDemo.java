/* Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.samples.errorhandling;

import java.io.IOException;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Iwein Fuld
 */
public class PartyDemo {

	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("errorHandlingDemo.xml", PartyDemo.class);
		System.out.println("hit key to stop");
		try {
			System.in.read();
		}
		catch (IOException e) {
 			throw new RuntimeException(e);
		}
		System.exit(0);
	}

}