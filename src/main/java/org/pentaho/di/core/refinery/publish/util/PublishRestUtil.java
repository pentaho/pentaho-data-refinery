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

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.ResponseStatus;

/**
 * @author Rowell Belen
 */
public class PublishRestUtil extends BaseRestUtil {

  protected static final String CAN_PUBLISH_PATH =
      "api/authorization/action/isauthorized?authAction=org.pentaho.security.publish";

  protected static final String CAN_CREATE_PATH =
      "api/authorization/action/isauthorized?authAction=org.pentaho.repository.create";

  protected static final String CAN_EXECUTE_PATH =
      "api/authorization/action/isauthorized?authAction=org.pentaho.repository.execute";

  protected static final String CAN_MANAGE_DATASOURCES =
      "api/authorization/action/isauthorized?authAction=org.pentaho.platform.dataaccess.datasource.security.manage";

  protected static final String PENTAHO_WEBCONTEXT_PATH = "webcontext.js";

  protected static final String PENTAHO_WEBCONTEXT_MATCH = "PentahoWebContextFilter";

  protected static final String SUCCESS_RESPONSE = "SUCCESS";

  private Log logger = LogFactory.getLog( PublishRestUtil.class );
  protected int lastHTTPStatus = 0;

  public boolean isUnauthenticatedUser( final BiServerConnection connection ) {

    ResponseStatus response = simpleHttpGet( connection, CAN_PUBLISH_PATH, true );
    if ( response != null ) {
      return response.getStatus() == 401;
    }

    return false;
  }

  public boolean canPublish( final BiServerConnection connection ) {

    ClientResponse response = httpGet( connection, CAN_PUBLISH_PATH, true );

    if ( response != null ) {
      lastHTTPStatus = response.getStatus();
      return Boolean.parseBoolean( response.getEntity( String.class ) );
    }
    lastHTTPStatus = -1;
    return false;
  }

  public boolean canManageDatasources( final BiServerConnection connection ) {

    ClientResponse response = httpGet( connection, CAN_MANAGE_DATASOURCES, true );
    if ( response != null ) {
      lastHTTPStatus = response.getStatus();
      return Boolean.parseBoolean( response.getEntity( String.class ) );
    }
    lastHTTPStatus = -1;
    return false;
  }

  public boolean canCreate( final BiServerConnection connection ) {

    ClientResponse response = httpGet( connection, CAN_CREATE_PATH, true );
    if ( response != null ) {
      return Boolean.parseBoolean( response.getEntity( String.class ) );
    }

    return false;
  }

  public boolean canExecute( final BiServerConnection connection ) {

    ClientResponse response = httpGet( connection, CAN_EXECUTE_PATH, true );
    if ( response != null ) {
      return Boolean.parseBoolean( response.getEntity( String.class ) );
    }

    return false;
  }

  public boolean isPentahoServer( final BiServerConnection connection ) {

    // fail immediately if url is empty
    if ( !isBiServerUrlProvided( connection ) ) {
      return false;
    }

    ClientResponse response = httpGet( connection, PENTAHO_WEBCONTEXT_PATH, false );
    if ( response != null ) {
      String content = response.getEntity( String.class );
      return ( content != null ) && ( content.contains( PENTAHO_WEBCONTEXT_MATCH ) );
    }

    return false;
  }

  public boolean isBiServerUrlProvided( final BiServerConnection connection ) {
    return connection != null && StringUtils.isNotBlank( connection.getUrl() );
  }

  public boolean isUserInfoProvided( final BiServerConnection connection ) {
    return
        connection != null
            && StringUtils.isNotBlank( connection.getUserId() )
            && StringUtils.isNotBlank( connection.getPassword() );
  }

  public int getLastHTTPStatus() {
    return lastHTTPStatus;
  }

}
