/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
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
