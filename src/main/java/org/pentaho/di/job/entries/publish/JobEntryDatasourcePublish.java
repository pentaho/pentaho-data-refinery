/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2023 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.job.entries.publish;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.pentaho.database.model.DatabaseAccessType;
import org.pentaho.database.model.DatabaseConnection;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.agilebi.ModelServerPublish;
import org.pentaho.di.core.refinery.publish.model.DataSourceAclModel;
import org.pentaho.di.core.refinery.publish.model.DataSourcePublishModel;
import org.pentaho.di.core.refinery.publish.util.PublishRestUtil;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.build.JobEntryBuildModel;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.metastore.MetaStoreConst;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.ui.job.entries.common.ConnectionValidator;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

import java.util.List;

@org.pentaho.di.core.annotations.JobEntry( id = "DATASOURCE_PUBLISH",
  i18nPackageName = "org.pentaho.di.job.entries.publish", image = "publish.svg",
  name = "JobEntryDatasourcePublish.JobName", description = "JobEntryDatasourcePublish.JobDescription",
  documentationUrl = "Work_with_data/Using_the_Publish_Model_job_entry_for_SDR",
  categoryDescription = "JobCategory.Category.Modeling" )
public class JobEntryDatasourcePublish extends JobEntryBase implements Cloneable, JobEntryInterface {

  private static Class<?> PKG = JobEntryDatasourcePublish.class; // for i18n purposes, needed by Translator2!!

  private DataSourcePublishModel dataSourcePublishModel;
  private DatasourcePublishService datasourcePublishService;

  public final class Fields {
    public static final String LOGICAL_MODEL = "logical_model";
    public static final String OVERRIDE = "override";
    public static final String BASERVER_URL = "ba_server_url";
    public static final String BASERVER_NAME = "ba_server_name";
    public static final String BASERVER_USERID = "ba_server_user_id";
    public static final String BASERVER_PASSWORD = "ba_server_password";
    public static final String ACL_ACCESS_TYPE = "acl_access_type";
    public static final String ACL_USER_OR_ROLE = "acl_user_or_role";
  }

  public JobEntryDatasourcePublish( DatasourcePublishService datasourcePublishService ) {
    this.datasourcePublishService = datasourcePublishService;
  }

  public DataSourcePublishModel getDataSourcePublishModel() {
    return dataSourcePublishModel;
  }

  public void setDataSourcePublishModel( DataSourcePublishModel dataSourcePublishModel ) {
    this.dataSourcePublishModel = dataSourcePublishModel;
  }

  @Override
  public Result execute( Result result, int i ) throws KettleException {
    boolean dsPublished, metaPublished;
    dsPublished = metaPublished = false;
    DatabaseMeta databaseMeta = null;
    ModelServerPublish modelServerPublish = null;
    String dswFlag = null;
    String modelName = null;
    try {
      BiServerConnection biServerModel = dataSourcePublishModel.getBiServerConnection();

      // Resolve parametized values before execution
      biServerModel.setName( environmentSubstitute( biServerModel.getName() ) );
      biServerModel.setUserId( environmentSubstitute( biServerModel.getUserId() ) );
      biServerModel.setPassword( environmentSubstitute( biServerModel.getPassword() ) );
      String url = environmentSubstitute( biServerModel.getUrl() );
      if ( url != null && url.endsWith( "//" ) ) {
        url = url.substring( 0, url.length() - 1 );
      }
      biServerModel.setUrl( url );
      // Fail early if invalid Pentaho BA Server or Unauthenticated user.
      // Prevent Spoon from displaying user/password prompt.
      ConnectionValidator validator = getConnectionValidator( biServerModel );
      validator.validateConnectionInRuntime();

      BiServerConnection connection = new BiServerConnection();
      connection.setName( biServerModel.getName() );
      connection.setUrl( biServerModel.getUrl() );
      connection.setPassword( biServerModel.getPassword() );
      connection.setUserId( biServerModel.getUserId() );
      log.logBasic( getMsg( "JobEntryDatasourcePublish.Publish.BAServer", biServerModel.getUrl() ) );

      modelServerPublish = getModelServerPublish();
      modelServerPublish.setBiServerConnection( connection );

      boolean forceOverride = dataSourcePublishModel.isOverride();

      DataSourceAclModel datasourceAcl = new DataSourceAclModel();
      String accessType =
        Const.isEmpty( dataSourcePublishModel.getAccessType() ) ? DataSourcePublishModel.ACCESS_TYPE_EVERYONE
          : environmentSubstitute( dataSourcePublishModel.getAccessType() ).toLowerCase();
      String userOrRole = environmentSubstitute( dataSourcePublishModel.getUserOrRole() );
      if ( DataSourcePublishModel.ACCESS_TYPE_ROLE.equals( accessType ) ) {
        if ( StringUtils.isBlank( userOrRole ) ) {
          throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Error.MissingRoleMsg" ) );
        }
        datasourceAcl.addRole( userOrRole );
      } else if ( DataSourcePublishModel.ACCESS_TYPE_USER.equals( accessType ) ) {
        if ( StringUtils.isBlank( userOrRole ) ) {
          throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Error.MissingUserMsg" ) );
        }
        datasourceAcl.addUser( userOrRole );
      } else if ( !DataSourcePublishModel.ACCESS_TYPE_EVERYONE.equals( accessType ) ) {
        throw new KettleException( "Access Type '" + accessType + "' not recognized" );
      }
      modelServerPublish.setAclModel( datasourceAcl );

      modelName = getModelName();
      log.logBasic( this.getMsg( "JobEntryDatasourcePublish.Publish.Model", modelName ) );

      // We support publishing whatever is available to the publish job entry.. so if a build model job entry
      // feeds this job entry, then we'll be publishing a DB connection and a DSW DS. If a custom set variables
      // job entry precedes this, then we publish whatever is set by this job entry.

      // Publish Database Meta
      databaseMeta = discoverDatabaseMeta( getParentJob().getJobMeta() );
      if ( databaseMeta != null ) {
        // Cannot publish JNDI data sources at this time, we don't know if BIServer has access to it
        if ( DatabaseAccessType.values()[ databaseMeta.getAccessType() ] == DatabaseAccessType.JNDI ) {
          throw new KettleException(
            this.getMsg( "JobEntryDatasourcePublish.Error.JNDIDatasource", databaseMeta.getName() ) );
        }

        // check overwrite condition
        DatabaseConnection dbConnection = modelServerPublish.connectionNameExists( databaseMeta.getName() );
        if ( dbConnection != null && !dataSourcePublishModel.isOverride() ) {
          throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Error.DBConnectionExists" ) );
        }

        datasourcePublishService.publishDatabaseMeta( modelServerPublish, databaseMeta, forceOverride );
      } else {
        throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Error.UnableToFindDBConnection" ) );
      }
      dsPublished = true;

      // Publish Metadata XMI
      dswFlag = getParentJob().getVariable( "JobEntryBuildModel.XMI.DSW." + modelName );
      log.logBasic( getMsg( "JobEntryDatasourcePublish.Publish.ReadVariable", "JobEntryBuildModel.XMI.DSW."
        + modelName, dswFlag ) );
      String xmiString = getParentJob().getVariable( "JobEntryBuildModel.XMI." + modelName );
      log.logDetailed(
        getMsg( "JobEntryDatasourcePublish.Publish.ReadVariable", "JobEntryBuildModel.XMI." + modelName, xmiString ) );
      if ( dswFlag != null && dswFlag.equalsIgnoreCase( "true" ) ) {
        datasourcePublishService.publishDswXmi( modelName, xmiString, modelServerPublish, forceOverride );
      } else {
        datasourcePublishService.publishMetadataXmi( modelName, xmiString, modelServerPublish, forceOverride );
      }
      metaPublished = true;

      // Publish Mondrian Schema
      String mondrianSchema = getParentJob().getVariable( "JobEntryBuildModel.Mondrian.Schema." + modelName );
      log.logDetailed( getMsg( "JobEntryDatasourcePublish.Publish.ReadVariable", "JobEntryBuildModel.Mondrian.Schema."
        + modelName, mondrianSchema ) );
      String mondrianDatasource = getParentJob().getVariable( "JobEntryBuildModel.Mondrian.Datasource." + modelName );
      log.logBasic( getMsg( "JobEntryDatasourcePublish.Publish.ReadVariable", "JobEntryBuildModel.Mondrian.Datasource."
        + modelName, mondrianDatasource ) );

      datasourcePublishService
        .publishMondrianSchema( modelName, mondrianSchema, mondrianDatasource, modelServerPublish, forceOverride );

      result.setResult( true );

    } catch ( KettleException e ) {
      logBasic( this.getMsg( "JobEntryDatasourcePublish.Rollback" ) );
      if ( dsPublished && databaseMeta != null ) {
        datasourcePublishService.deleteDatabaseMeta( modelServerPublish, databaseMeta );
      }
      if ( metaPublished && modelName != null ) {
        datasourcePublishService.deleteXMI( modelServerPublish, modelName, dswFlag );
      }
      logError( e.getMessage(), e );
      result.setResult( false );
      result.setNrErrors( 1 );
    }

    return result;
  }

  @Override
  public String getXML() {

    StringBuilder xml = new StringBuilder();
    xml.append( super.getXML() );

    DataSourcePublishModel model = getDataSourcePublishModel();
    if ( model != null ) {
      xml.append( "      " ).append( XMLHandler.addTagValue( Fields.LOGICAL_MODEL, model.getModelName() ) );
      xml.append( "      " ).append( XMLHandler.addTagValue( Fields.OVERRIDE, model.isOverride() ) );
      xml.append( "      " ).append( XMLHandler.addTagValue( Fields.ACL_ACCESS_TYPE, model.getAccessType() ) );
      xml.append( "      " ).append( XMLHandler.addTagValue( Fields.ACL_USER_OR_ROLE, model.getUserOrRole() ) );

      BiServerConnection biServerConnection = model.getBiServerConnection();
      if ( biServerConnection != null ) {
        xml.append( "      " ).append(
          XMLHandler.addTagValue( Fields.BASERVER_NAME, Const.nullToEmpty( biServerConnection.getName() ) ) );

        // Encrypt password
        String password = Encr.encryptPasswordIfNotUsingVariables( biServerConnection.getPassword() );
        xml.append( "      " )
          .append( XMLHandler.addTagValue( Fields.BASERVER_PASSWORD, Const.nullToEmpty( password ) ) );

        xml.append( "      " ).append( XMLHandler.addTagValue(
          Fields.BASERVER_URL, Const.nullToEmpty( biServerConnection.getUrl() ) ) );
        xml.append( "      " ).append(
          XMLHandler.addTagValue( Fields.BASERVER_USERID, Const.nullToEmpty( biServerConnection.getUserId() ) ) );
      }
    }

    return xml.toString();
  }

  @Override
  public void loadXML( Node entrynode, List<DatabaseMeta> databases, List<SlaveServer> slaveServers, Repository rep,
                       IMetaStore metaStore ) throws KettleXMLException {

    super.loadXML( entrynode, databases, slaveServers );
    DataSourcePublishModel model = new DataSourcePublishModel();

    BiServerConnection biServerModel = new BiServerConnection();
    biServerModel.setName( Const.nullToEmpty( XMLHandler.getTagValue( entrynode, Fields.BASERVER_NAME ) ) );
    biServerModel.setUrl( Const.nullToEmpty( XMLHandler.getTagValue( entrynode, Fields.BASERVER_URL ) ) );
    biServerModel.setUserId( Const.nullToEmpty( XMLHandler.getTagValue( entrynode, Fields.BASERVER_USERID ) ) );

    // Decrypt
    String password =
      Encr.decryptPasswordOptionallyEncrypted(
        Const.nullToEmpty( XMLHandler.getTagValue( entrynode, Fields.BASERVER_PASSWORD ) ) );
    biServerModel.setPassword( password );

    model.setBiServerConnection( biServerModel );

    model.setOverride( BooleanUtils.toBoolean( XMLHandler.getTagValue( entrynode, Fields.OVERRIDE ) ) );
    model.setAccessType( XMLHandler.getTagValue( entrynode, Fields.ACL_ACCESS_TYPE ) );
    model.setUserOrRole( XMLHandler.getTagValue( entrynode, Fields.ACL_USER_OR_ROLE ) );

    model.setModelName( XMLHandler.getTagValue( entrynode, Fields.LOGICAL_MODEL ) );

    setDataSourcePublishModel( model );
  }

  @Override
  public void loadRep( Repository rep, IMetaStore metaStore, ObjectId id_jobentry, List<DatabaseMeta> databases,
                       List<SlaveServer> slaveServers ) throws KettleException {
    super.loadRep( rep, metaStore, id_jobentry, databases, slaveServers );
    BiServerConnection biServerModel = new BiServerConnection();
    biServerModel.setName( rep.getJobEntryAttributeString( id_jobentry, Fields.BASERVER_NAME ) );

    // Decrypt
    String password =
      Encr.decryptPasswordOptionallyEncrypted( rep
        .getJobEntryAttributeString( id_jobentry, Fields.BASERVER_PASSWORD ) );
    biServerModel.setPassword( Const.nullToEmpty( password ) );

    biServerModel.setUrl( Const.nullToEmpty( rep.getJobEntryAttributeString( id_jobentry, Fields.BASERVER_URL ) ) );
    biServerModel
      .setUserId( Const.nullToEmpty( rep.getJobEntryAttributeString( id_jobentry, Fields.BASERVER_USERID ) ) );

    DataSourcePublishModel dsModel = new DataSourcePublishModel();
    dsModel.setModelName( rep.getJobEntryAttributeString( id_jobentry, Fields.LOGICAL_MODEL ) );
    dsModel.setOverride( rep.getJobEntryAttributeBoolean( id_jobentry, Fields.OVERRIDE ) );
    dsModel.setAccessType( rep.getJobEntryAttributeString( id_jobentry, Fields.ACL_ACCESS_TYPE ) );
    dsModel.setUserOrRole( rep.getJobEntryAttributeString( id_jobentry, Fields.ACL_USER_OR_ROLE ) );
    dsModel.setBiServerConnection( biServerModel );

    setDataSourcePublishModel( dsModel );
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_job ) throws KettleException {

    if ( dataSourcePublishModel != null ) {
      BiServerConnection biServerConnection = dataSourcePublishModel.getBiServerConnection();
      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.BASERVER_NAME,
        Const.nullToEmpty( biServerConnection.getName() ) );
      // Encrypt password
      String password = Encr.encryptPasswordIfNotUsingVariables( biServerConnection.getPassword() );
      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.BASERVER_PASSWORD, Const.nullToEmpty( password ) );
      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.BASERVER_URL,
        Const.nullToEmpty( biServerConnection.getUrl() ) );
      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.BASERVER_USERID, Const.nullToEmpty( biServerConnection
        .getUserId() ) );
      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.LOGICAL_MODEL, dataSourcePublishModel.getModelName() );
      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.OVERRIDE, dataSourcePublishModel.isOverride() );
      rep
        .saveJobEntryAttribute( id_job, getObjectId(), Fields.ACL_ACCESS_TYPE, dataSourcePublishModel.getAccessType() );
      rep.saveJobEntryAttribute( id_job, getObjectId(), Fields.ACL_USER_OR_ROLE,
        dataSourcePublishModel.getUserOrRole() );
    }

  }

  // Allows dependency injection/mocks
  protected ModelServerPublish getModelServerPublish() {
    return new ModelServerPublish( getLogChannel() );
  }

  // Allows dependency injection/mocks
  protected PublishRestUtil getPublishRestUtil() {
    return new PublishRestUtil();
  }

  // Allows dependency injection/mocks
  protected ConnectionValidator getConnectionValidator( BiServerConnection connection ) {
    ConnectionValidator validator = new ConnectionValidator();
    validator.setConnection( connection );
    return validator;
  }

  /**
   * @return Runtime substituted model name. May be parameterized with variables/params.
   */
  private String getModelName() throws KettleException {

    String modelName = dataSourcePublishModel.getModelName();
    if ( StringUtils.isBlank( modelName ) ) {
      // Model not specified in publish job entry so see if we can discover it from
      // a preceding build model job entry
      modelName = discoverModelName( getParentJob().getJobMeta() );
      if ( modelName == null ) {
        throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Error.UnableToDiscoverModel" ) );
      }
    }

    if ( StringUtils.isBlank( modelName ) ) {
      throw new KettleException( this.getMsg( "JobEntryDatasourcePublish.Error.UnableToFindModelName" ) );
    }

    return environmentSubstitute( modelName );
  }

  public DatabaseMeta discoverDatabaseMeta( final JobMeta jobMeta ) throws KettleException {
    JobEntryBuildModel jobEntryBuildModel = findPrecedingBuildModelJobEntry( jobMeta, this );
    if ( jobEntryBuildModel == null ) {
      return null;
    }
    // When the build model job entry is used during execution time, it will be cloned
    // and initalized with runtime properties.  We need to do the same when using
    // it to get the runtime DatabaseMeta
    JobEntryBuildModel cloneJei = (JobEntryBuildModel) jobEntryBuildModel.clone();
    ( (VariableSpace) cloneJei ).copyVariablesFrom( this );
    cloneJei.setRepository( rep );
    cloneJei.setMetaStore( MetaStoreConst.getDefaultMetastore() );
    cloneJei.setParentJob( this.getParentJob() );
    return cloneJei.getConnectionInfo().getDatabaseMeta();
  }

  public String discoverModelName( final JobMeta jobMeta ) {
    JobEntryBuildModel jobEntryBuildModel = findPrecedingBuildModelJobEntry( jobMeta, this );
    if ( jobEntryBuildModel == null ) {
      return null;
    }
    return jobEntryBuildModel.getModelName();
  }

  public JobEntryBuildModel findPrecedingBuildModelJobEntry( final JobMeta jobMeta, final JobEntryInterface jobEntry ) {

    // get previous job entry
    JobEntryInterface previous = getPreviousJobEntry( jobMeta, jobEntry );

    if ( previous == null ) {


      return null;
    }

    if ( JobEntryBuildModel.class.isAssignableFrom( previous.getClass() ) ) {
      // found it
      return (JobEntryBuildModel) previous;
    } else {
      // keep looking
      return findPrecedingBuildModelJobEntry( jobMeta, previous );
    }
  }

  protected JobEntryInterface getPreviousJobEntry( final JobMeta jobMeta, final JobEntryInterface jobEntry ) {

    for ( JobHopMeta hop : jobMeta.getJobhops() ) {
      if ( !hop.isEnabled() ) {
        continue;
      }
      JobEntryInterface toEntry = hop.getToEntry().getEntry();
      if ( toEntry.equals( jobEntry ) ) {
        return hop.getFromEntry().getEntry();
      }
    }

    return null;
  }

  public boolean evaluates() {
    return true;
  }

  private String getMsg( String key, String... parameters ) {
    return BaseMessages.getString( PKG, key, parameters );
  }
}
