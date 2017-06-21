package parkerordermanager;

import com.amazonaws.services.lambda.runtime.Context;

public class ParkerOrderContext {

	public enum RUN_TIME { STAGING, STAGING_TEST, PRODUCTION, LOCAL_STAGING, LOCAL_STAGING_TEST, LOCAL_PRODUCTION }
	
	private RUN_TIME runTime = RUN_TIME.STAGING;
	
	
	private String bucketName     = "zift-services-lambda-test";
	
	// SFTP Info
	private String sftpHostKey    = "30:93:f8:cc:dc:44:27:38:b3:e8:c5:4a:fc:3d:cf:c1";
	private String sftpHostname   = "ziftsolutions.exavault.com";
	private String sftpUsername   = "lp-staging";
	private String sftpPassword   = "parkerlambda123";
	private String folderSrc      = "Leads";
	private String folderDest     = "ZiftLeads";
	private String folderProcessed = "LeadsProcessed";

	// File system root
	private String localTmp       = "/tmp";
	
	// S3
	private String s3AccessKey = "AKIAJLRICS4YQEHUD7SQ";
	private String s3SecretKey = "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";
	private String s3Endpoint  = "s3.amazonaws.com";
	
	private Context context;
	
	public ParkerOrderContext(RUN_TIME rt, Context awsContext) {
		this.context = awsContext;
		this.runTime = rt;
		
		switch(rt) {
			case LOCAL_STAGING_TEST:
				this.setLocalTmp("C:\\Temp");
				this.setFolderSrc("Test-Leads");
				this.setFolderDest("Test-ZiftLeads");
				this.setFolderProcessed("Test-LeadsProcessed");
				this.setSftpUsername("lp-staging");
				this.setSftpPassword("parkerlambda123");
				break;
			case STAGING_TEST:
				this.setLocalTmp("/tmp");
				this.setFolderSrc("Test-Leads");
				this.setFolderDest("Test-ZiftLeads");
				this.setFolderProcessed("Test-LeadsProcessed");
				this.setSftpUsername("lp-staging");
				this.setSftpPassword("parkerlambda123");
				break;
			case LOCAL_STAGING:
				this.setLocalTmp("C:\\Temp");
				this.setSftpUsername("lp-staging");
				this.setSftpPassword("parkerlambda123");
				break;
			case STAGING:
				this.setSftpUsername("lp-staging");
				this.setSftpPassword("parkerlambda123");
				break;
			case LOCAL_PRODUCTION:
				this.setLocalTmp("C:\\Temp");
				this.setSftpUsername("sftp_prod_parkerhannifin");
				this.setSftpPassword("3qKafd6U22");
				this.setBucketName("zift-parker-orders");
				break;
			case PRODUCTION:
				this.setSftpUsername("sftp_prod_parkerhannifin");
				this.setSftpPassword("3qKafd6U22");
				this.setBucketName("zift-parker-orders");
				break;
		}
	}
	
	public boolean isLocal() {
		return !this.localTmp.equals("/tmp");
	}
	public String getBucketName() {
		return bucketName;
	}
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
	public String getSftpHostKey() {
		return sftpHostKey;
	}
	public void setSftpHostKey(String sftpHostKey) {
		this.sftpHostKey = sftpHostKey;
	}
	public String getSftpHostname() {
		return sftpHostname;
	}
	public void setSftpHostname(String sftpHostname) {
		this.sftpHostname = sftpHostname;
	}
	public String getSftpUsername() {
		return sftpUsername;
	}
	public void setSftpUsername(String sftpUsername) {
		this.sftpUsername = sftpUsername;
	}
	public String getSftpPassword() {
		return sftpPassword;
	}
	public void setSftpPassword(String sftpPassword) {
		this.sftpPassword = sftpPassword;
	}
	public String getFolderSrc() {
		return folderSrc;
	}
	public void setFolderSrc(String folderSrc) {
		this.folderSrc = folderSrc;
	}
	public String getFolderDest() {
		return folderDest;
	}
	public void setFolderDest(String folderDest) {
		this.folderDest = folderDest;
	}
	public String getFolderProcessed() {
		return folderProcessed;
	}
	public void setFolderProcessed(String folderProcessed) {
		this.folderProcessed = folderProcessed;
	}
	public String getLocalTmp() {
		return localTmp;
	}
	public void setLocalTmp(String localTmp) {
		this.localTmp = localTmp;
	}

	public Context getAWSContext() {
		return context;
	}

	public void setAWSContext(Context awsContext) {
		this.context = awsContext;
	}

	public String getS3Endpoint() {
		return s3Endpoint;
	}

	public void setS3Endpoint(String s3Endpoint) {
		this.s3Endpoint = s3Endpoint;
	}

	public String getS3SecretKey() {
		return s3SecretKey;
	}

	public void setS3SecretKey(String s3SecretKey) {
		this.s3SecretKey = s3SecretKey;
	}

	public String getS3AccessKey() {
		return s3AccessKey;
	}

	public void setS3AccessKey(String s3AccessKey) {
		this.s3AccessKey = s3AccessKey;
	}

	
}
