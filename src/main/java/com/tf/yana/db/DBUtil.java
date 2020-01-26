package com.tf.yana.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.tf.yana.excel.ExcelUtil;
import com.tf.yana.excel.model.ExcelHeadingAndColumn;
import com.tf.yana.excel.model.MessageFormatTemplate;
import com.tf.yana.exception.DataUploadException;

public class DBUtil {

	public static final String SUBJECT_AREA = "SUBJECT_AREA";
	public static final String SUBJECT_AREA_INSERT_QUERY = "SUBJECT_AREA_INSERT_QUERY";

	public static int uploadExcelDataToDB(String excelFileName, ArrayList<String> columnNamesToSkip,
			Map<String, MessageFormatTemplate> columnsToFormat, HashSet<String> subjectAreaSet)
			throws DataUploadException {
		try {
			String tableName = FilenameUtils.getBaseName(excelFileName).toUpperCase();
			Connection con = null;
			int numOfRecordsInserted = 0;
			boolean tableExist = false;
			boolean schemaExist = false;
			int recordInserted;
			int resultID = 0;
			String insertQuery;
			int dataUpdateChk = 0;
			boolean createColumn = false;
			boolean deleteColumn = false;
			int sheetNum = 0;

			try {
				String dbUrl = ExcelUtil.readConfig("DBURL", sheetNum);
				String dbUsername = ExcelUtil.readConfig("DBUSERNAME", sheetNum);
				String dbPassword = ExcelUtil.readConfig("DBPASSWORD", sheetNum);
				String schemaName = ExcelUtil.readConfig("DBSCHEMA_NAME", sheetNum);

				Class.forName("com.mysql.jdbc.Driver").newInstance();
				System.out.println("here here here----------------------");
				con = DriverManager.getConnection(
						dbUrl + "?useSSL=false&useUnicode=yes&characterEncoding=UTF-8&characterSetResults=UTF-8",
						dbUsername, dbPassword);
				System.out.println("hit here pls----------------");
				con.setAutoCommit(false);

				schemaExist = schemaExist(con, schemaName);
				if (!schemaExist) {
					String createSchemaQuery = ExcelUtil.readConfig("DBSCHEMA_NAME", sheetNum);
					createSchemaQuery = createSchemaQuery.replaceAll("schemaName", schemaName);
					System.out.println("createSchemaQuery::" + createSchemaQuery);
					Statement stmt = con.createStatement();
					stmt.executeUpdate(createSchemaQuery);
					System.out.println(schemaName + " db is created");
				}

				con = DriverManager.getConnection(dbUrl + schemaName + "?useUnicode=yes&characterEncoding=UTF-8",
						dbUsername, dbPassword);
				con.setAutoCommit(false);

				ExcelHeadingAndColumn headingAndValueList = ExcelUtil.getHeadingAndValuesFromExcel(excelFileName,
						columnNamesToSkip, columnsToFormat);
				ArrayList<String> valuesList = headingAndValueList.getValuesList();
				String headingStr = headingAndValueList.getHeadingStr();
				String[] headingStrArr = headingAndValueList.getHeadingStr().split("\\,");

				System.out.println("number of records to insert::" + valuesList.size());

				Statement stmt = null;

				String dropQuery = "DROP TABLE ";
				tableExist = tableExist(con, tableName);
				if (tableExist) {
					dropQuery += tableName;
					stmt = con.createStatement();
					System.out.println("Drop table query::" + dropQuery);
					try {
						int recordDeleted = stmt.executeUpdate(dropQuery);
						System.out.println(
								"Table  " + tableName + " deleted. Number of records deleted::" + recordDeleted);
					} catch (Exception ex) {
						System.out.println("Could not drop table::" + tableName);
						ex.printStackTrace();
					}
				}

				tableExist = tableExist(con, tableName + "_LANG");
				if (tableExist) {
					if (dataUpdateChk == 0) {
						dropQuery = "DROP TABLE " + tableName + "_LANG";

						if (stmt.isClosed())
							stmt = con.createStatement();

						recordInserted = stmt.executeUpdate(dropQuery);
						System.out.println("Table  " + tableName + " deleted");
						createTable(stmt, tableName + "_LANG", tableName);
					}

					dataUpdateChk = 1;
				}

				String query = ExcelUtil.readConfig(tableName, sheetNum);
				if (query.isEmpty()) {
					throw new DataUploadException(tableName + " table query doesn't exists");
				} else {
					if (stmt == null || stmt.isClosed())
						stmt = con.createStatement();

					System.out.println("Create table query::" + query);

					stmt.executeUpdate(query);

					System.out.println(tableName + " is created");
				}

				int posText = getArrayIndex(headingStrArr, "TEXT");
				int posLanguages = getArrayIndex(headingStrArr, "LANGUAGES");

				boolean columnExist = columnExist(con, tableName, "LANGUAGES");
				for (Iterator<String> iterator = valuesList.iterator(); iterator.hasNext();) {

					String rowValues = iterator.next();
					if (posText == -1 && posLanguages == -1) {
						rowValues = rowValues.replace('|', ',');
						insertQuery = "insert into " + tableName + " ( " + headingStr + ") " + " values(" + rowValues
								+ ")";
						System.out.println("Insert query::" + insertQuery);

						recordInserted = stmt.executeUpdate(insertQuery);

						if (recordInserted == 0) {
							System.out.println("Record could not be inserted and the insert query is::" + insertQuery);
							throw new DataUploadException(
									"Record could not be inserted and the insert query is::" + insertQuery);
						} else if (recordInserted == 1) {
							System.out.println("Successfully inserted record and the query is::" + insertQuery);
							numOfRecordsInserted++;
						} else {
							System.out.println("Could not insert record or more than one record inserted, "
									+ "this should not be the case:" + insertQuery);
						}

					} else {

						String[] headingStrArrRem = headingAndValueList.getHeadingStr().split("\\,");

						// String chk = rowValues.replaceAll("(?=(([^\']*\'){2})*[^\']*$),", "|");
						String[] rowValuesArr = rowValues.split("\\|");
						String[] rowValuesArrRem = rowValues.split("\\|");
						List<List<String>> langDataListOfList = getLangData(headingStrArr);
						String headingStrRem = null;
						String rowValuesRem = null;
						for (List<String> langDataList : langDataListOfList) {
							String[] langArr = new String[langDataList.size()];
							langArr = langDataList.toArray(langArr);
							for (int i = 0; i < langArr.length; i++) {
								int posHeader = getArrayIndex(headingStrArrRem, langArr[i]);
								headingStrArrRem = removeTheElement(headingStrArrRem, posHeader, con, tableName, stmt,
										deleteColumn);
								rowValuesArrRem = removeTheElement(rowValuesArrRem, posHeader, null, null, null,
										deleteColumn);
								headingStrRem = Arrays.toString(headingStrArrRem).replaceFirst("\\[", " ");
								headingStrRem = headingStrRem.substring(0, headingStrRem.length() - 1);
								rowValuesRem = Arrays.toString(rowValuesArrRem).replaceFirst("\\[", " ");
								rowValuesRem = rowValuesRem.substring(0, rowValuesRem.length() - 1);
							}
						}
						deleteColumn = true;
						headingStrRem = headingStrRem != null ? headingStrRem : headingAndValueList.getHeadingStr();
						rowValuesRem = rowValuesRem != null ? rowValuesRem : rowValues.replace('|', ',');

						if (headingStrRem.contains(SUBJECT_AREA)) {
							int posHeaderSubjectArea = getArrayIndex(headingStrArrRem, SUBJECT_AREA);
							String subjectAreaValue = rowValuesArrRem[posHeaderSubjectArea];

							if (subjectAreaValue != null
									&& !subjectAreaValue.isEmpty() /* && subjectAreaValue.length() > 1 */) {
								// subjectAreaValue = subjectAreaValue.substring(1, subjectAreaValue.length() -
								// 1);
								subjectAreaSet.add(subjectAreaValue);
							}
						}

						if (headingStrRem.contains("LANGUAGES")) {
							int posHeaderLanguages = getArrayIndex(headingStrArrRem, "LANGUAGES");
							if (!columnExist) {
								createColumn(stmt, tableName, "LANGUAGES");
								columnExist = true;
							}

							if (columnExist) {
								System.out.println("Rows values::" + Arrays.toString(rowValuesArrRem));
								if (rowValuesArrRem.length > posHeaderLanguages) {
									boolean isJSONValid = isJSONValid(rowValuesArrRem[posHeaderLanguages]);
									if (isJSONValid) {
										insertQuery = "insert into " + tableName + " ( " + headingStrRem + ") "
												+ " values(" + rowValuesRem + ")";
										System.out.println("Insert query::" + insertQuery);
										stmt = con.createStatement();
										recordInserted = stmt.executeUpdate(insertQuery);
										if (recordInserted == 0)
											System.out.println("Record could not be inserted and the insert query is::"
													+ insertQuery);
										else if (recordInserted == 1) {
											numOfRecordsInserted++;
											System.out.println("Successfully inserted record numbers into " + tableName
													+ "  ::" + numOfRecordsInserted);
										} else
											System.out.println(
													"Could not insert record or more than one record inserted, "
															+ "this should not be the case:" + insertQuery);

									} else {
										numOfRecordsInserted = numOfRecordsInserted + 1;
										System.out.println("Invalid Language Json at row number " + numOfRecordsInserted
												+ " ,hence exiting without inserting the data");
										throw new DataUploadException("Invalid Language Json at row number "
												+ numOfRecordsInserted + " ,hence exiting without inserting the data");
									}
								}
							}

						} else {
							insertQuery = "insert into " + tableName + " ( " + headingStrRem + ") " + " values("
									+ rowValuesRem + ")";
							System.out.println("Insert query::" + insertQuery);

							stmt = con.createStatement();
							recordInserted = stmt.executeUpdate(insertQuery, Statement.RETURN_GENERATED_KEYS);

							if (recordInserted == 0)
								System.out.println(
										"Record could not be inserted and the insert query is::" + insertQuery);
							else if (recordInserted == 1) {
								numOfRecordsInserted++;
								ResultSet rs = stmt.getGeneratedKeys();
								if (rs.next()) {
									resultID = rs.getInt(1);
								}
								tableExist = tableExist(con, tableName + "_LANG");
								if (tableExist) {
									if (dataUpdateChk == 0) {
										dropQuery = "DROP TABLE " + tableName + "_LANG";
										stmt = con.createStatement();
										recordInserted = stmt.executeUpdate(dropQuery);
										System.out.println("Table  " + tableName + " deleted");
										createTable(stmt, tableName + "_LANG", tableName);
									}

									insertDataToLangTable(rowValuesArr, headingStrArr, langDataListOfList, stmt,
											tableName + "_LANG", resultID, tableName, con, createColumn, columnExist);
									dataUpdateChk = 1;
								} else {
									createTable(stmt, tableName + "_LANG", tableName);
									insertDataToLangTable(rowValuesArr, headingStrArr, langDataListOfList, stmt,
											tableName + "_LANG", resultID, tableName, con, createColumn, columnExist);
									dataUpdateChk = 1;
								}
								// System.out.println("Successfully inserted record and the query is::" +
								// insertQuery);
								System.out.println("Successfully inserted record numbers into " + tableName + "  ::"
										+ numOfRecordsInserted);
							} else {
								System.out.println("Could not insert record or more than one record inserted, "
										+ "this should not be the case:" + insertQuery);
							}
						}
					}

				}
				con.commit();
			} catch (ClassNotFoundException ex) {
				ex.printStackTrace();
				System.out.println("Error: unable to load driver class!");
				throw new DataUploadException("ClassNotFoundException:: upload failed from Excel Data To DataBase");
			} catch (IllegalAccessException ex) {
				ex.printStackTrace();
				System.out.println("Error: access problem while loading!");
				throw new DataUploadException(
						"Error: access problem while loading! upload failed from Excel Data To DataBase");
			} catch (InstantiationException ex) {
				ex.printStackTrace();
				System.out.println("Error: unable to instantiate driver!");
				throw new DataUploadException("upload failed from Excel Data To DataBase");
			} catch (SQLException e) {
				try {
					if (con != null) {
						con.rollback();
					}
					System.out.println("Transaction rolled back");
				} catch (SQLException se2) {
					se2.printStackTrace();
					throw new DataUploadException(
							"Error: access problem while loading! upload failed from Excel Data To DataBase");
				}
				e.printStackTrace();
			} catch (DataUploadException e) {
				e.printStackTrace();
				System.out.println("Error:: dataupload exception");
				throw new DataUploadException("Error:: dataupload exception");
			} finally {
				if (con != null) {
					try {
						System.out.println("autocommit status::" + con.getAutoCommit());
						con.rollback();
						// System.out.println("Transaction rolled back");
						con.close();
					} catch (SQLException e) {
						System.out.println(
								"Ignore this exception as it is in finally block and connection is not getting closed");
						e.printStackTrace();
						throw new DataUploadException("Connection lost unexpectedly");
					}
				}
			}
			return numOfRecordsInserted;
		} catch (DataUploadException err) {
			throw err;
		}
	}

	private static void dropAndCreateTable(Connection con, Statement stmt, String tableName)
			throws DataUploadException, SQLException {
		if (stmt != null && !stmt.isClosed()) {
			// Drop table if exists
			if (tableExist(con, tableName)) {
				// Drop the table
				System.out.println(tableName + " exists and dropped::" + dropTable(con, stmt, tableName));
			}

			// create table
			String query = ExcelUtil.readConfig(SUBJECT_AREA, 0);
			createTableFromQuery(con, stmt, query);
		} else {
			throw new DataUploadException("Statement is closed to drop and create " + tableName + " table");
		}
	}

	private static void createTableFromQuery(Connection con, Statement stmt, String query) throws DataUploadException {
		try {
			if (stmt.isClosed())
				stmt = con.createStatement();

			stmt.executeUpdate(query);

			System.out.println("Table Created from query::" + query);
		} catch (SQLException e) {
			System.out.println("An error has occured on Table Creation");
			
			e.printStackTrace();
			throw new DataUploadException("An error has occured on Table Creation");
			//nandkishore
		}
	}

	public static boolean dropTable(Connection con, Statement stmt, String tableName) throws DataUploadException {
		String dropQuery = "DROP TABLE " + tableName;
		boolean isTableDropped = false;
		try {

			if (stmt.isClosed())
				stmt = con.createStatement();
			int recordsDropped = stmt.executeUpdate(dropQuery);

			System.out.println("Table  " + tableName + " dropped::" + recordsDropped);
			isTableDropped = true;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DataUploadException("An error has occured on Table Deletion");
			//nandkishore
		}

		return isTableDropped;
	}

	public static int getArrayIndex(String[] arr, String value) {
		int pos = -1;

		for (int i = 0; i < arr.length; i++)
			if (arr[i].equals(value)) {
				pos = i;
				break;
			} else {
				pos = -1;
			}
		return pos;
	}

	// Function to remove the element
	public static String[] removeTheElement(String[] arr, int index, Connection conn, String masterTableName,
			Statement stmt, boolean deleteColumn) {

		// If the array is empty
		// or the index is not in array range
		// return the original array
		if (arr == null || index < 0 || index >= arr.length) {
			return arr;
		}
		// Create another array of size one less
		String[] anotherArray = new String[arr.length - 1];

		// Copy the elements except the index
		// from original array to the other array
		for (int i = 0, k = 0; i < arr.length; i++) {
			// if the index is
			// the removal element index
			if (i == index) {
				try {
					if (!deleteColumn && conn != null) {
						boolean columnExistChk = columnExist(conn, masterTableName, arr[i]);
						if (columnExistChk) {
							deleteColumnExist(conn, masterTableName, arr[i], stmt);
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				continue;
			}
			// if the index is not
			// the removal element index
			anotherArray[k++] = arr[i];
		}
		// return the resultant array
		return anotherArray;
	}

	public static boolean schemaExist(Connection conn, String schemaName) throws SQLException {
		boolean sExists = false;
		try (ResultSet rs = conn.getMetaData().getCatalogs()) {
			while (rs.next()) {
				String schemaNameList = rs.getString(1);
				if (schemaNameList != null && schemaNameList.equals(schemaName)) {
					sExists = true;
					break;
				}
			}
		}
		return sExists;
	}

	public static boolean tableExist(Connection conn, String tableName) throws SQLException {
		boolean tExists = false;
		try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
			while (rs.next()) {
				String tName = rs.getString("TABLE_NAME");
				if (tName != null && tName.equals(tableName)) {
					tExists = true;
					break;
				}
			}
		}
		return tExists;
	}

	private static void createTable(Statement statement, String tableName, String masterTableName) throws DataUploadException {
		// String [] columnName = masterTableName.split("\\_");
		String tableCreation = "CREATE TABLE " + tableName + " (" + "ID INT(64) NOT NULL AUTO_INCREMENT," + ""
				+ masterTableName + "_ID INT(64) NOT NULL," + "TEXT text COLLATE utf8_bin,"
				+ "VOICE text COLLATE utf8_bin," + "VOICE_ONLY text COLLATE utf8_bin, "
				+ "LANGUAGE_CODE text COLLATE utf8_bin, " + "PRIMARY KEY(ID))";
		try {
			// This line has the issue
			statement.executeUpdate(tableCreation);
			// System.out.println("Table Created");
		} catch (SQLException e) {
			System.out.println("An error has occured on Table Creation");
			e.printStackTrace();
			throw new DataUploadException("An error has occured on Table Creation");
			//nandkishore
		}
	}

	private static void insertDataToLangTable(String[] rowValuesArr, String[] headingStrArr,
			List<List<String>> langDataListOfList, Statement stmt, String tableName, int resultID,
			String masterTableName, Connection con, boolean createColumn, boolean columnExist) throws DataUploadException {

		String headingStrAdd = null;
		String rowValuesAdd = null;
		String lang_Code;
		String lang_CodeReplace;
		for (List<String> langDataList : langDataListOfList) {
			String[] langArr = new String[langDataList.size()];
			langArr = langDataList.toArray(langArr);
			int[] posHeaderArrLang1 = new int[langArr.length];
			for (int i = 0; i < langArr.length; i++) {
				int posHeader = getArrayIndex(headingStrArr, langArr[i]);
				posHeaderArrLang1[i] = posHeader;
			}
			String[] rowValuesArrLang1 = rowValuesArr;
			String[] headingStrArrLang1 = headingStrArr;
			headingStrArrLang1 = addTheElement(headingStrArrLang1, posHeaderArrLang1);
			rowValuesArrLang1 = addTheElement(rowValuesArrLang1, posHeaderArrLang1);
			headingStrAdd = Arrays.toString(headingStrArrLang1).replace('[', ' ').replace(']', ' ');
			rowValuesAdd = Arrays.toString(rowValuesArrLang1).replace('[', ' ').replace(']', ' ');
			boolean isEmptyStringArray = isEmptyStringArray(rowValuesArrLang1);
			if (!isEmptyStringArray) {
				String[] text = headingStrArrLang1[0].split("_");
				if (text.length > 1) {
					lang_Code = text[1];
					lang_CodeReplace = "_" + text[1];
					headingStrAdd = headingStrAdd.replaceAll(lang_CodeReplace, "");
				} else {
					lang_Code = "EN";
				}
				try {
					// String [] columnName = masterTableName.split("\\_");
					String insertQuery = "insert into " + tableName + " ( " + masterTableName + "_ID, " + headingStrAdd
							+ ", LANGUAGE_CODE) " + " values(" + resultID + "," + rowValuesAdd + ",'" + lang_Code
							+ "')";
					// System.out.println(insertQuery);
					int recordInserted = stmt.executeUpdate(insertQuery, Statement.RETURN_GENERATED_KEYS);

					if (recordInserted == 0)
						System.out.println("Record could not be inserted and the insert query is::" + insertQuery);
					else if (recordInserted == 1) {
						ResultSet rs = stmt.getGeneratedKeys();
						if (rs.next()) {
							int insertResultID = rs.getInt(1);

							String selectGroupConcatLenQuery = "SELECT @@SESSION.group_concat_max_len";
							ResultSet recordSetGroupConcatLen = stmt.executeQuery(selectGroupConcatLenQuery);
							if (recordSetGroupConcatLen.next()) {
								String resultSetVal = recordSetGroupConcatLen
										.getString("@@SESSION.group_concat_max_len");

								if (Integer.parseInt(resultSetVal) == 1024) {
									String setGroupConcatLenQuery = "SET session group_concat_max_len=100000";
									stmt.executeUpdate(setGroupConcatLenQuery);
									selectGroupConcatLenQuery = "SELECT @@SESSION.group_concat_max_len";
									recordSetGroupConcatLen = stmt.executeQuery(selectGroupConcatLenQuery);
									if (recordSetGroupConcatLen.next()) {
										resultSetVal = recordSetGroupConcatLen
												.getString("@@SESSION.group_concat_max_len");
									}

								}

								if (Integer.parseInt(resultSetVal) > 1024) {
									if (!createColumn) {

										if (!columnExist) {
											createColumn(stmt, masterTableName, "LANGUAGES");
										}
										createColumn = true;
									}
									String updateQuery = "update " + masterTableName + " set LANGUAGES = "
											+ "(select concat('{',GROUP_CONCAT('\"',\r\n"
											+ "       concat(LANGUAGE_CODE,'\":'),JSON_OBJECT('TEXT',IFNULL(TEXT,\"\"),'VOICE',IFNULL(VOICE,\"\"),\r\n"
											+ "       'VOICE_ONLY',IFNULL(VOICE_ONLY,\"\"))SEPARATOR ','),'}') from "
											+ tableName + " WHERE \r\n" + "       " + masterTableName + "_ID = (SELECT "
											+ masterTableName + "_ID FROM " + tableName + " WHERE ID=" + insertResultID
											+ ")) \r\n" + "       WHERE " + masterTableName + "_ID = (SELECT "
											+ masterTableName + "_ID FROM " + tableName + " WHERE ID=" + insertResultID
											+ ") ";

									int recordUpdate = stmt.executeUpdate(updateQuery);

									System.out.println("resultSetVal is greater than 1024 so updating "
											+ "languages column ::" + recordUpdate);

								}
							}
						}
					} else
						System.out.println("Could not insert record or more than one record inserted, "
								+ "this should not be the case:" + insertQuery);
				} catch (SQLException e) {
					e.printStackTrace();
					throw new DataUploadException("Insert Data to lang table failed");
					//nandkishore
				}
			} else {
				if (!createColumn) {
					if (!columnExist) {
						createColumn(stmt, masterTableName, "LANGUAGES");
					}
					createColumn = true;
				}

				System.out.println("Record could not be inserted as the String Array is empty for Table " + tableName
						+ "_LANG  ::" + isEmptyStringArray);
			}
		}
	}

	// Function to remove the element
	public static String[] addTheElement(String[] arr, int[] index) {
		// Create another array of size one less
		String[] anotherArray = new String[index.length];

		// Copy the elements except the index
		// from original array to the other array
		for (int i = 0, k = 0; i < index.length; i++) {
			anotherArray[k++] = arr[index[i]];
		}

		// return the resultant array
		return anotherArray;
	}

	// Function to fetch lang Array
	public static List<List<String>> getLangData(String[] headingStrArr) {
		List<List<String>> dataList = new ArrayList<List<String>>();
		List<String> textList = new ArrayList<String>();
		List<String> voiceList = new ArrayList<String>();
		List<String> voiceOnlyList = new ArrayList<String>();
		for (int i = 0; i < headingStrArr.length; i++) {
			if (headingStrArr[i].startsWith("TEXT")) {
				textList.add(headingStrArr[i]);
			} else if (headingStrArr[i].startsWith("VOICE")) {
				if (headingStrArr[i].contains("VOICE_ONLY")) {
					voiceOnlyList.add(headingStrArr[i]);
				} else if (headingStrArr[i].contains("VOICE")) {
					voiceList.add(headingStrArr[i]);
				}
			}
		}
		for (int i = 0; i < textList.size(); i++) {
			String[] text = textList.get(i).split("_");
			List<String> tempList = new ArrayList<String>();

			if (text.length > 1) {
				if (textList.get(i).contains("_" + text[1])) {
					tempList.add(textList.get(i));
				}
				if (voiceList.get(i).contains("_" + text[1])) {
					tempList.add(voiceList.get(i));
				}
				if (voiceOnlyList.get(i).contains("_" + text[1])) {
					tempList.add(voiceOnlyList.get(i));
				}
			} else {
				if (textList.get(i).toUpperCase().equals("TEXT")) {
					tempList.add(textList.get(i));
				}
				if (voiceList.get(i).toUpperCase().equals("VOICE")) {
					tempList.add(voiceList.get(i));
				}
				if (voiceOnlyList.get(i).toUpperCase().equals("VOICE_ONLY")) {
					tempList.add(voiceOnlyList.get(i));
				}
			}
			dataList.add(tempList);
		}
		return dataList;

	}

	public static boolean isEmptyStringArray(String[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(null) || array[i].equals("null")) {

			} else {
				return false;
			}
		}
		return true;
	}

	public static void updateSubjectArea(HashSet<String> subjectAreaSet, int sheetNum) throws DataUploadException {
		System.out.println("Inserting records for Subject area");

		if (!subjectAreaSet.isEmpty()) {
			System.out.println("Unique subject area list we have got are::" + subjectAreaSet);

			String dbUrl = ExcelUtil.readConfig("DBURL", sheetNum);
			String dbUsername = ExcelUtil.readConfig("DBUSERNAME", sheetNum);
			String dbPassword = ExcelUtil.readConfig("DBPASSWORD", sheetNum);
			String schemaName = ExcelUtil.readConfig("DBSCHEMA_NAME", sheetNum);

			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();

				Connection con = DriverManager.getConnection(
						dbUrl + schemaName + "?useUnicode=yes&characterEncoding=UTF-8", dbUsername, dbPassword);
				con.setAutoCommit(false);

				Statement stmt = con.createStatement();

				dropAndCreateTable(con, stmt, SUBJECT_AREA);

				// insert records from subject area set
				System.out.println("Subject area records insert start");

				String mainQuery = ExcelUtil.readConfig(SUBJECT_AREA_INSERT_QUERY, 0);

				for (Iterator<String> iterator = subjectAreaSet.iterator(); iterator.hasNext();) {
					String subjectArea = iterator.next();
					// query = ExcelUtil.readConfig(SUBJECT_AREA_INSERT_QUERY, 0);
					String query = mainQuery.replace("?", subjectArea);
					stmt.execute(query);
				}

				System.out.println("Subject area records insert done");
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("unable to create " + SUBJECT_AREA + "Please create it manually");
				throw new DataUploadException("unable to create " + SUBJECT_AREA + "Please create it manually");
				//nandkishore
			}
		}
	}

	public static boolean columnExist(Connection conn, String masterTableName, String columnName) throws SQLException {
		boolean colExists = false;
		try (ResultSet rs = conn.getMetaData().getColumns(null, null, masterTableName, columnName)) {
			while (rs.next()) {
				String colName = rs.getString("COLUMN_NAME");
				if (colName != null && colName.equals(columnName)) {
					colExists = true;
					break;
				}
			}
		}
		return colExists;
	}

	private static void createColumn(Statement statement, String masterTableName, String columnName) {
		String columnCreation = "ALTER TABLE " + masterTableName + "\r\n" + "    ADD COLUMN " + columnName
				+ " text CHARACTER SET utf16 COLLATE utf16_bin DEFAULT NULL";
		try {
			// This line has the issue
			statement.executeUpdate(columnCreation);
			// System.out.println("Column Created");
		} catch (SQLException e) {
			// System.out.println("An error has occured on Column Creation");
			e.printStackTrace();
		}
	}

	public static void deleteColumnExist(Connection conn, String masterTableName, String columnName, Statement stmt)
			throws SQLException {
		try (ResultSet rs = conn.getMetaData().getColumns(null, null, masterTableName, columnName)) {
			while (rs.next()) {
				String colName = rs.getString("COLUMN_NAME");
				if (colName != null && colName.equals(columnName)) {
					String columnDeletion = "ALTER TABLE " + masterTableName + "\r\n" + "    DROP COLUMN " + columnName
							+ " ";
					stmt.executeUpdate(columnDeletion);
					
					break;
				}
			}
		}
	}

	public static boolean isJSONValid(String jsonInString) {
		System.out.println("inside isJSONValid method with input parameter::" + jsonInString);
		if (!jsonInString.equals("null")) {
			jsonInString = jsonInString.replace("'", "");
			try {
				new JSONObject(jsonInString);
			} catch (JSONException ex) {
				// edited, to include @Arthur's comment
				// e.g. in case JSONArray is valid as well...
				try {
					new JSONArray(jsonInString);
				} catch (JSONException ex1) {
					ex1.printStackTrace();
					return false;
				}
			}
		}
		return true;
	}
}
