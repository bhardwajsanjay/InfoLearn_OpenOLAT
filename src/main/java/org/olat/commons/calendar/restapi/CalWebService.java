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
package org.olat.commons.calendar.restapi;

import static org.olat.restapi.security.RestSecurityHelper.getUserRequest;
import static org.olat.commons.calendar.restapi.CalendarWSHelper.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.util.StringHelper;

public class CalWebService {
	
	private final KalendarRenderWrapper calendar;
	
	public CalWebService(KalendarRenderWrapper calendar) {
		this.calendar = calendar;
	}
	
	@GET
	@Path("events")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response getEventsByCalendar(@PathParam("calendarId") String calendarId,
			@PathParam("identityKey") Long identityKey, @QueryParam("start") @DefaultValue("0") Integer start,
			@QueryParam("limit") @DefaultValue("25") Integer limit,
			@QueryParam("onlyFuture") @DefaultValue("false") Boolean onlyFuture,
			@Context HttpServletRequest httpRequest, @Context Request request) {
		
		UserRequest ureq = getUserRequest(httpRequest);
		if(!ureq.getUserSession().isAuthenticated()) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		if(calendar == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if (!hasReadAccess(calendar)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		List<EventVO> events = new ArrayList<EventVO>();
		Collection<KalendarEvent> kalEvents = calendar.getKalendar().getEvents();
		for(KalendarEvent kalEvent:kalEvents) {
			EventVO eventVo = new EventVO(kalEvent);
			events.add(eventVo);
		}

		return processEvents(events, onlyFuture, start, limit, httpRequest, request);
	}
	
	@DELETE
	@Path("events/{eventId}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response deleteEventByCalendar(@PathParam("calendarId") String calendarId,
			@PathParam("eventId") String eventId, @PathParam("identityKey") Long identityKey,
			@Context HttpServletRequest httpRequest) {
		
		UserRequest ureq = getUserRequest(httpRequest);
		if(!ureq.getUserSession().isAuthenticated()) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		if(calendar == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if(!hasWriteAccess(calendar)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}

		CalendarManager calendarManager = CalendarManagerFactory.getInstance().getCalendarManager();
		if(eventId == null) {
			return Response.ok().status(Status.NOT_FOUND).build();
		} else {
			KalendarEvent kalEvent = calendar.getKalendar().getEvent(eventId);
			if(kalEvent == null) {
				return Response.ok().status(Status.NOT_FOUND).build();
			} else {
				calendarManager.removeEventFrom(calendar.getKalendar(), kalEvent);
			}
		}

		return Response.ok().build();
	}
	
	@PUT
	@Path("events")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response putEventByCalendar(@PathParam("calendarId") String calendarId,
			EventVO event, @Context HttpServletRequest httpRequest) {
		return addEventByCalendar(calendarId, event, httpRequest);
	}
	
	@POST
	@Path("events")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response postEventByCalendar(@PathParam("calendarId") String calendarId,
			EventVO event, @Context HttpServletRequest httpRequest) {
		return addEventByCalendar(calendarId, event, httpRequest);
	}
	
	private Response addEventByCalendar(String calendarId, EventVO event, HttpServletRequest httpRequest) {
		UserRequest ureq = getUserRequest(httpRequest);
		if(!ureq.getUserSession().isAuthenticated()) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		if(calendar == null) {
			return Response.serverError().status(Status.NOT_FOUND).build();
		} else if(!hasWriteAccess(calendar)) {
			return Response.serverError().status(Status.UNAUTHORIZED).build();
		}
		
		KalendarEvent kalEvent;
		CalendarManager calendarManager = CalendarManagerFactory.getInstance().getCalendarManager();
		if(!StringHelper.containsNonWhitespace(event.getId())) {
			String id = UUID.randomUUID().toString();
			kalEvent = new KalendarEvent(id, event.getSubject(), event.getBegin(), event.getEnd());
			transfer(event, kalEvent);
			calendarManager.addEventTo(calendar.getKalendar(), kalEvent);
		} else {
			kalEvent = calendar.getKalendar().getEvent(event.getId());
			if(kalEvent == null) {
				kalEvent = new KalendarEvent(event.getId(), event.getSubject(), event.getBegin(), event.getEnd());
				transfer(event, kalEvent);
				calendarManager.addEventTo(calendar.getKalendar(), kalEvent);
			} else {
				kalEvent.setBegin(event.getBegin());
				kalEvent.setEnd(event.getEnd());
				kalEvent.setSubject(event.getSubject());
				transfer(event, kalEvent);
			}
		}
		
		EventVO vo = new EventVO(kalEvent);
		return Response.ok(vo).build();
	}
}
