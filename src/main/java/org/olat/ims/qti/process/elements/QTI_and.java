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
 * <p>
 */

package org.olat.ims.qti.process.elements;

import java.util.List;

import org.dom4j.Element;
import org.olat.ims.qti.container.ItemContext;
import org.olat.ims.qti.process.QTIHelper;

public class QTI_and implements BooleanEvaluable{

	public boolean eval(Element boolElement, ItemContext userContext, EvalContext ect) {
		List elems = boolElement.elements();
		int size = elems.size();
		boolean ev = true;
		for (int i = 0; i < size; i++) {
			Element child = (Element)elems.get(i);
			String name = child.getName();
			BooleanEvaluable bev = QTIHelper.getBooleanEvaluableInstance(name);
			boolean res = bev.eval(child, userContext, ect);
			ev = ev && res;
			if (!ev) return false;
		}
		return true;
	}
}