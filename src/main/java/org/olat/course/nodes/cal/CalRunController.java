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

package org.olat.course.nodes.cal;

import java.util.Date;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.ui.events.KalendarModifiedEvent;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.fullWebApp.popup.BaseFullWebappPopupLayoutFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.control.generic.clone.CloneController;
import org.olat.core.gui.control.generic.clone.CloneLayoutControllerCreatorCallback;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.nodes.CalCourseNode;
import org.olat.course.run.calendar.CourseCalendarSubscription;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.modules.ModuleConfiguration;

/**
 * 
 * <h3>Description:</h3>
 * Run controller for calendar
 * <p>
 * Initial Date:  4 nov. 2009 <br>
 * @author srosse, stephane.rosse@frentix.com, www.frentix.com
 */
public class CalRunController extends BasicController {

	private final VelocityContainer mainVC;
	
	private CourseEnvironment courseEnv;
	private CourseCalendarController calCtr;
	private ModuleConfiguration config;
	private CloneController cloneCtr;
	
	/**
	 * 
	 * @param wControl
	 * @param ureq
	 * @param calCourseNode
	 * @param cenv
	 */
	public CalRunController(WindowControl wControl, UserRequest ureq, CalCourseNode calCourseNode, CourseEnvironment cenv, NodeEvaluation ne) {
		super(ureq, wControl);
		this.courseEnv = cenv;
		this.config = calCourseNode.getModuleConfiguration();
		mainVC = createVelocityContainer("run");

		ICourse course = CourseFactory.loadCourse(cenv.getCourseResourceableId());
		CourseCalendars myCal = CourseCalendars.createCourseCalendarsWrapper(ureq, wControl, course, ne);
		CourseCalendarSubscription calSubscription = myCal.createSubscription(ureq);
		if(CalEditController.getAutoSubscribe(config)) {
			if(!calSubscription.isSubscribed()) {
				calSubscription.subscribe(false);
				ureq.getUserSession().getSingleUserEventCenter().fireEventToListenersOf(new KalendarModifiedEvent(), OresHelper.lookupType(CalendarManager.class));
			}
		}
		
		calCtr = new CourseCalendarController(ureq, wControl, myCal, calSubscription, course, ne);

		boolean focused = false;
		ContextEntry ce = wControl.getBusinessControl().popLauncherContextEntry();
		if ( ce != null ) { // a context path is left for me
			OLATResourceable ores = ce.getOLATResourceable();
			String typeName = ores.getResourceableTypeName();
			String eventId = typeName.substring("path=".length());
			if(eventId.indexOf(':') > 0) {
				eventId = eventId.substring(0, eventId.indexOf(':'));
			}
			if  (eventId.length() > 0) {
				calCtr.setFocusOnEvent(eventId);
				focused = true;
			}
		}
		if(!focused) {
			Date startDate = null;
			if(CalEditController.getAutoDate(config)) {
				startDate = new Date();
			} else {
				startDate = CalEditController.getStartDate(config);
				if(startDate == null) startDate = new Date();
			}
			calCtr.setFocus(startDate);
		}

		CloneLayoutControllerCreatorCallback clccc = new CloneLayoutControllerCreatorCallback() {
			public ControllerCreator createLayoutControllerCreator(UserRequest ureq, final ControllerCreator contentControllerCreator) {
				return BaseFullWebappPopupLayoutFactory.createAuthMinimalPopupLayout(ureq, new ControllerCreator() {
					@SuppressWarnings("synthetic-access")
					public Controller createController(UserRequest lureq, WindowControl lwControl) {
						// wrapp in column layout, popup window needs a layout controller
						Controller ctr = contentControllerCreator.createController(lureq, lwControl);
						LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(lureq, lwControl, null, null, ctr.getInitialComponent(),
								null);
						layoutCtr.setCustomCSS(CourseFactory.getCustomCourseCss(lureq.getUserSession(), courseEnv));
						layoutCtr.addDisposableChildController(ctr);
						return layoutCtr;
					}
				});
			}
		};
		cloneCtr	 = new CloneController(ureq, getWindowControl(), calCtr, clccc);
		
		mainVC.put("cal", cloneCtr.getInitialComponent());
		putInitialPanel(mainVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		//no events yet
	}
	/**
	 * 
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Controller source, Event event) {
		//
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		if(calCtr != null){
			calCtr.dispose();
			calCtr = null;
		}
		if(cloneCtr != null){
			cloneCtr.dispose();
			cloneCtr = null;
		}
	}
}