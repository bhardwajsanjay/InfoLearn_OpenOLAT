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

package org.olat.course.condition.interpreter;

import com.neemsoft.jmep.UnitCB;

/**
 * Initial Date: Feb 6, 2004
 * @author Mike Stock Comment:
 */
public class MinuteUnit extends UnitCB {

	private static final long MINUTEMILLIS = 60 * 1000;

	/**
	 * 
	 */
	public MinuteUnit() {
		super();
	}

	/**
	 * @see com.neemsoft.jmep.UnitCB#apply(java.lang.Object)
	 */
	public Object apply(Object arg0) {
		if (arg0 instanceof Integer) return new Double(((Integer) arg0).intValue() * MINUTEMILLIS);
		if (arg0 instanceof Double) return new Double(((Double) arg0).doubleValue() * MINUTEMILLIS);
		return null;
	}

}