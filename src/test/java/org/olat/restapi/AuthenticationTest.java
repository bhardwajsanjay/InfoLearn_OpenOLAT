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

package org.olat.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.olat.core.util.StringHelper;
import org.olat.restapi.security.RestSecurityHelper;
import org.olat.test.OlatJerseyTestCase;

import com.oreilly.servlet.Base64Encoder;

/**
 * 
 * Description:<br>
 * Test the authentication service
 * 
 * <P>
 * Initial Date:  14 apr. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
public class AuthenticationTest extends OlatJerseyTestCase {

	@Test
	public void testSessionCookieLogin() throws IOException, URISyntaxException {
		RestConnection conn = new RestConnection();
		
		URI uri = UriBuilder.fromUri(getContextURI()).path("auth").path("administrator").queryParam("password", "openolat").build();
		HttpGet method = conn.createGet(uri, MediaType.TEXT_PLAIN, true);
		HttpResponse code = conn.execute(method);
		assertEquals(200, code.getStatusLine().getStatusCode());
		String response = EntityUtils.toString(code.getEntity());
		assertTrue(response.startsWith("<hello"));
		assertTrue(response.endsWith("Hello administrator</hello>"));
	
		List<Cookie> cookies = conn.getCookieStore().getCookies();
		assertNotNull(cookies);
		assertTrue(cookies.size() > 0);
		
		conn.shutdown();
  }
	
	@Test
	public void testSessionCookieLoginHttpClient4() throws IOException, URISyntaxException {
		RestConnection conn = new RestConnection();
		
		URI uri = UriBuilder.fromUri(getContextURI()).path("auth").path("administrator").queryParam("password", "openolat").build();

		HttpGet method = conn.createGet(uri, MediaType.TEXT_PLAIN, true);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		String hello = EntityUtils.toString(response.getEntity());
		assertTrue(hello.startsWith("<hello"));
		assertTrue(hello.endsWith("Hello administrator</hello>"));
		List<org.apache.http.cookie.Cookie> cookies = conn.getCookieStore().getCookies();

		assertNotNull(cookies);
		assertFalse(cookies.isEmpty());
		assertNotNull(response.getFirstHeader(RestSecurityHelper.SEC_TOKEN));
		
		conn.shutdown();
  }
	
	@Test
	public void testWrongPassword() throws IOException, URISyntaxException {
		RestConnection conn = new RestConnection();
		
		URI uri = UriBuilder.fromUri(getContextURI()).path("auth").path("administrator").queryParam("password", "blabla").build();
		HttpGet method = conn.createGet(uri, MediaType.TEXT_PLAIN, true);
		HttpResponse code = conn.execute(method);
		assertEquals(401, code.getStatusLine().getStatusCode());
		
		conn.shutdown();
	}
	
	@Test
	public void testUnkownUser() throws IOException, URISyntaxException {
		RestConnection conn = new RestConnection();
		
		URI uri = UriBuilder.fromUri(getContextURI()).path("auth").path("treuitr").queryParam("password", "blabla").build();
		HttpGet method = conn.createGet(uri, MediaType.TEXT_PLAIN, true);
		HttpResponse code = conn.execute(method);
		assertEquals(401, code.getStatusLine().getStatusCode());
		
		conn.shutdown();
	}
	
	@Test
	public void testBasicAuthentication() throws IOException, URISyntaxException {
		RestConnection conn = new RestConnection();
		
		//path is protected
		URI uri = UriBuilder.fromUri(getContextURI()).path("users").path("version").build();
		HttpGet method = conn.createGet(uri, MediaType.TEXT_PLAIN, false);
		method.setHeader("Authorization", "Basic " + Base64Encoder.encode("administrator:openolat"));
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		String securityToken = conn.getSecurityToken(response);
		assertTrue(StringHelper.containsNonWhitespace(securityToken));
		
		conn.shutdown();
	}
	
	@Test
	public void testWebStandardAuthentication() throws IOException, URISyntaxException {
		URI uri = UriBuilder.fromUri(getContextURI()).path("users").path("version").build();
		RestConnection conn = new RestConnection(uri.toURL(), "administrator", "openolat");
		HttpGet method = conn.createGet(uri, MediaType.TEXT_PLAIN, false);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		String securityToken = conn.getSecurityToken(response);
		assertTrue(StringHelper.containsNonWhitespace(securityToken));
		
		conn.shutdown();
	}
}
