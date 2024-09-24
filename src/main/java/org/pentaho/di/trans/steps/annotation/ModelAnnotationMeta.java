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

import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateCalculatedMember;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.LinkDimension;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.injection.InjectionDeep;
import org.pentaho.di.core.injection.InjectionSupported;

import java.util.ArrayList;
import java.util.List;

@InjectionSupported( localizationPrefix = "AnnotateStream.Injection.",
  groups = {"MEASURE", "ATTRIBUTE", "LINK_DIMENSION", "CALC_MEASURE"} )
@Step( id = "FieldMetadataAnnotation", image = "ModelAnnotation.svg",
  i18nPackageName = "org.pentaho.di.trans.steps.annotation", name = "ModelAnnotation.TransName",
  description = "ModelAnnotation.TransDescription",
  documentationUrl = "mk-95pdia003/advanced-pentaho-data-integration-topics/work-with-the-streamlined-data-refinery"
    + "/use-the-streamlined-data-refinery/building-blocks-for-the-sdr/using-the-annotate-stream-step/use-the-annotate"
    + "-stream-step",
  categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Flow" )
public class ModelAnnotationMeta extends BaseAnnotationMeta {
  private static Class<?> PKG = ModelAnnotationMeta.class; // for i18n purposes, needed by Translator2!!

  /////////////////////////////////////////////////////
  // Temp fields required to support metadata injection
  // These will be injected via the annotation-based injection system.
  // It's up to the init() method of the ModelAnnotationStep to take them and fill in the "real" fields they correspond to
  @Injection( name = "SHARED_ANNOTATION_GROUP" )
  protected String sharedAnnotationGroup;

  @InjectionDeep
  protected transient List<CreateMeasure> createMeasureAnnotations = new ArrayList<>();

  @InjectionDeep
  protected transient List<CreateAttribute> createAttributeAnnotations = new ArrayList<>();

  @InjectionDeep
  protected transient List<LinkDimension> createLinkDimensionAnnotations = new ArrayList<>();

  @InjectionDeep
  protected transient List<CreateCalculatedMember> createCalcMeasureAnnotations = new ArrayList<>();
  // end temp fields
  /////////////////////////////////////////////////////

}
