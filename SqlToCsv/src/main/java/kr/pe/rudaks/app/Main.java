package kr.pe.rudaks.app;

import au.com.bytecode.opencsv.CSVWriter;
import com.dropbox.core.*;
import com.dropbox.core.json.JsonReader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class Main 
{
	private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 8L << 20; // 8MiB
    private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;
    
	private String driver;
	private String url;
	private String user;
	private String password;
	private String startdate;
	private String enddate;
	private String sqlFilesEncoding;
	private String sqlFilesDir;
	private String outputEncoding;
	private String outputDir;
	private String outputDaily;
	private String dropboxUpload;
	private String dropboxUploadPath;
	private String dropboxAuthfile;
			
	private String replaceString;
	
	public static void main(String[] args) throws Exception 
	{
		Main main = new Main();
		
    	print("[properties] db.properties");
    	main.loadProperty("db.properties");
    	println("==> OK");
		
		main.execute();
	}

	private void execute() throws IOException
	{		
		HashMap<String, String> fileListMap = getFileList();
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try
		{
			print("[jdbc driver]" + driver);
			Class.forName(driver);
			println("==> OK");
			print("[Connection url]" + url);
			print(", user : " + user);
			print(", pass : " + password);
			
			conn = DriverManager.getConnection(url, user, password);
			println("==> OK");
            
			HashMap replaceStringMap = getQueryMap(replaceString);
			
			String currDate = DateUtil.getCurrDate("yyyyMMdd");
			String yesterday = DateUtil.calDays(currDate, -1);
			
			List<DropboxFile> dropboxFileList = new ArrayList<DropboxFile>();
			
            if ("true".equals(outputDaily))
            {
            	outputDir = outputDir + "/" + yesterday;
            	FileUtils.forceMkdir(new File(outputDir));
            }
            
			for (String key : fileListMap.keySet())
			{
	            String sql = fileListMap.get(key);
	            //System.err.println(sql);
	            
	            println("[execute sql] " + key);
	            
	            stmt = conn.createStatement();
	            try
	            {
	            	// 치환
	            	if (replaceStringMap != null && replaceStringMap.size() > 0)
	            	{
		            	Set<String> keys = replaceStringMap.keySet();
		            	for (String name : keys)
		            	{
		            	   String value = (String) replaceStringMap.get(name);		            	   
		            	   sql = StringUtils.replace(sql, name, value);
		            	}
		            	
		            	//System.err.println("--------------------");
		            	//System.err.println(sql);
	            	}
	            	
	            	
	            	rs = stmt.executeQuery(sql);
	            	            
	            	String filename = key.replaceAll(".sql", "");
		            String outputFile = outputDir + "/" + filename + ".csv";
		            	            	            
		            CSVWriter writer = new CSVWriter(new FileWriterWithEncoding(outputFile, outputEncoding));
		            writer.writeAll(rs, true);	            
		            writer.close();
		            
		            println("[saved] " + outputFile);		            
		            	            
		            if ("true".equals(dropboxUpload))
		            {
		            	String dropboxUploadFile = dropboxUploadPath + "/" + filename + ".csv";
		            	if ("true".equals(outputDaily))
		            	{
		            		dropboxUploadFile = dropboxUploadPath + "/" + yesterday + "/" + filename + ".csv";
		            	}
		            	
		            	println("[upload] " + outputFile);
		            	println("[dropbox path] " + dropboxUploadFile);
		            	
		            	DropboxFile dropboxFile = new DropboxFile();
		            	dropboxFile.localPath = outputFile;
		            	dropboxFile.dropboxPath = dropboxUploadFile;
		            	
		            	dropboxFileList.add(dropboxFile);
		            }
		            rs.close();
		            
		            if (dropboxFileList != null && dropboxFileList.size() > 0)
		            {
		            	uploadDropbox(dropboxFileList);
		            }
	            }
	            catch (Exception e)
	            {
	            	println("[ERROR] sql");
	            	println(sql);
	            	e.printStackTrace();
	            }
	            stmt.close();
	        }
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
    	{
    		if (stmt != null) { try { stmt.close(); } catch (Exception e) {} }
    		if (conn != null) { try { conn.close(); } catch (Exception e) {} }
    	}
	}
	
	private HashMap<String, String> getFileList() throws IOException
	{
		HashMap<String, String> hm = new HashMap<String, String>();

		if (sqlFilesDir.startsWith("."))
			sqlFilesDir = System.getProperty("user.dir") + "/" + sqlFilesDir;

		//sqlFilesDir = "D:\\_GIT\\java\\SqlToCsv\\sql";
		File fileList = new File(sqlFilesDir);
		
		File [] selectedFiles = fileList.listFiles(new FileFilter() {
			
			//@Override
			public boolean accept(File pathname) {
				// TODO Auto-generated method stub
				if (pathname.getName().endsWith("sql"))
					return true;
				else
					return false;
			}
		});
		
		if (selectedFiles != null)
		{
			for (File selectedFile : selectedFiles) 
			{
			    String sql = FileUtils.readFileToString(selectedFile, sqlFilesEncoding);
			    
			    //System.out.println("sql : " + sql);
			    hm.put(selectedFile.getName(), sql);
			}
		}
		else
		{
			System.err.println("no selected file in " + sqlFilesDir);
		}
		
		return hm;
	}
	
	private void uploadDropbox(List dropboxFileList) //String [] localPath, String [] dropboxPath)
	{		
		DbxAuthInfo authInfo;
		try 
		{
            authInfo = DbxAuthInfo.Reader.readFromFile(dropboxAuthfile);
        } 
		catch (JsonReader.FileLoadException ex) 
		{
            System.err.println("Error loading <auth-file>: " + ex.getMessage());
            //System.exit(1);
            return;
        }
		
		String userLocale = Locale.getDefault().toString();
        DbxRequestConfig requestConfig = new DbxRequestConfig("examples-upload-file", userLocale);
		DbxClientV2 dbxClient = new DbxClientV2(requestConfig, authInfo.getAccessToken(), authInfo.getHost());
		
		for (int i=0; i<dropboxFileList.size(); i++)
		{
			DropboxFile dropboxFile = (DropboxFile) dropboxFileList.get(i); 
			File localFile = new File(dropboxFile.localPath);
			
			uploadFile(dbxClient, localFile, dropboxFile.dropboxPath);
		}
		
	}
	
	private static void chunkedUploadFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) 
	{
        long size = localFile.length();

        // assert our file is at least the chunk upload size. We make this assumption in the code
        // below to simplify the logic.
        if (size < CHUNKED_UPLOAD_CHUNK_SIZE) {
            System.err.println("File too small, use upload() instead.");
            //System.exit(1);
            return;
        }

        long uploaded = 0L;
        DbxException thrown = null;

        // Chunked uploads have 3 phases, each of which can accept uploaded bytes:
        //
        //    (1)  Start: initiate the upload and get an upload session ID
        //    (2) Append: upload chunks of the file to append to our session
        //    (3) Finish: commit the upload and close the session
        //
        // We track how many bytes we uploaded to determine which phase we should be in.
        String sessionId = null;
        for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
            if (i > 0) {
                System.out.printf("Retrying chunked upload (%d / %d attempts)\n", i + 1, CHUNKED_UPLOAD_MAX_ATTEMPTS);
            }

            try {
            	
            	InputStream in = new FileInputStream(localFile);
                // if this is a retry, make sure seek to the correct offset
                in.skip(uploaded);

                // (1) Start
                if (sessionId == null) {
                    sessionId = dbxClient.files().uploadSessionStart()
                        .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE)
                        .getSessionId();
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    printProgress(uploaded, size);
                }

                // (2) Append
                while ((size - uploaded) > CHUNKED_UPLOAD_CHUNK_SIZE) {
                    dbxClient.files().uploadSessionAppend(sessionId, uploaded)
                        .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE);
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    printProgress(uploaded, size);
                }

                // (3) Finish
                long remaining = size - uploaded;
                UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);
                CommitInfo commitInfo = CommitInfo.newBuilder(dropboxPath)
                    .withMode(WriteMode.ADD)
                    .withClientModified(new Date(localFile.lastModified()))
                    .build();
                FileMetadata metadata = dbxClient.files().uploadSessionFinish(cursor, commitInfo)
                    .uploadAndFinish(in, remaining);

                System.out.println(metadata.toStringMultiline());
                return;
            } catch (RetryException ex) {
                thrown = ex;
                // RetryExceptions are never automatically retried by the client for uploads. Must
                // catch this exception even if DbxRequestConfig.getMaxRetries() > 0.
                sleepQuietly(ex.getBackoffMillis());
                continue;
            } catch (NetworkIOException ex) {
                thrown = ex;
                // network issue with Dropbox (maybe a timeout?) try again
                continue;
            } catch (UploadSessionLookupErrorException ex) {
                if (ex.errorValue.isIncorrectOffset()) {
                    thrown = ex;
                    // server offset into the stream doesn't match our offset (uploaded). Seek to
                    // the expected offset according to the server and try again.
                    uploaded = ex.errorValue
                        .getIncorrectOffsetValue()
                        .getCorrectOffset();
                    continue;
                } else {
                    // Some other error occurred, give up.
                    System.err.println("Error uploading to Dropbox: " + ex.getMessage());
                    //System.exit(1);
                    return;
                }
            } catch (UploadSessionFinishErrorException ex) {
                if (ex.errorValue.isLookupFailed() && ex.errorValue.getLookupFailedValue().isIncorrectOffset()) {
                    thrown = ex;
                    // server offset into the stream doesn't match our offset (uploaded). Seek to
                    // the expected offset according to the server and try again.
                    uploaded = ex.errorValue
                        .getLookupFailedValue()
                        .getIncorrectOffsetValue()
                        .getCorrectOffset();
                    continue;
                } else {
                    // some other error occurred, give up.
                    System.err.println("Error uploading to Dropbox: " + ex.getMessage());
                    //System.exit(1);
                    return;
                }
            } catch (DbxException ex) {
                System.err.println("Error uploading to Dropbox: " + ex.getMessage());
                //System.exit(1);
                return;
            } catch (IOException ex) {
                System.err.println("Error reading from file \"" + localFile + "\": " + ex.getMessage());
                //System.exit(1);
                return;
            }
        }

        // if we made it here, then we must have run out of attempts
        System.err.println("Maxed out upload attempts to Dropbox. Most recent error: " + thrown.getMessage());
        //System.exit(1);
    }
	
	private static void printProgress(long uploaded, long size) {
        System.out.printf("Uploaded %12d / %12d bytes (%5.2f%%)\n", uploaded, size, 100 * (uploaded / (double) size));
    }
	
	private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // just exit
            System.err.println("Error uploading to Dropbox: interrupted during backoff.");
            //System.exit(1);
        }
    }
	
	private static void uploadFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) {
		InputStream in = null;
        try {
        	
        	in = new FileInputStream(localFile);
        	
            FileMetadata metadata = dbxClient.files().uploadBuilder(dropboxPath)
                .withMode(WriteMode.OVERWRITE) // 쓰기모드
                .withClientModified(new Date(localFile.lastModified()))
                .uploadAndFinish(in);

            in.close();
            //System.out.println(metadata.toStringMultiline());
        } catch (UploadErrorException ex) {
            System.err.println("Error uploading to Dropbox: " + ex.getMessage());
            //System.exit(1);
        } catch (DbxException ex) {
            System.err.println("Error uploading to Dropbox: " + ex.getMessage());
            //System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error reading from file \"" + localFile + "\": " + ex.getMessage());
            //System.exit(1);
        } finally {
        	if (in != null)
        		try { in.close(); } catch(Exception e) {e.printStackTrace();}
        }
    }
	
	private void loadProperty(String filePath)
	{
		try
		{
			Properties prop = new Properties();
			prop.load(new FileInputStream(filePath));
			
			driver = prop.getProperty("driver");
			url = prop.getProperty("url");
			user = prop.getProperty("user");
			password = prop.getProperty("password");			
			startdate = prop.getProperty("startdate");
			enddate = prop.getProperty("enddate");
			sqlFilesEncoding = prop.getProperty("sql.files.encoding");
			sqlFilesDir = prop.getProperty("sql.files.dir");
			outputEncoding = prop.getProperty("output.file.encoding");
			outputDir = prop.getProperty("output.dir");
			outputDaily = prop.getProperty("output.daily");
			dropboxUpload = prop.getProperty("dropbox.upload");
			dropboxUploadPath = prop.getProperty("dropbox.upload.path");
			if (dropboxUploadPath != null && dropboxUploadPath.length() > 0)
			{
				dropboxUploadPath = new String(dropboxUploadPath.getBytes("8859_1"), "UTF-8");
			}
			
			dropboxAuthfile = prop.getProperty("dropbox.authfile");
			
			replaceString = prop.getProperty("replace.string");
			
			prop = null;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static HashMap<String, String> getQueryMap(String query)
	{
	    String[] params = query.split("&");
	    HashMap<String, String> map = new HashMap<String, String>();
	    for (String param : params)
	    {
	        String name = param.split("=")[0];
	        String value = param.split("=")[1];
	        map.put(name, value);
	    }
	    return map;
	}
	
	private static void println(String str)
	{
		System.out.println(str);
	}
	
	private static void print(String str)
	{
		System.out.print(str);
	}
	
	class DropboxFile
	{
		public String localPath;
		public String dropboxPath;
	}
}
