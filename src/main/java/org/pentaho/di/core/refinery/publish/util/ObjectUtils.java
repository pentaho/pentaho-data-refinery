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

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * @author Rowell Belen
 */
public class ObjectUtils {

  private static Logger logger = Logger.getLogger( ObjectUtils.class.getName() );

  @SuppressWarnings( "unchecked" )
  public static <T> T deepClone( T object ) {
    if ( object == null ) {
      return null;
    }
    if ( object instanceof Serializable ) {
      T ret = cloneSerialize( object );
      if ( ret != null ) {
        return ret;
      }
    }
    try {
      String xml = toXml( object );
      return (T) fromXml( xml );
    } catch ( Exception ex ) {
      logger.severe( ex.getMessage() );
      throw new RuntimeException( ex );
    }
  }

  protected static Object fromXml( String xml ) {
    XMLDecoder decoder = null;
    ByteArrayInputStream is = null;
    try {
      is = new ByteArrayInputStream( xml.getBytes() );
      decoder = new XMLDecoder( is );
      Object ret = decoder.readObject();
      is.close();
      return ret;
    } catch ( Exception ex ) {
      logger.severe( ex.getMessage() );
      throw new RuntimeException( ex );
    } finally {
      if ( decoder != null ) {
        decoder.close();
      }
    }
  }

  protected static <T> T cloneSerialize( T object ) {
    ByteArrayInputStream is = null;
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      oos = new ObjectOutputStream( out );
      oos.writeObject( object );
      oos.flush();
      is = new ByteArrayInputStream( out.toByteArray() );
      ois = new ObjectInputStream( is );
      T ret = (T) ois.readObject();
      return ret;
    } catch ( ClassNotFoundException | IOException | RuntimeException ex ) {
      logger.severe( ex.getMessage() );
    } finally {
      try {
        if ( out != null ) {
          out.close();
        }
      } catch ( Exception ex ) {
        logger.severe( "Unalbe to close resource" );
      }
      try {
        if ( oos != null ) {
          oos.close();
        }
      } catch ( Exception ex ) {
        logger.severe( "Unalbe to close resource" );
      }
      try {
        if ( is != null ) {
          is.close();
        }
      } catch ( Exception ex ) {
        logger.severe( "Unalbe to close resource" );
      }
      try {
        if ( ois != null ) {
          ois.close();
        }
      } catch ( Exception ex ) {
        logger.severe( "Unalbe to close resource" );
      }
    }
    return null;
  }

  public static String toXml( Object object ) {
    XMLEncoder encoder = null;
    try {
      if ( object != null ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encoder = new XMLEncoder( out );
        encoder.writeObject( object );
        encoder.flush();
        out.close();
        return out.toString();
      }
    } catch ( Exception ex ) {
      logger.severe( ex.getMessage() );
      throw new RuntimeException( ex );
    } finally {
      if ( encoder != null ) {
        encoder.close();
      }
    }
    return null;
  }

  public static void logInfo( Object object ) {
    if ( object != null ) {
      try {
        logger.info( toXml( object ) );
      } catch ( RuntimeException ex ) {
        //if we were unable to build xml message because of an object being not AJXB ready
        logger.info( object.toString() );
      }
    }
  }
}
