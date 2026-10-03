package com.stanislav.warrantyclaims.soap;

import com.stanislav.warrantyclaims.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Service
public class WarrantySoapClient implements WarrantyGateway {
    private static final String NAMESPACE = "urn:stanislav:warranty";

    private final WebServiceTemplate template;
    private final String url;

    public WarrantySoapClient(WebServiceTemplateBuilder builder, @Value("${app.soap.url}") String url) {
        this.template = builder.build();
        this.url = url;
    }

    // The payload is placed inside the SOAP envelope by Spring Web Services.
    @Override
    public WarrantyCheck check(String warrantyNumber) {
        String request = """
                <war:CheckWarrantyRequest xmlns:war="urn:stanislav:warranty">
                    <war:warrantyNumber>%s</war:warrantyNumber>
                </war:CheckWarrantyRequest>
                """.formatted(warrantyNumber);
        StringResult response = new StringResult();

        try {
            boolean received = template.sendSourceAndReceiveToResult(
                    url, new StringSource(request), new SoapActionCallback("urn:checkWarranty"), response
            );
            if (!received) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Warranty service returned no response");
            }
            return parseResponse(response.toString());
        } catch (WebServiceClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Warranty service is unavailable");
        }
    }

    private WarrantyCheck parseResponse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            if (!NAMESPACE.equals(root.getNamespaceURI()) || !"CheckWarrantyResponse".equals(root.getLocalName())) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Warranty service returned an invalid response");
            }
            String active = root.getElementsByTagNameNS(NAMESPACE, "active").item(0).getTextContent();
            String reason = root.getElementsByTagNameNS(NAMESPACE, "reason").item(0).getTextContent();
            if (!"true".equals(active) && !"false".equals(active)) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Warranty service returned an invalid response");
            }
            return new WarrantyCheck(Boolean.parseBoolean(active), reason);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Warranty service returned an invalid response");
        }
    }
}

