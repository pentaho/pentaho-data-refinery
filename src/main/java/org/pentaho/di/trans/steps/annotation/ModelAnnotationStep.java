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

package org.pentaho.di.trans.steps.annotation;

import org.apache.commons.lang.StringUtils;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import static org.pentaho.di.job.entries.build.JobEntryBuildModel.KEY_MODEL_ANNOTATIONS;
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
  public boolean processRow( final StepMetaInterface smi, final StepDataInterface sdi )
    throws KettleException {


    Object[] row = getRow();
    if ( first ) {
      first = false;
      ModelAnnotationMeta modelAnnotationMeta = (ModelAnnotationMeta) smi;
      ModelAnnotationData modelAnnotationData = (ModelAnnotationData) sdi;
      if ( modelAnnotationMeta.isSharedDimension()
          && !isOutputStepFound( modelAnnotationMeta.getTargetOutputStep() ) ) {
        throw new KettleException( BaseMessages.getString( PKG, "ModelAnnotation.Runtime.MissingDataProvider" ) );
      }
      modelAnnotationData.annotations = processAnnotations( modelAnnotationMeta );
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
  private ModelAnnotationGroup processAnnotations( ModelAnnotationMeta modelAnnotationMeta ) throws KettleException {

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

    if ( getTrans().getParentJob() != null ) {
      ModelAnnotationGroup allAnnotations = getExistingAnnotations();
      if ( !currentGroup.isSharedDimension() ) {
        allAnnotations.addAll( currentGroup );
      }
      getTrans().getParentJob().getExtensionDataMap().put( KEY_MODEL_ANNOTATIONS, allAnnotations );
    }
    return currentGroup;
  }

  private boolean isGroupLinked( ModelAnnotationMeta meta ) {
    return !StringUtils.isBlank( environmentSubstitute( meta.getModelAnnotationCategory() ) );
  }

  protected ModelAnnotationGroup fetchAnnotations( final String groupName, ModelAnnotationMeta modelAnnotationMeta )
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
        throw new KettleException( BaseMessages.getString( PKG, "ModelAnnotation.Runtime.GroupNotFound", groupName ) );
      }
      return group;
    } catch ( MetaStoreException e ) {
      // bad args, serialization issues..
      throw new KettleException( e );
    }
  }

  protected ModelAnnotationManager getModelAnnotationsManager( ModelAnnotationMeta modelAnnotationMeta ) {
    if ( modelAnnotationMeta.isSharedDimension() ) {
      return new ModelAnnotationManager( true );
    }

    return new ModelAnnotationManager();
  }

  private void validateMeasuresNumeric( ModelAnnotationGroup annotations )
    throws KettleException {
    if ( getInputRowMeta() != null ) {
      for ( ModelAnnotation<?> annotation : annotations ) {
        if ( annotation.getType() != null && annotation.getType().equals( ModelAnnotation.Type.CREATE_MEASURE ) ) {
          for ( ValueMetaInterface valueMeta : getInputRowMeta().getValueMetaList() ) {
            if ( !valueMeta.isNumeric() && valueMeta.getName().equals( annotation.getAnnotation().getField() ) ) {
              CreateMeasure createMeasure = (CreateMeasure) annotation.getAnnotation();
              if ( !createMeasure.getAggregateType().equals( AggregationType.COUNT )
                  && !createMeasure.getAggregateType().equals( AggregationType.COUNT_DISTINCT ) ) {
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
        ? null
        : getTrans().getParentJob().getExtensionDataMap().get( KEY_MODEL_ANNOTATIONS );
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
