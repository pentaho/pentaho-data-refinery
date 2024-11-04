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


package org.pentaho.di.core.refinery.test;

import java.util.Arrays;

import org.pentaho.di.core.QueueRowSet;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.RowProducer;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.dummytrans.DummyTrans;
import org.pentaho.di.trans.steps.dummytrans.DummyTransMeta;
import org.pentaho.metastore.api.IMetaStore;

public class TransTestUtil {

  public static RowMetaInterface getRowMeta( ValueMetaInterface ... valueMetas ) {
    RowMeta rowMeta = new RowMeta();
    rowMeta.setValueMetaList( Arrays.asList( valueMetas ) );
    return rowMeta;
  }

  public static DatabaseMeta newH2Db( String name, String db ) {
    DatabaseMeta dbMeta = new DatabaseMeta( name, "H2", "Native", null, db, null, "user", null );
    return dbMeta;
  }

  public static RowSet getRowSet( RowMetaInterface rowMeta, Object[] ... rows ) {
    RowSet rowSet = new QueueRowSet();
    rowSet.setRowMeta( rowMeta );
    for ( Object[] row : rows ) {
      rowSet.putRow( rowMeta, row );
    }
    return rowSet;
  }

  public static Database createTableH2( DatabaseMeta dbMeta, String tableName, RowMetaInterface rowMeta ) throws Exception {
    Database db = new Database( null, dbMeta );
    db.connect();
    db.execStatement( "DROP TABLE IF EXISTS " + dbMeta.quoteField( tableName )  + ";" );
    final String createTable = db.getCreateTableStatement( tableName, rowMeta, null, false, null, true );
    db.execStatement( createTable );
    return db;
  }

  public static Result runTransformation(
      TransMeta transMeta,
      StepMeta injectStep,
      RowMetaInterface rowMeta,
      Object[]... rows ) throws KettleException {
    Trans trans = new Trans( transMeta );
    trans.setMetaStore( transMeta.getMetaStore() );
    // creates combis
    trans.prepareExecution( null );
    if ( injectStep != null && rows != null ) {
      // insert rows
      RowProducer rowProd = trans.addRowProducer( injectStep.getName(), 0 );
      for ( Object[] row : rows ) {
        rowProd.putRow( rowMeta, row );
      }
      rowProd.finished();
    }
    // go
    trans.startThreads();
    trans.waitUntilFinished();
    return trans.getResult();
  }

  public static JobEntryTrans getTransAsJobEntry(
      TransMeta transMeta,
      StepMeta injectStep,
      RowMetaInterface rowMeta,
      Object[]... rows ) throws KettleException {

    if ( injectStep != null && rows != null ) {
      // In step
      RowSet rowSet = getRowSet( rowMeta, rows );
      DummyInjectorMeta inputMeta = new DummyInjectorMeta( rowSet );
      StepMeta inStepMeta = new StepMeta( "IN", inputMeta );
      transMeta.addOrReplaceStep( inStepMeta );
      transMeta.addTransHop( new TransHopMeta( inStepMeta, injectStep ) );
    }
    JobEntryTrans jeTrans = new JobEntryTestTrans( transMeta );
    jeTrans.setName( "trans" );
    jeTrans.setFileName( "trans.ktr" );
    return jeTrans;
  }

  static class JobEntryTestTrans extends JobEntryTrans {
    private final TransMeta transMeta;
    public JobEntryTestTrans( TransMeta transMeta ) {
      this.transMeta = transMeta;
    }
    @Override
    public TransMeta getTransMeta( Repository rep, IMetaStore metaStore, VariableSpace space ) throws KettleException {
      transMeta.copyVariablesFrom( this );
      return transMeta;
    }
  }

  static class DummyInjector extends DummyTrans {

    private RowSet rowSet;

    public DummyInjector(
        StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
        Trans trans, RowSet rowSet ) {
      super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
      this.rowSet = rowSet;
    }

    @Override
    public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
      for ( Object[] row = rowSet.getRow(); row != null; row = rowSet.getRow() ) {
        putRow( rowSet.getRowMeta(), row );
      }
      setOutputDone();
      return false;
    }
  }
  static class DummyInjectorMeta extends DummyTransMeta {
    private RowSet rowSet;
    public DummyInjectorMeta( RowSet rowSet ) {
      this.rowSet = rowSet;
    }
    @Override
    public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta tr,
        Trans trans ) {
      return new DummyInjector( stepMeta, stepDataInterface, cnr, tr, trans, rowSet );
    }
  }
}
