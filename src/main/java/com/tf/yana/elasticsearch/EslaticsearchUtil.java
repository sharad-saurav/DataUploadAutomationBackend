package com.tf.yana.elasticsearch;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import com.tf.yana.excel.ExcelUtil;
import com.tf.yana.exception.DataUploadException;

public class EslaticsearchUtil {
	public static int sheetNum =1;
	
	public static int esConnect(String docTypeName, String method, String query) throws DataUploadException {
		int code = 0;
		try
		{
			String esUrl = ExcelUtil.readConfig("ESURL", sheetNum);
		    String esAuth = ExcelUtil.readConfig("AUTHORIZATION", sheetNum);
				
			URL url = new URL(esUrl+"/"+docTypeName);
			String encodedAuth = "Basic " + java.util.Base64.getEncoder().encodeToString(esAuth.getBytes());
		    HttpURLConnection httpcon = (HttpURLConnection) url
	                 .openConnection();
	         httpcon.setReadTimeout(60 * 1000);
	         httpcon.setConnectTimeout(60 * 1000);
	         httpcon.setRequestProperty("Authorization", encodedAuth);
	         httpcon.setRequestMethod(method);
	         if (method.equals("PUT") || method.equals("POST")) {
	        	 httpcon.setRequestProperty("Content-Type", "application/json");
	        	 httpcon.setDoOutput(true);
	             OutputStreamWriter output=new OutputStreamWriter(httpcon.getOutputStream());
	             output.write(query);
	             output.flush();
	         }
             httpcon.connect();
	         code = httpcon.getResponseCode();
	         System.out.println("response code="+code);
	         String output=httpcon.getResponseMessage();
	         System.out.println("Message::"+output);
		} catch(Exception e) {
			e.printStackTrace();
			throw new DataUploadException("es connection failed");
		}
		return code;
	}
	
	public static int esConnect(String esUrl, String esAuth, 
			String docTypeName, String method, String query) throws DataUploadException {
		int code = 0;
		try
		{
			URL url = new URL(esUrl+"/"+docTypeName);
			
		    HttpURLConnection httpcon = (HttpURLConnection) url
	                 .openConnection();
	         httpcon.setReadTimeout(60 * 1000);
	         httpcon.setConnectTimeout(60 * 1000);
	         
	         if(esAuth != null)
	         {
	        	 String encodedAuth = "Basic " + 
	        			 java.util.Base64.getEncoder().encodeToString(esAuth.getBytes());
	        	 httpcon.setRequestProperty("Authorization", encodedAuth);
	         }
	         
	         httpcon.setRequestMethod(method);
	         if (method.equals("PUT") || method.equals("POST")) {
	        	 httpcon.setRequestProperty("Content-Type", "application/json");
	        	 httpcon.setDoOutput(true);
	             OutputStreamWriter output=new OutputStreamWriter(httpcon.getOutputStream());
	             output.write(query);
	             output.flush();
	         }
             httpcon.connect();
	         code = httpcon.getResponseCode();
	         System.out.println("response code="+code);
	         String output=httpcon.getResponseMessage();
	         System.out.println("Message::"+output);
		} catch(Exception e) {
			e.printStackTrace();
			throw new DataUploadException("es connection failed");
		}
		return code;
	}
	
	public static void esMapping(String docTypeName) throws DataUploadException {
		int checkEsValid = esConnect(docTypeName, "GET" , "");
		if (checkEsValid == 200) {
			esConnect(docTypeName, "DELETE", "");
			System.out.println(docTypeName+ " index is deleted");
		} 
		String query = ExcelUtil.readConfig(docTypeName.toUpperCase(), sheetNum);
		esConnect(docTypeName, "PUT", query);
		System.out.println(docTypeName+ " Index is created");
	}
	
	public static void esMapping(String esUrl, String esAuth, String docTypeName) throws DataUploadException {
		int checkEsValid = esConnect(esUrl, esAuth, docTypeName, "GET" , "");
		if (checkEsValid == 200) {
			esConnect(esUrl, esAuth, docTypeName, "DELETE", "");
			System.out.println(docTypeName+ " index is deleted");
		} 
		String query = ExcelUtil.readConfig(docTypeName.toUpperCase(), sheetNum);
		esConnect(esUrl, esAuth, docTypeName, "PUT", query);
		System.out.println(docTypeName+ " Index is created");
	}
	
}
