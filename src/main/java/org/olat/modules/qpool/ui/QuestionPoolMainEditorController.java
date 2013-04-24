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
package org.olat.modules.qpool.ui;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.services.mark.MarkManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.stack.StackedController;
import org.olat.core.gui.components.stack.StackedControllerAware;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.MenuTree;
import org.olat.core.gui.components.tree.TreeDropEvent;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.tree.TreeHelper;
import org.olat.group.BusinessGroup;
import org.olat.modules.qpool.Pool;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QuestionItemCollection;
import org.olat.modules.qpool.QuestionItemShort;
import org.olat.modules.qpool.ui.admin.PoolsAdminController;
import org.olat.modules.qpool.ui.admin.QEducationalContextsAdminController;
import org.olat.modules.qpool.ui.admin.QItemTypesAdminController;
import org.olat.modules.qpool.ui.admin.QLicensesAdminController;
import org.olat.modules.qpool.ui.admin.TaxonomyAdminController;
import org.olat.modules.qpool.ui.datasource.CollectionOfItemsSource;
import org.olat.modules.qpool.ui.datasource.DefaultItemsSource;
import org.olat.modules.qpool.ui.datasource.SharedItemsSource;

/**
 * 
 * Initial date: 21.01.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QuestionPoolMainEditorController extends BasicController implements Activateable2, StackedControllerAware {

	private final MenuTree menuTree;
	private GenericTreeNode sharesNode, myNode;
	
	private final Panel content;
	private StackedController stackPanel;

	private QuestionsController currentCtrl;
	private QuestionsController myQuestionsCtrl;
	private QuestionsController markedQuestionsCtrl;
	
	private PoolsAdminController poolAdminCtrl;
	private QItemTypesAdminController typesCtrl;
	private QEducationalContextsAdminController levelsCtrl;
	private QLicensesAdminController licensesCtrl;
	private TaxonomyAdminController taxonomyCtrl;
	private LayoutMain3ColsController columnLayoutCtr;
	private QuestionPoolAdminStatisticsController adminStatisticsCtrl;
	private DialogBoxController copyToMyCtrl;

	private final Roles roles;
	private final MarkManager markManager;
	private final QPoolService qpoolService;
	
	public QuestionPoolMainEditorController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);

		roles = ureq.getUserSession().getRoles();
		markManager = CoreSpringFactory.getImpl(MarkManager.class);
		qpoolService = CoreSpringFactory.getImpl(QPoolService.class);
		
		menuTree = new MenuTree("qpoolTree");
		menuTree.setTreeModel(buildTreeModel());
		menuTree.setSelectedNode(menuTree.getTreeModel().getRootNode());
		menuTree.setDragEnabled(false);
		menuTree.setDropEnabled(true);
		menuTree.setDropSiblingEnabled(false);
		menuTree.addListener(this);
		menuTree.setRootVisible(false);
		//open the nodes shared and my at start
		List<String> openNodeIds = new ArrayList<String>(2);
		openNodeIds.add(myNode.getIdent());
		openNodeIds.add(sharesNode.getIdent());
		menuTree.setOpenNodeIds(openNodeIds);
		
		content = new Panel("list");
		columnLayoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), menuTree, null, content, "qpool");				
		
		putInitialPanel(columnLayoutCtr.getInitialComponent());
	}

	@Override
	public void setStackedController(StackedController stackPanel) {
		this.stackPanel = stackPanel;
		if(myQuestionsCtrl != null) myQuestionsCtrl.setStackedController(stackPanel);
		if(markedQuestionsCtrl != null) markedQuestionsCtrl.setStackedController(stackPanel);
		if(currentCtrl != null) currentCtrl.setStackedController(stackPanel);
	}

	@Override
	protected void doDispose() {
		//
	}
	
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(menuTree == source) {
			if(event instanceof TreeDropEvent) {
				TreeDropEvent e = (TreeDropEvent)event;
				String targetId = e.getTargetNodeId();
				String dropId = e.getDroppedNodeId();
				//drop id w_o_fi1000002357-4
				doDrop(ureq, targetId, dropId);
			} else if(menuTree.getSelectedNode() != null){
				TreeNode node = menuTree.getSelectedNode();
				doSelectControllerTreeNode(ureq, node, null, null);
			}
		}
	}
	
	private void doSelectControllerTreeNode(UserRequest ureq, TreeNode node, List<ContextEntry> entries, StateEntry state) {
		Object uNode = node.getUserObject();
		if("Statistics".equals(uNode)) {
			doSelectAdmin(ureq, entries, state);
		} else if("Taxonomy".equals(uNode)) {
			doSelectAdminStudyFields(ureq, entries, state);
		} else if("Pools".equals(uNode)) {
			doSelectAdminPools(ureq, entries, state);
		} else if("Types".equals(uNode)) {
			doSelectAdminTypes(ureq, entries, state);
		} else if("EduContexts".equals(uNode)) {
			doSelectAdminLevels(ureq, entries, state);
		} else if("Licenses".equals(uNode)) {
			doSelectAdminLicenses(ureq, entries, state);
		} else if("My".equals(uNode)) {
			doSelectMyQuestions(ureq, entries, state);
		} else if("Marked".equals(uNode)) {
			doSelectMarkedQuestions(ureq, entries, state);
		} else if(uNode instanceof Pool) {
			Pool pool = (Pool)uNode;
			doSelectPool(ureq, pool, node, entries, state);
		} else if(uNode instanceof BusinessGroup) {
			BusinessGroup group = (BusinessGroup)uNode;
			doSelectGroup(ureq, group, node, entries, state);
		} else if(uNode instanceof QuestionItemCollection) {
			QuestionItemCollection coll = (QuestionItemCollection)uNode;
			doSelectCollection(ureq, coll, node, entries, state);
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(event instanceof QPoolEvent) {
			if(QPoolEvent.ITEM_SHARED.equals(event.getCommand())) {
				buildShareSubTreeModel(sharesNode);
				menuTree.setDirty(true);
			}	else if(QPoolEvent.COLL_CREATED.equals(event.getCommand())
					|| QPoolEvent.COLL_CHANGED.equals(event.getCommand())) {
				buildMySubTreeModel(myNode);
				menuTree.setDirty(true);
			}	else if(QPoolEvent.COLL_DELETED.equals(event.getCommand())) {
				buildMySubTreeModel(myNode);
				menuTree.setDirty(true);
				menuTree.setSelectedNode(myNode);
				currentCtrl = null;
				content.popContent();
			} else if(QPoolEvent.POOL_CREATED.equals(event.getCommand())
					|| QPoolEvent.POOL_DELETED.equals(event.getCommand())) {
				buildShareSubTreeModel(sharesNode);
				menuTree.setDirty(true);
			}
		} else if(copyToMyCtrl == source) {
			if(DialogBoxUIFactory.isYesEvent(event) || DialogBoxUIFactory.isOkEvent(event)) {
				QuestionItemShort item = (QuestionItemShort)copyToMyCtrl.getUserObject();
				doCopyToMy(ureq, item);
			}
		}
		super.event(ureq, source, event);
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		ContextEntry entry = entries.get(0);
		OLATResourceable resource = entry.getOLATResourceable();
		TreeNode rootNode = menuTree.getTreeModel().getRootNode();
		TreeNode node = TreeHelper.findNodeByResourceableUserObject(resource, rootNode);
		if(node == null) {
			node = TreeHelper.findNodeByUserObject(resource.getResourceableTypeName(),  rootNode);
		}
		if(node != null) {
			List<ContextEntry> subEntries = entries.subList(1, entries.size());
			stackPanel.popUpToRootController(ureq);
			doSelectControllerTreeNode(ureq, node, subEntries, entry.getTransientState());
			menuTree.setSelectedNode(node);
		}
	}
	
	private void doDrop(UserRequest ureq, String targetId, String dropId) {
		try {
			int lastIndex = dropId.lastIndexOf('-');
			String rowStr = dropId.substring(lastIndex+1, dropId.length());
			int row = Integer.parseInt(rowStr);
			QuestionItemShort item = currentCtrl.getQuestionAt(row);
			TreeNode node = menuTree.getTreeModel().getNodeById(targetId);
			if(node != null) {
				Object userObj = node.getUserObject();
				if(userObj instanceof BusinessGroup) {
					qpoolService.shareItemsWithGroups(singletonList(item), singletonList((BusinessGroup)userObj), false);
					showInfo("item.shared", item.getTitle());
				} else if(userObj instanceof Pool) {
					qpoolService.addItemsInPools(singletonList(item), singletonList((Pool)userObj), false);
					showInfo("item.pooled", item.getTitle());
				} else if(userObj instanceof QuestionItemCollection) {
					qpoolService.addItemToCollection(singletonList(item), singletonList((QuestionItemCollection)userObj));
					showInfo("item.collectioned", item.getTitle());
				} else if("My".equals(userObj)) {
					doCopyToMyConfirmation(ureq, item);
				} else if("Marked".equals(userObj)) {
					String businessPath = "[QuestionItem:" + item.getKey() + "]";
					markManager.setMark(item, getIdentity(), null, businessPath);
				}
			}
		} catch (Exception e) {
			logError("Cannot drop with id: " + dropId, e);
		}
	}
	
	private void doCopyToMyConfirmation(UserRequest ureq, QuestionItemShort item) {
		String title = translate("copy");
		String text = translate("copy.confirmation");
		copyToMyCtrl = activateYesNoDialog(ureq, title, text, copyToMyCtrl);
		copyToMyCtrl.setUserObject(item);
	}
	
	private void doCopyToMy(UserRequest ureq, QuestionItemShort item) {
		List<QuestionItem> copiedItems = qpoolService.copyItems(getIdentity(), singletonList(item));
		showInfo("item.copied", Integer.toString(copiedItems.size()));
		if(myQuestionsCtrl != null) {
			myQuestionsCtrl.updateSource(ureq);
		}
	}
	
	private void setContent(UserRequest ureq, Controller controller, List<ContextEntry> entries, StateEntry state) {
		addToHistory(ureq, controller);
		if(controller instanceof Activateable2) {
			((Activateable2)controller).activate(ureq, entries, state);
		}
		content.setContent(controller.getInitialComponent());
	}
	
	private void doSelectAdmin(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(adminStatisticsCtrl == null) {
			WindowControl swControl = addToHistory(ureq, OresHelper.createOLATResourceableType("Statistics"), null);
			adminStatisticsCtrl = new QuestionPoolAdminStatisticsController(ureq, swControl);
			listenTo(adminStatisticsCtrl);
		} 
		setContent(ureq, adminStatisticsCtrl, entries, state);
	}
	
	private void doSelectAdminStudyFields(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(taxonomyCtrl == null) {
			WindowControl swControl = addToHistory(ureq, OresHelper.createOLATResourceableType("Taxonomy"), null);
			taxonomyCtrl = new TaxonomyAdminController(ureq, swControl);
			listenTo(taxonomyCtrl);
		}
		setContent(ureq, taxonomyCtrl, entries, state);
	}
	
	private void doSelectAdminPools(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(poolAdminCtrl == null) {
			WindowControl swControl = addToHistory(ureq, OresHelper.createOLATResourceableType("Pools"), null);
			poolAdminCtrl = new PoolsAdminController(ureq, swControl);
			listenTo(poolAdminCtrl);
		}
		setContent(ureq, poolAdminCtrl, entries, state);
	}
	
	private void doSelectAdminTypes(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(typesCtrl == null) {
			WindowControl swControl = addToHistory(ureq, OresHelper.createOLATResourceableType("Types"), null);
			typesCtrl = new QItemTypesAdminController(ureq, swControl);
			listenTo(typesCtrl);
		}
		setContent(ureq, typesCtrl, entries, state);
	}
	
	private void doSelectAdminLevels(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(levelsCtrl == null) {
			WindowControl swControl = addToHistory(ureq, OresHelper.createOLATResourceableType("EduContexts"), null);
			levelsCtrl = new QEducationalContextsAdminController(ureq, swControl);
			listenTo(levelsCtrl);
		}
		setContent(ureq, levelsCtrl, entries, state);
	}
	
	private void doSelectAdminLicenses(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(licensesCtrl == null) {
			WindowControl swControl = addToHistory(ureq, OresHelper.createOLATResourceableType("Licenses"), null);
			licensesCtrl = new QLicensesAdminController(ureq, swControl);
			listenTo(licensesCtrl);
		}
		setContent(ureq, licensesCtrl, entries, state);
	}
	
	private void doSelectMyQuestions(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		DefaultItemsSource source = new DefaultItemsSource(getIdentity(), ureq.getUserSession().getRoles(), "My"); 
		source.getDefaultParams().setAuthor(getIdentity());
		if(myQuestionsCtrl == null) {
			WindowControl swControl = addToHistory(ureq, OresHelper.createOLATResourceableType("My"), null);
			myQuestionsCtrl = new QuestionsController(ureq, swControl, source);
			myQuestionsCtrl.setStackedController(stackPanel);
			listenTo(myQuestionsCtrl);
		} else {
			myQuestionsCtrl.updateSource(ureq, source);
		}
		currentCtrl = myQuestionsCtrl;
		setContent(ureq, myQuestionsCtrl, entries, state);
	}
	
	private void doSelectMarkedQuestions(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		DefaultItemsSource source = new DefaultItemsSource(getIdentity(), ureq.getUserSession().getRoles(), "Fav"); 
		source.getDefaultParams().setFavoritOnly(true);
		if(markedQuestionsCtrl == null) {
			WindowControl swControl = addToHistory(ureq, OresHelper.createOLATResourceableType("Marked"), null);
			markedQuestionsCtrl = new QuestionsController(ureq, swControl, source);
			markedQuestionsCtrl.setStackedController(stackPanel);
			listenTo(markedQuestionsCtrl);
		} else {
			markedQuestionsCtrl.updateSource(ureq, source);
		}
		;
		currentCtrl = markedQuestionsCtrl;
		setContent(ureq, markedQuestionsCtrl, entries, state);
	}
	
	private void doSelectPool(UserRequest ureq, Pool pool, TreeNode node, List<ContextEntry> entries, StateEntry state) {
		ControlledTreeNode cNode = (ControlledTreeNode)node;
		QuestionsController selectedPoolCtrl = cNode.getController();

		DefaultItemsSource source = new DefaultItemsSource(getIdentity(), ureq.getUserSession().getRoles(), pool.getName());
		source.getDefaultParams().setPoolKey(pool.getKey());
		if(selectedPoolCtrl == null) {
			WindowControl swControl = addToHistory(ureq, pool, null);
			selectedPoolCtrl = new QuestionsController(ureq, swControl, source);
			selectedPoolCtrl.setStackedController(stackPanel);
			listenTo(selectedPoolCtrl);
			cNode.setController(selectedPoolCtrl);
		} else {
			selectedPoolCtrl.updateSource(ureq, source);
		}
		currentCtrl = selectedPoolCtrl;
		setContent(ureq, selectedPoolCtrl, entries, state);
	}
	
	private void doSelectGroup(UserRequest ureq, BusinessGroup group, TreeNode node, List<ContextEntry> entries, StateEntry state) {
		ControlledTreeNode cNode = (ControlledTreeNode)node;
		QuestionsController sharedItemsCtrl = cNode.getController();

		if(sharedItemsCtrl == null) {
			WindowControl swControl = addToHistory(ureq, group, null);
			sharedItemsCtrl = new QuestionsController(ureq, swControl, new SharedItemsSource(group, getIdentity(), ureq.getUserSession().getRoles()));
			sharedItemsCtrl.setStackedController(stackPanel);
			listenTo(sharedItemsCtrl);
			cNode.setController(sharedItemsCtrl);
		} else {
			sharedItemsCtrl.updateSource(ureq, new SharedItemsSource(group, getIdentity(), ureq.getUserSession().getRoles()));
		}
		currentCtrl = sharedItemsCtrl;
		setContent(ureq, sharedItemsCtrl, entries, state);
	}
	
	private void doSelectCollection(UserRequest ureq, QuestionItemCollection coll, TreeNode node, List<ContextEntry> entries, StateEntry state) {
		ControlledTreeNode cNode = (ControlledTreeNode)node;
		CollectionQuestionsController collItemsCtrl = (CollectionQuestionsController)cNode.getController();
		
		CollectionOfItemsSource source = new CollectionOfItemsSource(coll, getIdentity(), ureq.getUserSession().getRoles());
		if(collItemsCtrl == null) {
			WindowControl swControl = addToHistory(ureq, coll, null);
			collItemsCtrl = new CollectionQuestionsController(ureq, swControl, source);
			collItemsCtrl.setStackedController(stackPanel);
			listenTo(collItemsCtrl);
			cNode.setController(collItemsCtrl);
		} else {
			collItemsCtrl.updateSource(ureq, source);
		}
		collItemsCtrl.activate(ureq, entries, state);
		currentCtrl = collItemsCtrl;
		setContent(ureq, collItemsCtrl, entries, state);
	}
	
	private TreeModel buildTreeModel() {
		QuestionPoolMenuTreeModel gtm = new QuestionPoolMenuTreeModel();
		GenericTreeNode rootNode = new GenericTreeNode(translate("topnav.qpool"), "topnav.qpool.alt");
		rootNode.setCssClass("o_sel_qpool_home");
		gtm.setRootNode(rootNode);
		
		//question database
		myNode = new GenericTreeNode(translate("menu.database"), "menu.database");
		myNode.setCssClass("o_sel_qpool_database");
		rootNode.addChild(myNode);
		buildMySubTreeModel(myNode);

		//pools + shares
		sharesNode = new GenericTreeNode(translate("menu.share"), "menu.share");
		sharesNode.setCssClass("o_sel_qpool_shares");
		rootNode.addChild(sharesNode);	
		buildShareSubTreeModel(sharesNode);
		
		//administration
		if(roles.isOLATAdmin() || roles.isPoolAdmin()) {
			GenericTreeNode adminNode = new GenericTreeNode(translate("menu.admin"), "Statistics");
			adminNode.setCssClass("o_sel_qpool_admin");
			rootNode.addChild(adminNode);
			buildAdminSubTreeModel(adminNode);
		}
		return gtm;
	}
	
	private void buildShareSubTreeModel(GenericTreeNode parentNode) {
		parentNode.removeAllChildren();
		
		List<Pool> pools = qpoolService.getPools(getIdentity(), roles);
		for(Pool pool:pools) {
			GenericTreeNode node = new ControlledTreeNode(pool.getName(), pool);
			node.setIconCssClass("o_sel_qpool_pool");
			parentNode.addChild(node);
		}

		List<BusinessGroup> groups = qpoolService.getResourcesWithSharedItems(getIdentity());
		for(BusinessGroup group:groups) {
			GenericTreeNode node = new ControlledTreeNode(group.getName(), group);
			node.setIconCssClass("o_sel_qpool_share");
			parentNode.addChild(node);
		}
	}
	
	private void buildAdminSubTreeModel(GenericTreeNode parentNode) {
		if(!roles.isOLATAdmin() && !roles.isPoolAdmin()) return;
		parentNode.removeAllChildren();
		
		GenericTreeNode node = new GenericTreeNode(translate("menu.admin.studyfields"), "Taxonomy");
		node.setIconCssClass("o_sel_qpool_study_fields");
		parentNode.addChild(node);
		
		node = new GenericTreeNode(translate("menu.admin.pools"), "Pools");
		node.setIconCssClass("o_sel_qpool_admin_pools");
		parentNode.addChild(node);
		
		node = new GenericTreeNode(translate("menu.admin.types"), "Types");
		node.setIconCssClass("o_sel_qpool_admin_types");
		parentNode.addChild(node);
		
		node = new GenericTreeNode(translate("menu.admin.levels"), "EduContexts");
		node.setIconCssClass("o_sel_qpool_admin_levels");
		parentNode.addChild(node);

		node = new GenericTreeNode(translate("menu.admin.licenses"), "Licenses");
		node.setIconCssClass("o_sel_qpool_admin_licenses");
		parentNode.addChild(node);
	}
	
	private void buildMySubTreeModel(GenericTreeNode parentNode) {
		parentNode.removeAllChildren();
		
		GenericTreeNode node = new GenericTreeNode(translate("menu.database.my"), "My");
		node.setIconCssClass("o_sel_qpool_my_items");
		parentNode.addChild(node);
		
		node = new GenericTreeNode(translate("menu.database.favorit"), "Marked");
		node.setIconCssClass("o_sel_qpool_favorits");
		parentNode.addChild(node);
		
		List<QuestionItemCollection> collections = qpoolService.getCollections(getIdentity());
		for(QuestionItemCollection coll: collections) {
			node = new ControlledTreeNode(coll.getName(), coll);
			node.setIconCssClass("o_sel_qpool_collection");
			parentNode.addChild(node);
		}
	}
	
	private static class ControlledTreeNode extends GenericTreeNode {
		private static final long serialVersionUID = 768640290449143804L;
		private QuestionsController controller;
		
		public ControlledTreeNode(String title, Object userObject) {
			super(title, userObject);
		}

		public QuestionsController getController() {
			return controller;
		}

		public void setController(QuestionsController controller) {
			this.controller = controller;
		}
	}
}
