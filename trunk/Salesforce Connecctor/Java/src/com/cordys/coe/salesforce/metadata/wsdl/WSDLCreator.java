/**
 * Copyright 2005 Cordys R&D B.V. 
 * 
 * This file is part of the Cordys SAP Connector. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package com.cordys.coe.salesforce.metadata.wsdl;

import java.io.ByteArrayOutputStream;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;

import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;

import javax.wsdl.factory.WSDLFactory;

import javax.wsdl.xml.WSDLWriter;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/**
 * This class uses the WSDL4J library to generate a WSDL for a specific method. This WSDL can be
 * used as a
 *
 * @author  pgussow
 */
public class WSDLCreator
{
    /**
     * Holds the base name to use for the port types, etc.
     */
    private String m_baseName;
    /**
     * Hold the element for the schema.
     */
    private Element m_eSchema;
    /**
     * Holds the name of the method.
     */
    private List<String> m_methods = new ArrayList<String>();
    /**
     * Holds the namespace of the method.
     */
    private String m_sNamespace;
    /**
     * Holds the service URL to use.
     */
    private String m_sServiceURL;

    /**
     * Creates a new WSDLCreator object.
     *
     * @param  baseName     DOCUMENTME
     * @param  sNamespace   The namespace of the method.
     * @param  sServiceURL  The service URL to use.
     */
    public WSDLCreator(String baseName, String sNamespace, String sServiceURL)
    {
        m_baseName = baseName;
        m_sNamespace = sNamespace;
        m_sServiceURL = sServiceURL;
    }

    /**
     * This method adds the method to the list.
     *
     * @param  method  The name of the method.
     */
    public void addMethod(String method)
    {
        m_methods.add(method);
    }

    /**
     * This method returns the proper WSDL for this method. This method makes a couple of
     * assumptions:<br>
     * - The name of the input and output message are: 'methodname' and 'methodname'Response
     *
     * @return  The proper WSDL.
     *
     * @throws  WSDLException  In case of any exceptions
     */
    public String createWSDL()
                      throws WSDLException
    {
        String sReturn = null;

        // Create the factory.
        WSDLFactory as = WSDLFactory.newInstance();

        // Create the needed extention points.
        QName qnSchema = new QName("http://www.w3.org/2001/XMLSchema", "schema");

        // Get the currently registered extentions.
        ExtensionRegistry er = as.newPopulatedExtensionRegistry();

        // Create a new WSDL definition.
        Definition dDef = as.newDefinition();
        dDef.setTargetNamespace(m_sNamespace);
        dDef.addNamespace("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
        dDef.addNamespace("tns", m_sNamespace);

        // Create the types.
        Types tTypes = dDef.createTypes();

        Schema sSchema = (Schema) er.createExtension(Types.class, qnSchema);
        tTypes.addExtensibilityElement(sSchema);
        sSchema.setElement(m_eSchema);

        dDef.setTypes(tTypes);

        // Create the port type.
        PortType ptPortType = dDef.createPortType();
        ptPortType.setUndefined(false);
        ptPortType.setQName(new QName(m_sNamespace, m_baseName + "PortType"));

        // Create the 2 messages
        ArrayList<Operation> operations = new ArrayList<Operation>();

        for (String methodName : m_methods)
        {
            Message mInputMessage = createMessage(dDef, methodName, m_sNamespace);
            Message mOutputMessage = createMessage(dDef, methodName + "Response", m_sNamespace);

            Operation oOperation = dDef.createOperation();
            oOperation.setUndefined(false);
            ptPortType.addOperation(oOperation);

            oOperation.setName(methodName);

            // Input and output
            Input iInput = dDef.createInput();
            oOperation.setInput(iInput);
            iInput.setMessage(mInputMessage);

            Output oOutput = dDef.createOutput();
            oOperation.setOutput(oOutput);
            oOutput.setMessage(mOutputMessage);

            operations.add(oOperation);
        }

        dDef.addPortType(ptPortType);

        // Create the binding.
        Binding bBinding = createBinding(dDef, er, m_baseName, ptPortType, operations);

        // Create the service.
        createService(dDef, er, m_baseName, bBinding, m_sServiceURL);

        // Now build up the response for this method.
        WSDLWriter wr = as.newWSDLWriter();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wr.writeWSDL(dDef, baos);

        sReturn = baos.toString();

        // Now we need to string of the declaration, since it shouldn't go in the busmethodsignature
        // It will cause problems with the build framework.
        sReturn = stripDeclaration(sReturn);

        return sReturn;
    }

    /**
     * This method gets the schema node for the generator.
     *
     * @return  The schema node for the generator.
     */
    public Element getSchema()
    {
        return m_eSchema;
    }

    /**
     * This method sets the schema node for the generator.
     *
     * @param  eSchema  The schema node for the generator.
     */
    public void setSchema(Element eSchema)
    {
        m_eSchema = eSchema;
    }

    /**
     * This method creates the binding for this WSDL.
     *
     * @param   dDef        The WSDL definition.
     * @param   erRegistry  The extension registry.
     * @param   baseName    The name of the method.
     * @param   ptPortType  The current port type.
     * @param   operations  The current operation.
     *
     * @return  The created binding.
     *
     * @throws  WSDLException  In case of any exceptions
     */
    private Binding createBinding(Definition dDef, ExtensionRegistry erRegistry, String baseName,
                                  PortType ptPortType, List<Operation> operations)
                           throws WSDLException
    {
        QName qnSoapBinding = new QName("http://schemas.xmlsoap.org/wsdl/soap/", "binding");
        QName qnSoapOperation = new QName("http://schemas.xmlsoap.org/wsdl/soap/", "operation");
        QName qnSoapBody = new QName("http://schemas.xmlsoap.org/wsdl/soap/", "body");

        Binding bReturn = dDef.createBinding();
        bReturn.setUndefined(false);
        //bReturn.setQName(new QName(m_sNamespace, baseName + "Binding"));
        bReturn.setQName(new QName(m_sNamespace, baseName));
        bReturn.setPortType(ptPortType);

        SOAPBinding sb = (SOAPBinding) erRegistry.createExtension(Binding.class, qnSoapBinding);
        sb.setTransportURI("http://schemas.xmlsoap.org/soap/http");
        sb.setStyle("document");
        bReturn.addExtensibilityElement(sb);

        for (Operation operation : operations)
        {
            BindingOperation boOperation = dDef.createBindingOperation();
            bReturn.addBindingOperation(boOperation);
            boOperation.setName(operation.getName());

            SOAPOperation so = (SOAPOperation) erRegistry.createExtension(BindingOperation.class,
                                                                          qnSoapOperation);
            boOperation.addExtensibilityElement(so);
            so.setStyle("document");
            so.setSoapActionURI("");

            // Input and output
            boOperation.setOperation(operation);

            BindingInput biInput = dDef.createBindingInput();
            SOAPBody sbBody = (SOAPBody) erRegistry.createExtension(BindingInput.class, qnSoapBody);
            sbBody.setUse("literal");
            biInput.addExtensibilityElement(sbBody);
            boOperation.setBindingInput(biInput);

            BindingOutput boOutput = dDef.createBindingOutput();
            SOAPBody sbBody2 = (SOAPBody) erRegistry.createExtension(BindingOutput.class,
                                                                     qnSoapBody);
            sbBody2.setUse("literal");
            boOutput.addExtensibilityElement(sbBody2);
            boOperation.setBindingOutput(boOutput);
        }
        dDef.addBinding(bReturn);

        return bReturn;
    }

    /**
     * This method creates and adds a message to the definition of the WSDL.
     *
     * @param   dDef         The WSDL definition.
     * @param   sMethodName  The name of the method.
     * @param   sNamespace   The namespace of the method.
     *
     * @return  The generated message.
     */
    private Message createMessage(Definition dDef, String sMethodName, String sNamespace)
    {
        Message mReturn = dDef.createMessage();
        mReturn.setUndefined(false);

        mReturn.setQName(new QName(m_sNamespace, sMethodName));

        Part pPart = dDef.createPart();
        pPart.setElementName(new QName(sNamespace, sMethodName));
        pPart.setName("body");
        mReturn.addPart(pPart);
        dDef.addMessage(mReturn);

        return mReturn;
    }

    /**
     * This method creates the service part of the WSDL.
     *
     * @param   dDef         The WSDL definition.
     * @param   erRegistry   The extention registry.
     * @param   baseName     The name of the method.
     * @param   bBinding     The current binding.
     * @param   sServiceURL  The current service URl to use.
     *
     * @throws  WSDLException  In case of any exceptions
     */
    private void createService(Definition dDef, ExtensionRegistry erRegistry, String baseName,
                               Binding bBinding, String sServiceURL)
                        throws WSDLException
    {
        QName qnSoapAddress = new QName("http://schemas.xmlsoap.org/wsdl/soap/", "address");

        Service sService = dDef.createService();
        sService.setQName(new QName(m_sNamespace, baseName + "Service"));

        Port pPort = dDef.createPort();
        sService.addPort(pPort);
        pPort.setBinding(bBinding);
        pPort.setName(baseName + "Port");

        SOAPAddress saAddress = (SOAPAddress) erRegistry.createExtension(Port.class, qnSoapAddress);
        saAddress.setLocationURI(sServiceURL);
        pPort.addExtensibilityElement(saAddress);

        dDef.addService(sService);
    }

    /**
     * This method strips all the processing instructions from the XML string.
     *
     * @param   sTarget  The string to strip the processing instructions from.
     *
     * @return  The clean XML string.
     */
    private String stripDeclaration(String sTarget)
    {
        Pattern pPattern = Pattern.compile("<\\?[^\\?]+\\?>");
        Matcher mMatcher = pPattern.matcher(sTarget);

        while (mMatcher.find())
        {
            sTarget = sTarget.substring(mMatcher.end() + 1);
            mMatcher = pPattern.matcher(sTarget);
        }

        return sTarget;
    }
}
