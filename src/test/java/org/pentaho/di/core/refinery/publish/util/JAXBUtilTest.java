/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2024 by Hitachi Vantara : http://www.pentaho.com
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

import org.junit.Before;
import org.junit.Ignore;
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

  @Ignore
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
