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
package org.olat.restapi.repository.course;

import static org.olat.restapi.security.RestSecurityHelper.getIdentity;
import static org.olat.restapi.security.RestSecurityHelper.getUserRequest;
import static org.olat.restapi.security.RestSecurityHelper.isAuthor;
import static org.olat.restapi.security.RestSecurityHelper.isAuthorEditor;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.admin.securitygroup.gui.IdentitiesAddEvent;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.commons.calendar.restapi.CalWebService;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.mail.MailPackage;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.config.CourseConfig;
import org.olat.course.nodes.cal.CourseCalendars;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.AccessResult;
import org.olat.restapi.security.RestSecurityHelper;
import org.olat.restapi.support.ErrorWindowControl;
import org.olat.restapi.support.ObjectFactory;
import org.olat.restapi.support.vo.CourseConfigVO;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.user.restapi.UserVO;
import org.olat.user.restapi.UserVOFactory;

import com.thoughtworks.xstream.XStream;

/**
 * Description:<br>
 * This web service will handle the functionality related to <code>Course</code>
 * and its contents.
 * 
 * <P>
 * Initial Date:  27 apr. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
@Path("repo/courses/{courseId}")
public class CourseWebService {

	private static final OLog log = Tracing.createLoggerFor(CourseWebService.class);
	private static final XStream myXStream = XStreamHelper.createXStreamInstance();
	
	private static final String VERSION = "1.0";
	
	public static CacheControl cc = new CacheControl();

	static {
		cc.setMaxAge(-1);
	}

	/**
	 * The version of the Course Web Service
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
	
	@Path("groups")
	public CourseGroupWebService getCourseGroupWebService(@PathParam("courseId") Long courseId) {
		OLATResource ores = getCourseOLATResource(courseId);
		if(ores == null) {
			throw new WebApplicationException(Response.serverError().status(Status.NOT_FOUND).build());
		}
		return new CourseGroupWebService(ores);
	}
	
	@Path("calendar")
	public CalWebService getCourseCalendarWebService(@PathParam("courseId") Long courseId,
			@Context HttpServletRequest request) {
		ICourse course = loadCourse(courseId);
		if(course == null) {
			throw new WebApplicationException(Response.serverError().status(Status.NOT_FOUND).build());
		}
		if(course.getCourseConfig().isCalendarEnabled()) {
			OLATResource ores = getCourseOLATResource(courseId);
			UserRequest ureq = getUserRequest(request);
			KalendarRenderWrapper wrapper = CourseCalendars.getCourseCalendarWrapper(ureq, ores, null);
			return new CalWebService(wrapper);
		}
		return null;
	}

	/**
	 * Publish the course.
	 * @response.representation.200.qname {http://www.example.com}courseVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The metadatas of the created course
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param locale The course locale
	 * @param request The HTTP request
	 * @return It returns the metadatas of the published course.
	 */
	@POST
	@Path("publish")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response publishCourse(@PathParam("courseId") Long courseId, @QueryParam("locale") Locale locale,
			@QueryParam("access") Integer access, @QueryParam("membersOnly") Boolean membersOnly,
			@Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		UserRequest ureq = getUserRequest(request);
		ICourse course = loadCourse(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		int newAccess = access == null ? RepositoryEntry.ACC_USERS : access.intValue();
		boolean members = membersOnly == null ? false : membersOnly.booleanValue();
		CourseFactory.publishCourse(course, newAccess, members, ureq.getIdentity(), locale);
		CourseVO vo = ObjectFactory.get(course);
		return Response.ok(vo).build();
	}
	


	/**
	 * Get the metadatas of the course by id
	 * @response.representation.200.qname {http://www.example.com}courseVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The metadatas of the created course
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSEVO}
   * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @return It returns the <code>CourseVO</code> object representing the course.
	 */
	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response findById(@PathParam("courseId") Long courseId, @Context HttpServletRequest httpRequest) {
		ICourse course = loadCourse(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isCourseAccessible(course, false, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		CourseVO vo = ObjectFactory.get(course);
		return Response.ok(vo).build();
	}
	
  /**
   * Export the course
   * @response.representation.200.mediaType application/zip
   * @response.representation.200.doc The course as a ZIP file
   * @response.representation.401.doc Not authorized to export the course
   * @response.representation.404.doc The course not found
   * @param courseId The course resourceable's id
	 * @return It returns the <code>CourseVO</code> object representing the course.
	 */

	
	@GET
	@Path("file")
	@Produces({ "application/zip", MediaType.APPLICATION_OCTET_STREAM })
	public Response getRepoFileById(@PathParam("courseId") Long courseId, @Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		ICourse course = loadCourse(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}
		
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry re = rm.lookupRepositoryEntry(course, true);
		if (re == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}

		RepositoryHandler typeToDownload = RepositoryHandlerFactory.getInstance().getRepositoryHandler(re);
		if (typeToDownload == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}

		OLATResource ores = OLATResourceManager.getInstance().findResourceable(re.getOlatResource());
		if (ores == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}

		Identity identity = getIdentity(request);
		boolean isAuthor = RestSecurityHelper.isAuthor(request);
		boolean isOwner = RepositoryManager.getInstance().isOwnerOfRepositoryEntry(identity, re);
		if (!(isAuthor | isOwner)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		boolean canDownload = re.getCanDownload() && typeToDownload.supportsDownload(re);
		if (!canDownload) {
			return Response.serverError().status(Status.NOT_ACCEPTABLE).build();
		}

		boolean isAlreadyLocked = typeToDownload.isLocked(ores);
		LockResult lockResult = null;
		try {
			lockResult = typeToDownload.acquireLock(ores, identity);
			if (lockResult == null || (lockResult != null && lockResult.isSuccess() && !isAlreadyLocked)) {
				MediaResource mr = typeToDownload.getAsMediaResource(ores, false);
				if (mr != null) {
					RepositoryManager.getInstance().incrementDownloadCounter(re);
					return Response.ok(mr.getInputStream()).cacheControl(cc).build(); // success
				} else {
					return Response.serverError().status(Status.NO_CONTENT).build();
				}
			} else {
				return Response.serverError().status(Status.CONFLICT).build();
			}
		} finally {
			if ((lockResult != null && lockResult.isSuccess() && !isAlreadyLocked)) {
				typeToDownload.releaseLock(lockResult);
			}
		}
	}

	/**
	 * Delete a course by id
   * @response.representation.200.doc The metadatas of the created course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param request The HTTP request
	 * @return It returns the XML representation of the <code>Structure</code>
	 *         object representing the course.
	 */
	@DELETE
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response deleteCourse(@PathParam("courseId") Long courseId, @Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		ICourse course = loadCourse(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		UserRequest ureq = getUserRequest(request);
		
		//fxdiff
		ErrorWindowControl error = new ErrorWindowControl();
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry re = rm.lookupRepositoryEntry(course, true);
		rm.deleteRepositoryEntryWithAllData(ureq, error, re);
		
		return Response.ok().build();
	}
	
	/**
	 * Get the configuration of the course
	 * @response.representation.200.qname {http://www.example.com}courseConfigVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The configuration of the course
   * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSECONFIGVO}
   * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param request The HTTP request
	 * @return It returns the XML representation of the <code>Structure</code>
	 *         object representing the course.
	 */
	@GET
	@Path("configuration")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getConfiguration(@PathParam("courseId") Long courseId, @Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		ICourse course = loadCourse(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		CourseConfigVO vo = ObjectFactory.getConfig(course);
		return Response.ok(vo).build();
	}
	
	/**
	 * Update the course configuration
	 * @response.representation.200.qname {http://www.example.com}courseConfigVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The metadatas of the created course
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_COURSECONFIGVO}
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
	 * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param calendar Enable/disable the calendar (value: true/false) (optional)
	 * @param chat Enable/disable the chat (value: true/false) (optional)
	 * @param cssLayoutRef Set the custom CSS file for the layout (optional)
	 * @param efficencyStatement Enable/disable the efficencyStatement (value: true/false) (optional)
	 * @param glossarySoftkey Set the glossary (optional)
	 * @param sharedFolderSoftkey Set the shared folder (optional)
	 * @param request The HTTP request
	 * @return It returns the XML/Json representation of the <code>CourseConfig</code>
	 *         object representing the course configuration.
	 */
	@POST
	@Path("configuration")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response updateConfiguration(@PathParam("courseId") Long courseId,
			@FormParam("calendar") Boolean calendar, @FormParam("chat") Boolean chat,
			@FormParam("cssLayoutRef") String cssLayoutRef, @FormParam("efficencyStatement") Boolean efficencyStatement,
			@FormParam("glossarySoftkey") String glossarySoftkey, @FormParam("sharedFolderSoftkey") String sharedFolderSoftkey,
			@Context HttpServletRequest request) {
		if(!isAuthor(request)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		ICourse course = CourseFactory.openCourseEditSession(courseId);
		//change course config
		CourseConfig courseConfig = course.getCourseEnvironment().getCourseConfig();
		if(calendar != null) {
			courseConfig.setCalendarEnabled(calendar.booleanValue());
		}
		if(chat != null) {
			courseConfig.setChatIsEnabled(chat.booleanValue());
		}
		if(StringHelper.containsNonWhitespace(cssLayoutRef)) {
			courseConfig.setCssLayoutRef(cssLayoutRef);
		}
		if(efficencyStatement != null) {
			courseConfig.setEfficencyStatementIsEnabled(efficencyStatement.booleanValue());
		}
		if(StringHelper.containsNonWhitespace(glossarySoftkey)) {
			courseConfig.setGlossarySoftKey(glossarySoftkey);
		}
		if(StringHelper.containsNonWhitespace(sharedFolderSoftkey)) {
			courseConfig.setSharedFolderSoftkey(sharedFolderSoftkey);
		}

		CourseFactory.setCourseConfig(course.getResourceableId(), courseConfig);
		CourseFactory.closeCourseEditSession(course.getResourceableId(),true);
		
		CourseConfigVO vo = ObjectFactory.getConfig(course);
		return Response.ok(vo).build();
	}
	
	/**
	 * Get the runstructure of the course by id
   * @response.representation.200.mediaType application/xml
   * @response.representation.200.doc The run structure of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return It returns the XML representation of the <code>Structure</code>
	 *         object representing the course.
	 */
	@GET
	@Path("runstructure")
	@Produces(MediaType.APPLICATION_XML)
	public Response findRunStructureById(@PathParam("courseId") Long courseId, @Context HttpServletRequest httpRequest, @Context Request request) {
		if(!isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		ICourse course = loadCourse(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		VFSItem runStructureItem = course.getCourseBaseContainer().resolve("runstructure.xml");
		Date lastModified = new Date(runStructureItem.getLastModified());
		
		Response.ResponseBuilder response = request.evaluatePreconditions(lastModified);
		if(response == null) {
			return Response.ok(myXStream.toXML(course.getRunStructure())).build();
		}
		return response.build();	
	}
	
	/**
	 * Get the editor tree model of the course by id
   * @response.representation.200.mediaType application/xml
   * @response.representation.200.doc The editor tree model of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return It returns the XML representation of the <code>Editor model</code>
	 *         object representing the course.
	 */
	@GET
	@Path("editortreemodel")
	@Produces(MediaType.APPLICATION_XML)
	public Response findEditorTreeModelById(@PathParam("courseId") Long courseId, @Context HttpServletRequest httpRequest, @Context Request request) {
		if(!isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		ICourse course = loadCourse(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		VFSItem editorModelItem = course.getCourseBaseContainer().resolve("editortreemodel.xml");
		Date lastModified = new Date(editorModelItem.getLastModified());
		
		Response.ResponseBuilder response = request.evaluatePreconditions(lastModified);
		if(response == null) {
			return Response.ok(myXStream.toXML(course.getEditorTreeModel())).build();
		}
		return response.build();
	}
	
	/**
	 * Get all owners and authors of the course
	 * @response.representation.200.qname {http://www.example.com}userVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The array of authors
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course not found
	 * @param courseId The course resourceable's id
	 * @param httpRequest The HTTP request
	 * @return It returns an array of <code>UserVO</code>
	 */
	@GET
	@Path("authors")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getAuthors(@PathParam("courseId") Long courseId, @Context HttpServletRequest httpRequest) {
		if(!isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		OLATResourceable course = getCourseOLATResource(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		SecurityGroup sg = repositoryEntry.getOwnerGroup();

		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		List<Identity> owners = securityManager.getIdentitiesOfSecurityGroup(sg);
		
		int count = 0;
		UserVO[] authors = new UserVO[owners.size()];
		for(Identity owner:owners) {
			authors[count++] = UserVOFactory.get(owner);
		}
		return Response.ok(authors).build();
	}
	
	/**
	 * Get this specific author and owner of the course
	 * @response.representation.200.qname {http://www.example.com}userVO
   * @response.representation.200.mediaType application/xml, application/json
   * @response.representation.200.doc The author
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course not found or the user is not an onwer or author of the course
	 * @param courseId The course resourceable's id
	 * @param identityKey The user identifier
	 * @param httpRequest The HTTP request
	 * @return It returns an <code>UserVO</code>
	 */
	@GET
	@Path("authors/{identityKey}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getAuthor(@PathParam("courseId") Long courseId, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest httpRequest) {
		if(!isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		OLATResourceable course = getCourseOLATResource(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		SecurityGroup sg = repositoryEntry.getOwnerGroup();
		
		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		SecurityGroup authorGroup = securityManager.findSecurityGroupByName(Constants.GROUP_AUTHORS);

		Identity author = securityManager.loadIdentityByKey(identityKey, false);
		if(securityManager.isIdentityInSecurityGroup(author, sg) &&
				securityManager.isIdentityInSecurityGroup(author, authorGroup)) {
			UserVO vo = UserVOFactory.get(author);
			return Response.ok(vo).build();
		}
		return Response.ok(author).build();
	}
	
	/**
	 * Add an owner and author to the course
   * @response.representation.200.doc The user is an author and owner of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course or the user not found
	 * @param courseId The course resourceable's id
	 * @param identityKey The user identifier
	 * @param httpRequest The HTTP request
	 * @return It returns 200  if the user is added as owner and author of the course
	 */
	@PUT
	@Path("authors/{identityKey}")
	public Response addAuthor(@PathParam("courseId") Long courseId, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest httpRequest) {
		if(!isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		OLATResourceable course = getCourseOLATResource(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}  else if (!isAuthorEditor(course, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		Identity author = securityManager.loadIdentityByKey(identityKey, false);
		if(author == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}
		
		Identity identity = getIdentity(httpRequest);

		SecurityGroup authorGroup = securityManager.findSecurityGroupByName(Constants.GROUP_AUTHORS);
		boolean hasBeenAuthor = securityManager.isIdentityInSecurityGroup(author, authorGroup);
		if(!hasBeenAuthor) {
			//not an author already, add this identity to the security group "authors"
			securityManager.addIdentityToSecurityGroup(author, authorGroup);
			log.audit("User::" + identity.getName() + " added system role::" + Constants.GROUP_AUTHORS + " to user::" + author.getName() + " via addAuthor method in course REST API", null);
		}
		
		//add the author as owner of the course
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		List<Identity> authors = Collections.singletonList(author);
		IdentitiesAddEvent identitiesAddedEvent = new IdentitiesAddEvent(authors);
		rm.addOwners(identity, identitiesAddedEvent, repositoryEntry);
		
		return Response.ok().build();
	}
	
	/**
	 * Remove an owner and author to the course
   * @response.representation.200.doc The user was successfully removed as owner of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course or the user not found
	 * @param courseId The course resourceable's id
	 * @param identityKey The user identifier
	 * @param httpRequest The HTTP request
	 * @return It returns 200  if the user is removed as owner of the course
	 */
	@DELETE
	@Path("authors/{identityKey}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response removeAuthor(@PathParam("courseId") Long courseId, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest httpRequest) {
		if(!isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		OLATResourceable course = getCourseOLATResource(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!isAuthorEditor(course, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		Identity author = securityManager.loadIdentityByKey(identityKey, false);
		if(author == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}
		
		Identity identity = getIdentity(httpRequest);
		
		//remove the author as owner of the course
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		List<Identity> authors = Collections.singletonList(author);
		rm.removeOwners(identity, authors, repositoryEntry);
		
		return Response.ok().build();
	}
	
	/**
	 * Add a coach to the course
   * @response.representation.200.doc The user is a coach of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course or the user not found
	 * @param courseId The course resourceable's id
	 * @param identityKey The user identifier
	 * @param httpRequest The HTTP request
	 * @return It returns 200  if the user is added as coach of the course
	 */
	@PUT
	@Path("tutors/{identityKey}")
	public Response addCoach(@PathParam("courseId") Long courseId, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest httpRequest) {
		if(!isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		OLATResourceable course = getCourseOLATResource(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}  else if (!isAuthorEditor(course, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		Identity tutor = securityManager.loadIdentityByKey(identityKey, false);
		if(tutor == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}
		
		Identity identity = getIdentity(httpRequest);
		UserRequest ureq = getUserRequest(httpRequest);
		
		//add the author as owner of the course
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		List<Identity> tutors = Collections.singletonList(tutor);
		IdentitiesAddEvent iae = new IdentitiesAddEvent(tutors);
		rm.addTutors(identity, ureq.getUserSession().getRoles(), iae, repositoryEntry, new MailPackage(false));
		
		return Response.ok().build();
	}
	
	/**
	 * Add an participant to the course
   * @response.representation.200.doc The user is a participant of the course
	 * @response.representation.401.doc The roles of the authenticated user are not sufficient
   * @response.representation.404.doc The course or the user not found
	 * @param courseId The course resourceable's id
	 * @param identityKey The user identifier
	 * @param httpRequest The HTTP request
	 * @return It returns 200  if the user is added as owner and author of the course
	 */
	@PUT
	@Path("participants/{identityKey}")
	public Response addParticipant(@PathParam("courseId") Long courseId, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest httpRequest) {
		if(!isAuthor(httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		OLATResourceable course = getCourseOLATResource(courseId);
		if(course == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}  else if (!isAuthorEditor(course, httpRequest)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		Identity participant = securityManager.loadIdentityByKey(identityKey, false);
		if(participant == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		}
		
		Identity identity = getIdentity(httpRequest);
		UserRequest ureq = getUserRequest(httpRequest);
		
		//add the author as owner of the course
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(course, true);
		List<Identity> participants = Collections.singletonList(participant);
		IdentitiesAddEvent iae = new IdentitiesAddEvent(participants);
		rm.addParticipants(identity, ureq.getUserSession().getRoles(), iae, repositoryEntry, new MailPackage(false));
		
		return Response.ok().build();
	}
	
	private OLATResource getCourseOLATResource(Long courseId) {
		String typeName = OresHelper.calculateTypeName(CourseModule.class);
		OLATResource ores = OLATResourceManager.getInstance().findResourceable(courseId, typeName);
		if(ores == null && Settings.isJUnitTest()) {
			//hack for the BGContextManagerImpl which load the course
			ores = OLATResourceManager.getInstance().findResourceable(courseId, "junitcourse");
		}
		return ores;
	}
	
	public static boolean isCourseAccessible(ICourse course, boolean authorRightsMandatory, HttpServletRequest request) {
		if(authorRightsMandatory && !isAuthor(request)) {
			return false;
		}

		Identity identity = getIdentity(request);
		RepositoryEntry entry = RepositoryManager.getInstance().lookupRepositoryEntry(course, true);
		ACService acManager = CoreSpringFactory.getImpl(ACService.class);
		AccessResult result = acManager.isAccessible(entry, identity, false);
		if(result.isAccessible()) {
			return true;
		}
		return false;
	}
	
	public static ICourse loadCourse(Long courseId) {
		try {
			ICourse course = CourseFactory.loadCourse(courseId);
			return course;
		} catch(Exception ex) {
			log.error("cannot load course with id: " + courseId, ex);
			return null;
		}
	}
}
