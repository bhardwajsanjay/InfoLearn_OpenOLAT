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
package org.olat.user;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.AuthHelper;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.test.OlatTestCase;

/**
 * Performance test for check if email exist
 * 
 * @author Christian Guretzki
 */
public class EmailCheckPerformanceTest extends OlatTestCase {
	private static final String TEST_DOMAIN = "@test.test";

	private static final String USERNAME_CONSTANT = "email-test";

	private static OLog log = Tracing.createLoggerFor(EmailCheckPerformanceTest.class);

	private static long createUserTime;
	private static long testExistEmailAddressTime;
	
	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setup()throws Exception {
			createUsers();
	}


	@Test public void testUserManger() throws Exception {
		System.out.println("testUserManger start...");
		int MAX_LOOP = 100;
		long startTime = System.currentTimeMillis();
		for (int i = 1; i<MAX_LOOP; i++) {
			Identity test = UserManager.getInstance().findIdentityByEmail(i+USERNAME_CONSTANT+ TEST_DOMAIN);
			if (test == null) System.out.println("user with email not found: "+i+USERNAME_CONSTANT+ TEST_DOMAIN);
			assertNotNull(test);
		}
		long endTime = System.currentTimeMillis();
		
		//optimized version
		
		long startTime2 = System.currentTimeMillis();
		for (int i = 1; i<MAX_LOOP; i++) {
			boolean test = UserManager.getInstance().userExist(i+USERNAME_CONSTANT+ TEST_DOMAIN);
			assertTrue(test);
		}
		long endTime2 = System.currentTimeMillis();
		
		log.info("testUserManger takes time=" + (endTime - startTime) + " testUserManger (optimized) takes time=" + (endTime2 - startTime2) + " ; testExistEmailAddressTime=" + testExistEmailAddressTime + " ; createUserTime=" + createUserTime);	
	}
	
	
	private void createUsers() {
		int numberUsers = 10000;
		String username;
		String institution;
		String gender;
		
		long startTime = System.currentTimeMillis();

		UserManager um = UserManager.getInstance();
		BaseSecurity sm = BaseSecurityManager.getInstance();
		//only create users if not yet done
		if (um.findIdentityByEmail("1"+USERNAME_CONSTANT+TEST_DOMAIN) == null) {
			// create users group
			if (sm.findSecurityGroupByName(Constants.GROUP_OLATUSERS) == null) {
				sm.createAndPersistNamedSecurityGroup(Constants.GROUP_OLATUSERS);
			}
			
			System.out.println("TEST start creating " + numberUsers + " testusers");
			for (int i = 1; i < numberUsers+1; i++) {
				username = i + USERNAME_CONSTANT;
				if (i % 2 == 0) {
					institution = "myinst";
					gender = "m";
				} else {
					institution = "yourinst";
					gender = "f";
				}
				User user = um.createUser(username + "first", username + "last", username + TEST_DOMAIN);
				user.setProperty(UserConstants.GENDER, gender);
				user.setProperty(UserConstants.BIRTHDAY, "24.07.3007");
				user.setProperty(UserConstants.STREET, "Zähringerstrasse 26");
				user.setProperty(UserConstants.EXTENDEDADDRESS, null);
				user.setProperty(UserConstants.POBOX, null);
				user.setProperty(UserConstants.CITY, "Zürich");
				user.setProperty(UserConstants.COUNTRY, "Switzerland");
				user.setProperty(UserConstants.TELMOBILE, "123456789");
				user.setProperty(UserConstants.TELOFFICE, "123456789");
				user.setProperty(UserConstants.TELPRIVATE, "123456789");
				user.setProperty(UserConstants.INSTITUTIONALEMAIL, username + "@" + institution);
				user.setProperty(UserConstants.INSTITUTIONALNAME, institution);
				user.setProperty(UserConstants.INSTITUTIONALUSERIDENTIFIER, username + "-" + institution);
				AuthHelper.createAndPersistIdentityAndUserWithUserGroup(username, "hokuspokus", user);
	
				if (i % 10 == 0) {
					// flush now to obtimize performance
					DBFactory.getInstance().closeSession();
					System.out.print(".");
				}
			}
			long endTime = System.currentTimeMillis();
			createUserTime = (endTime - startTime);
			System.out.println("TEST created " + numberUsers + " testusers in createUserTime=" + createUserTime);
		}
	}
}