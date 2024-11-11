/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.job.entries.publish;

import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.refinery.publish.agilebi.ModelServerPublish;

/**
 * Created by bmorrise on 8/31/16.
 */
public interface PublishService {
  void publishDatabaseMeta( final ModelServerPublish modelServerPublish, final DatabaseMeta databaseMeta,
                            final boolean forceOverride ) throws KettleException;

  void publishMondrianSchema( final String modelName, final String mondrainSchema, final String mondrianDatasource,
                              final ModelServerPublish modelServerPublish,
                              final boolean forceOverride ) throws KettleException;

  void publishMetadataXmi( final String modelName, final String xmiString, final ModelServerPublish modelServerPublish,
                           final boolean forceOverride ) throws KettleException;

  void publishDswXmi( final String modelName, final String xmiString, final ModelServerPublish modelServerPublish,
                      final boolean forceOverride )
    throws KettleException;
}
