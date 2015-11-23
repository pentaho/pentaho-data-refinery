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

package org.pentaho.di.core.refinery;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.agilebi.modeler.models.annotations.data.ColumnMapping;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.di.trans.steps.databaselookup.DatabaseLookupData;
import org.pentaho.di.trans.steps.databaselookup.DatabaseLookupMeta;
import org.pentaho.di.trans.steps.tableoutput.TableOutput;
import org.pentaho.di.trans.steps.tableoutput.TableOutputData;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;
import org.pentaho.metastore.api.IMetaStore;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class DataProviderHelperTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    KettleClientEnvironment.init();
  }

  @Test
  public void testUpdateDataProviderTableOut() throws Exception {
    IMetaStore metaStore = mock( IMetaStore.class );
    final DatabaseMeta dbMeta =
        new DatabaseMeta( "dbmetaTest", "postgresql", "Native", "${varhost}", "${db}", "3001", "user", "pass" );
    dbMeta.setVariable( "varhost", "somehost" );
    dbMeta.setVariable( "db", "db" );

    final RowMetaInterface rowMetaDb = getRowMeta(
        new ValueMetaInteger( "id" ),
        new ValueMetaString( "field1" ),
        new ValueMetaNumber( "field2" )
    );

    TableOutputMeta tableOutMeta = new TableOutputMeta();
    // Out step
    tableOutMeta.setDefault();
    tableOutMeta.setDatabaseMeta( dbMeta );
    tableOutMeta.setTableName( "store" );
    tableOutMeta.setSpecifyFields( true );
    tableOutMeta.setFieldStream( new String[] { "The ID Field", "Another Field", "Yet another field" } );
    tableOutMeta.setFieldDatabase( new String[] { "id", "field1", "field2" } );
    // populated combi
    StepMetaDataCombi combi = new StepMetaDataCombi();
    combi.stepname = "out1";
    TableOutputData tableOutData = new TableOutputData();
    tableOutData.insertRowMeta = rowMetaDb;
    StepMeta tableOutStepMeta = new StepMeta( combi.stepname, tableOutMeta );
    TableOutput tableOutStep = mock( TableOutput.class );
    combi.meta = tableOutMeta;
    combi.stepMeta = tableOutStepMeta;
    combi.data = tableOutData;
    combi.step = tableOutStep;

    final ModelAnnotationManager manager = mock( ModelAnnotationManager.class );
    when( manager.storeDatabaseMeta(
        argThat( matchDbMeta( "dbmetaTest", "somehost", "db" ) ), eq( metaStore ) ) ).thenReturn( "uniqueId" );
    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setName( "mag" );
    DataProviderHelper helper = new DataProviderHelper( metaStore ) {
      protected ModelAnnotationManager getModelAnnotationManager() {
        return manager;
      };
    };

    helper.updateDataProvider( group, combi );

    verify( manager, times( 1 ) ).updateGroup( any( ModelAnnotationGroup.class ), any( IMetaStore.class ) );
    assertEquals( 1, group.getDataProviders().size() );
    assertEquals( 3, group.getDataProviders().get( 0 ).getColumnMappings().size() );
    assertEquals( "uniqueId", group.getDataProviders().get( 0 ).getDatabaseMetaNameRef() );
    for ( ColumnMapping colMap : group.getDataProviders().get( 0 ).getColumnMappings() ) {
      if ( colMap.getColumnName().equals( "id" ) ) {
        assertEquals( "The ID Field", colMap.getName() );
        assertEquals( org.pentaho.metadata.model.concept.types.DataType.NUMERIC, colMap.getColumnDataType() );
      } else if ( colMap.getColumnName().equals( "field1" ) ) {
        assertEquals( "Another Field", colMap.getName() );
        assertEquals( org.pentaho.metadata.model.concept.types.DataType.STRING, colMap.getColumnDataType() );
      } else if ( colMap.getColumnName().equals( "field2" ) ) {
        assertEquals( "Yet another field", colMap.getName() );
        assertEquals( org.pentaho.metadata.model.concept.types.DataType.NUMERIC, colMap.getColumnDataType() );
      } else {
        fail( "no column match" );
      }
    }
  }

  @Test
  public void testUpdateDataProviderDatabaseLookup() throws Exception {
    IMetaStore metaStore = mock( IMetaStore.class );
    final DatabaseMeta dbMeta =
        new DatabaseMeta( "dbmetaTest", "postgresql", "Native", "${varhost}", "${db}", "3001", "user", "pass" );
    dbMeta.setVariable( "varhost", "somehost" );
    dbMeta.setVariable( "db", "db" );

    final RowMetaInterface rowMetaDb = getRowMeta(
        new ValueMetaInteger( "id" ),
        new ValueMetaString( "field1" ),
        new ValueMetaNumber( "field2" )
    );

    DatabaseLookupMeta databaseLookupMeta = new DatabaseLookupMeta();
    // Out step
    databaseLookupMeta.setDefault();
    databaseLookupMeta.setDatabaseMeta( dbMeta );
    databaseLookupMeta.setReturnValueNewName( new String[] { "The ID Field", "Another Field", "Yet another field" } );
    databaseLookupMeta.setReturnValueField( new String[] { "id", "field1", "field2" } );
    // populated combi
    StepMetaDataCombi combi = new StepMetaDataCombi();
    combi.stepname = "out1";
    DatabaseLookupData databaseLookupData = new DatabaseLookupData();
    databaseLookupData.returnMeta = rowMetaDb;
    StepMeta tableOutStepMeta = new StepMeta( combi.stepname, databaseLookupMeta );
    TableOutput tableOutStep = mock( TableOutput.class );
    combi.meta = databaseLookupMeta;
    combi.stepMeta = tableOutStepMeta;
    combi.data = databaseLookupData;
    combi.step = tableOutStep;

    final ModelAnnotationManager manager = mock( ModelAnnotationManager.class );
    when( manager.storeDatabaseMeta(
      argThat( matchDbMeta( "dbmetaTest", "somehost", "db" ) ), eq( metaStore ) ) ).thenReturn( "uniqueId" );
    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setName( "mag" );
    DataProviderHelper helper = new DataProviderHelper( metaStore ) {
      protected ModelAnnotationManager getModelAnnotationManager() {
        return manager;
      };
    };

    helper.updateDataProvider( group, combi );

    verify( manager, times( 1 ) ).updateGroup( any( ModelAnnotationGroup.class ), any( IMetaStore.class ) );
    assertEquals( 1, group.getDataProviders().size() );
    assertEquals( 3, group.getDataProviders().get( 0 ).getColumnMappings().size() );
    assertEquals( "uniqueId", group.getDataProviders().get( 0 ).getDatabaseMetaNameRef() );
    for ( ColumnMapping colMap : group.getDataProviders().get( 0 ).getColumnMappings() ) {
      if ( colMap.getColumnName().equals( "id" ) ) {
        assertEquals( "The ID Field", colMap.getName() );
        assertEquals( org.pentaho.metadata.model.concept.types.DataType.NUMERIC, colMap.getColumnDataType() );
      } else if ( colMap.getColumnName().equals( "field1" ) ) {
        assertEquals( "Another Field", colMap.getName() );
        assertEquals( org.pentaho.metadata.model.concept.types.DataType.STRING, colMap.getColumnDataType() );
      } else if ( colMap.getColumnName().equals( "field2" ) ) {
        assertEquals( "Yet another field", colMap.getName() );
        assertEquals( org.pentaho.metadata.model.concept.types.DataType.NUMERIC, colMap.getColumnDataType() );
      } else {
        fail( "no column match" );
      }
    }
  }
  private ArgumentMatcher<DatabaseMeta> matchDbMeta( final String name, final String hostName, final String dbName ) {
    return new ArgumentMatcher<DatabaseMeta>() {
      @Override public boolean matches( final Object argument ) {
        DatabaseMeta actual = (DatabaseMeta) argument;
        return actual.getName().equals( name )
            && actual.getHostname().equals( hostName )
            && actual.getDatabaseName().equals( dbName );
      }
    };
  }

  @Test
  public void testUpdateDataProviderTableOutNoSpecifyFields() throws Exception {
    IMetaStore metaStore = mock( IMetaStore.class );
    final DatabaseMeta dbMeta =
        new DatabaseMeta( "dbmetaTest", "postgresql", "Native", "somehost", "db", "3001", "user", "pass" );

    final RowMetaInterface rowMetaDb = getRowMeta(
        new ValueMetaInteger( "id" ),
        new ValueMetaString( "field1" ),
        new ValueMetaNumber( "field2" )
    );

    TableOutputMeta tableOutMeta = new TableOutputMeta();
    // Out step
    tableOutMeta.setDefault();
    tableOutMeta.setDatabaseMeta( dbMeta );
    tableOutMeta.setTableName( "store" );
    tableOutMeta.setSpecifyFields( false );
    tableOutMeta.setFieldStream( new String[] { "No No No", "Boom", "Kaboom" } );
    tableOutMeta.setFieldDatabase( new String[] { "id", "field1", "field2" } );
    // populated combi
    StepMetaDataCombi combi = new StepMetaDataCombi();
    combi.stepname = "out1";
    TableOutputData tableOutData = new TableOutputData();
    tableOutData.insertRowMeta = rowMetaDb;
    StepMeta tableOutStepMeta = new StepMeta( combi.stepname, tableOutMeta );
    TableOutput tableOutStep = mock( TableOutput.class );
    combi.meta = tableOutMeta;
    combi.stepMeta = tableOutStepMeta;
    combi.data = tableOutData;
    combi.step = tableOutStep;

    final ModelAnnotationManager manager = mock( ModelAnnotationManager.class );
    when( manager.storeDatabaseMeta( dbMeta, metaStore ) ).thenReturn( dbMeta.getName() );
    ModelAnnotationGroup group = new ModelAnnotationGroup();
    group.setName( "mag" );
    DataProviderHelper helper = new DataProviderHelper( metaStore ) {
      protected ModelAnnotationManager getModelAnnotationManager() {
        return manager;
      };
    };

    helper.updateDataProvider( group, combi );

    verify( manager, times( 1 ) ).updateGroup( any( ModelAnnotationGroup.class ), any( IMetaStore.class ) );
    assertEquals( 1, group.getDataProviders().size() );
    assertEquals( 3, group.getDataProviders().get( 0 ).getColumnMappings().size() );
    for ( ColumnMapping colMap : group.getDataProviders().get( 0 ).getColumnMappings() ) {
      if ( colMap.getColumnName().equals( "id" ) ) {
        assertEquals( "id", colMap.getName() );
        assertEquals( org.pentaho.metadata.model.concept.types.DataType.NUMERIC, colMap.getColumnDataType() );
      } else if ( colMap.getColumnName().equals( "field1" ) ) {
        assertEquals( "field1", colMap.getName() );
        assertEquals( org.pentaho.metadata.model.concept.types.DataType.STRING, colMap.getColumnDataType() );
      } else if ( colMap.getColumnName().equals( "field2" ) ) {
        assertEquals( "field2", colMap.getName() );
        assertEquals( org.pentaho.metadata.model.concept.types.DataType.NUMERIC, colMap.getColumnDataType() );
      } else {
        fail( "no column match" );
      }
    }
  }

  @Test
  public void testOutputSteppingMapper() throws Exception {
    StepMetaDataCombi stepMetaDataCombi = new StepMetaDataCombi();
    DatabaseLookupMeta databaseLookupMeta = mock( DatabaseLookupMeta.class );
    stepMetaDataCombi.data = mock( StepDataInterface.class );
    stepMetaDataCombi.meta = databaseLookupMeta;
    RowMeta rowMeta = mock( RowMeta.class );
    when( databaseLookupMeta.getRowMeta( stepMetaDataCombi.data ) ).thenReturn( rowMeta );
    List<String> databaseFields = Arrays.asList( "d1", "d2" );
    when( databaseLookupMeta.getDatabaseFields() ).thenReturn( databaseFields );
    List<String> streamFields = Arrays.asList( "st1", "st2" );
    when( databaseLookupMeta.getStreamFields() ).thenReturn( streamFields );
    DataProviderHelper.OutputStepMappingAdapter adapter =
        new DataProviderHelper.OutputStepMappingAdapter( stepMetaDataCombi );
    assertEquals( rowMeta, adapter.insertRowMeta );
    assertEquals( databaseFields, adapter.fieldDatabase );
    assertEquals( streamFields, adapter.fieldStream );
  }

  private RowMetaInterface getRowMeta( ValueMetaInterface ... valueMetas ) {
    RowMeta rowMeta = new RowMeta();
    rowMeta.setValueMetaList( Arrays.asList( valueMetas ) );
    return rowMeta;
  }
}
