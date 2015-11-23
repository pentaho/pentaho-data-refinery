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

package org.pentaho.di.ui.trans.steps.annotation;

import org.apache.commons.lang.StringUtils;
import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.agilebi.modeler.nodes.TimeRole;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metastore.api.IMetaStore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Rowell Belen
 */
public class OptionsResolver {

  static final Logger logger = Logger.getLogger( OptionsResolver.class.getName() );

  public String[] resolveTimeSourceFormatOptions( String timeLevelType ) {
    try {
      // not a complete list. will add more options as necessary
      ModelAnnotation.TimeType timeType = ModelAnnotation.TimeType.valueOf( timeLevelType );
      if ( timeType != null ) {

        if ( ModelAnnotation.TimeType.TimeYears.equals( timeType ) ) {
          return TimeRole.YEARS.formats;
        }

        if ( ModelAnnotation.TimeType.TimeHalfYears.equals( timeType ) ) {
          return TimeRole.HALFYEARS.formats;
        }

        if ( ModelAnnotation.TimeType.TimeQuarters.equals( timeType ) ) {
          return TimeRole.QUARTERS.formats;
        }

        if ( ModelAnnotation.TimeType.TimeMonths.equals( timeType ) ) {
          return TimeRole.MONTHS.formats;
        }

        if ( ModelAnnotation.TimeType.TimeWeeks.equals( timeType ) ) {
          return TimeRole.WEEKS.formats;
        }

        if ( ModelAnnotation.TimeType.TimeDays.equals( timeType ) ) {
          return TimeRole.DAYS.formats;
        }

        if ( ModelAnnotation.TimeType.TimeHours.equals( timeType ) ) {
          return TimeRole.HOURS.formats;
        }

        if ( ModelAnnotation.TimeType.TimeMinutes.equals( timeType ) ) {
          return TimeRole.MINUTES.formats;
        }

        if ( ModelAnnotation.TimeType.TimeSeconds.equals( timeType ) ) {
          return TimeRole.SECONDS.formats;
        }
      }
    } catch ( Exception e ) {
      // do nothing
    }

    return new String[] { };
  }

  public String[] resolveMeasureFormatOptions() {

    List<String> options = new ArrayList<String>();
    options.add( "#,###;(#,###)" );
    options.add( "#,###.00;(#,###.00)" );
    options.add( "$#,###;($#,###)" );
    options.add( "$#,###.00;($#,###.00)" );
    options.add( "#.#%;(#.#%)" );

    return options.toArray( new String[options.size()] );
  }

  public String[] resolveAggregationTypeOptions() {

    AggregationType[] types = AggregationType.values();
    List<String> names = new ArrayList<String>();
    for ( int i = 0; i < types.length; ++i ) {
      if ( !types[i].equals( AggregationType.NONE ) ) {
        names.add( types[i].name() );
      }
    }
    return names.toArray( new String[names.size()] );
  }

  public String[] resolveAggregationTypeOptions( ValueMetaInterface valueMetaInterface,
      AnnotationType annotationType ) {

    if ( valueMetaInterface != null && annotationType != null ) {
      if ( !valueMetaInterface.isNumeric() && annotationType.getType()
          .equals( ModelAnnotation.Type.CREATE_MEASURE ) ) {
        return new String[] { AggregationType.COUNT.toString(), AggregationType.COUNT_DISTINCT.toString() };
      }
    }

    return resolveAggregationTypeOptions(); // default
  }

  public String[] resolveBooleanOptions() {
    return new String[] { "True", "False" };
  }

  public String[] resolveOrdinalFieldOptions( final TransMeta transMeta, final String stepName,
      ModelAnnotation modelAnnotation ) {
    LinkedHashSet<String> names = new LinkedHashSet<String>();
    try {
      RowMetaInterface prevStepFields = transMeta.getPrevStepFields( stepName );
      for ( ValueMetaInterface valueMetaInterface : prevStepFields.getValueMetaList() ) {
        if ( !StringUtils.equals( modelAnnotation.getAnnotation().getField(), valueMetaInterface.getName() ) ) {
          names.add( valueMetaInterface.getName() );
        }
      }
    } catch ( Exception e ) {
      logger.warning( e.getMessage() );
    }
    return names.toArray( new String[names.size()] );
  }

  public String[] resolveSharedDimensions( final ModelAnnotationManager manager, final IMetaStore metaStore ) {
    try {
      ArrayList<String> values = new ArrayList<String>();
      List<ModelAnnotationGroup> groups = manager.listGroups( metaStore );
      for ( ModelAnnotationGroup group : groups ) {
        if ( group.isSharedDimension() ) {
          values.add( group.getName() );
        }
      }
      return values.toArray( new String[values.size()] );
    } catch ( Exception e ) {
      logger.warning( e.getMessage() );
      return new String[0];
    }
  }
}
