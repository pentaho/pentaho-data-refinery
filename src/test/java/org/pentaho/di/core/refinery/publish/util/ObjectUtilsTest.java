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

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.DataSourcePublishModel;

import static org.junit.Assert.*;

/**
 * @author Rowell Belen
 */
public class ObjectUtilsTest {

  private ObjectUtils objectUtils;
  private BiServerConnection biServerConnection;

  @Before
  public void init() {

    objectUtils = new ObjectUtils();

    biServerConnection = new BiServerConnection();
    biServerConnection.setName( "default" );
    biServerConnection.setUserId( "admin" );
    biServerConnection.setPassword( "password" );
    biServerConnection.setUrl( "http://localhost:8080/pentaho/" );
  }

  @Test
  public void test() {

    assertNull( objectUtils.toXml( null ) );

    String xml = objectUtils.toXml( biServerConnection );
    assertTrue( StringUtils.contains( xml, "<name>default</name>" ) );
    assertTrue( StringUtils.contains( xml, "<password>password</password>" ) );

    objectUtils.logInfo( null );
    objectUtils.logInfo( xml );
  }

  @Test
  public void testDeepClone() {

    BiServerConnection biServerModel = new BiServerConnection();
    biServerModel.setName( "default" );

    DataSourcePublishModel model = new DataSourcePublishModel();
    model.setOverride( true );
    model.setModelName( "logicalModel" );
    model.setBiServerConnection( biServerModel );

    BiServerConnection clone = objectUtils.deepClone( biServerModel );
    assertEquals( clone.getName(), biServerModel.getName() );

    objectUtils.deepClone( null );
  }
}
