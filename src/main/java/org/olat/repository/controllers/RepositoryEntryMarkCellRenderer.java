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
package org.olat.repository.controllers;

import java.util.Locale;
import java.util.UUID;

import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.CustomCellRenderer;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.repository.RepositoryEntry;

/**
 * 
 * Render to mark a group
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class RepositoryEntryMarkCellRenderer implements CustomCellRenderer {
	
	private final Translator translator;
	private final VelocityContainer container;
	private final Controller listeningController;
	
	public RepositoryEntryMarkCellRenderer(Controller listeningController, VelocityContainer container, Translator translator) {
		this.listeningController = listeningController;
		this.container = container;
		this.translator = translator;
	}

	@Override
	public void render(StringOutput sb, Renderer renderer, Object val, Locale locale, int alignment, String action) {
		if(val instanceof RepositoryEntry && renderer != null) {
			RepositoryEntry item = (RepositoryEntry)val;
			sb.append("<div class='b_mark'>");
			
			Link link = LinkFactory.createLink("marked_" + UUID.randomUUID().toString(), container, listeningController);
			link.setCustomDisplayText("&#160;&#160;&#160;");
			if(false /*item.isMarked()*/) {
				link.setCustomEnabledLinkCSS("b_mark_set");
			} else {
				link.setCustomEnabledLinkCSS("b_mark_not_set");
			}
			link.setUserObject(item);
			URLBuilder ubu = renderer.getUrlBuilder().createCopyFor(link);
			RenderResult renderResult = new RenderResult();
			link.getHTMLRendererSingleton().render(renderer, sb, link, ubu, translator, renderResult, null);
			sb.append("</div>");
		}
	}
}
