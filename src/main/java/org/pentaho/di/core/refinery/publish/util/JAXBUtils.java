/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package org.pentaho.di.core.refinery.publish.util;

import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import com.sun.jersey.api.json.JSONUnmarshaller;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author Rowell Belen
 */
public class JAXBUtils {

  public static String marshallToXml( Object source ) throws Exception {
    JAXBContext jaxbContext = JAXBContext.newInstance( source.getClass() );
    Marshaller marshaller = jaxbContext.createMarshaller();
    marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
    StringWriter writer = new StringWriter();
    marshaller.marshal( source, writer );
    return writer.toString();
  }

  @SuppressWarnings( "unchecked" )
  public static <T> T unmarshalFromXml( final String xml, Class<T> destinationClass ) throws Exception {
    JAXBContext jaxbContext = JAXBContext.newInstance( destinationClass );
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StringReader reader = new StringReader( xml );
    return (T) unmarshaller.unmarshal( reader );
  }

  public static String marshallToJson( Object source ) throws Exception {
    JAXBContext jaxbContext = JAXBContext.newInstance( source.getClass() );
    Marshaller marshaller = jaxbContext.createMarshaller();
    marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
    JSONMarshaller jsonMarshaller = JSONJAXBContext.getJSONMarshaller( marshaller, jaxbContext );
    StringWriter writer = new StringWriter();
    jsonMarshaller.marshallToJSON( source, writer );
    return writer.toString();
  }

  public static <T> T unmarshalFromJson( final String json, Class<T> destinationClass ) throws Exception {
    JAXBContext jaxbContext = JAXBContext.newInstance( destinationClass );
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    JSONUnmarshaller jsonUnmarshaller = JSONJAXBContext.getJSONUnmarshaller( unmarshaller, jaxbContext );
    StringReader reader = new StringReader( json );
    JAXBElement<T> element = jsonUnmarshaller.unmarshalJAXBElementFromJSON( reader, destinationClass );
    return element.getValue();
  }

}
