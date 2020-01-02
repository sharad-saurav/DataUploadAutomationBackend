package com.tf.yana.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.tf.yana.excel.model.ColumnDefinition;
import com.tf.yana.excel.model.ExcelHeadingAndColumn;
import com.tf.yana.excel.model.MessageFormatTemplate;
import com.tf.yana.exception.DataUploadException;

public class ExcelUtil {

	public static ExcelHeadingAndColumn getHeadingAndValuesFromExcel(String fileName,
			ArrayList<String> columnNamesToSkip, Map<String, MessageFormatTemplate> columnsToFormat)
			throws DataUploadException {
		Workbook wb = null;
		ArrayList<String> valuesList = null;
		ExcelHeadingAndColumn headingAndValueList = new ExcelHeadingAndColumn();
		try {
			wb = new XSSFWorkbook(Files.newInputStream(Paths.get(fileName), StandardOpenOption.READ));

			Sheet sheet = wb.getSheetAt(0);
			valuesList = new ArrayList<String>();

			String headingStr = null;
			int rowCount = 0;
			ColumnDefinition columnDef = null;
			for (Row row : sheet) {
				if (rowCount == 0) {
					columnDef = getRowHeadings(row, columnNamesToSkip, columnsToFormat);
					// headingStr = getRowValues(row, false);
					headingStr = columnDef.getHeadingStr();
					System.out.println("Heading values::" + headingStr);

					if (headingStr == null || headingStr.isEmpty()) {
						System.out.println("no column names found in file " + fileName + ", program is stopping here");
						System.exit(0);
					}
				} else {
					boolean isRowEmpty = checkIfRowIsEmpty(row);
					if (isRowEmpty) {
						System.out.println("Row is empty to not reading any more rows from this file");
						continue;
					}
					String rowValueList = getRowValues(row, columnDef.getSkipColumns(), columnDef.getHeadingIndexMap(),
							columnsToFormat, columnDef.getColumnCount());
					System.out.println("row value list::" + rowValueList);

					if (rowValueList != null && !rowValueList.isEmpty())
						valuesList.add(rowValueList);
				}

				rowCount++;
			}

			headingAndValueList.setHeadingStr(headingStr);
			headingAndValueList.setValuesList(valuesList);
		} catch (Exception Ex) {
			Ex.printStackTrace();
			throw new DataUploadException("Get Heading And Values From Excel Failed");
		} finally {
			if (wb != null)
				try {
					wb.close();
				} catch (IOException e) {
					// Ignore this exception
				}
		}

		return headingAndValueList;
	}

	public static String getRowValues(Row row, ArrayList<Integer> columnsToSkip, Map<Integer, String> headingIndexMap,
			Map<String, MessageFormatTemplate> columnsToFormat, int columnCount) throws DataUploadException {
		try {
			StringBuilder textBuilder = new StringBuilder();
			boolean firstCell = true;

			Set<String> cellValueList = new HashSet<String>();

			String languagesColumnValue = "";

			String langJsonFormat = "{\"EN\": {\"TEXT\":\"<TEXT>\", \"VOICE\":\"<VOICE>\", \"VOICE_ONLY\":\"<VOICE_ONLY>\"}}";

			for (int i = 0; i < row.getLastCellNum(); i++) {

				if (i >= (columnCount + columnsToSkip.size()))
					break;
				if (columnsToSkip.contains(i)) {
					continue;
				} else {
					Cell cell = row.getCell(i, MissingCellPolicy.RETURN_BLANK_AS_NULL);

					if (cell != null && cell.getCellTypeEnum() == CellType.STRING) {

						String configLanguage = ExcelUtil.readConfig("LANGUAGE", 0);
						if (configLanguage.equalsIgnoreCase("EN")) {
							getFilteredStringCellValue(cell);
						}
					}

					if (!firstCell)
						textBuilder.append("|");

					String cellValue = "";

					if (headingIndexMap.containsKey(i)) {
						String heading = headingIndexMap.get(i);
						if (columnsToFormat.containsKey(heading)) {
							if (cell != null && StringUtils.isBlank(cell.getStringCellValue())) {
								cell = null;
							}
							cellValue = (cell != null ? cell.getStringCellValue() : null);
							cellValue = formatCellValue(cellValue, heading, columnsToFormat.get(heading));

							/*
							 * if(cellValue.equalsIgnoreCase("[]")) { cellValue = null; }
							 */
						} else {
							if (cell == null) {
								textBuilder.append(cell);
								firstCell = false;
								continue;
							}
							if (cell != null && cell.getCellTypeEnum() == CellType.STRING) {
								cellValue = cell.getStringCellValue();
								if (!headingIndexMap.get(i).equalsIgnoreCase("LANGUAGES") && cellValue.contains("\"")) {
									System.out.println("text conatins double quote");
									cellValue = cellValue.replaceAll("\"", "\\\\\\\\\\\\\"");
									System.out.println("after replacement::" + cellValue);
								}

							} else if (cell != null && cell.getCellTypeEnum() == CellType.NUMERIC)
								cellValue = Double.toString(cell.getNumericCellValue());
							else if (cell.getCellTypeEnum() == CellType.FORMULA) {
								cellValue = cell.getRichStringCellValue().getString();
							}

							if (StringUtils.isBlank(cellValue)) {
								cellValue = null;
								textBuilder.append(cellValue);
								continue;
							}

							System.out.println("Cell Value::" + cellValue);

							if (cellValue.contains("'")) {
								System.out.println("text conatins single quote");
								cellValue = cellValue.replaceAll("'", "\\\\'");
								System.out.println("after replacement::" + cellValue);
							}

							if (cellValue.contains("\n")) {
								System.out.println("text conatins single quotenew line character");
								cellValue = cellValue.replaceAll("\n", "\\\\n");
								System.out.println("after replacement::" + cellValue);
							}

							if (headingIndexMap.get(i).equalsIgnoreCase("LANGUAGES")) {
								languagesColumnValue = cellValue;
							}

							if (headingIndexMap.get(i).equalsIgnoreCase("TEXT")
									|| headingIndexMap.get(i).equalsIgnoreCase("VOICE")
									|| headingIndexMap.get(i).equalsIgnoreCase("VOICE_ONLY")) {
								System.out.println("Cell value before adding to JSON format::" + cellValue);
								langJsonFormat = langJsonFormat.replace("<" + headingIndexMap.get(i) + ">", cellValue);
								System.out.println("language JSON format after change::" + langJsonFormat);
							}
							if (headingIndexMap.get(i).equalsIgnoreCase("LANGUAGES") && StringUtils.isEmpty(cellValue)
									&& !langJsonFormat.contains("<TEXT>")) {
								System.out.println("Language JSON format::" + langJsonFormat);
								cellValue = langJsonFormat;
								System.out.println("languages cell value::" + cellValue);
							}
						}

						// Add only TEXT, VOICE and VOICE_ONLY columns which has double quotes in it.
						// This map is used later on to replace LAGUAGES Column value with special
						// character of double quotes
						// We can not replace all the double quotes in LANGUAGES so we take this route.
						if (cellValue.contains("\"") && (heading.endsWith("TEXT") || heading.endsWith("VOICE")
								|| heading.endsWith("VOICE_ONLY"))) {
							cellValueList.add(cellValue.trim());
						}

						if (!headingIndexMap.get(i).equalsIgnoreCase("LANGUAGES")) {
							textBuilder.append("'").append(cellValue.trim()).append("'");
						} else {
							String langReplaceStr = "";

							if (StringUtils.isNotEmpty(languagesColumnValue)) {
								for (Iterator<String> iterator = cellValueList.iterator(); iterator.hasNext();) {
									String listCellValue = iterator.next();

									if (cellValue.contains("\"")) {
										String replacedStr = listCellValue.replaceAll("\\\\\\\\\\\\\"", "\"");
										langReplaceStr = languagesColumnValue.replace(replacedStr, listCellValue);
									}
								}
							}

							if (StringUtils.isNotEmpty(langReplaceStr)) {
								textBuilder.append("'").append(langReplaceStr).append("'");
							} else {
								textBuilder.append("'").append(languagesColumnValue).append("'");
							}
						}

						firstCell = false;
					}
				}

			}
			return textBuilder.toString();
		} catch (DataUploadException err) {
			throw err;
		}
		
	}

	private static String formatCellValue(String stringCellValue, String heading,
			MessageFormatTemplate msgFormatTemplate) {
		String formattedString = "";
		if (heading.equalsIgnoreCase("MEDIA")) {
			String mediaReturnString = "[";
			if (StringUtils.isNotEmpty(stringCellValue) && stringCellValue.startsWith("{")
					&& stringCellValue.endsWith("}")) {
				stringCellValue = stringCellValue.replaceFirst("\\{", "");
				stringCellValue = stringCellValue.substring(0, stringCellValue.length() - 1);
				String mediaValues[] = stringCellValue.split("\\},\\{");
				for (int i = 0; i < mediaValues.length; i++) {
					String mediaValue = mediaValues[i];
					if (i == 0)
						formattedString += getIndividualMedaiJSON(mediaValue, msgFormatTemplate);
					else
						formattedString += "," + getIndividualMedaiJSON(mediaValue, msgFormatTemplate);
				}
			} else if (StringUtils.isNotEmpty(stringCellValue)) {
				formattedString += getIndividualMedaiJSON(stringCellValue, msgFormatTemplate);
			}

			mediaReturnString += formattedString + "]";

			/*
			 * if(mediaReturnString.equalsIgnoreCase("[]")) { mediaReturnString = ""; }
			 */

			System.out.println("Value of mediaReturnString::" + mediaReturnString);

			return mediaReturnString;
		} else {
			return stringCellValue;
		}
	}

	/**
	 * This function is used to read two column row values. we have limited column
	 * length to 2. first coulmn would be property name and second column is
	 * property value
	 * 
	 * @param fileName    xlsx file name to read from.
	 * @param sheetNumber sheet number from where we need to load the configuration
	 * @return
	 * @throws DataUploadException 
	 */
	public static Map<String, String> getTwoColumnRowDataAsMap(String fileName, int sheetNumber) throws DataUploadException {
		Workbook wb = null;
		Map<String, String> twoColRowData = new HashMap<String, String>();
		try {
			wb = new XSSFWorkbook(Files.newInputStream(Paths.get(fileName), StandardOpenOption.READ));
			Sheet sheet = wb.getSheetAt(sheetNumber);

			int rowCount = 0;

			for (Row row : sheet) {

				Cell cell = row.getCell(0, MissingCellPolicy.RETURN_BLANK_AS_NULL);

				if (cell != null) {
					String propertyName = cell.getStringCellValue();

					cell = row.getCell(1, MissingCellPolicy.RETURN_BLANK_AS_NULL);
					if (cell != null) {
						String propertyValue = cell.getStringCellValue();
						twoColRowData.put(propertyName, propertyValue);
						System.out.println("Property name::" + propertyName + ":::: propertyValue::" + propertyValue);
					} else {
						System.out.println("Property value is null");
					}
				} else {
					System.out.println("Property name is null");
				}
				rowCount++;
			}

			System.out.println("Read " + rowCount + " from file:" + fileName + " in sheet::" + sheetNumber);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new DataUploadException("Get Two Column Row Data As Map Failed");
		}

		return twoColRowData;
	}

	public static String getIndividualMedaiJSON(String mediaValue, MessageFormatTemplate msgFormatTemplate) {
		Object objValues[] = new Object[msgFormatTemplate.getVariableCount()];
		String values[] = mediaValue.split("\\|");
		int templVariableCount = msgFormatTemplate.getVariableCount();

		if (values.length < templVariableCount) {
			for (int i = 0; i < values.length; i++) {
				objValues[i] = values[i];
			}
			for (int i = values.length; i < templVariableCount; i++) {
				objValues[i] = "";
			}
		} else {
			objValues = values;
		}

		String msgFormat = msgFormatTemplate.getMessageFormat();
		System.out.println("Message format::" + msgFormat);
		System.out.println("objValues::" + objValues);
		String formattedMsg = "{" + MessageFormat.format(msgFormat, objValues) + "}";
		return formattedMsg;
	}

	public static ColumnDefinition getRowHeadings(Row row, ArrayList<String> columnNamesToSkip,
			Map<String, MessageFormatTemplate> columnsToFormat) {
		boolean firstCell = true;
		StringBuilder textBuilder = new StringBuilder();
		ArrayList<Integer> columnsToSkip = new ArrayList<Integer>();
		Map<Integer, String> headingsToIndexMap = new HashMap<>();
		ColumnDefinition columnDef = new ColumnDefinition();

		int columnCount = 0;

		for (int i = 0; i < row.getLastCellNum(); i++) {

			Cell cell = row.getCell(i, MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if (cell == null) {
				break;
			}
			headingsToIndexMap.put(i, cell.getStringCellValue());

			if (columnNamesToSkip.contains(cell.getStringCellValue())) {
				columnsToSkip.add(i);
				continue;
			}

			if (!firstCell)
				textBuilder.append(",");

			textBuilder.append(cell.getStringCellValue().trim());

			firstCell = false;
			columnCount++;

		}
		columnDef.setHeadingStr(textBuilder.toString());
		columnDef.setHeadingIndexMap(headingsToIndexMap);
		columnDef.setSkipColumns(columnsToSkip);
		columnDef.setColumnCount(columnCount);
		return columnDef;
	}

	public static boolean checkIfRowIsEmpty(Row row) {
		if (row == null) {
			return true;
		}
		if (row.getLastCellNum() <= 0) {
			return true;
		}
		for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
			Cell cell = row.getCell(cellNum);
			if (cell != null && cell.getCellTypeEnum() != CellType.BLANK && StringUtils.isNotBlank(cell.toString())) {
				return false;
			}
		}
		return true;
	}

	public static String readConfig(String configParam, int sheetNum) throws DataUploadException {
		String masterTableQuery = "";
		try {
			// String dataFilePathPrefix = "\\config\\mysql_queries.xlsx";
			// String filePathHome = "D:\\Yana\\Data-Upload-master" + dataFilePathPrefix;
			String filePathHome = System.getenv("DATA_UPLOAD_CONFIG_FILE");
			// String filePathHome = "D:\\Yana\\Data-Upload\\config\\configuration.xlsx";
			if (StringUtils.isEmpty(filePathHome)) {
				throw new DataUploadException("Please set up system variable DATA_UPLOAD_CONFIG_FILE");
			}
			File file = new File(filePathHome);
			FileInputStream fs = new FileInputStream(file);
			XSSFWorkbook wb = new XSSFWorkbook(fs);
			XSSFSheet sh = wb.getSheetAt(sheetNum);
			Iterator<Row> ite = sh.rowIterator();

			while (ite.hasNext()) {
				Row row = ite.next();
				Iterator<Cell> cite = row.cellIterator();
				while (cite.hasNext()) {
					Cell c = cite.next();
					if (c.toString().toUpperCase().equalsIgnoreCase(configParam)) {
						masterTableQuery = cite.next().toString();
						break;
					}
				}
			}
			fs.close();
		} catch (Exception e) {
			e.printStackTrace();
			// } catch (NoSuchElementException e) {
			throw new DataUploadException("read configuration failed");
		}
		if (masterTableQuery.isEmpty()) {
			System.out.println(configParam + " is empty");
		}
		return masterTableQuery;
	}

	public static void getFilteredStringCellValue(Cell cell) {
		String cellValue = "";
		if (cell != null) {
			cellValue = cell.getStringCellValue();
			cellValue = cellValue.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}]", "");
			cellValue = cellValue.replaceAll("[^\\x00-\\x7F]", " ");
			cell.setCellValue(cellValue);
		}
	}

}
