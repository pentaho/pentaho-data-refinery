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
package org.pentaho.di.core.refinery.model;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.job.entry.JobEntryBase;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pentaho.di.core.row.ValueMetaInterface.*;
import static org.pentaho.di.i18n.BaseMessages.getString;

public class AnalysisModelerTest {

  private ProvidesDatabaseConnectionInformation connectionInfo;

  @Before
  public void setUp() throws Exception {
    connectionInfo = mock( ProvidesDatabaseConnectionInformation.class );
    when( connectionInfo.getTableName() ).thenReturn( "newTable" );
  }

  @Test
  public void testUpdateAnalysisModel() throws Exception {
    String schema = IOUtils.toString( getClass().getResourceAsStream( "resources/testAnalysisSchema.xml" ) );
    String expectedSchema = IOUtils.toString( getClass().getResourceAsStream( "resources/newTableAnalysisSchema.xml" ) );
    String newSchema = getAnalysisModeler().replaceTableAndSchemaNames( schema, "newModel" );
    assertEquals( expectedSchema.replaceAll( "\r", "" ), newSchema.replaceAll( "\r", "" ) );
  }

  @Test
  public void testSchemaWithMultipleTableNamesIsInvalid() throws Exception {
    String schema = IOUtils.toString( getClass().getResourceAsStream( "resources/multiTableAnalysisSchema.xml" ) );
    try {
      getAnalysisModeler().replaceTableAndSchemaNames( schema, "newModel" );
      Assert.fail( "should have thrown exception" );
    } catch ( KettleException e ) {
      assertEquals(
        getString( JobEntryBuildModel.class, "AnalysisModeler.SelectModelErrorMultipleTables" ),
        e.getMessage().trim() );
    }
  }

  @Test
  public void testSchemaWithColumnNotFoundIsInvalid() throws Exception {
    String schema = IOUtils.toString( getClass().getResourceAsStream( "resources/extraColumnAnalysisSchema.xml" ) );
    try {
      getAnalysisModeler().replaceTableAndSchemaNames( schema, "newModel" );
      Assert.fail( "should have thrown exception" );
    } catch ( KettleException e ) {
      assertEquals(
        getString( JobEntryBuildModel.class, "AnalysisModeler.ColumnValidation",
          getString(
            JobEntryBuildModel.class, "AnalysisModeler.SelectModelColumnNotFound",
            Arrays.asList( "extraLevel", "extraMeasure" ) ), "" ).trim(),
        e.getMessage().trim() );
    }
  }


  @Test
  public void testSchemaWithColumnTypeMismatchIsInvalid() throws Exception {
    String schema = IOUtils.toString( getClass().getResourceAsStream( "resources/typeMismatchAnalysisSchema.xml" ) );
    try {
      getAnalysisModeler().replaceTableAndSchemaNames( schema, "newModel" );
      Assert.fail( "should have thrown exception" );
    } catch ( KettleException e ) {
      assertEquals(
        getString( JobEntryBuildModel.class, "AnalysisModeler.ColumnValidation",
          "",
          getString(
            JobEntryBuildModel.class, "AnalysisModeler.SelectModelColumnTypeMismatch",
            Arrays.asList( "POSTALCODE" ) ), "" ).trim(),
        e.getMessage().trim() );
    }
  }

  @Test
  public void testSchemaWithMultipleCubesIsInvalid() throws Exception {
    String schema = IOUtils.toString( getClass().getResourceAsStream( "resources/multiCubeAnalysisSchema.xml" ) );
    try {
      getAnalysisModeler().replaceTableAndSchemaNames( schema, "newModel" );
      Assert.fail( "should have thrown exception" );
    } catch ( KettleException e ) {
      assertEquals(
        getString( JobEntryBuildModel.class, "AnalysisModeler.SelectModelErrorMultipleCubes" ),
        e.getMessage().trim() );
    }
  }

  private AnalysisModeler getAnalysisModeler() {
    return new AnalysisModeler( new JobEntryBase( ), connectionInfo ) {
      @Override List<ValueMetaInterface> getFieldsInDatabase()
        throws KettleDatabaseException {
        return Arrays.asList(
          valueMeta( "TERRITORY", TYPE_STRING ),
          valueMeta( "COUNTRY", TYPE_STRING ),
          valueMeta( "CITY", TYPE_STRING ),
          valueMeta( "STATE", TYPE_STRING ),
          valueMeta( "POSTALCODE", TYPE_STRING ),
          valueMeta( "ORDERDATE", TYPE_STRING ),
          valueMeta( "ORDERNUMBER", TYPE_NUMBER ),
          valueMeta( "QUANTITYORDERED", TYPE_INTEGER ),
          valueMeta( "SALES", TYPE_INTEGER ) );
      }
    };
  }

  private ValueMetaInterface valueMeta( final String name, final int type ) {
    return new ValueMetaBase( name, type );
  }
}
