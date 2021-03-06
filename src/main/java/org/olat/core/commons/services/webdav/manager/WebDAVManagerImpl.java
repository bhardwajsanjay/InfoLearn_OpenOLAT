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
*/

package org.olat.core.commons.services.webdav.manager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.core.commons.services.webdav.WebDAVManager;
import org.olat.core.commons.services.webdav.WebDAVModule;
import org.olat.core.commons.services.webdav.WebDAVProvider;
import org.olat.core.commons.services.webdav.servlets.WebResourceRoot;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.util.SessionInfo;
import org.olat.core.util.UserSession;
import org.olat.core.util.cache.CacheWrapper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.session.UserSessionManager;
import org.olat.core.util.vfs.MergeSource;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VirtualContainer;
import org.olat.core.util.vfs.callbacks.ReadOnlyCallback;

import com.oreilly.servlet.Base64Decoder;

/**
 * Initial Date:  16.04.2003
 *
 * @author Mike Stock
 * @author guido
 * 
 * Comment:  
 * 
 */
public class WebDAVManagerImpl implements WebDAVManager {
	private static boolean enabled = true;
	
	public static final String BASIC_AUTH_REALM = "OLAT WebDAV Access";
	private CoordinatorManager coordinatorManager;

	private CacheWrapper<CacheKey,UserSession> timedSessionCache;
	
	private UserSessionManager sessionManager;
	private WebDAVAuthManager webDAVAuthManager;
	private WebDAVModule webdavModule;

	/**
	 * [spring]
	 */
	private WebDAVManagerImpl(CoordinatorManager coordinatorManager) {
		this.coordinatorManager = coordinatorManager;
	}

	/**
	 * [used by Spring]
	 * @param sessionManager
	 */
	public void setSessionManager(UserSessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}
	
	/**
	 * [used by Spring]
	 * @param webDAVAuthManager
	 */
	public void setWebDAVAuthManager(WebDAVAuthManager webDAVAuthManager) {
		this.webDAVAuthManager = webDAVAuthManager;
	}
	
	/**
	 * [used by Spring]
	 * @param webdavModule
	 */
	public void setWebDAVModule(WebDAVModule webdavModule) {
		this.webdavModule = webdavModule;
	}
	
	public void init() {
		timedSessionCache = coordinatorManager.getCoordinator().getCacher().getCache(WebDAVManager.class.getSimpleName(), "webdav");
	}
	
	@Override
	public WebResourceRoot getWebDAVRoot(HttpServletRequest req) {
		UserSession usess = getUserSession(req);
		if (usess == null || usess.getIdentity() == null) {
			return createEmptyRoot(usess);
		}

		usess.getSessionInfo().setLastClickTime();
		VFSResourceRoot fdc = (VFSResourceRoot)usess.getEntry("_DIRCTX");
		if (fdc != null) {
			return fdc;
		}
		
		Identity identity = usess.getIdentity();
		VFSContainer webdavContainer = getMountableRoot(identity);
		
		//create the / folder
		VirtualContainer rootContainer = new VirtualContainer("");
		rootContainer.addItem(webdavContainer);
		rootContainer.setLocalSecurityCallback(new ReadOnlyCallback());

		fdc = new VFSResourceRoot(identity, rootContainer);
		usess.putEntry("_DIRCTX", fdc);
		return fdc;
	}
	
	/**
	 * Returns a mountable root containing all entries which will be exposed to the webdav mount.
	 * @return
	 */
	private VFSContainer getMountableRoot(Identity identity) {
		MergeSource vfsRoot = new MergeSource(null, "webdav");
		for (Map.Entry<String, WebDAVProvider> entry : webdavModule.getWebDAVProviders().entrySet()) {
			WebDAVProvider provider = entry.getValue();
			vfsRoot.addContainer(new WebDAVProviderNamedContainer(identity, provider));
		}
		return vfsRoot;
	}
	
	private WebResourceRoot createEmptyRoot(UserSession usess) {
		//create the / folder
		VirtualContainer rootContainer = new VirtualContainer("");
		rootContainer.setLocalSecurityCallback(new ReadOnlyCallback());

		VFSResourceRoot fdc = new VFSResourceRoot(usess.getIdentity(), rootContainer);
		return fdc;
	}

	/**
	 * @see org.olat.core.commons.services.webdav.WebDAVManager#handleAuthentication(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public boolean handleAuthentication(HttpServletRequest req, HttpServletResponse resp) {
		//manger not started
		if(timedSessionCache == null) {
			return false;
		}
		
		UserSession usess = sessionManager.getUserSession(req);
		if(usess != null && usess.isAuthenticated()) {
			req.setAttribute(REQUEST_USERSESSION_KEY, usess);
			return true;
		} 

		usess = doAuthentication(req, resp);
		if (usess == null) {
			return false;
		}

		// register usersession in REQUEST, not session !!
		// see SecureWebDAVServlet.setAuthor() and checkQuota()
		req.setAttribute(REQUEST_USERSESSION_KEY, usess);
		return true;
	}
	
	/**
	 * @see org.olat.core.commons.services.webdav.WebDAVManager#getUserSession(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public UserSession getUserSession(HttpServletRequest req) {
		return (UserSession)req.getAttribute(REQUEST_USERSESSION_KEY);
	}
	
	private UserSession doAuthentication(HttpServletRequest request, HttpServletResponse response) {
		// Get the Authorization header, if one was supplied
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null) {
			// fetch user session from a previous authentication
			
			String cacheKey = null;
			UserSession usess = null;
			String remoteAddr = request.getRemoteAddr();
			
			StringTokenizer st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				String basic = st.nextToken();

				// We only handle HTTP Basic authentication
				if (basic.equalsIgnoreCase("Basic")) {
					cacheKey = authHeader;
					usess = timedSessionCache.get(new CacheKey(remoteAddr, authHeader));
					if (usess == null || !usess.isAuthenticated()) {
						String credentials = st.nextToken();
						usess = handleBasicAuthentication(credentials, request);
					}
				} else if (basic.equalsIgnoreCase("Digest")) {
					int digestIndex = authHeader.indexOf("Digest");
					String digestInfos = authHeader.substring(digestIndex + 7);
					DigestAuthentication digestAuth = DigestAuthentication.parse(digestInfos);
					cacheKey = digestAuth.getUsername();
					usess = timedSessionCache.get(new CacheKey(remoteAddr, digestAuth.getUsername()));
					if (usess == null || !usess.isAuthenticated()) {
						usess = handleDigestAuthentication(digestInfos, request);
					}
				}
			}
	
			if(usess != null && cacheKey != null) {
				timedSessionCache.put(new CacheKey(remoteAddr, cacheKey), usess);
				return usess;
			}
		}

		// If the user was not validated or the browser does not know about the realm yet, fail with a
		// 401 status code (UNAUTHORIZED) and
		// pass back a WWW-Authenticate header for
		// this servlet.
		//
		// Note that this is the normal situation the
		// first time you access the page. The client
		// web browser will prompt for userID and password
		// and cache them so that it doesn't have to
		// prompt you again.

		if(request.isSecure() || Settings.isJUnitTest()) {
			response.addHeader("WWW-Authenticate", "Basic realm=\"" + BASIC_AUTH_REALM + "\"");
		}
		if(webdavModule.isDigestAuthenticationEnabled()) {
			String nonce = UUID.randomUUID().toString().replace("-", "");
			response.addHeader("WWW-Authenticate", "Digest realm=\"" + BASIC_AUTH_REALM + "\", qop=\"auth\", nonce=\"" + nonce + "\"");
		}
		response.setStatus(401);
		return null;
	}

	protected UserSession handleDigestAuthentication(String credentials, HttpServletRequest request) {
		DigestAuthentication digestAuth = DigestAuthentication.parse(credentials);
		Identity identity = webDAVAuthManager.digestAuthentication(request.getMethod(), digestAuth);
		if(identity != null) {
			return afterAuthorization(identity, request);
		}
		return null;
	}
	
	protected UserSession handleBasicAuthentication(String credentials, HttpServletRequest request) {
		// This example uses sun.misc.* classes.
		// You will need to provide your own
		// if you are not comfortable with that.
		String userPass = Base64Decoder.decode(credentials);

		// The decoded string is in the form
		// "userID:password".
		int p = userPass.indexOf(":");
		if (p != -1) {
			String userID = userPass.substring(0, p);
			String password = userPass.substring(p + 1);
			
			// Validate user ID and password
			// and set valid true if valid.
			// In this example, we simply check
			// that neither field is blank
			Identity identity = webDAVAuthManager.authenticate(null, userID, password);
			if (identity != null) {
				return afterAuthorization(identity, request);
			}
		}
		return null;
	}
	
	private UserSession afterAuthorization(Identity identity, HttpServletRequest request) {
		UserSession usess = sessionManager.getUserSession(request);
		synchronized(usess) {
			//double check to prevent severals concurrent login
			if(usess.isAuthenticated()) {
				return usess;
			}
		
			sessionManager.signOffAndClear(usess);
			usess.setIdentity(identity);
			UserDeletionManager.getInstance().setIdentityAsActiv(identity);
			// set the roles (admin, author, guest)
			Roles roles = BaseSecurityManager.getInstance().getRoles(identity);
			usess.setRoles(roles);
			// set authprovider
			//usess.getIdentityEnvironment().setAuthProvider(OLATAuthenticationController.PROVIDER_OLAT);
		
			// set session info
			SessionInfo sinfo = new SessionInfo(identity.getKey(), identity.getName(), request.getSession());
			User usr = identity.getUser();
			sinfo.setFirstname(usr.getProperty(UserConstants.FIRSTNAME, null));
			sinfo.setLastname(usr.getProperty(UserConstants.LASTNAME, null));
			sinfo.setFromIP(request.getRemoteAddr());
			sinfo.setFromFQN(request.getRemoteAddr());
			try {
				InetAddress[] iaddr = InetAddress.getAllByName(request.getRemoteAddr());
				if (iaddr.length > 0) sinfo.setFromFQN(iaddr[0].getHostName());
			} catch (UnknownHostException e) {
				 // ok, already set IP as FQDN
			}
			sinfo.setAuthProvider(BaseSecurityModule.getDefaultAuthProviderIdentifier());
			sinfo.setUserAgent(request.getHeader("User-Agent"));
			sinfo.setSecure(request.isSecure());
			sinfo.setWebDAV(true);
			sinfo.setWebModeFromUreq(null);
			// set session info for this session
			usess.setSessionInfo(sinfo);
			//
			sessionManager.signOn(usess);
			return usess;
		}
	}
	
	/**
	 * @see org.olat.core.commons.services.webdav.WebDAVManager#isEnabled()
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Spring setter method to enable/disable the webDAV module
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		WebDAVManagerImpl.enabled = enabled;
	}



}