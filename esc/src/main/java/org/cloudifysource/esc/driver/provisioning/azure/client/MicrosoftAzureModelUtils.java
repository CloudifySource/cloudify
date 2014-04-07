/******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved		  *
 * 																			  *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at									  *
 *																			  *
 *       http://www.apache.org/licenses/LICENSE-2.0							  *
 *																			  *
 * Unless required by applicable law or agreed to in writing, software		  *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.											  *
 ******************************************************************************/

package org.cloudifysource.esc.driver.provisioning.azure.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cloudifysource.esc.driver.provisioning.azure.model.ModelContextFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/***************************************************************************************
 * A Utility class for marshaling and unmarshaling azure model related objects.
 * *
 * 
 * @author elip 
 ***************************************************************************************/
public final class MicrosoftAzureModelUtils {

	private MicrosoftAzureModelUtils() {

	}

	/**
	 * 
	 * @param body
	 *            - the object to marshal
	 * @param network
	 *            - whether or not this object belongs to the network domain
	 *            model.
	 * @return - a String representation in the form of an XML of the body
	 * @throws MicrosoftAzureException
	 *             - indicates a marshaling exception happened
	 */
	public static String marshall(final Object body, final boolean network)
			throws MicrosoftAzureException {
		JAXBContext context = ModelContextFactory.createInstance();
		StringWriter sw = new StringWriter();
		Marshaller m;
		try {
			m = context.createMarshaller();
			m.marshal(body, sw);
			Document doc = createEmptyDocument();
			m.marshal(body, doc);
			String xml = getStringFromDocument(doc);
			if (network) { // so stupid !! TODO eli - find a proper way to deal
							// with different name spaces under the space
							// JAXBContext instance.
							// i cant seem to find the azure xsd anywhere so
							// that i can generate proper model object using
							// xjc.
				xml = addNameSpaceToRootElement(xml,
						"xmlns=\"http://schemas.microsoft.com/ServiceHosting/2011/07/NetworkConfiguration\"");
			} else {
				xml = addNameSpaceToRootElement(xml, "xmlns=\"http://schemas.microsoft.com/windowsazure\"");
                xml = addNameSpaceToRootElement(xml, "xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\"");
			}
			return xml;
		} catch (JAXBException e) {
			throw new MicrosoftAzureException(e);
		}
	}

	/**
	 * @param xml
	 * @param nameSpace
	 */
	private static String addNameSpaceToRootElement(final String xml, final String nameSpace) {
		int count = 0;
		int i = 0;
		while (count < 2) {
			if (xml.charAt(i) == '>') {
				count++;
			}
			i++;
		}
		String first = xml.substring(0, i - 1);
		String second = xml.substring(i, xml.length());
		return first + " " + nameSpace + ">" + second;

	}

	/**
	 * 
	 * @param entity
	 *            - the string representation off the object in XML form
	 * @return - an instance of {@link Object} representing the XML that can be
	 *         cast to a specific type
	 * @throws MicrosoftAzureException .
	 */
	public static Object unmarshall(final String entity)
			throws MicrosoftAzureException {
		JAXBContext context = ModelContextFactory.createInstance();
		Document xmlDoc = parse(entity);
		Unmarshaller um = null;
		try {
			um = context.createUnmarshaller();
			return um.unmarshal(xmlDoc);
		} catch (JAXBException e) {
			throw new MicrosoftAzureException(e);
		}
	}

	private static DocumentBuilder createDocumentBuilder() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setNamespaceAware(false);
			return dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Document parse(final String xml)
			throws MicrosoftAzureException {
		try {
			DocumentBuilder documentBuilder = createDocumentBuilder();
			Document xmlDoc = documentBuilder.parse(new InputSource(
					new StringReader(xml)));
			xmlDoc.normalizeDocument();
			return xmlDoc;
		} catch (SAXException e) {
			throw new MicrosoftAzureException(
					"Failed to parse XML Response from server. Response was: "
							+ xml + ", Error was: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new MicrosoftAzureException(
					"Failed to parse XML Response from server. Response was: "
							+ xml + ", Error was: " + e.getMessage(), e);
		}
	}

	private static String getStringFromDocument(final Document doc)
			throws MicrosoftAzureException {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			return writer.toString();
		} catch (TransformerException ex) {
			throw new MicrosoftAzureException(ex);
		}
	}

	private static Document createEmptyDocument() {
		DocumentBuilder documentBuilder = createDocumentBuilder();
		return documentBuilder.newDocument();
	}

}
