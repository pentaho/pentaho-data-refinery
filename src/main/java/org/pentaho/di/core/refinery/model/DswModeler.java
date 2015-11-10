/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
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

package org.pentaho.di.core.refinery.model;

import org.apache.commons.lang.StringUtils;
import org.pentaho.agilebi.modeler.BaseModelerWorkspaceHelper;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.ModelerPerspective;
import org.pentaho.agilebi.modeler.ModelerWorkspace;
import org.pentaho.agilebi.modeler.geo.GeoContext;
import org.pentaho.agilebi.modeler.geo.GeoContextFactory;
import org.pentaho.agilebi.modeler.geo.GeoContextPropertiesProvider;
import org.pentaho.agilebi.modeler.models.annotations.AnnotationType;
import org.pentaho.agilebi.modeler.models.annotations.CreateAttribute;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup.ApplyStatus;
import org.pentaho.agilebi.modeler.nodes.DimensionMetaData;
import org.pentaho.agilebi.modeler.nodes.DimensionMetaDataCollection;
import org.pentaho.agilebi.modeler.nodes.HierarchyMetaData;
import org.pentaho.agilebi.modeler.nodes.LevelMetaData;
import org.pentaho.agilebi.modeler.util.TableModelerSource;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.refinery.DataProviderHelper;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.metadata.automodel.AutoModeler;
import org.pentaho.metadata.automodel.PhysicalTableImporter;
import org.pentaho.metadata.automodel.SchemaTable;
import org.pentaho.metadata.model.Category;
import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.model.IPhysicalColumn;
import org.pentaho.metadata.model.IPhysicalModel;
import org.pentaho.metadata.model.LogicalColumn;
import org.pentaho.metadata.model.LogicalModel;
import org.pentaho.metadata.model.LogicalTable;
import org.pentaho.metadata.model.SqlDataSource;
import org.pentaho.metadata.model.SqlPhysicalColumn;
import org.pentaho.metadata.model.SqlPhysicalModel;
import org.pentaho.metadata.model.SqlPhysicalTable;
import org.pentaho.metadata.model.concept.Concept;
import org.pentaho.metadata.model.concept.types.DataType;
import org.pentaho.metadata.model.concept.types.LocalizedString;
import org.pentaho.metadata.model.olap.OlapCube;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.pms.core.exception.PentahoMetadataException;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup.ApplyStatus.FAILED;
import static org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup.ApplyStatus.NULL_ANNOTATION;
import static org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup.ApplyStatus.SUCCESS;

public class DswModeler {

  private static final Class<?> PKG = JobEntryBuildModel.class;

  private LogChannelInterface log;

  private static final String MONDRIAN_CATALOG_REF = "MondrianCatalogRef";

  public DswModeler( LogChannelInterface log ) {
    this.log = log;
  }

  public void setLog( final LogChannelInterface log ) {
    this.log = log;
  }

  /**
   * Creates a new DSW-enabled XMI model
   * @param modelName
   * @param source
   * @param dbMeta
   * @param stepMetaDataCombi
   * @param modelAnnotations
   * @param metaStore
   * @return
   * @throws ModelerException
   */
  public Domain createModel( final String modelName, TableModelerSource source, DatabaseMeta dbMeta,
                             final StepMetaDataCombi stepMetaDataCombi, final ModelAnnotationGroup modelAnnotations,
                             final IMetaStore metaStore )
    throws ModelerException {
    // Create PME with physical metadata and then set into modeler
    Domain domain = source.generateDomain( valueMetaStrategy( stepMetaDataCombi ) );

    if ( domain.getLogicalModels().get( 0 ).getLogicalTables().get( 0 ).getLogicalColumns().size() == 0 ) {
      throw new ModelerException( BaseMessages.getString( PKG, "BuildModelJob.Error.NoData" ) );
    }

    GeoContext geoContext = initGeoContext();
    ModelerWorkspace model =
        new ModelerWorkspace( new RefineryModelerWorkspaceHelper( geoContext ), geoContext );
    model.setModelSource( source );
    model.setDomain( domain );
    model.setModelName( modelName );

    // Use modeler to generate OLAP metadata
    model.getWorkspaceHelper().autoModelFlat( model );

    // if there is a dimension with the same name as the geo, remove it preemptively
    if ( hasGeoDimConflict( geoContext, modelAnnotations ) ) {
      removeAutoGeo( model );
    }

    // Now save update PME with OLAP metadata
    model.getWorkspaceHelper().populateDomain( model );

    // Now to do black magic to make PME a DSW data source
    enableDswModel( domain, model );

    // set the category name that will show up in report designer
    setReportModelName( modelName, model );

    applyAnnotations( model, modelAnnotations, metaStore );

    final Domain modeledDomain = model.getDomain();
    // Swap data source before xmi generation
    updateDatasourceAccess( modeledDomain, dbMeta );

    return modeledDomain;
  }

  private boolean hasGeoDimConflict( GeoContext geoContext, ModelAnnotationGroup annotations ) {
    if ( geoContext == null || StringUtils.isEmpty( geoContext.getDimensionName() ) ) {
      return false;
    }
    String geoDim = geoContext.getDimensionName();
    for ( ModelAnnotation<? extends AnnotationType> annotation : annotations ) {
      if ( annotation.getType().equals( ModelAnnotation.Type.CREATE_ATTRIBUTE ) ) {
        if ( geoDim.equalsIgnoreCase( ( (CreateAttribute) annotation.getAnnotation() ).getDimension() ) ) {
          return true;
        }
      }
    }
    return false;
  }

  private void removeAutoGeo( final ModelerWorkspace workspace ) {
    DimensionMetaDataCollection dimensions = workspace.getModel().getDimensions();
    DimensionMetaData toRemove = null;
    GeoContext geoContext = workspace.getGeoContext();
    for ( DimensionMetaData dimensionMetaData : dimensions ) {
      if ( geoContext != null && dimensionMetaData.getName().equals( geoContext.getDimensionName() ) ) {
        for ( HierarchyMetaData hierarchyMetaData : dimensionMetaData ) {
          if ( hierarchyMetaData.getName().equals( geoContext.getDimensionName() ) ) {
            for ( LevelMetaData levelMetaData : hierarchyMetaData ) {
              if ( levelMetaData.getMemberAnnotations().get( "Data.Role" ) != null ) {
                toRemove = dimensionMetaData;
                break;
              }
            }
          }
        }
      }
    }
    if ( toRemove != null ) {
      dimensions.remove( toRemove );
    }
  }

  private GeoContext initGeoContext() {
    try {
      FileInputStream fis = new FileInputStream( geoRolesFile() );
      Properties props = new Properties();
      props.load( fis );
      GeoContextPropertiesProvider config = new GeoContextPropertiesProvider( props );

      return GeoContextFactory.create( config );
    } catch ( Throwable e ) {
      log.logDebug( "unable to locate geoRoles.properties" );
    }
    return null;
  }

  private File geoRolesFile() {
    final File spoonFile = new File( geoRolesProperties() );
    if ( spoonFile.exists() ) {
      return spoonFile;
    }
    String solutionPath = PentahoSystem.getApplicationContext()
        .getSolutionPath( "system/kettle/plugins/data-refinery-pdi-plugin/agile-bi/geoRoles.properties" );
    return new File( solutionPath );
  }

  String geoRolesProperties() {
    return "plugins/data-refinery-pdi-plugin/agile-bi/geoRoles.properties";
  }

  private void applyAnnotations(
      final ModelerWorkspace model, final ModelAnnotationGroup modelAnnotations, final IMetaStore metaStore )
    throws ModelerException {
    Map<ApplyStatus, List<ModelAnnotation>> statusMap =
        modelAnnotations.applyAnnotations( model, metaStore );
    if ( log.isDetailed() ) {
      logDetailed( statusMap.get( SUCCESS ), "ModelAnnotation.log.AnnotationSuccess" );
      logDetailed( statusMap.get( FAILED ), "ModelAnnotation.log.AnnotationFailure" );
    }
    logDebug( statusMap.get( NULL_ANNOTATION ), "Ignoring a null annotation" );
  }

  private void logDetailed( final List<ModelAnnotation> modelAnnotations, final String msgKey ) {
    for ( ModelAnnotation modelAnnotation : modelAnnotations ) {
      log.logDetailed(
          BaseMessages.getString(
              PKG, msgKey, modelAnnotation.getAnnotation().getSummary() ) );
    }
  }

  private void logDebug( final List<ModelAnnotation> modelAnnotations, final String msg ) {
    for ( ModelAnnotation modelAnnotation : modelAnnotations ) {
      log.logDebug( msg );
    }
  }

  private PhysicalTableImporter.ImportStrategy valueMetaStrategy( final StepMetaDataCombi stepMetaDataCombi )
    throws ModelerException {
    return new RefineryValueMetaStrategy( stepMetaDataCombi );
  }

  /**
   * Updates the connection for an existing DSW
   * 
   * @param modelName Name of model to be generated ( no .xmi )
   * @param domain Template model
   * @param dbMeta
   * @param schemaName
   * @param tableName
   * @return
   * @throws ColumnMismatchException
   * @throws UnsupportedModelException
   * @throws PentahoMetadataException
   */
  public Domain updateModel( String modelName, Domain domain, DatabaseMeta dbMeta, String schemaName, String tableName )
    throws ColumnMismatchException, UnsupportedModelException, PentahoMetadataException {
    final String dswId = modelName + ".xmi";
    domain.setId( dswId ); // ?
    SqlPhysicalModel physicalModel = createPhysicalModel( modelName, dbMeta, schemaName, tableName );
    updatePhysicalModel( domain, physicalModel );
    updateDatasourceAccess( domain, dbMeta );
    updateModelName( modelName, domain );
    return domain;
  }

  private void updateModelName( String modelName, Domain domain ) throws UnsupportedModelException {
    for ( LogicalModel model : domain.getLogicalModels() ) {
      updateConceptName( modelName, model );
      if ( model.getProperty( MONDRIAN_CATALOG_REF ) != null ) {
        model.setProperty( MONDRIAN_CATALOG_REF, modelName );
        @SuppressWarnings( "unchecked" )
        List<OlapCube> cubes = (List<OlapCube>) model.getProperty( LogicalModel.PROPERTY_OLAP_CUBES );
        if ( cubes != null && cubes.size() == 1 ) {
          cubes.get( 0 ).setName( modelName );
        } else {
          throw new UnsupportedModelException();
        }
      }
      for ( Category category : model.getCategories() ) {
        if ( model.getCategories().size() == 1 ) {
          // doesn't seem to show up anywhere but is set to model name when created
          category.setId( modelName );
        }
        updateConceptName( modelName, category );
      }
    }
  }

  private void updateConceptName( String newName, Concept concept ) {
    LocalizedString name = (LocalizedString) concept.getProperty( Concept.NAME_PROPERTY );
    for ( String locale : name.getLocales() ) {
      name.setString( locale, newName );
    }
  }

  private void enableDswModel( Domain domain, ModelerWorkspace model ) {
    domain.setId( model.getModelName() + ".xmi" );
    LogicalModel lModel = model.getLogicalModel( ModelerPerspective.ANALYSIS );
    if ( lModel == null ) {
      lModel = model.getLogicalModel( ModelerPerspective.REPORTING );
    }
    lModel.setProperty( "AGILE_BI_GENERATED_SCHEMA", "TRUE" );
    lModel.setProperty( "WIZARD_GENERATED_SCHEMA", "TRUE" );

    String catName = lModel.getName( Locale.getDefault().toString() );

    // strip off the _olap suffix for the catalog ref
    catName = catName.replace( BaseModelerWorkspaceHelper.OLAP_SUFFIX, "" );
    lModel.setProperty( MONDRIAN_CATALOG_REF, catName ); //$NON-NLS-1$
  }

  /**
   * Set friendly category name for reports
   * @param modelName
   * @param model
   */
  private void setReportModelName( final String modelName, ModelerWorkspace model ) {
    LogicalModel reportModel = model.getLogicalModel( ModelerPerspective.REPORTING );
    // should only be one category with a default locale
    for ( Category category : reportModel.getCategories() ) {
      updateConceptName( modelName, category );
    }
  }

  public void updateDatasourceAccess( Domain domain, final DatabaseMeta databaseMeta ) {
    // For now, there is only one physical model / data source
    SqlDataSource sqlDataSource =
        ( (SqlPhysicalModel) domain.getPhysicalModels().get( 0 ) ).getDatasource();
    if ( sqlDataSource != null ) {

      // Replace embedded database connection properties with published named connection
      // See org.pentaho.reporting.platform.plugin.connection.PentahoPmdConnectionProvider.createConnection()
      sqlDataSource.setType( SqlDataSource.DataSourceType.JNDI );
      sqlDataSource.setDatabaseName( databaseMeta.getName() ); // Use database connection name, not database name

      // Clear the rest of the connection properties
      sqlDataSource.setPort( null );
      sqlDataSource.setUsername( null );
      sqlDataSource.setPassword( null );
      sqlDataSource.setHostname( null );
    }
  }

  private void updatePhysicalModel( Domain domain, SqlPhysicalModel pModel ) throws ColumnMismatchException, UnsupportedModelException {
    // ASSUMES both are sql physical models
    List<IPhysicalModel> physicalModels = new ArrayList<IPhysicalModel>( 1 );
    physicalModels.add( pModel );
    domain.setPhysicalModels( physicalModels );
    for ( LogicalModel lModel : domain.getLogicalModels() ) {
      // explicitly disallowing star models here until properly handled
      checkIfStarModel( lModel );
      updatePhysicalModel( lModel, pModel );
    }
  }

  private void updatePhysicalModel( LogicalModel logicalModel, SqlPhysicalModel physicalModel )
    throws ColumnMismatchException, UnsupportedModelException {
    // only one table now, matching columns for name and type
    SqlPhysicalTable theTable = physicalModel.getPhysicalTables().get( 0 );
    Map<ColumnKey, IPhysicalColumn> columns = new HashMap<DswModeler.ColumnKey, IPhysicalColumn>();
    for ( IPhysicalColumn column : theTable.getPhysicalColumns() ) {
      columns.put( new ColumnKey( (SqlPhysicalColumn) column ), column );
    }
    // replace every physical table/column
    logicalModel.setPhysicalModel( physicalModel );
    for ( LogicalTable logicalTable: logicalModel.getLogicalTables() ) {
      logicalTable.setPhysicalTable( theTable );
      for ( LogicalColumn column : logicalTable.getLogicalColumns() ) {
        ColumnKey prevColumn = new ColumnKey( column.getPhysicalColumn() );
        IPhysicalColumn physicalColumn = columns.get( prevColumn );
        if ( physicalColumn != null ) {
          column.setPhysicalColumn( physicalColumn );
        } else {
          throw new ColumnMismatchException( prevColumn );
        }
      }
    }
  }

  private void checkIfStarModel( LogicalModel existingModel ) throws UnsupportedModelException {
    // only for update until feature is added
    if ( existingModel.getLogicalTables().size() > 1 ) {
      throw new UnsupportedModelException();
    }
  }

  private static class ColumnKey {
    private DataType dataType;
    private String columnName;

    ColumnKey( IPhysicalColumn column ) throws UnsupportedModelException {
      dataType = column.getDataType();
      if ( column instanceof SqlPhysicalColumn ) {
        columnName = ( (SqlPhysicalColumn) column ).getTargetColumn().toLowerCase();
      } else {
        throw new UnsupportedModelException();
      }
      // else if column instanceof InlineEtlPhysicalColumn )
      assert dataType != null && columnName != null;
    }

    public boolean equals( Object obj ) {
      if ( this == obj ) {
        return true;
      } else if ( obj instanceof ColumnKey ) {
        return equals( (ColumnKey) obj );
      }
      return false;
    }

    public boolean equals( ColumnKey other ) {
      return this.dataType.equals( other.dataType )
          && this.columnName.equals( other.columnName );
    }

    public int hashCode() {
      return columnName.hashCode() + 31 * dataType.hashCode();
    }

    public String getColumnName() {
      return columnName;
    }
    public DataType getColumnDataType() {
      return dataType;
    }
  }

  private SqlPhysicalModel createPhysicalModel( String modelName, DatabaseMeta dbMeta, String schema, String table )
    throws PentahoMetadataException {
    SchemaTable schemaTable = new SchemaTable( schema, table );
    // generate a new physical domain
    AutoModeler autoModeler =
        new AutoModeler( LocalizedString.DEFAULT_LOCALE, modelName, dbMeta, new SchemaTable[] { schemaTable } );
    Domain newDomain = autoModeler.generateDomain();
    return (SqlPhysicalModel) newDomain.getPhysicalModels().get( 0 );
  }

  public static class ModelingException extends Exception {
    private static final long serialVersionUID = 1L;
  }

  /**
   * Presumption not met
   */
  public static class UnsupportedModelException extends ModelingException {
    private static final long serialVersionUID = 1L;
  }

  public static class ColumnMismatchException extends ModelingException {
    private static final long serialVersionUID = 1L;
    private ColumnKey columnKey;

    public ColumnMismatchException( ColumnKey unmatchedColumn ) {
      columnKey = unmatchedColumn;
    }

    public String getColumnName() {
      return columnKey.getColumnName();
    }
    public String getDataType() {
      return columnKey.getColumnDataType().getName();
    }
  }

  private static class RefineryValueMetaStrategy extends DataProviderHelper.OutputStepMappingAdapter
      implements PhysicalTableImporter.ImportStrategy {

    public RefineryValueMetaStrategy( final StepMetaDataCombi stepMetaDataCombi ) throws ModelerException {
      super( stepMetaDataCombi );
    }

    @Override
    public boolean shouldInclude( final ValueMetaInterface valueMeta ) {
      return insertRowMeta != null && insertRowMeta.exists( valueMeta );
    }

    @Override
    public String displayName( final ValueMetaInterface valueMeta ) {
      for ( int i = 0; i < fieldDatabase.size(); i++ ) {
        final String fieldName = fieldDatabase.get( i );
        if ( fieldName.equalsIgnoreCase( valueMeta.getName() ) ) {
          return fieldStream.get( i );
        }
      }
      ValueMetaInterface insertValueMeta = insertRowMeta.searchValueMeta( valueMeta.getName() );
      if ( insertValueMeta != null ) {
        return insertValueMeta.getName();
      }
      return valueMeta.getName();
    }
  }
}
