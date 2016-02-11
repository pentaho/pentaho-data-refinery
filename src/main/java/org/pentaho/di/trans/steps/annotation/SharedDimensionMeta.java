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

import org.pentaho.di.core.annotations.Step;

@Step( id = "CreateSharedDimensions", image = "SharedDimensions.svg",
    i18nPackageName = "org.pentaho.di.trans.steps.annotation", name = "SharedDimension.TransName",
    description = "SharedDimension.TransDescription",
    documentationUrl = "0N0/060/0B0/020/0C0",
    categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Flow" )
public class SharedDimensionMeta extends ModelAnnotationMeta {

  private static Class<?> PKG = SharedDimensionMeta.class; // for i18n purposes, needed by Translator2!!

}
