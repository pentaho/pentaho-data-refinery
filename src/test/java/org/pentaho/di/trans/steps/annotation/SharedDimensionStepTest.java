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

import org.junit.Test;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateDimensionKey;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.job.Job;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.metastore.api.IMetaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SharedDimensionStepTest extends InitializeLogging {

  private SharedDimensionStep createSharedDimensionStep( StepDataInterface stepDataInterface, IMetaStore metaStore ) {
    StepMeta stepMeta = mock( StepMeta.class );
    TransMeta transMeta = mock( TransMeta.class );
    final Trans trans = mock( Trans.class );
    when( stepMeta.getName() ).thenReturn( "someName" );
    when( transMeta.findStep( "someName" ) ).thenReturn( stepMeta );
    Job job = mock( Job.class );
    StepMetaDataCombi stepMetaDataCombi = new StepMetaDataCombi();
    stepMetaDataCombi.stepname = "step name";
    SharedDimensionStep sharedDimensionStep = new SharedDimensionStep( stepMeta, stepDataInterface, 1, transMeta, trans ) {
      @Override public Object[] getRow() throws KettleException {
        return new Object[] {};
      }

      @Override public Trans getTrans() {
        return trans;
      }

    };
    sharedDimensionStep.setLogLevel( LogLevel.BASIC );
    sharedDimensionStep.setMetaStore( metaStore );
    return sharedDimensionStep;
  }

  @Test
  public void testInit() throws Exception {
    // step
    StepDataInterface stepDataInterface = new ModelAnnotationData();
    SharedDimensionStep modelAnnotationStep = createSharedDimensionStep( stepDataInterface, null );

    SharedDimensionMeta meta = new SharedDimensionMeta();

    List<CreateDimensionKey> dimKeyAnnotations = new ArrayList<>();
    dimKeyAnnotations.add( new CreateDimensionKey() );
    meta.createDimensionKeyAnnotations = dimKeyAnnotations;

    List<CreateAttribute> attrAnnotations = new ArrayList<>();
    attrAnnotations.add( new CreateAttribute() );
    meta.createAttributeAnnotations = attrAnnotations;

    meta.sharedDimensionName = "myName";

    ModelAnnotationGroup modelAnnotationGroup = new ModelAnnotationGroup();
    meta.setModelAnnotations( modelAnnotationGroup );

    boolean status = modelAnnotationStep.init( meta, stepDataInterface );
    assertEquals( 2, modelAnnotationGroup.size() );
    assertEquals( ModelAnnotation.Type.CREATE_DIMENSION_KEY, modelAnnotationGroup.get( 0 ).getType() );
    assertEquals( ModelAnnotation.Type.CREATE_ATTRIBUTE, modelAnnotationGroup.get( 1 ).getType() );
    assertEquals( "myName", modelAnnotationGroup.getName() );
    assertTrue( status );
  }

  @Test
  public void testNoInjectionDoesNotSaveToMetastore() throws Exception {
    // step
    StepDataInterface stepDataInterface = new ModelAnnotationData();
    SharedDimensionStep modelAnnotationStep = createSharedDimensionStep( stepDataInterface, null );

    SharedDimensionMeta meta = new SharedDimensionMeta() {
      @Override public void saveToMetaStore( IMetaStore metaStore )
        throws Exception {
        fail( "should not try and save to metastore because not meta injected" );
      }
    };

    meta.sharedDimensionName = "myName";
    boolean status = modelAnnotationStep.init( meta, stepDataInterface );
    assertTrue( status );
  }
}
