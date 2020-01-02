package com.tf.yana.excel.model;

import java.util.ArrayList;

public class ExcelHeadingAndColumn {

	private ArrayList<String> valuesList;
	private String headingStr;
	
	public ArrayList<String> getValuesList() {
		return valuesList;
	}
	public void setValuesList(ArrayList<String> valuesList) {
		this.valuesList = valuesList;
	}
	public String getHeadingStr() {
		return headingStr;
	}
	public void setHeadingStr(String headingStr) {
		this.headingStr = headingStr;
	}
}
