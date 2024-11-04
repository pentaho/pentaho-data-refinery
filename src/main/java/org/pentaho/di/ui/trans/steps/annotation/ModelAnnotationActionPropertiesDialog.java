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


package org.pentaho.di.ui.trans.steps.annotation;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.BlankAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.CreateCalculatedMember;
import org.pentaho.agilebi.modeler.models.annotations.CreateDimensionKey;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.LinkDimension;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.agilebi.modeler.models.annotations.ModelProperty;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.steps.annotation.BaseAnnotationMeta;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ComboValuesSelectionListener;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metastore.api.IMetaStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Rowell Belen
 */
public class ModelAnnotationActionPropertiesDialog extends ModelAnnotationActionDialog {

  protected Composite contentPanel;

  private Label wField;
  private CCombo wActionList;
  private ColumnInfo[] ciFields;
  private ComboValuesSelectionListener comboValuesSelectionListener;
  private TableView wProperties;

  private AnnotationType annotationType;
  private OptionsResolver optionsResolver;

  public ModelAnnotationActionPropertiesDialog( Shell parent,
      BaseAnnotationMeta baseStepMeta,
      TransMeta transMeta, String stepname,
      IMetaStore metaStore ) {
    super( parent, baseStepMeta, transMeta, stepname, metaStore );
    optionsResolver = new OptionsResolver();
  }

  @Override
  protected void initializeListeners() {

    super.initializeListeners();

    comboValuesSelectionListener = new ComboValuesSelectionListener() {
      @Override
      public String[] getComboValues( TableItem tableItem, int rowNr, int colNr ) {
        try {

          String name = tableItem.getText( 1 );

          if ( StringUtils.isNotBlank( name ) ) {

            // Use currently loaded model in the table
            final AnnotationType at = annotationType;
            final Class propertyClassType = at.getModelPropertyNameClassType( name );

            // Parent options
            if ( name.equals( CreateAttribute.PARENT_ATTRIBUTE_NAME ) ) {
              List<String> names = new ArrayList<String>();
              for ( ModelAnnotation m : getModifiedModelAnnotations() ) {
                if ( ModelAnnotation.Type.CREATE_ATTRIBUTE.equals( m.getType() ) ) {
                  if ( StringUtils.isNotBlank( m.getAnnotation().getName() ) ) {
                    // Do not include self
                    if ( !StringUtils
                        .equals( at.getName(), m.getAnnotation().getName() ) ) {
                      names.add( m.getAnnotation().getName() ); // return display names
                    }
                  }
                }
              }
              return names.toArray( new String[names.size()] );
            }

            // Dimension options
            if ( name.equals( CreateAttribute.DIMENSION_NAME ) ) {
              LinkedHashSet<String> names = new LinkedHashSet<String>();
              for ( ModelAnnotation<?> m : getModifiedModelAnnotations() ) {
                // Do not include self
                if ( m.getAnnotation() != null
                    && StringUtils.isNotBlank( m.getAnnotation().getName() )
                    && !at.equals( m.getAnnotation() ) ) {
                  // return dimension names
                  String dimensionName = null;
                  if ( ModelAnnotation.Type.CREATE_ATTRIBUTE.equals( m.getType() ) ) {
                    // CCombo.setItems (the end consumer of this call) will throw an error if you give it an array
                    // of strings that has a null entry. prevent that from happening here
                    dimensionName = ( (CreateAttribute) m.getAnnotation() ).getDimension();
                    if ( StringUtils.isNotBlank( dimensionName ) ) {
                      names.add( dimensionName );
                    }
                  } else if ( ModelAnnotation.Type.CREATE_DIMENSION_KEY.equals( m.getType() ) ) {
                    // CCombo.setItems (the end consumer of this call) will throw an error if you give it an array
                    // of strings that has a null entry. prevent that from happening here
                    dimensionName = ( (CreateDimensionKey) m.getAnnotation() ).getDimension();
                    if ( StringUtils.isNotBlank( dimensionName ) ) {
                      names.add( dimensionName );
                    }
                  }
                }
              }
              return names.toArray( new String[names.size()] );
            }

            // Hierarchy options
            if ( name.equals( CreateAttribute.HIERARCHY_NAME ) ) {
              LinkedHashSet<String> names = new LinkedHashSet<String>();
              for ( ModelAnnotation m : getModifiedModelAnnotations() ) {
                if ( ModelAnnotation.Type.CREATE_ATTRIBUTE.equals( m.getType() ) ) {
                  if ( StringUtils.isNotBlank( m.getAnnotation().getName() ) ) {
                    // Do not include self
                    if ( !StringUtils.equals( at.getName(), m.getAnnotation().getName() ) ) {
                      String hierarchy = ( (CreateAttribute) m.getAnnotation() ).getHierarchy();
                      if ( StringUtils.isNotBlank( hierarchy ) ) {
                        names.add( hierarchy );
                      }
                    }
                  }
                }
              }
              return names.toArray( new String[names.size()] );
            }

            // Time Type options
            if ( name.equals( CreateAttribute.TIME_TYPE_NAME )
                && ( propertyClassType != null )
                && propertyClassType.equals( ModelAnnotation.TimeType.class ) ) {
              return ModelAnnotation.TimeType.names();
            }

            // GeoType options
            if ( name.equals( CreateAttribute.GEO_TYPE_NAME )
                && ( propertyClassType != null )
                && propertyClassType.equals( ModelAnnotation.GeoType.class ) ) {
              return ModelAnnotation.GeoType.names();
            }

            if ( CreateAttribute.LATITUDE_FIELD_NAME.equals( name )
                || CreateAttribute.LONGITUDE_FIELD_NAME.equals( name ) ) {
              return optionsResolver.resolveAvailableFieldsOptions( transMeta, stepname, getModelAnnotation() );
            }

            // Time Format String options
            if ( name.equals( CreateAttribute.TIME_FORMAT_NAME ) ) {
              String timeType = StringUtils.defaultIfBlank( findTableItemValue( CreateAttribute.TIME_TYPE_NAME ), "" );
              return optionsResolver.resolveTimeSourceFormatOptions( timeType );
            }

            // Ordinal Field options
            if ( name.equals( CreateAttribute.ORDINAL_FIELD_NAME ) ) {
              return optionsResolver.resolveAvailableFieldsOptions( transMeta, stepname, getModelAnnotation() );
            }

            // Measure Format String options
            if ( name.equals( CreateMeasure.FORMAT_STRING_NAME )
              && ModelAnnotation.Type.CREATE_MEASURE.equals( at.getType() ) ) {
              return optionsResolver.resolveMeasureFormatOptions();
            }

            if ( name.equals( CreateMeasure.FORMAT_STRING_NAME )
              && ModelAnnotation.Type.CREATE_ATTRIBUTE.equals( at.getType() ) ) {
              return optionsResolver.resolveAttributeFormatOptions( applyToType( getValueMeta() ) );
            }

            // AggregationType options
            if ( name.equals( CreateMeasure.AGGREGATE_TYPE_NAME )
                && ( propertyClassType != null )
                && propertyClassType.equals( AggregationType.class ) ) {
              return optionsResolver.resolveAggregationTypeOptions( getValueMeta(), at );
            }

            // Boolean Type options
            if ( propertyClassType != null && ( propertyClassType.equals( Boolean.class ) || propertyClassType
                .equals( boolean.class ) ) ) {
              return optionsResolver.resolveBooleanOptions();
            }

            if ( name.equals( LinkDimension.SHARED_DIMENSION_NAME ) ) {
              return optionsResolver.resolveSharedDimensions(
                  new ModelAnnotationManager( true ), getMetaStore() );
            }
          }

        } catch ( Exception e ) {
          logError( e.getMessage() );
        }

        return new String[0];
      }
    };
  }

  private void createContentPanel( Composite parent ) {
    contentPanel = new Composite( parent, SWT.NULL );
    FormData fd = new FormData();
    fd.top = new FormAttachment( 0, 0 );
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.bottom = new FormAttachment( wBottomHorizontalSeparator, 0 );
    contentPanel.setLayoutData( fd );
    contentPanel.setLayout( new FormLayout() );
    PropsUI.getInstance().setLook( contentPanel );
  }

  protected Composite getMainComposite() {
    return shell;
  }

  public void createWidgets() {

    createContentPanel( getMainComposite() );

    // Create Annotate Field label
    Label wlField = new Label( contentPanel, SWT.LEFT );
    wlField.setText( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.AnnotateField.Label" ) );
    props.setLook( wlField );
    FormData fdlField = new FormData();
    fdlField.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fdlField.top = new FormAttachment( 0, margin );
    wlField.setLayoutData( fdlField );

    // Create Annotate Field
    wField = new Label( contentPanel, SWT.LEFT );
    props.setLook( wField );
    FormData fdwField = new FormData();
    fdwField.width = SHELL_MIN_WIDTH;
    fdwField.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fdwField.top = new FormAttachment( wlField, margin );
    wField.setLayoutData( fdwField );

    // ActionList Label
    Label wlActionList = new Label( contentPanel, SWT.RIGHT );
    wlActionList.setText( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.Actions.Label" ) );
    props.setLook( wlActionList );
    FormData fdlActionList = new FormData();
    fdlActionList.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fdlActionList.top = new FormAttachment( wField, margin );
    wlActionList.setLayoutData( fdlActionList );

    // Create Action List
    wActionList = new CCombo( contentPanel, SWT.LEFT | SWT.BORDER );
    wActionList.setText( ModelAnnotation.Type.CREATE_ATTRIBUTE.description() ); // default
    wActionList
        .setToolTipText( BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.Actions.Label" ) );
    props.setLook( wActionList );
    FormData fdActionList = new FormData();
    fdActionList.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET );
    fdActionList.top = new FormAttachment( wlActionList, margin );
    wActionList.setLayoutData( fdActionList );

    // Create Properties Table
    createPropertiesTable( wActionList );

    // Add Listeners
    wActionList.addModifyListener( new ModifyListener() {
      @Override
      public void modifyText( ModifyEvent modifyEvent ) {

        if ( StringUtils.isBlank( wActionList.getText() ) ) {
          return;
        }

        if ( getModelAnnotation().getAnnotation() != null && getModelAnnotation().getAnnotation().getType()
            .description().equals( wActionList.getText() ) ) {
          loadAnnotationProperties( getModelAnnotation().getAnnotation() );
        } else {
          AnnotationType annotationType = createAnnotationType();
          if ( annotationType != null ) {
            loadAnnotationProperties( annotationType );
          }
        }
      }
    } );
  }

  private void createPropertiesTable( Control topWidget ) {

    int tableCols = 2;
    int rowCount = 0; // TODO

    ciFields = new ColumnInfo[tableCols];
    ciFields[0] =
        new ColumnInfo(
            BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.PropertiesTable.Column.Name" ),
            ColumnInfo.COLUMN_TYPE_TEXT, new String[] { "" }, true );
    ciFields[1] =
        new ColumnInfo(
            BaseMessages.getString( PKG, "ModelAnnotation.ModelAnnotationActionDialog.PropertiesTable.Column.Value" ),
            ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false );

    // Add combo values listener
    ciFields[1].setComboValuesSelectionListener( comboValuesSelectionListener );

    // Annotations Table
    wProperties =
        new TableView( transMeta, contentPanel, SWT.BORDER
            | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, ciFields, rowCount, lsMod, props );
    FormData fdProperties = new FormData();
    fdProperties.top = new FormAttachment( topWidget, margin + 10 );
    fdProperties.left = new FormAttachment( 0, LEFT_MARGIN_OFFSET + 2 );
    fdProperties.right = new FormAttachment( 100, RIGHT_MARGIN_OFFSET );
    fdProperties.bottom = new FormAttachment( 100, -10 );
    wProperties.setLayoutData( fdProperties );

    // Try to set initial size of the columns
    wProperties.getTable().setSize( SHELL_MIN_WIDTH, SHELL_MIN_HEIGHT );
    int width = ( SHELL_MIN_WIDTH - wProperties.getTable().getColumn( 0 ).getWidth() ) / 2;
    wProperties.getTable().getColumn( 0 ).setWidth( 0 ); // hide numbers column
    wProperties.getTable().getColumn( 1 ).setWidth( width - 10 );
    wProperties.getTable().getColumn( 2 ).setWidth( width - 10 );
  }

  private void populateActionList() {
    wActionList.removeAll();
    for ( ModelAnnotation.Type annotationType : getAnnotationTypes() ) {
      wActionList.add( annotationType.description() );
      wActionList.setData( annotationType.description(), annotationType );
    }
  }

  public void populateDialog() {
    populateActionList();
    if ( getModelAnnotation() == null ) {
      return;
    }

    wField.setText( StringUtils.defaultIfBlank( getModelAnnotation().getField(), "" ) );

    if ( getModelAnnotation().getAnnotation() != null ) {
      wActionList.setText( getModelAnnotation().getAnnotation().getType().description() );
      if ( wActionList.getData( wActionList.getText() ) == null ) {
        // invalid action
        wActionList.setText( "" );
      }
    } else {
      wActionList.setText( "" );
    }

    loadAnnotationProperties( getModelAnnotation().getAnnotation() );
  }

  private void loadAnnotationProperties( AnnotationType annotationType ) {

    wProperties.clearAll();

    if ( annotationType == null ) {
      return;
    }

    this.annotationType = annotationType;
    for ( ModelProperty prop : annotationType.getModelProperties() ) {
      String name = prop.name();
      TableItem item = new TableItem( wProperties.table, SWT.NONE );
      if ( getModelAnnotation() != null && !prop.hideUI() && applies( prop ) ) {
        item.setText( 1, name );
        try {
          Object object = annotationType.getModelPropertyValueByName( name );
          if ( object != null ) {
            String value = object.toString();
            if ( StringUtils.equals( name, CreateAttribute.UNIQUE_NAME )
              || StringUtils.equals( name, CreateAttribute.HIDDEN_NAME )
              || StringUtils.equals( name, CreateMeasure.HIDDEN_NAME ) ) {
              value = StringUtils.capitalize( value );
            }
            item.setText( 2, value );
          }
        } catch ( Exception e ) {
          logError( e.getMessage() );
        }
      }
    }

    wProperties.removeEmptyRows();
    wProperties.setRowNums();
  }

  private boolean applies( final ModelProperty prop ) {
    return Arrays.asList( prop.appliesTo() ).contains( applyToType( getValueMeta() ) );
  }

  private ModelProperty.AppliesTo applyToType( final ValueMetaInterface valueMeta ) {
    int type = valueMeta.getType();
    switch ( type ) {
      case ValueMetaInterface.TYPE_BIGNUMBER:
      case ValueMetaInterface.TYPE_INTEGER:
      case ValueMetaInterface.TYPE_NUMBER:
        return ModelProperty.AppliesTo.Numeric;
      case ValueMetaInterface.TYPE_TIMESTAMP:
      case ValueMetaInterface.TYPE_DATE:
        return ModelProperty.AppliesTo.Time;
      default:
        return ModelProperty.AppliesTo.String;
    }
  }

  protected AnnotationType createAnnotationType() {

    if ( StringUtils.isBlank( wActionList.getText() ) ) {
      BlankAnnotation blankAnnotation = new BlankAnnotation();
      blankAnnotation.setField( getModelAnnotation().getField() );
      return blankAnnotation;
    }

    switch ( (ModelAnnotation.Type) wActionList.getData( wActionList.getText() ) ) {
      case CREATE_MEASURE:
        CreateMeasure cm = new CreateMeasure();
        cm.setField( getModelAnnotation().getField() );
        cm.setName( getModelAnnotation().getField()  );
        ValueMetaInterface vmi = getValueMeta();
        if ( vmi != null && !vmi.isNumeric() ) {
          cm.setAggregateType( AggregationType.COUNT ); // change default for non-numeric fields
        }
        return cm;
      case CREATE_ATTRIBUTE:
        CreateAttribute createAttribute = new CreateAttribute();
        createAttribute.setField( getModelAnnotation().getField() );
        createAttribute.setName( getModelAnnotation().getField()  );
        return createAttribute;
      case CREATE_DIMENSION_KEY:
        CreateDimensionKey createKey = new CreateDimensionKey();
        createKey.setField( getModelAnnotation().getField() );
        createKey.setName( wField.getText() );
        return createKey;
      case LINK_DIMENSION:
        LinkDimension linkDimension = new LinkDimension();
        linkDimension.setField( getModelAnnotation().getField() );
        return linkDimension;
      case CREATE_CALCULATED_MEMBER:
        CreateCalculatedMember createCalculatedMember = new CreateCalculatedMember();
        return createCalculatedMember;
    }

    return null;
  }

  protected void persistAnnotationProperties() throws ModelerException {

    if ( getModelAnnotation() == null ) {
      return;
    }

    AnnotationType annotationType = createAnnotationType();
    getModelAnnotation().setAnnotation( annotationType ); // always override existing type
    for ( int i = 0; i < wProperties.getItemCount(); i++ ) {
      String name = wProperties.getItem( i, 1 );
      String value = wProperties.getItem( i, 2 );
      if ( value.equals( "" ) ) {
        value = null;
      }
      try {
        getModelAnnotation().getAnnotation().setModelPropertyByName( name, value );
      } catch ( Exception e ) {
        logError( e.getMessage() );
      }
    }

    if ( annotationType != null ) {
      annotationType.validate(); // may throw ModelerException
      if ( !getModifiedModelAnnotations().contains( getModelAnnotation() ) ) {
        getModifiedModelAnnotations().add( getModelAnnotation() );
      }
    }
  }

  private Iterable<ModelAnnotation.Type> getAnnotationTypes() {
    List<ModelAnnotation.Type> types =
        new ArrayList<ModelAnnotation.Type>();
    for ( ModelAnnotation.Type type : ModelAnnotation.Type.values() ) {
      if ( type.isApplicable( getModelAnnotations(), getModelAnnotation(), getValueMeta() ) ) {
        types.add( type );
      }
    }
    return types;
  }

  private ValueMetaInterface getValueMeta() {
    if ( StringUtils.isNotBlank( getModelAnnotation().getField() ) ) {
      try {
        RowMetaInterface rowMeta = transMeta.getPrevStepFields( stepname );
        for ( ValueMetaInterface valueMeta : rowMeta.getValueMetaList() ) {
          if ( getModelAnnotation().getField().equals( valueMeta.getName() ) ) {
            return valueMeta;
          }
        }
      } catch ( KettleStepException e ) {
        log.logError( e.getLocalizedMessage() );
      }
    }
    return null;
  }

  private String findTableItemValue( final String propertyName ) {

    if ( StringUtils.isNotBlank( propertyName ) && ( wProperties.getTable() != null ) ) {
      for ( int i = 0; i < wProperties.getTable().getItemCount(); i++ ) {
        TableItem item = wProperties.getTable().getItem( i );
        if ( item != null ) {
          if ( StringUtils.equals( propertyName, item.getText( 1 ) ) ) {
            return item.getText( 2 );
          }
        }
      }
    }

    return null;
  }
}
