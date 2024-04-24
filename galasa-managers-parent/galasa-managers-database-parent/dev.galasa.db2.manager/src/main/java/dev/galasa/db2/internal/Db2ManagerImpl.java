/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.db2.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import dev.galasa.ICredentialsUsernamePassword;
import dev.galasa.db2.Db2ManagerException;
import dev.galasa.db2.IDb2Instance;
import dev.galasa.db2.internal.properties.Db2Credentials;
import dev.galasa.db2.internal.properties.Db2DSEInstanceName;
import dev.galasa.db2.internal.properties.Db2InstanceUrl;
import dev.galasa.db2.internal.properties.Db2PropertiesSingleton;
import dev.galasa.framework.spi.ConfigurationPropertyStoreException;
import dev.galasa.framework.spi.IConfigurationPropertyStoreService;
import dev.galasa.framework.spi.IFramework;
import dev.galasa.framework.spi.creds.CredentialsException;
import dev.galasa.framework.spi.cps.CpsProperties;

/**
 * The Db2Instance will establish a connection to a database and must be created
 * for a IDb2Schema to be established.
 * 
 * This instance also provides the connection itself back to the tester for any
 * complex usecases not covered by the methods inside this manager.
 * 
 * @author jamesdavies
 *
 */
public class Db2InstanceImpl implements IDb2Instance {
	private Connection conn;

	private static final Log logger = LogFactory.getLog(Db2InstanceImpl.class);

	public Db2InstanceImpl(IFramework framework, Db2ManagerImpl manager, String tag) throws Db2ManagerException{
		String instance = Db2DSEInstanceName.get(tag);
		try {
			//Check Db2 classes for the driver
			Class.forName("com.ibm.db2.jcc.DB2Driver");
			//Get the credentials
			ICredentialsUsernamePassword creds = (ICredentialsUsernamePassword)framework.getCredentialsService().getCredentials(Db2Credentials.get(instance));
			//Get the JDBC URL
			//URL will need to include sslConnection and sslCertLocation like 
			// sslConnection=true;sslCertLocation=./common_cacert;
			// ex. "jdbc:db2://mysever/DBD1LOC:sslConnection=true;sslCertLocation=~/.galasa/common_cacert;";
			// to set URL for the data source
			//Load the Db2 license jar, if present
			// ex. "jdbc:db2://mysever/DBD1LOC:sslConnection=true;sslCertLocation=~/.galasa/common_cacert;licenseJar=~/.galasa/db2jcc_license_cisuz.jar";
			String url = retrieveDb2UrlAndLoadDb2LicenseJar(framework, instance, tag);
			//Create connection
			conn = DriverManager.getConnection(url, creds.getUsername(), creds.getPassword());
		} catch (ClassNotFoundException e) {
			throw new Db2ManagerException("Could not load the com.ibm.db2.jcc.DB2Driver", e);
		} catch (SQLException e) {
			throw new Db2ManagerException("Failed to connect to " + instance, e);
		} catch (CredentialsException e) {
			throw new Db2ManagerException("Failed to find an Credentials for: " + instance, e);
		}
	}

	public String retrieveDb2UrlAndLoadDb2LicenseJar(IFramework framework, String instance, String tag) throws Db2ManagerException{
		//load from the JDBC Url
		final String db2url = Db2InstanceUrl.get(instance);
		String licenseJarDb2UriLoc = licenseJarLocationInDb2ConnectionURI(db2url);
		if(licenseJarLoad("Env", licenseJarDb2UriLoc)) {
			String db2urlPrime = removeLicenseJarLocationFromDb2ConnectionURI(db2url);
			return db2urlPrime;
		}
		// load from the Galasa properties settings
		try {
			IConfigurationPropertyStoreService cpss = Db2PropertiesSingleton.cps();
			String licenseJarCpsLoc = cpss.getProperty("instance", "license", tag);
			if (licenseJarLoad("CPS", licenseJarCpsLoc)) {
				return db2url;
			}
		} catch (ConfigurationPropertyStoreException e) {
			throw new Db2ManagerException("Load the ConfigurationPropertyStore", e);
		}
		//load JVM properties
		String licenseJarJvmLoc = getDb2LicenseJarJVMPropertyLocation();
		if (licenseJarLoad("JVM", licenseJarJvmLoc)) {
			return db2url;
		}
		//load System Environment
		String licenseJarEnvLoc = getDb2LicenseJarSystemEnvironmentLocation();
		if (licenseJarLoad("Env", licenseJarEnvLoc)) {
			return db2url;
		}
		//load default
		String licenseJarDefaultLoc = getDb2LicenseJarDefaultLocation();
		licenseJarLoad("Default", licenseJarDefaultLoc);
		return db2url;
	}

	private boolean licenseJarLoad(String origin, String licenseJarLoc) {
		logger.info("licenseJarLoad()  location: " + ((licenseJarLoc == null) ? "null" : licenseJarLoc));
		System.out.println("licenseJarLoad()  location: " + ((licenseJarLoc == null) ? "null" : licenseJarLoc));
		URI uri = getFileURI(licenseJarLoc);
		if (uri != null) {
			try {
				install(uri);
			} catch (Exception e) {
				//may not be fatal? 
				logger.info("licenseJarLoad() Exception on install: "+ e.getMessage());
				System.out.println("licenseJarLoad() Exception on install: "+ e.getMessage());
				e.printStackTrace();
			}
			return checkLicenseIsInstalled();
		}
		return false;
	}

	public boolean checkLicenseIsInstalled() {
		try {
			// Check Db2 classes for the license
			Class<?> clazz = Class.forName("com.ibm.db2.jcc.licenses.DB2zOS");
			logger.info("Loaded the Db2 license jar classes");
			System.out.println("Loaded the Db2 license jar classes");
			return true;
		} catch (ClassNotFoundException | LinkageError e) {
			// ClassNotFoundException - if the class cannot be located
			// ExceptionInInitializerError - if the initialization fails
			// LinkageError - if the linkage fails
			logger.info("Could not load the Db2 license jar classes");
			System.out.println("Could not load the Db2 license jar classes");

		}
		return false;
	}

	public String getDb2LicenseJarDefaultLocation() {
		// Default
		String licenseJarDefaultLocation = "~/.galasa/db2jcc_license_cisuz.jar";
		logger.info("getDb2LicenseJarLocation() default location: " + licenseJarDefaultLocation);
		System.out.println("getDb2LicenseJarLocation() default location: " + licenseJarDefaultLocation);
		return licenseJarDefaultLocation;
	}

	public String getDb2LicenseJarSystemEnvironmentLocation() {
		// Check System level environment variables
		try {
			String licenseJarEnvLoc = System.getenv("GALASADB2JCCLICENSEJAR");
			if (null != licenseJarEnvLoc) {
				logger.info("getDb2LicenseJarLocation() environment location: " + licenseJarEnvLoc);
				System.out.println("getDb2LicenseJarLocation() environment location: " + licenseJarEnvLoc);
				return licenseJarEnvLoc;
			}
		} catch (RuntimeException e) {
			logger.info("getDb2LicenseJarLocation() RuntimeException on GALASADB2JCCLICENSEJAR retrieval");
			System.out.println("getDb2LicenseJarLocation() RuntimeException on GALASADB2JCCLICENSEJAR retrieval");
			e.printStackTrace();
		}
		return null;
	}

	public String getDb2LicenseJarJVMPropertyLocation() {
		// Check JVM level properties
		try {
			String licenseJarPropLoc = System.getProperty("GALASADB2JCCLICENSEJAR");
			if (null != licenseJarPropLoc) {
				logger.info("getDb2LicenseJarLocation() property location: " + licenseJarPropLoc);
				System.out.println("getDb2LicenseJarLocation() property location: " + licenseJarPropLoc);
				return licenseJarPropLoc;
			}
		} catch (RuntimeException e) {
			logger.info("getDb2LicenseJarLocation() RuntimeException on GALASADB2JCCLICENSEJAR retrieval");
			System.out.println("getDb2LicenseJarLocation() RuntimeException on GALASADB2JCCLICENSEJAR retrieval");
			e.printStackTrace();
		}
		return null;
	}

	public String licenseJarLocationInDb2ConnectionURI(String db2url) {
		String[] values = db2url.split(";");
		for (String value : values) {
			if (value.contains("db2jcc_license_cisuz.jar")) {
				String[] keyval = value.split("=");
				String licUriString = keyval[keyval.length - 1];
				return licUriString;
			}
		}
		return null;
	}

	public String removeLicenseJarLocationFromDb2ConnectionURI(String db2url) {
		String db2urlPrime = db2url;
		String[] values = db2url.split(";");
		for (String value : values) {
			if (value.contains("db2jcc_license_cisuz.jar")) {
				db2urlPrime.replace(";" + value, "");
				break;
			}
		}
		return db2urlPrime;
	}

	private URI getFileURI(String fileString) {
		logger.info("getFileURI(String) string = " + (fileString == null ? "null" : "-->" + fileString + "<--"));
		System.out.println("getFileURI(String) string = " + (fileString == null ? "null" : "-->" + fileString + "<--"));
		URI uri = null;
		try {
			File file = new File(fileString);
			if (file.isFile()) {
				try {
					uri = file.toURI();
				} catch (SecurityException se) {
					logger.info("getFileURI(String) SecurityException");
					System.out.println("getFileURI(String) SecurityException");
					se.printStackTrace();
				}
			} else {
				try {
					URI uriCheck = new URI(fileString);
					file = new File(uriCheck);
					if (file.isFile()) {
						uri = uriCheck;
					}
				} catch (URISyntaxException | IllegalArgumentException ue) {
					logger.info("getFileURI(String) string was not a URI");
					System.out.println("getFileURI(String) string was not a URI");
					ue.printStackTrace();
				}
			}
		} catch (NullPointerException npe) {
			logger.info("getFileURI(String) string was null");
			System.out.println("getFileURI(String) string was null");
			npe.printStackTrace();
		} catch (SecurityException se) {
			logger.info("getFileURI(String) SecurityException o file constructor");
			System.out.println("getFileURI(String) SecurityException o file constructor");
			se.printStackTrace();
		}
		return uri;
	}

	private BundleContext getBundleContext() {
		return FrameworkUtil.getBundle(Db2InstanceImpl.class).getBundleContext();
	}

	private void install(URI fileURI) throws Exception {
	    String fileString = fileURI.toString();
	    Bundle bundle = getBundleContext().installBundle(fileString);
	    bundle.start();
	    bundle.update();
	}

	private void uninstall(URI fileURI) throws Exception {
		String fileString = fileURI.toString();
		Bundle bundle = getBundleContext().installBundle(fileString);
		bundle.uninstall();
	}

	/**
	 * Retrieves the database name from the connected database
	 */
	@Override
	public String getDatabaseName() throws Db2ManagerException {
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("VALUES CURRENT SERVER");
			rs.next();
			return rs.getString(1);
		} catch (SQLException e) {
			throw new Db2ManagerException("Failed to retrieve database name", e);
		}

	}

	/**
	 * Provides the standard java.sql.Connection
	 */
	public Connection getConnection() {
		return this.conn;
	}

}
