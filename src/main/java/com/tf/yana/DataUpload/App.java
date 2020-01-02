package com.tf.yana.DataUpload;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.tf.yana.db.DBUtil;
import com.tf.yana.excel.ExcelUtil;
import com.tf.yana.excel.model.MessageFormatTemplate;
import com.tf.yana.exception.DataUploadException;
import com.tf.yana.javatpoint.ResultMap;
import com.tf.yana.logstash.LogStashUtil;
import com.tf.yana.elasticsearch.ElasticDumpUtil;
import com.tf.yana.elasticsearch.EslaticsearchUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class App implements Constants {
	static ArrayList<String> columnNamesToSkip = new ArrayList<String>();
	static Map<String, MessageFormatTemplate> columnsToFormat = new HashMap<String, MessageFormatTemplate>();
	static File[] directoryListing = null;
	static String filePathHome = null;

	public static ResultMap startProcessing() throws DataUploadException, IOException {
		try {
			long startTime = System.currentTimeMillis();
			filePathHome = null;
			directoryListing = null;
			

			filePathHome = ExcelUtil.readConfig("DATA_FILE_PATH", 0);
			File dir = new File(filePathHome);
			directoryListing = dir.listFiles();

			System.out.println("Excel files upload starting");
			Map<String, Integer> results = uploadFilesInDirectioryToDB();
			System.out.println("Excel file upload finished");

			System.out.println("Log stash upload starting");
			Map<String, String> esResults = uploadDataFromDBToES();
			System.out.println("Log stash upload finished");
			
			ResultMap rsMap = new ResultMap();
			rsMap.setResult(results);
			rsMap.setEsResult(esResults);
			long endTime = System.currentTimeMillis();
			long timeElapsed = endTime - startTime;
			long seconds = (timeElapsed / 1000) % 60;
			System.out.println("Process Execution time in milliseconds: " + timeElapsed);
			System.out.println("Process Execution time in seconds: " + seconds);
			return rsMap;
		} catch (DataUploadException err) {
			System.out.println("hit error---------------------------------------------------------11");
			throw err;
		}
	}

	public static Map<String, Integer> uploadFilesInDirectioryToDB() throws DataUploadException {
		try {
			String skipColumnsStr = ExcelUtil.readConfig("IGNORE_COLUMN", 0);
			Map<String, Integer> results = new HashMap<String, Integer>();
			String[] commaSeparatedArr = skipColumnsStr.split("\\s*,\\s*");
			columnNamesToSkip.addAll(Arrays.asList(commaSeparatedArr));

			System.out.println("Coulmns to be skipped::" + columnNamesToSkip);

			MessageFormatTemplate msgFormatTemplate = new MessageFormatTemplate();
			msgFormatTemplate.setMessageFormat(
					"\"URL\":\"{0}\", " + "\"MEDIA_TYPE\":\"{1}\", \"TITLE\":\"{2}\", \"AUTHOR\":\"{3}\"");
			msgFormatTemplate.setVariableCount(4);
			columnsToFormat.put("MEDIA", msgFormatTemplate);

			HashSet<String> subjectAreaSet = new HashSet<String>();

			if (directoryListing != null) {
				for (File child : directoryListing) {

					System.out.println("Uploading the  " + child.getPath() + "data to data base");

					int recordsInserted = DBUtil.uploadExcelDataToDB(child.getPath(), columnNamesToSkip,
							columnsToFormat, subjectAreaSet);

					System.out.println("Data upload completed for " + child.getPath());
					results.put(child.getName(), recordsInserted);
				}

				DBUtil.updateSubjectArea(subjectAreaSet, 0);

				System.out.println("Excel to Db upload Summary::");
				System.out.println(results);

			} else {

				System.out.println("No files in exists in the folder:" + filePathHome);
				throw new DataUploadException("No files in exists in the folder:" + filePathHome);
			}
			
			return results;

		} catch (DataUploadException err) {
			System.out.println("hit error---------------------------------------------------------12");
			throw err;
		}
	}

	public static Map<String, String> uploadDataFromDBToES() throws IOException, DataUploadException {
		try {
			String esURL = ExcelUtil.readConfig(Constants.LOCAL_ES_URL, 1);
			String esauth = ExcelUtil.readConfig(Constants.AUTHORIZATION, 1);
			int esConnectTestCode = EslaticsearchUtil.esConnect(esURL, esauth, "", "GET", "");
			
			Map<String, String> esResults = new HashMap<String, String>();

			if (esConnectTestCode == 200) {
				Map<String, String> logStashConfigData = ExcelUtil
						.getTwoColumnRowDataAsMap(System.getenv("DATA_UPLOAD_CONFIG_FILE"), 2);

				String parentDirectory = null;

				String connectionString = ExcelUtil.readConfig(Constants.DBURL, 0);
				String dbUserName = ExcelUtil.readConfig(Constants.DBUSERNAME, 0);
				String dbPassword = ExcelUtil.readConfig(Constants.DBPASSWORD, 0);
				String dbSchema = ExcelUtil.readConfig(Constants.DBSCHEMA_NAME, 0);

				/*
				 * String esURL = ExcelUtil.readConfig(Constants.ESURL, 1); esURL =
				 * esURL.replaceAll("https://", "");
				 */
				// String authorization = ExcelUtil.readConfig(Constants.AUTHORIZATION, 1);

				String driverPath = ExcelUtil.readConfig(Constants.JDBC_DRIVER_LIBRARY, 2);

				for (int i = 0; i < directoryListing.length; i++) {

					if (parentDirectory == null) {
						parentDirectory = directoryListing[i].getParent();
					}

					String propertyName = FilenameUtils
							.removeExtension(FilenameUtils.getName(directoryListing[i].getName().toLowerCase()));

					try {

						String propertyValue = logStashConfigData.get(propertyName);

						System.out.println(propertyName + "Property value from Excel::" + propertyValue);

						// replacing all the variables

						/*
						 * propertyValue = propertyValue.replaceAll( Constants.AUTHORIZATION_REPLACE,
						 * authorization);
						 */
						propertyValue = propertyValue.replaceAll(Constants.DB_USER_NAME_REPLACE, dbUserName);
						propertyValue = propertyValue.replaceAll(Constants.DB_PASSWORD_REPLACE, dbPassword);

						propertyValue = propertyValue.replaceAll(Constants.CONNECTION_STRING_REPLACE, connectionString);

						propertyValue = propertyValue.replaceAll(Constants.JDBC_DRIVER_LIBRARY_REPLACE, driverPath);

						propertyValue = propertyValue.replaceAll(Constants.SCHEMA_NAME_REPLACE, dbSchema);

						propertyValue = propertyValue.replaceAll(Constants.LOCAL_ES_URL_REPLACE, esURL);

						System.out.println(propertyName + "Property value after replacing constants::" + propertyValue);

						String fileName = parentDirectory + "\\" + propertyName + ".conf";
						FileWriter fileWriter = new FileWriter(fileName);

						fileWriter.write(propertyValue);
						fileWriter.close();

						System.out.println("Loading index mapping for ::" + propertyName);
						// EslaticsearchUtil.esMapping(propertyName);
						EslaticsearchUtil.esMapping(esURL, esauth, propertyName);

						System.out.println("loading data through logstash for index:" + propertyName);
						String errorMessage = LogStashUtil.uploadData(fileName);
						FileUtils.forceDelete(new File(fileName));
						System.out.println("File deleted");

						if (!errorMessage.isEmpty()) {
							System.out.println("ERROR: Could not upload data for index :" + propertyName);
							System.out.println("Please see the log trace above");
							throw new DataUploadException("ERROR: Could not upload data for index :" + propertyName);
							// nandkishore
						} else {
							System.out.println("Data upload completed for " + propertyName);
						}

						esResults.put(propertyName, errorMessage);

						String remoteESURl = ExcelUtil.readConfig("REMOTE_ES_URL", 1);
						System.out.println("starting elastic dump");
						ElasticDumpUtil.dumpIndexDataFromESToES(esURL, remoteESURl, propertyName);
					} catch (IOException e) {
						e.printStackTrace();
						throw e;
					}
				}
			} else {
				System.out.println("Can not connect to" + esURL + " elsatic, please run local elastic");
				throw new DataUploadException("Can not connect to" + esURL + " elsatic, please run local elastic");
			}

			System.out.println("LogStash upload summary::");
			System.out.println(esResults);
			
			return esResults;
		} catch (DataUploadException err) {
			throw err;
		}

	}

}
