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

package org.pentaho.di.job.entries.publish;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.pentaho.database.model.DatabaseConnection;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.refinery.publish.agilebi.ModelServerPublish;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.entries.publish.exception.DuplicateDataSourceException;
import org.pentaho.di.trans.dataservice.client.DataServiceConnectionInformation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by bmorrise on 9/1/16.
 */
public class DatasourcePublishService implements PublishService {

  private static Class<?> PKG = JobEntryDatasourcePublish.class;

  private static final String METADATA_EXTENSION = ".xmi";
  private static final String ENCODING = "UTF-8";

  private LogChannelInterface log;

  public DatasourcePublishService( LogChannelInterface log ) {
    this.log = log;
  }

  public void publishDatabaseMeta( final ModelServerPublish modelServerPublish, final DatabaseMeta databaseMeta,
                                   final boolean forceOverride ) throws KettleException {

    if ( isKettleThinLocal( databaseMeta ) ) {
      throw new KettleException( getMsg( "JobEntryDatasourcePublish.Publish.LocalPentahoDataService" ) );
    }
    if ( isKettleThin( databaseMeta ) ) {
      databaseMeta.setForcingIdentifiersToLowerCase( false );
    }

    modelServerPublish.setDatabaseMeta( databaseMeta ); // provide database info

    // TODO Simple Check - Need to make this smarter and inspect the database connection
    DatabaseConnection connection = modelServerPublish.connectionNameExists( databaseMeta.getName() );

    try {
      boolean success;
      if ( forceOverride ) {
        if ( connection != null ) {
          success = modelServerPublish.publishDataSource( true, connection.getId() ); // update
        } else {
          success = modelServerPublish.publishDataSource( false, null ); // add
        }
      } else {
        // always use add operation, will fail if exists
        success = modelServerPublish.publishDataSource( false, connection != null ? connection.getId() : null );
      }

      if ( !success ) {
        throw new Exception( this.getMsg( "JobEntryDatasourcePublish.Publish.DBConnection.Failed", databaseMeta
          .getName() ) );
      }

    } catch ( KettleException ke ) {
      throw ke;
    } catch ( Exception e ) {
      throw new KettleException( e );
    }
    log.logBasic( this.getMsg( "JobEntryDatasourcePublish.Publish.DBConnection.Success", databaseMeta.getName() ) );
  }

  protected void deleteDatabaseMeta( final ModelServerPublish modelServerPublish, final DatabaseMeta databaseMeta )
    throws KettleException {

    if ( isKettleThinLocal( databaseMeta ) ) {
      throw new KettleException( getMsg( "JobEntryDatasourcePublish.Publish.LocalPentahoDataService" ) );
    }

    // TODO Simple Check - Need to make this smarter and inspect the database connection
    DatabaseConnection connection = modelServerPublish.connectionNameExists( databaseMeta.getName() );

    try {
      boolean success = true;
      if ( connection != null ) {
        success = modelServerPublish.deleteConnection( connection.getName() );
      }

      if ( !success ) {
        throw new Exception( this.getMsg( "JobEntryDatasourcePublish.Delete.DBConnection.Failed", databaseMeta
          .getName() ) );
      }

    } catch ( KettleException ke ) {
      throw ke;
    } catch ( Exception e ) {
      throw new KettleException( e );
    }
    log.logBasic( this.getMsg( "JobEntryDatasourcePublish.Delete.DBConnection.Success", databaseMeta.getName() ) );
  }

  protected void deleteXMI( final ModelServerPublish modelServerPublish, final String modelName, String dswFlag )
    throws KettleException {
    try {
      boolean success = true;
      if ( dswFlag != null && dswFlag.equalsIgnoreCase( "true" ) ) {
        success = modelServerPublish.deleteDSWXmi( checkDswId( modelName ) );
      } else {
        success = modelServerPublish.deleteMetadataXmi( modelName );
      }

      if ( !success ) {
        throw new Exception( this.getMsg( "JobEntryDatasourcePublish.Delete.XMI.Failed", modelName ) );
      }

    } catch ( KettleException ke ) {
      throw ke;
    } catch ( Exception e ) {
      throw new KettleException( e );
    }
    log.logBasic( this.getMsg( "JobEntryDatasourcePublish.Delete.XMI.Success", modelName ) );
  }

  protected String checkDswId( String modelName ) {
    if ( !modelName.endsWith( METADATA_EXTENSION ) ) {
      if ( StringUtils.endsWithIgnoreCase( modelName, METADATA_EXTENSION ) ) {
        modelName = StringUtils.removeEndIgnoreCase( modelName, METADATA_EXTENSION );
      }
      modelName += METADATA_EXTENSION;
    }
    return modelName;
  }

  public void publishMondrianSchema( final String modelName, final String mondrianSchema,
                                     final String mondrianDatasource, final ModelServerPublish modelServerPublish,
                                     final boolean forceOverride ) throws KettleException {

    if ( mondrianSchema == null || mondrianDatasource == null ) {
      return;
    }

    // Publish Mondrian Schema
    InputStream mondrianInputStream = null;
    try {
      mondrianInputStream = new ByteArrayInputStream( mondrianSchema.getBytes( ENCODING ) );
      modelServerPublish.setForceOverwrite( forceOverride );
      int status =
        modelServerPublish.publishMondrianSchema( mondrianInputStream, modelName, mondrianDatasource, forceOverride );
      if ( status != ModelServerPublish.PUBLISH_SUCCESS ) {
        throw new Exception( this.getMsg( "JobEntryDatasourcePublish.Publish.Mondrian.Failed", modelName ) );
      }
    } catch ( Exception e ) {
      throw new KettleException( e );
    } finally {
      IOUtils.closeQuietly( mondrianInputStream );
    }
    log.logBasic( this.getMsg( "JobEntryDatasourcePublish.Publish.Mondrian.Success", modelName ) );
  }

  public void publishMetadataXmi( final String modelName, final String xmiString,
                                  final ModelServerPublish modelServerPublish,
                                  final boolean forceOverride ) throws KettleException {

    if ( xmiString == null ) {
      return;
    }

    // Publish XMI
    InputStream xmiInputStream = null;
    try {
      xmiInputStream = new ByteArrayInputStream( xmiString.getBytes( ENCODING ) );
      modelServerPublish.setForceOverwrite( forceOverride );
      int status = modelServerPublish.publishMetaDataFile( xmiInputStream, modelName );
      if ( status != ModelServerPublish.PUBLISH_SUCCESS ) {
        throw new Exception( this.getMsg( "JobEntryDatasourcePublish.Publish.Metadata.Failed", modelName ) );
      }
    } catch ( Exception e ) {
      throw new KettleException( e );
    } finally {
      IOUtils.closeQuietly( xmiInputStream );
    }
    log.logBasic( this.getMsg( "JobEntryDatasourcePublish.Publish.Metadata.Success", modelName ) );
  }

  public void publishDswXmi( final String modelName, final String xmiString,
                             final ModelServerPublish modelServerPublish,
                             final boolean forceOverride ) throws KettleException {

    if ( xmiString == null ) {
      return;
    }

    // Publish XMI
    InputStream xmiInputStream = null;
    try {
      xmiInputStream = IOUtils.toInputStream( xmiString, ENCODING );
      modelServerPublish.setForceOverwrite( forceOverride );
      int status = modelServerPublish.publishDsw( xmiInputStream, checkDswId( modelName ) );
      if ( status == ModelServerPublish.PUBLISH_CONFLICT ) {
        throw new DuplicateDataSourceException( this.getMsg( "JobEntryDatasourcePublish.Publish.Dsw.Conflict", modelName ) );
      } else if ( status != ModelServerPublish.PUBLISH_SUCCESS ) {
        throw new Exception( this.getMsg( "JobEntryDatasourcePublish.Publish.Dsw.Failed", modelName ) );
      }
    } catch ( Exception e ) {
      throw new KettleException( e );
    } finally {
      IOUtils.closeQuietly( xmiInputStream );
    }
    log.logBasic( this.getMsg( "JobEntryDatasourcePublish.Publish.Dsw.Success", modelName ) );
  }


  private boolean isKettleThinLocal( final DatabaseMeta databaseMeta ) {
    return isKettleThin( databaseMeta )
      && "true".equals( databaseMeta.getExtraOptions().get( DataServiceConnectionInformation.KETTLE_THIN + ".local" ) );
  }

  private boolean isKettleThin( final DatabaseMeta databaseMeta ) {
    return DataServiceConnectionInformation.KETTLE_THIN.equals( databaseMeta.getDatabaseInterface().getPluginId() );
  }

  private String getMsg( String key, String... parameters ) {
    return BaseMessages.getString( PKG, key, parameters );
  }

}
