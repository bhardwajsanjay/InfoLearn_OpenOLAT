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
package org.olat.restapi.group;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.core.CoreSpringFactory;
import org.olat.core.id.Identity;
import org.olat.core.util.StringHelper;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.group.model.SearchBusinessGroupParams;
import org.olat.restapi.support.MediaTypeVariants;
import org.olat.restapi.support.ObjectFactory;
import org.olat.restapi.support.vo.GroupInfoVO;
import org.olat.restapi.support.vo.GroupInfoVOes;
import org.olat.restapi.support.vo.GroupVO;
import org.olat.restapi.support.vo.GroupVOes;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  18 oct. 2011 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class MyGroupWebService {
	
	private final Identity retrievedUser;
	
	public MyGroupWebService(Identity retrievedUser) {
		this.retrievedUser = retrievedUser;
	}
	
	/**
	 * Return all groups of a user where the user is coach or participant.
	 * @response.representation.200.qname {http://www.example.com}groupVO
	 * @response.representation.200.mediaType application/xml, application/json
	 * @response.representation.200.doc The groups of the user
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPVOes}
	 * @response.representation.404.doc The identity not found
	 * @param start The first result
	 * @param limit The maximum results
	 * @param externalId Search with an external ID
	 * @param managed (true / false) Search only managed / not managed groups 
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return The list of groups informations
	 */
	@GET
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getUserGroupList(@QueryParam("start") @DefaultValue("0") Integer start, @QueryParam("limit") @DefaultValue("25") Integer limit,
			@QueryParam("externalId") String externalId, @QueryParam("managed") Boolean managed,
			@Context HttpServletRequest httpRequest, @Context Request request) {

		BusinessGroupService bgs = CoreSpringFactory.getImpl(BusinessGroupService.class);
		
		SearchBusinessGroupParams params = new SearchBusinessGroupParams(retrievedUser, true, true);
		if(StringHelper.containsNonWhitespace(externalId)) {
			params.setExternalId(externalId);
		}
		params.setManaged(managed);
		
		List<BusinessGroup> groups;
		if(MediaTypeVariants.isPaged(httpRequest, request)) {
			int totalCount = bgs.countBusinessGroups(params, null);
			groups = bgs.findBusinessGroups(params, null, start, limit);
			
			int count = 0;
			GroupVO[] groupVOs = new GroupVO[groups.size()];
			for(BusinessGroup group:groups) {
				groupVOs[count++] = ObjectFactory.get(group);
			}
			GroupVOes voes = new GroupVOes();
			voes.setGroups(groupVOs);
			voes.setTotalCount(totalCount);
			return Response.ok(voes).build();
		} else {
			groups = bgs.findBusinessGroups(params, null, 0, -1);
			
			int count = 0;
			GroupVO[] groupVOs = new GroupVO[groups.size()];
			for(BusinessGroup group:groups) {
				groupVOs[count++] = ObjectFactory.get(group);
			}
			return Response.ok(groupVOs).build();
		}
	}
	
	/**
	 * Return all groups with information of a user. Paging is mandatory!
	 * @response.representation.200.qname {http://www.example.com}groupInfoVO
	 * @response.representation.200.mediaType application/xml;pagingspec=1.0, application/json;pagingspec=1.0
	 * @response.representation.200.doc The groups of the user
	 * @response.representation.200.example {@link org.olat.restapi.support.vo.Examples#SAMPLE_GROUPINFOVOes}
	 * @response.representation.406.doc The request hasn't paging information
	 * @param start The first result
	 * @param limit The maximum results
	 * @param externalId Search with an external ID
	 * @param managed (true / false) Search only managed / not managed groups 
	 * @param httpRequest The HTTP request
	 * @param request The REST request
	 * @return The list of groups with additional informations
	 */
	@GET
	@Path("infos")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getUserGroupInfosList(@QueryParam("start") @DefaultValue("0") Integer start,
			@QueryParam("limit") @DefaultValue("25") Integer limit,
			@QueryParam("externalId") String externalId, @QueryParam("managed") Boolean managed,
			@Context HttpServletRequest httpRequest, @Context Request request) {

		BusinessGroupService bgs = CoreSpringFactory.getImpl(BusinessGroupService.class);
		
		SearchBusinessGroupParams params = new SearchBusinessGroupParams(retrievedUser, true, true);
		if(StringHelper.containsNonWhitespace(externalId)) {
			params.setExternalId(externalId);
		}
		params.setManaged(managed);
		
		List<BusinessGroup> groups;
		if(MediaTypeVariants.isPaged(httpRequest, request)) {
			int totalCount = bgs.countBusinessGroups(params, null);
			groups = bgs.findBusinessGroups(params, null, start, limit);
			
			int count = 0;
			GroupInfoVO[] groupVOs = new GroupInfoVO[groups.size()];
			for(BusinessGroup group:groups) {
				groupVOs[count++] = ObjectFactory.getInformation(group);
			}
			GroupInfoVOes voes = new GroupInfoVOes();
			voes.setGroups(groupVOs);
			voes.setTotalCount(totalCount);
			return Response.ok(voes).build();
		} else {
			return Response.serverError().status(Status.NOT_ACCEPTABLE).build();
		}
	}
}
