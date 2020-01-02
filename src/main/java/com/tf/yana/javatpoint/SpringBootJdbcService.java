package com.tf.yana.javatpoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.tf.yana.DataUpload.App;
import com.tf.yana.exception.DataUploadException;



@Service
public class SpringBootJdbcService {
	
	public static ResultMap callDataUpload() throws DataUploadException, IOException {
	  try {
		  ResultMap rsMap = new ResultMap();
		  rsMap = App.startProcessing();
		  return rsMap;
		  } catch (DataUploadException err) {
				System.out.println("hit error---------------------------------------------------------8");
		    throw err;
		  }
	  
	}
}
