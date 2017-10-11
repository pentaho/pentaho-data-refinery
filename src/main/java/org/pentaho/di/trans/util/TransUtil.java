/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.trans.util;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.base.AbstractMeta;
import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.parameters.UnknownParamException;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.metainject.MetaInjectMeta;
import org.pentaho.metastore.api.IMetaStore;

import java.util.HashMap;
import java.util.Map;

public class TransUtil {
  public static Map<String, ProvidesDatabaseConnectionInformation> collectOutputStepInTrans(
      final TransMeta transMeta, final Repository repository, final IMetaStore metastore )
    throws KettleException {
    HashMap<String, ProvidesDatabaseConnectionInformation> stepMap =
        new HashMap<String, ProvidesDatabaseConnectionInformation>();
    for ( StepMeta stepMeta : transMeta.getSteps() ) {
      ProvidesDatabaseConnectionInformation info = getDatabaseConnectionInformation( stepMeta.getStepMetaInterface() );
      if ( info != null ) {
        stepMap.put( StringUtils.trimToEmpty( stepMeta.getName() ), info );
      } else if ( stepMeta.getStepMetaInterface() instanceof MetaInjectMeta ) {
        MetaInjectMeta metaInject = ( (MetaInjectMeta) stepMeta.getStepMetaInterface() );
        TransMeta injectedTransMeta =
            MetaInjectMeta.loadTransformationMeta( metaInject, repository, metastore, transMeta );
        stepMap.putAll( collectOutputStepInTrans( injectedTransMeta, repository, metastore ) );
      }
    }
    return stepMap;
  }

  private static ProvidesDatabaseConnectionInformation getDatabaseConnectionInformation( Object o ) {
    if ( o != null && ProvidesDatabaseConnectionInformation.class.isAssignableFrom( o.getClass() ) ) {
      return ProvidesDatabaseConnectionInformation.class.cast( o );
    }
    return null;
  }

  public static void resetParams( final AbstractMeta meta, final LogChannelInterface logChannel ) {
    String[] params = meta.listParameters();
    for ( String key : params ) {
      try {
        meta.setParameterValue( key, meta.getParameterDefault( key ) );
        meta.setVariable( key, meta.getParameterDefault( key ) );
      } catch ( UnknownParamException e ) {
        //since the keys came from listParameters, should never get here
        logChannel.logDebug( "couldn't set param " + key );
      }
    }

  }
}
