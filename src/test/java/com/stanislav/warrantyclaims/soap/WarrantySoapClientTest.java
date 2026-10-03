package com.stanislav.warrantyclaims.soap;

import com.stanislav.warrantyclaims.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.transform.StringSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.ws.test.client.RequestMatchers.anything;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;

class WarrantySoapClientTest {
    @Test
    void readsActiveWarrantyFromSoapResponse() {
        WebServiceTemplate template = new WebServiceTemplate();
        MockWebServiceServer server = MockWebServiceServer.createServer(template);
        server.expect(anything()).andRespond(withPayload(new StringSource("""
                <war:CheckWarrantyResponse xmlns:war="urn:stanislav:warranty">
                    <war:active>true</war:active><war:reason>Active</war:reason>
                </war:CheckWarrantyResponse>
                """)));
        WarrantySoapClient client = new WarrantySoapClient(template, "http://test/soap/warranties");

        WarrantyCheck result = client.check("POL-1001");

        assertTrue(result.active());
        assertEquals("Active", result.reason());
        server.verify();
    }

    @Test
    void rejectsMalformedResponse() {
        WebServiceTemplate template = new WebServiceTemplate();
        MockWebServiceServer server = MockWebServiceServer.createServer(template);
        server.expect(anything()).andRespond(withPayload(new StringSource("""
                <war:UnexpectedResponse xmlns:war="urn:stanislav:warranty"/>
                """)));
        WarrantySoapClient client = new WarrantySoapClient(template, "http://test/soap/warranties");

        ApiException error = assertThrows(ApiException.class, () -> client.check("POL-1001"));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
        server.verify();
    }
}

