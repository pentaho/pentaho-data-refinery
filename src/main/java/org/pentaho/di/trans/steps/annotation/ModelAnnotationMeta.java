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

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroupXmlReader;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroupXmlWriter;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationManager;
import org.pentaho.agilebi.modeler.models.annotations.data.ColumnMapping;
import org.pentaho.agilebi.modeler.models.annotations.data.DataProvider;
import org.pentaho.agilebi.modeler.models.annotations.util.KeyValueClosure;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaChangeListenerInterface;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metadata.model.concept.types.DataType;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Rowell Belen
 */
@Step( id = "FieldMetadataAnnotation", image = "ModelAnnotation.svg",
    i18nPackageName = "org.pentaho.di.trans.steps.annotation", name = "ModelAnnotation.TransName",
    description = "ModelAnnotation.TransDescription",
    documentationUrl = "https://help.pentaho.com/Documentation/6.0/0N0/060/0B0/020/0B0",
    categoryDescription = "i18n:org.pentaho.di.trans.step:BaseStep.Category.Flow" )
public class ModelAnnotationMeta extends BaseStepMeta implements StepMetaInterface, StepMetaChangeListenerInterface {

  private static Class<?> PKG = ModelAnnotationMeta.class; // for i18n purposes, needed by Translator2!!

  private boolean sharedDimension; // need to know this before loading from the MetaStore

  private ModelAnnotationGroup modelAnnotations;

  private String modelAnnotationCategory;

  private String targetOutputStep;

  public ModelAnnotationGroup getModelAnnotations() {
    return modelAnnotations;
  }

  public void setModelAnnotations( ModelAnnotationGroup modelAnnotations ) {
    this.modelAnnotations = modelAnnotations;
  }

  public String getModelAnnotationCategory() {
    return modelAnnotationCategory;
  }

  public void setModelAnnotationCategory( String modelAnnotationCategory ) {
    this.modelAnnotationCategory = modelAnnotationCategory;
  }

  public String getTargetOutputStep() {
    return targetOutputStep;
  }

  public void setTargetOutputStep( String targetOutputStep ) {
    this.targetOutputStep = targetOutputStep;
  }

  public void setSharedDimension( boolean sharedDimension ) {
    this.sharedDimension = sharedDimension;
  }

  public boolean isSharedDimension() {
    return sharedDimension;
  }

  @Override
  public void setDefault() {
    modelAnnotations = new ModelAnnotationGroup();
  }

  @Override
  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
      TransMeta transMeta, Trans trans ) {

    return new ModelAnnotationStep( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  @Override
  public StepDataInterface getStepData() {

    return new ModelAnnotationData();
  }

  @Override
  public String getXML() {
    return getModelAnnotationsXml();
  }

  protected String getModelAnnotationsXml() {

    final StringBuffer xml = new StringBuffer();

    xml.append( "    " ).append( XMLHandler.addTagValue( "category", getModelAnnotationCategory() ) );
    xml.append( "    " ).append( XMLHandler.addTagValue( "targetOutputStep", getTargetOutputStep() ) );

    // Use common writer
    ModelAnnotationGroupXmlWriter xmlWriter = new ModelAnnotationGroupXmlWriter( getModelAnnotations() );
    xml.append( xmlWriter.getXML() );

    return xml.toString();
  }

  @Override
  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
    readData( stepnode );

    // This may override the loaded model annotation group
    if ( StringUtils.isNotBlank( getModelAnnotationCategory() ) ) {
      readDataFromMetaStore( metaStore );
    }
  }

  private void readData( Node step ) throws KettleXMLException {
    try {
      ModelAnnotationGroup modelAnnotationGroup = readModelAnnotationGroup( step );
      this.setModelAnnotations( modelAnnotationGroup );

    } catch ( Exception e ) {
      throw new KettleXMLException( BaseMessages.getString(
          PKG, "ModelAnnotationMeta.Exception.UnableToReadStepInfoFromXML" ), e );
    }
  }

  protected ModelAnnotationGroup readModelAnnotationGroup( Node step ) throws KettleXMLException {

    setModelAnnotationCategory( XMLHandler.getTagValue( step, "category" ) );
    setTargetOutputStep( XMLHandler.getTagValue( step, "targetOutputStep" ) );
    ModelAnnotationGroupXmlReader mar = new ModelAnnotationGroupXmlReader();
    ModelAnnotationGroup modelAnnotationGroup = mar.readModelAnnotationGroup( step );
    sharedDimension = modelAnnotationGroup.isSharedDimension();

    return modelAnnotationGroup;
  }

  @Override
  public void saveRep( final Repository rep, final IMetaStore metaStore, final ObjectId id_transformation,
      final ObjectId id_step ) throws
      KettleException {

    try {

      rep.saveStepAttribute( id_transformation, id_step, "CATEGORY_NAME", getModelAnnotationCategory() );
      rep.saveStepAttribute( id_transformation, id_step, "TARGET_OUTPUT_STEP", getTargetOutputStep() );

      // Save model annotations
      if ( getModelAnnotations() != null ) {

        for ( int i = 0; i < getModelAnnotations().size(); i++ ) {

          final ModelAnnotation<?> modelAnnotation =
              getModelAnnotations().get( i );

          // Add default name
          if ( StringUtils.isBlank( modelAnnotation.getName() ) ) {
            modelAnnotation.setName( UUID.randomUUID().toString() ); // backwards compatibility
          }

          rep.saveStepAttribute( id_transformation, id_step, i, "ANNOTATION_NAME",
              modelAnnotation.getName() );
          rep.saveStepAttribute( id_transformation, id_step, i, "ANNOTATION_FIELD_NAME",
              modelAnnotation.getAnnotation().getField() );

          if ( modelAnnotation.getType() != null ) {
            rep.saveStepAttribute( id_transformation, id_step, i, "ANNOTATION_TYPE",
                modelAnnotation.getType().toString() );

            final int INDEX = i; // trap index so we can use in inner class
            modelAnnotation.iterateProperties( new KeyValueClosure() {
              @Override
              public void execute( String key, Serializable serializable ) {
                try {
                  if ( serializable != null && StringUtils.isNotBlank( serializable.toString() ) ) {
                    rep.saveStepAttribute( id_transformation, id_step, INDEX, "PROPERTY_VALUE_" + key,
                        serializable.toString() );
                  }
                } catch ( KettleException e ) {
                  logError( e.getMessage() );
                }
              }
            } );
          }
        }

        rep.saveStepAttribute( id_transformation, id_step, "SHARED_DIMENSION",
            getModelAnnotations().isSharedDimension() );
        rep.saveStepAttribute( id_transformation, id_step, "DESCRIPTION",
            getModelAnnotations().getDescription() );

        List<DataProvider> dataProviders = getModelAnnotations().getDataProviders();
        if ( dataProviders != null && !dataProviders.isEmpty() ) {
          for ( int dIdx = 0; dIdx < dataProviders.size(); dIdx++ ) {

            DataProvider dataProvider = dataProviders.get( dIdx );

            // Save Data Provider properties
            rep.saveStepAttribute( id_transformation, id_step, dIdx, "DP_NAME",
                dataProvider.getName() );
            rep.saveStepAttribute( id_transformation, id_step, dIdx, "DP_SCHEMA_NAME",
                dataProvider.getSchemaName() );
            rep.saveStepAttribute( id_transformation, id_step, dIdx, "DP_TABLE_NAME",
                dataProvider.getTableName() );
            rep.saveStepAttribute( id_transformation, id_step, dIdx, "DP_DATABASE_META_NAME_REF",
                dataProvider.getDatabaseMetaNameRef() );

            List<ColumnMapping> columnMappings = dataProvider.getColumnMappings();
            if ( columnMappings != null && !columnMappings.isEmpty() ) {

              // Save count for loading
              rep.saveStepAttribute( id_transformation, id_step, "CM_COUNT_" + dIdx, columnMappings.size() );

              for ( int cIdx = 0; cIdx < columnMappings.size(); cIdx++ ) {

                ColumnMapping columnMapping = columnMappings.get( cIdx );

                // Save ColumnMapping properties
                rep.saveStepAttribute( id_transformation, id_step, dIdx, "CM_NAME_" + cIdx,
                    columnMapping.getName() );
                rep.saveStepAttribute( id_transformation, id_step, dIdx, "CM_COLUMN_NAME_" + cIdx,
                    columnMapping.getColumnName() );

                if ( columnMapping.getColumnDataType() != null ) {
                  rep.saveStepAttribute( id_transformation, id_step, dIdx, "CM_DATA_TYPE_" + cIdx,
                      columnMapping.getColumnDataType().name() );
                }
              }
            }
          }
        }
      }

    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString(
          PKG, "ModelAnnotationMeta.Exception.UnableToSaveStepInfoToRepository" )
          + id_step, e );
    }
  }

  @Override
  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases )
    throws KettleException {

    ModelAnnotationGroup modelAnnotationGroup = new ModelAnnotationGroup();
    try {

      setModelAnnotationCategory( rep.getStepAttributeString( id_step, "CATEGORY_NAME" ) );
      setTargetOutputStep( rep.getStepAttributeString( id_step, "TARGET_OUTPUT_STEP" ) );

      int nrAnnotations = rep.countNrStepAttributes( id_step, "ANNOTATION_FIELD_NAME" );

      // Read annotations
      for ( int i = 0; i < nrAnnotations; i++ ) {
        String annotationName = rep.getStepAttributeString( id_step, i, "ANNOTATION_NAME" );
        String annotationFieldName = rep.getStepAttributeString( id_step, i, "ANNOTATION_FIELD_NAME" );
        String annotationType = rep.getStepAttributeString( id_step, i, "ANNOTATION_TYPE" );

        // Create model annotation
        ModelAnnotation<?> modelAnnotation = ModelAnnotationGroupXmlReader.create( annotationType, annotationFieldName );
        if ( StringUtils.isNotBlank( annotationName ) ) {
          modelAnnotation.setName( annotationName );
        }

        if ( StringUtils.isNotBlank( annotationType ) ) {
          // Populate annotation properties
          Map<String, Serializable> map = new HashMap<String, Serializable>();
          for ( String key : modelAnnotation.getAnnotation().getModelPropertyIds() ) {
            try {
              String value = rep.getStepAttributeString( id_step, i, "PROPERTY_VALUE_" + key );
              if ( StringUtils.isNotBlank( value ) ) {
                map.put( key, value );
              }
            } catch ( KettleException ke ) {
              // Ignore - not found
            }
          }
          modelAnnotation.populateAnnotation( map );
        }

        // Add to group
        modelAnnotationGroup.add( modelAnnotation );
      }

      modelAnnotationGroup
          .setSharedDimension( BooleanUtils.toBoolean( rep.getStepAttributeString( id_step, "SHARED_DIMENSION" ) ) );
      sharedDimension = modelAnnotationGroup.isSharedDimension();
      modelAnnotationGroup.setDescription( rep.getStepAttributeString( id_step, "DESCRIPTION" ) );

      List<DataProvider> dataProviders = new ArrayList<DataProvider>();
      int nrDataProviders = rep.countNrStepAttributes( id_step, "DP_NAME" );
      for ( int i = 0; i < nrDataProviders; i++ ) {

        DataProvider dataProvider = new DataProvider();

        dataProvider.setName( rep.getStepAttributeString( id_step, i, "DP_NAME" ) );
        dataProvider.setSchemaName( rep.getStepAttributeString( id_step, i, "DP_SCHEMA_NAME" ) );
        dataProvider.setTableName( rep.getStepAttributeString( id_step, i, "DP_TABLE_NAME" ) );
        dataProvider.setDatabaseMetaNameRef( rep.getStepAttributeString( id_step, i, "DP_DATABASE_META_NAME_REF" ) );

        List<ColumnMapping> columnMappings = new ArrayList<ColumnMapping>();
        long nrColumnMappings = rep.getStepAttributeString( id_step, "CM_COUNT_" + i ) != null
            ? Long.valueOf( rep.getStepAttributeString( id_step, "CM_COUNT_" + i ) )
            : 0;
        for ( int j = 0; j < nrColumnMappings; j++ ) {

          ColumnMapping columnMapping = new ColumnMapping();

          columnMapping.setName( rep.getStepAttributeString( id_step, i, "CM_NAME_" + j ) );
          columnMapping.setColumnName( rep.getStepAttributeString( id_step, i, "CM_COLUMN_NAME_" + j ) );
          String dataType = rep.getStepAttributeString( id_step, i, "CM_DATA_TYPE_" + j );
          if ( StringUtils.isNotBlank( dataType ) ) {
            columnMapping.setColumnDataType( DataType.valueOf( dataType ) );
          }

          columnMappings.add( columnMapping );
        }

        dataProvider.setColumnMappings( columnMappings );
        dataProviders.add( dataProvider );
      }
      modelAnnotationGroup.setDataProviders( dataProviders );

    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString(
          PKG, "ModelAnnotationMeta.Exception.UnexpectedErrorReadingStepInfoFromRepository" ), e );
    }

    setModelAnnotations( modelAnnotationGroup );

    // This may override the loaded model annotation group
    if ( StringUtils.isNotBlank( getModelAnnotationCategory() ) ) {
      readDataFromMetaStore( metaStore );
    }
  }

  public void readDataFromMetaStore( IMetaStore metaStore ) {
    try {

      if ( metaStore == null ) {
        return;
      }

      ModelAnnotationManager manager = getModelAnnotationManager( getModelAnnotations() );
      if ( manager.containsGroup( getModelAnnotationCategory(), metaStore ) ) {
        setModelAnnotations( manager.readGroup( getModelAnnotationCategory(), metaStore ) );
      }
    } catch ( Exception e ) {
      logError( e.getMessage() );
    }
  }

  public void saveToMetaStore( IMetaStore metaStore ) throws Exception {
    saveToMetaStore( metaStore, getModelAnnotations() );
  }

  public void saveToMetaStore( IMetaStore metaStore, ModelAnnotationGroup modelAnnotations ) throws Exception {
    if ( metaStore == null ) {
      return;
    }

    ModelAnnotationManager manager = getModelAnnotationManager( modelAnnotations );
    if ( modelAnnotations != null && StringUtils.isBlank( modelAnnotations.getName() ) ) {
      modelAnnotations.setName( this.getName() );
    }
    checkValidName( modelAnnotations.getName() );
    manager.createGroup( modelAnnotations, metaStore );
  }

  public void checkValidName( final String name ) throws KettleException {
    // potentially problematic characters in filesystem/repository metastores
    char[] fileSystemReservedChars = new char[] { '\\', '/', ':', '*', '?', '"', '<', '>', '|', '\t', '\r', '\n' };
    if ( StringUtils.isBlank( name ) ) {
      throw new KettleException(
          BaseMessages.getString( PKG, isSharedDimension() ? "ModelAnnotation.SharedDimensionMissingName.Message"
            : "ModelAnnotation.ModelAnnotationGroupMissingName.Message" ) );
    }
    if ( StringUtils.indexOfAny( name, fileSystemReservedChars ) >= 0
        || StringUtils.startsWith( name, "." ) ) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for ( char ch : fileSystemReservedChars ) {
        if ( first ) {
          first = false;
        } else {
          sb.append( " " );
        }
        if ( Character.isWhitespace( ch ) ) {
          sb.append( StringEscapeUtils.escapeJava( Character.toString( ch ) ) );
        } else {
          sb.append( ch );
        }
      }
      throw new KettleException(
          BaseMessages.getString( PKG, isSharedDimension() ? "ModelAnnotation.MetaStoreInvalidName.SharedDimension.Message"
            : "ModelAnnotation.MetaStoreInvalidName.Message", sb.toString() ) );
    }
  }

  protected ModelAnnotationManager getModelAnnotationManager( ModelAnnotationGroup modelAnnotationGroup ) {
    if ( modelAnnotationGroup.isSharedDimension() ) {
      return new ModelAnnotationManager( true );
    }

    return new ModelAnnotationManager(); // default namespace
  }

  @Override
  public void onStepChange( TransMeta transMeta, StepMeta oldMeta, StepMeta newMeta ) {
    String target = getTargetOutputStep();
    if ( !StringUtils.isBlank( target ) ) {
      if ( target.equals( oldMeta.getName() ) ) {
        setTargetOutputStep( newMeta.getName() );
      }
    }
  }
}
