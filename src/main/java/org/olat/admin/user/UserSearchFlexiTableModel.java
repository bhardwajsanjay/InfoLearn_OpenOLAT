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
package org.olat.admin.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModel;
import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class UserSearchFlexiTableModel extends DefaultTableDataModel<UserResultWrapper> implements FlexiTableDataModel<UserResultWrapper> {
	private Locale locale;
	private boolean isAdministrativeUser;
	private FlexiTableColumnModel columnModel;
	private List<UserPropertyHandler> userPropertyHandlers;
	
	public UserSearchFlexiTableModel(List<UserResultWrapper> identities, List<UserPropertyHandler> userPropertyHandlers,
			boolean isAdministrativeUser, Locale locale, FlexiTableColumnModel columnModel) {
		super(identities);
		this.locale = locale;
		this.columnModel = columnModel;
		this.isAdministrativeUser = isAdministrativeUser;
		this.userPropertyHandlers = userPropertyHandlers;
	}

	@Override
	public FlexiTableColumnModel getTableColumnModel() {
		return columnModel;
	}

	@Override
	public void setTableColumnModel(FlexiTableColumnModel tableColumnModel) {
		columnModel = tableColumnModel;
	}

	@Override
	public int getColumnCount() {
		return columnModel.getColumnCount();
	}

	@Override
	public Object getValueAt(int row, int col) {
		UserResultWrapper option = getObject(row);
		if(col == 0) {
			return option.getSelectEl();
		}
		if(col == 1 && isAdministrativeUser) {
			return option.getIdentity().getName();
		} 

		int pos = isAdministrativeUser ? col - 2 : col - 1;
		if(pos >= 0 && pos < userPropertyHandlers.size()) {
			UserPropertyHandler handler = userPropertyHandlers.get(pos);
			return handler.getUserProperty(option.getIdentity().getUser(), locale);
		} else if(pos > 0) {
			return option.getSelectLink();
		}
		return "";
	}

	@Override
	public UserSearchFlexiTableModel createCopyWithEmptyList() {
		return new UserSearchFlexiTableModel(new ArrayList<UserResultWrapper>(), userPropertyHandlers, isAdministrativeUser, locale, columnModel);
	}
}
