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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.course.nodes.fo;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlsite.OlatCmdEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.util.Formatter;
import org.olat.core.util.Util;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.Message;

/**
 * <h3>Description:</h3> The forum peekview controller displays the configurable
 * amount of the newest forum messages.
 * <p>
 * <h4>Events fired by this Controller</h4>
 * <ul>
 * <li>OlatCmdEvent to notify that a jump to the course node is desired</li>
 * </ul>
 * <p>
 * Initial Date: 29.09.2009 <br>
 * 
 * @author gnaegi, gnaegi@frentix.com, www.frentix.com
 */
public class FOPeekviewController extends BasicController implements Controller {
	// the current course node id
	private final String nodeId;

	/**
	 * Constructor
	 * @param ureq The user request
	 * @param wControl The window control
	 * @param forum The forum instance
	 * @param nodeId The course node ID
	 * @param itemsToDisplay number of items to be displayed, must be > 0
	 */
	public FOPeekviewController(UserRequest ureq, WindowControl wControl, Forum forum, String nodeId, int itemsToDisplay) {		
		// Use fallback translator from forum
		super(ureq, wControl, Util.createPackageTranslator(Forum.class, ureq.getLocale()));
		this.nodeId = nodeId;
	
		VelocityContainer peekviewVC = createVelocityContainer("peekview");
		// add items, only as many as configured
		ForumManager foMgr = ForumManager.getInstance();
		List<Message> messages = foMgr.getMessagesByForumID(forum.getKey(), 0, itemsToDisplay, Message.OrderBy.creationDate, false);
		// only take the configured amount of messages
		for (Message message :messages) {
			// add link to item
			// Add link to jump to course node
			Link nodeLink = LinkFactory.createLink("nodeLink_" + message.getKey(), peekviewVC, this);
			nodeLink.setCustomDisplayText(message.getTitle());
			nodeLink.setCustomEnabledLinkCSS("b_with_small_icon_left o_forum_message_icon o_gotoNode");
			nodeLink.setUserObject(Long.toString(message.getKey()));				
		}
		peekviewVC.contextPut("messages", messages);
		// Add link to show all items (go to node)
		Link allItemsLink = LinkFactory.createLink("peekview.allItemsLink", peekviewVC, this);
		allItemsLink.setCustomEnabledLinkCSS("b_float_right");
		// Add Formatter for proper date formatting
		peekviewVC.contextPut("formatter", Formatter.getInstance(getLocale()));
		//
		this.putInitialPanel(peekviewVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source instanceof Link) {
			Link nodeLink = (Link) source;
			String messageId = (String) nodeLink.getUserObject();
			if (messageId == null) {
				fireEvent(ureq, new OlatCmdEvent(OlatCmdEvent.GOTONODE_CMD, nodeId));								
			} else {
				fireEvent(ureq, new OlatCmdEvent(OlatCmdEvent.GOTONODE_CMD, nodeId + "/" + messageId));				
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing to dispose
	}

}