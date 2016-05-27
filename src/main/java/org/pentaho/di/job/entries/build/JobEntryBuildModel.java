/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.job.entries.build;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.agilebi.modeler.util.TableModelerSource;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.refinery.model.AnalysisModeler;
import org.pentaho.di.core.refinery.model.DswModeler;
import org.pentaho.di.core.refinery.model.DswModeler.ColumnMismatchException;
import org.pentaho.di.core.refinery.model.DswModeler.UnsupportedModelException;
import org.pentaho.di.core.refinery.model.ModelServerFetcher;
import org.pentaho.di.core.refinery.model.ModelServerFetcher.AuthorizationException;
import org.pentaho.di.core.refinery.model.ModelServerFetcher.ServerException;
import org.pentaho.di.core.refinery.model.RefineryValueMetaStrategy;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.dataservice.DataServiceContext;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.di.trans.util.TransUtil;
import org.pentaho.di.ui.job.entries.build.JobEntryBuildModelDialog;
import org.pentaho.di.ui.job.entries.common.ConnectionValidator;
import org.pentaho.metadata.automodel.PhysicalTableImporter;
import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.util.XmiParser;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.api.exceptions.MetaStoreException;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.pentaho.di.core.Const.nullToEmpty;
import static org.pentaho.platform.util.StringUtil.isEmpty;

@org.pentaho.di.core.annotations.JobEntry( id = JobEntryBuildModel.PLUGIN_ID,
    categoryDescription = "JobCategory.Category.Modeling", i18nPackageName = "org.pentaho.di.job.entries.build",
    documentationUrl = "0N0/060/0B0/020/0A0", image = "model_entry.svg",
    name = "BuildModelJob.Name", description = "BuildModelJob.Description" )
public class JobEntryBuildModel extends JobEntryBase implements JobEntryInterface, Cloneable {

  public static final String PLUGIN_ID = "DataRefineryBuildModel";
  public static final String KEY_MODEL_ANNOTATIONS = "KEY_MODEL_ANNOTATIONS";
  public static final String KEY_OUTPUT_STEP_PREFIX = "JobEntryBuildModel.OutputStep.";
  private static Class<?> PKG = JobEntryBuildModel.class; // for i18n purposes, needed by Translator2!!

  private DswModeler modeler;

  /* serializable */
  private String outputStep;
  private String modelName;
  private boolean useExistingModel;
  private String existingModel;

  private boolean createOnPublish;
  private String selectedModel;
  private BiServerConnection biServerConnection;
  private DataServiceContext dataServiceContext;

  public boolean useExistingModel() {
    return useExistingModel;
  }

  public String getExistingModel() {
    return existingModel;
  }

  public void setUseExistingModel( final boolean useExistingModel ) {
    this.useExistingModel = useExistingModel;
  }

  public void setExistingModel( final String existingModel ) {
    this.existingModel = existingModel;
  }

  public void setDataServiceContext( DataServiceContext dataServiceContext ) {
    this.dataServiceContext = dataServiceContext;
  }

  /**
   *
   */
  public final class Fields {
    public static final String NAME = "name";
    public static final String OUTPUT_STEP = "outputStep";
    public static final String MODEL_NAME = "modelName";
    public static final String USE_EXISTING_MODEL = "useExistingModel";
    public static final String EXISTING_MODEL = "existingModel";

    public static final String BASERVER_URL = "ba_server_url";
    public static final String BASERVER_USERID = "ba_server_user_id";
    public static final String BASERVER_PASSWORD = "ba_server_password";
    public static final String SELECTED_MODEL = "selected_model";
    public static final String CREATE_ON_PUBLISH = "create_on_publish";
  }

  public JobEntryBuildModel() {
    super();
  }

  public JobEntryBuildModel( String name, String description ) {
    super( name, description );
  }

  @Override public void setParentJob( final Job parentJob ) {
    super.setParentJob( parentJob );
    if ( log == null ) {
      log = new LogChannel( this );
    }
    modeler.setLog( log );
  }

  public DswModeler getModeler() {
    return modeler;
  }

  public void setModeler( DswModeler modeler ) {
    this.modeler = modeler;
    this.modeler.setLog( this.log );
  }

  /**
   * Returns output steps that can be picked for UI.
   * 
   * @param jobMeta
   * @return
   */
  public String[] getOutputStepList( JobMeta jobMeta ) {
    ArrayList<String> stepNames = new ArrayList<String>();
    try {
      for ( JobEntryCopy copy : jobMeta.getJobCopies() ) {
        if ( !( copy.getEntry() instanceof JobEntryTrans ) ) {
          continue;
        }

        if ( !jobMeta.isPathExist( copy.getEntry(), this ) ) {
          continue;
        }
        JobEntryTrans trans = (JobEntryTrans) copy.getEntry();

        TransMeta transMeta = trans.getTransMeta( jobMeta.getRepository(), jobMeta.getMetaStore(), jobMeta );
        stepNames.addAll( TransUtil.collectOutputStepInTrans( transMeta, getRepository(), getMetaStore() ).keySet() );
        stepNames.addAll( dataServiceContext.getMetaStoreUtil().getDataServiceNames( transMeta ) );
      }
    } catch ( Exception e ) {
      // UI aid, not a problem if it fails at this point
    }
    return stepNames.toArray( new String[stepNames.size()] );
  }

  /**
   * Searches for tags on output steps to determine physical layer. Then, auto-models to generate the logical layer.
   * This method can be called during design time or run time.
   * 
   * @param jobMeta
   * @return
   * @throws KettleException
   */
  public String buildXmi( JobMeta jobMeta, String outputStep, final String modelName ) throws KettleException {

    if ( StringUtils.isEmpty( outputStep ) ) {
      throw new KettleException( this.getMsg( "BuildModelJob.Missing.OutputStep" ) );
    }

    if ( StringUtils.isEmpty( modelName ) ) {
      throw new KettleException( this.getMsg( "BuildModelJob.Missing.ModelName" ) );
    }

    DatabaseMeta dbMeta = getConnectionInfo().getDatabaseMeta();
    String schemaName = StringUtils.defaultIfBlank( environmentSubstitute( getConnectionInfo().getSchemaName() ), "" );
    String tableName = environmentSubstitute( getConnectionInfo().getTableName() );

    TableModelerSource source = new TableModelerSource( dbMeta, tableName, schemaName ); //$NON-NLS-1$
    source.setSchemaName( StringUtils.defaultIfBlank( source.getSchemaName(), "" ) );
    try {
      Domain modeledDomain;
      PhysicalTableImporter.ImportStrategy importStrategy = getImportStrategy();

      final ModelAnnotationGroup modelAnnotations = getModelAnnotations();

      if ( useExistingModel() ) {
        String existingModelId = environmentSubstitute( getSelectedModel() );
        ModelServerFetcher fetcher = getModelServerFetcher();
        // model can be created/deleted in between checking and fetching, but there is no sure way to tell
        // if a model doesn't exist or some other error occurred
        if ( !modelExists( existingModelId, fetcher ) ) {
          if ( isCreateOnPublish() ) {
            logBasic( getMsg( "BuildModelJob.Info.ModelNotFound", existingModelId ) );
            modeledDomain =
                getDswModeler()
                    .createModel( modelName, source, dbMeta, importStrategy, modelAnnotations, getMetaStore() );
          } else {
            if ( Const.isEmpty( existingModelId ) ) {
              throw new KettleException( getMsg( "BuildModelJob.Error.ModelNullNotFound", getName() ) );
            } else {
              throw new KettleException( getMsg( "BuildModelJob.Error.ModelNotFound", existingModelId ) );
            }
          }
        } else {
          final Domain templateModel = fetcher.downloadDswFile( existingModelId );
          modeledDomain = getDswModeler().updateModel( modelName, templateModel, dbMeta, schemaName, tableName );
        }
      } else {
        modeledDomain =
            getDswModeler()
                .createModel( modelName, source, dbMeta, importStrategy, modelAnnotations, getMetaStore() );
      }
      XmiParser parser = new XmiParser();
      String localXmi = parser.generateXmi( modeledDomain );
      return localXmi;
    } catch ( AuthorizationException e ) {
      throw new KettleException( getMsg( "BuildModelJob.Error.Authorization" ) );
    } catch ( ServerException e ) {
      throw new KettleException( getMsg( "BuildModelJob.Error.ErrorFetchingModel" ) );
    } catch ( ColumnMismatchException e ) {
      throw new KettleException( getMsg( "BuildModelJob.Error.CannotUpdateModel", getMsg(
          "BuildModelJob.Error.UnmatchedColumn", e.getColumnName(), e.getDataType() ) ) );
    } catch ( UnsupportedModelException e ) {
      throw new KettleException( getMsg( "BuildModelJob.Error.CannotUpdateModel",
          getMsg( "BuildModelJob.Error.UnsupportedModel" ) ) );
    } catch ( ModelerException e ) {
      if ( isOutputStepADataService() ) {
        throw new KettleException( getMsg( "BuildModelJob.Error.DataServiceProblem" ) );
      } else {
        throw new KettleException( e );
      }
    } catch ( Exception e ) {
      throw new KettleException( e );
    }
  }

  private ModelAnnotationGroup getModelAnnotations() {
    Object modelAnnotationGroup = this.getParentJob().getExtensionDataMap().get( KEY_MODEL_ANNOTATIONS );
    if ( modelAnnotationGroup != null && modelAnnotationGroup instanceof ModelAnnotationGroup ) {
      return (ModelAnnotationGroup) modelAnnotationGroup;
    }
    return new ModelAnnotationGroup();
  }

  PhysicalTableImporter.ImportStrategy getImportStrategy() throws KettleException, ModelerException {
    StepMetaDataCombi stepMetaDataCombi = getStepMetaDataCombi();
    if ( stepMetaDataCombi != null ) {
      return new RefineryValueMetaStrategy( stepMetaDataCombi );
    }
    return PhysicalTableImporter.defaultImportStrategy;
  }

  StepMetaDataCombi getStepMetaDataCombi() {
    return (StepMetaDataCombi) this.getParentJob().getExtensionDataMap().get( KEY_OUTPUT_STEP_PREFIX + getName() );
  }

  private List<TransMeta> findAllTransInJob() throws KettleException {
    List<JobEntryCopy> jobCopies = getParentJobCopies();
    List<TransMeta> transMetas = new ArrayList<>();
    for ( JobEntryCopy jobCopy : jobCopies ) {
      if ( jobCopy.isTransformation() ) {
        JobEntryTrans entry = (JobEntryTrans) jobCopy.getEntry();
        try {
          transMetas.add( entry.getTransMeta( getRepository(), getMetaStore(), getVariables() ) );
        } catch ( Exception e ) {
          log.logDebug( getMsg( "BuildModelJob.Debug.BadTrans", jobCopy.getName() ), e );
        }
      }
    }
    return transMetas;
  }

  List<JobEntryCopy> getParentJobCopies() {
    return getParentJob().getJobMeta().getJobCopies();
  }

  boolean isOutputStepADataService() throws KettleException {
    List<TransMeta> transMetas = findAllTransInJob();
    try {
      for ( TransMeta transMeta : transMetas ) {
        if ( dataServiceContext.getMetaStoreUtil().getDataServiceNames( transMeta ).contains( getOutputStep() ) ) {
          return true;
        }
      }
    } catch ( MetaStoreException e ) {
      return false;
    }
    return false;
  }

  public ProvidesDatabaseConnectionInformation getConnectionInfo() throws KettleException {
    StepMetaDataCombi stepMetaDataCombi = getStepMetaDataCombi();
    String sourceName = environmentSubstitute( getOutputStep() );
    if ( stepMetaDataCombi == null ) {
      if ( isOutputStepADataService() ) {
        return new DataServiceConnectionInformation( getOutputStep(), getRepository(), log );
      }
      throw new KettleException( this.getMsg( isEmpty( sourceName )
        ? "BuildModelJob.Error.SourceUndefined" : "BuildModelJob.Error.UnableToFindStep", sourceName ) );
    }
    if ( ProvidesDatabaseConnectionInformation.class.isAssignableFrom( stepMetaDataCombi.meta.getClass() ) ) {
      return ProvidesDatabaseConnectionInformation.class.cast( stepMetaDataCombi.meta );
    }
    throw new KettleException( this.getMsg( "BuildModelJob.Error.NoConnectionInfo", sourceName ) );
  }

  protected BiServerConnection validBIServerConnection() throws KettleException {
    BiServerConnection connection = environmentSubstitute( getBiServerConnection() );
    // Fail early if invalid Pentaho BA Server, Unauthenticated user, Missing permissions, etc.
    // Prevent Spoon from displaying user/password prompt.
    ConnectionValidator validator = getConnectionValidator( connection );
    validator.validateConnectionInRuntime(); // throw exception if not valid connection
    return connection;
  }

  protected ConnectionValidator getConnectionValidator( BiServerConnection connection ) {
    ConnectionValidator validator = new ConnectionValidator();
    validator.setConnection( connection );
    return validator;
  }

  protected boolean modelExists( String modelId, ModelServerFetcher fetcher ) throws KettleException,
    AuthorizationException, ServerException {
    HashSet<String> allIds = new HashSet<String>();
    allIds.addAll( fetcher.fetchAnalysisList() );
    allIds.addAll( fetcher.fetchDswList() );
    return allIds.contains( modelId );
  }

  public BiServerConnection environmentSubstitute( BiServerConnection conn ) {
    if ( conn == null ) {
      return null;
    }
    BiServerConnection substituted = new BiServerConnection();
    // BiServerConnection always adds a trailing '/' if not there.. including vars
    String url = environmentSubstitute( conn.getUrl() );
    if ( StringUtils.endsWith( url, "//" ) ) {
      url = StringUtils.chop( url );
    }
    substituted.setUrl( url );
    substituted.setUserId( environmentSubstitute( conn.getUserId() ) );
    substituted.setPassword( environmentSubstitute( conn.getPassword() ) );
    return substituted;
  }

  @Override
  public Result execute( Result result, int nr ) throws KettleException {

    String outputStep = environmentSubstitute( getOutputStep() );
    String modelName = environmentSubstitute( getModelName() );

    try {

      setVarAndLogBasic( "JobEntryBuildModel.DatabaseConnection." + modelName, getConnectionInfo().getDatabaseMeta()
          .getName() );

      if ( isPublishAnalysis() ) {
        setVarAndLogDebug( "JobEntryBuildModel.Mondrian.Schema." + modelName, buildAnalysis( modelName ) );
        setVarAndLogBasic( "JobEntryBuildModel.Mondrian.Datasource." + modelName, getConnectionInfo().getDatabaseMeta()
            .getName() );
      } else {
        setVarAndLogDebug( "JobEntryBuildModel.XMI." + modelName, buildXmi( getParentJob().getJobMeta(), outputStep,
            modelName ) );
        setVarAndLogBasic( "JobEntryBuildModel.XMI.DSW." + modelName, "true" );
      }

      result.setResult( true );
    } catch ( Exception e ) {
      log.logError( e.getMessage(), e );
      result.setResult( false );
      result.setNrErrors( 1 );
    }

    return result;
  }

  String buildAnalysis( final String modelName ) throws KettleException {
    String analysisFile;
    String selectedModelName = environmentSubstitute( getSelectedModel() );

    try {
      if ( modelExists( selectedModelName, getModelServerFetcher() ) ) {
        analysisFile = getModelServerFetcher().downloadAnalysisFile( selectedModelName );
      } else {
        if ( Const.isEmpty( selectedModelName ) ) {
          throw new KettleException( getMsg( "BuildModelJob.Error.ModelNullNotFound", getName() ) );
        } else {
          throw new KettleException( getMsg( "BuildModelJob.Error.ModelNotFound", selectedModelName ) );
        }
      }
    } catch ( Exception e ) {
      throw new KettleException( e );
    }
    AnalysisModeler analysisModeler = getAnalysisModeler();
    return analysisModeler.replaceTableAndSchemaNames( analysisFile, modelName );
  }

  AnalysisModeler getAnalysisModeler() throws KettleException {
    return new AnalysisModeler( this, getConnectionInfo() );
  }

  ModelServerFetcher getModelServerFetcher() throws KettleException {
    return new ModelServerFetcher( validBIServerConnection() );
  }

  protected void setVarAndLogDebug( final String varName, final String value ) {
    parentJob.setVariable( varName, value );
    log.logDebug( getMsg( "BuildModelJob.SetVariable", varName, parentJob.getVariable( varName ) ) );
  }

  protected void setVarAndLogBasic( final String varName, final String value ) {
    parentJob.setVariable( varName, value );
    log.logBasic( getMsg( "BuildModelJob.SetVariable", varName, parentJob.getVariable( varName ) ) );
  }

  protected boolean isPublishAnalysis() {
    return useExistingModel() && !StringUtils.isBlank( getSelectedModel() )
        && !environmentSubstitute( getSelectedModel() ).endsWith( ".xmi" );
  }

  @Override
  public String getDialogClassName() {
    return JobEntryBuildModelDialog.class.getCanonicalName();
  }

  public String getOutputStep() {
    return outputStep;
  }

  public void setOutputStep( String outputStep ) {
    this.outputStep = outputStep;
  }

  @Override
  public String getXML() {
    StringBuffer retval = new StringBuffer( 100 );
    retval.append( super.getXML() );
    retval.append( "      " ).append( XMLHandler.addTagValue( Fields.OUTPUT_STEP, getOutputStep() ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( Fields.MODEL_NAME, getModelName() ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( Fields.USE_EXISTING_MODEL, useExistingModel() ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( Fields.EXISTING_MODEL, getExistingModel() ) );

    if ( biServerConnection != null ) {
      // Encrypt password
      String password = Encr.encryptPasswordIfNotUsingVariables( biServerConnection.getPassword() );
      retval.append( "      " ).append( XMLHandler.addTagValue( Fields.BASERVER_PASSWORD, nullToEmpty( password ) ) );

      retval.append( "      " ).append(
          XMLHandler.addTagValue( Fields.BASERVER_URL, nullToEmpty( biServerConnection.getUrl() ) ) );
      retval.append( "      " ).append(
          XMLHandler.addTagValue( Fields.BASERVER_USERID, nullToEmpty( biServerConnection.getUserId() ) ) );
    }
    retval.append( "      " ).append( XMLHandler.addTagValue( Fields.SELECTED_MODEL, getSelectedModel() ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( Fields.CREATE_ON_PUBLISH, isCreateOnPublish() ) );
    return retval.toString();
  }

  public void loadXML( Node entrynode, List<DatabaseMeta> databases, List<SlaveServer> slaveServers, Repository rep,
      IMetaStore metaStore ) throws KettleXMLException {
    super.loadXML( entrynode, databases, slaveServers );
    setOutputStep( XMLHandler.getTagValue( entrynode, Fields.OUTPUT_STEP ) );
    setModelName( XMLHandler.getTagValue( entrynode, Fields.MODEL_NAME ) );
    setUseExistingModel( "Y".equals( XMLHandler.getTagValue( entrynode, Fields.USE_EXISTING_MODEL ) ) );
    setExistingModel( XMLHandler.getTagValue( entrynode, Fields.EXISTING_MODEL ) );

    BiServerConnection biServer = new BiServerConnection();
    biServer.setUrl( nullToEmpty( XMLHandler.getTagValue( entrynode, Fields.BASERVER_URL ) ) );
    biServer.setUserId( nullToEmpty( XMLHandler.getTagValue( entrynode, Fields.BASERVER_USERID ) ) );

    // Decrypt
    String password =
        Encr.decryptPasswordOptionallyEncrypted( XMLHandler.getTagValue( entrynode, Fields.BASERVER_PASSWORD ) );
    biServer.setPassword( nullToEmpty( password ) );
    setBiServerConnection( biServer );

    setSelectedModel( XMLHandler.getTagValue( entrynode, Fields.SELECTED_MODEL ) );
    setCreateOnPublish( BooleanUtils.toBoolean( XMLHandler.getTagValue( entrynode, Fields.CREATE_ON_PUBLISH ) ) );
  }

  @Override
  public void loadRep( Repository rep, IMetaStore metaStore, ObjectId id_jobentry, List<DatabaseMeta> databases,
      List<SlaveServer> slaveServers ) throws KettleException {
    super.loadRep( rep, metaStore, id_jobentry, databases, slaveServers );
    setOutputStep( rep.getJobEntryAttributeString( id_jobentry, Fields.OUTPUT_STEP ) );
    setModelName( rep.getJobEntryAttributeString( id_jobentry, Fields.MODEL_NAME ) );
    setUseExistingModel( rep.getJobEntryAttributeBoolean( id_jobentry, Fields.USE_EXISTING_MODEL ) );
    setExistingModel( rep.getJobEntryAttributeString( id_jobentry, Fields.EXISTING_MODEL ) );

    BiServerConnection biServerModel = new BiServerConnection();
    // Decrypt
    String password =
        Encr.decryptPasswordOptionallyEncrypted( rep
            .getJobEntryAttributeString( id_jobentry, Fields.BASERVER_PASSWORD ) );
    biServerModel.setPassword( nullToEmpty( password ) );

    biServerModel.setUrl( nullToEmpty( rep.getJobEntryAttributeString( id_jobentry, Fields.BASERVER_URL ) ) );
    biServerModel.setUserId( nullToEmpty( rep.getJobEntryAttributeString( id_jobentry, Fields.BASERVER_USERID ) ) );
    setBiServerConnection( biServerModel );

    setSelectedModel( rep.getJobEntryAttributeString( id_jobentry, Fields.SELECTED_MODEL ) );
    setCreateOnPublish( BooleanUtils
        .toBoolean( rep.getJobEntryAttributeString( id_jobentry, Fields.CREATE_ON_PUBLISH ) ) );
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_job ) throws KettleException {
    rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.OUTPUT_STEP, getOutputStep() );
    rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.MODEL_NAME, getModelName() );
    rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.USE_EXISTING_MODEL, useExistingModel() );
    rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.EXISTING_MODEL, getExistingModel() );

    if ( biServerConnection != null ) {
      // Encrypt password
      String password = Encr.encryptPasswordIfNotUsingVariables( biServerConnection.getPassword() );
      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.BASERVER_PASSWORD, nullToEmpty( password ) );

      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.BASERVER_URL, nullToEmpty( biServerConnection.getUrl() ) );
      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.BASERVER_USERID, nullToEmpty( biServerConnection
          .getUserId() ) );
    }
    rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.SELECTED_MODEL, getSelectedModel() );
    rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.CREATE_ON_PUBLISH, isCreateOnPublish() );
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName( String modelName ) {
    this.modelName = modelName;
  }

  public BiServerConnection getBiServerConnection() {
    return biServerConnection;
  }

  public void setBiServerConnection( BiServerConnection biServerConnection ) {
    this.biServerConnection = biServerConnection;
  }

  public String getSelectedModel() {
    return selectedModel;
  }

  public void setSelectedModel( String selectedModel ) {
    this.selectedModel = selectedModel;
  }

  public boolean isCreateOnPublish() {
    return createOnPublish;
  }

  public void setCreateOnPublish( boolean createOnPublish ) {
    this.createOnPublish = createOnPublish;
  }

  private String getMsg( String key, String... parameters ) {
    return BaseMessages.getString( PKG, key, parameters );
  }

  public boolean evaluates() {
    return true;
  }

  protected DswModeler getDswModeler() {
    return modeler;
  }
}
