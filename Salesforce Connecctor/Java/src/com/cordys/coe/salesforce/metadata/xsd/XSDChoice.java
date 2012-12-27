// XSD4J - XML Schema library for Java
//
// Copyright (C) 2006 Josef Spillner <js177634@inf.tu-dresden.de>
// Published under GNU General Public License conditions.
//
// This file is part of the XSD4J library.
// It has been created under a student deployment contract
// at the Chair of Computer Networks, Faculty for Computer Sciences,
// Dresden University of Technology.
// See http://dynvocation.selfip.net/xsd4j/ for more information.

package com.cordys.coe.salesforce.metadata.xsd;

import java.util.*;

/**
 * \brief Class corresponding to a choice in XML Schema.
 *
 * Choices are represented as pseudo-elements of the special type
 * \ref XSDType::TYPE_CHOICE. The type's \ref XSDType::getChoice method
 * will then return all the choices (either elements or element groups),
 * one of which will take the place of the pseudo-element in instances.
 *
 * @author Josef Spillner <js177634@inf.tu-dresden.de>
 */
public class XSDChoice
{
	// Fully-qualified name
	// FIXME: choices have no names!?
	//private QName name;
	// Element groups (XSDSequence) contained in this choice
	private ArrayList<XSDSequence> groups;
	// Single elements (XSDElement) contained in this choice
	private ArrayList<XSDElement> elements;

	/**
	 * Default constructor.
	 *
	 * Produces an empty choice.
	 */
	public XSDChoice()
	{
		this.groups = new ArrayList<XSDSequence>();
		this.elements = new ArrayList<XSDElement>();
	}

	/*public void setName(QName name)
	{
		this.name = name;
	}*/

	/**
	 * Adds an element to the choice.
	 *
	 * @param xsdelement Element to be added to this choice
	 */
	public void addElement(XSDElement xsdelement)
	{
		this.elements.add(xsdelement);
	}

	/**
	 * Adds an element group to the choice.
	 *
	 * @param xsdgroup Group of elements to be added
	 */
	public void addGroup(XSDSequence xsdgroup)
	{
		this.groups.add(xsdgroup);
	}

	/*public QName getName()
	{
		return this.name;
	}*/

	/**
	 * Returns all elements of the choice.
	 *
	 * All single elements which were added are returned here.
	 *
	 * @return List of XSDElement objects
	 */
	public ArrayList<XSDElement> getElements()
	{
		return this.elements;
	}

	/**
	 * Returns all element groups of this choice.
	 *
	 * All element groups which were added are returned here.
	 *
	 * @return List of XSDSequence objects representing element groups
	 */
	public ArrayList<XSDSequence> getGroups()
	{
		return this.groups;
	}
}

