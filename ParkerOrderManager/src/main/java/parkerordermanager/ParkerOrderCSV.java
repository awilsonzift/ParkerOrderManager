package parkerordermanager;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import parkerordermanager.RowSet.RowSetException;

public class ParkerOrderCSV {
	
	private String csvInFilename;
	private LambdaLogger logger;
	private ParkerOrderContext context;
	
	private boolean fileExists = false;
	private String[] headerRowSource = null;
	private HashMap<String, Vector<RowSet>> sourceRowMap = null;
	private Vector<String> productCSVs = null;
	private Vector<RowSet> leadRows = null;

	private static String TOTAL_ITEMS = "TotalItems";
	private static String ITEM_LIST_URL = "ItemListUrl";
	
	private static String[] headerRowSourceProductFields = {
		"Item", "Quantity", "Price", "CurrencyCode", "Comments"
	};
	private static String[] headerRowExport = {
		"Email", "FirstName", "LastName", "leadName", "WorkPhone", "State", "Country", "PostalCode",
		"AddressLine1", "AddressLine2", "AddressLine3", "Notes",
		"PartnerForRouting", "AccountNumber", "SourceCampaign", "IntegrationId",
		TOTAL_ITEMS, ITEM_LIST_URL, 		
		"Item", "Quantity", "Price", "CurrencyCode", "Comments", "RevnItemNumber", "RevnStatCode",   
		"Item2", "Quantity2", "Price2", "CurrencyCode2", "Comments2", "RevnItemNumber2", "RevnStatCode2", 
		"Item3", "Quantity3", "Price3", "CurrencyCode3", "Comments3", "RevnItemNumber3", "RevnStatCode3", 
		"Item4", "Quantity4", "Price4", "CurrencyCode4", "Comments4", "RevnItemNumber4", "RevnStatCode4", 
		"Item5", "Quantity5", "Price5",	"CurrencyCode5", "Comments5", "RevnItemNumber5", "RevnStatCode5"
	};
	


	public ParkerOrderCSV(String filename, ParkerOrderContext poContext) throws FileNotFoundException {
		this.csvInFilename = filename; 
		this.context = poContext;
		this.logger = poContext.getAWSContext().getLogger();
		File testFile = new File(filename);
		this.fileExists = testFile.exists();
		if (!this.fileExists) throw new FileNotFoundException();
	}
	
	public HashMap<String, Vector<RowSet>> getSourceRowMap() throws IOException, RowSetException {
		if (this.sourceRowMap == null) {
			initializeRowMap();
		}
		return this.sourceRowMap;
	}
	
	public String[] getSourceHeaderRow() throws IOException, RowSetException {
		if (this.headerRowSource == null) {
			initializeRowMap();
		}
		return this.headerRowSource;
	}
	
	public Vector<String> getS3ProductCSVs() {
		return this.productCSVs;
	}
	
	public String[] getExportHeaderRow() {
		return ParkerOrderCSV.headerRowExport;
	}
	
	public int getLeadCount() {
		if (this.leadRows == null)
			return 0;
		return this.leadRows.size();
	}
	
	public void writeLeadCSV(String filename) throws IOException, RowSetException {
		if (this.sourceRowMap == null || this.headerRowSource == null) {
			initializeRowMap();
		}
		
		Set emails = this.sourceRowMap.keySet();		
		Iterator<String> it = emails.iterator();
		if (this.leadRows == null)
			this.leadRows = new Vector<RowSet>();
		
		while (it.hasNext()) {
			String email = it.next();
			Vector<RowSet> emailRows = this.sourceRowMap.get(email);
			this.leadRows.add(getExportRowForCSV(emailRows));			
		}
		
		writeCSV(filename, leadRows);
		
	}
	
	private void initializeRowMap() throws IOException, RowSetException {
		if (!fileExists) {
			throw new FileNotFoundException();
		}
		
    	CSVReader reader = new CSVReader(new FileReader(this.csvInFilename));
    	
    	List csvRows = reader.readAll();    	
    	for (int i = 0; i < csvRows.size(); i++) {
    		String[] csvRow = (String[]) csvRows.get(i);
    		if (i == 0) {
    			this.headerRowSource = csvRow;
    		}
    		else {
    			addSourceRowToMap(csvRow);
    		}
    	}    	
		
	}
	
	private String getEmailFromRow(String[] row) {
		String email = null;
		String partner = null;
		for (int i = 0; i < row.length && this.headerRowSource != null; i++) {
			if (this.headerRowSource[i].toLowerCase().contains("email"))
				email = row[i];
			if (this.headerRowSource[i].toLowerCase().contains("partner"))
				partner = row[i];
		}	
		return partner + ":" + email;
	}
	
	private void addSourceRowToMap(String[] row) throws IOException, RowSetException {
		if (this.sourceRowMap == null) {
			this.sourceRowMap = new HashMap<String, Vector<RowSet>>();
		}
		String partner2email = getEmailFromRow(row);
		if (partner2email == null) throw new IOException();
		
		RowSet orderRow = new RowSet(this.getSourceHeaderRow());		
		orderRow.addRow(row);
		
		if (!this.sourceRowMap.containsKey(partner2email)) {
			this.sourceRowMap.put(partner2email, new Vector<RowSet>());
		}
		
		this.sourceRowMap.get(partner2email).add(orderRow);
	}
	
	private RowSet getExportRowForCSV(Vector<RowSet> orderRows) throws RowSetException {
		if (orderRows == null || orderRows.isEmpty()) {
			return null;
		}

		RowSet exportRowSet = new RowSet(this.getExportHeaderRow());

		// Loads common lead fields and Products (0) value
		exportRowSet.addMatchingElements(orderRows.get(0));
		
		// Load Vector of Product Info Set from each originating lead
		Vector<RowSet> exportProductRows = new Vector<RowSet>();
		for (int i = 0; i < orderRows.size(); i++) {
			RowSet productRowSet = new RowSet(ParkerOrderCSV.headerRowSourceProductFields);
			productRowSet.addMatchingElements(orderRows.get(i));			
			exportProductRows.add(productRowSet);
			
			// Add Product data to the export Row up to 5 products
			if (i > 0 && i < 5)
				exportRowSet.addMatchingElements(productRowSet, Integer.toString(i+1));
		}
		
		// If fewer than 5 products, add blank columns
		String[] blanks = this.getBlankStringArray(ParkerOrderCSV.headerRowSourceProductFields.length);
		for (int i = exportProductRows.size(); i < 5; i++) {
			RowSet blankProductRowSet = new RowSet(ParkerOrderCSV.headerRowSourceProductFields);			
			blankProductRowSet.addRow(blanks);
			exportRowSet.addMatchingElements(blankProductRowSet, Integer.toString(i));
		}
		
		// Set total product field
		exportRowSet.addRowElement(TOTAL_ITEMS, Integer.toString(exportProductRows.size()));
		
		// Upload product CSV file to S3
		String productCSVFilename = createS3ProductCSV(exportProductRows);
		String s3Url = "https://s3.amazonaws.com/" + this.context.getBucketName() + "/" + productCSVFilename;

		// Add URL to Export CSV
		exportRowSet.addRowElement(ITEM_LIST_URL, s3Url);
				
		return exportRowSet;
	}
	
	private String createS3ProductCSV(Vector<RowSet> productRows) {
		if (this.productCSVs == null) {
			this.productCSVs = new Vector<String>();
		}
		
		String prodCSVFileName = this.getNewCSVFilename();
		try {
			writeCSV(this.context.getLocalTmp() + "/" + prodCSVFileName, productRows);
			
			File csv = new File(this.context.getLocalTmp() + "/" + prodCSVFileName);
			uploadObject(csv);
			
			this.productCSVs.add(prodCSVFileName);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return prodCSVFileName;
	}
	
	private String getNewCSVFilename() {
    	Date now = new Date();
    	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
    	return "order" + this.getS3ProductCSVs().size() + "-" + dateFormat.format(now) + ".csv";
	}
	
	private void writeCSV(String filename, Vector<RowSet> rows) throws IOException {

		CSVWriter writer = new CSVWriter(new FileWriter(filename));
		
		writer.writeNext(rows.get(0).getHeaderArray());
		
		for (int i = 0; i < rows.size(); i++) {
			RowSet row = rows.get(i);
			writer.writeNext(row.getElementArray());
		}
		
		writer.close();			
		
	}
	
    private void uploadObject(File file) {

    	AmazonS3 s3client;
    	if (context.isLocal()) {
        	BasicAWSCredentials awsCreds = new BasicAWSCredentials(this.context.getS3AccessKey(), this.context.getS3SecretKey());
        	s3client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();  
    	} else {
        	s3client = AmazonS3ClientBuilder.defaultClient();    		
    	}
    	
        try {        	
            PutObjectRequest requestObject = new PutObjectRequest(this.context.getBucketName(), file.getName(), file); 
            
            s3client.putObject(requestObject);

         } catch (AmazonServiceException ase) {
        	logger.log("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.\n");
        	logger.log("Error Message:    " + ase.getMessage() + "\n");
        	logger.log("HTTP Status Code: " + ase.getStatusCode() + "\n");
        	logger.log("AWS Error Code:   " + ase.getErrorCode() + "\n");
        	logger.log("Error Type:       " + ase.getErrorType() + "\n");
        	logger.log("Request ID:       " + ase.getRequestId() + "\n");
        } catch (AmazonClientException ace) {
        	logger.log("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network. \n");
        	logger.log("Error Message: " + ace.getMessage() + "\n");
        } 
        
    }
	
    private String[] getBlankStringArray(int length) {
    	String[] blanks = new String[length];
    	for (int i = 0; i < length; i++) blanks[i] = "";
    	return blanks;
    }

}
