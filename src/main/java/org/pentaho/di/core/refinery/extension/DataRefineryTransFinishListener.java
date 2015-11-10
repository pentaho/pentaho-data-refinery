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
package org.pentaho.di.core.refinery.extension;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.extension.ExtensionPoint;
import org.pentaho.di.core.extension.ExtensionPointInterface;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.refinery.DataProviderHelper;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.step.BaseStepData.StepExecutionStatus;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationData;
import org.pentaho.di.trans.steps.annotation.ModelAnnotationMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.api.exceptions.MetaStoreException;

@ExtensionPoint( id = "DataRefineryTransFinishListener", description = "Updates data provider info on annotated steps.",
    extensionPointId = "TransformationFinish" )

public class DataRefineryTransFinishListener implements ExtensionPointInterface {

  private static final Class<?> PKG = ModelAnnotationMeta.class;

  @Override
  public void callExtensionPoint( LogChannelInterface log, Object object ) throws KettleException {
    Trans trans = (Trans) object;
    try {
      if ( trans.getParentJob() != null ) {
        setBuildModelOutputStep( trans.getParentJob(), trans );
      }
    } catch ( KettleException e ) {
      log.logError( e.getLocalizedMessage() );
    } catch ( Exception e ) {
      log.logError( "Error setting output step for Build Model", e );
    }
    try {
      IMetaStore metaStore = trans.getMetaStore();
      updateDataProviders( log, trans, metaStore );
    } catch ( KettleException e ) {
      log.logError( e.getLocalizedMessage() );
    } catch ( Exception e ) {
      log.logError( "Error processing data providers for annotations.", e );
    }
  }

  /**
   * Sets magic variables required for build model to use output steps
   */
  public void setBuildModelOutputStep( Job job, Trans trans ) throws KettleException {
    for ( JobEntryCopy jeCopy : job.getJobMeta().getJobCopies() ) {
      if ( JobEntryBuildModel.PLUGIN_ID.equals( jeCopy.getEntry().getPluginId() ) ) {
        JobEntryBuildModel jeBuildModel = (JobEntryBuildModel) jeCopy.getEntry();
        final String outputStepName =
            StringUtils.trimToNull( job.environmentSubstitute( jeBuildModel.getOutputStep() ) );
        if ( outputStepName == null ) {
          continue;
        }
        for ( StepMetaDataCombi stepMetaData : trans.getSteps() ) {
          String stepName = StringUtils.trimToNull( stepMetaData.stepname );
          if ( outputStepName.equals( stepName ) ) {
            Map<String, Object> map = job.getExtensionDataMap();
            String key = JobEntryBuildModel.KEY_OUTPUT_STEP_PREFIX + jeBuildModel.getName();
            if ( map.containsKey( key ) ) {
              throw new KettleException(
                  "Unable to auto-model because more than one step with the same name was found: "
                      + stepMetaData.stepname );
            }
            map.put( key, stepMetaData );
          }
        }
      }
    }
  }

  /**
   * Updates shared annotation groups with data providers
   */
  public void updateDataProviders( LogChannelInterface log, Trans trans, IMetaStore metaStore )
    throws KettleException, MetaStoreException {
    log.logDebug( "searching for annotations" );
    boolean hasAnnotations = false;

    for ( StepMetaDataCombi combi : trans.getSteps() ) {
      if ( combi.meta instanceof ModelAnnotationMeta ) {
        hasAnnotations = true;
        log.logDebug( "found annotations step '" + combi.stepname + "'" );
        ModelAnnotationData maData = (ModelAnnotationData) combi.data;
        if ( maData.annotations != null && maData.annotations.isSharedDimension() ) {
          if ( metaStore == null ) {
            log.logError( BaseMessages.getString( PKG, "ModelAnnotation.Runtime.NoMetastore" ) );
            return;
          }
          log.logDebug( "found shared dimension " + maData.annotations.getName() );
          StepMetaDataCombi outCombi = getOutputStep( combi, trans );
          // TransformationFinish is called before last step is marked as not running,
          // so that step will never report STATUS_FINISHED; using status of data instead
          if ( outCombi.data.getStatus() == StepExecutionStatus.STATUS_DISPOSED ) {
            DataProviderHelper dataProviderHelper = getDataProviderHelper( metaStore );
            dataProviderHelper.updateDataProvider( maData.annotations, outCombi );
          } else {
            log.logError( BaseMessages.getString( PKG, "ModelAnnotation.Runtime.OutputStepFail",
                outCombi.stepname,
                outCombi.data.getStatus() ) );
          }
        }
      }
    }
    if ( !hasAnnotations ) {
      log.logDebug( "no annotations found" );
    }
  }

  protected DataProviderHelper getDataProviderHelper( IMetaStore mstore ) {
    return new DataProviderHelper( mstore );
  }

  private StepMetaDataCombi getOutputStep( StepMetaDataCombi annotationCombi, final Trans trans )
    throws KettleException {
    final String outStepName = getOutputStepName( annotationCombi, trans );
    for ( StepMetaDataCombi outCombi : annotationCombi.step.getTrans().getSteps() ) {
      if ( outCombi.stepname.equals( outStepName ) ) {
        return outCombi;
      }
    }
    throw new KettleException(
        BaseMessages.getString( JobEntryBuildModel.class, "BuildModelJob.Error.UnableToFindStep", outStepName ) );
  }

  private String getOutputStepName( StepMetaDataCombi annotationCombi, final Trans trans ) throws KettleException {
    if ( annotationCombi.meta instanceof ModelAnnotationMeta ) {
      String outputStepName =
          trans.environmentSubstitute( ( (ModelAnnotationMeta) annotationCombi.meta ).getTargetOutputStep() );
      if ( StringUtils.isNotBlank( outputStepName ) ) {
        return outputStepName;
      }
    }
    throw new KettleException( "output step not set" );
  }
}

