package parkerordermanager;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class RowSet extends TreeSet<RowSet.RowElement> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String[] rowHeader;
	private List<String> headerList = null;
	
	public RowSet(String[] headerArray) {
		super(new ElementComparator(headerArray));
		this.rowHeader = headerArray;
		this.headerList = getListHeaderForMatching(headerArray);
	}
	
	public void addRowElement(String fieldName, String fieldValue) {
		if (this.headerList.contains(cleanHeaderForMatch(fieldName))) {
			RowElement newElem = new RowElement(fieldName, fieldValue);
			this.add(newElem);			
		}
	}
	
	public void addRow(String[] row) throws RowSetException {
		if (row.length != this.getHeaderArray().length) {
			String message = "Error in addRow - RowSet header length (" + this.getHeaderArray().length + ") <> row length (" + row.length + ")";
			throw new RowSetException(message);
		}
		for (int i = 0; i < row.length; i++) {
			addRowElement(this.getHeaderArray()[i], row[i]);
		}
	}
	
	public void addMatchingElements(RowSet newElements) {
		this.addMatchingElements(newElements, "");
	}
	public void addMatchingElements(RowSet newElements, String suffix) {
		Iterator<RowElement> it = newElements.iterator();
		while (it.hasNext()) {
			RowElement elem = it.next();
			String fieldNameSuffix = elem.getFieldName() + suffix;
			if (this.headerList.contains(cleanHeaderForMatch(fieldNameSuffix))) {
				this.addRowElement(fieldNameSuffix, elem.getFieldValue());
			}
		}
	}
	
	public String[] getHeaderArray() {
		return this.rowHeader;
	}
	
	public String[] getElementArray() {
		addBlankElements();
		String[] elementArray = new String[this.size()];
		Iterator<RowElement> it = this.iterator();
		
		int count = 0;
		while (it.hasNext()) {
			elementArray[count++] = it.next().getFieldValue();
		}
		
		return elementArray;
	}
	
	public static String cleanHeaderForMatch(String header) {
		return header.toLowerCase().replace(" ", "");
	}
	
	private void addBlankElements() {
		// Assume if the set has same number of elements as header, no need to add blanks
		if (this.getHeaderArray().length > this.size()){
			for (int i = 0; i < this.getHeaderArray().length; i++) {
				RowElement elem = new RowElement(this.getHeaderArray()[i],"");
				if (!this.contains(elem)) {
					this.add(elem);
				}				
			}
		}
	}
	
	private static List<String> getListHeaderForMatching(String[] arrHeader) {
		String[] matchHeaders = new String[arrHeader.length];
		for (int i = 0; i < arrHeader.length; i++) {
			matchHeaders[i] = cleanHeaderForMatch(arrHeader[i]);
		}
		return Arrays.asList(matchHeaders);
	}

	class RowElement implements Comparable<RowSet.RowElement> {
		private String sField;
		private String sValue;
		
		public RowElement(String fieldName, String fieldValue) {
			this.sField = fieldName;
			this.sValue = fieldValue;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((sField == null) ? 0 : sField.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RowElement other = (RowElement) obj;
			if (sField == null) {
				if (other.sField != null)
					return false;
			} else if (!sField.equals(other.sField))
				return false;	
			return true;
		}

		public String getFieldName() {
			return this.sField;
		}
		
		public String getFieldValue() {
			return this.sValue;
		}

		public int compareTo(RowElement o) {
			return this.getFieldName().compareTo(o.getFieldName());
		}

		private RowSet getOuterType() {
			return RowSet.this;
		}
	}
	
	static class ElementComparator implements Comparator<RowElement> {
		
		private String[] header;
		
		public ElementComparator(String[] headerRow) {
			this.header = headerRow;
		}

		public int compare(RowElement elem1, RowElement elem2) {
			Integer iFirst = new Integer(0);
			Integer iSecond = new Integer(0);
			for (int i = 0; i < this.header.length; i++) {
				if (RowSet.cleanHeaderForMatch(elem1.getFieldName()).equals(RowSet.cleanHeaderForMatch(this.header[i]))) {
					iFirst = new Integer(i);
				}
				if (RowSet.cleanHeaderForMatch(elem2.getFieldName()).equals(RowSet.cleanHeaderForMatch(this.header[i]))) {
					iSecond = new Integer(i);
				}
			}
			return iFirst.compareTo(iSecond);
		}
		
	}
	
	class RowSetException extends Exception {
	
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public RowSetException() {
			
		}
		public RowSetException(String message) {
			super(message);
		}
		public RowSetException(Throwable cause) {
			super(cause);
		}
		public RowSetException(String message, Throwable cause) {
			super(message, cause);
		}
		public RowSetException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}		
	}
}
