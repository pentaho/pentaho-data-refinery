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
