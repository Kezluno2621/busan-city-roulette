package com.cityroulette.service;

import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.function.Function;

@Component
public class BusanBusXmlParser {
    public Document parse(String xml) {
        if (xml == null || xml.isBlank()) throw new ExternalApiException("응답 본문이 없습니다.");
        try {
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception exception) { throw new ExternalApiException("XML 응답을 해석할 수 없습니다."); }
    }
    public <T> List<T> items(Document document,Function<Element,T> mapper) {
        NodeList nodes=document.getElementsByTagName("item"); List<T> result=new ArrayList<>();
        for(int i=0;i<nodes.getLength();i++) result.add(mapper.apply((Element)nodes.item(i)));
        return result;
    }
    public String text(Node node,String tag) {
        if (!(node instanceof Element element)) return null;
        NodeList nodes=element.getElementsByTagName(tag);
        if(nodes.getLength()==0) return null;
        String value=nodes.item(0).getTextContent(); return value==null||value.isBlank()?null:value.trim();
    }
    public int integer(Document document,String tag) { try{return Integer.parseInt(first(document,tag));}catch(Exception e){return 0;} }
    public String first(Document document,String tag) {
        NodeList nodes=document.getElementsByTagName(tag); return nodes.getLength()==0?null:nodes.item(0).getTextContent().trim();
    }
    public Set<String> firstItemFields(Document document) {
        NodeList items=document.getElementsByTagName("item"); Set<String> fields=new LinkedHashSet<>();
        if(items.getLength()==0)return fields; NodeList children=items.item(0).getChildNodes();
        for(int i=0;i<children.getLength();i++)if(children.item(i) instanceof Element)fields.add(children.item(i).getNodeName());
        return fields;
    }
}
