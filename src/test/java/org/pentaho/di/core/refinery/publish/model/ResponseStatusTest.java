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


package org.pentaho.di.core.refinery.publish.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class ResponseStatusTest {
  @Test
  public void testGetAndSet() throws Exception {
    ResponseStatus responseStatus = new ResponseStatus();
    responseStatus.setMessage( "aMessage" );
    assertEquals( "aMessage", responseStatus.getMessage() );
    responseStatus.setStatus( 123 );
    assertEquals( 123, responseStatus.getStatus() );
  }
}
