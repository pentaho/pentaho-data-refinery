/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2018 by Hitachi Vantara : http://www.pentaho.com
 *
 * *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ********************************************************************************/

package org.pentaho.di.core.refinery.publish.util;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author Rowell Belen
 */
public class JAXBUtils {

  private static ObjectMapper mapper = new ObjectMapper();
  private static JaxbAnnotationModule module = new JaxbAnnotationModule();

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
