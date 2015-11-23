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
