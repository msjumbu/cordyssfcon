package com.cordys.coe.salesforce.metadata.metadata;

import javax.wsdl.WSDLException;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.cordys.coe.salesforce.metadata.wsdl.WSDLCreator;
import com.cordys.coe.salesforce.metadata.xsd.XSDDumper;
import com.cordys.coe.salesforce.metadata.xsd.XSDElement;
import com.cordys.coe.salesforce.metadata.xsd.XSDSchema;
import com.cordys.coe.salesforce.metadata.xsd.XSDSequence;
import com.cordys.coe.salesforce.metadata.xsd.XSDType;

/**
 * @author Senthil Kumar Murugesan
 * 
 */
public class MethodGenerator {

	String m_serviceName;
	String m_operationName;
	String m_nameSpace;
	public MethodGenerator(String serviceName, String operationName, String nameSpace) {
		m_serviceName = serviceName;
		m_operationName = operationName;
		m_nameSpace = nameSpace;
	}
	
	public String generateWSDL(int inputXML, int outputXML) throws WSDLException {
		
		XSDSchema xs = new XSDSchema();
		xs.setTargetNamespace(m_nameSpace);

		XSDElement xdInput = new XSDElement();
        xdInput.setName(new QName(m_nameSpace, m_operationName));
        xs.addElement(xdInput);

        XSDType xtComplex = new XSDType();
        xtComplex.setType(XSDType.TYPE_COMPLEX);

        XSDSequence xsAll = new XSDSequence();
        xsAll.setAll(true);
        xtComplex.setSequence(xsAll);
        xdInput.setType(xtComplex);

        
        SchemaGenerator sg = new SchemaGenerator(m_nameSpace);
		sg.generateInputSchema(xs,xsAll,inputXML);

		XSDElement xdOutput = new XSDElement();
        xdOutput.setName(new QName(sg.getNamespace(), m_operationName + "Response"));
        xs.addElement(xdOutput);

        XSDType xtComplex1 = new XSDType();
        xtComplex1.setType(XSDType.TYPE_COMPLEX);

        XSDSequence xsAll1 = new XSDSequence();
        xsAll1.setAll(true);
        xtComplex1.setSequence(xsAll1);
        xdOutput.setType(xtComplex1);

        sg.generateInputSchema(xs,xsAll1,outputXML);
        
        WSDLCreator wsdl = new WSDLCreator(m_serviceName, m_nameSpace,
        "com.eibus.web.soap.Gateway.wcp");
		
		XSDDumper xdDumper = new XSDDumper();
        xdDumper.declareNamespace("xsd", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        xdDumper.declareNamespace("tns", m_nameSpace);

        Element schema = xdDumper.convert(xs);
        schema.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:tns", m_nameSpace);

        // Make sure that the elements are qualified.
        schema.setAttribute("attributeFormDefault", "unqualified");
        schema.setAttribute("elementFormDefault", "qualified");
        wsdl.addMethod(m_operationName);

        // Attach the schema to the WSDL.
        wsdl.setSchema(schema);
        String finalWSDL = wsdl.createWSDL();
        
        return finalWSDL;
	}
}
