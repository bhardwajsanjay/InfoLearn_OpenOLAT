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

package org.olat.modules.fo;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.user.UserManager;

/**
* @author Felix Jost
*/
public class ForumMessagesTableDataModel extends DefaultTableDataModel<Message> {

	private Set<Long> readMsgs;
	private UserManager userManager;

	public ForumMessagesTableDataModel(UserManager userManager, List<Message> objects, Set<Long> readMsgs) {
		super(objects);
		this.readMsgs = readMsgs;
		this.userManager = userManager;
	}

	public int getColumnCount() {
		return 4;
	}

	/**
	 * 4 columns: 
	 * first is the title of the message
	 * second the (name, firstname) of the creator
	 * third lastModifiedDate
	 * fourth if it is new or not
	 */
	public final Object getValueAt(int row, int col) {
		Message m= getObject(row);
		switch (col) {
			case 0 :
				String title = m.getTitle();
				return title;
			case 1 :
				return userManager.getUserDisplayName(m.getCreator().getUser());
			case 2 :
				Date mod = m.getLastModified();
				return mod;
			case 3: // return if we have read this message before or not
				return ( readMsgs.contains(m.getKey())? Boolean.TRUE : Boolean.FALSE );
			default :
				return "error";
		}
	}
}
	
