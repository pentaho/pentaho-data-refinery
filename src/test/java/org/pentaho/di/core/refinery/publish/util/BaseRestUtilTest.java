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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import org.junit.Assert;
import org.junit.Test;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.util.EnvUtil;

import static org.junit.Assert.*;


public class BaseRestUtilTest {

  @Test
  public void getAnonymousClient() throws Exception {

    //without property default value 2000
    Client anonymousClient = new PublishRestUtil().getAnonymousClient();
    Assert.assertEquals( 2000, anonymousClient.getProperties().get( ClientConfig.PROPERTY_READ_TIMEOUT ) );

    int timeOut = 5000;
    System.setProperty( BaseRestUtil.KETTLE_DATA_REFINERY_HTTP_CLIENT_TIMEOUT, String.valueOf( timeOut ) );
    anonymousClient = new PublishRestUtil().getAnonymousClient();
    Assert.assertEquals( timeOut, anonymousClient.getProperties().get( ClientConfig.PROPERTY_READ_TIMEOUT ) );
  }

}
