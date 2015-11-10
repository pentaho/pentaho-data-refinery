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

package org.pentaho.di.ui.trans.steps.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.nodes.TimeRole;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metastore.api.IMetaStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OptionsResolverTest {

  private OptionsResolver optionsResolver;

  @Before
  public void init() {
    optionsResolver = new OptionsResolver();
  }

  @Test
  public void testResolveTimeSourceFormatOptions() {

    assertEquals( 0, optionsResolver.resolveTimeSourceFormatOptions( null ).length );
    assertEquals( 0, optionsResolver.resolveTimeSourceFormatOptions( "" ).length );
    assertEquals( 0, optionsResolver.resolveTimeSourceFormatOptions( "MISSING" ).length );

    assertEquals( TimeRole.YEARS.formats.length,
        optionsResolver.resolveTimeSourceFormatOptions( ModelAnnotation.TimeType.TimeYears.name() ).length );
    assertEquals( TimeRole.HALFYEARS.formats.length,
        optionsResolver.resolveTimeSourceFormatOptions( ModelAnnotation.TimeType.TimeHalfYears.name() ).length );
    assertEquals( TimeRole.QUARTERS.formats.length,
        optionsResolver.resolveTimeSourceFormatOptions( ModelAnnotation.TimeType.TimeQuarters.name() ).length );
    assertEquals( TimeRole.MONTHS.formats.length,
        optionsResolver.resolveTimeSourceFormatOptions( ModelAnnotation.TimeType.TimeMonths.name() ).length );
    assertEquals( TimeRole.WEEKS.formats.length,
        optionsResolver.resolveTimeSourceFormatOptions( ModelAnnotation.TimeType.TimeWeeks.name() ).length );
    assertEquals( TimeRole.DAYS.formats.length,
        optionsResolver.resolveTimeSourceFormatOptions( ModelAnnotation.TimeType.TimeDays.name() ).length );
    assertEquals( TimeRole.HOURS.formats.length,
        optionsResolver.resolveTimeSourceFormatOptions( ModelAnnotation.TimeType.TimeHours.name() ).length );
    assertEquals( TimeRole.MINUTES.formats.length,
        optionsResolver.resolveTimeSourceFormatOptions( ModelAnnotation.TimeType.TimeMinutes.name() ).length );
    assertEquals( TimeRole.SECONDS.formats.length,
        optionsResolver.resolveTimeSourceFormatOptions( ModelAnnotation.TimeType.TimeSeconds.name() ).length );
  }

  @Test
  public void testResolveMeasureFormatOptions() {
    assertEquals( 5, optionsResolver.resolveMeasureFormatOptions().length );
  }

  @Test
  public void testResolveAggregationTypeOptions() {
    assertEquals( AggregationType.values().length - 1, optionsResolver.resolveAggregationTypeOptions().length );
  }

  @Test
  public void testResolveAggregationTypeOptionsForNonNumericMeasures() {

    CreateMeasure cm = new CreateMeasure();

    String[] options = optionsResolver.resolveAggregationTypeOptions( new ValueMetaString( "f1" ), cm );
    assertTrue( options.length == 2 );

    options = optionsResolver.resolveAggregationTypeOptions( new ValueMetaInteger( "f1" ), cm );
    assertEquals( AggregationType.values().length - 1, options.length );
  }

  @Test
  public void testResolveBooleanOptions() {
    assertEquals( 2, optionsResolver.resolveBooleanOptions().length );
  }

  @Test
  public void testResolveOrdinalFieldOptions() throws Exception {

    ValueMetaInterface v1 = mock( ValueMetaInterface.class );
    when( v1.getName() ).thenReturn( "f1" );

    ValueMetaInterface v2 = mock( ValueMetaInterface.class );
    when( v2.getName() ).thenReturn( "f2" );

    List<ValueMetaInterface> valueMetaInterfaceList = new ArrayList<ValueMetaInterface>();
    valueMetaInterfaceList.add( v1 );
    valueMetaInterfaceList.add( v2 );

    RowMetaInterface prevStepFields = mock( RowMetaInterface.class );
    when( prevStepFields.getValueMetaList() ).thenReturn( valueMetaInterfaceList );

    TransMeta transMeta = mock( TransMeta.class );
    when( transMeta.getPrevStepFields( "myStep" ) ).thenReturn( prevStepFields );

    CreateAttribute ca = new CreateAttribute();
    ca.setField( "selectedField" );
    ModelAnnotation<CreateAttribute> modelAnnotation =
        new ModelAnnotation<CreateAttribute>( ca );

    String[] options = optionsResolver.resolveOrdinalFieldOptions( transMeta, "myStep", modelAnnotation );

    assertNotNull( options );
    assertTrue( options.length == 2 );

    options = optionsResolver.resolveOrdinalFieldOptions( transMeta, "myStepError", modelAnnotation ); // error
    assertTrue( options.length == 0 );
  }

  @Test
  public void testResolveSharedDimensions() throws Exception {
    OptionsResolver optionsResolver = new OptionsResolver();
    ModelAnnotationManager manager = mock( ModelAnnotationManager.class );
    IMetaStore metaStore = mock( IMetaStore.class );
    ModelAnnotationGroup group1 = new ModelAnnotationGroup();
    group1.setName( "group1" );
    group1.setSharedDimension( true );
    ModelAnnotationGroup group2 = new ModelAnnotationGroup();
    group2.setName( "group2" );
    group2.setSharedDimension( true );
    ModelAnnotationGroup group3 = new ModelAnnotationGroup();
    group3.setName( "group3" );
    ModelAnnotationGroup group4 = new ModelAnnotationGroup();
    group4.setName( "group4" );
    when( manager.listGroups( metaStore ) ).thenReturn( Arrays.asList( group1, group2, group3, group4 ) );
    String[] values = optionsResolver.resolveSharedDimensions( manager, metaStore );
    assertEquals( 2, values.length );
    assertEquals( "group1", values[0] );
    assertEquals( "group2", values[1] );
  }
}
