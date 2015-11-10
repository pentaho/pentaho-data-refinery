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

import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;
import org.pentaho.metastore.api.IMetaStore;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransUtilTest {
  @Test
  public void testCollectionsAllTableOutputSteps() throws Exception {
    final TransMeta transMeta = mock( TransMeta.class );
    final Repository repository = mock( Repository.class );
    final IMetaStore metastore = mock( IMetaStore.class );
    final StepMeta outputStep1 = mock( StepMeta.class );
    when( outputStep1.getName() ).thenReturn( "outputStep1" );
    final StepMeta outputStep2 = mock( StepMeta.class );
    when( outputStep2.getName() ).thenReturn( "outputStep2" );
    final StepMeta notOutputStep = mock( StepMeta.class );
    when( notOutputStep.getName() ).thenReturn( "notOutputStep" );
    when( transMeta.getSteps() ).thenReturn( Arrays.asList( outputStep1, outputStep2, notOutputStep ) );
    final StepMetaInterface outputInterface1 = mock( TableOutputMeta.class );
    when( outputStep1.getStepMetaInterface() ).thenReturn( outputInterface1 );
    final StepMetaInterface outputInterface2 = mock( TableOutputMeta.class );
    when( outputStep2.getStepMetaInterface() ).thenReturn( outputInterface2 );
    final StepMetaInterface notOutputInterface = mock( StepMetaInterface.class );
    when( notOutputStep.getStepMetaInterface() ).thenReturn( notOutputInterface );
    Map<String, ProvidesDatabaseConnectionInformation> stepMap =
        TransUtil.collectOutputStepInTrans( transMeta, repository, metastore );
    assertEquals( 2, stepMap.size() );
    assertNotNull( stepMap.get( "outputStep1" ) );
    assertNotNull( stepMap.get( "outputStep2" ) );
    assertNull( stepMap.get( "notOutputStep" ) );
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    if ( !KettleClientEnvironment.isInitialized() ) {
      KettleClientEnvironment.init();
    }
    PluginRegistry.addPluginType( StepPluginType.getInstance() );
    PluginRegistry.init();
    if ( !Props.isInitialized() ) {
      Props.init( 0 );
    }
  }

  @Test
  public void testResetParams() throws Exception {

    TransMeta meta = new TransMeta();
    String paramA = "paramA";
    meta.addParameterDefinition( paramA, "defA", "desc" );
    meta.setParameterValue( paramA, "other" );
    meta.setVariable( paramA, "other" );
    LogChannelInterface logChannel = mock( LogChannelInterface.class );

    TransUtil.resetParams( meta, logChannel );
    assertEquals( "defA", meta.getParameterValue( paramA ) );
    assertEquals( "defA", meta.getVariable( paramA ) );
  }
}
