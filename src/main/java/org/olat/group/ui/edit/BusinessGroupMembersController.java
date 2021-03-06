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
package org.olat.group.ui.edit;

import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.wizard.Step;
import org.olat.core.gui.control.generic.wizard.StepRunnerCallback;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.id.Identity;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailPackage;
import org.olat.core.util.mail.MailTemplate;
import org.olat.course.member.wizard.ImportMember_1a_LoginListStep;
import org.olat.course.member.wizard.ImportMember_1b_ChooseMemberStep;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagedFlag;
import org.olat.group.BusinessGroupService;
import org.olat.group.GroupLoggingAction;
import org.olat.group.model.BusinessGroupMembershipChange;
import org.olat.group.ui.main.MemberPermissionChangeEvent;
import org.olat.group.ui.main.SearchMembersParams;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class BusinessGroupMembersController extends BasicController {
	
	private final VelocityContainer mainVC;

	private final DisplayMemberSwitchForm dmsForm;
	private MemberListController membersController;
	private final Link importMemberLink, addMemberLink;
	private StepsMainRunController importMembersWizard;
	
	private BusinessGroup businessGroup;
	private final BusinessGroupService businessGroupService;

	public BusinessGroupMembersController(UserRequest ureq, WindowControl wControl, BusinessGroup businessGroup) {
		super(ureq, wControl);
		
		this.businessGroup = businessGroup;
		businessGroupService = CoreSpringFactory.getImpl(BusinessGroupService.class);
		
		mainVC = createVelocityContainer("tab_bgGrpMngmnt");
		putInitialPanel(mainVC);
		
		boolean hasWaitingList = businessGroup.getWaitingListEnabled().booleanValue();

		// Member Display Form, allows to enable/disable that others partips see
		// partips and/or owners
		// configure the form with checkboxes for owners and/or partips according
		// the booleans
		dmsForm = new DisplayMemberSwitchForm(ureq, getWindowControl(), true, true, hasWaitingList);
		dmsForm.setEnabled(!BusinessGroupManagedFlag.isManaged(businessGroup, BusinessGroupManagedFlag.display));
		listenTo(dmsForm);
		// set if the checkboxes are checked or not.
		dmsForm.setDisplayMembers(businessGroup);
		mainVC.put("displayMembers", dmsForm.getInitialComponent());
		
		boolean managed = BusinessGroupManagedFlag.isManaged(businessGroup, BusinessGroupManagedFlag.membersmanagement);
		SearchMembersParams searchParams = new SearchMembersParams(false, false, false, true, true, true, true);
		membersController = new MemberListController(ureq, getWindowControl(), businessGroup, searchParams);
		listenTo(membersController);
		
		membersController.reloadModel();

		mainVC.put("members", membersController.getInitialComponent());
		
		addMemberLink = LinkFactory.createButton("add.member", mainVC, this);
		addMemberLink.setElementCssClass("o_sel_group_add_member");
		addMemberLink.setVisible(!managed);
		mainVC.put("addMembers", addMemberLink);
		importMemberLink = LinkFactory.createButton("import.member", mainVC, this);
		importMemberLink.setElementCssClass("o_sel_group_import_members");
		importMemberLink.setVisible(!managed);
		mainVC.put("importMembers", importMemberLink);
	}
	
	public BusinessGroup getGroup() {
		return businessGroup;
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		 if (source == addMemberLink) {
			doChooseMembers(ureq);
		} else if (source == importMemberLink) {
			doImportMembers(ureq);
		}
	}
	
	protected void updateBusinessGroup(BusinessGroup businessGroup) {
		this.businessGroup = businessGroup;
		
		boolean hasWaitingList = businessGroup.getWaitingListEnabled().booleanValue();	
		Boolean waitingFlag = (Boolean)mainVC.getContext().get("hasWaitingGrp");
		if(waitingFlag == null || waitingFlag.booleanValue() != hasWaitingList) {
			mainVC.contextPut("hasWaitingGrp", new Boolean(hasWaitingList));
			dmsForm.setWaitingListVisible(hasWaitingList);
		}
		
		membersController.reloadModel();
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == dmsForm) {
			if(event == Event.CHANGED_EVENT) {
				boolean ownersIntern = dmsForm.isDisplayOwnersIntern();
				boolean participantsIntern = dmsForm.isDisplayParticipantsIntern();
				boolean waitingIntern = dmsForm.isDisplayWaitingListIntern();
				boolean ownersPublic = dmsForm.isDisplayOwnersPublic();
				boolean participantsPublic = dmsForm.isDisplayParticipantsPublic();
				boolean waitingPublic = dmsForm.isDisplayWaitingListPublic();
				boolean download = dmsForm.isDownloadList();
				
				businessGroup = businessGroupService.updateDisplayMembers(businessGroup,
						ownersIntern, participantsIntern, waitingIntern,
						ownersPublic, participantsPublic, waitingPublic,
						download);
				// notify current active users of this business group
				BusinessGroupModifiedEvent.fireModifiedGroupEvents(BusinessGroupModifiedEvent.CONFIGURATION_MODIFIED_EVENT, businessGroup, null);
				// do loggin
				ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_CONFIGURATION_CHANGED, getClass());
				fireEvent(ureq, event);
			}
		} else if(source == importMembersWizard) {
			if(event == Event.CANCELLED_EVENT || event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				getWindowControl().pop();
				removeAsListenerAndDispose(importMembersWizard);
				importMembersWizard = null;
				if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
					membersController.reloadModel();
				}
			}
		}
		super.event(ureq, source, event);
	}
	
	private void doChooseMembers(UserRequest ureq) {
		removeAsListenerAndDispose(importMembersWizard);

		Step start = new ImportMember_1b_ChooseMemberStep(ureq, null, businessGroup);
		StepRunnerCallback finish = new StepRunnerCallback() {
			@Override
			public Step execute(UserRequest ureq, WindowControl wControl, StepsRunContext runContext) {
				addMembers(ureq, runContext);
				return StepsMainRunController.DONE_MODIFIED;
			}
		};
		
		importMembersWizard = new StepsMainRunController(ureq, getWindowControl(), start, finish, null,
				translate("add.member"), "o_sel_group_import_1_wizard");
		listenTo(importMembersWizard);
		getWindowControl().pushAsModalDialog(importMembersWizard.getInitialComponent());
	}
	
	private void doImportMembers(UserRequest ureq) {
		removeAsListenerAndDispose(importMembersWizard);

		Step start = new ImportMember_1a_LoginListStep(ureq, null, businessGroup);
		StepRunnerCallback finish = new StepRunnerCallback() {
			@Override
			public Step execute(UserRequest ureq, WindowControl wControl, StepsRunContext runContext) {
				addMembers(ureq, runContext);
				if(runContext.containsKey("notFounds")) {
					showWarning("user.notfound", runContext.get("notFounds").toString());
				}
				return StepsMainRunController.DONE_MODIFIED;
			}
		};
		
		importMembersWizard = new StepsMainRunController(ureq, getWindowControl(), start, finish, null,
				translate("import.member"), "o_sel_group_import_logins_wizard");
		listenTo(importMembersWizard);
		getWindowControl().pushAsModalDialog(importMembersWizard.getInitialComponent());
	}
	
	protected void addMembers(UserRequest ureq, StepsRunContext runContext) {
		@SuppressWarnings("unchecked")
		List<Identity> members = (List<Identity>)runContext.get("members");
		
		MemberPermissionChangeEvent changes = (MemberPermissionChangeEvent)runContext.get("permissions");

		//commit all changes to the group memberships
		List<BusinessGroupMembershipChange> allModifications = changes.generateBusinessGroupMembershipChange(members);
		
		MailTemplate template = (MailTemplate)runContext.get("mailTemplate");
		MailPackage mailing = new MailPackage(template, getWindowControl().getBusinessControl().getAsString(), template != null);
		businessGroupService.updateMemberships(getIdentity(), allModifications, mailing);
		MailHelper.printErrorsAndWarnings(mailing.getResult(), getWindowControl(), getLocale());
	}
}
