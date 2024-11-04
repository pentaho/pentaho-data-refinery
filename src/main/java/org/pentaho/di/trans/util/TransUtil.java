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
      } else if ( stepMeta != null && stepMeta.getStepMetaInterface() != null ) {

        TransMeta relatedTransMeta = stepMeta.getStepMetaInterface().fetchTransMeta( stepMeta.getStepMetaInterface(),
            repository, metastore, transMeta );

        if ( relatedTransMeta != null ) {
          stepMap.putAll( collectOutputStepInTrans( relatedTransMeta, repository, metastore ) );
        }
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
