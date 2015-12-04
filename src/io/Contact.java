/*
 * Funambol is a mobile platform developed by Funambol, Inc. 
 * Copyright (C) 2003 - 2007 Funambol, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission 
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY FUNAMBOL, FUNAMBOL DISCLAIMS THE 
 * WARRANTY OF NON INFRINGEMENT  OF THIRD PARTY RIGHTS.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 * 
 * You can contact Funambol, Inc. headquarters at 643 Bair Island Road, Suite 
 * 305, Redwood City, CA 94063, USA, or at email address info@funambol.com.
 * 
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * "Powered by Funambol" logo. If the display of the logo is not reasonably 
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by Funambol". 
 * 
 * 2009-02-25 ducktayp: Modified to parse and format more Vcard fields, including PHOTO.
 * 						No attempt was made to preserve compatibility with previous code.
 *    					
 */

package io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethodsColumns;

import util.Log;
import util.QuotedPrintable;
import util.StringUtil;

/**
 * A Contact item
 */
public class Contact {
    static final String NL = "\r\n";
    
    
    // Property name for Instant-message addresses
    static final String IMPROP = "X-IM-NICK";

    // Property parameter name for custom labels
    static final String LABEL_PARAM = "LABEL";
    
    // Property parameter for IM protocol
    static final String PROTO_PARAM = "PROTO";

    // Protocol labels
    static final String[] PROTO = {
    	"AIM",		// ContactMethods.PROTOCOL_AIM = 0
    	"MSN",		// ContactMethods.PROTOCOL_MSN = 1
    	"YAHOO",	// ContactMethods.PROTOCOL_YAHOO = 2
    	"SKYPE",	// ContactMethods.PROTOCOL_SKYPE = 3
    	"QQ",		// ContactMethods.PROTOCOL_QQ = 4
    	"GTALK",	// ContactMethods.PROTOCOL_GOOGLE_TALK = 5
    	"ICQ",		// ContactMethods.PROTOCOL_ICQ = 6
    	"JABBER"	// ContactMethods.PROTOCOL_JABBER = 7
    };
    
    long parseLen;
    
    static final String BIRTHDAY_FIELD = "Birthday:";
    
    /**
     * Contact fields declaration
     */
    // Contact identifier
    String _id;
    
    String syncid;
    
    // Contact displayed name
    String displayName;
    // Contact first name
    String firstName;
    // Contact last name
    String lastName;
    
    static class RowData {
    	RowData(int type, String data, boolean preferred, String customLabel) {
    		this.type = type;
    		this.data = data;
    		this.preferred = preferred;
    		this.customLabel = customLabel;
    		auxData = null;
    	}
    	RowData(int type, String data, boolean preferred) {
    		this(type, data, preferred, null);
    	}

    	int type;
    	String data;
    	boolean preferred;    	
    	String customLabel;
    	String auxData;
    }
    
    static class OrgData {
    	OrgData(int type, String title, String company, String customLabel) {
    		this.type = type;
    		this.title = title;
    		this.company = company;
    		this.customLabel = customLabel;
    	}
    	int type;
    	
        // Contact title
        String title;
        // Contact company name
    	String company;
    	
    	String customLabel;
    }
    
    // Phones dictionary; keys are android Contact Column ids
    List<RowData> phones;
    
    // Emails dictionary; keys are android Contact Column ids
    List<RowData> emails;
    
    // Address dictionary; keys are android Contact Column ids
    List<RowData> addrs;

    // Instant message addr dictionary; keys are android Contact Column ids
    List<RowData> ims;

    // Organizations list
    List<OrgData> orgs;
    
    // Compressed photo
    byte[] photo;
    
    // Contact note
    String notes;

    // Contact's birthday
    String birthday;
    
	Hashtable<String, handleProp> propHandlers;
	
	interface handleProp {
		void parseProp(final String propName, final Vector<String> propVec, final String val);
	}
	
	// Initializer block
	{
		reset();
		 propHandlers = new Hashtable<String, handleProp>();

		 handleProp simpleValue = new handleProp() {
			 public void parseProp(final String propName, final Vector<String> propVec, final String val) {
				 if (propName.equals("FN")) {
					 displayName = val;
				 } else if (propName.equals("NOTE")) {
					 notes = val;
				 } else if (propName.equals("BDAY")) {
					 birthday = val;
				 } else if (propName.equals("X-IRMC-LUID") || propName.equals("UID")) {
					 syncid = val;
				 } else if (propName.equals("N")) {
					 String[] names = StringUtil.split(val, ";");
					 // We set only the first given name.
					 // The others are ignored in input and will not be
					 // overridden on the server in output.
					 if (names.length >= 2) {
						 firstName = names[1];
						 lastName = names[0];
					 } else {
						 String[] names2 = StringUtil.split(names[0], " ");
						 firstName = names2[0];
						 if (names2.length > 1)
							 lastName = names2[1];
					 }
				 } 
			 }
		 };

		 propHandlers.put("FN", simpleValue);
		 propHandlers.put("NOTE", simpleValue);
		 propHandlers.put("BDAY", simpleValue);
		 propHandlers.put("X-IRMC-LUID", simpleValue);
		 propHandlers.put("UID", simpleValue);
		 propHandlers.put("N", simpleValue);

		 handleProp orgHandler = new handleProp() {

			@Override
			public void parseProp(String propName, Vector<String> propVec,
					String val) {
				String label = null;
				for (String prop : propVec) {
					String[] propFields = StringUtil.split(prop, "=");
					if (propFields[0].equalsIgnoreCase(LABEL_PARAM) && propFields.length > 1) {
						label = propFields[1];
					}
				}
				if (propName.equals("TITLE")) {
					boolean setTitle = false;
					for (OrgData org : orgs) {
						if (label == null && org.customLabel != null)
							continue;
						if (label != null && !label.equals(org.customLabel))
							continue;
						
						if (org.title == null) {
							org.title = val;
							setTitle = true;
							break;
						}
					}
					if (!setTitle) {
						orgs.add(new OrgData(label == null ? ContactMethodsColumns.TYPE_WORK : ContactMethodsColumns.TYPE_CUSTOM,
								val, null, label));
					}
				} else if (propName.equals("ORG")) {
					String[] orgFields = StringUtil.split(val, ";");
					boolean setCompany = false;
					for (OrgData org : orgs) {
						if (label == null && org.customLabel != null)
							continue;
						if (label != null && !label.equals(org.customLabel))
							continue;

						if (org.company == null) {
							org.company = val;
							setCompany = true;
							break;
						}
					}
					if (!setCompany) {
						orgs.add(new OrgData(label == null ? ContactMethodsColumns.TYPE_WORK : ContactMethodsColumns.TYPE_CUSTOM,
								null, orgFields[0], label));
					}
				 }
			}
		 };
		 

		 propHandlers.put("ORG", orgHandler);
		 propHandlers.put("TITLE", orgHandler);
		 
		 propHandlers.put("TEL", new handleProp() {
			 public void parseProp(final String propName, final Vector<String> propVec, final String val) {
				 String label = null;
				 int subtype = Contacts.PhonesColumns.TYPE_OTHER;
				 boolean preferred = false;
				 for (String prop : propVec) {
					 if (prop.equalsIgnoreCase("HOME") || prop.equalsIgnoreCase("VOICE")) {
						 if (subtype != Contacts.PhonesColumns.TYPE_FAX_HOME)
							 subtype = Contacts.PhonesColumns.TYPE_HOME;
					 } else if (prop.equalsIgnoreCase("WORK")) {
						 if (subtype == Contacts.PhonesColumns.TYPE_FAX_HOME) {
							 subtype = Contacts.PhonesColumns.TYPE_FAX_WORK;
						 } else
						 	subtype = Contacts.PhonesColumns.TYPE_WORK;
					 } else if (prop.equalsIgnoreCase("CELL")) {
						 subtype = Contacts.PhonesColumns.TYPE_MOBILE;
					 } else if (prop.equalsIgnoreCase("FAX")) {
						 if (subtype == Contacts.PhonesColumns.TYPE_WORK) {
							 subtype = Contacts.PhonesColumns.TYPE_FAX_WORK;
						 } else
							 subtype = Contacts.PhonesColumns.TYPE_FAX_HOME;
					 } else if (prop.equalsIgnoreCase("PAGER")) {
						 subtype = Contacts.PhonesColumns.TYPE_PAGER;
					 } else if (prop.equalsIgnoreCase("PREF")) {
						 preferred = true;
					 } else {
						 String[] propFields = StringUtil.split(prop, "=");
						 
						 if (propFields.length > 1 && propFields[0].equalsIgnoreCase(LABEL_PARAM)) {
							 label = propFields[1];
							 subtype = ContactMethodsColumns.TYPE_CUSTOM;
						 }
					 }
				 }
				 phones.add(new RowData(subtype, toCanonicalPhone(val), preferred, label));
			 }
		 });
		 

		 propHandlers.put("ADR", new handleProp() {
			 public void parseProp(final String propName, final Vector<String> propVec, final String val) {
				 boolean preferred = false;
				 String label = null;
				 int subtype = ContactMethodsColumns.TYPE_WORK; // vCard spec says default is WORK
				 for (String prop : propVec) {
					 if (prop.equalsIgnoreCase("WORK")) {
						 subtype = ContactMethodsColumns.TYPE_WORK;
					 } else if (prop.equalsIgnoreCase("HOME")) {
						 subtype = ContactMethodsColumns.TYPE_HOME;
					 } else if (prop.equalsIgnoreCase("PREF")) {
						 preferred = true;
					 } else {
						 String[] propFields = StringUtil.split(prop, "=");
						 
						 if (propFields.length > 1 && propFields[0].equalsIgnoreCase(LABEL_PARAM)) {
							 label = propFields[1];
							 subtype = ContactMethodsColumns.TYPE_CUSTOM;
						 }
					 }
				 }
	             String[] addressFields = StringUtil.split(val, ";");
	             StringBuffer addressBuf = new StringBuffer(val.length());
	             if (addressFields.length > 2) {
	            	 addressBuf.append(addressFields[2]);
	            	 int maxLen = Math.min(7, addressFields.length);
		             for (int i = 3; i < maxLen; ++i) {
		            	 addressBuf.append(", ").append(addressFields[i]);
		             }
	             }
	             String address = addressBuf.toString();
	             addrs.add(new RowData(subtype, address, preferred, label));
			 }
		 });
		 

		 propHandlers.put("EMAIL", new handleProp() {
			 public void parseProp(final String propName, final Vector<String> propVec, final String val) {
				 boolean preferred = false;
				 String label = null;
				 int subtype = ContactMethodsColumns.TYPE_HOME;
				 for (String prop : propVec) {
					 if (prop.equalsIgnoreCase("PREF")) {
						 preferred = true;
					 } else if (prop.equalsIgnoreCase("WORK")) {
						 subtype = ContactMethodsColumns.TYPE_WORK;
					 } else {
						 String[] propFields = StringUtil.split(prop, "=");
						 
						 if (propFields.length > 1 && propFields[0].equalsIgnoreCase(LABEL_PARAM)) {
							 label = propFields[1];
							 subtype = ContactMethodsColumns.TYPE_CUSTOM;
						 }
					 } 
				 }
				 emails.add(new RowData(subtype, val, preferred, label));
			 }
		 });
		 
		 propHandlers.put(IMPROP, new handleProp() {
			 public void parseProp(final String propName, final Vector<String> propVec, final String val) {
				 boolean preferred = false;
				 String label = null;
				 String proto = null;
				 int subtype = ContactMethodsColumns.TYPE_HOME;
				 for (String prop : propVec) {
					 if (prop.equalsIgnoreCase("PREF")) {
						 preferred = true;
					 } else if (prop.equalsIgnoreCase("WORK")) {
						 subtype = ContactMethodsColumns.TYPE_WORK;
					 } else {
						 String[] propFields = StringUtil.split(prop, "=");
						 
						 if (propFields.length > 1) {
							 if (propFields[0].equalsIgnoreCase(PROTO_PARAM)) {
								 proto = propFields[1];
							 } else if (propFields[0].equalsIgnoreCase(LABEL_PARAM)) {
								 label = propFields[1];
							 }
						 }
					 } 
				 }
				 RowData newRow = new RowData(subtype, val, preferred, label); 
				 newRow.auxData = proto;
				 ims.add(newRow);
			 }
		 });
		 
		 propHandlers.put("PHOTO", new handleProp() {
			 public void parseProp(final String propName, final Vector<String> propVec, final String val) {
				 boolean isUrl = false;
				 photo = new byte[val.length()];
				 for (int i = 0; i < photo.length; ++i)
				 	photo[i] = (byte) val.charAt(i);
				 for (String prop : propVec) {
					 if (prop.equalsIgnoreCase("VALUE=URL")) {
						 isUrl = true;
					 }
				 }
				 if (isUrl) {
					 // TODO: Deal with photo URLS
				 }
			 }
		 });
		 
	}
	
	private void reset() {
		 _id = null;
		 syncid = null;
		 parseLen = 0;
		 displayName = null;
		 notes = null;
		 birthday = null;
		 photo = null;
		 firstName = null;
		 lastName = null;
		 if (phones == null) phones = new ArrayList<RowData>();
		 else phones.clear();
		 if (emails == null) emails = new ArrayList<RowData>();
		 else emails.clear();
		 if (addrs == null) addrs = new ArrayList<RowData>();
		 else addrs.clear();
		 if (orgs == null) orgs = new ArrayList<OrgData>();
		 else orgs.clear();
		 if (ims == null) ims = new ArrayList<RowData>();
		 else ims.clear();
	}

	SQLiteStatement querySyncId;
	SQLiteStatement queryPersonId;
	SQLiteStatement insertSyncId;
	
    // Constructors------------------------------------------------
    public Contact(SQLiteStatement querySyncId, SQLiteStatement queryPersionId, SQLiteStatement insertSyncId) {
    	this.querySyncId = querySyncId;
    	this.queryPersonId = queryPersionId;
    	this.insertSyncId = insertSyncId;
    }

    public Contact(String vcard, SQLiteStatement querySyncId, SQLiteStatement queryPersionId, SQLiteStatement insertSyncId) {
    	this(querySyncId, queryPersionId, insertSyncId);
    	BufferedReader vcardReader = new BufferedReader(new StringReader(vcard)); 
        try {
			parseVCard(vcardReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    public Contact(BufferedReader vcfReader, SQLiteStatement querySyncId, SQLiteStatement queryPersionId, SQLiteStatement insertSyncId) throws IOException { 
    	this(querySyncId, queryPersionId, insertSyncId);
    	parseVCard(vcfReader);
    }
    
    public Contact(Cursor peopleCur, ContentResolver cResolver, 
    		SQLiteStatement querySyncId, SQLiteStatement queryPersionId, SQLiteStatement insertSyncId) {
    	this(querySyncId, queryPersionId, insertSyncId);
        populate(peopleCur, cResolver);
    }

    
    final static Pattern[] phonePatterns = {
			Pattern.compile("[+](1)(\\d\\d\\d)(\\d\\d\\d)(\\d\\d\\d\\d.*)"),
			Pattern.compile("[+](972)(2|3|4|8|9|50|52|54|57|59|77)(\\d\\d\\d)(\\d\\d\\d\\d.*)"),
	};
    
    
    /**
     * Change the phone to canonical format (with dashes, etc.) if it's in a supported country.
     * @param phone
     * @return
     */
	String toCanonicalPhone(String phone) {
		for (final Pattern phonePattern : phonePatterns) {
			Matcher m = phonePattern.matcher(phone);
			if (m.matches()) {
				return "+" + m.group(1) + "-" + m.group(2) + "-" + m.group(3) + "-" + m.group(4);
			}
		}

		return phone;
	}

    
    /**
     * Set the person identifier
     */
    public void setId(String id) {
        _id = id;
    }

    /**
     * Get the person identifier
     */
    public long getId() {
        return Long.parseLong(_id);
    }

	final static Pattern beginPattern = Pattern.compile("BEGIN:VCARD",Pattern.CASE_INSENSITIVE);
	final static Pattern propPattern = Pattern.compile("([^:]+):(.*)");
	final static Pattern propParamPattern = Pattern.compile("([^;=]+)(=([^;]+))?(;|$)");
	final static Pattern base64Pattern = Pattern.compile("\\s*([a-zA-Z0-9+/]+={0,2})\\s*$");
    final static Pattern namePattern = Pattern.compile("(([^,]+),(.*))|((.*?)\\s+(\\S+))");
    
	// Parse birthday in notes
	final static Pattern birthdayPattern = Pattern.compile("^" + BIRTHDAY_FIELD + ":\\s*([^;]+)(;\\s*|\\s*$)",Pattern.CASE_INSENSITIVE);
	   
    /**
     * Parse the vCard string into the contacts fields
     */
    public long parseVCard(BufferedReader vCard) throws IOException {
    	// Reset the currently read values.
    	reset();
    	
    	// Find Begin.
    	String line = vCard.readLine();
    	if (line != null)
    		parseLen += line.length();
    	else
    		return -1;
    	
    	while (line != null && !beginPattern.matcher(line).matches()) {
    		line = vCard.readLine();
    		parseLen += line.length();
    	}
    	
    	if (line == null)
    		return -1;
    	
    	boolean skipRead = false;
    	
    	while (line != null) {
    		if (!skipRead)
    			line = vCard.readLine();
    		
    		if (line == null) {
    			return 0;
    		}
    		
    		skipRead = false;
    	
        	// do multi-line unfolding (cr lf with whitespace immediately following is removed, joining the two lines).  
        	vCard.mark(1);
        	for (int ch = vCard.read(); ch == (int) ' ' || ch == (int) '\t'; ch = vCard.read()) {
        		vCard.reset();
        		String newLine = vCard.readLine();
        		if (newLine != null)
        			line += newLine;
        		vCard.mark(1);
        	}
        	vCard.reset();
    		
    		parseLen += line.length(); // TODO: doesn't include CR LFs
    		
    		Matcher pm = propPattern.matcher(line);
    		
    		if (pm.matches()) {
    			String prop = pm.group(1);
    			String val = pm.group(2);

    			if (prop.equalsIgnoreCase("END") && val.equalsIgnoreCase("VCARD")) {
    				// End of vCard
    				return parseLen;
    			}
 
    			Matcher ppm = propParamPattern.matcher(prop);
    			if (!ppm.find())
    				// Doesn't seem to be a valid vCard property
    				continue;
    			
    			String propName = ppm.group(1).toUpperCase();
    			Vector<String> propVec = new Vector<String>();
    			String charSet = "UTF-8";
    			String encoding = "";
    			while (ppm.find()) {
    				String param = ppm.group(1);
    				String paramVal = ppm.group(3);
    				propVec.add(param + (paramVal != null ? "=" + paramVal : ""));
    				if (param.equalsIgnoreCase("CHARSET"))
    					charSet = paramVal;
    				else if (param.equalsIgnoreCase("ENCODING"))
    					encoding = paramVal;
    			}
    			if (encoding.equalsIgnoreCase("QUOTED-PRINTABLE")) {
    				try {
    					val = QuotedPrintable.decode(val.getBytes(charSet), "UTF-8");
    				} catch (UnsupportedEncodingException uee) {
    					
    				}
    			} else if (encoding.equalsIgnoreCase("BASE64")) {
    				StringBuffer tmpVal = new StringBuffer(val);
    				do {
    					line = vCard.readLine();
     			
    					if ((line == null) || (line.length() == 0) || (!base64Pattern.matcher(line).matches())) {
    						//skipRead = true;
    						break;
    					}
   						tmpVal.append(line);
    				} while (true);
    				
    				Base64Coder.decodeInPlace(tmpVal);
    				val = tmpVal.toString();
    			}
    			handleProp propHandler = propHandlers.get(propName);
    			if (propHandler != null)
    				propHandler.parseProp(propName, propVec, val);
    		}
    	}
    	return 0;
    }

    public long getParseLen() {
    	return parseLen;
    }
    
    /**
     * Format an email as a vCard field.
     *  
     * @param cardBuff Formatted email will be appended to this buffer
     * @param email The rowdata containing the actual email data.
     */
    public static void formatEmail(Appendable cardBuff, RowData email) throws IOException {
    	cardBuff.append("EMAIL;INTERNET");
    	if (email.preferred)
    		cardBuff.append(";PREF");

    	if (email.customLabel != null) {
    		cardBuff.append(";" + LABEL_PARAM + "=");
    		cardBuff.append(email.customLabel);
    	}
    	switch (email.type) {
    	case ContactMethodsColumns.TYPE_WORK:
    		cardBuff.append(";WORK");
    		break;
    	}
    	
    	if (!StringUtil.isASCII(email.data))
    		cardBuff.append(";CHARSET=UTF-8");

    	cardBuff.append(":").append(email.data.trim()).append(NL);
    }

    /**
     * Format a phone as a vCard field.
     *  
     * @param formatted Formatted phone will be appended to this buffer
     * @param phone The rowdata containing the actual phone data.
     */
    public static void formatPhone(Appendable formatted, RowData phone) throws IOException  {
    	formatted.append("TEL");
    	if (phone.preferred)
    		formatted.append(";PREF");

    	if (phone.customLabel != null) {
    		formatted.append(";" + LABEL_PARAM + "=");
    		formatted.append(phone.customLabel);
    	}
    	switch (phone.type) {
    	case Contacts.PhonesColumns.TYPE_HOME:
    		formatted.append(";VOICE");
    		break;
    	case Contacts.PhonesColumns.TYPE_WORK:
    		formatted.append(";VOICE;WORK");
    		break;
    	case Contacts.PhonesColumns.TYPE_FAX_WORK:
    		formatted.append(";FAX;WORK");
    		break;
    	case Contacts.PhonesColumns.TYPE_FAX_HOME:
    		formatted.append(";FAX;HOME");
    		break;
    	case Contacts.PhonesColumns.TYPE_MOBILE:
    		formatted.append(";CELL");
    		break;
    	case Contacts.PhonesColumns.TYPE_PAGER:
    		formatted.append(";PAGER");
    		break;
    	}
    	
    	
    	if (!StringUtil.isASCII(phone.data))
    		formatted.append(";CHARSET=UTF-8");
    	formatted.append(":").append(phone.data.trim()).append(NL);
    }
    
    /**
     * Format a phone as a vCard field.
     *  
     * @param formatted Formatted phone will be appended to this buffer
     * @param addr The rowdata containing the actual phone data.
     */
    public static void formatAddr(Appendable formatted, RowData addr)  throws IOException  {
    	formatted.append("ADR");
    	if (addr.preferred)
    		formatted.append(";PREF");

    	if (addr.customLabel != null) {
    		formatted.append(";" + LABEL_PARAM + "=");
    		formatted.append(addr.customLabel);
    	}
    	
    	switch (addr.type) {
    	case ContactMethodsColumns.TYPE_HOME:
    		formatted.append(";HOME");
    		break;
    	case Contacts.PhonesColumns.TYPE_WORK:
    		formatted.append(";WORK");
    		break;
    	}
    	if (!StringUtil.isASCII(addr.data))
    		formatted.append(";CHARSET=UTF-8");
    	formatted.append(":;;").append(addr.data.replace(", ", ";").trim()).append(NL);
    }
    
    /**
     * Format an IM contact as a vCard field.
     *  
     * @param formatted Formatted im contact will be appended to this buffer
     * @param addr The rowdata containing the actual phone data.
     */
    public static void formatIM(Appendable formatted, RowData im)  throws IOException  {
    	formatted.append(IMPROP);
    	if (im.preferred)
    		formatted.append(";PREF");
    	
    	if (im.customLabel != null) {
    		formatted.append(";" + LABEL_PARAM + "=");
    		formatted.append(im.customLabel);
    	}
    	
    	switch (im.type) {
    	case ContactMethodsColumns.TYPE_HOME:
    		formatted.append(";HOME");
    		break;
    	case ContactMethodsColumns.TYPE_WORK:
    		formatted.append(";WORK");
    		break;
    	}
    	
    	if (im.auxData != null) {
    		formatted.append(";").append(PROTO_PARAM).append("=").append(im.auxData);
    	}
    	if (!StringUtil.isASCII(im.data))
    		formatted.append(";CHARSET=UTF-8");
    	formatted.append(":").append(im.data.trim()).append(NL);
    }
    
    /**
     * Format Organization fields.
     *  
     *  
     *  
     * @param formatted Formatted organization info will be appended to this buffer
     * @param addr The rowdata containing the actual organization data.
     */
    public static void formatOrg(Appendable formatted, OrgData org)  throws IOException  {
    	if (org.company != null) {
    		formatted.append("ORG");
        	if (org.customLabel != null) {
        		formatted.append(";" + LABEL_PARAM + "=");
        		formatted.append(org.customLabel);
        	}
        	if (!StringUtil.isASCII(org.company))
        		formatted.append(";CHARSET=UTF-8");
        	formatted.append(":").append(org.company.trim()).append(NL);
        	if (org.title == null)
        		formatted.append("TITLE:").append(NL);
    	}
    	if (org.title != null) {
        	if (org.company == null)
        		formatted.append("ORG:").append(NL);
    		formatted.append("TITLE");
        	if (org.customLabel != null) {
        		formatted.append(";" + LABEL_PARAM + "=");
        		formatted.append(org.customLabel);
        	}
        	if (!StringUtil.isASCII(org.title))
        		formatted.append(";CHARSET=UTF-8");
        	formatted.append(":").append(org.title.trim()).append(NL);
    	}
    }

    
    public String toString() {
        StringWriter out = new StringWriter();
        try {
        	writeVCard(out);
        } catch (IOException e) {
        	// Should never happen
        }
        return out.toString();
    }    
    
    /**
     * Write the contact vCard to an appendable stream.
     */
    public void writeVCard(Appendable vCardBuff) throws IOException {
        // Start vCard

        vCardBuff.append("BEGIN:VCARD").append(NL);
        vCardBuff.append("VERSION:2.1").append(NL);
        
       	appendField(vCardBuff, "X-IRMC-LUID", syncid);
        
        vCardBuff.append("N");

    	if (!StringUtil.isASCII(lastName) || !StringUtil.isASCII(firstName))
    		vCardBuff.append(";CHARSET=UTF-8");
    	
        vCardBuff.append(":").append((lastName != null) ? lastName.trim() : "")
                .append(";").append((firstName != null) ? firstName.trim() : "")
                .append(";").append(";").append(";").append(NL);

        
        for (RowData email : emails) {
    		formatEmail(vCardBuff, email);
    	}
        
        for (RowData phone : phones) {
    		formatPhone(vCardBuff, phone);
    	}

        for (OrgData org : orgs) {
        	formatOrg(vCardBuff, org);
        }
        
        for (RowData addr : addrs) {
        	formatAddr(vCardBuff, addr);
        }

        for (RowData im : ims) {
        	formatIM(vCardBuff, im);
        }

        appendField(vCardBuff, "NOTE", notes);
        appendField(vCardBuff, "BDAY", birthday);
        
        if (photo != null) {
        	appendField(vCardBuff, "PHOTO;TYPE=JPEG;ENCODING=BASE64", " ");
        	Base64Coder.mimeEncode(vCardBuff, photo, 76, NL);
        	vCardBuff.append(NL);
        	vCardBuff.append(NL);
        }

        // End vCard
        vCardBuff.append("END:VCARD").append(NL);
    }

    /**
     * Append the field to the StringBuffer out if not null.
     */
    private static void appendField(Appendable out, String name, String val) throws IOException {
        if(val != null && val.length() > 0) {
        	out.append(name);
        	if (!StringUtil.isASCII(val))
        		out.append(";CHARSET=UTF-8");
            out.append(":").append(val).append(NL);
        }
    }

    /**
     * Populate the contact fields from a cursor
     */
    public void populate(Cursor peopleCur, ContentResolver cResolver) {
    	reset();
        setPeopleFields(peopleCur);
        String personID = _id;
        
        if (querySyncId != null) {
        	querySyncId.bindString(1, personID);
        	try {
        		syncid = querySyncId.simpleQueryForString();
        	} catch (SQLiteDoneException e) {
        		if (insertSyncId != null) {
	            	// Create a new syncid 
	            	syncid = UUID.randomUUID().toString();
	            	
	            	// Write the new syncid
	            	insertSyncId.bindString(1, personID);
	            	insertSyncId.bindString(2, syncid);
	            	insertSyncId.executeInsert();
        		}
        	}
        }

        Cursor organization = cResolver.query(Contacts.Organizations.CONTENT_URI, null,
        		Contacts.OrganizationColumns.PERSON_ID + "=" + personID, null, null);
        
        // Set the organization fields
        if (organization.moveToFirst()) {
        	do {
        		setOrganizationFields(organization);
        	} while (organization.moveToNext());
        }
        organization.close();
        
        Cursor phones = cResolver.query(Contacts.Phones.CONTENT_URI, null,
        		Contacts.Phones.PERSON_ID + "=" + personID, null, null);

        // Set all the phone numbers
        if (phones.moveToFirst()) {
            do {
                setPhoneFields(phones);
            } while (phones.moveToNext());
        }
        phones.close();

        Cursor contactMethods = cResolver.query(Contacts.ContactMethods.CONTENT_URI,
                null, Contacts.ContactMethods.PERSON_ID + "=" + personID, null, null);

        // Set all the contact methods (emails, addresses, ims)
        if (contactMethods.moveToFirst()) {
            do {
                setContactMethodsFields(contactMethods);
            } while (contactMethods.moveToNext());
        }
        contactMethods.close();
        
        // Load a photo if one exists.
        Cursor contactPhoto = cResolver.query(Contacts.Photos.CONTENT_URI, null, Contacts.PhotosColumns.PERSON_ID + "=" + personID, null, null);
        if (contactPhoto.moveToFirst()) {
        	photo = contactPhoto.getBlob(contactPhoto.getColumnIndex(Contacts.PhotosColumns.DATA));
        }
        contactPhoto.close();
    }

    /**
     * Retrieve the People fields from a Cursor
     */
    private void setPeopleFields(Cursor cur) {

        int selectedColumn;

        // Set the contact id
        selectedColumn = cur.getColumnIndex(Contacts.People._ID);
        long nid = cur.getLong(selectedColumn);
        _id = String.valueOf(nid);

        //
        // Get PeopleColumns fields
        //
        selectedColumn = cur.getColumnIndex(Contacts.PeopleColumns.NAME);
        displayName = cur.getString(selectedColumn);

        if (displayName != null) {
        	Matcher m = namePattern.matcher(displayName);
        	if (m.matches()) {
        		if (m.group(1) != null) {
        			lastName = m.group(2);
        			firstName = m.group(3);
        		} else {
        			firstName = m.group(5);
        			lastName = m.group(6);
        		}
        	} else {
        		firstName = displayName;
        		lastName = "";
        	}
        } else {
        	firstName = lastName = "";
        }
        
        selectedColumn = cur.getColumnIndex(Contacts.People.NOTES);
        notes = cur.getString(selectedColumn);
        if (notes != null) {
        	Matcher ppm = birthdayPattern.matcher(notes);

        	if (ppm.find()) {
        		birthday = ppm.group(1);
        		notes = ppm.replaceFirst("");
        	}
        }
    }
    
    /**
     * Retrieve the organization fields from a Cursor
     */
    private void setOrganizationFields(Cursor cur) {
        
        int selectedColumn;
        
        //
        // Get Organizations fields
        //
        selectedColumn = cur.getColumnIndex(Contacts.OrganizationColumns.COMPANY);
        String company = cur.getString(selectedColumn);

        selectedColumn = cur.getColumnIndex(Contacts.OrganizationColumns.TITLE);
        String title = cur.getString(selectedColumn);

        selectedColumn = cur.getColumnIndex(Contacts.OrganizationColumns.TYPE);
        int orgType = cur.getInt(selectedColumn);
        
        String customLabel = null;        
        if (orgType == ContactMethodsColumns.TYPE_CUSTOM) {
        	selectedColumn = cur
        		.getColumnIndex(ContactMethodsColumns.LABEL);
        	customLabel = cur.getString(selectedColumn);
        }
        
        orgs.add(new OrgData(orgType, title, company, customLabel));
    }

    /**
     * Retrieve the Phone fields from a Cursor
     */
    private void setPhoneFields(Cursor cur) {

        int selectedColumn;
        int selectedColumnType;
        int preferredColumn;
        int phoneType;
        String customLabel = null;

        //
        // Get PhonesColums fields
        //
        selectedColumn = cur.getColumnIndex(Contacts.PhonesColumns.NUMBER);
        selectedColumnType = cur.getColumnIndex(Contacts.PhonesColumns.TYPE);
        preferredColumn = cur.getColumnIndex(Contacts.PhonesColumns.ISPRIMARY);
        phoneType = cur.getInt(selectedColumnType);
        String phone = cur.getString(selectedColumn);
        boolean preferred = cur.getInt(preferredColumn) != 0;
        if (phoneType == Contacts.PhonesColumns.TYPE_CUSTOM) {
        	customLabel = cur.getString(cur.getColumnIndex(Contacts.PhonesColumns.LABEL));
        }
        
        
        phones.add(new RowData(phoneType, phone, preferred, customLabel));
    }

    /**
     * Retrieve the email fields from a Cursor
     */
    private void setContactMethodsFields(Cursor cur) {

        int selectedColumn;
        int selectedColumnType;
        int selectedColumnKind;
        int selectedColumnPrimary;
        int selectedColumnLabel;
        
        int methodType;
        int kind;
        String customLabel = null;
        String auxData = null;

        //
        // Get ContactsMethodsColums fields
        //
        selectedColumn = cur
                .getColumnIndex(ContactMethodsColumns.DATA);
        selectedColumnType = cur
                .getColumnIndex(ContactMethodsColumns.TYPE);
        selectedColumnKind = cur
                .getColumnIndex(ContactMethodsColumns.KIND);
        selectedColumnPrimary = cur
                .getColumnIndex(ContactMethodsColumns.ISPRIMARY);
        
        kind = cur.getInt(selectedColumnKind);
        
        methodType = cur.getInt(selectedColumnType);
        String methodData = cur.getString(selectedColumn);
        boolean preferred = cur.getInt(selectedColumnPrimary) != 0;
        if (methodType == ContactMethodsColumns.TYPE_CUSTOM) {
        	selectedColumnLabel = cur
        		.getColumnIndex(ContactMethodsColumns.LABEL);
        	customLabel = cur.getString(selectedColumnLabel);
        }
        
        switch (kind) {
        case Contacts.KIND_EMAIL:
        	emails.add(new RowData(methodType, methodData, preferred, customLabel));
        	break;
        case Contacts.KIND_POSTAL:
        	addrs.add(new RowData(methodType, methodData, preferred, customLabel));
        	break;
        case Contacts.KIND_IM:
            RowData newRow = new RowData(methodType, methodData, preferred, customLabel);
            
            selectedColumn = cur.getColumnIndex(ContactMethodsColumns.AUX_DATA);
            auxData = cur.getString(selectedColumn);

            if (auxData != null) {
            	String[] auxFields = StringUtil.split(auxData, ":");
            	if (auxFields.length > 1) {
            		if (auxFields[0].equalsIgnoreCase("pre")) {
            			int protval = 0;
            			try {
            				protval = Integer.decode(auxFields[1]);
            			} catch (NumberFormatException e) {
            				// Do nothing; protval = 0
            			}
            			if (protval < 0 || protval >= PROTO.length)
            				protval = 0;
            			newRow.auxData = PROTO[protval];
            		} else if (auxFields[0].equalsIgnoreCase("custom")) {
            			newRow.auxData = auxFields[1];
            		}
            	} else {
            		newRow.auxData = auxData;
            	}
            }
            
        	ims.add(newRow);
        	break;
        }
    }


    public ContentValues getPeopleCV() {
        ContentValues cv = new ContentValues();
    	
        StringBuffer fullname = new StringBuffer();
        if (displayName != null)
        	fullname.append(displayName);
        else {
        	if (firstName != null)
        		fullname.append(firstName);
        	if (lastName != null) {
        		if (firstName != null)
        			fullname.append(" ");
        		fullname.append(lastName);
        	}
        }
        
        // Use company name if only the company is given.
        if (fullname.length() == 0 && orgs.size() > 0 && orgs.get(0).company != null)
        	fullname.append(orgs.get(0).company);

        cv.put(Contacts.People.NAME, fullname.toString());

        if (!StringUtil.isNullOrEmpty(_id)) {
            cv.put(Contacts.People._ID, _id);
        }
        
        StringBuffer allnotes = new StringBuffer();
        if (birthday != null) {
        	allnotes.append(BIRTHDAY_FIELD).append(" ").append(birthday);
        }
        if (notes != null) {
        	if (birthday != null) {
        		allnotes.append(";\n");
        	}
        	allnotes.append(notes);
        }
        
        if (allnotes.length() > 0)
        	cv.put(Contacts.People.NOTES, allnotes.toString());
        
        return cv;
    }
    
    public ContentValues getOrganizationCV(OrgData org) {

        if(StringUtil.isNullOrEmpty(org.company) && StringUtil.isNullOrEmpty(org.title)) {
            return null;
        }
        ContentValues cv = new ContentValues();
    
        cv.put(Contacts.Organizations.COMPANY, org.company);
        cv.put(Contacts.Organizations.TITLE, org.title);
        cv.put(Contacts.Organizations.TYPE, org.type);
        cv.put(Contacts.Organizations.PERSON_ID, _id);
        if (org.customLabel != null) {
        	cv.put(Contacts.Organizations.LABEL, org.customLabel);
        }
        
        return cv;
    }

    public ContentValues getPhoneCV(RowData data) {
        ContentValues cv = new ContentValues();

        cv.put(Contacts.Phones.NUMBER, data.data);
        cv.put(Contacts.Phones.TYPE, data.type);
        cv.put(Contacts.Phones.ISPRIMARY, data.preferred ? 1 : 0);
        cv.put(Contacts.Phones.PERSON_ID, _id);
        if (data.customLabel != null) {
        	cv.put(Contacts.Phones.LABEL, data.customLabel);
        }

        return cv;
    }


    public ContentValues getEmailCV(RowData data) {
        ContentValues cv = new ContentValues();

        cv.put(Contacts.ContactMethods.DATA, data.data);
        cv.put(Contacts.ContactMethods.TYPE, data.type);
        cv.put(Contacts.ContactMethods.KIND,
                Contacts.KIND_EMAIL);
        cv.put(Contacts.ContactMethods.ISPRIMARY, data.preferred ? 1 : 0);
        cv.put(Contacts.ContactMethods.PERSON_ID, _id);
        if (data.customLabel != null) {
        	cv.put(Contacts.ContactMethods.LABEL, data.customLabel);
        }

        return cv;
    }
     
    public ContentValues getAddressCV(RowData data) {
        ContentValues cv = new ContentValues();

        cv.put(Contacts.ContactMethods.DATA, data.data);
        cv.put(Contacts.ContactMethods.TYPE, data.type);
        cv.put(Contacts.ContactMethods.KIND, Contacts.KIND_POSTAL);
        cv.put(Contacts.ContactMethods.ISPRIMARY, data.preferred ? 1 : 0);
        cv.put(Contacts.ContactMethods.PERSON_ID, _id);
        if (data.customLabel != null) {
        	cv.put(Contacts.ContactMethods.LABEL, data.customLabel);
        }

        return cv;
    }
    

    public ContentValues getImCV(RowData data) {
        ContentValues cv = new ContentValues();

        cv.put(Contacts.ContactMethods.DATA, data.data);
        cv.put(Contacts.ContactMethods.TYPE, data.type);
        cv.put(Contacts.ContactMethods.KIND, Contacts.KIND_IM);
        cv.put(Contacts.ContactMethods.ISPRIMARY, data.preferred ? 1 : 0);
        cv.put(Contacts.ContactMethods.PERSON_ID, _id);
        if (data.customLabel != null) {
        	cv.put(Contacts.ContactMethods.LABEL, data.customLabel);
        }
        
        if (data.auxData != null) {
        	int protoNum = -1;
        	for (int i = 0; i < PROTO.length; ++i) {
        		if (data.auxData.equalsIgnoreCase(PROTO[i])) {
        			protoNum = i;
        			break;
        		}
        	}
        	if (protoNum >= 0) {
        		cv.put(Contacts.ContactMethods.AUX_DATA, "pre:"+protoNum);
        	} else {
        		cv.put(Contacts.ContactMethods.AUX_DATA, "custom:"+data.auxData);
        	}
        }

        return cv;
    }


    /**
     * Add a new contact to the Content Resolver
     * 
     * @param key the row number of the existing contact (if known)
     * @return The row number of the inserted column
     */
    public long addContact(Context context, long key, boolean replace) {
        ContentResolver cResolver = context.getContentResolver();
        ContentValues pCV = getPeopleCV();
        
        boolean addSyncId = false;
        boolean replacing = false;
        
        if (key <= 0 && syncid != null) {
        	if (queryPersonId != null) try {
        		queryPersonId.bindString(1, syncid);
        		setId(queryPersonId.simpleQueryForString());
        		key = getId();
        	} catch(SQLiteDoneException e) {
        		// Couldn't locate syncid, we'll add it;
        		// need to wait until we know what the key is, though.
        		addSyncId = true;
        	}
        }
        
        Uri newContactUri = null;
        
        if (key > 0) {
        	newContactUri = ContentUris.withAppendedId(Contacts.People.CONTENT_URI, key);
        	Cursor testit = cResolver.query(newContactUri, null, null, null, null);
        	if (testit == null || testit.getCount() == 0) {
        		newContactUri = null;
        		pCV.put(Contacts.People._ID, key);
        	}
        	if (testit != null)
        		testit.close();
        }
        if (newContactUri == null) {
        	newContactUri = insertContentValues(cResolver, Contacts.People.CONTENT_URI, pCV);
        	if (newContactUri == null) {
            	Log.error("Error adding contact." + " (key: " + key + ")");
            	return -1;
            }
            // Set the contact person id
            setId(newContactUri.getLastPathSegment());
            key = getId();
            
            // Add the new contact to the myContacts group
            Contacts.People.addToMyContactsGroup(cResolver, key);
        } else {
        	// update existing Uri
    		if (!replace)
    			return -1;

    		replacing = true;
    		
        	cResolver.update(newContactUri, pCV, null, null);
        }
        	

        // We need to add the syncid to the database so
        // that we'll detect this contact if we try to import
        // it again.
        if (addSyncId && insertSyncId != null) {
        	insertSyncId.bindLong(1, key);
        	insertSyncId.bindString(2, syncid);
        	insertSyncId.executeInsert();
        }
 
        /*
         * Insert all the new ContentValues
         */
        if (replacing) {
        	// Remove existing phones
        	Uri phones = Uri.withAppendedPath(ContentUris.withAppendedId(Contacts.People.CONTENT_URI, key), Contacts.People.Phones.CONTENT_DIRECTORY);
        	String[] phoneID = {Contacts.People.Phones._ID};
        	Cursor existingPhones = cResolver.query(phones, phoneID, null, null, null);
        	if (existingPhones != null && existingPhones.moveToFirst()) {
        		int idColumn = existingPhones.getColumnIndex(Contacts.People.Phones._ID); 
        		List<Long> ids = new ArrayList<Long>(existingPhones.getCount());
        		do {
        			ids.add(existingPhones.getLong(idColumn));
        		} while (existingPhones.moveToNext());
        		existingPhones.close();
        		for (Long id : ids) {
        			Uri phone = ContentUris.withAppendedId(Contacts.Phones.CONTENT_URI, id);
        			cResolver.delete(phone, null, null);
        		}
        	}
        	
        	// Remove existing contact methods (emails, addresses, etc.)
        	Uri methods = Uri.withAppendedPath(ContentUris.withAppendedId(Contacts.People.CONTENT_URI, key), Contacts.People.ContactMethods.CONTENT_DIRECTORY);
        	String[] methodID = {Contacts.People.ContactMethods._ID};
        	Cursor existingMethods = cResolver.query(methods, methodID, null, null, null);
        	if (existingMethods != null && existingMethods.moveToFirst()) {
        		int idColumn = existingMethods.getColumnIndex(Contacts.People.ContactMethods._ID); 
        		List<Long> ids = new ArrayList<Long>(existingMethods.getCount());
        		do {
        			ids.add(existingMethods.getLong(idColumn));
        		} while (existingMethods.moveToNext());
        		existingMethods.close();
        		for (Long id : ids) {
        			Uri method = ContentUris.withAppendedId(Contacts.Phones.CONTENT_URI, id);
        			cResolver.delete(method, null, null);
        		}
        	}
        }
        
        // Phones
        for (RowData phone : phones) {
        	insertContentValues(cResolver, Contacts.Phones.CONTENT_URI, getPhoneCV(phone));
        }
        
        // Organizations
        for (OrgData org : orgs) {
        	insertContentValues(cResolver, Contacts.Organizations.CONTENT_URI, getOrganizationCV(org));
        }
        
        Builder builder = newContactUri.buildUpon();
        builder.appendEncodedPath(Contacts.ContactMethods.CONTENT_URI.getPath());

        // Emails
        for (RowData email : emails) {
        	insertContentValues(cResolver, builder.build(), getEmailCV(email));
        }
        
        // Addressess
        for (RowData addr : addrs) {
        	insertContentValues(cResolver, builder.build(), getAddressCV(addr));
        }
        
        // IMs
        for (RowData im : ims) {
        	insertContentValues(cResolver, builder.build(), getImCV(im));
        }
        
        // Photo
        if (photo != null) {
        	Uri person = ContentUris.withAppendedId(Contacts.People.CONTENT_URI, key);
        	Contacts.People.setPhotoData(cResolver, person, photo);
        }

        return key;
    }
    
    /**
     * Insert a new ContentValues raw into the Android ContentProvider
     */
    private Uri insertContentValues(ContentResolver cResolver, Uri uri, ContentValues cv) {
        if (cv != null) {
        	return cResolver.insert(uri, cv);
        }
        return null;
    }

    /**
     * Get the item content
     */
    public String getContent() {
    	return toString();
    }

    /**
     * Check if the email string is well formatted
     */
    @SuppressWarnings("unused")
	private boolean checkEmail(String email) {
        return (email != null && !"".equals(email) && email.indexOf("@") != -1);
    }
}
