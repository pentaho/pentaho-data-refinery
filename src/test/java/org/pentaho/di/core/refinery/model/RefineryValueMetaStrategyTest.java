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
