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
