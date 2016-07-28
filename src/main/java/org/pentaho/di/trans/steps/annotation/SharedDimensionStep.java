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

package org.pentaho.di.trans.steps.annotation;

import org.apache.commons.lang.StringUtils;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

public class SharedDimensionStep extends ModelAnnotationStep implements StepInterface {

  private static Class<?> PKG = SharedDimensionStep.class; // for i18n purposes, needed by Translator2!!

  public SharedDimensionStep( StepMeta stepMeta,
      StepDataInterface stepDataInterface, int copyNr,
      TransMeta transMeta, Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  @Override public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {

    SharedDimensionMeta meta = (SharedDimensionMeta) smi;
    meta.setSharedDimension( true );
    if ( StringUtils.isNotEmpty( meta.sharedDimensionName ) ) {
      meta.setModelAnnotationCategory( meta.sharedDimensionName );
    }
    if ( StringUtils.isNotEmpty( meta.dataProviderStep ) ) {
      meta.setTargetOutputStep( meta.dataProviderStep );
    }

    ModelAnnotationGroup modelAnnotations = meta.getModelAnnotations();
    if ( modelAnnotations == null ) {
      modelAnnotations = new ModelAnnotationGroup();
      meta.setModelAnnotations( modelAnnotations );
    }

    meta.getModelAnnotations().setName( meta.getModelAnnotationCategory() );
    modelAnnotations.addInjectedAnnotations( meta.createDimensionKeyAnnotations );
    modelAnnotations.addInjectedAnnotations( meta.createAttributeAnnotations );

    try {
      meta.saveToMetaStore( getMetaStore() );
    } catch ( Exception e ) {
      logError( e.getMessage(), e );
    }

    return super.baseInit( smi, sdi );
  }
}
