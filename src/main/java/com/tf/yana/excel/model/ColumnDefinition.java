package com.tf.yana.excel.model;

import java.util.ArrayList;
import java.util.Map;

public class ColumnDefinition {
	
	private String headingStr;
	private Map<Integer, String> headingIndexMap;
	private ArrayList<Integer> skipColumns;
	private int columnCount;
	
	public String getHeadingStr() {
		return headingStr;
	}
	public void setHeadingStr(String headingStr) {
		this.headingStr = headingStr;
	}
	public ArrayList<Integer> getSkipColumns() {
		return skipColumns;
	}
	public void setSkipColumns(ArrayList<Integer> skipColumns) {
		this.skipColumns = skipColumns;
	}
	public int getColumnCount() {
		return columnCount;
	}
	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}
	public Map<Integer, String> getHeadingIndexMap() {
		return headingIndexMap;
	}
	public void setHeadingIndexMap(Map<Integer, String> headingIndexMap) {
		this.headingIndexMap = headingIndexMap;
	}
}
