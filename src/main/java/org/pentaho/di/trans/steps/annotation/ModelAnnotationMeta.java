/*
 * ******************************************************************************
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
 *
 * ******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * Created by rfellows on 6/17/16.
 */
@InjectionSupported( localizationPrefix = "AnnotateStream.Injection.",
  groups = {"MEASURE", "ATTRIBUTE", "LINK_DIMENSION", "CALC_MEASURE"} )
@Step( id = "FieldMetadataAnnotation", image = "ModelAnnotation.svg",
  i18nPackageName = "org.pentaho.di.trans.steps.annotation", name = "ModelAnnotation.TransName",
  description = "ModelAnnotation.TransDescription",
  documentationUrl = "0N0/060/0B0/020/0B0",
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
