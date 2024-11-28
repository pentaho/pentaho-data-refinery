/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.core.refinery.publish.util;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author Rowell Belen
 */
public class JAXBUtils {

  private static ObjectMapper mapper = new ObjectMapper();
  private static JakartaXmlBindAnnotationModule module = new JakartaXmlBindAnnotationModule();

  static {
    mapper.configure( MapperFeature.USE_STD_BEAN_NAMING, true );
    mapper.registerModule( module );
  }

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
    return mapper.writeValueAsString( source );
  }

  public static <T> T unmarshalFromJson( final String json, Class<T> destinationClass ) throws Exception {
    return mapper.readValue( json, destinationClass );
  }

}
