package com.cityroulette.service;

import com.cityroulette.dto.external.ExternalApiProbeResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BusanBusClientTest {
    @Test
    void parsesBusStopXmlResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://bus.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> assertThat(request.getURI().getPath())
                .isEqualTo("/busStopList"))
                .andRespond(withSuccess("""
                    <response><header><resultCode>00</resultCode><resultMsg>OK</resultMsg></header>
                    <body><totalCount>3</totalCount><items><item><arsno>13708</arsno></item></items></body></response>
                    """, MediaType.APPLICATION_XML));

        ExternalApiProbeResult result = new BusanBusClient(builder.build(), "encoded%2Bkey", new BusanBusXmlParser()).probe();

        assertThat(result.status()).isEqualTo(ExternalApiProbeResult.Status.CONNECTED);
        assertThat(result.sampleCount()).isEqualTo(1);
        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.responseFields()).contains("arsno");
        server.verify();
    }

    @Test
    void rejectsMalformedXmlSafely() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://bus.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> { }).andRespond(withSuccess("not-xml", MediaType.TEXT_PLAIN));

        ExternalApiProbeResult result = new BusanBusClient(builder.build(), "secret", new BusanBusXmlParser()).probe();

        assertThat(result.status()).isEqualTo(ExternalApiProbeResult.Status.PARSE_ERROR);
        assertThat(result.message()).doesNotContain("secret");
    }

    @Test
    void parsesTypedStopPageAndDistinguishesAuthenticationError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://bus.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> { }).andRespond(withSuccess("""
                <response><header><resultCode>00</resultCode></header><body><items><item>
                <bstopid>167970102</bstopid><arsno>01001</arsno><bstopnm>영주삼거리</bstopnm>
                <gpsx>129.0333</gpsx><gpsy>35.1153</gpsy><stoptype>일반</stoptype>
                </item></items><pageNo>1</pageNo><numOfRows>5</numOfRows><totalCount>8800</totalCount></body></response>
                """, MediaType.APPLICATION_XML));
        var page = new BusanBusClient(builder.build(), "key", new BusanBusXmlParser()).getStops(1,5,null,null);
        assertThat(page.totalCount()).isEqualTo(8800);
        assertThat(page.items()).singleElement().satisfies(x -> assertThat(x.bstopid()).isEqualTo("167970102"));

        RestClient.Builder authBuilder = RestClient.builder().baseUrl("https://bus.test");
        MockRestServiceServer authServer = MockRestServiceServer.bindTo(authBuilder).build();
        authServer.expect(request -> { }).andRespond(withSuccess("<response><header><resultCode>30</resultCode></header></response>", MediaType.APPLICATION_XML));
        var result = new BusanBusClient(authBuilder.build(), "bad-key", new BusanBusXmlParser()).probe();
        assertThat(result.status()).isEqualTo(ExternalApiProbeResult.Status.AUTH_FAILED);
        assertThat(result.message()).doesNotContain("bad-key");
    }

    @Test
    void mapsConfirmedRouteStopFieldsAndUsesRpointAsDirectionFallback() {
        RestClient.Builder builder=RestClient.builder().baseUrl("https://bus.test");
        MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
        server.expect(request->assertThat(request.getURI().getPath()).isEqualTo("/busInfoByRouteId"))
                .andRespond(withSuccess("""
                        <response><header><resultCode>00</resultCode></header><body><items><item>
                        <nodeid>NODE-1</nodeid><bstopidx>1</bstopidx><bstopnm>첫 정류장</bstopnm>
                        <arsno>10001</arsno><rpoint>START</rpoint><lat>35.1</lat><lin>129.1</lin>
                        </item></items></body></response>
                        """,MediaType.APPLICATION_XML));

        var items=new BusanBusClient(builder.build(),"key",new BusanBusXmlParser()).getRouteStops("R1");

        assertThat(items).singleElement().satisfies(x->{
            assertThat(x.nodeid()).isEqualTo("NODE-1");
            assertThat(x.direction()).isEqualTo("START");
            assertThat(x.lat()).isEqualTo("35.1");
            assertThat(x.lin()).isEqualTo("129.1");
        });
    }
}
