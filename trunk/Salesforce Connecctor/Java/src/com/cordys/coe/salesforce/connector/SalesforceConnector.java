/**
 * 
 */
package com.cordys.coe.salesforce.connector;

import com.eibus.soap.ApplicationConnector;
import com.eibus.soap.ApplicationTransaction;
import com.eibus.soap.Processor;
import com.eibus.soap.SOAPTransaction;
import com.eibus.util.logger.CordysLogger;
import com.eibus.util.logger.Severity;
import com.eibus.xml.nom.Node;
import com.eibus.xml.xpath.XPath;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

/**
 * @author Senthil Kumar Murugesan
 * 
 */
public class SalesforceConnector extends ApplicationConnector {

	String userName = "";
	String password = "";
	static PartnerConnection connection;

	public static PartnerConnection getConnection() {
		return connection;
	}

	public static void setConnection(PartnerConnection connection) {
		SalesforceConnector.connection = connection;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getSecCode() {
		return secCode;
	}

	String secCode = "";
	
	
	public static CordysLogger logger = CordysLogger
			.getCordysLogger(SalesforceConnector.class);

	/**
	 * 
	 */
	public SalesforceConnector() {
		// TODO Auto-generated constructor stub
	}
	
	public ApplicationTransaction createTransaction(
			SOAPTransaction soapTransaction) {
		return new SFTransaction(this);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void open(Processor processor) {
		super.open(processor);
		try {
			ReadConfiguration(processor);
			connect();
		} catch (Exception e) {
			logger.log(Severity.ERROR, e.getMessage());
			close(processor);
			e.printStackTrace();
		}
	}

	private boolean connect() {
		ConnectorConfig config = new ConnectorConfig();
		config.setUsername(userName);
		config.setPassword(password+secCode);
		try {

			connection = Connector.newConnection(config);

			// display some current settings
			logger.debug("Auth EndPoint: " + config.getAuthEndpoint());
			logger.debug("Service EndPoint: " + config.getServiceEndpoint());
			logger.debug("Username: " + config.getUsername());
			logger.debug("SessionId: " + config.getSessionId());
		} catch (ConnectionException e1) {
			e1.printStackTrace();
		}
		return false;
	}
	
	private void ReadConfiguration(Processor processor) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Reading Configuration");
		}

		int configurationode = processor.getProcessorConfigurationNode();
		if (logger.isDebugEnabled()) {
			logger.debug(Node.writeToString(configurationode, true));
		}
		this.userName = Node.getDataWithDefault(XPath.getFirstMatch(
				".//sfUName", null, configurationode), "");
		this.password = Node.getDataWithDefault(XPath.getFirstMatch(
				".//sfPasswd", null, configurationode), "");
		this.secCode = Node.getDataWithDefault(XPath.getFirstMatch(
				".//sfSecCode", null, configurationode),"");
	}

}
