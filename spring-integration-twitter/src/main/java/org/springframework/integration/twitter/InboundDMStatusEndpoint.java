/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.twitter;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * This class handles support for receiving DMs (direct messages) using Twitter.
 *
 * @author Josh Long
 */
public class InboundDMStatusEndpoint extends AbstractInboundTwitterEndpointSupport<DirectMessage> {
	private Comparator<DirectMessage> dmComparator = new Comparator<DirectMessage>() {
		public int compare(DirectMessage directMessage, DirectMessage directMessage1) {
			return directMessage.getCreatedAt().compareTo(directMessage1.getCreatedAt());
		}
	};

	@Override
	protected void markLastStatusId(DirectMessage dm) {
		this.markerId = dm.getId();
	}

	@Override
	protected List<DirectMessage> sort(List<DirectMessage> rl) {
		List<DirectMessage> dms = new ArrayList<DirectMessage>();
		dms.addAll(rl);
		Collections.sort(dms, dmComparator);

		return dms;
	}

	@Override
	protected void refresh() throws Exception {
		this.runAsAPIRateLimitsPermit(new ApiCallback<InboundDMStatusEndpoint>() {
			public void run(InboundDMStatusEndpoint t, Twitter twitter)
					throws Exception {
				forwardAll((!hasMarkedStatus()) ? t.twitter.getDirectMessages() : t.twitter.getDirectMessages(new Paging(t.getMarkerId())));
			}
		});
	}
}
