/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2015 Pentaho Corporation (Pentaho). All rights reserved.
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

package org.pentaho.di.core.refinery.publish.agilebi;

import org.pentaho.database.model.IDatabaseType;
import org.pentaho.database.service.DatabaseDialectService;
import org.pentaho.database.util.DatabaseTypeHelper;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;

public class ModelServerAction {

  protected BiServerConnection biServerConnection;
  private Client _client = null;
  protected DatabaseMeta databaseMeta;

  public ModelServerAction() {
    super();
  }

  public ModelServerAction( BiServerConnection serverConnection ) {
    setBiServerConnection( serverConnection );
  }

  protected Client getClient() {

    // initialize
    if ( this._client == null ) {
      ClientConfig clientConfig = new DefaultClientConfig();
      clientConfig.getFeatures().put( JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE );
      this._client = Client.create( clientConfig );
    }

    return this._client;
  }

  protected IDatabaseType getDatabaseType( DatabaseInterface databaseInterface ) {
    DatabaseDialectService dds = new DatabaseDialectService();
    DatabaseTypeHelper dth = new DatabaseTypeHelper( dds.getDatabaseTypes() );
    return dth.getDatabaseTypeByShortName( databaseInterface.getPluginId() );
  }

  protected ClientResponse httpPut( final Builder builder ) {
    return builder.put( ClientResponse.class );
  }

  protected ClientResponse httpPost( final Builder builder ) {
    return builder.post( ClientResponse.class );
  }

  protected ClientResponse httpGet( final Builder builder ) {
    return builder.get( ClientResponse.class );
  }

  protected String getUrl( final String path ) {
    return biServerConnection.getUrl() + path;
  }

  protected WebResource getResource( final String path ) {
    return getClient().resource( getUrl( path ) );
  }

  /**
   * Sets the current BI server connection
   *
   * @param biServerConnection
   */
  public void setBiServerConnection( BiServerConnection biServerConnection ) {
    this.biServerConnection = biServerConnection;
    getClient()
        .addFilter( new HTTPBasicAuthFilter( biServerConnection.getUserId(), biServerConnection.getPassword() ) );
  }

  public DatabaseMeta getDatabaseMeta() {
    return databaseMeta;
  }

  public void setDatabaseMeta( DatabaseMeta databaseMeta ) {
    this.databaseMeta = databaseMeta;
  }

  /**
   * 2xx
   */
  public boolean isSuccess( ClientResponse response ) {
    return response.getStatus() >= 200 && response.getStatus() < 300;
  }

}
