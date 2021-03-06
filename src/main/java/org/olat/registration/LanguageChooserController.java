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
package org.olat.registration;

import java.util.Locale;
import java.util.Map;

import org.olat.core.commons.chiefcontrollers.LanguageChangedEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.StringHelper;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.resource.OresHelper;
import org.olat.dispatcher.LocaleNegotiator;


/**
 * Description:<br>
 * TODO: srosse Class Description for LanguageChooserController
 * 
 * <P>
 * Initial Date:  17 nov. 2009 <br>
 * @author srosse, stephane.rosse@frentix.com, www.frentix.com
 */
public class LanguageChooserController extends FormBasicController {

	private String curlang;
	private FormSubmit nextButton;
	private SingleSelection langs;
	
	private boolean fireStandardEvent = true;
	
	
	/**
	 * @param ureq
	 * @param wControl
	 * @param fireStandardEvent fire event with the standard fireEvent method of the controller, otherwise
	 * use the singleUserEventCenter
	 */
	public LanguageChooserController(UserRequest ureq, WindowControl wControl, boolean fireStandardEvent) {
		super(ureq, wControl);
		this.fireStandardEvent = fireStandardEvent;
		curlang = ureq.getLocale().toString();
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDefaultController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDefaultController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem,
	 *      org.olat.core.gui.components.form.flexible.FormEvent)
	 */
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == langs) {
			Locale loc = I18nManager.getInstance().getLocaleOrDefault(getSelectedLanguage());
			MultiUserEvent mue = new LanguageChangedEvent(loc, ureq);
			setLocale(loc, true);
			ureq.getUserSession().setLocale(loc);
			ureq.getUserSession().putEntry(LocaleNegotiator.NEGOTIATED_LOCALE, loc);
			
			FormLayoutContainer langLayout = (FormLayoutContainer)flc.getFormComponent("langLayout");
			langLayout.contextPut("languageCode", loc.toString());
			langLayout.contextPut("selectLanguage", translate("select.language"));
			nextButton.setTranslator(getTranslator());
			
			if(fireStandardEvent) {
				fireEvent(ureq, mue);
			} else {
				OLATResourceable wrappedLocale = OresHelper.createOLATResourceableType(Locale.class);
				ureq.getUserSession().getSingleUserEventCenter().fireEventToListenersOf(mue, wrappedLocale);
			}
		}
	}

	/**
	 * selected language
	 * @return
	 */
	public String getSelectedLanguage() {
		return langs.getSelectedKey();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDefaultController#initFormElements(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller)
	 */
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, final UserRequest ureq) {
		Map<String, String> languages = I18nManager.getInstance().getEnabledLanguagesTranslated();
		String[] langKeys = StringHelper.getMapKeysAsStringArray(languages);
		String[] langValues = StringHelper.getMapValuesAsStringArray(languages);
		ArrayHelper.sort(langKeys, langValues, false, true, false);
		// Build css classes for reference languages
		String[] langCssClasses = I18nManager.getInstance().createLanguageFlagsCssClasses(langKeys, "b_with_small_icon_left");
		
		final FormLayoutContainer langLayout = FormLayoutContainer.createCustomFormLayout("langLayout", getTranslator(), velocity_root + "/langchooser.html");
		formLayout.add(langLayout);
		langs = uifactory.addDropdownSingleselect("select.language", langLayout, langKeys, langValues, langCssClasses); 
		langs.addActionListener(this, FormEvent.ONCHANGE);
		langs.select(curlang, true);
		Locale loc = I18nManager.getInstance().getLocaleOrDefault(curlang);
		langLayout.contextPut("languageCode", loc.toString());
		langLayout.contextPut("selectLanguage", translate("select.language"));
		
		final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("buttonLayout", getTranslator());
		formLayout.add(buttonLayout);
		nextButton = uifactory.addFormSubmitButton("submit.weiter", buttonLayout);
		uifactory.addFormCancelButton("cancel", buttonLayout, ureq, getWindowControl());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		langs = null;
	}
}