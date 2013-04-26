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
package org.olat.restapi.support;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.WindowBackOffice;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.info.WindowControlInfo;
import org.olat.core.id.context.BusinessControl;

/**
 * 
 * Description:<br>
 * Collect messages: info, warning and error<br>
 * WARNING! use this class with extrem cautious and only if you know what you do
 * as some methods return null!
 * <P>
 * Initial Date:  6 oct. 2011 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class ErrorWindowControl implements WindowControl {
	private String info;
	private String error;
	private String warning;
	
	public ErrorWindowControl() {/* */}
	
	@Override
	public void pushToMainArea(Component comp) {/* */}

	@Override
	public void pushAsModalDialog(Component comp) {/* */}
	
	@Override
	public void pushAsCallout(Component comp, String targetId) {/* */}

	@Override
	public void pop() {/* */}

	public String getInfo() {
		return info;
	}

	@Override
	public void setInfo(String string) {
		this.info = string;
	}
	
	public String getError() {
		return error;
	}
	@Override
	public void setError(String string) {
		this.error = string;
	}
	
	public String getWarning() {
		return warning;
	}

	@Override
	public void setWarning(String string) {
		this.warning = string;
	}

	@Override
	public WindowControlInfo getWindowControlInfo() {
		return null;
	}

	@Override
	public void makeFlat() {/* */}

	@Override
	public BusinessControl getBusinessControl() {
		return null;
	}

	@Override
	public WindowBackOffice getWindowBackOffice() {
		return null;
	}
}