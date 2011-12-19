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
package org.olat.ims.cp.ui;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.olat.core.gui.control.generic.ajax.tree.AjaxTreeNode;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Encoder;
import org.olat.core.util.vfs.AbstractVirtualContainer;
import org.olat.core.util.vfs.VFSConstants;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.core.util.vfs.VFSStatus;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.core.util.vfs.filters.VFSItemFilter;
import org.olat.ims.cp.CPManager;
import org.olat.ims.cp.CPManagerImpl;
import org.olat.ims.cp.CPTreeDataModel;
import org.olat.ims.cp.ContentPackage;

/**
 * 
 * Description:<br>
 * Map the content (pages) of the CP to simulate the VFS
 * 
 * <P>
 * Initial Date:  4 mai 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
//fxdiff FXOLAT-125: virtual file system for CP
public class VFSCPContainer extends AbstractVirtualContainer implements VFSContainer {
	
	private static final OLog log = Tracing.createLoggerFor(VFSCPContainer.class);
	
	private VFSSecurityCallback secCallback;
	private final CPTreeDataModel treeModel;
	private final CPManagerImpl cpMgm;
	private final ContentPackage cp;
	private String rootNodeId;
	
	public VFSCPContainer(String name, ContentPackage cp) {
		super(name);
		
		String orgaIdentifier = cp.getFirstOrganizationInManifest().getIdentifier();
		rootNodeId = Encoder.encrypt(orgaIdentifier);

		this.cp = cp;
		cpMgm = (CPManagerImpl) CPManager.getInstance();
		treeModel = cpMgm.getTreeDataModel(cp);
	}
	
	@Override
	public boolean isSame(VFSItem vfsItem) {
		if(this == vfsItem) {
			return true;
		}
		return false;
	}
	
	@Override
	public VFSItem resolve(String path) {
		if(path == null || path.length() == 0 || path.equals("/")) {
			return this;
		}
		return VFSManager.resolveFile(this, path);
	}

	@Override
	public List<VFSItem> getItems() {
		return getItems(cp, treeModel, rootNodeId);
	}

	protected static List<VFSItem> getItems(ContentPackage cp, CPTreeDataModel model, String nodeId) {
		List<AjaxTreeNode> nodes = model.getChildrenFor(nodeId);
		
		CPManager cpMgm =CPManager.getInstance();

		List<VFSItem> items = new ArrayList<VFSItem>();
		for(AjaxTreeNode node:nodes) {
			try {
				String nid = (String)node.get(AjaxTreeNode.CONF_ID);
				String id = model.getIdentifierForNodeID(nid);
				String filePath = cpMgm.getPageByItemId(cp, id);
				String title = cpMgm.getItemTitle(cp, id);
				VFSItem f = cp.getRootDir().resolve(filePath);
				if(f instanceof VFSLeaf) {
					title += " (" + filePath + ")";
					
					VFSItem item;
					List<AjaxTreeNode> children = model.getChildrenFor(nid);
					if(children.isEmpty()) {
						item = new VFSCPNamedItem(title, (VFSLeaf)f);
					} else {
						item = new VFSCPNamedContainerItem(nid, title, (VFSLeaf)f, cp, model);
					}
					items.add(item);
				}
			} catch (JSONException e) {
				log.error("", e);
			}
		}

		return items;
	}

	@Override
	public List<VFSItem> getItems(VFSItemFilter filter) {
		return getItems();
	}

	@Override
	public VFSContainer getParentContainer() {
		return null;
	}

	@Override
	public void setParentContainer(VFSContainer parentContainer) {
		//
	}

	@Override
	public VFSStatus canWrite() {
		return VFSConstants.NO;
	}

	@Override
	public VFSSecurityCallback getLocalSecurityCallback() {
		return secCallback;
	}

	@Override
	public void setLocalSecurityCallback(VFSSecurityCallback secCallback) {
		this.secCallback = secCallback;
	}
}