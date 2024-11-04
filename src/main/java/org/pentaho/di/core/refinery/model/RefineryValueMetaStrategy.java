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

import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.di.core.refinery.DataProviderHelper;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.metadata.automodel.PhysicalTableImporter;

public class RefineryValueMetaStrategy extends DataProviderHelper.OutputStepMappingAdapter
  implements PhysicalTableImporter.ImportStrategy {

  public RefineryValueMetaStrategy( final StepMetaDataCombi stepMetaDataCombi ) throws ModelerException {
    super( stepMetaDataCombi );
  }

  @Override
  public boolean shouldInclude( final ValueMetaInterface valueMeta ) {
    return insertRowMeta != null && insertRowMeta.exists( valueMeta );
  }

  @Override
  public String displayName( final ValueMetaInterface valueMeta ) {
    for ( int i = 0; i < fieldDatabase.size(); i++ ) {
      final String fieldName = fieldDatabase.get( i );
      if ( fieldName.equalsIgnoreCase( valueMeta.getName() ) ) {
        return fieldStream.get( i );
      }
    }
    ValueMetaInterface insertValueMeta = insertRowMeta.searchValueMeta( valueMeta.getName() );
    if ( insertValueMeta != null ) {
      return insertValueMeta.getName();
    }
    return valueMeta.getName();
  }
}
