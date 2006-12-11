package fedora.utilities.install;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Properties;

import fedora.server.config.ServerConfiguration;
import fedora.server.config.ServerConfigurationParser;
import fedora.server.security.BESecurityConfig;
import fedora.server.security.DefaultRoleConfig;
import fedora.server.security.servletfilters.xmluserfile.FedoraUsers;
import fedora.server.security.servletfilters.xmluserfile.User;
import fedora.utilities.ExecUtility;
import fedora.utilities.FileUtils;
import fedora.utilities.Zip;

public class FedoraHome {
	private Distribution _dist;
	private InstallOptions _opts;
	private File _installDir;
	
	public FedoraHome(Distribution dist, InstallOptions opts) {
		_dist = dist;
		_opts = opts;
		_installDir = new File(_opts.getValue(InstallOptions.FEDORA_HOME));
	}
	
	public void install() throws InstallationFailedException {
		unpack();
		configure();
	}
	
	
	/**
	 * Unpacks the contents of the FEDORA_HOME directory from the Distribution.
	 * @throws InstallationFailedException
	 */
	private void unpack() throws InstallationFailedException {
		System.out.println("Preparing FEDORA_HOME...");
		
		if (!_installDir.exists() && !_installDir.mkdirs()) {
			throw new InstallationFailedException("Unable to create FEDORA_HOME: " + _installDir.getAbsolutePath());
		}
		if (!_installDir.isDirectory()) {
			throw new InstallationFailedException(_installDir.getAbsolutePath() + " is not a directory");
		}
		try {
			Zip.unzip(_dist.get(Distribution.FEDORA_HOME), _installDir);
            setScriptsExecutable(new File(_installDir, "client" + File.separator + "bin"));
            setScriptsExecutable(new File(_installDir, "server" + File.separator + "bin"));
		} catch (IOException e) {
			throw new InstallationFailedException(e.getMessage(), e);
		}
	}
	
	/**
	 * Sets various configuration files based on InstallOptions
	 * @throws InstallationFailedException
	 */
	private void configure() throws InstallationFailedException {
		configureFCFG();
		configureFedoraUsers();
		configureBeSecurity();
		configureXACML();
	}
	
	private void configureFCFG() throws InstallationFailedException {
    	System.out.println("\tConfiguring fedora.fcfg");
    	File fcfgBase = new File(_installDir, "server/fedora-internal-use/config/fedora-base.fcfg");
    	File fcfg = new File(_installDir, "server/config/fedora.fcfg");
        
        Properties props = new Properties();
        if (_opts.getValue(InstallOptions.TOMCAT_HTTP_PORT) != null) {
        	props.put("server.fedoraServerPort", _opts.getValue(InstallOptions.TOMCAT_HTTP_PORT));
        }
        if (_opts.getValue(InstallOptions.TOMCAT_SHUTDOWN_PORT) != null) {
        	props.put("server.fedoraShutdownPort", _opts.getValue(InstallOptions.TOMCAT_SHUTDOWN_PORT));
        }
        if (_opts.getValue(InstallOptions.TOMCAT_SSL_PORT) != null) {
        	props.put("server.fedoraRedirectPort", _opts.getValue(InstallOptions.TOMCAT_SSL_PORT));
        }
        
        String database = _opts.getValue(InstallOptions.DATABASE);
        String dbPoolName = "";
        String backslashIsEscape = "true";
        if (database.equals(InstallOptions.MCKOI) || database.equals(InstallOptions.INCLUDED)) {
        	dbPoolName = "localMcKoiPool";
        	backslashIsEscape = "false";
        } else if (database.equals(InstallOptions.MYSQL)) {
        	dbPoolName = "localMySQLPool";
        } else if (database.equals(InstallOptions.ORACLE)) {
        	dbPoolName = "localOraclePool";
        	backslashIsEscape = "false";
        } else if (database.equals(InstallOptions.POSTGRESQL)) {
        	dbPoolName = "localPostgresqlPool";
        } else {
        	throw new InstallationFailedException("unable to configure for unknown database: " + database);
        }
        props.put("module.fedora.server.storage.DOManager.storagePool", dbPoolName);
    	props.put("module.fedora.server.search.FieldSearch.connectionPool", dbPoolName);
    	props.put("module.fedora.server.storage.ConnectionPoolManager.poolNames", dbPoolName);
    	props.put("module.fedora.server.storage.ConnectionPoolManager.defaultPoolName", dbPoolName);
    	props.put("module.fedora.server.storage.lowlevel.ILowlevelStorage.backslash_is_escape", backslashIsEscape);
        props.put("datastore." + dbPoolName + ".jdbcURL", _opts.getValue(InstallOptions.DATABASE_JDBCURL));
        props.put("datastore." + dbPoolName + ".dbUsername", _opts.getValue(InstallOptions.DATABASE_USERNAME));
        props.put("datastore." + dbPoolName + ".dbPassword", _opts.getValue(InstallOptions.DATABASE_PASSWORD));
        props.put("datastore." + dbPoolName + ".jdbcDriverClass", _opts.getValue(InstallOptions.DATABASE_DRIVERCLASS));
        
        if (_opts.getBooleanValue(InstallOptions.XACML_ENABLED, true)) {
        	props.put("module.fedora.server.security.Authorization.ENFORCE-MODE", "enforce-policies");
        } else {
        	props.put("module.fedora.server.security.Authorization.ENFORCE-MODE", "permit-all-requests");
        }
        
        props.put("module.fedora.server.access.Access.doMediateDatastreams", _opts.getValue(InstallOptions.APIA_AUTH_REQUIRED));
        
        try {
	        FileInputStream fis = new FileInputStream(fcfgBase);
	        ServerConfiguration config = new ServerConfigurationParser(fis).parse();
	        config.applyProperties(props);
	        config.serialize(new FileOutputStream(fcfg));
        } catch(IOException e) {
    		throw new InstallationFailedException(e.getMessage(), e);
    	}
    }
	
	private void configureFedoraUsers() throws InstallationFailedException {
    	FedoraUsers fu = FedoraUsers.getInstance();
    	for (User user : fu.getUsers()) {
			if (user.getName().equals("fedoraAdmin")) {
				user.setPassword(_opts.getValue(InstallOptions.FEDORA_ADMIN_PASS));
			}
		}
    	
    	try {
    		Writer outputWriter = new BufferedWriter(new FileWriter(FedoraUsers.fedoraUsersXML));
			fu.write(outputWriter);
			outputWriter.close();
		} catch (IOException e) {
			throw new InstallationFailedException(e.getMessage(), e);
		}
    }
    
    private void configureBeSecurity() throws InstallationFailedException {
    	System.out.println("\tInstalling beSecurity");
    	File beSecurity = new File(_installDir, "/server/config/beSecurity.xml");
    	boolean apiaAuth = _opts.getBooleanValue(InstallOptions.APIA_AUTH_REQUIRED, false);
    	boolean apiaSSL = _opts.getBooleanValue(InstallOptions.APIA_SSL_REQUIRED, false);
    	//boolean apimSSL = _opts.getBooleanValue(InstallOptions.APIM_SSL_REQUIRED, false);
    	
    	String[] ipList;
    	String host = _opts.getValue(InstallOptions.FEDORA_SERVERHOST);
    	if (host != null  && host.length() != 0 && 
    			!(host.equals("localhost") || host.equals("127.0.01"))) {
    		ipList = new String[] {"127.0.0.1", host};
    	} else {
    		ipList = new String[] {"127.0.0.1"};
    	}

    	PrintWriter pwriter;
		try {
			pwriter = new PrintWriter(new FileOutputStream(beSecurity));
		} catch (FileNotFoundException e) {
			throw new InstallationFailedException(e.getMessage(), e);
		}
    	BESecurityConfig becfg = new BESecurityConfig();

    	becfg.setDefaultConfig(new DefaultRoleConfig());
    	becfg.setInternalBasicAuth(new Boolean(apiaAuth));
    	becfg.setInternalIPList(ipList);
    	becfg.setInternalPassword("changeme");
    	becfg.setInternalSSL(new Boolean(apiaSSL));
    	becfg.setInternalUsername("fedoraIntCallUser");
    	becfg.write(true, true, pwriter);
    	pwriter.close();
    }
    
    /**
     * Add the serverHost to the following XACML policies:
     * 		deny-apim-if-not-localhost.xml
     * 		deny-reloadPolicies-if-not-localhost.xml
     * 		deny-serverShutdown-if-not-localhost.xml
     * if not already present.
     * 
     * @throws InstallationFailedException
     */
    private void configureXACML() throws InstallationFailedException {
    	String host = _opts.getValue(InstallOptions.FEDORA_SERVERHOST);
    	String[] policies = new String[] {
    			"deny-apim-if-not-localhost.xml",
    			"deny-reloadPolicies-if-not-localhost.xml",
    			"deny-serverShutdown-if-not-localhost.xml"
    	};
    	File defaultPolicyDir = new File(_installDir + "/server/fedora-internal-use/fedora-internal-use-repository-policies-approximating-2.0");
    	try {
    		for (String policy : policies) {
    			File pFile = new File(defaultPolicyDir, policy);
    			XACMLPolicy xacml = new XACMLPolicy(pFile, _opts);
    			xacml.addServerHost(host);
    			xacml.write(pFile.getAbsolutePath());
    		}
		} catch (Exception e) {
			throw new InstallationFailedException(e.getMessage(), e);
		}
    }
	
	/**
     * Make scripts (ending with .sh) executable on *nix systems.
     */
    public static void setScriptsExecutable(File dir) {
		String os = System.getProperty("os.name");
		if (os != null && !os.startsWith("Windows")) {
			FileFilter filter = FileUtils.getSuffixFileFilter(".sh");
			setExecutable(dir, filter);
		}
    }
    
    private static void setExecutable(File dir, FileFilter filter) {
    	File[] files;
    	if (filter != null) {
    		files = dir.listFiles(filter);
    	} else {
    		files = dir.listFiles();
    	}
		for (int i = 0; i < files.length; i++) {
			ExecUtility.exec("chmod +x " + files[i].getAbsolutePath());
		}
    }
}
