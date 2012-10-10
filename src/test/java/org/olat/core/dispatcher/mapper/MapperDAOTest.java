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
package org.olat.core.dispatcher.mapper;

import java.util.Calendar;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.dispatcher.mapper.manager.MapperDAO;
import org.olat.core.dispatcher.mapper.model.PersistedMapper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class MapperDAOTest extends OlatTestCase {
	
	@Autowired
	private MapperDAO mapperDao;
	@Autowired
	private DB dbInstance;
	
	@Test
	public void testCreateMapper() {
		String mapperId = UUID.randomUUID().toString();
		String sessionId = UUID.randomUUID().toString().substring(0, 32);
		PersistentMapper mapper = new PersistentMapper(mapperId);
		
		PersistedMapper pMapper = mapperDao.persistMapper(sessionId, mapperId, mapper);
		Assert.assertNotNull(pMapper);
		Assert.assertNotNull(pMapper.getKey());
		Assert.assertNotNull(pMapper.getCreationDate());
		Assert.assertNotNull(pMapper.getXmlConfiguration());
		Assert.assertEquals(mapperId, pMapper.getMapperId());
		Assert.assertEquals(sessionId, pMapper.getOriginalSessionId());
		
		dbInstance.commit();	
	}
	
	@Test
	public void testLoadMapperByMapperId() {
		//create a mapper
		String mapperId = UUID.randomUUID().toString();
		String sessionId = UUID.randomUUID().toString().substring(0, 32);
		PersistedMapper pMapper = mapperDao.persistMapper(sessionId, mapperId, null);
		Assert.assertNotNull(pMapper);
		dbInstance.commitAndCloseSession();
		
		//load the mapper
		PersistedMapper loadedMapper = mapperDao.loadByMapperId(mapperId);
		Assert.assertNotNull(loadedMapper);
		Assert.assertEquals(pMapper, loadedMapper);
		Assert.assertEquals(mapperId, loadedMapper.getMapperId());
		Assert.assertEquals(sessionId, loadedMapper.getOriginalSessionId());
	}
	
	@Test
	public void testDeleteMapperByMapper() throws Exception {
		//create mappers

		String mapperIdToDelete = null;
		for(int i=0; i<10; i++) {
			mapperIdToDelete = UUID.randomUUID().toString();
			String sessionId = UUID.randomUUID().toString().substring(0, 32);
			mapperDao.persistMapper(sessionId, mapperIdToDelete, null);
		}
		dbInstance.commitAndCloseSession();
		
		Calendar cal = Calendar.getInstance();
		Thread.sleep(5000);
		
		//create a new mapper
		String mapperId = UUID.randomUUID().toString();
		String sessionId = UUID.randomUUID().toString().substring(0, 32);
		mapperDao.persistMapper(sessionId, mapperId, null);
		dbInstance.commitAndCloseSession();
		
		//delete old mappers
		cal.add(Calendar.SECOND, 3);
		int numOfDeletedRow = mapperDao.deleteMapperByDate(cal.getTime());
		Assert.assertTrue(numOfDeletedRow >= 10);

		//load the last mapper
		PersistedMapper loadedMapper = mapperDao.loadByMapperId(mapperId);
		Assert.assertNotNull(loadedMapper);
		//try to load a deleted mapper
		PersistedMapper deletedMapper = mapperDao.loadByMapperId(mapperIdToDelete);
		Assert.assertNull(deletedMapper);
	}

}