package parkerordermanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import parkerordermanager.RowSet.RowSetException;

public class ParkerOrderManager {

	private ParkerOrderContext context;
	// Logging
	private String log;
	
	public ParkerOrderManager(ParkerOrderContext poContext) {
		this.context = poContext;
	}
	
    public void processFilesFromSFTP() {
    	appendLog("Processing Parker Files from " + this.context.getFolderSrc());
    	LambdaLogger logger = this.context.getAWSContext().getLogger();
    	Vector<String> cleanupFiles = new Vector<String>();
    	final SSHClient ssh = new SSHClient();
    	try {
        	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
        	Date now = new Date();

    		logger.log("Connecting to SFTP Host");
    		ssh.addHostKeyVerifier(this.context.getSftpHostKey());
    		ssh.connect(this.context.getSftpHostname());
			
    		ssh.authPassword(this.context.getSftpUsername(), this.context.getSftpPassword());

    		logger.log("Getting SFTP Client");
			final SFTPClient sftp = ssh.newSFTPClient();
			SFTPFileTransfer sftpTransfer = sftp.getFileTransfer();
			sftpTransfer.setPreserveAttributes(false);
			
			// 1) Pull Files from SFTP Server
			
			
			// 2) For each File
			//     a) Pull file from SFTP to local /tmp
			//     b) Create ParkerOrderCSV
			//     c) Upload new Lead CSV into SFTP
			//     d) Move CSV to backup folder if success
			//     e) Clean /tmp
			//     f) Write log entry for each success/failure
			
			
			try {
				List<RemoteResourceInfo> listFiles = sftp.ls(this.context.getFolderSrc());
				appendLog("File Count: " + listFiles.size());
	            for (int fileCount = 0; fileCount < listFiles.size(); fileCount++) {
	            	RemoteResourceInfo info = listFiles.get(fileCount);
	            	appendLog("Processing File: " + info.getName());
	            	
	            	// a) Pull file from SFTP to local /tmp folder
	            	String csvTmpFilename = this.context.getLocalTmp() + "/" + info.getName();
	            	cleanupFiles.add(csvTmpFilename);
	            	logger.log("Pulling from SFTP - " + this.context.getFolderSrc() + "/" + info.getName() + " to " + csvTmpFilename);
	            	sftp.get(this.context.getFolderSrc() + "/" + info.getName(), csvTmpFilename);

	            	// b) Create ParkerOrderCSV	            	
	            	logger.log("Creating ParkerOrderCSV for " + csvTmpFilename);
	            	ParkerOrderCSV csv = new ParkerOrderCSV(csvTmpFilename, context);
	            	
	            	// c) Upload new Lead CSV into SFTP
	            	String csvLeadFilename = "leads" + fileCount + "-" + dateFormat.format(now) + ".csv";

	            	try {
		            	logger.log("Writing Lead CSV File to " + this.context.getLocalTmp() + "/" + csvLeadFilename);	            	
						csv.writeLeadCSV(this.context.getLocalTmp() + "/" + csvLeadFilename);
						cleanupFiles.add(this.context.getLocalTmp() + "/" + csvLeadFilename);
						appendLog("Leads processed: " + csv.getLeadCount());

		            	logger.log("Pushing Lead CSV File to " + this.context.getFolderDest() + "/" + csvLeadFilename);	            	
						sftp.put(this.context.getLocalTmp() + "/" + csvLeadFilename, this.context.getFolderDest() + "/" + csvLeadFilename);

						// d) Move original CSV into backup folder
		            	logger.log("Pushing Original CSV File to " + this.context.getFolderProcessed() + "/" + info.getName());	            	
						sftpTransfer.upload(csvTmpFilename, this.context.getFolderProcessed() + "/" + info.getName());						
		            	logger.log("Removing Original CSV File from " + this.context.getFolderSrc() + "/" + info.getName());	            	
		            	sftp.rm(this.context.getFolderSrc() + "/" + info.getName());
						
					} catch (RowSetException e) {
						logger.log("Exception writing lead CSV File: " + e.getMessage());
						appendLog("Exception writing lead CSV File: " + e.getMessage());
					}
	            	
					for (int i = 0 ; i < csv.getS3ProductCSVs().size(); i++) {
						cleanupFiles.add(this.context.getLocalTmp() + "/" + csv.getS3ProductCSVs().get(i));						
					}
	            	

	            }
	            
				// e) Write Log file
				appendLog("Creating log file and doing temp folder cleanup");
            	String logFilename = "log-" + dateFormat.format(now) + ".txt";
            	File logFile = new  File(this.context.getLocalTmp() + "/" + logFilename);
          
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                FileWriter fw = new FileWriter(logFile.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(this.log);
                bw.close();	            	

                logger.log("Uploading log file from " + this.context.getLocalTmp() + "/" + logFilename + " to " + this.context.getFolderProcessed() + "/" + logFilename + " \n");
				sftpTransfer.upload(this.context.getLocalTmp() + "/" + logFilename, this.context.getFolderProcessed() + "/" + logFilename);
				cleanupFiles.add(this.context.getLocalTmp() + "/" + logFilename);


				
			} finally {
				sftp.close();
			}

			ssh.disconnect();
			
			
		} catch (IOException e) {
			logger.log("IOException occured: " + e.getMessage());
			appendLog("IOException occured: " + e.getMessage());
			e.printStackTrace();
		} 
    	
    	// f) Clean up /tmp directory
		try {
			for (int i = 0; i < cleanupFiles.size(); i++)
				Files.deleteIfExists(Paths.get(cleanupFiles.get(i)));
			
		} catch (IOException e) {
			logger.log("IOException deleting files " + e.getMessage() + "\n");
			appendLog("IOException deleting files " + e.getMessage());
			e.printStackTrace();
		}
    	
    }


	public String getLog() {
		return this.log;
	}
	
	private String appendLog(String text) {
    	if (log == null) log = "";
    	log += text + "\r\n";
    	return log;
    }
    
	
}
