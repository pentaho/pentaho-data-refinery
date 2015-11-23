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

package org.pentaho.di.core.refinery.publish.agilebi;

import org.pentaho.database.IDatabaseDialect;
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
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    IDatabaseType dbType = null;
    try {
      Thread.currentThread().setContextClassLoader( IDatabaseDialect.class.getClassLoader() );

      DatabaseDialectService dds = new DatabaseDialectService();
      DatabaseTypeHelper dth = new DatabaseTypeHelper( dds.getDatabaseTypes() );
      dbType = dth.getDatabaseTypeByShortName( databaseInterface.getPluginId() );
    } finally {
      Thread.currentThread().setContextClassLoader( orig );
      return dbType;
    }
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
