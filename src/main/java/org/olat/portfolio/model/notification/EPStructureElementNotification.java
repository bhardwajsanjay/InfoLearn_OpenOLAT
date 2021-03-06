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
package org.olat.portfolio.model.notification;

import java.util.Date;

import org.olat.basesecurity.IdentityShort;
import org.olat.core.commons.persistence.PersistentObject;

/**
 * Map the notification for structure element
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class EPStructureElementNotification extends PersistentObject implements EPNotification {

	private static final long serialVersionUID = -1065069940086963966L;
	
	private Date lastModified;
	private String type;
	private String title;
	
	private Long rootKey;
	private Long rootMapKey;
	
	private Long pageKey;

	@Override
	public Date getLastModified() {
		return lastModified;
	}

	@Override
	public void setLastModified(Date date) {
		this.lastModified = date;
	}

	public Long getRootKey() {
		return rootKey;
	}

	public void setRootKey(Long rootKey) {
		this.rootKey = rootKey;
	}

	public Long getRootMapKey() {
		return rootMapKey;
	}

	public void setRootMapKey(Long rootMapKey) {
		this.rootMapKey = rootMapKey;
	}

	public Long getPageKey() {
		return pageKey;
	}

	public void setPageKey(Long pageKey) {
		this.pageKey = pageKey;
	}

	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public IdentityShort getAuthor() {
		return null;
	}
	
	@Override
	public String toString() {
		return "StructureELNotification[" + super.toString() + ";pageKey=" + pageKey + "]";
	}
	
	@Override
	public int hashCode() {
		return getKey().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj instanceof EPStructureElementNotification) {
			EPStructureElementNotification notification = (EPStructureElementNotification)obj;
			return getKey().equals(notification.getKey());
		}
		return false;
	}
}