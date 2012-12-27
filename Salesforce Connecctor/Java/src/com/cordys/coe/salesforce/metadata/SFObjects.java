package com.cordys.coe.salesforce.metadata;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.wsdl.WSDLException;

import com.cordys.coe.salesforce.metadata.metadata.MethodGenerator;
import com.eibus.xml.nom.Document;
import com.eibus.xml.nom.Node;
import com.eibus.xml.nom.XMLException;
import com.eibus.xml.xpath.XPath;
import com.sforce.soap.partner.DescribeGlobalResult;
import com.sforce.soap.partner.DescribeGlobalSObjectResult;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.PicklistEntry;
import com.sforce.ws.ConnectionException;

/**
 * @author Senthil Kumar Murugesan
 * 
 */

public class SFObjects extends ASFObjectInfo {

	private String method = "";

	public SFObjects(PartnerConnection connection, String methodName) {
		super(connection);
		this.method = methodName;
		// TODO Auto-generated constructor stub
	}

	@Override
	public int listObjects(int responseNode) throws ConnectionException {
		DescribeGlobalResult dgr = this.getPartnerConnection().describeGlobal();
		int dgrNode = Node.createElement("DescribeGlobalResult", responseNode);
		Node.setAttribute(dgrNode, "encoding", dgr.getEncoding());
		Node.setAttribute(dgrNode, "maxBatchSize",
				Integer.toString(dgr.getMaxBatchSize()));
		DescribeGlobalSObjectResult[] sObjects = dgr.getSobjects();
		for (DescribeGlobalSObjectResult sObject : sObjects) {
			populateGlobalObjectDetails(dgrNode, sObject);
		}
		return 0;
	}

	private void populateGlobalObjectDetails(int dgrNode,
			DescribeGlobalSObjectResult sObject) {
		int sObjectsNode = Node.createElement("sobjects", dgrNode);
		Node.setDataElement(sObjectsNode, "activateable",
				Boolean.toString(sObject.getActivateable()));
		Node.setDataElement(sObjectsNode, "createable",
				Boolean.toString(sObject.getUpdateable()));
		Node.setDataElement(sObjectsNode, "custom",
				Boolean.toString(sObject.getCustom()));
		Node.setDataElement(sObjectsNode, "customSetting",
				Boolean.toString(sObject.getCustomSetting()));
		Node.setDataElement(sObjectsNode, "deletable",
				Boolean.toString(sObject.getDeletable()));
		Node.setDataElement(sObjectsNode, "deprecatedAndHidden",
				Boolean.toString(sObject.getDeprecatedAndHidden()));
		Node.setDataElement(sObjectsNode, "feedEnabled",
				Boolean.toString(sObject.getFeedEnabled()));
		Node.setDataElement(sObjectsNode, "keyPrefix", sObject.getKeyPrefix());
		Node.setDataElement(sObjectsNode, "label", sObject.getLabel());
		Node.setDataElement(sObjectsNode, "labelPlural",
				sObject.getLabelPlural());
		Node.setDataElement(sObjectsNode, "layoutable",
				Boolean.toString(sObject.getLayoutable()));
		Node.setDataElement(sObjectsNode, "mergeable",
				Boolean.toString(sObject.getMergeable()));
		Node.setDataElement(sObjectsNode, "name", sObject.getName());
		Node.setDataElement(sObjectsNode, "queryable",
				Boolean.toString(sObject.getQueryable()));
		Node.setDataElement(sObjectsNode, "replicateable",
				Boolean.toString(sObject.getReplicateable()));
		Node.setDataElement(sObjectsNode, "retrieveable",
				Boolean.toString(sObject.getRetrieveable()));
		Node.setDataElement(sObjectsNode, "searchable",
				Boolean.toString(sObject.getSearchable()));
		Node.setDataElement(sObjectsNode, "triggerable",
				Boolean.toString(sObject.getTriggerable()));
		Node.setDataElement(sObjectsNode, "undeletable",
				Boolean.toString(sObject.getUndeletable()));
		Node.setDataElement(sObjectsNode, "updateable",
				Boolean.toString(sObject.getUpdateable()));
	}

	@Override
	public int objectDefinition(String[] sObjectTypes, int responseNode)
			throws ConnectionException {

		DescribeSObjectResult[] dsors = this.getPartnerConnection()
				.describeSObjects(sObjectTypes);

		for (DescribeSObjectResult dsor : dsors) {
			Field[] fields = dsor.getFields();
			int dsorNode = Node.createElement("DescribeSObjectResult",
					responseNode);
			Node.setDataElement(dsorNode, "name", dsor.getName());
			Node.setDataElement(dsorNode, "label", dsor.getLabel());
			for (Field field : fields) {
				populateFieldDetails(dsorNode, field);
			}
		}
		return 0;
	}

	private void populateFieldDetails(int dsorNode, Field field) {
		int fieldsNode = Node.createElement("fields", dsorNode);
		Node.setDataElement(fieldsNode, "autonumber",
				Boolean.toString(field.getAutoNumber()));
		Node.setDataElement(fieldsNode, "byteLength",
				Integer.toString(field.getByteLength()));
		Node.setDataElement(fieldsNode, "calculated",
				Boolean.toString(field.getCalculated()));
		Node.setDataElement(fieldsNode, "caseSensitive",
				Boolean.toString(field.getCaseSensitive()));
		Node.setDataElement(fieldsNode, "controllerName",
				field.getControllerName());
		Node.setDataElement(fieldsNode, "createable",
				Boolean.toString(field.getCreateable()));
		Node.setDataElement(fieldsNode, "custom",
				Boolean.toString(field.getCustom()));
		Node.setDataElement(fieldsNode, "defaultedOnCreate",
				Boolean.toString(field.getDefaultedOnCreate()));
		Node.setDataElement(fieldsNode, "defaultValueFormula",
				field.getDefaultValueFormula());
		Node.setDataElement(fieldsNode, "dependentPicklist",
				Boolean.toString(field.getDependentPicklist()));
		Node.setDataElement(fieldsNode, "deprecatedAndHidden",
				Boolean.toString(field.getDeprecatedAndHidden()));
		Node.setDataElement(fieldsNode, "digits",
				Integer.toString(field.getDigits()));
		Node.setDataElement(fieldsNode, "displayLocationInDecimal",
				Boolean.toString(field.getDisplayLocationInDecimal()));
		Node.setDataElement(fieldsNode, "filterable",
				Boolean.toString(field.getFilterable()));
		Node.setDataElement(fieldsNode, "formula", field.getCalculatedFormula());
		Node.setDataElement(fieldsNode, "groupable",
				Boolean.toString(field.getGroupable()));
		Node.setDataElement(fieldsNode, "htmlFormatted",
				Boolean.toString(field.getHtmlFormatted()));
		Node.setDataElement(fieldsNode, "idLookup",
				Boolean.toString(field.getIdLookup()));
		Node.setDataElement(fieldsNode, "inlineHelpText",
				field.getInlineHelpText());
		Node.setDataElement(fieldsNode, "label", field.getLabel());
		Node.setDataElement(fieldsNode, "length",
				Integer.toString(field.getLength()));
		Node.setDataElement(fieldsNode, "name", field.getName());
		Node.setDataElement(fieldsNode, "nameField",
				Boolean.toString(field.getNameField()));
		Node.setDataElement(fieldsNode, "namePointing",
				Boolean.toString(field.getNamePointing()));
		Node.setDataElement(fieldsNode, "nillable",
				Boolean.toString(field.getNillable()));
		Node.setDataElement(fieldsNode, "permissionable",
				Boolean.toString(field.getPermissionable()));

		if (field.getType() == FieldType.picklist) {
			PicklistEntry[] picklistValues = field.getPicklistValues();
			if (picklistValues != null && picklistValues[0] != null) {
				for (int k = 0; k < picklistValues.length; k++) {
					int enumNode = Node.createElement("picklistValues",
							fieldsNode);
					Node.setDataElement(enumNode, "label",
							picklistValues[k].getLabel());
					Node.setDataElement(enumNode, "value",
							picklistValues[k].getValue());
					Node.setDataElement(enumNode, "active",
							Boolean.toString(picklistValues[k].getActive()));
					Node.setDataElement(enumNode, "defaultValue", Boolean
							.toString(picklistValues[k].getDefaultValue()));
				}
			}
		}

		Node.setDataElement(fieldsNode, "precision",
				Integer.toString(field.getPrecision()));
		Node.setDataElement(fieldsNode, "relationshipName",
				field.getRelationshipName());
		Node.setDataElement(fieldsNode, "relationshipOrder",
				Integer.toString(field.getRelationshipOrder()));

		String[] refTos = field.getReferenceTo();
		for (String refTo : refTos) {
			Node.setDataElement(fieldsNode, "referenceTo", refTo);
		}
		Node.setDataElement(fieldsNode, "restrictedPicklist",
				Boolean.toString(field.getRestrictedPicklist()));
		Node.setDataElement(fieldsNode, "scale",
				Integer.toString(field.getScale()));
		Node.setDataElement(fieldsNode, "soapType", field.getSoapType().name());
		Node.setDataElement(fieldsNode, "sortable",
				Boolean.toString(field.getSortable()));
		Node.setDataElement(fieldsNode, "type", field.getType().name());
		Node.setDataElement(fieldsNode, "unique",
				Boolean.toString(field.getUnique()));
		Node.setDataElement(fieldsNode, "updateable",
				Boolean.toString(field.getUpdateable()));
		Node.setDataElement(fieldsNode, "writeRequiresMasterRead",
				Boolean.toString(field.getWriteRequiresMasterRead()));

		Node.setDataElement(fieldsNode, "type", field.getType().name());
		Node.setDataElement(fieldsNode, "length",
				Integer.toString(field.getLength()));

	}

	@Override
	public int process(int requestNode, int responseNode)
			throws ConnectionException {
		if (this.method.equals("DescribeGlobal")) {
			return listObjects(responseNode);
		} else if (this.method.equals("DescribeSObjects")) {
			int[] objTypes = XPath.getMatchingNodes(".//sObjectType", null,
					requestNode);
			ArrayList<String> sObjects = new ArrayList<String>();

			for (int objType : objTypes) {
				String sObject = Node.getDataWithDefault(objType, "");
				if (!sObject.isEmpty()) {
					sObjects.add(sObject);
				}
			}

			String[] temp = new String[sObjects.size()];
			for (int i = 0; i < sObjects.size(); i++) {
				temp[i] = sObjects.get(i);
			}

			return objectDefinition(temp, responseNode);
		} else if (this.method.equals("GenerateWSDL")) {
			try {
				generateWSDL(requestNode, responseNode);
			} catch (XMLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WSDLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return 0;
	}

	private void generateWSDL(int requestXML, int responseXML)
			throws ConnectionException, WSDLException, XMLException,
			UnsupportedEncodingException {
		// Getting the parameters from the requestXML
		String wsdl = null;
		String interfacename = Node.getData(XPath.getFirstMatch(
				".//Interfacename", null, requestXML));
		String namespace = Node.getData(XPath.getFirstMatch(".//Namespace",
				null, requestXML));
		String operations = Node.getData(XPath.getFirstMatch(".//Operation",
				null, requestXML));
		String[] operation = operations.split(",");

		String ObjectType = Node.getData(XPath.getFirstMatch(".//ObjectType",
				null, requestXML));
		String[] sObjectTypes = { ObjectType };
		Document oDoc = new Document();
		int objectTypeXML = oDoc.createElement("input");
		objectDefinition(sObjectTypes, objectTypeXML);

		// int outputXML =
		// oDoc.parseString("<SaveResult><id/><success/><errors/><errors/></SaveResult>");

		// calling the wsdl generator to generate the wsdl

		for (String op : operation) {
			if (op.length() > 0) {
				int WSDLNode = Node.createElement("WSDL", responseXML);
				MethodGenerator mg = new MethodGenerator(interfacename, op
						+ ObjectType, namespace);
				String implementation = "<implementation xmlns:c='http://schemas.cordys.com/cws/1.0' xmlns:SOAP='http://schemas.xmlsoap.org/soap/envelope/' xmlns='' type='SalesforceSOAP'>";
				implementation += "<SObject>" + ObjectType + "</SObject>";
				implementation += "<Operation>" + op + "</Operation>";
				if (op.equals("get")) {
					String input = "<DescribeSObjectsResponse><DescribeSObjectResult><name>"
							+ ObjectType
							+ "</name><fields><name>Id</name><length>18</length><type>id</type></fields></DescribeSObjectResult></DescribeSObjectsResponse>";
					int[] fields = XPath.getMatchingNodes(".//fields", null,
							objectTypeXML);
					String fieldNames = "";
					for (int j = 0; j < fields.length; j++) {
						if (j < fields.length - 1) {
							fieldNames += Node.getDataWithDefault(XPath
									.getFirstMatch(".//name", null, fields[j]),
									"")
									+ ",";
						} else {
							fieldNames += Node.getDataWithDefault(XPath
									.getFirstMatch(".//name", null, fields[j]),
									"");
						}
					}
					String qString = "SELECT " + fieldNames + " FROM "
							+ ObjectType;
					implementation += "<query>" + qString + "</query>";
					int inXML = oDoc.parseString(input);
					wsdl = mg.generateWSDL(inXML, objectTypeXML);
				} else {
					wsdl = mg.generateWSDL(objectTypeXML, objectTypeXML);
				}
				implementation += "</implementation>";
				int wsdlNode = 0;
				int implNode = 0;
				wsdlNode = oDoc.parseString(wsdl);
				implNode = oDoc.parseString(implementation);
				Node.appendToChildren(Node.clone(Node.getFirstChildElement(objectTypeXML), true), implNode);

				Node.appendToChildren(wsdlNode, WSDLNode);
				Node.appendToChildren(implNode, WSDLNode);
			}
		}
	}

}
