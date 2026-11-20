/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.security;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/** SAX reader for SVG: no document type declarations or external resources. */
public final class SvgXmlReader {
    private SvgXmlReader() {
    }

    public static XMLReader create() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // Android's ExpatReader supports explicitly disabling both entity features.
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setEntityResolver((publicId, systemId) -> {
            throw new SAXException("External SVG resources are not allowed");
        });
        // A lexical handler also rejects internal entity declarations before expansion.
        // Unlike Xerces-specific feature names, this property is supported by Expat.
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", new DefaultHandler2() {
            @Override
            public void startDTD(String name, String publicId, String systemId) throws SAXException {
                throw new SAXException("SVG document type declarations are not allowed");
            }
        });
        return reader;
    }
}
