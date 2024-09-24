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

import javax.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.Assert;
import org.junit.Test;

public class BaseRestUtilTest {

  @Test
  public void getAnonymousClient() throws Exception {

    //without property default value 2000
    Client anonymousClient = new PublishRestUtil().getAnonymousClient();
    Assert.assertEquals( 2000, anonymousClient.getConfiguration().getProperty( ClientProperties.READ_TIMEOUT ) );

    int timeOut = 5000;
    System.setProperty( BaseRestUtil.KETTLE_DATA_REFINERY_HTTP_CLIENT_TIMEOUT, String.valueOf( timeOut ) );
    anonymousClient = new PublishRestUtil().getAnonymousClient();
    Assert.assertEquals( timeOut, anonymousClient.getConfiguration().getProperty( ClientProperties.READ_TIMEOUT ) );
  }

}
