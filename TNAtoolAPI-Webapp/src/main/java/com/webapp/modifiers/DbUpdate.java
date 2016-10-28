// Copyright (C) 2015 Oregon State University - School of Mechanical,Industrial and Manufacturing Engineering 
//   This file is part of Transit Network Analysis Software Tool.
//
//    Transit Network Analysis Software Tool is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Transit Network Analysis Software Tool is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU  General Public License for more details.
//
//    You should have received a copy of the GNU  General Public License
//    along with Transit Network Analysis Software Tool.  If not, see <http://www.gnu.org/licenses/>.

package com.webapp.modifiers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException; 
import net.lingala.zip4j.io.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import com.model.database.*;
import com.model.database.onebusaway.gtfs.hibernate.ext.GtfsHibernateReaderExampleMain;
import com.model.database.queries.EventManager;
import com.model.database.queries.UpdateEventManager;
import com.model.database.queries.objects.DatabaseStatus;
import com.model.database.queries.util.Hutil;
import com.webapp.api.MainMap;
import com.webapp.api.Queries;

import org.w3c.dom.*;
import org.xml.sax.SAXException;


@Path("/dbupdate")
@XmlRootElement
public class DbUpdate {
	private final static String basePath = "C:/Users/tnatool/Development/Repository/test/";
	private final static String psqlPath = "C:/Program Files/PostgreSQL/9.4/bin/";
	private final static int USER_COUNT = 10;
	private final static int QUOTA = 10000000;
	private static final String dbURL = Databases.connectionURLs[Databases.connectionURLs.length-1];
	private static final String dbUSER = Databases.usernames[Databases.usernames.length-1];
	private static final String dbPASS = Databases.passwords[Databases.passwords.length-1];
//	private static final int DBINDEX = Databases.dbsize-1;
	public final static String VERSION = "V4.16.07";
	
	public static List<String> getSelectedAgencies(String username){
		List<String> selectedAgencies = new ArrayList<String>();
		Connection c = null;
		Statement statement = null;
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.createStatement();
			/*ResultSet rs = statement.executeQuery("SELECT defaultid FROM gtfs_feed_info "
					+ "JOIN gtfs_selected_feeds "
					+ "ON gtfs_feed_info.feedname=gtfs_selected_feeds.feedname "
					+ "WHERE gtfs_selected_feeds.username = '"+username+"';");*/
			ResultSet rs = statement.executeQuery("SELECT agency_id FROM gtfs_selected_feeds "
					+ "WHERE username = '"+username+"';");
			while(rs.next()){
				selectedAgencies.add(rs.getString("agency_id"));
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		if(selectedAgencies.isEmpty()){
			selectedAgencies.add("null");
		}
		return selectedAgencies;
	}
	
	@GET
    @Path("/getDefaultDbIndex")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getDefaultDbIndex(){
		PDBerror b = new PDBerror();
		b.DBError = (Databases.dbsize-1)+"";
		return b;
	}
	
	@GET
    @Path("/getVersion")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public static Object getVersion(){
		
		PDBerror b = new PDBerror();
		b.DBError = VERSION;
		return b;
	}
	
	@POST
    @Path("/correctAjax")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object tests(@QueryParam("x") String x){
		
		PDBerror b = new PDBerror();
		b.DBError = x;
		System.out.println(x);
		
		return b;
	}
	
	@GET
    @Path("/readDBinfo")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object readDBinfo() throws IOException{
		/*String tmpPath = basePath+"../../src/main/webapp/resources/admin/";
		File inputFile = new File(tmpPath + "dbInfo.csv");*/
		
		ClassLoader classLoader = getClass().getClassLoader();
		File inputFile = new File(classLoader.getResource("admin/resources/dbInfo.csv").getFile());
		
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String dbInfo = reader.readLine();
		String line;
//		int j=0;
//		reader.readLine();
		while((line=reader.readLine()) != null) {
			dbInfo += "#$#"+line;
		} 
		reader.close();
		return dbInfo;
	}
	
	@GET
    @Path("/getIndex")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getIndex() throws IOException{
		/*String tmpPath = basePath+"../../src/main/webapp/resources/admin/";
		File inputFile = new File(tmpPath + "dbInfo.csv");*/
		
		ClassLoader classLoader = getClass().getClassLoader();
		File inputFile = new File(classLoader.getResource("admin/resources/dbInfo.csv").getFile());
		
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		int j=0;
		reader.readLine();
		while(reader.readLine() != null) {
			j++;
		} 
		reader.close();
		
		return j+"";
	}
	
	@GET
    @Path("/activateDBs")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object activateDBs(@QueryParam("db") String db) throws IOException{
		Databases.infoMap = Databases.getDbInfo();
		updateDatabaseStaticInfo(true);
		
		String[] dbInfo = db.split(",");
		Connection c = null;
		Statement statement = null;
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			statement = c.createStatement();
			statement.executeUpdate("UPDATE database_status SET activated = true;");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return "done";
	}
	
	public void updateDatabaseStaticInfo(boolean b){
		Databases.updateDbInfo(b);
		Queries.updateDefaultDBindex();
		GtfsHibernateReaderExampleMain.updateSessions();
		Hutil.updateSessions();
		EventManager.updateSessions();
	}
	
	@GET
    @Path("/changeDBStatus")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object changeDBStatus(@QueryParam("db") String db, @QueryParam("field") String fieldName, @QueryParam("b") boolean b) throws IOException{
		
		String[] dbInfo = db.split(",");
		Connection c = null;
		Statement statement = null;
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			statement = c.createStatement();
			statement.executeUpdate("UPDATE database_status SET "+fieldName+" = "+b+";");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return "done";
	}
	
	@GET
    @Path("/deactivateDBs")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object deactivateDBs(@QueryParam("db") String db, @QueryParam("index") int index) throws IOException{
		Databases.deactivateDB(index);
		updateDatabaseStaticInfo(false);
		
		String[] dbInfo = db.split(",");
		Connection c = null;
		Statement statement = null;
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			statement = c.createStatement();
			statement.executeUpdate("UPDATE database_status SET activated = false;");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return "done";
	}
	
	@GET
    @Path("/userCount")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object userCount(){
		Connection c = null;
		Statement statement = null;
		PDBerror error = new PDBerror();
		int count=0;
		error.DBError = "true";
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
			ResultSet rs = statement.executeQuery("select * from gtfs_pg_users;");
			rs.last();
			count = rs.getRow();
			if ( count>=USER_COUNT ) {
				error.DBError = "false";
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			error.DBError = "error";
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return error;
	}
	
	@GET
    @Path("/activateUser")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object activateUser(@QueryParam("key") String key, @QueryParam("user") String username) throws IOException, NoSuchAlgorithmException, UnsupportedEncodingException{
		/*String root = new File(".").getAbsolutePath();
        root = removeLastChar(root);*/
        /*File passFile = new File(basePath + "TNAtoolAPI-Webapp/WebContent/playground/pass.txt");
        BufferedReader bf; */
        String passkey = "";
        Connection c = null;
		Statement statement = null;
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.createStatement();
			ResultSet rs = statement.executeQuery("SELECT key FROM gtfs_pg_users WHERE username='"+username+"';");
			if ( rs.next() ) {
				passkey = rs.getString("key");
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			
		}
        /*String pass ="PGpass";
        try{
        	bf = new BufferedReader(new FileReader(passFile));
            passkey = bf.readLine();
            
            byte[] passByte = passkey.getBytes("UTF-8");
    		MessageDigest md = MessageDigest.getInstance("MD5");
    		passByte = md.digest(passByte);
    		pass = new String(passByte, "UTF-8");
//    		bf.close();
        }catch(IOException e){
        	e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}*/
        
        String email="";
        String lastname="";
        String firstname="";
		if(passkey.equals(key)){
			try {
				statement = c.createStatement();
				statement.executeUpdate("UPDATE gtfs_pg_users SET active=true WHERE username='"+username+"';");
				ResultSet rs = statement.executeQuery("select email,lastname,firstname from gtfs_pg_users where username='"+username+"';");
				if(rs.next()){
					email = rs.getString("email");
					lastname = rs.getString("lastname");
					firstname = rs.getString("firstname");
				}
				
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			} finally {
				if (statement != null) try { statement.close(); } catch (SQLException e) {}
				if (c != null) try { c.close(); } catch (SQLException e) {}
			}
		}else{
			return "exit";
		}
		
		  String to = email;
	      final String emailUser = "tnatooltech";
	      final String emailPass = "OSUteam007@gmail";
	      String host = "smtp.gmail.com";
	
	      Properties properties = System.getProperties();
	      properties.put("mail.smtp.host", host); 
	      properties.put("mail.smtp.user", emailUser);
	      properties.put("mail.smtp.password", emailPass);
	      properties.put("mail.smtp.port", "587"); 
	      properties.put("mail.smtp.auth", "true");  
	      //properties.put("mail.debug", "true");              
	      properties.put("mail.smtp.starttls.enable", "true");
	      //properties.put("mail.smtp.EnableSSL.enable", "true");
	      
	      Session session = Session.getInstance(properties,null);
	      System.out.println("Port: "+session.getProperty("mail.smtp.port"));
	
	      Transport trans=null;
	
	      try{
	         MimeMessage message = new MimeMessage(session);
	         InternetAddress addressFrom = new InternetAddress(emailUser+"@gmail.com");  
	         message.setFrom(addressFrom);
	         
	         InternetAddress[] addressesTo = {new InternetAddress(to)}; 
	         message.setRecipients(Message.RecipientType.TO, addressesTo);
	         
	         Multipart multipart = new MimeMultipart("alternative");
	         BodyPart messageBodyPart = new MimeBodyPart();
	         String htmlMessage = firstname+" "+lastname+",<br><br>"+"Your GTFS Playground account was successfully activated!<br>"
	         		+ "You can now log into the website using your credentials.";
	         messageBodyPart.setContent(htmlMessage, "text/html");
	         multipart.addBodyPart(messageBodyPart);
	         message.setContent(multipart);
	         
	         message.setSubject("GTFS Playground Account Activated");
	         trans = session.getTransport("smtp");
	         trans.connect(host,emailUser,emailPass);
	         //message.saveChanges();
	         trans.sendMessage(message, message.getAllRecipients()); 
	      }catch (MessagingException mex) {
	         mex.printStackTrace();
	      }
		PDBerror er = new PDBerror();
		er.DBError=username+"'s account was successfully activated.";
		return er;
	}
	
	/*@GET
    @Path("/validatePass")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object validatePass(@QueryParam("pass") String pass) throws IOException, NoSuchAlgorithmException, UnsupportedEncodingException{
		String tmpPath = basePath+"TNAtoolAPI-Webapp/WebContent/playground/";
		File inputFile = new File(tmpPath + "pass.txt");
	
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		PDBerror b = new PDBerror();
		String passkey = reader.readLine();
		if(passkey.equals(pass)){
			b.DBError = "true";
		}else{
			b.DBError = "false";
		}
		reader.close();
		
		return b;
	}*/
	
	@GET
    @Path("/getUserInfo")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getUserInfo(@QueryParam("user") String user){
		Connection c = null;
		Statement statement = null;
		UserInfo userInfo = new UserInfo();
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.createStatement();
			ResultSet rs = statement.executeQuery("select * from gtfs_pg_users where username='"+user+"' or email='"+user+"';");
			if ( rs.next() ) {
				userInfo.Firstname = rs.getString("firstname");
				userInfo.Lastname = rs.getString("lastname");
				userInfo.Username = rs.getString("username");
				userInfo.Quota = rs.getString("quota");
				userInfo.Usedspace = rs.getString("usedspace");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return userInfo;
	}
	
	@GET
    @Path("/checkUser")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object checkUser(@QueryParam("user") String user){
		Connection c = null;
		PreparedStatement statement = null;
		PDBerror error = new PDBerror();
		error.DBError = "false";
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.prepareStatement("select * from gtfs_pg_users where username=? or email=?;");
			statement.setString(1, user);
			statement.setString(2, user);
			ResultSet rs = statement.executeQuery();
			if ( rs.next() ) {
				error.DBError = "true";
			}else{
				error.DBError = "false";
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			error.DBError = "error";
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return error;
	}
	
	@GET
    @Path("/changePublic")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object changePublic(@QueryParam("isPublic") String p, @QueryParam("feedname") String feedname){
		Connection c = null;
		Statement statement = null;
		PDBerror error = new PDBerror();
		error.DBError = "";
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.createStatement();
			statement.executeUpdate("UPDATE gtfs_uploaded_feeds SET ispublic = '"+p+"' WHERE feedname = '"+feedname+"';");
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			error.DBError = "error";
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return error;
	}
	
	@GET
    @Path("/isActive")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object isActive(@QueryParam("user") String username){
		Connection c = null;
		Statement statement = null;
		PDBerror error = new PDBerror();
		error.DBError = "false";
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM gtfs_pg_users WHERE username = '"+username+"';");
			if(rs.next()){
				error.DBError = rs.getString("active");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			error.DBError = "error";
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return error;
	}
	
	/*@POST
    @Path("/uploadfeed")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.MULTIPART_FORM_DATA })
    public Object uploadFeed(RequestContext request){
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		try {
			List<FileItem> items = upload.parseRequest(request);
			System.out.println(items.size());
		}catch (FileUploadException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(request);

		PDBerror error = new PDBerror();
		
		
		return error;
	}*/
	
	/**
	 * Changes the playground passkey. Delete this method from the server!!
	 * @param password
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 *//*
	@GET
    @Path("/makePassKey")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object makePassKey(@QueryParam("pass") String password) 
    				throws UnsupportedEncodingException, NoSuchAlgorithmException, IOException{
		
		byte[] passByte = password.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		passByte = md.digest(passByte);
		String pass = new String(passByte, "UTF-8");
		
		String root = new File(".").getAbsolutePath();
        root = removeLastChar(root);
        File passFile = new File(basePath + "TNAtoolAPI-Webapp/WebContent/playground/pass.txt");
		BufferedWriter writer = new BufferedWriter(new FileWriter(passFile));
		
		writer.write(pass);
		
		writer.close();
		return "";
	}*/
	
	@GET
    @Path("/validateUser")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object validateUser(@QueryParam("user") String user, @QueryParam("pass") String password) 
    				throws UnsupportedEncodingException, NoSuchAlgorithmException{
		
		byte[] passByte = password.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		passByte = md.digest(passByte);
		String pass = new String(passByte, "UTF-8");
		
		Connection c = null;
		PreparedStatement statement = null;
		PDBerror error = new PDBerror();
		error.DBError = "false";
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.prepareStatement("SELECT * FROM gtfs_pg_users WHERE (username = ? or email = ?) and password = ?;");
			statement.setString(1, user);
			statement.setString(2, user);
			statement.setString(3, pass);
			ResultSet rs = statement.executeQuery();
			
			if(rs.next()){
				error.DBError = rs.getString("username");
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			error.DBError = e.getMessage();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return error;
	}
	
	@GET
    @Path("/addUser")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object addUser(@QueryParam("user") String user, @QueryParam("pass") String password, @QueryParam("email") String email,
    		@QueryParam("firstname") String firstname, @QueryParam("lastname") String lastname) 
    				throws UnsupportedEncodingException, NoSuchAlgorithmException{
		
		byte[] passByte = password.getBytes("UTF-8");
		MessageDigest md = MessageDigest.getInstance("MD5");
		passByte = md.digest(passByte);
		String pass = new String(passByte, "UTF-8");
		
		long millis = System.currentTimeMillis() % 1000000;
		
		Connection c = null;
		PreparedStatement statement = null;
		PDBerror error = new PDBerror();
		error.DBError = "";
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.prepareStatement("INSERT INTO gtfs_pg_users (username,password,email,firstname,lastname,quota,usedspace,active,key) "
					+ "VALUES (?,?,?,?,?,?,?,?,?);");
			statement.setString(1, user);
			statement.setString(2, pass);
			statement.setString(3, email);
			statement.setString(4, firstname);
			statement.setString(5, lastname);
			statement.setInt(6, QUOTA);
			statement.setInt(7, 0);
			statement.setBoolean(8, false);
			statement.setFloat(9, millis);
			statement.executeUpdate();
			error.DBError = "true";
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			error.DBError = e.getMessage();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return error;
	}
	
	@GET
    @Path("/checkInput")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object checkInput(@QueryParam("dbname") String dbname, @QueryParam("cURL") String cURL, @QueryParam("db") String db,
    		@QueryParam("user") String user, @QueryParam("pass") String pass, @QueryParam("oldURL") String oldURL, @QueryParam("olddbname") String olddbname) throws IOException{
		PDBerror b = new PDBerror();
		b.DBError = "true";
		List<String> dbnames = Arrays.asList(Databases.dbnames);
		List<String> urls = Arrays.asList(Databases.connectionURLs);
		if(!olddbname.equals(dbname) && dbnames.contains(dbname)){
			b.DBError = "Database display name \""+dbname+"\" already exists.";
		}else if(!oldURL.equals(cURL+db) && urls.contains(cURL+db)){
			b.DBError = "The connection \""+cURL.split("//")[1]+db+"\" already exists";
		}

		Connection c = null;
		try {
			c = DriverManager.getConnection(cURL, user, pass);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			b.DBError = e.getMessage();
		}finally {
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return b;
	}
	
	@GET
    @Path("/deleteDB")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object deleteDB(@QueryParam("index") String i) throws IOException{
		
//		String tmpPath = basePath+"TNAtoolAPI-Webapp/WebContent/admin/";
		String path = DbUpdate.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		ClassLoader classLoader = getClass().getClassLoader();
		File inputFile = new File(classLoader.getResource("admin/resources/dbInfo.csv").getFile());
		File tempFile = new File(path + "admin/resources/tmp.csv");
		File dstfile = new File(path+"../../src/main/resources/admin/resources/dbInfo.csv");
//		File inputFile = new File(tmpPath + "dbInfo.csv");
		

		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
		String currentLine;
		String index;
		String[] elems = new String[10];
		String[] elemsIndex = new String[10];
		int j=0;
		while((currentLine = reader.readLine()) != null) {
			elemsIndex = currentLine.split(",");
			index = elemsIndex[0];
		    if(!index.equals(i)){
		    	if(!index.equals("databaseIndex")){
		    		elemsIndex[0]=j+"";
		    		j++;
		    		currentLine="";
		    		for(int k=0;k<elemsIndex.length-1;k++){
		    			currentLine+=elemsIndex[k]+",";
		    		}
		    		currentLine+=elemsIndex[elemsIndex.length-1];
		    	}
		    	writer.write(currentLine + System.getProperty("line.separator"));
		    }else{
		    	elems = elemsIndex; 
		    }
		}
		writer.close(); 
		reader.close(); 
		inputFile.delete();
		tempFile.renameTo(inputFile);
		Files.copy(inputFile.toPath(), dstfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		inputFile = new File(path + elems[2]);
		inputFile.delete();
		dstfile = new File(path+"../../src/main/resources/"+elems[2]);
		dstfile.delete();
		/*String censusPath = basePath+"library-hibernate-spatial/src/main/resources/";
		String gtfsPath = basePath+"onebusaway-gtfs-hibernate/src/test/resources/org/onebusaway/gtfs/examples/";
		File f = new File(censusPath+elems[2]);
		f.delete();*/
		
		inputFile = new File(path + elems[3]);
		inputFile.delete();
		dstfile = new File(path+"../../src/main/resources/"+elems[3]);
		dstfile.delete();
		/*f = new File(gtfsPath+element[element.length-1]);
		f.delete();*/
		
		PDBerror b = new PDBerror();
		b.DBError = "";
		String[] element = elems[4].split("/");
		String url = "";
		for (int k=0;k<element.length-1;k++){
			url += element[k]+"/";
		}
		Connection c = null;
		Statement statement = null;
		try {
			c = DriverManager.getConnection(url, elems[5], elems[6]);
			statement = c.createStatement();
			ResultSet rs = statement.executeQuery("select pg_terminate_backend(pid) from pg_stat_activity where datname='"+element[element.length-1]+"'");
			
			statement.executeUpdate("DROP DATABASE "+element[element.length-1]);
			b.DBError = "Database was successfully deleted";
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			b.DBError = e.getMessage();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		System.out.println(b.DBError);
		
		Databases.infoMap = Databases.getDbInfo();
		updateDatabaseStaticInfo(true);
		
		return b;
	}
	
	@GET
    @Path("/checkForDeactivated")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object checkForDeactivated(@QueryParam("db") String db){
		String b = "false";
		Connection c = null;
		Statement statement = null;
		String[] dbInfo = db.split(",");
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			statement = c.createStatement();
			ResultSet rs = statement.executeQuery("SELECT activated FROM database_status");
			rs.next();
			boolean bb = rs.getBoolean("activated");
			if(!bb){
				b = "true";
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return b;
	}
	
	@GET
    @Path("/checkDBStatus")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object checkDBStatus(@QueryParam("db") String db){
		DatabaseStatus dbstat = new DatabaseStatus();
		Connection c = null;
		Statement statement = null;
		String[] dbInfo = db.split(",");
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			statement = c.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM database_status");
			rs.next();
			dbstat.Activated = rs.getBoolean("activated");
			dbstat.Census = rs.getBoolean("census");
			dbstat.CreateDate = rs.getString("create_date");
			dbstat.ModifyDate = rs.getString("modify_date");
			dbstat.Employment = rs.getBoolean("employment");
			dbstat.FutureEmp = rs.getBoolean("future_emp");
			dbstat.FuturePop = rs.getBoolean("future_pop");
			dbstat.GtfsFeeds = rs.getBoolean("gtfs_feeds");
			dbstat.Parknride = rs.getBoolean("parknride");
			dbstat.Title6 = rs.getBoolean("title6");
			dbstat.Updated = rs.getBoolean("update_process");
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return dbstat;
	}
	
	
	@GET
    @Path("/updateDB")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object updateDB(@QueryParam("db") String db, @QueryParam("oldName") String oldName, @QueryParam("oldcfgSpatial") String oldcfgSpatial, @QueryParam("oldcfgTransit") String oldcfgTransit) throws IOException{
		String path = DbUpdate.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		ClassLoader classLoader = getClass().getClassLoader();
		File inputFile = new File(classLoader.getResource("admin/resources/dbInfo.csv").getFile());
		File tempFile = new File(path + "admin/resources/tmp.csv");
		File dstfile = new File(path+"../../src/main/resources/admin/resources/dbInfo.csv");
		String[] dbInfo = db.split(",");
		
		
		String[] p = dbInfo[4].split("/");
		String name = p[p.length-1];
		String url = "";
		for (int k=0;k<p.length-1;k++){
			url += p[k]+"/";
		}
		Connection c = null;
		Statement statement = null;
		PDBerror error = new PDBerror();
		error.DBError = "";
		try {
			c = DriverManager.getConnection(url, dbInfo[5], dbInfo[6]);
			statement = c.createStatement();
			ResultSet rs = statement.executeQuery("select pg_terminate_backend(pid) from pg_stat_activity where datname='"+oldName+"'");
			statement.executeUpdate("ALTER DATABASE "+oldName+" RENAME TO "+name);
			error.DBError = "Database was successfully updated";
			
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
			String currentLine;
			String index;
			while((currentLine = reader.readLine()) != null) {
				index = currentLine.split(",")[0];
			    if(index.equals(dbInfo[0])){
			    	currentLine = "";
			    	for(int k=0;k<dbInfo.length-1;k++){
		    			currentLine+=dbInfo[k]+",";
		    		}
		    		currentLine+=dbInfo[dbInfo.length-1];
			    }
			    writer.write(currentLine + System.getProperty("line.separator"));
			}
			writer.close(); 
			reader.close(); 
			inputFile.delete();
			tempFile.renameTo(inputFile);
			Files.copy(inputFile.toPath(), dstfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			
			inputFile = new File(path + oldcfgSpatial);
			inputFile.delete();
			dstfile = new File(path+"../../src/main/resources/"+oldcfgSpatial);
			dstfile.delete();
			
			inputFile = new File(path + oldcfgTransit);
			inputFile.delete();
			dstfile = new File(path+"../../src/main/resources/"+oldcfgTransit);
			dstfile.delete();
			
			inputFile = new File(classLoader.getResource("admin/resources/censusDb.cfg.xml").getFile());
			dstfile = new File(path + dbInfo[2]);
			parseXmlFile(inputFile, dstfile, dbInfo, true);
			Files.copy(dstfile.toPath(), new File(path+"../../src/main/resources/"+dbInfo[2]).toPath(), StandardCopyOption.REPLACE_EXISTING);
			
			inputFile = new File(classLoader.getResource("admin/resources/gtfsDb.cfg.xml").getFile());
			dstfile = new File(path + dbInfo[3]);
			parseXmlFile(inputFile, dstfile, dbInfo, false);
			Files.copy(dstfile.toPath(), new File(path+"../../src/main/resources/"+dbInfo[3]).toPath(), StandardCopyOption.REPLACE_EXISTING);
			
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			error.DBError = e.getMessage();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return error;
	}
	
	@GET
    @Path("/addDB")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object addDB(@QueryParam("db") String db){
		String path = DbUpdate.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("admin/resources/dbInfo.csv").getFile());
		File dstfile = new File(path+"../../src/main/resources/admin/resources/dbInfo.csv");
		String[] dbInfo = db.split(",");
		String[] p;
		
		PDBerror error = new PDBerror();
		error.DBError = "";
		
		
		p = dbInfo[4].split("/");
		String name = p[p.length-1];
		String url = "";
		for (int k=0;k<p.length-1;k++){
			url += p[k]+"/";
		}
		Connection c = null;
		Statement statement = null;
		try {
			c = DriverManager.getConnection(url, dbInfo[5], dbInfo[6]);
			statement = c.createStatement();
			statement.executeUpdate("CREATE DATABASE "+name);
//			addCensus(dbInfo[5], dbInfo[6], name);
			statement.close();
			c.close();
			
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			statement = c.createStatement();
			statement.executeUpdate("CREATE EXTENSION postgis;");
			statement.executeUpdate("DROP TABLE IF EXISTS database_status;");
			statement.executeUpdate("CREATE TABLE database_status ("
					+ "name character varying(255) NOT NULL,"
					+ "create_date date,"
					+ "modify_date date,"
					+ "activated boolean,"
					+ "gtfs_feeds boolean,"
					+ "census boolean,"
					+ "employment boolean,"
					+ "parknride boolean,"
					+ "title6 boolean,"
					+ "future_emp boolean,"
					+ "future_pop boolean,"
					+ "update_process boolean,"
					+ "CONSTRAINT database_status_pkey PRIMARY KEY (name))");
			statement.executeUpdate("INSERT INTO database_status "
					+ "VALUES ('"+name+"', '2015-10-15', '2015-10-15', "
							+ "false, false, false, false, false, false, false, false, false)");
			
			UpdateEventManager.createTables(c, dbInfo);
			
			statement.executeUpdate("insert into gtfs_pg_users (username,email,firstname,lastname,quota,usedspace,password,active,key) "
					+ "VALUES ('admin','admin','','',1,0,'1234',false,1234);");
			
			error.DBError = "Database was successfully added";
			
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
		    for(int i=0; i<dbInfo.length-1; i++){
		    	out.print(dbInfo[i]+",");
		    }
		    out.print(dbInfo[dbInfo.length-1] + System.getProperty("line.separator"));
		    out.close();
		    
		    Files.copy(file.toPath(), dstfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		    
		    file = new File(classLoader.getResource("admin/resources/censusDb.cfg.xml").getFile());
			dstfile = new File(path + dbInfo[2]);
    		parseXmlFile(file, dstfile, dbInfo, true);
    		Files.copy(dstfile.toPath(), new File(path+"../../src/main/resources/"+dbInfo[2]).toPath(), StandardCopyOption.REPLACE_EXISTING);
    		
    		file = new File(classLoader.getResource("admin/resources/gtfsDb.cfg.xml").getFile());
			dstfile = new File(path + dbInfo[3]);
    		parseXmlFile(file, dstfile, dbInfo, false);
    		Files.copy(dstfile.toPath(), new File(path+"../../src/main/resources/"+dbInfo[3]).toPath(), StandardCopyOption.REPLACE_EXISTING);
    		
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			error.DBError = e.getMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		System.out.println(error.DBError);
		
		return error;
	}
	
	@GET
    @Path("/restoreCensus")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object restoreCensus(@QueryParam("db") String db){
		String[] dbInfo = db.split(",");
		String[] p;
		p = dbInfo[4].split("/");
		String name = p[p.length-1];
		
		String path = DbUpdate.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String batFile = path+"../../src/main/resources/admin/resources/restoreCensus.bat";
		Process pr;
		ProcessBuilder pb;
		dbInfo[6] = "123123";
		dbInfo[5] = "postgres";
		name = "testdb";
		batFile = batFile.substring(1, batFile.length());
		try {
			pb = new ProcessBuilder("cmd", "/c", batFile, dbInfo[6], dbInfo[5], name,
					"C:/Program Files/PostgreSQL/9.3/bin/pg_dump.exe",
					"C:/Program Files/PostgreSQL/9.3/bin/psql.exe");
//			pb.redirectErrorStream(true);
			pr = pb.start();
			pr.waitFor(5,TimeUnit.MINUTES);
			System.out.println("done adding");
			/*BufferedReader in = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		    String line;
		    while ((line = in.readLine()) != null) {
		        System.out.println(line);
		    }
			*/
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		return "done";
	}
	 
	private void parseXmlFile(File xmlFile, File dstFile, String[] dbInfo, boolean b){
		
//		File xmlFile = new File(srcFile);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            
            NodeList props = doc.getElementsByTagName("property");
            Element prop = null;
             
            for(int i=0; i<3/*props.getLength()*/; i++){
            	prop = (Element) props.item(i+1);
                prop.appendChild(doc.createTextNode(dbInfo[i+4]));
            }
             
            NodeList mappings = doc.getElementsByTagName("mapping");
            Element map = null;
            
            if(b){
            	map = (Element) mappings.item(0);
            	map.setAttribute("resource", dbInfo[7]);
            }else{
            	for(int i=0; i<2; i++){
                	map = (Element) mappings.item(i);
                	map.setAttribute("resource", dbInfo[i+8]);
                }
            }
            
            
            XMLSerializer serializer = new XMLSerializer();
            serializer.setOutputCharStream(new java.io.FileWriter(dstFile));
            OutputFormat format = new OutputFormat();
            format.setStandalone(true);
            serializer.setOutputFormat(format);
            serializer.serialize(doc);
            
        } catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@GET
    @Path("/deletefeed")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object deletefeed(@QueryParam("feedname") String feedname, @QueryParam("db") String db) throws IOException{
		
		String[] dbInfo = db.split(",");
		Connection c = null;
		Statement statement = null;
		ResultSet rs = null;
		String agencyId = "";
		String agencyIds = "";
		String[] agencyIdList;
		
		String[][] defAgencyIds  = {{"census_congdists_trip_map","agencyid_def"},
									{"census_places_trip_map","agencyid_def"},
									{"census_urbans_trip_map","agencyid_def"},
									{"census_counties_trip_map","agencyid_def"},
									{"census_tracts_trip_map","agencyid_def"},
									{"gtfs_fare_rules","fare_agencyid"},
									{"gtfs_fare_attributes","agencyid"},
									{"gtfs_trip_stops","stop_agencyid_origin"},
									{"gtfs_stop_service_map","agencyid_def"},
									{"gtfs_route_serviceid_map","agencyid_def"},
									{"gtfs_stop_route_map","agencyid_def"},
									{"gtfs_frequencies","defaultid"},
									{"gtfs_pathways","agencyid"},
									{"gtfs_shape_points","shapeid_agencyid"},
									{"gtfs_stop_times","stop_agencyid"},
									{"gtfs_transfers","defaultid"},
									{"tempstopcodes","agencyid"},
									{"tempetriptimes","agencyid"},
									{"tempestshapes","agencyid"},
									{"tempshapes","agencyid"},
									{"gtfs_trips","serviceid_agencyid"},
									{"gtfs_calendar_dates","serviceid_agencyid"},
									{"gtfs_calendars","serviceid_agencyid"},
									{"gtfs_stops","agencyid"},
									{"gtfs_routes","defaultid"},
									{"gtfs_agencies","defaultid"},
									{"gtfs_feed_info","defaultid"}};
		
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			
			statement = c.createStatement();
			
			statement.executeUpdate("DELETE FROM gtfs_selected_feeds WHERE feedname = '"+feedname+"';");
			statement.executeUpdate("DELETE FROM gtfs_uploaded_feeds WHERE feedname = '"+feedname+"';");
			
			
			rs = statement.executeQuery("SELECT defaultid FROM gtfs_feed_info where feedname = '"+feedname+"';");
			if ( rs.next() ) {
				agencyId = rs.getString("defaultid");
			}
			
			rs = statement.executeQuery("SELECT agencyids FROM gtfs_feed_info where feedname = '"+feedname+"';");
			if ( rs.next() ) {
				agencyIds = rs.getString("agencyids");
			}
			agencyIdList = agencyIds.split(",");
//			
			for(int i=0;i<defAgencyIds.length;i++){
				System.out.println(defAgencyIds[i][0]);
				try{
					if(defAgencyIds[i][0].startsWith("temp")){
						statement.executeUpdate("DELETE FROM "+defAgencyIds[i][0]+" WHERE "+sqlString(agencyIdList,defAgencyIds[i][1])+"';");
						
					}else{
						statement.executeUpdate("ALTER TABLE "+defAgencyIds[i][0]+" DISABLE TRIGGER ALL;");
						statement.executeUpdate("DELETE FROM "+defAgencyIds[i][0]+" WHERE "+defAgencyIds[i][1]+"='"+agencyId+"';");
						statement.executeUpdate("ALTER TABLE "+defAgencyIds[i][0]+" ENABLE TRIGGER ALL;");
					}
					
				}catch (SQLException e) {
					System.out.println(e.getMessage());
				}
			}
			System.out.println("vacuum start");
			statement.executeUpdate("VACUUM");
			System.out.println("vacuum finish");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			
		} finally {
			if (rs != null) try { rs.close(); } catch (SQLException e) {}
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		PDBerror error = new PDBerror();
		error.DBError = "done";
		return error;
	}
	
	@GET
    @Path("/deleteUploadedGTFS")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object deleteUploadedGTFS() throws IOException{
		String path = DbUpdate.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		
		File gtfsFolder = new File(path+"../../src/main/webapp/resources/admin/uploads/gtfs");
		File[] files = gtfsFolder.listFiles();
//		System.out.println(files.length);
	    if(files!=null) { 
	        for(File f: files) {
	        	f.delete();
	        }
	    }
		return "done";
	}
	
	@GET
    @Path("/deleteProcessGTFS")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object deleteProcessGTFS() throws IOException{
		String path = DbUpdate.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		
		File gtfsFolder = new File(path+"../../src/main/webapp/resources/admin/processFiles/gtfs");
		File[] files = gtfsFolder.listFiles();
//		System.out.println(files.length);
	    if(files!=null) { 
	        for(File f: files) {
	        	f.delete();
	        }
	    }
		return "done";
	}
		
	
	@GET
    @Path("/checkDuplicateFeeds")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object checkDuplicateFeeds(@QueryParam("feed") String feed, @QueryParam("db") String db) throws IOException{
		for(int i=0;i<4;i++){
			feed = removeLastChar(feed);
		}
		String b = "false";
		String[] dbInfo = db.split(",");
		Connection c = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			
			statement = c.createStatement();
			
			rs = statement.executeQuery("SELECT * FROM gtfs_feed_info where feedname = '"+feed+"';");
			if ( rs.next() ) {
				b = "true";
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			
		} finally {
			if (rs != null) try { rs.close(); } catch (SQLException e) {}
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		return b;
	}
		
	
	public String sqlString(String[] ids, String column){
		String sql = "";
		for(int i=0;i<ids.length-1;i++){
			sql += column+" = '"+ids[i]+"' OR ";
		}
		sql += column+" = '"+ids[ids.length-1];
		return sql;
	}
	
	@GET
    @Path("/addfeed")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object addfeed(@QueryParam("feedname") String feedname, @QueryParam("db") String db) throws IOException{
		String [] args = new String[5];
		String[] dbInfo = db.split(",");
		args[0] = "--driverClass=\"org.postgresql.Driver\"";
		args[1] = "--url=\""+dbInfo[4]+"\"";
		args[2] = "--username=\""+dbInfo[5]+"\"";
		args[3] = "--password=\""+dbInfo[6]+"\"";
		
		String path = DbUpdate.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		File source = new File(path+"../../src/main/webapp/resources/admin/uploads/gtfs/"+feedname);
		String feed = path+"../../src/main/webapp/resources/admin/processFiles/gtfs/"+feedname;
		File target = new File(feed);
//		File[] files = gtfsFolder.listFiles();
//		System.out.println(files.length);
    	Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
//    	f.delete();
    	String message = "done";
		args[4] = feed;
		try{
			GtfsDatabaseLoaderMain.main(args);	
		}catch(Exception e){
			message = e.getMessage();
			target.delete();
			return message;
		}
		
		
		for(int i=0;i<4;i++){
			feedname = removeLastChar(feedname);
		}
//		String[] feedName = feedname.split("/");
//		String fName = feedName[feedName.length-1];
		Connection c = null;
		Statement statement = null;
		ResultSet rs = null;
		String defaultId = "";
		String agencyNames = "";
		String agencyIds = "";
		
		
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			
			statement = c.createStatement();
			rs = statement.executeQuery("SELECT * FROM gtfs_agencies Where defaultid IS NULL;");
			if ( rs.next() ) {
				String tmpAgencyId = rs.getString("id");
				rs = statement.executeQuery("SELECT * FROM gtfs_routes where agencyid = '"+tmpAgencyId+"' limit 1;");
				if ( rs.next() ) {
					defaultId = rs.getString("defaultid");
				}
				statement.executeUpdate("UPDATE gtfs_agencies SET defaultid = '"+defaultId+"' WHERE defaultid IS NULL;");
			}
			
			rs = statement.executeQuery("SELECT * FROM gtfs_agencies Where added IS NULL;");
			
			while ( rs.next() ) {
				defaultId = rs.getString("defaultid");
				agencyNames += rs.getString("name")+",";
				agencyIds += rs.getString("id")+",";
			}
			agencyNames = removeLastChar(agencyNames);
			agencyIds = removeLastChar(agencyIds);
			statement.executeUpdate("UPDATE gtfs_agencies SET added='added' WHERE added IS NULL;");
			
			rs = statement.executeQuery("SELECT * FROM gtfs_feed_info Where defaultid = '"+defaultId+"';");
			if (!rs.next() ){
				rs = statement.executeQuery("SELECT gid FROM gtfs_feed_info;");
				List<String> ids = new ArrayList<String>();
				while ( rs.next() ) {
					ids.add(rs.getString("gid"));
				}
				int gid;
				int Low = 10000;
				int High = 99999;
				do {
					Random r = new Random();
					gid = r.nextInt(High-Low) + Low;
				} while (ids.contains(Integer.toString(gid)));
				String sql = "INSERT INTO gtfs_feed_info "+
							 "(gid,publishername,publisherurl,lang,startdate,enddate,version,defaultid,agencyids,agencynames,feedname) "+
							 "VALUES ("+Integer.toString(gid)+",'N/A','N/A','N/A','N/A','N/A','N/A','"+defaultId+"','"+agencyIds+"','"+agencyNames+"','"+feedname+"')";
				statement.executeUpdate(sql);
			}else{
				statement.executeUpdate("UPDATE gtfs_feed_info SET feedname = '"+feedname+"' WHERE defaultid = '"+defaultId+"';");
			}
			statement.executeUpdate("with calendars as (select serviceid_agencyid as agencyid, min(startdate::int) as calstart, max(enddate::int) as calend from gtfs_calendars where serviceid_agencyid='"+defaultId+"' group by serviceid_agencyid),"
	        		+ "calendardates as (select serviceid_agencyid as agencyid, min(date::int) as calstart, max(date::int) as calend from gtfs_calendar_dates where serviceid_agencyid='"+defaultId+"' group by serviceid_agencyid),"
	        		+ "calendar as (select cals.agencyid, least(cals.calstart, calds.calstart) as calstart, greatest(cals.calend, calds.calend) as calend from calendars cals full join calendardates calds using(agencyid)) "
	        		+ "update gtfs_feed_info set startdate= calendar.calstart::varchar , enddate=calendar.calend::varchar from calendar where defaultid = agencyid;");
	        
			statement.executeUpdate("INSERT INTO gtfs_uploaded_feeds (feedname,username,ispublic) "
					+ "VALUES ('"+feedname+"','admin',False);");
			statement.executeUpdate("INSERT INTO gtfs_selected_feeds (username,feedname,agency_id) "
					+ "VALUES ('admin','"+feedname+"','"+defaultId+"');");
			
			//UpdateEventManager.updateTables(To BE DELETED, defaultId);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			message = e.getMessage();
		} finally {
			if (rs != null) try { rs.close(); } catch (SQLException e) {}
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
			target.delete();
		}
		
//		System.out.println("done");
//		return new TransitError(feedname +"Has been added to the database");
		return message;
	}
	
	public String removeLastChar(String str) {
    	if (str.length() > 0) {
            str = str.substring(0, str.length()-1);
        }
        return str;
    }
	
	@GET
    @Path("/updateSingleFeed")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Object updateSingleFeed(@QueryParam("dbindex") int dbindex, @QueryParam("feedname") String feedname){
		String defaultId="";
		
		Connection c = null;
		Statement statement = null;
		try {
			c = DriverManager.getConnection(Databases.connectionURLs[dbindex], Databases.usernames[dbindex], Databases.passwords[dbindex]);
			statement = c.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM gtfs_feed_info WHERE feedname = '"+feedname+"';");
			if(rs.next()){
				defaultId = rs.getString("defaultId");
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		System.out.println(defaultId);
		UpdateEventManager.updateTables(dbindex, defaultId);
		return "done";
	}
	
	@GET
    @Path("/updateFeeds")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Object updateFeeds(@QueryParam("db") String db, @QueryParam("folder") String folder){
		
		String[] dbInfo = db.split(",");
		Process pr;
		ProcessBuilder pb;
		String[] dbname = dbInfo[4].split("/");
		String name = dbname[dbname.length-1];
		String usrn = dbInfo[5];
		String pass = dbInfo[6];
		
		try {
			pb = new ProcessBuilder("cmd", "/c", "start", basePath+"TNAtoolAPI-Webapp/WebContent/admin/Development/PGSQL/dbUpdate.bat", pass, usrn, name,
					psqlPath+"psql.exe",
					basePath+"TNAtoolAPI-Webapp/WebContent/admin/Development/PGSQL/");
			pb.redirectErrorStream(true);
			pr = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "done";
	}
	
	/*@GET
    @Path("/addPsqlFunctions")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Object addPsqlFunctions(@QueryParam("db") String db){
		
		String[] dbInfo = db.split(",");
		Process pr;
		ProcessBuilder pb;
		String[] dbname = dbInfo[4].split("/");
		String name = dbname[dbname.length-1];
		String usrn = dbInfo[5];
		String pass = dbInfo[6];
		
		try {
			pb = new ProcessBuilder("cmd", "/c", "start", basePath+"TNAtoolAPI-Webapp/WebContent/admin/Development/PGSQL/addFunctions.bat", pass, usrn, name,
					psqlPath+"psql.exe",
					basePath+"TNAtoolAPI-Webapp/WebContent/admin/Development/PGSQL/");
			pb.redirectErrorStream(true);
			pr = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "done";
	}*/
	
	@GET
    @Path("/addIndex")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Object addIndex(@QueryParam("db") String db){
		
		String[] dbInfo = db.split(",");
		Connection c = null;
		Statement statement = null;
		
		String[][] defAgencyIds  = {{"census_congdists_trip_map","agencyid_def"},
									{"census_places_trip_map","agencyid_def"},
									{"census_urbans_trip_map","agencyid_def"},
									{"census_counties_trip_map","agencyid_def"},
									{"census_tracts_trip_map","agencyid_def"},
									{"gtfs_fare_rules","fare_agencyid"},
									{"gtfs_fare_attributes","agencyid"},
									{"gtfs_trip_stops","stop_agencyid_origin"},
									{"gtfs_stop_service_map","agencyid_def"},
									{"gtfs_route_serviceid_map","agencyid_def"},
									{"gtfs_stop_route_map","agencyid_def"},
									{"gtfs_frequencies","defaultid"},
									{"gtfs_pathways","agencyid"},
									{"gtfs_shape_points","shapeid_agencyid"},
									{"gtfs_stop_times","stop_agencyid"},
									{"gtfs_transfers","defaultid"},
									{"tempstopcodes","agencyid"},
									{"tempetriptimes","agencyid"},
									{"tempestshapes","agencyid"},
									{"tempshapes","agencyid"},
									{"gtfs_trips","serviceid_agencyid"},
									{"gtfs_calendar_dates","serviceid_agencyid"},
									{"gtfs_calendars","serviceid_agencyid"},
									{"gtfs_stops","agencyid"},
									{"gtfs_routes","defaultid"},
									{"gtfs_agencies","defaultid"},
									{"gtfs_feed_info","defaultid"}};
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			statement = c.createStatement();
			
			for(int i=0;i<defAgencyIds.length;i++){
				System.out.println("creating index for table: "+defAgencyIds[i][0]);
				try{
					statement.executeUpdate("CREATE INDEX defaid"+i+" ON "+defAgencyIds[i][0]+" ("+defAgencyIds[i][1]+");");
				}catch (SQLException e) {
					System.out.println(e.getMessage());
				}
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			
		} finally {
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
		
		return "done";
	}
	
	@GET
    @Path("/agencyList")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Object agencyList(@QueryParam("db") String db){
		String[] dbInfo = db.split(",");
		FeedNames fn = new FeedNames();
		Connection c = null;
		Statement statement = null;
		ResultSet rs = null;
		Boolean b = true;
		PDBerror error = new PDBerror();
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			
			statement = c.createStatement();
			rs = statement.executeQuery("SELECT * FROM gtfs_feed_info;");
			
			while ( rs.next() ) {
				fn.feeds.add(rs.getString("feedname"));
				fn.names.add(rs.getString("agencynames"));
				fn.startdates.add(rs.getString("startdate"));
				fn.enddates.add(rs.getString("enddate"));
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			
			error.DBError = e.getMessage();
			b=false;
			
		} finally {
			if (rs != null) try { rs.close(); } catch (SQLException e) {}
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
	    if(b){
	    	return fn;
	    }else{
	    	return error;
	    }
	   
	}    
	
	@GET
    @Path("/selectedFeeds")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Object selectedFeeds(@QueryParam("feeds") String feed, @QueryParam("username") String username){
		
		String[] feeds = feed.split(",");
		Connection c = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			c = DriverManager.getConnection(dbURL, dbUSER, dbPASS);
			statement = c.createStatement();
			statement.executeUpdate("DELETE FROM gtfs_selected_feeds WHERE username = '"+username+"';");
					
			for(String f: feeds){
				statement.executeUpdate("INSERT INTO gtfs_selected_feeds (username,feedname) "
						+ "VALUES ('"+username+"','"+f+"');");
			}
			statement.executeUpdate("update gtfs_selected_feeds "
					+ "set agency_id = gtfs_feed_info.defaultid "
					+ "from gtfs_feed_info "
					+ "where gtfs_selected_feeds.feedname = gtfs_feed_info.feedname;");
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			
		} finally {
			if (rs != null) try { rs.close(); } catch (SQLException e) {}
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
	    
	   return "done";
	}    
	
	@GET
    @Path("/feedlist")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
	public Object listf(@QueryParam("foldername") String directoryName, @QueryParam("db") String db) throws IOException{

	    File directory = new File(directoryName);
	    File[] fList = directory.listFiles();
	    FeedNames fn = new FeedNames();
	    //ArrayList<String> fNames = new ArrayList<String>(); 
	    try {
	    	for (File file : fList) {
		        if (file.isDirectory()) {
		            fn.feeds.add(file.getName());
		        }
		    }
	    } catch (NullPointerException e) {
	        System.err.println("IndexOutOfBoundsException: " + e.getMessage());
	    }
	    
	    String[] dbInfo = db.split(",");
		Connection c = null;
		Statement statement = null;
		ResultSet rs = null;
		try {
			c = DriverManager.getConnection(dbInfo[4], dbInfo[5], dbInfo[6]);
			
			statement = c.createStatement();
			rs = statement.executeQuery("SELECT feedname FROM gtfs_feed_info;");
			while ( rs.next() ) {
				fn.feeds.remove(rs.getString("feedname"));
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();
			
		} finally {
			if (rs != null) try { rs.close(); } catch (SQLException e) {}
			if (statement != null) try { statement.close(); } catch (SQLException e) {}
			if (c != null) try { c.close(); } catch (SQLException e) {}
		}
	    
	    return fn;
	}            
	
}