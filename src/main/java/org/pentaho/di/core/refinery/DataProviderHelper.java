/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2015 Pentaho Corporation (Pentaho). All rights reserved.
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
package org.pentaho.di.core.refinery;

import org.apache.commons.lang.StringUtils;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.agilebi.modeler.models.annotations.data.ColumnMapping;
import org.pentaho.agilebi.modeler.models.annotations.data.DataProvider;
import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.ProvidesModelerMeta;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.metadata.model.concept.types.DataType;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.api.exceptions.MetaStoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataProviderHelper {

  private final IMetaStore mstore;
  private final ModelAnnotationManager annotationManager = new ModelAnnotationManager( true );

  public DataProviderHelper( IMetaStore mstore ) {
    this.mstore = mstore;
  }

  protected IMetaStore getMetaStore() {
    return mstore;
  }

  protected ModelAnnotationManager getModelAnnotationManager() {
    return annotationManager;
  }

  /**
   * @param annotations
   * @param outputCombi
   * @param mstore
   * @throws KettleException
   * @throws MetaStoreException
   */
  public void updateDataProvider( ModelAnnotationGroup annotations, StepMetaDataCombi outputCombi )
    throws KettleException, MetaStoreException {
    DataProvider provider = createDataProvider( outputCombi, mstore );
    updateOrAdd( annotations.getDataProviders(), provider );
    getModelAnnotationManager().updateGroup( annotations, mstore );
  }

  /**
   * Creates DataProvider and stores DatabaseMeta
   */
  private DataProvider createDataProvider( StepMetaDataCombi outputCombi, IMetaStore mstore )
    throws KettleException, MetaStoreException {
    DataProvider provider = new DataProvider();
    provider.setName( outputCombi.stepname );
    ProvidesDatabaseConnectionInformation connInfo = (ProvidesDatabaseConnectionInformation) outputCombi.meta;
    DatabaseMeta dbMeta = fillConnectionInfo( provider, connInfo, outputCombi.step );
    provider.setColumnMappings( getColumnMappings( outputCombi ) );
    provider.setDatabaseMetaNameRef( getModelAnnotationManager().storeDatabaseMeta( dbMeta, mstore ) );
    return provider;
  }

  private static DatabaseMeta fillConnectionInfo(
      DataProvider provider, ProvidesDatabaseConnectionInformation connInfo, VariableSpace varSpace ) {
    provider.setSchemaName( varSpace.environmentSubstitute( connInfo.getSchemaName() ) );
    provider.setTableName( varSpace.environmentSubstitute( connInfo.getTableName() ) );
    DatabaseMeta dbMeta = (DatabaseMeta) connInfo.getDatabaseMeta().clone();
    dbMeta.setHostname( connInfo.getDatabaseMeta().environmentSubstitute( dbMeta.getHostname() ) );
    dbMeta.setDBName( connInfo.getDatabaseMeta().environmentSubstitute( dbMeta.getDatabaseName() ) );
    return dbMeta;
  }

  private static List<ColumnMapping> getColumnMappings( StepMetaDataCombi outputStepMetaDataCombi )
    throws KettleException {
    try {
      OutputStepMappingAdapter adapter = new OutputStepMappingAdapter( outputStepMetaDataCombi );

      // db->stream as defined in meta (if def)
      Map<String, String> metaFieldMap = new HashMap<String, String>( adapter.fieldDatabase.size() );
      for ( int i = 0; i < adapter.fieldDatabase.size(); i++ ) {
        metaFieldMap.put( adapter.fieldDatabase.get( i ), adapter.fieldStream.get( i ) );
      }

      List<ColumnMapping> columnMappings = new ArrayList<ColumnMapping>();
      if ( adapter.insertRowMeta != null ) {
        // mapping for every field in stream
        for ( ValueMetaInterface valueMeta : adapter.insertRowMeta.getValueMetaList() ) {
          ColumnMapping colMap = new ColumnMapping();
          colMap.setColumnName( valueMeta.getName() );
          if ( metaFieldMap.containsKey( valueMeta.getName() ) ) {
            colMap.setName( metaFieldMap.get( colMap.getColumnName() ) );
          } else {
            colMap.setName( valueMeta.getName() );
          }
          colMap.setColumnDataType( getDataType( valueMeta ) );
          columnMappings.add( colMap );
        }
      }
      return columnMappings;
    } catch ( Exception e ) {
      throw new KettleException( e );
    }
  }

  private static boolean equalsNoColumnMappings( DataProvider one, DataProvider other ) {
    return StringUtils.equals( one.getDatabaseMetaNameRef(), other.getDatabaseMetaNameRef() )
        && StringUtils.equals( one.getSchemaName(), other.getSchemaName() )
        && StringUtils.equals( one.getTableName(), other.getTableName() );
  }

  /**
   * updates column mappings or adds new provider
   */
  private DataProvider updateOrAdd( List<DataProvider> annotationProviders, DataProvider newProvider ) {
    for ( DataProvider dataProvider : annotationProviders ) {
      if ( equalsNoColumnMappings( dataProvider, newProvider ) ) {
        dataProvider.setColumnMappings( newProvider.getColumnMappings() );
        return dataProvider;
      }
    }
    annotationProviders.add( newProvider );
    return newProvider;
  }

  public static DataType getDataType( ValueMetaInterface valueMeta ) {
    switch ( valueMeta.getType() ) {
      case ValueMetaInterface.TYPE_BIGNUMBER:
      case ValueMetaInterface.TYPE_INTEGER:
      case ValueMetaInterface.TYPE_NUMBER:
        return DataType.NUMERIC;
      case ValueMetaInterface.TYPE_BINARY:
        return DataType.BINARY;
      case ValueMetaInterface.TYPE_BOOLEAN:
        return DataType.BOOLEAN;
      case ValueMetaInterface.TYPE_DATE:
        return DataType.DATE;
      case ValueMetaInterface.TYPE_STRING:
        return DataType.STRING;
      case ValueMetaInterface.TYPE_NONE:
      default:
        return DataType.UNKNOWN;
    }
  }

  public static class OutputStepMappingAdapter {
    protected final List<String> fieldDatabase;
    protected final List<String> fieldStream;
    protected final RowMeta insertRowMeta;

    public OutputStepMappingAdapter( final StepMetaDataCombi stepMetaDataCombi ) throws ModelerException {
      if ( stepMetaDataCombi.meta instanceof ProvidesModelerMeta ) {
        ProvidesModelerMeta modelerMeta = (ProvidesModelerMeta) stepMetaDataCombi.meta;
        insertRowMeta = modelerMeta.getRowMeta( stepMetaDataCombi.data );
        fieldDatabase = modelerMeta.getDatabaseFields();
        fieldStream = modelerMeta.getStreamFields();
      } else {
        throw new ModelerException( "Step being Annotated is unsupported" );
      }
    }
  }
}
