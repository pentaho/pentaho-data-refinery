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

package org.pentaho.di.trans.steps.annotation;

import org.apache.commons.lang.StringUtils;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metastore.api.exceptions.MetaStoreException;

/**
 * @author Rowell Belen
 */
public class ModelAnnotationStep extends BaseStep implements StepInterface {

  private static final Class<?> PKG = ModelAnnotationMeta.class;

  /**
   * This is the base step that forms that basis for all steps. You can derive from this class to implement your own
   * steps.
   *
   * @param stepMeta          The StepMeta object to run.
   * @param stepDataInterface the data object to store temporary data, database connections, caches, result sets, hashtables etc.
   * @param copyNr            The copynumber for this step.
   * @param transMeta         The TransInfo of which the step stepMeta is part of.
   * @param trans
   */
  public ModelAnnotationStep( StepMeta stepMeta,
      StepDataInterface stepDataInterface, int copyNr,
      TransMeta transMeta, Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  @Override
  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    // SharedDimensionStep inherits from this but SharedDimensionMeta doesn't inherit from ModelAnnotationMeta
    BaseAnnotationMeta bmeta = (BaseAnnotationMeta) smi;

    if ( !bmeta.isSharedDimension() ) {
      ModelAnnotationMeta meta = (ModelAnnotationMeta) bmeta;
      if ( StringUtils.isNotEmpty( meta.sharedAnnotationGroup ) ) {
        meta.setModelAnnotationCategory( meta.sharedAnnotationGroup );
      }
      ModelAnnotationGroup modelAnnotations = meta.getModelAnnotations();
      // if a shared group is referenced, assume we should not be injecting any annotations. they will come from there
      if ( StringUtils.isEmpty( meta.getModelAnnotationCategory() ) ) {
        if ( modelAnnotations == null ) {
          modelAnnotations = new ModelAnnotationGroup();
          meta.setModelAnnotations( modelAnnotations );
        }

        modelAnnotations.addInjectedAnnotations( meta.createMeasureAnnotations );
        modelAnnotations.addInjectedAnnotations( meta.createAttributeAnnotations );
        modelAnnotations.addInjectedAnnotations( meta.createLinkDimensionAnnotations );

        // default all calc measure annotations to the Measures dimension
        meta.createCalcMeasureAnnotations.stream().forEach( calc -> calc.setDimension( "Measures" ) );
        modelAnnotations.addInjectedAnnotations( meta.createCalcMeasureAnnotations );
      }
    }
    try {
      ModelAnnotationData modelAnnotationData = (ModelAnnotationData) sdi;
      if ( bmeta.isSharedDimension()
          && !isOutputStepFound( bmeta.getTargetOutputStep() ) ) {
        log.logError( BaseMessages.getString( PKG, "ModelAnnotation.Runtime.MissingDataProvider" ) );
      } else {
        modelAnnotationData.annotations = processAnnotations( bmeta );
      }
    } catch ( KettleException e ) {
      log.logError( e.getLocalizedMessage(), e );
      return false;
    }
    final boolean superInit = super.init( smi, sdi );
    return superInit;
  }

  @Override
  public boolean processRow( final StepMetaInterface smi, final StepDataInterface sdi ) throws KettleException {
    Object[] row = getRow();
    if ( first && row != null ) {
      first = false;
    }
    if ( row == null ) { // no more input to be expected...
      setOutputDone();
      return false;
    }
    putRow( getInputRowMeta(), row );
    return true;
  }

  /**
   * exposes an annotation group to build model
   *
   * @param modelAnnotationMeta
   * @throws KettleException
   */
  private ModelAnnotationGroup processAnnotations( BaseAnnotationMeta modelAnnotationMeta ) throws KettleException {

    ModelAnnotationGroup currentGroup;
    if ( isGroupLinked( modelAnnotationMeta ) ) {
      // use meta store annotations
      String groupName = environmentSubstitute( modelAnnotationMeta.getModelAnnotationCategory() );
      currentGroup = fetchAnnotations( groupName, modelAnnotationMeta );
    } else {
      // use locally defined annotations
      currentGroup = modelAnnotationMeta.getModelAnnotations();
    }
    validateMeasuresNumeric( currentGroup );

    ModelAnnotationGroup allAnnotations = getExistingAnnotations();
    if ( !currentGroup.isSharedDimension() ) {
      allAnnotations.addAll( currentGroup );
    }
    if ( getTrans().getParentJob() != null ) {
      getTrans().getParentJob().getExtensionDataMap().put( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS, allAnnotations );
    } else {
      getTrans().getExtensionDataMap().put( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS, allAnnotations );
    }
    return currentGroup;
  }

  private boolean isGroupLinked( BaseAnnotationMeta meta ) {
    return !StringUtils.isBlank( environmentSubstitute( meta.getModelAnnotationCategory() ) );
  }

  protected ModelAnnotationGroup fetchAnnotations( final String groupName, BaseAnnotationMeta modelAnnotationMeta )
    throws KettleException {
    if ( StringUtils.isBlank( groupName ) ) {
      throw new IllegalArgumentException( "no group" );
    }
    if ( getMetaStore() == null ) {
      throw new KettleException( BaseMessages.getString( PKG, "ModelAnnotation.Runtime.NoMetastore" ) );
    }
    try {
      ModelAnnotationManager mgr = getModelAnnotationsManager( modelAnnotationMeta );
      ModelAnnotationGroup group = mgr.readGroup( groupName, metaStore );
      if ( group == null ) {
        throw new KettleException( BaseMessages.getString(
          PKG, modelAnnotationMeta.isSharedDimension()
            ? "ModelAnnotation.Runtime.GroupNotFound" : "ModelAnnotation.Runtime.AnnotationGroupNotFound", groupName ) );
      }
      return group;
    } catch ( MetaStoreException e ) {
      // bad args, serialization issues..
      throw new KettleException( e );
    }
  }

  protected ModelAnnotationManager getModelAnnotationsManager( BaseAnnotationMeta modelAnnotationMeta ) {
    if ( modelAnnotationMeta.isSharedDimension() ) {
      return new ModelAnnotationManager( true );
    }

    return new ModelAnnotationManager();
  }


  private void validateMeasuresNumeric( ModelAnnotationGroup annotations ) throws KettleException {
    RowMetaInterface inputRowMeta = getInputRowMeta();
    if (  inputRowMeta == null ) {
      inputRowMeta = getTransMeta().getPrevStepFields( getStepMeta() );
    }
    if ( inputRowMeta != null ) {
      for ( ModelAnnotation<?> annotation : annotations ) {
        if ( annotation.getType() != null && annotation.getType().equals( ModelAnnotation.Type.CREATE_MEASURE ) ) {
          for ( ValueMetaInterface valueMeta : inputRowMeta.getValueMetaList() ) {
            if ( !valueMeta.isNumeric() && valueMeta.getName().equals( annotation.getAnnotation().getField() ) ) {
              CreateMeasure createMeasure = (CreateMeasure) annotation.getAnnotation();
              if ( !createMeasure.getAggregateType().equals( AggregationType.COUNT ) && !createMeasure
                  .getAggregateType().equals( AggregationType.COUNT_DISTINCT ) ) {
                throw new KettleException( BaseMessages.getString( PKG, "ModelAnnotation.Runtime.NonNumericMeasure",
                    createMeasure.getAggregateType().name() ) );
              }
            }
          }
        }
      }
    }
  }

  private ModelAnnotationGroup getExistingAnnotations() {
    Object o = ( getTrans().getParentJob() == null )
        ? getTrans().getExtensionDataMap().get( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS )
        : getTrans().getParentJob().getExtensionDataMap().get( JobEntryBuildModel.KEY_MODEL_ANNOTATIONS );
    if ( o == null ) {
      return new ModelAnnotationGroup();
    } else {
      return (ModelAnnotationGroup) o;
    }
  }

  private boolean isOutputStepFound( final String outputStep ) throws KettleException {
    if ( !Const.isEmpty( outputStep ) ) {
      for ( StepMetaDataCombi outCombi : getTrans().getSteps() ) {
        if ( outCombi.stepname.equals( environmentSubstitute( outputStep ) ) ) {
          return true;
        }
      }
    }
    return false;
  }

}
