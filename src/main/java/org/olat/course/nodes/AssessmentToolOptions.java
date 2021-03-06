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
package org.olat.course.nodes;

import java.util.List;

import org.olat.basesecurity.SecurityGroup;
import org.olat.core.id.Identity;
import org.olat.group.BusinessGroup;

/**
 * 
 * Initial date: 20.12.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentToolOptions {
	
	private BusinessGroup group;
	private List<Identity> identities;
	private AlternativeToIdentities alternativeToIdentities;
	
	public BusinessGroup getGroup() {
		return group;
	}
	
	public void setGroup(BusinessGroup group) {
		this.group = group;
	}
	
	public List<Identity> getIdentities() {
		return identities;
	}
	
	public void setIdentities(List<Identity> identities) {
		this.identities = identities;
	}
	
	public AlternativeToIdentities getAlternativeToIdentities() {
		return alternativeToIdentities;
	}
	
	public void setAlternativeToIdentities(List<SecurityGroup> secGroups, boolean mayViewAllUsersAssessments) {
		alternativeToIdentities = new AlternativeToIdentities();
		alternativeToIdentities.setSecGroups(secGroups);
		alternativeToIdentities.setMayViewAllUsersAssessments(mayViewAllUsersAssessments);
	}
	
	public static class AlternativeToIdentities {
		
		private List<SecurityGroup> secGroups;
		private boolean mayViewAllUsersAssessments;
		
		public List<SecurityGroup> getSecGroups() {
			return secGroups;
		}
		
		public void setSecGroups(List<SecurityGroup> secGroups) {
			this.secGroups = secGroups;
		}
		
		public boolean isMayViewAllUsersAssessments() {
			return mayViewAllUsersAssessments;
		}
		
		public void setMayViewAllUsersAssessments(boolean mayViewAllUsersAssessments) {
			this.mayViewAllUsersAssessments = mayViewAllUsersAssessments;
		}	
	}
}
