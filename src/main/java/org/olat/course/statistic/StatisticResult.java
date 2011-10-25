package org.olat.course.statistic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.olat.core.gui.components.table.TableDataModel;
import org.olat.core.util.nodes.INode;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.nodes.CourseNode;

/** work in progress **/
public class StatisticResult implements TableDataModel {

	/** token representing the title cell in the total row - renderers must know how to render this **/
	static final Object TOTAL_ROW_TITLE_CELL = new Object();
	
	public static final String KEY_NODE = "result_key_node";
	
	private List<String> columnHeaders_ = new LinkedList<String>();
	
	private List<CourseNode> orderedNodesList_ = new LinkedList<CourseNode>();

	private Map<CourseNode,Map<String,Integer>> statistic_ = new HashMap<CourseNode, Map<String,Integer>>();
	
	/**
	 * mysql> select businesspath,day,value from o_stat_dayofweek where businesspath like '[RepositoryEntry:393216]%';
+-----------------------------------------------------+-----+-------+
| businesspath                                        | day | value |
+-----------------------------------------------------+-----+-------+
| [RepositoryEntry:393216][CourseNode:73156787421533] |   2 |     4 |
| [RepositoryEntry:393216][CourseNode:73156787421533] |   3 |    33 |
| [RepositoryEntry:393216][CourseNode:73156787421533] |   4 |    34 |
	 */
	public StatisticResult(ICourse course, List<?> result) {
		final Set<String> groupByKeys = new HashSet<String>();
		doAddQueryListResultsForNodeAndChildren(course.getRunStructure().getRootNode(), result, groupByKeys);
		if (result.size()!=0) {
			System.out.println("ERROR - should have 0 left....: "+result.size());
		}
		
		columnHeaders_ = new LinkedList<String>(groupByKeys);
		Collections.sort(columnHeaders_, new Comparator<String>() {

			@Override 
			public int compare(String o1, String o2){
				try{
					Integer n1 = Integer.parseInt(o1);
					Integer n2 = Integer.parseInt(o2);
					if (n1>n2) {
						return 1;
					} else if (n1<n2) {
						return -1;
					} else {
						return 0;
					}
				} catch(NumberFormatException nfe) {
					return o1.compareTo(o2);
				}
				
			}
			
		});
	}
	
	public List<String> getColumnHeaders() {
		return new ArrayList<String>(columnHeaders_);
	}
	
	public void setColumnHeaders(List<String> columnHeaders) {
		columnHeaders_ = new ArrayList<String>(columnHeaders);
	}
	
	private void doAddQueryListResultsForNodeAndChildren(CourseNode node, List<?> result, Set<String> groupByKeys) {
		orderedNodesList_.add(node);
		
		for (Iterator<?> it = result.iterator(); it.hasNext();) {
			Object[] columns = (Object[]) it.next();
			if (columns.length!=3) {
				throw new IllegalStateException("result should be three columns wide");
			}
			
			String businessPath = (String)columns[0];
			if (!businessPath.matches("\\[RepositoryEntry:.*\\]\\[CourseNode:"+node.getIdent()+"\\]")) {
				continue;
			}
			
			String groupByKey = String.valueOf(columns[1]);
			groupByKeys.add(groupByKey);
			int count = (Integer)columns[2];
			
			Map<String,Integer> nodeMap = statistic_.get(node);
			if (nodeMap==null) {
				nodeMap = new HashMap<String,Integer>();
				statistic_.put(node, nodeMap);
			}
			
			Integer existingCount = nodeMap.get(groupByKey);
			
			if (existingCount==null) {
				nodeMap.put(groupByKey, count);
			} else {
				nodeMap.put(groupByKey, existingCount + count);
			}
			
			it.remove();
		}
		
		int childCount = node.getChildCount();
		for(int i = 0; i < childCount; i++) {
			INode n = node.getChildAt(i);
			if(n instanceof CourseNode) {
				doAddQueryListResultsForNodeAndChildren((CourseNode)n, result, groupByKeys);
			}
		}
	}
	
	private Map<String, Object> getIndentednodeRendererMap(int row) {
		if (row>=orderedNodesList_.size()) {
			throw new IllegalStateException("row count too big: "+row+", only having "+orderedNodesList_.size()+" elements");
		}
		CourseNode node = orderedNodesList_.get(row);
		int recursionLevel = 0;
		INode parent = node.getParent();
		while(parent!=null) {
			recursionLevel++;
			parent = parent.getParent();
		}
		
		// Store node data in hash map. This hash map serves as data model for 
		// the user assessment overview table. Leave user data empty since not used in
		// this table. (use only node data)
		Map<String,Object> nodeData = new HashMap<String, Object>();
		// indent
		nodeData.put(AssessmentHelper.KEY_INDENT, new Integer(recursionLevel));
		// course node data
		nodeData.put(AssessmentHelper.KEY_TYPE, node.getType());
		nodeData.put(AssessmentHelper.KEY_TITLE_SHORT, node.getShortTitle());
		nodeData.put(AssessmentHelper.KEY_TITLE_LONG, node.getLongTitle());
		nodeData.put(AssessmentHelper.KEY_IDENTIFYER, node.getIdent());
		// plus the node
		nodeData.put(StatisticResult.KEY_NODE, node);
			
		return nodeData;
	}

	public List<String> getHeaders() {
		return new ArrayList<String>(columnHeaders_);
	}

	@Override
	public Object createCopyWithEmptyList() {
		return null;
	}
	
	@Override
	public int getColumnCount() {
		return columnHeaders_.size()+1/*+1 because first column is not in the columnheaders*/+1/*+1 because we add the total*/;
	}
	

	@Override
	public Object getObject(int row) {
		// nothing returned here
		return null;
	}

	@Override
	public int getRowCount() {
		return orderedNodesList_.size()+1/*+1 because we add the total */;
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (row-1>=orderedNodesList_.size()) {
			return null;
		}
		if (row==orderedNodesList_.size()) {
			// that's the "total" row
			if (col==0) {
				return TOTAL_ROW_TITLE_CELL;
			}
			
			int total = 0;
			if (col-1==columnHeaders_.size()) {
				for (Iterator<Map<String,Integer>> it = statistic_.values().iterator(); it.hasNext();) {
					Map<String,Integer> statisticMap = it.next();
					if (statisticMap==null) {
						continue;
					}
					for (Iterator it2 = statisticMap.values().iterator(); it2.hasNext();) {
						Integer num = (Integer) it2.next();
						if (num!=null) {
							total+=num;
						}
					}
				}
				return total;
			}
			String groupByKey = columnHeaders_.get(col-1);
			for (Iterator<Map<String,Integer>> it = statistic_.values().iterator(); it.hasNext();) {
				Map<String,Integer> statisticMap = it.next();
				if (statisticMap!=null) {
					Integer num = statisticMap.get(groupByKey);
					if (num!=null) {
						total+=num;
					}
				}
			}
			
			return total;
		}
		if (col==0) {
			return getIndentednodeRendererMap(row);
		}
		
		CourseNode node = orderedNodesList_.get(row);
		Map<String, Integer> statisticMap = statistic_.get(node);
		if (col-1>=columnHeaders_.size()) {
			// that's the total
			int total = 0;
			if (statisticMap!=null) {
				for (Iterator<Integer> it = statisticMap.values().iterator(); it.hasNext();) {
					Integer cnt = it.next();
					total+=cnt;
				}
			}
			return total;
		}
		if (statisticMap==null) {
			return null;
		}
		String headerKey = columnHeaders_.get(col-1);
		if (headerKey==null) {
			return null;
		}
		return statisticMap.get(headerKey);
	}

	@Override
	public void setObjects(List objects) {
		// nothing done here
	}
	
}