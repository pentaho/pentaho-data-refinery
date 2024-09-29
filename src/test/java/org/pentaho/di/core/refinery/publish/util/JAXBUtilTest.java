/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.di.core.refinery.publish.util;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.database.model.DatabaseConnection;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;

/**
 * @author Rowell Belen
 */
public class JAXBUtilTest {

  private JAXBUtils jaxbUtils;

  @Before
  public void init() {
    new JAXBUtils();
  }

  @Test
  public void test() throws Exception {

    jaxbUtils = new JAXBUtils();

    String id = UUID.randomUUID().toString();

    DatabaseConnection connection = new DatabaseConnection();
    connection.setId( id );

    String xml = JAXBUtils.marshallToXml( connection );
    assertNotNull( xml );

    String json = JAXBUtils.marshallToJson( connection );
    assertNotNull( json );

    connection = JAXBUtils.unmarshalFromXml( xml, DatabaseConnection.class );
    assertNotNull( connection );

    connection = JAXBUtils.unmarshalFromJson( json, DatabaseConnection.class );
    assertNotNull( connection );

  }
}
