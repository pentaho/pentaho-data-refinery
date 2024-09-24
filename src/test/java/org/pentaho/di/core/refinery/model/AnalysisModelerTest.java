/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
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
