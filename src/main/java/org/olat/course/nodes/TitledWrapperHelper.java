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
package org.olat.course.nodes;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.title.TitleInfo;
import org.olat.core.gui.control.generic.title.TitledWrapperController;
import org.olat.core.util.StringHelper;

public class TitledWrapperHelper {

	
	public static Controller getWrapper(UserRequest ureq, WindowControl wControl,
			Controller controller, CourseNode courseNode, String iconCssClass) {
		
		String displayOption = courseNode.getDisplayOption();
		if(CourseNode.DISPLAY_OPTS_CONTENT.equals(displayOption)) {
			return controller;
		} else if (CourseNode.DISPLAY_OPTS_SHORT_TITLE_CONTENT.equals(displayOption)) {
			TitleInfo titleInfo = new TitleInfo(null, courseNode.getShortTitle(), null, courseNode.getIdent());
			titleInfo.setDescriptionCssClass("o_course_run_objectives");
			TitledWrapperController titledController = new TitledWrapperController(ureq, wControl, controller, "o_course_run", titleInfo);
			if (StringHelper.containsNonWhitespace(iconCssClass)) {
				titledController.setTitleCssClass(" b_with_small_icon_left " + iconCssClass + " ");
			}
			return titledController; 
		} else if (CourseNode.DISPLAY_OPTS_TITLE_CONTENT.equals(displayOption)) {
			TitleInfo titleInfo = new TitleInfo(null, courseNode.getLongTitle(), null, courseNode.getIdent());
			titleInfo.setDescriptionCssClass("o_course_run_objectives");
			TitledWrapperController titledController = new TitledWrapperController(ureq, wControl, controller, "o_course_run", titleInfo);
			if (StringHelper.containsNonWhitespace(iconCssClass)) {
				titledController.setTitleCssClass(" b_with_small_icon_left " + iconCssClass + " ");
			}
			return titledController; 
		} else if (CourseNode.DISPLAY_OPTS_SHORT_TITLE_DESCRIPTION_CONTENT.equals(displayOption)) {
			String title = courseNode.getShortTitle();
			String description = null;
			if (StringHelper.containsNonWhitespace(courseNode.getLearningObjectives())) {
				if (StringHelper.containsNonWhitespace(title)) {
					description = courseNode.getLearningObjectives();
				}
			}
			
			TitleInfo titleInfo = new TitleInfo(null, title, description, courseNode.getIdent());
			titleInfo.setDescriptionCssClass("o_course_run_objectives");
			TitledWrapperController titledController = new TitledWrapperController(ureq, wControl, controller, "o_course_run", titleInfo);
			if (StringHelper.containsNonWhitespace(iconCssClass)) {
				titledController.setTitleCssClass(" b_with_small_icon_left " + iconCssClass + " ");
			}
			return titledController; 
		} else if (CourseNode.DISPLAY_OPTS_TITLE_DESCRIPTION_CONTENT.equals(displayOption)) {

			String title = courseNode.getLongTitle();
			String description = null;
			
			if (StringHelper.containsNonWhitespace(courseNode.getLearningObjectives())) {
					description = courseNode.getLearningObjectives();
			}
			
			TitleInfo titleInfo = new TitleInfo(null, title, description, courseNode.getIdent());
			titleInfo.setDescriptionCssClass("o_course_run_objectives");
			TitledWrapperController titledController = new TitledWrapperController(ureq, wControl, controller, "o_course_run", titleInfo);
			if (StringHelper.containsNonWhitespace(iconCssClass)) {
				titledController.setTitleCssClass(" b_with_small_icon_left " + iconCssClass + " ");
			}
			return titledController; 
		} else {
			return controller;
		}
	}
}
