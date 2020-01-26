package com.tf.yana.logstash;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.tf.yana.exception.DataUploadException;

public class LogStashUtil {
	
	public static void main(String args[]) throws DataUploadException
	{
		try {
		uploadData("D:\\Latha\\eclipse-workspace\\DataUpload\\Data\\LogstashConfig\\TYPE.conf");
		}catch(DataUploadException err) {
			throw err;
		}
	}
	
	public static String uploadData(String configFileName) throws DataUploadException
	{
		String errorMessage = "";
		try
		{
			
			ProcessBuilder builder = new ProcessBuilder("bash", "-c", "logstash -f "+ configFileName);
			
	        builder.redirectErrorStream(true);
	        Process p = builder.start();
	        
	        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String line;
	        
	        while (true) {
	            line = r.readLine();
	           
	            if (line == null) { break; }
	           
	            if(line.contains("Failed to execute"))
	            {
	            	errorMessage = line;
	            }
	        }
	        
	        p.waitFor();
	        System.out.println("command exit value::" + p.exitValue());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new DataUploadException("Upload Data Failed");
		}
		
		return errorMessage;
	}
	
	public static String uploadDataAsString(String configString) throws DataUploadException
	{
		String errorMessage = "";
		try
		{
			
			ProcessBuilder builder = new ProcessBuilder(
				"bash", "-c", 
	            "logstash -e "+ configString);
	        builder.redirectErrorStream(true);
	        Process p = builder.start();
	        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        String line;
	        
	        while (true) {
	            line = r.readLine();
	           
	            if (line == null) { break; }
	            
	            if(line.contains("Failed to execute"))
	            {
	         
	            	errorMessage = line;
	            }
	        }
	        
	        System.out.println("command exit value:::" + p.exitValue());
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			throw new DataUploadException("Upload Data Failed");
		}
		System.out.println("errorMessage------" + errorMessage);
		return errorMessage;
	}

}
