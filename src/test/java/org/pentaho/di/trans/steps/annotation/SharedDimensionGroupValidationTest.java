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

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.agilebi.modeler.models.annotations.BlankAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateDimensionKey;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.i18n.LanguageChoice;

public class SharedDimensionGroupValidationTest {

  @Test
  public void testBasicOk() throws Exception {
    final String dimension = "dim";

    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setSharedDimension( true );

    CreateDimensionKey createDimKey = new CreateDimensionKey();
    createDimKey.setName( "a1" );
    createDimKey.setDimension( dimension );
    createDimKey.setField( "a1" );
    group.add( new ModelAnnotation<CreateDimensionKey>( createDimKey ) );

    CreateAttribute createAttrib = new CreateAttribute();
    createAttrib.setName( "a2" );
    createAttrib.setDimension( dimension );
    createAttrib.setField( "a2" );
    group.add( new ModelAnnotation<CreateAttribute>( createAttrib ) );

    CreateAttribute createAnotherAttrib = new CreateAttribute();
    createAnotherAttrib.setName( "a3" );
    createAnotherAttrib.setDimension( dimension );
    createAnotherAttrib.setField( "a3" );
    group.add( new ModelAnnotation<CreateAttribute>( createAnotherAttrib ) );

    SharedDimensionGroupValidation validation = new SharedDimensionGroupValidation( group, getLog() );
    assertFalse( validation.hasErrors() );
    assertEquals( 0, validation.getErrorSummary().size() );
  }

  @Test
  public void testDimensionMismatch() throws Exception {
    final String dimension = "dim";

    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setSharedDimension( true );

    CreateDimensionKey createDimKey = new CreateDimensionKey();
    createDimKey.setName( "a1" );
    createDimKey.setDimension( dimension );
    createDimKey.setField( "a1" );
    group.add( new ModelAnnotation<CreateDimensionKey>( createDimKey ) );

    CreateAttribute createAttrib = new CreateAttribute();
    createAttrib.setName( "a2" );
    createAttrib.setDimension( "another dimension" );
    createAttrib.setField( "a2" );
    group.add( new ModelAnnotation<CreateAttribute>( createAttrib ) );

    CreateAttribute createAnotherAttrib = new CreateAttribute();
    createAnotherAttrib.setName( "a3" );
    createAnotherAttrib.setDimension( dimension );
    createAnotherAttrib.setField( "a3" );
    group.add( new ModelAnnotation<CreateAttribute>( createAnotherAttrib ) );

    LanguageChoice.getInstance().setDefaultLocale( Locale.US );
    SharedDimensionGroupValidation validation = new SharedDimensionGroupValidation( group, getLog() );
    assertTrue( validation.hasErrors() );
    assertEquals( 1, validation.getErrorSummary().size() );
    assertTrue( validation.getErrorSummary().contains( "All actions must refer to the same dimension." ) );
  }

  @Test
  public void testTwoKeys() throws Exception {
    final String dimension = "dim";

    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setSharedDimension( true );

    CreateDimensionKey createDimKey = new CreateDimensionKey();
    createDimKey.setName( "a1" );
    createDimKey.setDimension( dimension );
    createDimKey.setField( "a1" );
    group.add( new ModelAnnotation<CreateDimensionKey>( createDimKey ) );

    CreateAttribute createAttrib = new CreateAttribute();
    createAttrib.setName( "a2" );
    createAttrib.setDimension( dimension );
    createAttrib.setField( "a2" );
    group.add( new ModelAnnotation<CreateAttribute>( createAttrib ) );

    CreateDimensionKey createAnotherDimKey = new CreateDimensionKey();
    createAnotherDimKey.setName( "a3" );
    createAnotherDimKey.setDimension( dimension );
    createAnotherDimKey.setField( "a3" );
    group.add( new ModelAnnotation<CreateDimensionKey>( createAnotherDimKey ) );

    LanguageChoice.getInstance().setDefaultLocale( Locale.US );
    SharedDimensionGroupValidation validation = new SharedDimensionGroupValidation( group, getLog() );
    assertTrue( validation.hasErrors() );
    assertEquals( 1, validation.getErrorSummary().size() );
    assertTrue( validation.getErrorSummary().contains( "Only one Create Dimension Key action is allowed." ) );
  }

  @Test
  public void testWithMeasure() throws Exception {
    final String dimension = "dim";

    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setSharedDimension( true );

    CreateDimensionKey createDimKey = new CreateDimensionKey();
    createDimKey.setName( "a1" );
    createDimKey.setDimension( dimension );
    createDimKey.setField( "a1" );
    group.add( new ModelAnnotation<CreateDimensionKey>( createDimKey ) );

    CreateAttribute createAttrib = new CreateAttribute();
    createAttrib.setName( "a2" );
    createAttrib.setDimension( dimension );
    createAttrib.setField( "a2" );
    group.add( new ModelAnnotation<CreateAttribute>( createAttrib ) );

    CreateMeasure measure = new CreateMeasure();
    measure.setName( "a3" );
    measure.setField( "a3" );
    group.add( new ModelAnnotation<CreateMeasure>( measure ) );

    LanguageChoice.getInstance().setDefaultLocale( Locale.US );
    SharedDimensionGroupValidation validation = new SharedDimensionGroupValidation( group, getLog() );
    assertTrue( validation.hasErrors() );
    assertEquals( 1, validation.getErrorSummary().size() );
    assertTrue( validation.getErrorSummary().contains(
        "Only Create Attribute and Create Dimension Key actions are allowed." ) );
  }

  @Test
  public void testNotShared() throws Exception {
    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setSharedDimension( false );

    CreateAttribute attr1 = new CreateAttribute();
    attr1.setName( "a1" );
    attr1.setDimension( "d1" );
    attr1.setField( "a1" );
    group.add( new ModelAnnotation<CreateAttribute>( attr1 ) );

    CreateMeasure measure = new CreateMeasure();
    measure.setName( "a2" );
    measure.setField( "a2" );
    group.add( new ModelAnnotation<CreateMeasure>( measure ) );

    SharedDimensionGroupValidation validation = new SharedDimensionGroupValidation( group, getLog() );
    assertFalse( validation.hasErrors() );
    assertEquals( 0, validation.getErrorSummary().size() );
  }

  @Test
  public void testMeasureAndDimensionMismatch() throws Exception {
    final String dimension = "dim";

    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setSharedDimension( true );

    CreateDimensionKey createDimKey = new CreateDimensionKey();
    createDimKey.setName( "a1" );
    createDimKey.setDimension( dimension );
    createDimKey.setField( "a1" );
    group.add( new ModelAnnotation<CreateDimensionKey>( createDimKey ) );

    CreateAttribute createAttrib = new CreateAttribute();
    createAttrib.setName( "a2" );
    createAttrib.setDimension( dimension );
    createAttrib.setField( "a2" );
    group.add( new ModelAnnotation<CreateAttribute>( createAttrib ) );

    CreateMeasure measure = new CreateMeasure();
    measure.setName( "a3" );
    measure.setField( "a3" );
    group.add( new ModelAnnotation<CreateMeasure>( measure ) );

    CreateAttribute createAnotherAttrib = new CreateAttribute();
    createAnotherAttrib.setName( "a4" );
    createAnotherAttrib.setDimension( "another dimension" );
    createAnotherAttrib.setField( "a4" );
    group.add( new ModelAnnotation<CreateAttribute>( createAnotherAttrib ) );


    CreateAttribute createYetAnotherAttrib = new CreateAttribute();
    createYetAnotherAttrib.setName( "a5" );
    createYetAnotherAttrib.setDimension( "yet another dimension" );
    createYetAnotherAttrib.setField( "a5" );
    group.add( new ModelAnnotation<CreateAttribute>( createYetAnotherAttrib ) );

    SharedDimensionGroupValidation validation = new SharedDimensionGroupValidation( group, getLog() );
    assertTrue( validation.hasErrors() );
    assertEquals( 2, validation.getErrorSummary().size() );
    assertTrue( validation.getErrorSummary().contains(
        "Only Create Attribute and Create Dimension Key actions are allowed." ) );
    assertTrue( validation.getErrorSummary().contains( "All actions must refer to the same dimension." ) );
  }

  @Test
  public void testVariablesNotRequiredToHaveDimensionKey() throws Exception {
    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setSharedDimension( true );
    group.setName( "${someVariable}" );

    SharedDimensionGroupValidation validation = new SharedDimensionGroupValidation( group, getLog() );
    assertFalse( validation.hasErrors() );
  }

  @Test
  public void testFieldWithEmptyAnnotationIsAllowed() throws Exception {
    BlankAnnotation blankAnnotation = new BlankAnnotation();
    blankAnnotation.setField( "anyField" );
    ModelAnnotationGroup group = new ModelAnnotationGroup(
      new ModelAnnotation<>( blankAnnotation ), new ModelAnnotation<>( new CreateDimensionKey() ) );
    group.setSharedDimension( true );
    SharedDimensionGroupValidation validation = new SharedDimensionGroupValidation( group, getLog() );
    assertFalse( validation.hasErrors() );
  }

  private LogChannelInterface getLog() {
    return Mockito.mock( LogChannelInterface.class );
  }
}
