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

package org.olat.course.run.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.TreeEvent;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.CourseLoggingAction;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Formatter;
import org.olat.core.util.Util;
import org.olat.core.util.nodes.INode;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.course.condition.additionalconditions.AdditionalConditionAnswerContainer;
import org.olat.course.condition.additionalconditions.AdditionalConditionManager;
import org.olat.course.editor.EditorMainController;
import org.olat.course.nodes.AbstractAccessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.CourseNodeFactory;
import org.olat.course.nodes.STCourseNode;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.TreeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.util.logging.activity.LoggingResourceable;

import de.bps.course.nodes.CourseNodePasswordManager;
import de.bps.course.nodes.CourseNodePasswordManagerImpl;


/**
 * Description: <br>
 * TODO: Felix Jost Class Description for NavigationHandler
 * Initial Date: 19.01.2005 <br>
 * @author Felix Jost
 */
public class NavigationHandler {
	private static final OLog log = Tracing.createLoggerFor(NavigationHandler.class);
	


	private final UserCourseEnvironment userCourseEnv;
	private final boolean previewMode;

	// remember so subsequent click to a subtreemodel's node has a handler
	private ControllerEventListener subtreemodelListener = null;
	
	private String selectedCourseNodeId;
	private Set<String> openCourseNodeIds = new HashSet<String>();
	private List<String> openTreeNodeIds = new ArrayList<String>();
	private Map<String,TreeModel> externalTreeModels = new HashMap<String,TreeModel>();

	/**
	 * @param userCourseEnv
	 * @param previewMode
	 */
	public NavigationHandler(UserCourseEnvironment userCourseEnv, boolean previewMode) {
		this.userCourseEnv = userCourseEnv;
		this.previewMode = previewMode;
	}

	/**
	 * to be called upon entering a course. <br>
	 * 
	 * @param ureq
	 * @param wControl
	 * @return NodeClickedRef
	 * @param calledCourseNode the coursenode to jump to; if null, the root
	 *          coursenode is selected
	 * @param listeningController
	 */
	public NodeClickedRef evaluateJumpToCourseNode(UserRequest ureq, WindowControl wControl, CourseNode calledCourseNode,
			ControllerEventListener listeningController, String nodecmd) {
		CourseNode cn;
		if (calledCourseNode == null) {
			// indicate to jump to root course node
			cn = userCourseEnv.getCourseEnvironment().getRunStructure().getRootNode();
		} else {
			cn = calledCourseNode;
		}
		return doEvaluateJumpTo(ureq, wControl, cn, listeningController, nodecmd, null, null);
	}

	/**
	 * to be called when the users clickes on a node when in the course
	 * 
	 * @param ureq
	 * @param wControl
	 * @param treeModel
	 * @param treeEvent
	 * @param listeningController
	 * @param nodecmd null or a subcmd which activates a node-specific view (e.g. opens a certain uri in a contentpackaging- buildingblock)
	 * @return the NodeClickedRef
	 * @return currentNodeController the current node controller that will be dispose before creating the new one
	 */
	public NodeClickedRef evaluateJumpToTreeNode(UserRequest ureq, WindowControl wControl, TreeModel treeModel, TreeEvent treeEvent,
			ControllerEventListener listeningController, String nodecmd, Controller currentNodeController) {
		NodeClickedRef ncr;
		String treeNodeId = treeEvent.getNodeId();
		TreeNode selTN = treeModel.getNodeById(treeNodeId);
		if (selTN == null) throw new AssertException("no treenode found:" + treeNodeId);

		// check if appropriate for subtreemodelhandler
		Object userObject = selTN.getUserObject();
		if (!(userObject instanceof NodeEvaluation)) {
			// yes, appropriate
			
			NodeRunConstructionResult nrcr = null;
			CourseNode internCourseNode = null;
			GenericTreeModel subTreeModel;
			if (subtreemodelListener == null) {
				//throw new AssertException("no handler for subtreemodelcall!");
				//reattach the subtreemodellistener
				TreeNode internNode = getFirstInternParentNode(selTN);
				NodeEvaluation prevEval = (NodeEvaluation) internNode.getUserObject();
				internCourseNode = prevEval.getCourseNode();
				
				final OLATResourceable ores = OresHelper.createOLATResourceableInstance(CourseNode.class, Long.parseLong(internCourseNode.getIdent()));
				ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ores);
				WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, wControl);
				nrcr = internCourseNode.createNodeRunConstructionResult(ureq, bwControl, userCourseEnv, prevEval, nodecmd);
				// remember as instance variable for next click
				subtreemodelListener = nrcr.getSubTreeListener();
				subTreeModel = (GenericTreeModel)nrcr.getSubTreeModel();
				externalTreeModels.put(internCourseNode.getIdent(), subTreeModel);
			} else {
				TreeNode internNode = getFirstInternParentNode(selTN);
				NodeEvaluation prevEval = (NodeEvaluation) internNode.getUserObject();
				internCourseNode = prevEval.getCourseNode();
				subTreeModel = (GenericTreeModel)externalTreeModels.get(internCourseNode.getIdent());
			}
			if (log.isDebug()){
				log.debug("delegating to handler: treeNodeId = " + treeNodeId);
			}

			// update the node and event to match the new tree model
			selTN = subTreeModel.findNodeByUserObject(userObject);
			treeEvent = new TreeEvent(treeEvent.getCommand(), treeEvent.getSubCommand(), selTN.getIdent());

			boolean dispatch = true;
			if(userObject instanceof String) {
				if(TreeEvent.COMMAND_TREENODE_OPEN.equals(treeEvent.getSubCommand())) {
					openCourseNodeIds.add((String)userObject);
					openTreeNodeIds.add((String)userObject);
					dispatch = false;
				} else if(TreeEvent.COMMAND_TREENODE_CLOSE.equals(treeEvent.getSubCommand())) {
					removeChildrenFromOpenNodes(selTN);
					dispatch = false;
				}
			}
			
			if(dispatch) {
			// null as controller source since we are not a controller
				subtreemodelListener.dispatchEvent(ureq, null, treeEvent);
				// no node construction result indicates handled
			}
			ncr = new NodeClickedRef(treeModel, true, null, null, internCourseNode, nrcr, true);
		} else {
			// normal dispatching to a coursenode.
			// get the courseNode that was called
			NodeEvaluation prevEval = (NodeEvaluation) selTN.getUserObject();
			if (!prevEval.isVisible()) throw new AssertException("clicked on a node which is not visible: treenode=" + selTN.getIdent() + ", "
					+ selTN.getTitle());
			CourseNode calledCourseNode = prevEval.getCourseNode();
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrap(calledCourseNode));
			// dispose old node controller before creating the NodeClickedRef which creates 
			// the new node controller. It is important that the old node controller is 
			// disposed before the new one to not get conflicts with cacheable mappers that
			// might be used in both controllers with the same ID (e.g. the course folder)
			if(TreeEvent.COMMAND_TREENODE_OPEN.equals(treeEvent.getSubCommand()) || TreeEvent.COMMAND_TREENODE_CLOSE.equals(treeEvent.getSubCommand())) {
				if(isInParentLine(calledCourseNode)) {
					if (currentNodeController != null) {
						currentNodeController.dispose();
					}
				}
				ncr = doEvaluateJumpTo(ureq, wControl, calledCourseNode, listeningController, nodecmd, treeEvent.getSubCommand(), currentNodeController);
			} else {
				if (currentNodeController != null) {
					currentNodeController.dispose();
				}
				ncr = doEvaluateJumpTo(ureq, wControl, calledCourseNode, listeningController, nodecmd, treeEvent.getSubCommand(), currentNodeController);
			}
		}
		return ncr;
	}

	private NodeClickedRef doEvaluateJumpTo(UserRequest ureq, WindowControl wControl, CourseNode courseNode,
			ControllerEventListener listeningController, String nodecmd, String nodeSubCmd, Controller currentNodeController) {
		NodeClickedRef nclr;
		if (log.isDebug()){
			log.debug("evaluateJumpTo courseNode = " + courseNode.getIdent() + ", " + courseNode.getShortName());
		}

		// build the new treemodel by evaluating the preconditions
		TreeEvaluation treeEval = new TreeEvaluation();
		GenericTreeModel treeModel = new GenericTreeModel();
		CourseNode rootCn = userCourseEnv.getCourseEnvironment().getRunStructure().getRootNode();
		CourseNodePasswordManager cnpm = CourseNodePasswordManagerImpl.getInstance();
		Long courseId = userCourseEnv.getCourseEnvironment().getCourseResourceableId();
		NodeEvaluation rootNodeEval = rootCn.eval(userCourseEnv.getConditionInterpreter(), treeEval);
		TreeNode treeRoot = rootNodeEval.getTreeNode();
		treeModel.setRootNode(treeRoot);

		// find the treenode that corresponds to the node (!= selectedTreeNode since
		// we built the TreeModel anew in the meantime
		TreeNode newCalledTreeNode = treeEval.getCorrespondingTreeNode(courseNode);
		if (newCalledTreeNode == null) {
			// the clicked node is not visible anymore!
			// if the new calculated model does not contain the selected node anymore
			// (because of visibility changes of at least one of the ancestors
			// -> issue an user infomative msg
			// nclr: the new treemodel, not visible, no selected nodeid, no
			// calledcoursenode, no nodeconstructionresult
			nclr = new NodeClickedRef(treeModel, false, null, null, null, null, false);
		} else {
			// calculate the NodeClickedRef
			// 1. get the correct (new) nodeevaluation
			NodeEvaluation nodeEval = (NodeEvaluation) newCalledTreeNode.getUserObject();
			if (nodeEval.getCourseNode() != courseNode) throw new AssertException("error in structure");
			if (!nodeEval.isVisible()) throw new AssertException("node eval not visible!!");
			// 2. start with the current NodeEvaluation, evaluate overall accessiblity
			// per node bottom-up to see if all ancestors still grant access to the
			// desired node
			boolean mayAccessWholeTreeUp = mayAccessWholeTreeUp(nodeEval);
			String newSelectedNodeId = newCalledTreeNode.getIdent();
			Controller controller;
			AdditionalConditionManager addMan = null;
			if (courseNode instanceof AbstractAccessableCourseNode) {
				AdditionalConditionAnswerContainer answerContainer = cnpm.getAnswerContainer(ureq.getIdentity());
				addMan = new AdditionalConditionManager( (AbstractAccessableCourseNode) courseNode, courseId, answerContainer);
			}
			
			if (!mayAccessWholeTreeUp|| (addMan != null && !addMan.evaluateConditions())) {
				// we cannot access the node anymore (since e.g. a time constraint
				// changed), so give a (per-node-configured) explanation why and what
				// the access conditions would be (a free form text, should be
				// nontechnical).

				//this is the case if only one of the additional conditions failed

				if (nodeEval.oldStyleConditionsOk()) {
					controller = addMan.nextUserInputController(ureq, wControl);
					if (listeningController != null) {
						controller.addControllerListener(listeningController);
					}
				} else {
					// NOTE: we do not take into account what node caused the non-access by
					// being !isAtLeastOneAccessible, but always state the
					// NoAccessExplanation of the Node originally called by the user
					String explan = courseNode.getNoAccessExplanation();
					String sExplan = (explan == null ? "" : Formatter.formatLatexFormulas(explan));
					 controller = MessageUIFactory.createInfoMessage(ureq, wControl, null, sExplan);
					// write log information
					ThreadLocalUserActivityLogger.log(CourseLoggingAction.COURSE_NAVIGATION_NODE_NO_ACCESS, getClass(),
							LoggingResourceable.wrap(courseNode));
				}
				NodeRunConstructionResult ncr = new NodeRunConstructionResult(controller, null, null, null);
				// nclr: the new treemodel, visible, selected nodeid, calledcoursenode,
				// nodeconstructionresult
				nclr = new NodeClickedRef(treeModel, true, newSelectedNodeId, null, courseNode, ncr, false);
			} else if (!CourseNodeFactory.getInstance().getCourseNodeConfigurationEvenForDisabledBB(courseNode.getType()).isEnabled()) {
				Translator pT = Util.createPackageTranslator(EditorMainController.class, ureq.getLocale());
				controller = MessageUIFactory.createInfoMessage(ureq, wControl, null, pT.translate("course.building.block.disabled.user"));
				NodeRunConstructionResult ncr = new NodeRunConstructionResult(controller, null, null, null);
				nclr = new NodeClickedRef(treeModel, true, newSelectedNodeId, null, courseNode, ncr, false);
			} else { // access ok
				
				// fxdiff FXOLAT-262
				if (STCourseNode.isDelegatingSTCourseNode(courseNode) && (courseNode.getChildCount() > 0)) {
					// the clicked node is a STCourse node and is set to "delegate", so
					// delegate to its first visible child; if no child is visible, just skip and do normal eval
					INode child;
					for (int i = 0; i < courseNode.getChildCount(); i++) {
						child = courseNode.getChildAt(i);
						if (child instanceof CourseNode) {
							CourseNode cn = (CourseNode) child;
							NodeEvaluation cnEval = cn.eval(userCourseEnv.getConditionInterpreter(), treeEval);
							if (cnEval.isVisible()) return this.doEvaluateJumpTo(ureq, wControl, cn, listeningController, nodecmd, nodeSubCmd,
									currentNodeController);
						}
					}
				}
					
					
				// access the node, display its result in the right pane
				NodeRunConstructionResult ncr;
				
				// calculate the new businesscontext for the coursenode being called.	
				// type: class of node; key = node.getIdent;
				
				Class<CourseNode> oresC = CourseNode.class; // don't use the concrete instance since for the course: to jump to a coursenode with a given id is all there is to know
				Long oresK = new Long(Long.parseLong(courseNode.getIdent()));
				final OLATResourceable ores = OresHelper.createOLATResourceableInstance(oresC, oresK);
				
				ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ores);
				WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, wControl);
				if (previewMode) {
					ncr = new NodeRunConstructionResult(courseNode.createPreviewController(ureq, bwControl, userCourseEnv, nodeEval));
				} else {
					ncr = courseNode.createNodeRunConstructionResult(ureq, bwControl, userCourseEnv, nodeEval, nodecmd);

					// remember as instance variable for next click
					subtreemodelListener = ncr.getSubTreeListener();
					if (subtreemodelListener != null) {
						externalTreeModels.put(courseNode.getIdent(), ncr.getSubTreeModel());
						if(!newSelectedNodeId.equals(ncr.getSelectedTreeNodeId())) {
							TreeNode selectedNode = ncr.getSubTreeModel().getNodeById(ncr.getSelectedTreeNodeId());
							openCourseNodeIds.add((String)selectedNode.getUserObject());
						}
					}
				}
				
				if(TreeEvent.COMMAND_TREENODE_OPEN.equals(nodeSubCmd)) {
					openCourseNodeIds.add(courseNode.getIdent());
					newSelectedNodeId = convertToTreeNodeId(treeEval, selectedCourseNodeId);
				} else if(TreeEvent.COMMAND_TREENODE_CLOSE.equals(nodeSubCmd)) {
					removeChildrenFromOpenNodes(courseNode);
					newSelectedNodeId = convertToTreeNodeId(treeEval, selectedCourseNodeId);
					if(!isInParentLine(courseNode)) {
						selectedCourseNodeId = courseNode.getIdent();
					} else {
						selectedCourseNodeId = null;
						newSelectedNodeId = null;
					}
				} else {
					//add the selected node to the open one, if not, strange behaviour
					selectedCourseNodeId = courseNode.getIdent();
					openCourseNodeIds.add(selectedCourseNodeId);
				}
				
				openTreeNodeIds = convertToTreeNodeIds(treeEval, openCourseNodeIds);
				reattachExternalTreeModels(treeEval);
				

				if((TreeEvent.COMMAND_TREENODE_OPEN.equals(nodeSubCmd) || TreeEvent.COMMAND_TREENODE_CLOSE.equals(nodeSubCmd)) &&
						currentNodeController != null && !currentNodeController.isDisposed()) {
					nclr = new NodeClickedRef(treeModel, true, null, openTreeNodeIds, null, null, false);
				} else {
					// nclr: the new treemodel, visible, selected nodeid, calledcoursenode,
					// nodeconstructionresult
					nclr = new NodeClickedRef(treeModel, true, newSelectedNodeId, openTreeNodeIds, courseNode, ncr, false);
					// attach listener; we know we have a runcontroller here
					if (listeningController != null) {
						nclr.getRunController().addControllerListener(listeningController);
					}
				}
				// write log information
				ThreadLocalUserActivityLogger.log(CourseLoggingAction.COURSE_NAVIGATION_NODE_ACCESS, getClass(),
						LoggingResourceable.wrap(courseNode));
			}
		}
		return nclr;
	}
	
	private void reattachExternalTreeModels(TreeEvaluation treeEval) {
		if(externalTreeModels == null || externalTreeModels.isEmpty()) return;
		
		for(Map.Entry<String, TreeModel> entry:externalTreeModels.entrySet()) {
			String courseNodeId = entry.getKey();
			TreeModel treeModel = entry.getValue();
			
			CourseNode courseNode = userCourseEnv.getCourseEnvironment().getRunStructure().getNode(courseNodeId);
			TreeNode treeNode = treeEval.getCorrespondingTreeNode(courseNode);
			if(treeNode != null) {
				addSubTreeModel(treeNode, treeModel);
			}
		}
	}
	
	private TreeNode getFirstInternParentNode(TreeNode node) {
		while(node != null) {
			if(node.getUserObject() instanceof NodeEvaluation) {
				return node;
			}
			node = (TreeNode)node.getParent();
		}
		return null;
	}

	private void removeChildrenFromOpenNodes(TreeNode treeNode) {
		openCourseNodeIds.remove(treeNode.getIdent());
		openCourseNodeIds.remove(treeNode.getUserObject());
		for(int i=treeNode.getChildCount(); i-->0; ) {
			removeChildrenFromOpenNodes((TreeNode)treeNode.getChildAt(i));
		}
	}
	
	private void removeChildrenFromOpenNodes(CourseNode courseNode) {
		openCourseNodeIds.remove(courseNode.getIdent());
		for(int i=courseNode.getChildCount(); i-->0; ) {
			removeChildrenFromOpenNodes((CourseNode)courseNode.getChildAt(i));
		}
	}
	
	private boolean isInParentLine(CourseNode courseNode) {
		if(selectedCourseNodeId == null) return false;
		
		CourseNode selectedCourseNode = userCourseEnv.getCourseEnvironment().getRunStructure().getNode(selectedCourseNodeId);
		while(selectedCourseNode != null) {
			if(selectedCourseNode.getIdent().equals(courseNode.getIdent())) {
				return true;
			}
			selectedCourseNode = (CourseNode)selectedCourseNode.getParent();
		}
		return false;
	}
	
	private List<String> convertToTreeNodeIds(TreeEvaluation treeEval, Collection<String> courseNodeIds) {
		if(courseNodeIds == null || courseNodeIds.isEmpty()) return new ArrayList<String>();

		List<String> convertedIds = new ArrayList<String>(courseNodeIds.size());
		for(String courseNodeId:courseNodeIds) {
			convertedIds.add(convertToTreeNodeId(treeEval, courseNodeId));
		}
		return convertedIds;
	}
	
	private String convertToTreeNodeId(TreeEvaluation treeEval, String courseNodeId) {
		if(courseNodeId == null) return null;
		
		CourseNode courseNode = userCourseEnv.getCourseEnvironment().getRunStructure().getNode(courseNodeId);
		TreeNode newCalledTreeNode = treeEval.getCorrespondingTreeNode(courseNode);
		if(newCalledTreeNode == null) {
			return courseNodeId;
		} else {
			return newCalledTreeNode.getIdent();
		}
	}

	private void addSubTreeModel(TreeNode parent, TreeModel modelToAppend) {
		// ignore root and directly add children.
		// need to clone children so that are not detached from their original
		// parent (which is the cp treemodel)
		// parent.addChild(modelToAppend.getRootNode());
		TreeNode root = modelToAppend.getRootNode();
		int chdCnt = root.getChildCount();
		
		// full cloning of ETH webclass energie takes about 4/100 of a second
		for (int i = chdCnt; i > 0; i--) {
			INode chd = root.getChildAt(i-1);
			INode chdc = (INode) XStreamHelper.xstreamClone(chd);
			// always insert before already existing course building block children
			parent.insert(chdc, 0);
		}
	}
	/**
	 * @param ne
	 * @return
	 */
	public static boolean mayAccessWholeTreeUp(NodeEvaluation ne) {
		NodeEvaluation curNodeEval = ne;
		boolean mayAccess;
		do {
			mayAccess = curNodeEval.isAtLeastOneAccessible();
			curNodeEval = (NodeEvaluation) curNodeEval.getParent();
		} while (curNodeEval != null && mayAccess);
		// top reached or may not access node
		return mayAccess;
	}

}
