package com.cityroulette.integration.bus;

import com.cityroulette.integration.ExternalApiException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class BusanBusXmlParserTest {
    private final BusanBusXmlParser parser=new BusanBusXmlParser();
    @Test void parsesItems(){var d=parser.parse("<response><header><resultCode>00</resultCode></header><body><items><item><bstopid>1</bstopid></item></items><totalCount>1</totalCount></body></response>");assertThat(parser.first(d,"resultCode")).isEqualTo("00");assertThat(parser.integer(d,"totalCount")).isEqualTo(1);assertThat(parser.firstItemFields(d)).containsExactly("bstopid");}
    @Test void blocksDoctypeAndExternalEntities(){String xml="<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><response><item><name>&e;</name></item></response>";assertThatThrownBy(()->parser.parse(xml)).isInstanceOf(ExternalApiException.class).hasMessageContaining("XML");}
}
