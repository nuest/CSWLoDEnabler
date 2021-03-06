/**
 * ﻿Copyright (C) 2013-2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.lod.csw;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import net.opengis.cat.csw.x202.AbstractQueryType;
import net.opengis.cat.csw.x202.ElementSetNameType;
import net.opengis.cat.csw.x202.GetRecordsDocument;
import net.opengis.cat.csw.x202.GetRecordsResponseDocument;
import net.opengis.cat.csw.x202.GetRecordsType;
import net.opengis.cat.csw.x202.QueryDocument;
import net.opengis.cat.csw.x202.QueryType;
import net.opengis.cat.csw.x202.ResultType;

import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.n52.lod.Configuration;
import org.n52.oxf.OXFException;
import org.n52.oxf.adapter.OperationResult;
import org.n52.oxf.adapter.ParameterContainer;
import org.n52.oxf.csw.adapter.CSWAdapter;
import org.n52.oxf.csw.adapter.CSWRequestBuilder;
import org.n52.oxf.ows.ExceptionReport;
import org.n52.oxf.ows.capabilities.Operation;
import org.n52.oxf.util.web.HttpClientException;
import org.n52.oxf.util.web.SimpleHttpClient;
import org.n52.oxf.xmlbeans.tools.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogInteractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogInteractor.class);

    private CSWAdapter adapter;

    private Configuration config;

    public CatalogInteractor(Configuration config) {
        adapter = new CSWAdapter();
        this.config = config;
    }

    public String executeGetRecords(int maxRecords,
            int startPos) throws OXFException, ExceptionReport {

        ParameterContainer paramCon = new ParameterContainer();
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_MAX_RECORDS, maxRecords);
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_START_POSITION, startPos);
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_RESULT_TYPE, ResultType.RESULTS.toString());
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_OUTPUT_FORMAT_PARAMETER, "application/xml");
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_OUTPUT_SCHEMA_FORMAT, "http://www.opengis.net/cat/csw/2.0.2");
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_QUERY_TYPE_NAMES_PARAMETER, "csw:Record");
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_ELEMENT_SET_NAME_FORMAT, ElementSetNameType.BRIEF.toString());

        String cswUrl = config.getUrlCSW();
        Operation getRecOp = new Operation(CSWAdapter.GET_RECORDS, cswUrl + "?", cswUrl);

        String result = execute(paramCon, getRecOp);

        return result;
    }

    private String execute(ParameterContainer paramCon,
            Operation op) throws ExceptionReport, OXFException {
        LOGGER.debug("Executing operation {}", op);
        OperationResult opResult = adapter.doOperation(op, paramCon);

        String result = new String(opResult.getIncomingResult());
        LOGGER.debug("Received (excerpt): {}", result.substring(0, Math.min(result.length(), 17 * 42)));
        return result;
    }

    public long getNumberOfRecords() throws HttpClientException, IllegalStateException, IOException, XmlException {
        // ParameterContainer paramCon = new ParameterContainer();
        // paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_MAX_RECORDS,
        // 1);
        // paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_START_POSITION,
        // 0);
        // paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_RESULT_TYPE,
        // ResultType.HITS.toString());
        // TODO HITS not implemented in OX-F yet...

        // paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_OUTPUT_SCHEMA_FORMAT,
        // "http://www.opengis.net/cat/csw/2.0.2");
        // paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_QUERY_TYPE_NAMES_PARAMETER,
        // "csw:Record");
        // paramCon.addParameterShell(CSWRequestBuilder.GET_RECORDS_ELEMENT_SET_NAME_FORMAT,
        // ElementSetNameType.BRIEF.toString());

        // csw?request=GetRecords&service=CSW
        // &version=2.0.2&namespace=xmlns(csw=http://www.opengis.net/cat/csw)
        // &resultType=results&outputSchema=http://www.opengis.net/cat/csw/2.0.2
        // &outputFormat=application/xml
        // catalog only support POST for GetRecords

        GetRecordsDocument xb_getRecordsDocument = GetRecordsDocument.Factory.newInstance();
        GetRecordsType xb_getRecords = xb_getRecordsDocument.addNewGetRecords();
        xb_getRecords.setService(CSWAdapter.SERVICE_TYPE);
        xb_getRecords.setVersion(CSWAdapter.SUPPORTED_VERSIONS[0]);
        xb_getRecords.setResultType(ResultType.HITS);
        QueryType xb_query = QueryType.Factory.newInstance();
        List<QName> typeNameList = new ArrayList<>();
        typeNameList.add(new QName(CSWAdapter.NAMESPACE, "Record"));
        xb_query.setTypeNames(typeNameList);
        AbstractQueryType abstractQuery = xb_getRecords.addNewAbstractQuery();
        abstractQuery.set(xb_query);
        XmlUtil.qualifySubstitutionGroup(xb_getRecords.getAbstractQuery(),
                QueryDocument.type.getDocumentElementName(),
                QueryType.type);
        
        String request = xb_getRecordsDocument.xmlText(new XmlOptions().setSavePrettyPrint());

        SimpleHttpClient httpClient = new SimpleHttpClient(20000);
        String cswUrl = config.getUrlCSW();
        HttpResponse response = httpClient.executePost(cswUrl, request, ContentType.TEXT_XML);
        GetRecordsResponseDocument doc = GetRecordsResponseDocument.Factory.parse(response.getEntity().getContent());
        
        BigInteger numberOfRecordsMatched = doc.getGetRecordsResponse().getSearchResults().getNumberOfRecordsMatched();
        return numberOfRecordsMatched.longValueExact();
    }

    public String executeGetRecordsById(String recordID) throws OXFException, ExceptionReport {
        LOGGER.debug("Calling GetRecordsById for record '" + recordID + "'");

        String elementSetName = "full";
        String outputSchema = config.getNsGMD();

        ParameterContainer paramCon = new ParameterContainer();
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORD_BY_ID_REQUEST, CSWAdapter.GET_RECORD_BY_ID);
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORD_BY_ID_VERSION, CSWAdapter.SUPPORTED_VERSIONS[0]);
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORD_BY_ID_SERVICE, CSWAdapter.SERVICE_TYPE);
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORD_BY_ID_ID, recordID);
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORD_BY_ID_ELEMENT_SET_NAME, elementSetName);
        paramCon.addParameterShell(CSWRequestBuilder.GET_RECORD_BY_ID_OUTPUT_SCHEMA, outputSchema);

        String cswUrl = config.getUrlCSW();
        Operation op = new Operation(CSWAdapter.GET_RECORD_BY_ID, cswUrl + "?", cswUrl);

        String result = execute(paramCon, op);

        return result;
    }

}
