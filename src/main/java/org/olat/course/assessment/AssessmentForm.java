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
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.course.assessment;

import java.util.Locale;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;


/**
 * Initial Date:  Jun 24, 2004
 *
 * @author gnaegi
 */
public class AssessmentForm extends FormBasicController {
	
	private TextElement score;
	private IntegerElement attempts;
	private StaticTextElement cutVal;
	private SingleSelection passed;
	private TextElement userComment, coachComment;

	private boolean hasScore, hasPassed, hasComment, hasAttempts;
	Float min, max, cut;
	Locale locale;

	private AssessedIdentityWrapper assessedIdentityWrapper;
	private AssessableCourseNode assessableCourseNode;
	
	private Integer attemptsValue;
	private Float   scoreValue;
	private String  userCommentValue;
	private String  coachCommentValue;
	
	/**
	 * Constructor for an assessment detail form. The form will be configured according
	 * to the assessable course node parameters
	 * @param name The form name
	 * @param assessableCourseNode The course node
	 * @param assessedIdentityWrapper The wrapped identity
	 * @param trans The package translator
	 */
	AssessmentForm(UserRequest ureq, WindowControl wControl, AssessableCourseNode assessableCourseNode, AssessedIdentityWrapper assessedIdentityWrapper) {
		super(ureq, wControl);
		
		this.hasAttempts = assessableCourseNode.hasAttemptsConfigured();
		this.hasScore = assessableCourseNode.hasScoreConfigured();
		this.hasPassed = assessableCourseNode.hasPassedConfigured();
		this.hasComment = assessableCourseNode.hasCommentConfigured();
		
		this.assessedIdentityWrapper = assessedIdentityWrapper;
		this.assessableCourseNode = assessableCourseNode;

		initForm(ureq);
	}

	boolean isAttemptsDirty() {
		return hasAttempts && attemptsValue.intValue() != attempts.getIntValue();
	}
	
	int getAttempts() {
		return attempts.getIntValue();
	}


	Float getCut() {
		return cut;
	}

	StaticTextElement getCutVal() {
		return cutVal;
	}

	boolean isHasAttempts() {
		return hasAttempts;
	}

	boolean isHasComment() {
		return hasComment;
	}

	boolean isHasPassed() {
		return hasPassed;
	}

	boolean isHasScore() {
		return hasScore;
	}

	SingleSelection getPassed() {
		return passed;
	}

	boolean isScoreDirty() {
		if (!hasScore) return false;
		if (scoreValue == null) return !score.getValue().equals("");
		return parseFloat(score) != scoreValue.floatValue();
	}
	
	Float getScore() {
		return parseFloat(score);
	}

	boolean isUserCommentDirty () {
		return hasComment && !userComment.getValue().equals(userCommentValue);
	}
	TextElement getUserComment() {
		return userComment;
	}
	
	boolean isCoachCommentDirty () {
		return !coachComment.getValue().equals(coachCommentValue);
	}
	
	TextElement getCoachComment() {
		return coachComment;
	}

	@Override
	protected void formOK(UserRequest ureq) {
			fireEvent(ureq, Event.DONE_EVENT);
	}
	
	protected void formCancelled (UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected boolean validateFormLogic (UserRequest ureq) {
		if (hasScore) {
		//fxdiff VCRP-4: assessment overview with max score
			try {
				if(parseFloat(score) == null) {
					score.setErrorKey("form.error.wrongFloat", null);
					return false;
				}
			} catch (NumberFormatException e) {
				score.setErrorKey("form.error.wrongFloat", null);
				return false;
			}
			
			Float fscore = parseFloat(score);
			if ((min != null && fscore < min.floatValue()) 
					|| fscore < AssessmentHelper.MIN_SCORE_SUPPORTED) {
				score.setErrorKey("form.error.scoreOutOfRange", null);
				return false;
			}
			if ((max != null && fscore > max.floatValue())
					|| fscore > AssessmentHelper.MAX_SCORE_SUPPORTED) {
				score.setErrorKey("form.error.scoreOutOfRange", null);
				return false;
			}
		}	
		return true;
	}
	
	//fxdiff VCRP-4: assessment overview with max score
	private Float parseFloat(TextElement textEl) throws NumberFormatException {
		String scoreStr = textEl.getValue();
		if(!StringHelper.containsNonWhitespace(scoreStr)) {
			return null;
		}
		int index = scoreStr.indexOf(',');
		if(index >= 0) {
			scoreStr = scoreStr.replace(',', '.');
			return Float.parseFloat(scoreStr);
		}
		return Float.parseFloat(scoreStr);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("form.title", null);
		
		UserCourseEnvironment userCourseEnv = assessedIdentityWrapper.getUserCourseEnvironment();
		ScoreEvaluation scoreEval = userCourseEnv.getScoreAccounting().evalCourseNode(assessableCourseNode);
		if (scoreEval == null) scoreEval = new ScoreEvaluation(null, null);

		if (hasAttempts) {
			attemptsValue = assessableCourseNode.getUserAttempts(userCourseEnv);
			attempts = uifactory.addIntegerElement("attempts", "form.attempts", (attemptsValue == null ? 0 : attemptsValue.intValue()), formLayout);
			attempts.setDisplaySize(3);
			attempts.setMinValueCheck(0, null);
		}

		if (hasScore) {
			min = assessableCourseNode.getMinScoreConfiguration();
			max = assessableCourseNode.getMaxScoreConfiguration();
			if (hasPassed) {
				cut = assessableCourseNode.getCutValueConfiguration();
			}
			
			//fxdiff VCRP-4: assessment overview with max score
			String minStr = AssessmentHelper.getRoundedScore(min);
			String maxStr = AssessmentHelper.getRoundedScore(max);
			uifactory.addStaticTextElement("minval", "form.min", ((min == null) ? translate("form.valueUndefined") : minStr), formLayout);
			uifactory.addStaticTextElement("maxval", "form.max", ((max == null) ? translate("form.valueUndefined") : maxStr), formLayout);

			// Use init variables from wrapper, already loaded from db
			scoreValue = scoreEval.getScore();
			score = uifactory.addTextElement("score","form.score" , 10, "", formLayout);
			score.setDisplaySize(4);
			score.setExampleKey("form.score.rounded", null);
			if (scoreValue != null) {
				score.setValue(AssessmentHelper.getRoundedScore(scoreValue));
			} 
			//fxdiff VCRP-4: assessment overview with max score
			score.setRegexMatchCheck("(\\d+)||(\\d+\\.\\d{1,3})||(\\d+\\,\\d{1,3})", "form.error.wrongFloat");
		}

		if (hasPassed) {
			if (cut != null) {
				// Display cut value if defined
				cutVal = uifactory.addStaticTextElement(
						"cutval","form.cut" ,
						((cut == null) ? translate("form.valueUndefined") : AssessmentHelper.getRoundedScore(cut)),
						formLayout
				);
			}
			
			String[] trueFalseKeys = new String[] { "undefined", "true", "false" };
			String[] passedNotPassedValues = new String[] {
					translate("form.passed.undefined"),
					translate("form.passed.true"),
					translate("form.passed.false")
			};

			//passed = new StaticSingleSelectionElement("form.passed", trueFalseKeys, passedNotPassedValues);
			passed = uifactory.addRadiosVertical("passed", "form.passed", formLayout, trueFalseKeys, passedNotPassedValues);	
			
			Boolean passedValue = scoreEval.getPassed();
			passed.select(passedValue == null ? "undefined" :passedValue.toString(), true);
			// When cut value is defined, no manual passed possible
			passed.setEnabled(cut == null);
		}

		if (hasComment) {
			// Use init variables from db, not available from wrapper
			userCommentValue = assessableCourseNode.getUserUserComment(userCourseEnv);
			if (userCommentValue == null) {
				userCommentValue = "";
			}
			userComment = uifactory.addTextAreaElement("usercomment", "form.usercomment", 2500, 5, 40, true, userCommentValue, formLayout);
		}

		coachCommentValue = assessableCourseNode.getUserCoachComment(userCourseEnv);
		if (coachCommentValue == null) {
			coachCommentValue = "";
		}
		coachComment = uifactory.addTextAreaElement("coachcomment", "form.coachcomment", 2500, 5, 40, true, coachCommentValue, formLayout);
	
		//why does the TextElement not use its default error key??? 
		//userComment could be null for course elements of type Assessment (MSCourseNode)
		if(userComment!=null) {
		  userComment.setNotLongerThanCheck(2500, "input.toolong");
		}
		if(coachComment!=null) {
		  coachComment.setNotLongerThanCheck(2500, "input.toolong");
		}
		
		FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonGroupLayout", getTranslator());
		formLayout.add(buttonGroupLayout);
		uifactory.addFormSubmitButton("save", buttonGroupLayout);
		uifactory.addFormCancelButton("cancel", buttonGroupLayout, ureq, getWindowControl());
	}

	@Override
	protected void doDispose() {
		//
	}
}
