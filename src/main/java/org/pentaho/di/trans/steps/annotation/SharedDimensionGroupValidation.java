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
package org.pentaho.di.trans.steps.annotation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.i18n.BaseMessages;

public class SharedDimensionGroupValidation {

  private static Class<?> PKG = ModelAnnotationMeta.class;

  private Set<String> msgs = new LinkedHashSet<String>();
  private LogChannelInterface log;

  public SharedDimensionGroupValidation( ModelAnnotationGroup targetGroup, LogChannelInterface log ) {
    this.log = log;
    try {
      validateSharedDimension( targetGroup );
    } catch ( Exception e ) {
      log.logError( e.getLocalizedMessage(), e );
    }
  }

  public boolean hasErrors() {
    return !msgs.isEmpty();
  }

  /**
   * 
   * @return
   */
  public Collection<String> getErrorSummary() {
    return msgs;
  }

  private void validateSharedDimension( ModelAnnotationGroup annotations ) {
    if ( !annotations.isSharedDimension() || StringUtil.isVariable( annotations.getName() ) ) {
      return;
    }
    int keyCount = 0;
    String dimension = null;
    for ( ModelAnnotation<?> annotation : annotations ) {
      if ( annotation.getType() == null ) {
        addError( annotation,
            BaseMessages.getString( PKG, "ModelAnnotation.SharedDimension.ValidationError.WrongAnnotationType" ) );
        continue;
      }
      switch ( annotation.getType() ) {
        case CREATE_DIMENSION_KEY:
          if ( ++keyCount > 1 ) {
            addError( annotation,
                BaseMessages.getString( PKG, "ModelAnnotation.SharedDimension.ValidationError.OneKeyOnly" ) );
          }
          // fall ok
        case CREATE_ATTRIBUTE:
          dimension = validateDimension( dimension, annotation );
          break;
        default:
          addError( annotation,
              BaseMessages.getString( PKG, "ModelAnnotation.SharedDimension.ValidationError.WrongAnnotationType" ) );
      }
    }
    if ( keyCount == 0 ) {
      addError( null,
          BaseMessages.getString( PKG, "ModelAnnotation.SharedDimension.ValidationError.NoKey" ) );
    }
  }

  private String validateDimension( String dimension, ModelAnnotation<?> annotation ) {
    try {
      String currDim = getDimension( annotation.getAnnotation() );
      if ( StringUtils.isBlank( dimension ) ) {
        dimension = currDim;
      } else if ( !StringUtils.equals( dimension, currDim ) ) {
        addError( annotation,
            BaseMessages.getString( PKG, "ModelAnnotation.SharedDimension.ValidationError.DimensionMismatch" ) );
      }
    } catch ( Exception e ) {
      log.logError( e.getLocalizedMessage(), e );
    }
    return dimension;
  }

  private String getDimension( AnnotationType annotationType ) throws Exception {
    return (String) annotationType.getModelPropertyValueById( CreateAttribute.DIMENSION_ID );
  }

  private void addError( ModelAnnotation<?> annotation, String summary ) {
    msgs.add( summary );
  }

}
