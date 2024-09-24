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

package org.pentaho.di.core.refinery;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.pentaho.di.core.Const;

public class UIBuilder {

  public static final int SHELL_MIN_WIDTH = 435;

  public static final int SHELL_MARGIN_WIDTH = 15;
  public static final int SHELL_MARGIN_HEIGHT = 10;

  public static final int DEFAULT_TEXT_SIZE_SHORT = 50;
  public static final int DEFAULT_TEXT_SIZE_REGULAR = 250;
  public static final int DEFAULT_TEXT_SIZE_LONG = 350;
  public static final int DEFAULT_COMBO_SIZE = 150;

  public static final int BUTTON_MIN_WIDTH = 65;

  public static final int DEFAULT_NO_MARGIN = 0;
  public static final int DEFAULT_LABEL_INPUT_MARGIN = 5;
  public static final int DEFAULT_CONTROLS_TOP_MARGIN = 10;
  public static final int DEFAULT_COMPOSITE_TOP_MARGIN = 15;
  public static final int DEFAULT_COMPOSITE_BOTTOM_MARGIN = 15;

  public static final int LEFT_MARGIN_OFFSET = 7;  // Adjust. UX recommendation: 9
  public static final int RIGHT_MARGIN_OFFSET = -10;

  // groups / fieldsets
  public static final int GROUP_MARGIN_HEIGHT = 15;
  public static final int GROUP_MARGIN_WIDTH = 5;

  public static void positionLabelInputPair( Label label, Control control ) {

    FormData fdl = new FormData();
    fdl.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fdl.top = new FormAttachment( 0, Const.MARGIN );
    label.setLayoutData( fdl );

    FormData fd = new FormData();
    fd.top = new FormAttachment( label, Const.MARGIN );
    fd.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fd.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fd.right = new FormAttachment( 0, LEFT_MARGIN_OFFSET + 252 );
    control.setLayoutData( fd );
  }

  public static void positionControlBelow( Control control, Control widgetAbove, int topMarginHint ) {

    positionControlBelow( control, widgetAbove, topMarginHint, 0 );
  }

  public static void positionControlBelow( Control control, Control widgetAbove, int topMarginHint,
      int leftMarginHint ) {

    int topMargin = DEFAULT_LABEL_INPUT_MARGIN;
    if ( topMarginHint >= 0 ) {
      topMargin = topMarginHint;
    }

    int leftMargin = LEFT_MARGIN_OFFSET;
    if ( leftMarginHint >= 0 ) {
      leftMargin = leftMarginHint;
    }

    FormData fd = new FormData();
    if ( control.getLayoutData() != null && control.getLayoutData() instanceof FormData ) {
      fd = (FormData) control.getLayoutData();
    }

    fd.left = new FormAttachment( 0, leftMargin );
    if ( widgetAbove != null ) {
      fd.top = new FormAttachment( widgetAbove, topMargin );
    } else {
      fd.top = new FormAttachment( 0 );
    }
    control.setLayoutData( fd );
  }

  public static void positionLabelInputPairBelow( Label label, Control control, Control widgetAbove, int topMargin ) {
    positionControlBelow( label, widgetAbove, topMargin );
    positionControlBelow( control, label, DEFAULT_LABEL_INPUT_MARGIN );
  }

  public static void positionLabelInputPairBelow( Label label, Control control, Control widgetAbove, int topMargin,
      int leftMarginHint ) {
    positionControlBelow( label, widgetAbove, topMargin, leftMarginHint );
    positionControlBelow( control, label, DEFAULT_LABEL_INPUT_MARGIN, leftMarginHint );
  }

  public static void positionInputLabelPairBelow( Control control, Label label, Control widgetAbove,
      int topMarginHint ) {

    int topMargin = DEFAULT_LABEL_INPUT_MARGIN;
    if ( topMarginHint >= 0 ) {
      topMargin = topMarginHint;
    }

    FormData fd = new FormData();
    if ( control.getLayoutData() != null && control.getLayoutData() instanceof FormData ) {
      fd = (FormData) control.getLayoutData();
    }

    fd.left = new FormAttachment( 0, 0 );
    if ( widgetAbove == null ) {
      fd.top = new FormAttachment( 0, topMargin );
    } else {
      fd.top = new FormAttachment( widgetAbove, topMargin );
    }
    control.setLayoutData( fd );

    FormData fdl = new FormData();
    if ( label.getLayoutData() != null && label.getLayoutData() instanceof FormData ) {
      fdl = (FormData) label.getLayoutData();
    }

    fdl.left = new FormAttachment( control, 5, SWT.TOP );
    if ( widgetAbove == null ) {
      fdl.top = new FormAttachment( 0, topMargin );
    } else {
      fdl.top = new FormAttachment( widgetAbove, topMargin );
    }
    label.setLayoutData( fdl );
  }

  public static Composite createFormComposite( final Composite parent ) {

    Composite composite = new Composite( parent, SWT.NONE );

    FormLayout layout = new FormLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout( layout );

    FormData fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );

    composite.setLayoutData( fd );

    return composite;
  }

  public static Group createFormGroup( final Composite parent ) {

    Group group = new Group( parent, SWT.NONE );

    FormLayout layout = new FormLayout();
    layout.marginTop = -5; // Adjust. UX Recommendation for Top Margin of Group: 15
    layout.marginWidth = 0;
    layout.marginHeight = GROUP_MARGIN_HEIGHT;
    group.setLayout( layout );

    FormData fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );

    group.setLayoutData( fd );
    return group;
  }
}
