/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package org.fcrepo.server.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.fcrepo.common.Constants;
import org.fcrepo.server.errors.GeneralException;
import org.fcrepo.server.errors.ObjectValidityException;
import org.fcrepo.server.storage.types.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * XML Schema validation for Digital Objects.
 *
 * @author Sandy Payette
 */
public class DOValidatorXMLSchema
        implements Constants, EntityResolver {

    private static final Logger logger =
            LoggerFactory.getLogger(DOValidatorXMLSchema.class);

    /** Constants used for JAXP 1.2 */
    private static final String JAXP_SCHEMA_LANGUAGE =
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    private URI schemaURI = null;

    public DOValidatorXMLSchema(String schemaPath)
            throws GeneralException {
        try {
            schemaURI = (new File(schemaPath)).toURI();
        } catch (Exception e) {
            logger.error("Error constructing validator", e);
            throw new GeneralException(e.getMessage());
        }
    }

    public void validate(File objectAsFile) throws ObjectValidityException,
            GeneralException {
        try {
            validate(new InputSource(new FileInputStream(objectAsFile)));
        } catch (IOException e) {
            String msg =
                    "DOValidatorXMLSchema returned error.\n"
                            + "The underlying exception was a "
                            + e.getClass().getName() + ".\n"
                            + "The message was " + "\"" + e.getMessage() + "\"";
            throw new GeneralException(msg);
        }
    }

    public void validate(InputStream objectAsStream)
            throws ObjectValidityException, GeneralException {
        validate(new InputSource(objectAsStream));
    }

    private void validate(InputSource objectAsSource)
            throws ObjectValidityException, GeneralException {
        InputSource doXML = objectAsSource;
        try {
            // XMLSchema validation via SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setValidating(true);
            SAXParser sp = spf.newSAXParser();
            sp.setProperty(JAXP_SCHEMA_LANGUAGE, XML_XSD.uri);

            // JAXP property for schema location
            sp
                    .setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource",
                                 schemaURI.toString());

            XMLReader xmlreader = sp.getXMLReader();
            xmlreader.setErrorHandler(new DOValidatorXMLErrorHandler());
            xmlreader.setEntityResolver(this);
            xmlreader.parse(doXML);
        } catch (ParserConfigurationException e) {
            String msg =
                    "DOValidatorXMLSchema returned parser error.\n"
                            + "The underlying exception was a "
                            + e.getClass().getName() + ".\n"
                            + "The message was " + "\"" + e.getMessage() + "\"";
            throw new GeneralException(msg, e);
        } catch (SAXException e) {
            String msg =
                    "DOValidatorXMLSchema returned validation exception.\n"
                            + "The underlying exception was a "
                            + e.getClass().getName() + ".\n"
                            + "The message was " + "\"" + e.getMessage() + "\"";
            Validation validation = new Validation("unknown");
            List<String> problems = new ArrayList<String>();
            problems.add(msg);
            validation.setObjectProblems(problems);
            throw new ObjectValidityException(msg, validation, e);
        } catch (Exception e) {
            String msg =
                    "DOValidatorXMLSchema returned error.\n"
                            + "The underlying error was a "
                            + e.getClass().getName() + ".\n"
                            + "The message was " + "\"" + e.getMessage() + "\"";
            throw new GeneralException(msg, e);
        }
    }

    /**
     * Resolve the entity if it's referring to a local schema. Otherwise, return
     * an empty InputSource. This behavior is required in order to ensure that
     * Xerces never attempts to load external schemas specified with
     * xsi:schemaLocation. It is not enough that we specify
     * processContents="skip" in our own schema.
     */
    public InputSource resolveEntity(String publicId, String systemId) {
        if (systemId != null && systemId.startsWith("file:")) {
            return null;
        } else {
            return new InputSource();
        }
    }
}
