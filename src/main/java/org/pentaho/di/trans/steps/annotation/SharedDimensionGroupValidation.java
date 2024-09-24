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
        case BLANK:
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
