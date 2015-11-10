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
