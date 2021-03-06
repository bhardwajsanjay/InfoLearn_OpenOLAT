/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.ims.lti;

import java.util.List;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.resource.OLATResource;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 15.05.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LTIManagerTest extends OlatTestCase {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private LTIManager ltiManager;
	
	@Test
	public void createOutcome() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("lti-1-" + UUID.randomUUID().toString());
		OLATResource resource = JunitTestHelper.createRandomResource();
		LTIOutcome outcome = ltiManager.createOutcome(id, resource, "sub", "action", "lti-outcome", "my value");
		dbInstance.commitAndCloseSession();
		
		Assert.assertNotNull(outcome);
		Assert.assertNotNull(outcome.getCreationDate());
		Assert.assertNotNull(outcome.getLastModified());
		Assert.assertEquals(id, outcome.getAssessedIdentity());
		Assert.assertEquals(resource, outcome.getResource());
		Assert.assertEquals("sub", outcome.getResSubPath());
		Assert.assertEquals("action", outcome.getAction());
		Assert.assertEquals("lti-outcome", outcome.getOutcomeKey());
		Assert.assertEquals("my value", outcome.getOutcomeValue());
	}

	@Test
	public void loadOutcome() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("lti-2-" + UUID.randomUUID().toString());
		OLATResource resource = JunitTestHelper.createRandomResource();
		LTIOutcome outcome = ltiManager.createOutcome(id, resource, "sub", "update", "new-outcome", "lti score value");
		dbInstance.commitAndCloseSession();
		
		LTIOutcome reloadedOutcome = ltiManager.loadOutcomeByKey(outcome.getKey());
		Assert.assertNotNull(reloadedOutcome);
		Assert.assertNotNull(reloadedOutcome.getCreationDate());
		Assert.assertNotNull(reloadedOutcome.getLastModified());
		Assert.assertEquals(id, reloadedOutcome.getAssessedIdentity());
		Assert.assertEquals(resource, reloadedOutcome.getResource());
		Assert.assertEquals("sub", reloadedOutcome.getResSubPath());
		Assert.assertEquals("update", outcome.getAction());
		Assert.assertEquals("new-outcome", reloadedOutcome.getOutcomeKey());
		Assert.assertEquals("lti score value", reloadedOutcome.getOutcomeValue());
	}
	
	@Test
	public void loadOutcomes_byIdentity() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("lti-3-" + UUID.randomUUID().toString());
		OLATResource resource = JunitTestHelper.createRandomResource();
		LTIOutcome outcome1 = ltiManager.createOutcome(id, resource, "sub", "update", "new-outcome", "lti score value");
		LTIOutcome outcome2 = ltiManager.createOutcome(id, resource, "sub", "delete", "new-outcome", null);
		dbInstance.commitAndCloseSession();
		
		List<LTIOutcome> outcomes = ltiManager.loadOutcomes(id, resource, "sub");
		Assert.assertNotNull(outcomes);
		Assert.assertEquals(2, outcomes.size());
		Assert.assertTrue(outcomes.contains(outcome1));
		Assert.assertTrue(outcomes.contains(outcome2));
	}
	


}
