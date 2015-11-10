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

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.model.ResponseStatus;
import org.springframework.stereotype.Component;

/**
 * @author Rowell Belen
 */
@Component
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
