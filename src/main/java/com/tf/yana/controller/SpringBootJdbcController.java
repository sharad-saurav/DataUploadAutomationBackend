package com.tf.yana.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tf.yana.PathVariables;
import com.tf.yana.ResultMap;
import com.tf.yana.DataUpload.App;
import com.tf.yana.exception.DataUploadException;
import com.tf.yana.service.SpringBootJdbcService;

@RestController
public class SpringBootJdbcController {
//	@Autowired
//	JdbcTemplate jdbc;

	@Autowired
	private SpringBootJdbcService springBootJdbcService;

//	@RequestMapping("/insert")
//	public String index() {
//		jdbc.execute("insert into user(name,email)values('javatpoint','java@javatpoint.com')");
//		return "data inserted Successfully";
//	}
	private static final int BUFFER_SIZE = 4096;

//	@ResponseStatus(HttpStatus.OK)
//	@RequestMapping(value = "/uploadZippedFile", method = RequestMethod.POST, consumes = "multipart/form-data")
//	public String uploadZipFiles(@RequestParam("file") MultipartFile DataUploadConfig) throws Exception {
//		System.out.println(DataUploadConfig.getOriginalFilename() + "---");
//		
//		return null;
//	}

	@CrossOrigin
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/uploadFile", method = RequestMethod.POST, consumes = "multipart/form-data")
	public ResultMap uploadFiles(@RequestParam("file") MultipartFile file,
			@RequestParam("configurationFile") MultipartFile configurationFile) throws Exception {

		ResultMap rsMap = new ResultMap();
		String newFilePath = "";
		String tempDir = System.getProperty("java.io.tmpdir");
		String destDir = tempDir + File.separator + "dataUploadFolder";
		String destConfigDir = tempDir + File.separator + "configurationUploadFolder";
		File configDir = new File(destConfigDir);

		if (!configDir.exists()) {
			configDir.mkdirs();
		} else {
			deleteDir(configDir);
			configDir.mkdirs();
		}
		
		File configFile = new File(configDir + File.separator + "DataUploadConfig.xlsx");

		InputStream cis = configurationFile.getInputStream();
		copyInputStreamToFile(cis, configFile);

		File dir = new File(destDir);

		if (!dir.exists()) {
			dir.mkdirs();
		} else {
			deleteDir(dir);
			dir.mkdirs();
		}
		
		InputStream fis;
		byte[] buffer = new byte[1024];
		try {
			fis = file.getInputStream();
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				String fileName = ze.getName();
				File newFile = new File(destDir + File.separator + fileName);
				newFilePath = newFile.getParent();
				
				new File(newFile.getParent()).mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
				// close this ZipEntry
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
			// close last ZipEntry
			zis.closeEntry();
			zis.close();
			fis.close();
			
			PathVariables.pathOfData = newFilePath;
			
			PathVariables.pathOfConfig = configFile.getAbsolutePath();

			
			rsMap = SpringBootJdbcService.callDataUpload();
			return rsMap;
		} catch (IOException e) {
			e.printStackTrace();
			throw new DataUploadException("IOexception file upload to excel failed");
		} catch (DataUploadException err) {
			throw err;
		}
	}

	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

	private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
		new File(file.getParent()).mkdirs();
		try (FileOutputStream outputStream = new FileOutputStream(file)) {

			int read;
			byte[] bytes = new byte[1024];
			while ((read = inputStream.read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
			outputStream.close(); // commons-io //IOUtils.copy(inputStream,outputStream);
		}
	}
}