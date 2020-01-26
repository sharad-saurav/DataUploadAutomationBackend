package com.tf.yana.elasticsearch;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.lang3.StringUtils;

import com.tf.yana.excel.ExcelUtil;
import com.tf.yana.exception.DataUploadException;

public class ElasticDumpUtil {

	/***
	 * Run elsatic dummp to copy analyzer, mapping and data from source ES URL to
	 * destination ES URL
	 * 
	 * @param sourceESURL
	 * @param destinationESURL
	 * @throws DataUploadException
	 */
	public static boolean dumpIndexDataFromESToES(String sourceESURL, String destinationESURL, String esIndexName)
			throws DataUploadException {
		try {
			boolean copiedCompletely = false;

			System.out.println("Deleting the index on destination::" + esIndexName);

			System.out.println("Source dump clean up");
			EslaticsearchUtil.esMapping(sourceESURL, "", esIndexName + "_dump");

			// Taking backup of respective destination onto source
			String dumpDataNDelete = "elasticdump / --input=" + destinationESURL + "/" + esIndexName + " / --output="
					+ sourceESURL + "/" + esIndexName + "_dump / --type=data --delete";
		
			System.out.println("dumpDataNDelete-------" + dumpDataNDelete);
			String erroMsg = executeCommandAsProcess(dumpDataNDelete);
			System.out.println("erroMsg-------" + erroMsg);
			if (StringUtils.isEmpty(erroMsg)) {
				System.out.println("back of destination is taken on source with index suffixed with _dump");
			}

			// Execute analyzer first
			String analyzerCmd = "elasticdump / --input=" + sourceESURL + "/" + esIndexName + " / --output="
					+ destinationESURL + "/" + esIndexName + " / --type=analyzer";
			
			erroMsg = executeCommandAsProcess(analyzerCmd);
			System.out.println("Analyzer copied and the process output is::" + erroMsg);

			if (StringUtils.isEmpty(erroMsg)) {
				String mappingCmd = "elasticdump / --input=" + sourceESURL + "/" + esIndexName + " / --output="
						+ destinationESURL + "/" + esIndexName + " / --type=mapping";
				erroMsg = executeCommandAsProcess(mappingCmd);
				System.out.println("Mapping copied and the process output is::" + erroMsg);

				if (StringUtils.isEmpty(erroMsg)) {
					String dataCmd = "elasticdump / --input=" + sourceESURL + "/" + esIndexName + " / --output="
							+ destinationESURL + "/" + esIndexName + " / --type=data";
					erroMsg = executeCommandAsProcess(dataCmd);
					System.out.println("data copied and the process output is::" + erroMsg);

					if (StringUtils.isEmpty(erroMsg)) {
						copiedCompletely = true;
					}
				}
			}
			return copiedCompletely;
		} catch (DataUploadException err) {
			throw err;
		}

		
	}

	public static String executeCommandAsProcess(String command) throws DataUploadException {

		String errorMessage = "";
		try {
			System.out.println("command---------" + command);
			
			ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
			builder.redirectErrorStream(true);
			Process p = builder.start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;

			while (true) {
				line = r.readLine();
				if (line == null) {
					break;
				}
				System.out.println(line);
				if (line.contains("Failed to execute")) {
					errorMessage = line;
				}
			}
			p.waitFor();
			System.out.println("command exit value::" + p.exitValue());
		} catch (Exception ex) {
			errorMessage = ex.getMessage();
			ex.printStackTrace();
			throw new DataUploadException(errorMessage);
		}

		return errorMessage;
	}
}
