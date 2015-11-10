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


package org.pentaho.di.core.refinery.publish.agilebi;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataMultiPart;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.pentaho.database.model.DatabaseAccessType;
import org.pentaho.database.model.DatabaseConnection;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.refinery.publish.model.DataSourceAclModel;
import org.pentaho.di.core.refinery.publish.util.JAXBUtils;

import javax.ws.rs.core.MediaType;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * This is copied from AgileBI's org.pentaho.agilebi.spoon.publish.ModelServerPublish
 */
public class ModelServerPublish extends ModelServerAction {

  public static final int PUBLISH_FAILED = 2;
  public static final int PUBLISH_SUCCESS = 3;
  public static final int PUBLISH_CATALOG_EXISTS = 8;
  private static final String REST_NAME_PARM = "?name=";
  private static final String MONDRIAN_POST_ANALYSIS_URL = "plugin/data-access/api/mondrian/postAnalysis";
  private static final String PLUGIN_DATA_ACCESS_API_CONNECTION_ADD = "plugin/data-access/api/connection/add";
  private static final String PLUGIN_DATA_ACCESS_API_CONNECTION_UPDATE = "plugin/data-access/api/connection/update";
  private static final String DATA_ACCESS_API_CONNECTION_GET = "plugin/data-access/api/connection/getresponse";
  private static Logger logger = Logger.getLogger( ModelServerPublish.class.getName() );
  private boolean forceOverwrite;
  private DataSourceAclModel aclModel;

  /**
   * Publishes a datasource to the current BI server
   *
   * @param update
   * @return
   * @throws org.pentaho.di.core.exception.KettleDatabaseException
   */
  public boolean publishDataSource( boolean update, String connectionId ) throws KettleDatabaseException {

    // create a new connection object and populate it from the databaseMeta
    DatabaseConnection connection = new DatabaseConnection();
    DatabaseInterface intf = getDatabaseMeta().getDatabaseInterface();

    connection.setId( connectionId );
    connection.setName( databaseMeta.getName() );
    connection.setPassword( getDatabaseMeta().environmentSubstitute( getDatabaseMeta().getPassword() ) );
    connection.setUsername( getDatabaseMeta().environmentSubstitute( getDatabaseMeta().getUsername() ) );
    connection.setDatabaseName( getDatabaseMeta().environmentSubstitute(  intf.getDatabaseName() ) );
    connection.setDatabasePort( getDatabaseMeta().environmentSubstitute(
            String.valueOf( intf.getAttributes().getProperty( "PORT_NUMBER" ) ) ) );
    connection.setHostname(  getDatabaseMeta().environmentSubstitute( getDatabaseMeta().getHostname() ) );
    connection.setForcingIdentifiersToLowerCase(
        "N".equals( intf.getAttributes().getProperty( "FORCE_IDENTIFIERS_TO_LOWERCASE" ) ) ? false : true );
    connection.setQuoteAllFields( "N".equals( intf.getAttributes().getProperty( "QUOTE_ALL_FIELDS" ) ) ? false : true );
    connection.setAccessType( DatabaseAccessType.NATIVE );
    connection.setExtraOptions( getDatabaseMeta().getExtraOptions() );

    connection.setDatabaseType( getDatabaseType( intf ) );
    return updateConnection( connection, update );
  }

  /**
   * Jersey call to add or update connection
   *
   * @param connection
   * @param update
   * @return
   */
  protected boolean updateConnection( DatabaseConnection connection, boolean update ) {
    String storeDomainUrl;
    try {
      if ( update ) {
        storeDomainUrl = biServerConnection.getUrl() + PLUGIN_DATA_ACCESS_API_CONNECTION_UPDATE;
      } else {
        storeDomainUrl = biServerConnection.getUrl() + PLUGIN_DATA_ACCESS_API_CONNECTION_ADD;
      }
      WebResource resource = getClient().resource( storeDomainUrl );
      Builder builder = resource
          .type( MediaType.APPLICATION_JSON )
          .entity( connection );

      ClientResponse resp = httpPost( builder );
      if ( resp == null || resp.getStatus() != 200 ) {
        return false;
      }
    } catch ( Exception ex ) {
      Log.error( ex.getMessage() );
      return false;
    }
    return true;
  }

  /**
   * Jersey call to use the put service to load a mondrain file into the Jcr repsoitory
   *
   * @param mondrianFile
   * @param catalogName
   * @param datasourceInfo
   * @param overwriteInRepos
   * @throws Exception
   */
  public int publishMondrianSchema( InputStream mondrianFile, String catalogName, String datasourceInfo,
      boolean overwriteInRepos ) throws Exception {
    String storeDomainUrl = biServerConnection.getUrl() + MONDRIAN_POST_ANALYSIS_URL;
    WebResource resource = getClient().resource( storeDomainUrl );
    String parms = "Datasource=" + datasourceInfo + ";retainInlineAnnotations=true";
    int response = PUBLISH_FAILED;
    FormDataMultiPart part = new FormDataMultiPart();
    part.field( "parameters", parms, MediaType.MULTIPART_FORM_DATA_TYPE )
        .field( "uploadAnalysis", mondrianFile, MediaType.MULTIPART_FORM_DATA_TYPE )
        .field( "catalogName", catalogName, MediaType.MULTIPART_FORM_DATA_TYPE )
        .field( "overwrite", overwriteInRepos ? "true" : "false", MediaType.MULTIPART_FORM_DATA_TYPE )
        .field( "xmlaEnabledFlag", "true", MediaType.MULTIPART_FORM_DATA_TYPE );

    addAclToRequest( part );

    // If the import service needs the file name do the following.
    part.getField( "uploadAnalysis" ).setContentDisposition(
        FormDataContentDisposition.name( "uploadAnalysis" ).fileName( catalogName ).build() );
    try {
      Builder builder = resourceBuilder( resource, part );
      ClientResponse resp = httpPost( builder );
      String entity = null;
      if ( resp != null && resp.getStatus() == 200 ) {
        entity = resp.getEntity( String.class );
        if ( entity.equals( String.valueOf( PUBLISH_CATALOG_EXISTS ) ) ) {
          response = PUBLISH_CATALOG_EXISTS;
        } else {
          response = Integer.parseInt( entity );
        }
      } else {
        Log.info( resp );
      }
    } catch ( Exception ex ) {
      Log.error( ex.getMessage() );
    }
    return response;
  }

  Builder resourceBuilder( final WebResource resource, final FormDataMultiPart part ) {
    return resource
        .type( MediaType.MULTIPART_FORM_DATA_TYPE )
        .entity( part );
  }

  /**
   * Jersey call to use the put service to load a metadataFile file into the Jcr repsoitory
   *
   * @param metadataFile
   * @param domainId     is fileName
   * @throws Exception return code to detrmine next step
   */
  public int publishMetaDataFile( InputStream metadataFile, String domainId ) throws Exception {
    String storeDomainUrl = biServerConnection.getUrl() + "plugin/data-access/api/metadata/import";
    WebResource resource = getClient().resource( storeDomainUrl );

    int response = PUBLISH_FAILED;
    FormDataMultiPart part = new FormDataMultiPart();
    part.field( "domainId", domainId, MediaType.MULTIPART_FORM_DATA_TYPE )
        .field( "metadataFile", metadataFile, MediaType.MULTIPART_FORM_DATA_TYPE );

    if ( this.isForceOverwrite() ) {
      part.field( "overwrite", this.isForceOverwrite() + "", MediaType.MULTIPART_FORM_DATA_TYPE );
    }

    addAclToRequest( part );

    part.getField( "metadataFile" ).setContentDisposition(
        FormDataContentDisposition.name( "metadataFile" )
            .fileName( domainId ).build() );
    try {
      Builder builder = resourceBuilder( resource, part );
      ClientResponse resp = httpPut( builder );
      if ( resp != null && resp.getStatus() == 200 ) {
        if ( resp.getEntity( String.class ).equals( PUBLISH_SUCCESS + "" ) ) {
          response = PUBLISH_SUCCESS;
        }
      }
    } catch ( Exception ex ) {
      Log.error( ex.getMessage() );
    }
    return response;
  }

  public int publishDsw( InputStream metadataFile, String domainId ) throws Exception {
    if ( !StringUtils.endsWith( domainId, ".xmi" ) ) {
      throw new IllegalArgumentException( "Domain ID for DSW must end in .xmi" );
    }

    final String publishDswUrl = biServerConnection.getUrl() + "plugin/data-access/api/datasource/dsw/import";
    WebResource resource = getClient().resource( publishDswUrl );

    FormDataMultiPart part = new FormDataMultiPart();
    part.field( "domainId", domainId, MediaType.MULTIPART_FORM_DATA_TYPE )
        .field( "metadataFile", metadataFile, MediaType.MULTIPART_FORM_DATA_TYPE );
    if ( this.isForceOverwrite() ) {
      part.field( "overwrite", Boolean.toString( this.isForceOverwrite() ), MediaType.MULTIPART_FORM_DATA_TYPE );
    }
    addAclToRequest( part );


    // TODO do we want this?
    part.field( "checkConnection", Boolean.TRUE.toString(), MediaType.MULTIPART_FORM_DATA_TYPE );

    try {
      Builder builder = resourceBuilder( resource, part );
      ClientResponse resp = httpPut( builder );
      if ( resp != null ) {
        // TODO: we can get more info from the response;
        switch ( ClientResponse.Status.fromStatusCode( resp.getStatus() ) ) {
          case OK:
          case CREATED:
            return PUBLISH_SUCCESS;
          default:
            return PUBLISH_FAILED;
        }
      }
    } catch ( Exception ex ) {
      Log.error( ex.getMessage() );
    }
    return PUBLISH_FAILED;
  }

  private void addAclToRequest( FormDataMultiPart part ) {
    if ( this.aclModel != null ) {
      String xml = this.aclModel.toXml();
      if ( xml != null ) {
        part.field( "acl", this.aclModel.toXml(), MediaType.MULTIPART_FORM_DATA_TYPE );
      }
    }
  }

  private void error( String message ) {
    logger.severe( message );
  }

  private void success( String message ) {
    logger.info( message );
  }

  public DatabaseConnection connectionNameExists( String connectionName ) {

    if ( StringUtils.isBlank( connectionName ) ) {
      return null;
    }

    try {
      String storeDomainUrl =
          biServerConnection.getUrl() + DATA_ACCESS_API_CONNECTION_GET + REST_NAME_PARM + connectionName;
      storeDomainUrl = URIUtil.encodeQuery( storeDomainUrl );
      WebResource resource = getClient().resource( storeDomainUrl );
      Builder builder = resource
          .type( MediaType.APPLICATION_JSON )
          .type( MediaType.APPLICATION_XML );
      ClientResponse response = httpGet( builder );
      if ( response != null && response.getStatus() == 200 ) {

        String payload = response.getEntity( String.class );
        DatabaseConnection connection = JAXBUtils.unmarshalFromJson( payload, DatabaseConnection.class );

        return connection;
      }
    } catch ( Exception ex ) {
      Log.error( ex.getMessage() );
    }

    return null;
  }

  public boolean isForceOverwrite() {
    return forceOverwrite;
  }

  public void setForceOverwrite( boolean forceOverwrite ) {
    this.forceOverwrite = forceOverwrite;
  }

  public DataSourceAclModel getAclModel() {
    return aclModel;
  }

  public void setAclModel( DataSourceAclModel aclModel ) {
    this.aclModel = aclModel;
  }
}
