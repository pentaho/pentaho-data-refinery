/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.di.trans.steps.annotation;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.step.StepInjectionMetaEntry;
import org.pentaho.di.trans.step.StepMetaInjectionInterface;

import java.util.List;

/**
 * @author Rowell Belen
 */
public class ModelAnnotationMetaInjection implements StepMetaInjectionInterface {
  @Override public List<StepInjectionMetaEntry> getStepInjectionMetadataEntries() throws KettleException {
    return null;
  }

  @Override public void injectStepMetadataEntries( List<StepInjectionMetaEntry> metadata ) throws KettleException {

  }

  @Override public List<StepInjectionMetaEntry> extractStepMetadataEntries() throws KettleException {
    return null;
  }
}
