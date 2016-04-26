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
import org.pentaho.agilebi.modeler.models.annotations.ModelProperty;
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
    assertEquals( 16, optionsResolver.resolveMeasureFormatOptions().length );
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
  public void testResolveAttributeFormat() throws Exception {
    OptionsResolver resolver = new OptionsResolver();
    String[] formatStrings = resolver.resolveAttributeFormatOptions( ModelProperty.AppliesTo.Time );
    assertEquals( 17, formatStrings.length );
    assertEquals( "m/d", formatStrings[0] );
    assertEquals( "[h]:mm:ss", formatStrings[16] );
    formatStrings = resolver.resolveAttributeFormatOptions( ModelProperty.AppliesTo.Numeric );
    assertEquals( 16, formatStrings.length );
    assertEquals( "0", formatStrings[0] );
    assertEquals( "##0.0E+0", formatStrings[15] );
    formatStrings = resolver.resolveAttributeFormatOptions( ModelProperty.AppliesTo.String );
    assertEquals( 0, formatStrings.length );
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
