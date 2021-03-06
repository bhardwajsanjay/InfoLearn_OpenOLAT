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
package org.olat.util;

import org.olat.core.util.filter.FilterFactory;

/**
 * 
 * @author jkraehemann, joel.kraehemann@frentix.com, frentix.com
 */
public class FunctionalHtmlUtil {
	
	/**
	 * Strips all markup of specified string.
	 * This method is depracted and shouldn't be used in newly written code.
	 * Use removeMarkup instead.
	 * 
	 * @param html
	 * @param insertNewlines
	 * @return
	 */
	@Deprecated
	public String stripTags(String html, boolean insertNewlines){
		if(html.indexOf("<body") != -1){
			html = html.substring(html.indexOf('>', html.indexOf("<body")) + 1, html.indexOf("</body"));
		}
		
		StringBuffer textBuffer = new StringBuffer();
		int offset = 0;
		int nextOffset = 0;
		char prevLineLastChar = '\n';
		
		while((nextOffset = html.indexOf('<', offset)) != -1){
			String currentText = html.substring(offset, nextOffset);
			
			if(!currentText.matches("^[\\s]+$")){
				currentText = currentText.trim();
				textBuffer.append(currentText);
				
				if(insertNewlines){
					if(prevLineLastChar != '\n'){
						textBuffer.append('\n');
					}
				}else{
					if(prevLineLastChar != '\n' && prevLineLastChar != ' '){
						textBuffer.append(' ');
					}
				}
				
				if(!currentText.isEmpty()){
					prevLineLastChar = currentText.charAt(currentText.length() - 1);
				}
			}
			
			offset = html.indexOf('>', nextOffset);
			
			if(offset != -1){
				offset += 1;
			}
		}
		 
		String currentText = html.substring(offset);
		
		if(!currentText.matches("^[\\s]+$")){
			textBuffer.append(currentText);
			
			if(!currentText.isEmpty()){
				prevLineLastChar = currentText.charAt(currentText.length() - 1);
			}
		}
		
		if(prevLineLastChar != '\n'){
			textBuffer.append('\n');
		}
		
		return(textBuffer.toString());
	}
	
	public String removeMarkup(String html){
		 return(FilterFactory.getHtmlTagsFilter().filter(html));
	}
}
