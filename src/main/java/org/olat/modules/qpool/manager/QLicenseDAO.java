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
package org.olat.modules.qpool.manager;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.olat.core.commons.persistence.DB;
import org.olat.modules.qpool.model.QLicense;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 18.03.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service("qpoolLicenseDao")
public class QLicenseDAO implements ApplicationListener<ContextRefreshedEvent> {
	
	@Autowired
	private DB dbInstance;
	
	protected static final String[] defaultLicenses = new String[] {
		"all rights reserved",
		"CC by", "CC by-sa", "CC by-nd", "CC by-nc", "CC by-nc-sa", "CC by-nc-nd"
	};
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		createDefaultLicenses();
	}
	
	protected void createDefaultLicenses() {
		List<QLicense> licenses = getLicenses();
		Set<String> licenseKeys = new HashSet<String>();
		for(QLicense license:licenses) {
			licenseKeys.add(license.getLicenseKey());
		}
		for(String defaultLicenseKey:defaultLicenses) {
			if(!licenseKeys.contains(defaultLicenseKey)) {
				create(defaultLicenseKey, null, false);
			}
		}
		dbInstance.commitAndCloseSession();
	}
	
	public QLicense create(String licenseKey, String text, boolean deletable) {
		QLicense license = new QLicense();
		license.setCreationDate(new Date());
		license.setLicenseKey(licenseKey);
		license.setLicenseText(text);
		license.setDeletable(deletable);
		dbInstance.getCurrentEntityManager().persist(license);
		return license;
	}
	
	public QLicense loadById(Long key) {
		List<QLicense> licenses = dbInstance.getCurrentEntityManager()
				.createNamedQuery("loadQLicenseById", QLicense.class)
				.setParameter("licenseKey", key)
				.getResultList();
		if(licenses.isEmpty()) {
			return null;
		}
		return licenses.get(0);
	}
	
	public QLicense loadByLicenseKey(String licenseKey) {
		List<QLicense> licenses = dbInstance.getCurrentEntityManager()
				.createNamedQuery("loadQLicenseByLicenseKey", QLicense.class)
				.setParameter("licenseKey", licenseKey)
				.getResultList();
		if(licenses.isEmpty()) {
			return null;
		}
		return licenses.get(0);
	}
	
	
	public boolean delete(QLicense license) {
		QLicense reloadLicense = loadById(license.getKey());
		if(reloadLicense.isDeletable()) {
			dbInstance.getCurrentEntityManager().remove(reloadLicense);
			return true;
		}
		return false;
	}
	
	public List<QLicense> getLicenses() {
		return dbInstance.getCurrentEntityManager()
				.createNamedQuery("loadQLicenses", QLicense.class)
				.getResultList();
	}
}
