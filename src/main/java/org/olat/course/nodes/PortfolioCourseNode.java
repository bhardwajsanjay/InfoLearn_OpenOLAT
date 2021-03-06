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

import java.io.File;
import java.util.List;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.StackedController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.export.CourseEnvironmentMapper;
import org.olat.course.nodes.portfolio.PortfolioCourseNodeConfiguration;
import org.olat.course.nodes.portfolio.PortfolioCourseNodeEditController;
import org.olat.course.nodes.portfolio.PortfolioCourseNodeRunController;
import org.olat.course.nodes.portfolio.PortfolioResultDetailsController;
import org.olat.course.repository.ImportPortfolioReferencesController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.portfolio.manager.EPStructureManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryManager;


/**
 * 
 * Description:<br>
 * course node of type portfolio.
 * 
 * <P>
 * Initial Date:  6 oct. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class PortfolioCourseNode extends AbstractAccessableCourseNode implements AssessableCourseNode {
	
	private static final int CURRENT_CONFIG_VERSION = 2;
	
	public static final String EDIT_CONDITION_ID = "editportfolio";
	
	private static final String PACKAGE_EP = Util.getPackageName(PortfolioCourseNodeRunController.class);
	private static final String TYPE = "ep";
	
	private Condition preConditionEdit;
	
	public PortfolioCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}
	
	@Override
	public void updateModuleConfigDefaults(boolean isNewNode) {
		ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			MSCourseNode.initDefaultConfig(config);
	    config.setConfigurationVersion(CURRENT_CONFIG_VERSION);
		} 
		if (config.getConfigurationVersion() < 2) {
			if(config.get(PortfolioCourseNodeConfiguration.REPO_SOFT_KEY) == null) {
				Object mapKey = config.get(PortfolioCourseNodeConfiguration.MAP_KEY);
				if(mapKey instanceof Long) {
					EPStructureManager eSTMgr = (EPStructureManager) CoreSpringFactory.getBean("epStructureManager");
					RepositoryEntry re = eSTMgr.loadPortfolioRepositoryEntryByMapKey((Long)mapKey);
					config.set(PortfolioCourseNodeConfiguration.REPO_SOFT_KEY, re.getSoftkey());
				}
			}
	    config.setConfigurationVersion(2);
		}
	}
	
	@Override
	public void postImport(CourseEnvironmentMapper envMapper) {
		super.postImport(envMapper);
		postImportCondition(preConditionEdit, envMapper);
	}

	@Override
	public void postExport(CourseEnvironmentMapper envMapper, boolean backwardsCompatible) {
		super.postExport(envMapper, backwardsCompatible);
		postExportCondition(preConditionEdit, envMapper, backwardsCompatible);
	}

	@Override
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, StackedController stackPanel, ICourse course, UserCourseEnvironment euce) {
		PortfolioCourseNodeEditController childTabCntrllr = new PortfolioCourseNodeEditController(ureq, wControl, stackPanel,
				course, this, getModuleConfiguration(), euce);
		updateModuleConfigDefaults(false);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, course.getCourseEnvironment()
				.getCourseGroupManager(), euce, childTabCntrllr);
	}
	
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		updateModuleConfigDefaults(false);
		Controller controller;
		// OO-136 : do not allow guests to access portfolio task
		Roles roles = ureq.getUserSession().getRoles();
		if (roles.isGuestOnly()) {
			Translator trans =  Util.createPackageTranslator(PortfolioCourseNode.class, ureq.getLocale());
			String title = trans.translate("guestnoaccess.title");
			String message = trans.translate("guestnoaccess.message");
			controller = MessageUIFactory.createInfoMessage(ureq, wControl, title, message);
		} else {
			controller = new PortfolioCourseNodeRunController(ureq, wControl, userCourseEnv, ne, this);
		}
		Controller ctrl = TitledWrapperHelper.getWrapper(ureq, wControl, controller, this, "o_ep_icon");
		return new NodeRunConstructionResult(ctrl);
	}
	
	/**
	 * Default set the write privileges to coaches and admin only
	 * @return
	 */
	public Condition getPreConditionEdit() {
		if (preConditionEdit == null) {
			preConditionEdit = new Condition();
			preConditionEdit.setEasyModeCoachesAndAdmins(true);
			preConditionEdit.setConditionExpression(preConditionEdit.getConditionFromEasyModeConfiguration());
			preConditionEdit.setExpertMode(false);
		}
		preConditionEdit.setConditionId(EDIT_CONDITION_ID);
		return preConditionEdit;
	}

	/**
	 * 
	 * @param preConditionEdit
	 */
	public void setPreConditionEdit(Condition preConditionEdit) {
		if (preConditionEdit == null) {
			preConditionEdit = getPreConditionEdit();
		}
		preConditionEdit.setConditionId(EDIT_CONDITION_ID);
		this.preConditionEdit = preConditionEdit;
	}
	
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		Object repoSoftkey = getModuleConfiguration().get(PortfolioCourseNodeConfiguration.REPO_SOFT_KEY);
		if(repoSoftkey instanceof String) {
			RepositoryManager rm = RepositoryManager.getInstance();
			RepositoryEntry entry = rm.lookupRepositoryEntryBySoftkey((String)repoSoftkey, false);
			if(entry != null) {
				return entry;
			}
		}
		Long mapKey = (Long)getModuleConfiguration().get(PortfolioCourseNodeConfiguration.MAP_KEY);
		if(mapKey != null) {
			EPStructureManager eSTMgr = (EPStructureManager) CoreSpringFactory.getBean("epStructureManager");
			RepositoryEntry re = eSTMgr.loadPortfolioRepositoryEntryByMapKey(mapKey);
			return re;
		}
		return null;
	}
	
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		return true;
	}
	
	@Override
	public StatusDescription isConfigValid() {
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		StatusDescription sd = StatusDescription.NOERROR;
		boolean isValid = PortfolioCourseNodeEditController.isModuleConfigValid(getModuleConfiguration());
		if (!isValid) {
			String shortKey = "error.noreference.short";
			String longKey = "error.noreference.long";
			String[] params = new String[] { getShortTitle() };
			sd = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, PACKAGE_EP);
			sd.setDescriptionForUnit(getIdent());
			// set which pane is affected by error
			sd.setActivateableViewIdentifier(PortfolioCourseNodeEditController.PANE_TAB_CONFIG);
		}
		return sd;
	}
	
	@Override
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		oneClickStatusCache = null;
		List<StatusDescription> statusDescs = isConfigValidWithTranslator(cev, PACKAGE_EP, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(statusDescs);
		return oneClickStatusCache;
	}

	@Override
	protected void calcAccessAndVisibility(ConditionInterpreter ci, NodeEvaluation nodeEval) {
		//nodeEval.setVisible(true);
		super.calcAccessAndVisibility(ci, nodeEval);
		
		// evaluate the preconditions
		boolean editor = (getPreConditionEdit().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionEdit()));
		nodeEval.putAccessStatus(EDIT_CONDITION_ID, editor);
	}

	@Override
	public Float getMaxScoreConfiguration() {
		if (!hasScoreConfigured()) { 
			throw new OLATRuntimeException(PortfolioCourseNode.class, "getMaxScore not defined when hasScore set to false", null);
		}
		ModuleConfiguration config = getModuleConfiguration();
		Float max = (Float) config.get(MSCourseNode.CONFIG_KEY_SCORE_MAX);
		return max;
	}

	@Override
	public Float getMinScoreConfiguration() {
		if (!hasScoreConfigured()) { 
			throw new OLATRuntimeException(PortfolioCourseNode.class, "getMinScore not defined when hasScore set to false", null);
		}
		ModuleConfiguration config = getModuleConfiguration();
		Float min = (Float)config.get(MSCourseNode.CONFIG_KEY_SCORE_MIN);
		return min;
	}

	@Override
	public Float getCutValueConfiguration() {
		if (!hasPassedConfigured()) { 
			throw new OLATRuntimeException(PortfolioCourseNode.class, "getCutValue not defined when hasPassed set to false", null);
		}
		ModuleConfiguration config = getModuleConfiguration();
		Float cut = (Float) config.get(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE);
		return cut;
	}

	@Override
	public boolean hasScoreConfigured() {
		ModuleConfiguration config = getModuleConfiguration();
		Boolean score = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD);
		return (score == null) ? false : score.booleanValue();
	}

	@Override
	public boolean hasPassedConfigured() {
		ModuleConfiguration config = getModuleConfiguration();
		Boolean passed = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD);
		return (passed == null) ? false : passed.booleanValue();
	}

	@Override
	public boolean hasCommentConfigured() {
		ModuleConfiguration config = getModuleConfiguration();
		Boolean comment = (Boolean) config.get(MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD);
		return (comment == null) ? false : comment.booleanValue();
	}

	@Override
	public boolean hasAttemptsConfigured() {
		return true;
	}

	@Override
	public boolean hasDetails() {
		return true;
	}

	@Override
	public boolean isEditableConfigured() {
		return true;
	}

	@Override
	public ScoreEvaluation getUserScoreEvaluation(UserCourseEnvironment userCourseEnvironment) {
		// read score from properties
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		Boolean passed = null;
		Float score = null;
		// only db lookup if configured, else return null
		if (hasPassedConfigured()) passed = am.getNodePassed(this, mySelf);
		if (hasScoreConfigured()) score = am.getNodeScore(this, mySelf);

		ScoreEvaluation se = new ScoreEvaluation(score, passed);
		return se;
	}

	@Override
	public String getUserUserComment(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		String userCommentValue = am.getNodeComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return userCommentValue;
	}

	@Override
	public String getUserCoachComment(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		String coachCommentValue = am.getNodeCoachComment(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return coachCommentValue;
	}

	@Override
	public String getUserLog(UserCourseEnvironment userCourseEnvironment) {
		UserNodeAuditManager am = userCourseEnvironment.getCourseEnvironment().getAuditManager();
		String logValue = am.getUserNodeLog(this, userCourseEnvironment.getIdentityEnvironment().getIdentity());
		return logValue;
	}

	@Override
	public Integer getUserAttempts(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		Integer userAttemptsValue = am.getNodeAttempts(this, mySelf);
		return userAttemptsValue;
	}

	@Override
	public String getDetailsListView(UserCourseEnvironment userCourseEnvironment) {
		return null;
	}

	@Override
	public String getDetailsListViewHeaderKey() {
		return "table.header.details.ta";
	}

	@Override
	public Controller getDetailsEditController(UserRequest ureq, WindowControl wControl, StackedController stackPanel, UserCourseEnvironment userCourseEnvironment) {
		return new PortfolioResultDetailsController(ureq, wControl, stackPanel, this, userCourseEnvironment);
	}

	@Override
	public void updateUserScoreEvaluation(ScoreEvaluation scoreEvaluation, UserCourseEnvironment userCourseEnvironment,
			Identity coachingIdentity, boolean incrementAttempts) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.saveScoreEvaluation(this, coachingIdentity, mySelf, new ScoreEvaluation(scoreEvaluation.getScore(), scoreEvaluation.getPassed()), userCourseEnvironment, incrementAttempts);
	}

	@Override
	public void updateUserUserComment(String userComment, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		if (userComment != null) {
			am.saveNodeComment(this, coachingIdentity, mySelf, userComment);
		}
	}

	@Override
	public void incrementUserAttempts(UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		am.incrementNodeAttempts(this, mySelf, userCourseEnvironment);
	}

	@Override
	public void updateUserAttempts(Integer userAttempts, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		if (userAttempts != null) {
			AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
			Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
			am.saveNodeAttempts(this, coachingIdentity, mySelf, userAttempts);
		}
	}

	@Override
	public void updateUserCoachComment(String coachComment, UserCourseEnvironment userCourseEnvironment) {
		AssessmentManager am = userCourseEnvironment.getCourseEnvironment().getAssessmentManager();
		Identity mySelf = userCourseEnvironment.getIdentityEnvironment().getIdentity();
		if (coachComment != null) {
			am.saveNodeCoachComment(this, mySelf, coachComment);
		}
	}

	@Override
	public boolean hasStatusConfigured() {
		return true;
	}

	@Override
	public void exportNode(File exportDirectory, ICourse course) {
		RepositoryEntry re = getReferencedRepositoryEntry();
		if (re == null) return;
		
		File fExportDirectory = new File(exportDirectory, getIdent());
		fExportDirectory.mkdirs();
		RepositoryEntryImportExport reie = new RepositoryEntryImportExport(re, fExportDirectory);
		reie.exportDoExport();
	}

	@Override
	public Controller importNode(File importDirectory, ICourse course, boolean unattendedImport, UserRequest ureq, WindowControl wControl) {
		File importSubdir = new File(importDirectory, getIdent());
		RepositoryEntryImportExport rie = new RepositoryEntryImportExport(importSubdir);
		if (!rie.anyExportedPropertiesAvailable()) return null;

		// do import referenced repository entries
		if (unattendedImport) {
			Identity admin = BaseSecurityManager.getInstance().findIdentityByName("administrator");
			ImportPortfolioReferencesController.doImport(rie, this, true, admin);
			return null;
		} else {
			return new ImportPortfolioReferencesController(ureq, wControl, this, rie);
		}
	}
}