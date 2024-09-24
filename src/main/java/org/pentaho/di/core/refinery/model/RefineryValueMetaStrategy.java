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
