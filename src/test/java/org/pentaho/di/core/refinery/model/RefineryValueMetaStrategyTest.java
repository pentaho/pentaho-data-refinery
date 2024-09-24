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

package org.pentaho.di.core.refinery.model;

import org.junit.Test;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.di.trans.steps.tableoutput.TableOutputData;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;

import static org.junit.Assert.*;

public class RefineryValueMetaStrategyTest {
  @Test
  public void testUsesFieldStreamForDisplayAndIgnoresOtherFields() throws Exception {
    StepMetaDataCombi combi = new StepMetaDataCombi();
    TableOutputMeta tableOutputMeta = new TableOutputMeta();
    tableOutputMeta.setSpecifyFields( true );
    tableOutputMeta.setFieldDatabase( new String[] {"fieldOne", "fieldTwo", "fieldThree"} );
    tableOutputMeta.setFieldStream( new String[] { "One", "Two", "Three"} );
    combi.meta = tableOutputMeta;
    TableOutputData tableOutputData = new TableOutputData();
    tableOutputData.insertRowMeta = new RowMeta();
    ValueMetaInteger fieldOne = new ValueMetaInteger( "fieldOne" );
    tableOutputData.insertRowMeta.addValueMeta( fieldOne );
    ValueMetaString fieldTwo = new ValueMetaString( "fieldTwo" );
    tableOutputData.insertRowMeta.addValueMeta( fieldTwo );
    ValueMetaNumber fieldThree = new ValueMetaNumber( "fieldThree" );
    tableOutputData.insertRowMeta.addValueMeta( fieldThree );
    combi.data = tableOutputData;

    RefineryValueMetaStrategy strategy = new RefineryValueMetaStrategy( combi );
    assertTrue( strategy.shouldInclude( fieldOne ) );
    assertTrue( strategy.shouldInclude( fieldTwo ) );
    assertTrue( strategy.shouldInclude( fieldThree ) );
    assertFalse( strategy.shouldInclude( new ValueMetaString( "Other" ) ) );
    assertEquals( "One", strategy.displayName( fieldOne ) );
    assertEquals( "Two", strategy.displayName( fieldTwo ) );
    assertEquals( "Three", strategy.displayName( fieldThree ) );
  }
}
