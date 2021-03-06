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

package org.olat.course.nodes.st;

import java.util.Iterator;
import java.util.List;

import org.olat.commons.file.filechooser.FileChooseCreateEditController;
import org.olat.commons.file.filechooser.LinkChooseCreateEditController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.iframe.DeliveryOptions;
import org.olat.core.gui.control.generic.iframe.DeliveryOptionsConfigurationController;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.STCourseNode;
import org.olat.course.run.scoring.ScoreCalculator;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.course.tree.CourseInternalLinkTreeModel;
import org.olat.modules.ModuleConfiguration;

/**
 * Description:<BR/> Edit controller for a course node of type structure <P/>
 * 
 * Initial Date: Oct 12, 2004
 * @author gnaegi
 */
public class STCourseNodeEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	public static final String ACCESSABILITY_CONDITION_FORM = "accessabilityConditionForm";
	private static final String PANE_TAB_ST_SCORECALCULATION = "pane.tab.st_scorecalculation";
	private static final String PANE_TAB_DELIVERYOPTIONS = "pane.tab.deliveryOptions";
	public static final String PANE_TAB_ST_CONFIG = "pane.tab.st_config";
	private static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	
	/** configuration key for the filename */
	public static final String CONFIG_KEY_FILE = "file";
	/**
	 * configuration key: should relative links like ../otherfolder/my.css be
	 * allowed? *
	 */
	public static final String CONFIG_KEY_ALLOW_RELATIVE_LINKS = "allowRelativeLinks";
	public static final String CONFIG_KEY_DELIVERYOPTIONS = "deliveryOptions";
	// key to store information on what to display in the run 
	public static final String CONFIG_KEY_DISPLAY_TYPE = "display";
	// display a custom file
	public static final String CONFIG_VALUE_DISPLAY_FILE = "file";
	// display a simple table on content
	public static final String CONFIG_VALUE_DISPLAY_TOC = "toc";
	// display a detailed peek view
	public static final String CONFIG_VALUE_DISPLAY_PEEKVIEW = "peekview";
	//do not display peek view, delegate to first child CourseNode
	public static final String CONFIG_VALUE_DISPLAY_DELEGATE = "delegate";
	// key to display the enabled child node peek views
	public static final String CONFIG_KEY_PEEKVIEW_CHILD_NODES = "peekviewChildNodes";
	// key to store the number of columns
	public static final String CONFIG_KEY_COLUMNS = "columns";	
	
	private static final String[] paneKeys = { PANE_TAB_ST_SCORECALCULATION, PANE_TAB_ST_CONFIG, PANE_TAB_ACCESSIBILITY };

	private STCourseNode stNode;
	private EditScoreCalculationExpertForm scoreExpertForm;
	private EditScoreCalculationEasyForm scoreEasyForm;
	private List<CourseNode> assessableChildren;
	private STCourseNodeDisplayConfigFormController nodeDisplayConfigFormController;
	
	private VelocityContainer score, configvc;
	private Link activateEasyModeButton;
	private Link activateExpertModeButton;

	private VFSContainer courseFolderContainer;
	private String chosenFile;
	private Boolean allowRelativeLinks;
	private DeliveryOptions deliveryOptions;

	private Panel fccePanel;
	private FileChooseCreateEditController fccecontr;
	private DeliveryOptionsConfigurationController deliveryOptionsCtrl;
	private ConditionEditController accessibilityCondContr;

	private boolean editorEnabled = false;
	private UserCourseEnvironment euce;
	
	private TabbedPane myTabbedPane;
	private CourseEditorTreeModel editorModel;


	/**
	 * @param ureq
	 * @param wControl
	 * @param stNode
	 * @param courseFolderPath
	 * @param groupMgr
	 * @param editorModel
	 */
	public STCourseNodeEditController(UserRequest ureq, WindowControl wControl, STCourseNode stNode, VFSContainer courseFolderContainer,
			CourseGroupManager groupMgr, CourseEditorTreeModel editorModel, UserCourseEnvironment euce) {
		super(ureq, wControl);

		this.stNode = stNode;
		this.courseFolderContainer = courseFolderContainer;
		this.euce = euce;
		this.editorModel = editorModel;

		Translator fallback = Util.createPackageTranslator(Condition.class, getLocale());
		Translator newTranslator = Util.createPackageTranslator(STCourseNodeEditController.class, getLocale(), fallback);
		setTranslator(newTranslator);
				
		score = createVelocityContainer("scoreedit");
		activateEasyModeButton = LinkFactory.createButtonSmall("cmd.activate.easyMode", score, this);
		activateExpertModeButton = LinkFactory.createButtonSmall("cmd.activate.expertMode", score, this);
				
		configvc = createVelocityContainer("config");

		// Load configured value for file if available and enable editor when in
		// file display move, even when no file is selected (this will display the
		// file selector button)
		chosenFile = (String) stNode.getModuleConfiguration().get(CONFIG_KEY_FILE); 
		editorEnabled = (CONFIG_VALUE_DISPLAY_FILE.equals(stNode.getModuleConfiguration().getStringValue(CONFIG_KEY_DISPLAY_TYPE)));
		
		allowRelativeLinks = stNode.getModuleConfiguration().getBooleanSafe(CONFIG_KEY_ALLOW_RELATIVE_LINKS);
		deliveryOptions = (DeliveryOptions)stNode.getModuleConfiguration().get(CONFIG_KEY_DELIVERYOPTIONS);

		nodeDisplayConfigFormController = new STCourseNodeDisplayConfigFormController(ureq, wControl, stNode.getModuleConfiguration(), editorModel.getCourseEditorNodeById(stNode.getIdent()));
		listenTo(nodeDisplayConfigFormController);
		configvc.put("nodeDisplayConfigFormController", nodeDisplayConfigFormController.getInitialComponent());

		if (editorEnabled) {
			configvc.contextPut("editorEnabled", Boolean.valueOf(editorEnabled));
			addStartEditorToView(ureq);
		}
		
		deliveryOptionsCtrl = new DeliveryOptionsConfigurationController(ureq, getWindowControl(), deliveryOptions);
		listenTo(deliveryOptionsCtrl);

		// Find assessable children nodes
		assessableChildren = AssessmentHelper.getAssessableNodes(editorModel, stNode);

		// Accessibility precondition
		Condition accessCondition = stNode.getPreConditionAccess();
		accessibilityCondContr = new ConditionEditController(ureq, getWindowControl(), groupMgr, accessCondition,
				ACCESSABILITY_CONDITION_FORM, assessableChildren, euce, true);		
		listenTo(accessibilityCondContr);

		ScoreCalculator scoreCalc = stNode.getScoreCalculator();
		if (scoreCalc != null) {
			if (scoreCalc.isExpertMode() && scoreCalc.getPassedExpression() == null && scoreCalc.getScoreExpression() == null) {
				scoreCalc = null;
			} else if (!scoreCalc.isExpertMode() && scoreCalc.getPassedExpressionFromEasyModeConfiguration() == null
					&& scoreCalc.getScoreExpressionFromEasyModeConfiguration() == null) {
				scoreCalc = null;
			}
		}

		if (assessableChildren.size() == 0 && scoreCalc == null) {
			// show only the no assessable children message, if no previous score
			// config exists.
			score.contextPut("noAssessableChildren", Boolean.TRUE);
		} else {
			score.contextPut("noAssessableChildren", Boolean.FALSE);
		}

		// Init score calculator form
		if (scoreCalc != null && scoreCalc.isExpertMode()) {
			initScoreExpertForm(ureq);
		} else {
			initScoreEasyForm(ureq);
		}
	}

	/**
	 * Initialize an easy mode score calculator form and push it to the score
	 * velocity container
	 */
	private void initScoreEasyForm(UserRequest ureq) {
		removeAsListenerAndDispose(scoreEasyForm);
		scoreEasyForm = new EditScoreCalculationEasyForm(ureq, getWindowControl(), stNode.getScoreCalculator(), assessableChildren);
		listenTo(scoreEasyForm);
		score.put("scoreForm", scoreEasyForm.getInitialComponent());
		score.contextPut("isExpertMode", Boolean.FALSE);
	}

	/**
	 * Initialize an expert mode score calculator form and push it to the score
	 * velocity container
	 */
	private void initScoreExpertForm(UserRequest ureq) {
		removeAsListenerAndDispose(scoreExpertForm);
		scoreExpertForm = new EditScoreCalculationExpertForm(ureq, getWindowControl(), stNode.getScoreCalculator(), euce, assessableChildren);
		listenTo(scoreExpertForm);
		scoreExpertForm.setScoreCalculator(stNode.getScoreCalculator());
		score.put("scoreForm", scoreExpertForm.getInitialComponent());
		score.contextPut("isExpertMode", Boolean.TRUE);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == activateEasyModeButton) {
			initScoreEasyForm(ureq);
		} else if (source == activateExpertModeButton){
			initScoreExpertForm(ureq);
		}
	}
	
	/**
	 * 
	 * @param nodeDescriptions
	 * @return the warning message if any, null otherwise
	 */
	
	private String getWarningMessage(List<String> nodeDescriptions) {
		if(nodeDescriptions.size()>0) {			
			String invalidNodeTitles = "";
			Iterator<String> titleIterator = nodeDescriptions.iterator();
			while(titleIterator.hasNext()) {
				if(!invalidNodeTitles.equals("")) {
					invalidNodeTitles += "; ";
				}
				invalidNodeTitles += titleIterator.next();
			}	
			return translate("scform.error.configuration") + ": " + invalidNodeTitles;
		}
		return null;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == accessibilityCondContr) {
			if (event == Event.CHANGED_EVENT) {
				Condition cond = accessibilityCondContr.getCondition();
				stNode.setPreConditionAccess(cond);
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (event == FileChooseCreateEditController.FILE_CHANGED_EVENT) {
			chosenFile = fccecontr.getChosenFile();
			if (chosenFile != null) {
				stNode.getModuleConfiguration().set(CONFIG_KEY_FILE, chosenFile);
			} else {
				stNode.getModuleConfiguration().remove(CONFIG_KEY_FILE);
			}
			fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
		} else if (event == FileChooseCreateEditController.ALLOW_RELATIVE_LINKS_CHANGED_EVENT) {
			allowRelativeLinks = fccecontr.getAllowRelativeLinks();
			stNode.getModuleConfiguration().setBooleanEntry(CONFIG_KEY_ALLOW_RELATIVE_LINKS, allowRelativeLinks.booleanValue());
			fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
		} else if (source == deliveryOptionsCtrl) {
			deliveryOptions = deliveryOptionsCtrl.getDeliveryOptions();
			stNode.getModuleConfiguration().set(CONFIG_KEY_DELIVERYOPTIONS, deliveryOptions);
			fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
		} else if (source == nodeDisplayConfigFormController) {
			if (event == Event.DONE_EVENT) {
				// update the module configuration
				ModuleConfiguration moduleConfig = stNode.getModuleConfiguration();
				nodeDisplayConfigFormController.updateModuleConfiguration(moduleConfig);
				allowRelativeLinks = moduleConfig.getBooleanEntry(CONFIG_KEY_ALLOW_RELATIVE_LINKS);
				// update some class vars
				if (CONFIG_VALUE_DISPLAY_FILE.equals(moduleConfig.getStringValue(CONFIG_KEY_DISPLAY_TYPE))) {
					editorEnabled = true;
					configvc.contextPut("editorEnabled", Boolean.valueOf(editorEnabled));
					stNode.getModuleConfiguration().set(CONFIG_KEY_FILE, chosenFile);
					addStartEditorToView(ureq);
				} else { // user generated overview
					editorEnabled = false;
					configvc.contextPut("editorEnabled", Boolean.valueOf(editorEnabled));
					//fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
					// Let other config values from old config setup remain in config,
					// maybe used when user switches back to other config (OLAT-5610)
					if(myTabbedPane.containsTab(deliveryOptionsCtrl.getInitialComponent())) {
						myTabbedPane.removeTab(deliveryOptionsCtrl.getInitialComponent());
					}
				}
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
			
		} else if (source == scoreEasyForm) {
			
			if (event == Event.DONE_EVENT) {	
				//show warning if the score might be wrong because of the invalid nodes used for calculation
				List<String> testElemWithNoResource = scoreEasyForm.getInvalidNodeDescriptions();
				String msg = getWarningMessage(testElemWithNoResource);
				if(msg!=null) {								
					showWarning(msg);
				}

				ScoreCalculator sc = scoreEasyForm.getScoreCalulator();
				/*
				 * OLAT-1144 bug fix if Calculation Score -> NO and Calculate passing
				 * score -> NO we get a ScoreCalculator == NULL !
				 */
				if (sc != null) {
					sc.setPassedExpression(sc.getPassedExpressionFromEasyModeConfiguration());
					sc.setScoreExpression(sc.getScoreExpressionFromEasyModeConfiguration());
				}
				// ..setScoreCalculator(sc) can handle NULL values!
				stNode.setScoreCalculator(sc);
				initScoreEasyForm(ureq); // reload form, remove deleted nodes
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			} else if (event == Event.CANCELLED_EVENT) { // reload form
				initScoreEasyForm(ureq);
			}
		} else if (source == scoreExpertForm) {
			if (event == Event.DONE_EVENT) {
        //show warning if the score might be wrong because of the invalid nodes used for calculation
				List<String> testElemWithNoResource = scoreExpertForm.getInvalidNodeDescriptions();
				String msg = getWarningMessage(testElemWithNoResource);
				if(msg!=null) {								
					getWindowControl().setWarning(msg);
				}
				
				ScoreCalculator sc = scoreExpertForm.getScoreCalulator();
				/*
				 * OLAT-1144 bug fix if a ScoreCalculator == NULL !
				 */
				if (sc != null) {
					sc.clearEasyMode();
				}
				// ..setScoreCalculator(sc) can handle NULL values!
				stNode.setScoreCalculator(sc);
				fireEvent(ureq, NodeEditController.NODECONFIG_CHANGED_EVENT);
			} else if (event == Event.CANCELLED_EVENT) { // reload form
				initScoreExpertForm(ureq);
			}
		}
	}

	private void addStartEditorToView(UserRequest ureq) {
		fccecontr = new LinkChooseCreateEditController(ureq, getWindowControl(), chosenFile, allowRelativeLinks, courseFolderContainer, new CourseInternalLinkTreeModel(editorModel) );		
		listenTo(fccecontr);

		fccePanel = new Panel("filechoosecreateedit");
		Component fcContent = fccecontr.getInitialComponent();
		fccePanel.setContent(fcContent);
		configvc.put(fccePanel.getComponentName(), fccePanel);
		
		if(myTabbedPane != null) {
			if(!myTabbedPane.containsTab(deliveryOptionsCtrl.getInitialComponent())) {
				myTabbedPane.addTab(translate(PANE_TAB_DELIVERYOPTIONS), deliveryOptionsCtrl.getInitialComponent());
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableDefaultController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	public void addTabs(TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessibilityCondContr.getWrappedDefaultAccessConditionVC(translate("condition.accessibility.title")));
		tabbedPane.addTab(translate(PANE_TAB_ST_CONFIG), configvc);
		tabbedPane.addTab(translate(PANE_TAB_ST_SCORECALCULATION), score);
		if(editorEnabled) {
			tabbedPane.addTab(translate(PANE_TAB_DELIVERYOPTIONS), deliveryOptionsCtrl.getInitialComponent());
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
    //child controllers registered with listenTo() get disposed in BasicController
	}

	/**
	 * @param mc The module confguration
	 * @return The configured file name
	 */
	public static String getFileName(ModuleConfiguration mc) {
		return (String) mc.get(CONFIG_KEY_FILE);
	}

	public String[] getPaneKeys() {
		return paneKeys;
	}

	public TabbedPane getTabbedPane() {
		return myTabbedPane;
	}

}