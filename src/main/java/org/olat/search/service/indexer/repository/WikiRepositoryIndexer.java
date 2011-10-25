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

package org.olat.search.service.indexer.repository;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.fileresource.types.WikiResource;
import org.olat.modules.wiki.Wiki;
import org.olat.modules.wiki.WikiManager;
import org.olat.modules.wiki.WikiPage;
import org.olat.repository.RepositoryEntry;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.document.WikiPageDocument;
import org.olat.search.service.indexer.Indexer;
import org.olat.search.service.indexer.OlatFullIndexer;

/**
 * Index a repository entry of type wiki.
 * @author Christian Guretzki
 */
public class WikiRepositoryIndexer implements Indexer {
	private static OLog log = Tracing.createLoggerFor(WikiRepositoryIndexer.class);
	
	// Must correspond with LocalString_xx.properties
	// Do not use '_' because we want to seach for certain documenttypes and lucene has problems with '_' 
	public final static String TYPE = "type.repository.entry.wiki";

	public final static String ORES_TYPE_WIKI = WikiResource.TYPE_NAME;
	
	public WikiRepositoryIndexer() {
		// Repository types
		
	}
	
	/**
	 * 
	 */
	public String getSupportedTypeName() {	
		return ORES_TYPE_WIKI; 
	}
	
	/**
	 * @see org.olat.repository.handlers.RepositoryHandler#supportsDownload()
	 */

	public void doIndex(SearchResourceContext resourceContext, Object parentObject, OlatFullIndexer indexWriter) throws IOException,InterruptedException  {
		RepositoryEntry repositoryEntry = (RepositoryEntry) parentObject;
		if (log.isDebug()) log.debug("Analyse Wiki RepositoryEntry...");
		String repoEntryName = "*name not available*";
		try {
			repoEntryName = repositoryEntry.getDisplayname();
			Wiki wiki = WikiManager.getInstance().getOrLoadWiki(repositoryEntry.getOlatResource());
			// loop over all wiki pages
			List<WikiPage> wikiPageList = wiki.getAllPagesWithContent();
			for (WikiPage wikiPage : wikiPageList) {
		    try {
					resourceContext.setDocumentType(TYPE);
					resourceContext.setDocumentContext(Long.toString(repositoryEntry.getKey()));
					resourceContext.setParentContextType(TYPE);
					resourceContext.setParentContextName(wikiPage.getPageName());
					resourceContext.setFilePath(wikiPage.getPageName());

					Document document = WikiPageDocument.createDocument(resourceContext, wikiPage);
					indexWriter.addDocument(document);
				} catch (Exception e) {
					log.error("Error indexing wiki page:" + repoEntryName + " " + (wikiPage == null ? "null" : wikiPage.getPageName()), e);
				}
			}
		} catch (Exception e) {
			log.error("Error indexing wiki:" + repoEntryName, e);
		}
	}


	/**
	 * Bean setter method used by spring. 
	 * @param indexerList
	 */
	public void setIndexerList(List indexerList) {
	}

	public boolean checkAccess(ContextEntry contextEntry, BusinessControl businessControl, Identity identity, Roles roles) {
		return true;
	}

}