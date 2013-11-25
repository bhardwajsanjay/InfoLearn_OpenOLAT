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
package org.olat.course.assessment.bulk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.olat.core.commons.modules.bc.vfs.OlatRootFileImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalImpl;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.course.assessment.model.BulkAssessmentDatas;
import org.olat.course.assessment.model.BulkAssessmentRow;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.TACourseNode;

/**
 * 
 * Initial date: 18.11.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class DataStepForm extends StepFormBasicController {

	private static final String[] keys = new String[] {"tab","comma"};
	
	private TextElement dataEl;
	private FileElement returnFileEl;
	private SingleSelection delimiter;

	private BulkAssessmentDatas datas;
	private final AssessableCourseNode courseNode;
	private OlatRootFolderImpl bulkAssessmentTmpDir;
	
	public DataStepForm(UserRequest ureq, WindowControl wControl, StepsRunContext runContext, Form rootForm) {
		super(ureq, wControl, rootForm, runContext, LAYOUT_DEFAULT, null);
		
		courseNode = (AssessableCourseNode)getFromRunContext("courseNode");
		
		initForm(ureq);
	}
	
	public DataStepForm(UserRequest ureq, WindowControl wControl, AssessableCourseNode courseNode, BulkAssessmentDatas datas, 
			StepsRunContext runContext, Form rootForm) {
		super(ureq, wControl, rootForm, runContext, LAYOUT_DEFAULT, null);
		
		this.datas = datas;
		this.courseNode = courseNode;
		addToRunContext("courseNode", courseNode);
		initForm(ureq);
	}
	
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("data.title");
		setFormDescription("data.description");
		
		String dataVal = "";
		if(datas != null && StringHelper.containsNonWhitespace(datas.getDataBackupFile())) {
			OlatRootFileImpl file = new OlatRootFileImpl(datas.getDataBackupFile(), null);
			InputStream in = file.getInputStream();
			try {
				dataVal = IOUtils.toString(in);
			} catch (IOException e) {
				logError("", e);
			} finally {
				IOUtils.closeQuietly(in);
			}	
		}
		
		dataEl = uifactory.addTextAreaElement("data", "data", -1, 6, 60, true, dataVal, formLayout);

		String[] values = new String[] {translate("form.step3.delimiter.tab"),translate("form.step3.delimiter.comma")};
		delimiter = uifactory.addRadiosVertical("delimiter", "form.step3.delimiter", formLayout, keys, values);
		delimiter.select("tab", true);
		
		if(courseNode instanceof TACourseNode) {
			//return files
			returnFileEl = uifactory.addFileElement("returnfiles", "return.files", formLayout);
			if(datas != null && StringHelper.containsNonWhitespace(datas.getReturnFiles())) {
				OlatRootFileImpl returnFiles = new OlatRootFileImpl(datas.getReturnFiles(), null);
				if(returnFiles.exists()) {
					returnFileEl.setInitialFile(returnFiles.getBasefile());
				}
			}
		}
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		String val = dataEl.getValue();
		BulkAssessmentDatas datas = new BulkAssessmentDatas();
		
		if(bulkAssessmentTmpDir == null) {
			OlatRootFolderImpl bulkAssessmentDir = new OlatRootFolderImpl("/bulkassessment/", null);
			bulkAssessmentTmpDir = bulkAssessmentDir.createChildContainer(UUID.randomUUID().toString());
		}

		backupInputDatas(val, datas, bulkAssessmentTmpDir);
		List<BulkAssessmentRow> rows = processInputData(val);
		if(returnFileEl != null) {
			processReturnFiles(datas, rows, bulkAssessmentTmpDir);
		}
		datas.setRows(rows);
		addToRunContext("datas", datas);
		fireEvent(ureq, StepsEvent.ACTIVATE_NEXT);
	}
	
	/**
	 * Backup the input field for later editing purpose
	 * @param val
	 * @param datas
	 */
	private void backupInputDatas(String val, BulkAssessmentDatas datas, OlatRootFolderImpl tmpDir) {
		String inputFilename = UUID.randomUUID().toString() + ".csv";
		OlatRootFileImpl inputFile = tmpDir.createChildLeaf(inputFilename);
		OutputStream out = inputFile.getOutputStream(false);
		
		try {
			IOUtils.write(val, out);
			datas.setDataBackupFile(inputFile.getRelPath());
		} catch (IOException e) {
			logError("", e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}
	
	private void processReturnFiles(BulkAssessmentDatas datas, List<BulkAssessmentRow> rows, OlatRootFolderImpl tmpDir) {
		File uploadedFile = returnFileEl.getUploadFile();
		if(uploadedFile == null) {
			File initialFile = returnFileEl.getInitialFile();
			if(initialFile != null && initialFile.exists()) {
				VFSLeaf target = new LocalFileImpl(initialFile);
				processReturnFiles(target, rows);
			}
		} else if(uploadedFile.exists()) {
			//transfer to secured
			try {
				String uploadedFilename = returnFileEl.getUploadFileName();
				if(!StringHelper.containsNonWhitespace(uploadedFilename)) {
					uploadedFilename = "bulkAssessment.zip";
				}

				VFSItem currentTarget = tmpDir.resolve(uploadedFilename);
				if(!isSame(currentTarget, uploadedFile)) {
					if(currentTarget != null && currentTarget.exists()) {
						currentTarget.deleteSilently();
					}
					
					OlatRootFileImpl target = tmpDir.createChildLeaf(uploadedFilename);
					FileInputStream inStream = new FileInputStream(uploadedFile);
					if(VFSManager.copyContent(inStream, target)) {
						datas.setReturnFiles(target.getRelPath());
						processReturnFiles(target, rows);
					}
				}
			} catch (FileNotFoundException e) {
				logError("", e);
			}
		}
	}
	
	private boolean isSame(VFSItem currentTarget, File uploadedFile) {
		if(currentTarget instanceof LocalImpl) {
			LocalImpl local = (LocalImpl)currentTarget;
			File currentFile = local.getBasefile();
			if(currentFile.length() == uploadedFile.length()) {
				try {
					return org.apache.commons.io.FileUtils.contentEquals(currentFile, uploadedFile);
				} catch (IOException e) {
					logError("", e);
					//do nothing -> return false at the end
				}
			}
		}
		return false;
	}
	
	private void processReturnFiles(VFSLeaf target, List<BulkAssessmentRow> rows) {
		Map<String, BulkAssessmentRow> assessedIdToRow = new HashMap<>();
		for(BulkAssessmentRow row:rows) {
			assessedIdToRow.put(row.getAssessedId(), row);
		}
		
		if(target.exists()) {
			InputStream is = target.getInputStream();
			File parentTarget = ((LocalImpl)target).getBasefile().getParentFile();
			ZipInputStream zis = new ZipInputStream(is);
			
			ZipEntry entry;
			try {
				byte[] b = new byte[FileUtils.BSIZE];
				while ((entry = zis.getNextEntry()) != null) {
					if(!entry.isDirectory()) {
						while (zis.read(b) > 0) {
							//continue
						}
						
						Path op = new File(parentTarget, entry.getName()).toPath();
						if(!Files.isHidden(op) && !Files.isDirectory(op)) {
							Path parentDir = op.getParent();
							String assessedId = parentDir.getFileName().toString();
							String filename = op.getFileName().toString();
							
							BulkAssessmentRow row;
							if(assessedIdToRow.containsKey(assessedId)) {
								row = assessedIdToRow.get(assessedId);
							} else {
								row = new BulkAssessmentRow();
								assessedIdToRow.put(assessedId, row);
								rows.add(row);
							}
							
							if(row.getReturnFiles() == null) {
								row.setReturnFiles(new ArrayList<String>(2));
							}
							row.getReturnFiles().add(filename);
						}
					}
				}
			} catch(Exception e) {
				logError("", e);
			} finally {
				IOUtils.closeQuietly(is);
				IOUtils.closeQuietly(zis);
			}
		}
	}
	
	private List<BulkAssessmentRow> processInputData(String idata) {
		String[] lines = idata.split("\r?\n");
		int numOfLines = lines.length;
		
		List<BulkAssessmentRow> rows = new ArrayList<BulkAssessmentRow>(numOfLines);
		
		String d;
		if (delimiter.getSelectedKey().startsWith("t")) {
			d = "\t";
		} else {
			d = ",";
		}
		
		for (int i = 0; i < numOfLines; i++) {
			String line = lines[i];

			if(StringHelper.containsNonWhitespace(line)){
				String[] values = line.split(d,-1);
				BulkAssessmentRow row = createRow(values);
				if(row != null) {
					rows.add(row);
				}
			}
		}
		return rows;
	}
	
	/**
	 * Create a row object from an array of strings. The array
	 * is assessed identity identifier, score, status, comment.
	 * @param values
	 * @return
	 */
	private BulkAssessmentRow createRow(String[] values) {
		int valuesLength = values.length;
		if(valuesLength <= 0) {
			return null;
		}

		BulkAssessmentRow row = new BulkAssessmentRow();
		row.setAssessedId(values[0]);

		if(valuesLength > 1) {
			String scoreStr = values[1];
			Float score;
			if("".equals(scoreStr)) {
				score = null;
			} else if("\"\"".equals(scoreStr) || "''".equals(scoreStr)) {
				score = new Float(0.0f);
			} else if (StringHelper.containsNonWhitespace(scoreStr)) {
				try {
					score = Float.parseFloat(scoreStr);
				} catch (NumberFormatException e) {
					score = null;
				}
			} else {
				score = null;
			}
			row.setScore(score);
		}

		if(valuesLength > 2) {
			String passedStr = values[2];
			Boolean passed;
			if("\"\"".equals(passedStr) || "''".equals(passedStr)) {
				passed = Boolean.FALSE;
			} else if("y".equalsIgnoreCase(passedStr) || "yes".equalsIgnoreCase(passedStr)
					|| "true".equalsIgnoreCase(passedStr) || "1".equalsIgnoreCase(passedStr)) {
				passed = Boolean.TRUE;
			} else if("n".equalsIgnoreCase(passedStr) || "no".equalsIgnoreCase(passedStr)
					|| "false".equalsIgnoreCase(passedStr) || "0".equalsIgnoreCase(passedStr)) {
				passed = Boolean.FALSE;
			} else {
				passed = null;
			}
			row.setPassed(passed);
		}
		
		if(valuesLength > 3) {
			String commentStr = values[3];
			if(commentStr.isEmpty()) {
				row.setComment(null);
			} else if("\"\"".equals(commentStr) || "''".equals(commentStr)) {
				row.setComment("");
			} else {
				row.setComment(commentStr);
			}
		}
		
		return row;
	}
}