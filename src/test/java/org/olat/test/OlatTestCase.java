/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 

package org.olat.test;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.helpers.Settings;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.event.FrameworkStartupEventChannel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Initial Date:  25.10.2002
 *
 * @author Florian Gnaegi
 * @author guido
 *
 * This class is common parent to all JUnit Use Case tests in OLAT framework integration tests. 
 */
@ContextConfiguration(loader = MockServletContextWebContextLoader.class, locations = {
	"classpath:/org/olat/_spring/mainContext.xml"
})
public abstract class OlatTestCase extends AbstractJUnit4SpringContextTests {
	private static final OLog log = Tracing.createLoggerFor(OlatTestCase.class);
	
	private static boolean postgresqlConfigured = false;
	private static boolean oracleConfigured = false;
	private static boolean started = false;
	
	 @Rule public TestName name = new TestName();
	
	/**
	 * If you like to disable a test method for some time just add the
	 * @Ignore("not today") annotation
	 * 
	 * The normal flow is that the spring context gets loaded and befor each test method the @before will be executed and after the the method each time
	 * the @after will be executed
	 */
	
	/**
	 * @param arg0
	 */
	public OlatTestCase() {
		Settings.setJUnitTest(true);
	}
	
	@Before
	public void printBanner(){
		log.info("Method run: " + name.getMethodName() + "(" + this.getClass().getCanonicalName() + ")");
		
		if(started) {
			return;
		}
		
		FrameworkStartupEventChannel.fireEvent();
		
		String dbVendor = DBFactory.getInstance().getDbVendor();
		postgresqlConfigured = dbVendor != null && dbVendor.startsWith("postgres");
		oracleConfigured = dbVendor != null && dbVendor.startsWith("oracle");
		
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		printOlatLocalProperties();
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println("+ OLAT configuration initialized, starting now with junit tests +");
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

		started = true;
	}
	
	@After
	public void closeConnectionAfter() {
		log.info("Method test finished: " + name.getMethodName() + "(" + this.getClass().getCanonicalName() + ")");
		try {
			DBFactory.getInstance().commitAndCloseSession();
		} catch (Exception e) {
			e.printStackTrace();

			try {
				DBFactory.getInstance().rollbackAndCloseSession();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void printOlatLocalProperties() {
		Resource overwritePropertiesRes = new ClassPathResource("olat.local.properties");
		try {
			Properties overwriteProperties = new Properties();
			overwriteProperties.load(overwritePropertiesRes.getInputStream());
			Enumeration<String> propNames = (Enumeration<String>)overwriteProperties.propertyNames();
			
			System.out.println("### olat.local.properties : ###");
			while (propNames.hasMoreElements()) {
				String propName = propNames.nextElement();
				System.out.println("++" + propName + "='" + overwriteProperties.getProperty(propName) + "'");
			}
		} catch (IOException e) {
			System.err.println("Could not load properties files from classpath! Exception=" + e);
		}
		
	}
	
	protected void sleep(int milliSeconds) {
		try {
			Thread.sleep(milliSeconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return True if the test run on PostreSQL
	 */
	protected boolean isPostgresqlConfigured() {
		return postgresqlConfigured;
	}

	/**
	 * @return True if the test run on Oracle
	 */
	protected boolean isOracleConfigured() {
		return oracleConfigured;
	}
}