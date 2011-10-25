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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.restapi.repository.course;

import static org.olat.restapi.security.RestSecurityHelper.isGroupManager;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.admin.quota.QuotaConstants;
import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.id.Identity;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.restapi.VFSWebServiceSecurityCallback;
import org.olat.core.util.vfs.restapi.VFSWebservice;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.PersistingCourseGroupManager;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.group.context.BGContext;
import org.olat.group.context.BGContextManager;
import org.olat.group.context.BGContextManagerImpl;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.restapi.ForumWebService;
import org.olat.resource.OLATResource;
import org.olat.restapi.group.LearningGroupWebService;
import org.olat.restapi.security.RestSecurityHelper;
import org.olat.restapi.support.ObjectFactory;
import org.olat.restapi.support.vo.GroupVO;

/**
 * 
 * Description:<br>
 * CourseGroupWebService
 * 
 * <P>
 * Initial Date:  7 apr. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
public class CourseGroupWebService {
	
	private static final String VERSION = "1.0";
	
	private final OLATResource course;
	
	public CourseGroupWebService(OLATResource ores) {
		this.course = ores;
	}
	
	/**
	 * Retrieves the version of the Course Group Web Service.
   * @response.representation.200.mediaType text/plain
   * @response.representation.200.doc The version of this specific Web Service
   * @response.representation.200.example 1.0
	 * @return
	 */
	@GET
	@Path("version")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getVersion() {
		return Response.ok(VERSION).build();
	}
	
	@Path("{groupKey}/folder")
	public VFSWebservice getFolder(@PathParam("groupKey") Long groupKey, @Context HttpServletRequest request) {
		BusinessGroupManager bgm = BusinessGroupManagerImpl.getInstance();
		BusinessGroup bg = bgm.loadBusinessGroup(groupKey, false);
		if(bg == null) {
			return null;
		}
		
		if(!isGroupManager(request)) {
			Identity identity = RestSecurityHelper.getIdentity(request);
			if(!bgm.isIdentityInBusinessGroup(identity, bg)) {
				return null;
			}
		}
		
		CollaborationTools collabTools = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(bg);
		if(!collabTools.isToolEnabled(CollaborationTools.TOOL_FOLDER)) {
			return null;
		}
		
		String relPath = collabTools.getFolderRelPath();
		QuotaManager qm = QuotaManager.getInstance();
		Quota folderQuota = qm.getCustomQuota(relPath);
		if (folderQuota == null) {
			Quota defQuota = qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_GROUPS);
			folderQuota = QuotaManager.getInstance().createQuota(relPath, defQuota.getQuotaKB(), defQuota.getUlLimitKB());
		}
		SubscriptionContext subsContext = null;
		VFSWebServiceSecurityCallback secCallback = new VFSWebServiceSecurityCallback(true, true, true, folderQuota, subsContext);
		OlatRootFolderImpl rootContainer = new OlatRootFolderImpl(relPath, null);
		rootContainer.setLocalSecurityCallback(secCallback);
		return new VFSWebservice(rootContainer);
	}
	
	/**
	 * Return the Forum web service
	 * @param groupKey The key of the group
	 * @param request The HTTP Request
	 * @return
	 */
	@Path("{groupKey}/forum")
	public ForumWebService getForum(@PathParam("groupKey") Long groupKey, @Context HttpServletRequest request) {
		BusinessGroupManager bgm = BusinessGroupManagerImpl.getInstance();
		BusinessGroup bg = bgm.loadBusinessGroup(groupKey, false);
		if(bg == null) {
			return null;
		}
		
		if(!isGroupManager(request)) {
			Identity identity = RestSecurityHelper.getIdentity(request);
			if(!bgm.isIdentityInBusinessGroup(identity, bg)) {
				return null;
			}
		}
		
		CollaborationTools collabTools = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(bg);
		if(collabTools.isToolEnabled(CollaborationTools.TOOL_FORUM)) {
			Forum forum = collabTools.getForum();
			return new ForumWebService(forum);
		}
		return null;
	}
	
	/**
	 * Lists all learn groups of the specified course.
	 * @response.representation.200.qname {http://www.example.com}groupVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The list of all learning group of the course
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPVOes}
   * @response.representation.404.doc The context of the group not found
	 * @param request The HTTP request
	 * @return
	 */
	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getGroupList(@Context HttpServletRequest request) {
		CourseGroupManager groupManager = PersistingCourseGroupManager.getInstance(course);
		List<BGContext> groupContexts = groupManager.getLearningGroupContexts();
		if(groupContexts.size() == 1) {
			BGContextManager contextManager = BGContextManagerImpl.getInstance();
			List<BusinessGroup> groups = contextManager.getGroupsOfBGContext(groupContexts.get(0));
			
			int count = 0;
			GroupVO[] vos = new GroupVO[groups.size()];
			for(BusinessGroup group:groups) {
				vos[count++] = ObjectFactory.get(group);
			}
			return Response.ok(vos).build();
		}
		return Response.serverError().status(Status.NOT_FOUND).build();
	}
	
	/**
	 * Creates a new group for the course.
	 * @response.representation.qname {http://www.example.com}groupVO
   * @response.representation.mediaType application/xml, application/json
   * @response.representation.doc A group to save
   * @response.representation.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPVO}
	 * @response.representation.200.qname {http://www.example.com}groupVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The persisted group
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPVO}
   * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @param group The group's metadatas
   * @param request The HTTP request
	 * @return
	 */
	@PUT
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response putNewGroup(GroupVO group, @Context HttpServletRequest request) {
		if(!RestSecurityHelper.isGroupManager(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		} else if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}

		UserRequest ureq = RestSecurityHelper.getUserRequest(request);
    BusinessGroupManager bgm = BusinessGroupManagerImpl.getInstance();
		CourseGroupManager groupManager = PersistingCourseGroupManager.getInstance(course);
		List<BGContext> groupContexts = groupManager.getLearningGroupContexts();
		if(groupContexts.size() == 1) {
			BGContext context = groupContexts.get(0);
			String name = group.getName();
			String desc = group.getDescription();
			Integer min = normalize(group.getMinParticipants());
			Integer max = normalize(group.getMaxParticipants());
			
			BusinessGroup bg = bgm.createAndPersistBusinessGroup(BusinessGroup.TYPE_LEARNINGROUP, ureq.getIdentity(), name, desc, min, max, false, false, context);
			GroupVO savedVO = ObjectFactory.get(bg);
			return Response.ok(savedVO).build();
		} else {
			//This case is ignored in the controller. Why???
			return Response.serverError().status(Status.NOT_FOUND).build();
		}
	}
	
	/**
	 * Fallback method for the browser.
	 * @response.representation.qname {http://www.example.com}groupVO
   * @response.representation.mediaType application/xml, application/json
   * @response.representation.doc A group to save
   * @response.representation.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPVO}
	 * @response.representation.200.qname {http://www.example.com}groupVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The persisted group
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @param group The group's metadatas
   * @param request The HTTP request
	 * @return
	 */
	@POST
	@Path("new")
	public Response postNewGroup(GroupVO group, @Context HttpServletRequest request) {
		return putNewGroup(group, request);
	}
	
	/**
	 * Retrieves the metadata of the specified group.
	 * @response.representation.200.qname {http://www.example.com}groupVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc This is the list of all groups in OLAT system
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPVO}
	 * @response.representation.404.doc The business group cannot be found
   * @param groupKey The group's id
   * @param request The REST request
   * @param httpRequest The HTTP request
	 * @return
	 */
	@GET
	@Path("{groupKey}")
	public Response getGroup(@PathParam("groupKey") Long groupKey, @Context Request request, @Context HttpServletRequest httpRequest) {
		//further security check: group is in the course
		return new LearningGroupWebService().findById(groupKey, request, httpRequest);
	}

	/**
	 * Updates the metadata for the specified group.
	 * @response.representation.qname {http://www.example.com}groupVO
   * @response.representation.mediaType application/xml, application/json
   * @response.representation.doc The group to update
   * @response.representation.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPVO}
	 * @response.representation.200.qname {http://www.example.com}groupVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The saved group
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The business group cannot be found
   * @param groupKey The group's id
   * @param group The group metadatas
   * @param request The HTTP request
	 * @return
	 */
	@POST
	@Path("{groupKey}")
	public Response updateGroup(@PathParam("groupKey") Long groupKey, GroupVO group, @Context HttpServletRequest request) {
		if(!isGroupManager(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		return new LearningGroupWebService().postGroup(groupKey, group, request);
	}
	
	/**
	 * Deletes the business group specified by the key of the group.
	 * @response.representation.200.doc The business group is deleted
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The business group cannot be found
	 * @param groupKey The group id
	 * @param request The HTTP request
	 * @return
	 */
	@DELETE
	@Path("{groupKey}")
	public Response deleteGroup(@PathParam("groupKey") Long groupKey, @Context HttpServletRequest request) {
		if(!isGroupManager(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		return new LearningGroupWebService().deleteGroup(groupKey, request);
	}
	
	/**
	 * @param integer
	 * @return value bigger or equal than 0
	 */
	private Integer normalize(Integer integer) {
	    //fxdiff FXOLAT-122: course management
		if(integer == null) return null;
		if(integer.intValue() < 0) return null;
		return integer;
	}
}