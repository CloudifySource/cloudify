/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.azure.files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.cloudifysource.azure.files.xml.XMLElementAttribute;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//CR: I think it is proper to have a unit tests for these methods (something like read/write/read)
//    If there is a small bug here I am not sure the system test will catch it. Big headache
public class XMLXPathEditor {

    private final Document document;
    private final XPath xPath;

    public XMLXPathEditor(File cscfgFile) throws XMLXPathEditorException {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(false);
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.parse(cscfgFile);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            xPath = xPathFactory.newXPath();
        } catch (ParserConfigurationException e) {
            throw new XMLXPathEditorException("Error in file " + cscfgFile,e);
        } catch (SAXException e) {
            throw new XMLXPathEditorException("Error in file " + cscfgFile,e);
        } catch (IOException e) {
            throw new XMLXPathEditorException("Error in file " + cscfgFile,e);
        }
    }
    
    public String getNodeValue(String xPathExpression) throws XMLXPathEditorException {
        try {
            return (String)xPath.evaluate(xPathExpression, document, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new XMLXPathEditorException(e);
        }
    }
        
    public void setNodeValue(String xPathExpression, String nodeValue) throws XMLXPathEditorException {
        setNodeValue(xPathExpression, nodeValue, document);
    }

    public void setNodeValue(String xPathExpression, String nodeValue, Object item) throws XMLXPathEditorException {
        try {
            Node node = (Node)xPath.evaluate(xPathExpression, item, XPathConstants.NODE);
            node.setNodeValue(nodeValue);
        } catch (XPathExpressionException e) {
            throw new XMLXPathEditorException(e);
        }
    }

    public void setNodeAttribute(String xPathExpression, String attributeName, String attributeValue) throws XMLXPathEditorException {
        setNodeAttribute(xPathExpression, attributeName, attributeValue, document);
    }

    public void setNodeAttribute(String xPathExpression, String attributeName, String attributeValue, Object item) throws XMLXPathEditorException {
        try {
            NodeList nodes = (NodeList)xPath.evaluate(xPathExpression, item, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                node.getAttributes().getNamedItem(attributeName).setNodeValue(attributeValue);
            }
        } catch (XPathExpressionException e) {
            throw new XMLXPathEditorException(e);
        }
    }
    
    public void findNodeDuplicateByAttribute(String xPathExpression, String[] values, String attributeName) throws XMLXPathEditorException {
        try {
            Node node = (Node)xPath.evaluate(xPathExpression, document, XPathConstants.NODE);
            for (String value : values) {
                Node duplicate = node.cloneNode(true);
                duplicate.getAttributes().getNamedItem(attributeName).setNodeValue(value);
                node.getParentNode().appendChild(duplicate);
            }
            node.getParentNode().removeChild(node);
        } catch (XPathExpressionException e) {
            throw new XMLXPathEditorException(e);
        }
    }
    
    public void findNodeDuplicateChildrenText(String xPathExpression, String[] values, String[] childrenXPathExpressions) throws XMLXPathEditorException {
        try {
            Node node = (Node)xPath.evaluate(xPathExpression, document, XPathConstants.NODE);
            for (String value : values) {
                Node duplicate = node.cloneNode(true);
                for (String childrenXPath : childrenXPathExpressions) {
                    setNodeValue(childrenXPath, value, duplicate);  
                }
                node.getParentNode().appendChild(duplicate);
            }
            node.getParentNode().removeChild(node);
        } catch (XPathExpressionException e) {
            throw new XMLXPathEditorException(e);
        }
    }

    public void addElement(String parentXPath, String elementName, XMLElementAttribute[] attributes) throws XMLXPathEditorException {
        Element element = document.createElement(elementName);
        for (XMLElementAttribute attribute : attributes) {
            element.setAttribute(attribute.getKey(), attribute.getValue());
        }
        try {
            Node parent = (Node)xPath.evaluate(parentXPath, document, XPathConstants.NODE);
            parent.appendChild(element);
        } catch (XPathExpressionException e) {
            throw new XMLXPathEditorException(e);
        }
    }
    
    public File writeXmlFile(File file) throws XMLXPathEditorException {
        FileOutputStream fos = null;
        try {
            Source source = new DOMSource(document);
            fos = new FileOutputStream(file);
            Result result = new StreamResult(fos);
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
            return file;
        } catch (TransformerException e) {
            throw new XMLXPathEditorException(e);
        } catch (FileNotFoundException e) {
            throw new XMLXPathEditorException(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    throw new XMLXPathEditorException(e);
                }
            }
        }
    }
    
}
