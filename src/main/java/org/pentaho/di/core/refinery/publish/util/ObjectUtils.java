package org.pentaho.di.core.refinery.publish.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * @author Rowell Belen
 */
@Component
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
