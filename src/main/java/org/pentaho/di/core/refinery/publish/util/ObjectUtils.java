/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import java.util.logging.Logger;

/**
 * @author Rowell Belen
 */
public class ObjectUtils {

  private static XStream xStream = new XStream(new DomDriver());
  private static Logger logger = Logger.getLogger( ObjectUtils.class.getName() );

  static {
     xStream.setClassLoader( ObjectUtils.class.getClassLoader() );
  }

  @SuppressWarnings( "unchecked" )
  public static <T> T deepClone( T object ) {

    if ( object == null ) {
      return null;
    }

    return (T) xStream.fromXML( xStream.toXML( object ) );

  }

  public static String toXml( Object object ) {
    if ( object != null ) {
      return xStream.toXML( object );
    }

    return null;
  }

  public static void logInfo( Object object ) {
    if ( object != null ) {
      logger.info( toXml( object ) );
    }
  }
}
