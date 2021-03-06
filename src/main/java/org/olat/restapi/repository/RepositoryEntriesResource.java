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
package org.olat.restapi.repository;


import static org.olat.restapi.security.RestSecurityHelper.getIdentity;
import static org.olat.restapi.security.RestSecurityHelper.getRoles;
import static org.olat.restapi.security.RestSecurityHelper.isAuthor;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.FileResource;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.SearchRepositoryEntryParameters;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.restapi.security.RestSecurityHelper;
import org.olat.restapi.support.MediaTypeVariants;
import org.olat.restapi.support.MultipartReader;
import org.olat.restapi.support.ObjectFactory;
import org.olat.restapi.support.vo.RepositoryEntryVO;
import org.olat.restapi.support.vo.RepositoryEntryVOes;

/**
 * Description:<br>
 * This handles the repository entries
 * 
 * <P>
 * Initial Date: 19.05.2009 <br>
 * 
 * @author patrickb, srosse, stephane.rosse@frentix.com
 */
@Path("repo/entries")
public class RepositoryEntriesResource {
	
	private static final OLog log = Tracing.createLoggerFor(RepositoryEntriesResource.class);
	private static final String VERSION = "1.0";
	
	/**
	 * The version number of this web service
	 * @return
	 */
	@GET
	@Path("version")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getVersion() {
		return Response.ok(VERSION).build();
	}

	/**
	 * List all entries in the OLAT repository
	 * @response.representation.200.qname {http://www.example.com}repositoryEntryVO
   * @response.representation.200.mediaType text/plain, text/html, application/xml, application/json
   * @response.representation.200.doc List all entries in the repository
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_REPOENTRYVOes}
	 * @param uriInfo The URI information
	 * @param httpRequest The HTTP request
	 * @return
	 */
	@GET
	@Produces({MediaType.TEXT_HTML, MediaType.TEXT_PLAIN})
	public Response getEntriesText(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest) {
		try {
			// list of courses open for everybody
			Roles roles = getRoles(httpRequest);
			Identity identity = getIdentity(httpRequest);

			//fxdiff VCRP-1,2: access control of resources
			SearchRepositoryEntryParameters params = new SearchRepositoryEntryParameters(identity, roles);
			List<RepositoryEntry> coursRepos = RepositoryManager.getInstance().genericANDQueryWithRolesRestriction(params, 0, -1, false);
			
			StringBuilder sb = new StringBuilder();
			sb.append("Course List\n");
			for (RepositoryEntry repoE : coursRepos) {
				UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
				URI repoUri = baseUriBuilder.path(RepositoryEntriesResource.class)
					.path(repoE.getKey().toString())
					.build();
				
				sb.append("<a href=\"").append(repoUri).append(">")
					.append(repoE.getDisplayname()).append("(").append(repoE.getKey()).append(")")
					.append("</a>").append("\n");
			}
			
			return Response.ok(sb.toString()).build();
		} catch(Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * List all entries in the OLAT repository
	 * @response.representation.200.qname {http://www.example.com}repositoryEntryVO
   * @response.representation.200.mediaType text/plain, text/html, application/xml, application/json
   * @response.representation.200.doc List all entries in the repository
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_REPOENTRYVOes}
	 * @param start (optional)
	 * @param limit (optional)
	 * @param managed (optional)
	 * @param externalId External ID (optional)
	 * @param externalRef External reference number (optional)
	 * @param resourceType The resource type (CourseModule) (optional)
	 * @param httpRequest The HTTP request
	 * @param request The RESt request
	 * @return
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response getEntries(@QueryParam("start") @DefaultValue("0") Integer start,
			@QueryParam("limit") @DefaultValue("25") Integer limit,
			@QueryParam("managed") Boolean managed, @QueryParam("externalId") String externalId,
			@QueryParam("externalRef") String externalRef, @QueryParam("resourceType") String resourceType,
			@Context HttpServletRequest httpRequest, @Context Request request) {
		try {
			// list of courses open for everybody
			Roles roles = getRoles(httpRequest);
			Identity identity = getIdentity(httpRequest);
			RepositoryManager rm = RepositoryManager.getInstance();
			SearchRepositoryEntryParameters params = new SearchRepositoryEntryParameters(identity, roles);
			params.setManaged(managed);
			if(StringHelper.containsNonWhitespace(externalId)) {
				params.setExternalId(externalId);
			}
			if(StringHelper.containsNonWhitespace(externalRef)) {
				params.setExternalRef(externalRef);
			}
			if(StringHelper.containsNonWhitespace(resourceType)) {
				params.setResourceTypes(Collections.singletonList(resourceType));
			}
			
			if(MediaTypeVariants.isPaged(httpRequest, request)) {
				int totalCount = rm.countGenericANDQueryWithRolesRestriction(params, true);
				List<RepositoryEntry> res = rm.genericANDQueryWithRolesRestriction(params, start, limit, true);
				RepositoryEntryVOes voes = new RepositoryEntryVOes();
				voes.setRepositoryEntries(toArrayOfVOes(res));
				voes.setTotalCount(totalCount);
				return Response.ok(voes).build();
			} else {
				List<RepositoryEntry> res = rm.genericANDQueryWithRolesRestriction(params, 0, -1, false);
				RepositoryEntryVO[] voes = toArrayOfVOes(res);
				return Response.ok(voes).build();
			}
		} catch(Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	private RepositoryEntryVO[] toArrayOfVOes(List<RepositoryEntry> coursRepos) {
		int i=0;
		RepositoryEntryVO[] entryVOs = new RepositoryEntryVO[coursRepos.size()];
		for (RepositoryEntry repoE : coursRepos) {
			entryVOs[i++] = ObjectFactory.get(repoE);
		}
		return entryVOs;
	}

	/**
	 * Search for repository entries, possible search attributes are name, author and type
	 * @response.representation.mediaType multipart/form-data
   * @response.representation.doc Search for repository entries
	 * @response.representation.200.qname {http://www.example.com}repositoryEntryVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc Search for repository entries
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_REPOENTRYVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @param type Filter by the file resource type of the repository entry
	 * @param author Filter by the author's username
	 * @param name Filter by name of repository entry
	 * @param myEntries Only search entries the requester owns
	 * @param httpRequest The HTTP request
	 * @return
	 */
	@GET
	@Path("search")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response searchEntries(@QueryParam("type") String type, @QueryParam("author") @DefaultValue("*") String author,
			@QueryParam("name") @DefaultValue("*") String name, @QueryParam("myentries") @DefaultValue("false") boolean myEntries,
			@Context HttpServletRequest httpRequest) {
		RepositoryManager rm = RepositoryManager.getInstance();
		try {
			List<RepositoryEntry> reposFound = new ArrayList<RepositoryEntry>();
			Identity identity = getIdentity(httpRequest);
			boolean restrictedType = type != null && !type.isEmpty();
			
			// list of courses open for everybody
			Roles roles = getRoles(httpRequest);
			
			if(myEntries) {
				List<RepositoryEntry> lstRepos = rm.queryByOwner(identity, restrictedType ? new String[] {type} : null);
				boolean restrictedName = !name.equals("*");
				boolean restrictedAuthor = !author.equals("*");
				if(restrictedName | restrictedAuthor) {
					// filter by search conditions
					for(RepositoryEntry re : lstRepos) {
						boolean nameOk = restrictedName ? re.getDisplayname().toLowerCase().contains(name.toLowerCase()) : true;
						boolean authorOk = restrictedAuthor ? re.getInitialAuthor().toLowerCase().equals(author.toLowerCase()) : true;
						if(nameOk & authorOk) reposFound.add(re);
					}
				} else {
					if(!lstRepos.isEmpty()) reposFound.addAll(lstRepos);
				}
			} else {
				List<String> types = new ArrayList<String>(1);
				if(restrictedType) types.add(type);

				SearchRepositoryEntryParameters params = new SearchRepositoryEntryParameters(name, author, null, restrictedType ? types : null, identity, roles, null);
				List<RepositoryEntry> lstRepos = rm.genericANDQueryWithRolesRestriction(params, 0, -1, false);
				if(!lstRepos.isEmpty()) reposFound.addAll(lstRepos);
			}
			
			int i=0;
			RepositoryEntryVO[] reVOs = new RepositoryEntryVO[reposFound.size()];
			for (RepositoryEntry re : reposFound) {
				reVOs[i++] = ObjectFactory.get(re);
			}
			return Response.ok(reVOs).build();
		} catch(Exception e) {
			throw new WebApplicationException(e);
		}
	}
	
	/**
	 * Import a resource in the repository
   * @response.representation.mediaType multipart/form-data
   * @response.representation.doc The file, its name and the resourcename
	 * @response.representation.200.qname {http://www.example.com}repositoryEntryVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc Import the resource and return the repository entry
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_REPOENTRYVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @param filename The name of the imported file
	 * @param file The file input stream
   * @param resourcename The name of the resource
   * @param displayname The display name
   * @param softkey The soft key (can be null)
	 * @param request The HTTP request
	 * @return
	 */
	@PUT
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Consumes({MediaType.MULTIPART_FORM_DATA})
	public Response putResource(@Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		MultipartReader partsReader = null;
		try {
			Identity identity = RestSecurityHelper.getUserRequest(request).getIdentity();
			partsReader = new MultipartReader(request);
			File tmpFile = partsReader.getFile();
			long length = tmpFile.length();
			if(length > 0) {
				Long accessRaw = partsReader.getLongValue("access");
				int access = accessRaw != null ? accessRaw.intValue() : RepositoryEntry.ACC_OWNERS;
				String softkey = partsReader.getValue("softkey");
				String resourcename = partsReader.getValue("resourcename");
				String displayname = partsReader.getValue("displayname");	
				RepositoryEntry re = importFileResource(identity, tmpFile, resourcename, displayname, softkey, access);
				RepositoryEntryVO vo = ObjectFactory.get(re);
				return Response.ok(vo).build();
			}
			return Response.serverError().status(Status.NO_CONTENT).build();
		} catch (Exception e) {
			log.error("Error while importing a file",e);
		} finally {
			MultipartReader.closeQuietly(partsReader);
		}
		return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
	}
	
	private RepositoryEntry importFileResource(Identity identity, File fResource, String resourcename, String displayname,
			String softkey, int access) {
		try {
			FileResourceManager frm = FileResourceManager.getInstance();
			FileResource newResource = frm.addFileResource(fResource, fResource.getName());
			return importResource(identity, newResource, resourcename, displayname, softkey, access);
		} catch(Exception e) {
			log.error("Fail to import a resource", e);
			throw new WebApplicationException(e);
		}
	}
		
	public static RepositoryEntry importResource(Identity identity, OLATResourceable newResource, String resourcename, String displayname,
			String softkey, int access) {

		RepositoryEntry addedEntry = RepositoryManager.getInstance().createRepositoryEntryInstance(identity.getName());
		addedEntry.setCanDownload(false);
		addedEntry.setCanLaunch(true);
		if(StringHelper.containsNonWhitespace(resourcename)) {
			addedEntry.setResourcename(resourcename);
		}
		if(StringHelper.containsNonWhitespace(displayname)) {
			addedEntry.setDisplayname(displayname);
		}
		if(StringHelper.containsNonWhitespace(softkey)) {
			addedEntry.setSoftkey(softkey);
		}
		// Do set access for owner at the end, because unfinished course should be
		// invisible
		// addedEntry.setAccess(RepositoryEntry.ACC_OWNERS);
		addedEntry.setAccess(0);// Access for nobody

		// Set the resource on the repository entry and save the entry.
		RepositoryManager rm = RepositoryManager.getInstance();
		OLATResource ores = OLATResourceManager.getInstance().findOrPersistResourceable(newResource);
		addedEntry.setOlatResource(ores);

		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		// create security group
		SecurityGroup newGroup = securityManager.createAndPersistSecurityGroup();
		// member of this group may modify member's membership
		securityManager.createAndPersistPolicy(newGroup, Constants.PERMISSION_ACCESS, newGroup);
		// members of this group are always authors also
		securityManager.createAndPersistPolicy(newGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);

		securityManager.addIdentityToSecurityGroup(identity, newGroup);
		addedEntry.setOwnerGroup(newGroup);
		
		//fxdiff VCRP-1,2: access control of resources
		// security group for tutors / coaches
		SecurityGroup tutorGroup = securityManager.createAndPersistSecurityGroup();
		// member of this group may modify member's membership
		securityManager.createAndPersistPolicy(tutorGroup, Constants.PERMISSION_ACCESS, addedEntry.getOlatResource());
		// members of this group are always tutors also
		securityManager.createAndPersistPolicy(tutorGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_TUTOR);
		addedEntry.setTutorGroup(tutorGroup);
		
		// security group for participants
		SecurityGroup participantGroup = securityManager.createAndPersistSecurityGroup();
		// member of this group may modify member's membership
		securityManager.createAndPersistPolicy(participantGroup, Constants.PERMISSION_ACCESS, addedEntry.getOlatResource());
		// members of this group are always participants also
		securityManager.createAndPersistPolicy(participantGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_PARTICIPANT);
		addedEntry.setParticipantGroup(participantGroup);
		
		// Do set access for owner at the end, because unfinished course should be
		// invisible
		if(access < RepositoryEntry.ACC_OWNERS || access > RepositoryEntry.ACC_USERS_GUESTS) {
			addedEntry.setAccess(RepositoryEntry.ACC_OWNERS);
		} else {
			addedEntry.setAccess(access);
		}
		rm.saveRepositoryEntry(addedEntry);
		return addedEntry;
	}
	
	@Path("{repoEntryKey}")
	public RepositoryEntryResource getRepositoryEntryResource() {
		RepositoryManager rm = RepositoryManager.getInstance();
		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		return new RepositoryEntryResource(rm, securityManager);
	}
}