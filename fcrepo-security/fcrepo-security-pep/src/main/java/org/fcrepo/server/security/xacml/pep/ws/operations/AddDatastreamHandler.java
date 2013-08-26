/*
 * File: AddDatastreamHandler.java
 *
 * Copyright 2007 Macquarie E-Learning Centre Of Excellence
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.server.security.xacml.pep.ws.operations;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.binding.soap.SoapFault;
import org.fcrepo.common.Constants;
import org.fcrepo.server.security.xacml.pdp.data.FedoraPolicyStore;
import org.fcrepo.server.security.xacml.pep.ContextHandler;
import org.fcrepo.server.security.xacml.pep.PEPException;
import org.fcrepo.server.security.xacml.pep.ResourceAttributes;
import org.fcrepo.server.security.xacml.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.StringAttribute;
import com.sun.xacml.ctx.RequestCtx;

/**
 * @author nishen@melcoe.mq.edu.au
 */
public class AddDatastreamHandler
        extends AbstractOperationHandler {

    private static final Logger logger = LoggerFactory
            .getLogger(AddDatastreamHandler.class);

    public AddDatastreamHandler(ContextHandler contextHandler)
            throws PEPException {
        super(contextHandler);
    }

    @Override
    public RequestCtx handleResponse(SOAPMessageContext context)
            throws OperationHandlerException {
        return null;
    }

    @Override
    public RequestCtx handleRequest(SOAPMessageContext context)
            throws OperationHandlerException {
        logger.debug("AddDatastreamHandler/handleRequest!");

        RequestCtx req = null;
        Object oMap = null;

        String pid = null;
        String dsID = null;
        // String[] altIDs = null;
        // String dsLabel = null;
        // Boolean versionable = null;
        String mimeType = null;
        String formatURI = null;
        String dsLocation = null;
        String controlGroup = null;
        String dsState = null;
        String checksumType = null;
        String checksum = null;
        // String logMessage = null;

        try {
            oMap = getSOAPRequestObjects(context);
            logger.debug("Retrieved SOAP Request Objects");
        } catch (SoapFault sf) {
            logger.error("Error obtaining SOAP Request Objects", sf);
            throw new OperationHandlerException("Error obtaining SOAP Request Objects",
                                                sf);
        }

        try {
            pid = (String) callGetter("getPid",oMap);
            dsID = (String) callGetter("getDsID", oMap);
            mimeType = (String) callGetter("getMIMEType", oMap);
            formatURI = (String) callGetter("getFormatURI", oMap);
            dsLocation = (String) callGetter("getDsLocation", oMap);
            controlGroup = (String) callGetter("getControlGroup", oMap);
            dsState = (String) callGetter("getDsState", oMap);
            checksumType = (String) callGetter("getChecksumType", oMap);
            checksum = (String) callGetter("getChecksum", oMap);
        } catch (Exception e) {
            logger.error("Error obtaining parameters", e);
            throw new OperationHandlerException("Error obtaining parameters.",
                                                e);
        }

        logger.debug("Extracted SOAP Request Objects");

        Map<URI, AttributeValue> actions = new HashMap<URI, AttributeValue>();
        Map<URI, AttributeValue> resAttr;

        try {
            resAttr = ResourceAttributes.getResources(pid);
            if (dsID != null && !dsID.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.ID.getURI(),
                            new StringAttribute(dsID));
            }
            if (mimeType != null && !mimeType.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_MIME_TYPE.getURI(),
                            new StringAttribute(mimeType));
            }
            if (formatURI != null && !formatURI.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_FORMAT_URI.getURI(),
                            new AnyURIAttribute(new URI(formatURI)));
            }
            if (dsLocation != null && !dsLocation.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_LOCATION.getURI(),
                            new AnyURIAttribute(new URI(dsLocation)));
            }
            if (controlGroup != null && !controlGroup.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_CONTROL_GROUP.getURI(),
                            new StringAttribute(controlGroup));
            }
            if (dsState != null && !dsState.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_STATE.getURI(),
                            new StringAttribute(dsState));
            }
            if (checksumType != null && !checksumType.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_CHECKSUM_TYPE.getURI(),
                            new StringAttribute(checksumType));
            }
            if (checksum != null && !checksum.isEmpty()) {
                resAttr.put(Constants.DATASTREAM.NEW_CHECKSUM.getURI(),
                            new StringAttribute(checksum));
            }

            actions.put(Constants.ACTION.ID.getURI(),
                        Constants.ACTION.ADD_DATASTREAM
                                .getStringAttribute());
            actions.put(Constants.ACTION.API.getURI(),
                        Constants.ACTION.APIM.getStringAttribute());
            // modifying the FeSL policy datastream requires policy management permissions
            if (dsID != null
                    && dsID.equals(FedoraPolicyStore.FESL_POLICY_DATASTREAM)) {
                actions.put(Constants.ACTION.ID.getURI(),
                            Constants.ACTION.MANAGE_POLICIES
                                    .getStringAttribute());

            }

            req =
                    getContextHandler().buildRequest(getSubjects(context),
                                                     actions,
                                                     resAttr,
                                                     getEnvironment(context));

            LogUtil.statLog(getUser(context), Constants.ACTION.ADD_DATASTREAM.uri, pid, dsID);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new OperationHandlerException(e.getMessage(), e);
        }

        return req;
    }
}
