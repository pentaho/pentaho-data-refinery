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

package org.pentaho.di.job.entries.build;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.models.annotations.CreateMeasure;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotation;
import org.pentaho.agilebi.modeler.models.annotations.ModelAnnotationGroup;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.ProvidesDatabaseConnectionInformation;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.core.refinery.model.AnalysisModeler;
import org.pentaho.di.core.refinery.model.DswModeler;
import org.pentaho.di.core.refinery.model.ModelServerFetcher;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.value.ValueMetaBigNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobHopMeta;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.special.JobEntrySpecial;
import org.pentaho.di.job.entries.success.JobEntrySuccess;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.dataservice.DataServiceContext;
import org.pentaho.di.trans.dataservice.client.DataServiceConnectionInformation;
import org.pentaho.di.trans.dataservice.serialization.DataServiceMetaStoreUtil;
import org.pentaho.di.trans.step.StepMetaDataCombi;
import org.pentaho.di.trans.steps.tableoutput.TableOutputData;
import org.pentaho.di.trans.steps.tableoutput.TableOutputMeta;
import org.pentaho.di.ui.job.entries.common.ConnectionValidator;
import org.pentaho.metadata.automodel.PhysicalTableImporter;
import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.model.LogicalModel;
import org.pentaho.metadata.model.concept.types.AggregationType;
import org.pentaho.metadata.model.olap.OlapCube;
import org.pentaho.metadata.util.XmiParser;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobEntryBuildModelTest {
  private LogChannelInterface mockLog;
  private Job job;
  private JobEntryTrans trans;
  private DatabaseMeta databaseMeta;
  private JobEntryBuildModel buildJobEntry;
  private JobEntryCopy buildCopy;
  private ModelServerFetcher modelServerFetcher;
  private AnalysisModeler analysisModeler;
  private ConnectionValidator connectionValidator;
  private ProvidesDatabaseConnectionInformation connectionInfo;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    if ( !KettleClientEnvironment.isInitialized() ) {
      KettleClientEnvironment.init();
    }
    PluginRegistry.addPluginType( StepPluginType.getInstance() );
    PluginRegistry.init();
    if ( !Props.isInitialized() ) {
      Props.init( 0 );
    }
  }

  @Before
  public void setUp() throws Exception {

    // DB Setup
    String dbDir = "./target/test-db/JobEntryBuildModelTest-H2-DB";
    File file = new File( dbDir + ".h2.db" );
    if ( file.exists() ) {
      file.delete();
    }
    databaseMeta = new DatabaseMeta( "myh2", "H2", "Native", null, dbDir, null, "sa", null );
    Database db = new Database( null, databaseMeta );
    db.connect();
    db.execStatement( "DROP TABLE IF EXISTS customer_test;" );
    db.execStatement( "DROP TABLE IF EXISTS sales_test;" );
    db.execStatement(
        "CREATE TABLE customer_test(ID DOUBLE, First VARCHAR(1000000000), Last VARCHAR(1000000000),"
            + " State VARCHAR(1000000000));" );
    db.execStatement(
        "CREATE TABLE sales_test(Price VARCHAR(1000000000), Customer VARCHAR(1000000000), Date TIMESTAMP,"
          + "\"Product Name\" VARCHAR(1000000000), \"Product SKU\" VARCHAR(1000000000),"
          + "\"Product Category\" VARCHAR(1000000000), Quantity DOUBLE, Customer_Id DOUBLE);" );
    db.disconnect();

    // Job Setup
    job = new Job( null, new JobMeta() );
    job.getJobMeta().addParameterDefinition( "tableSuffix", "_test", "" );
    // Add start job entry
    JobEntrySpecial start = new JobEntrySpecial( "START", true, false );
    JobEntryCopy startCopy = new JobEntryCopy( start );
    startCopy.setDrawn();
    job.getJobMeta().addJobEntry( startCopy );
    start.setParentJob( job );

    // Call transformation to load table
    trans = new JobEntryTrans();
    trans.setName( "Load Market Data Table" );
    trans.parameters = new String[0];
    trans.parameterFieldNames = new String[0];
    trans.parameterValues = new String[0];
    JobEntryCopy transCopy = new JobEntryCopy( trans );
    transCopy.setDrawn();
    job.getJobMeta().addJobEntry( transCopy );
    trans.setParentJob( job );
    trans.setFileName( getClass().getResource( "/Sales Data Load.ktr" ).getPath() );

    JobHopMeta hop = new JobHopMeta( startCopy, transCopy );
    job.getJobMeta().addJobHop( hop );

    modelServerFetcher = mock( ModelServerFetcher.class );
    analysisModeler = mock( AnalysisModeler.class );
    connectionValidator = mock( ConnectionValidator.class );

    connectionInfo = mock( ProvidesDatabaseConnectionInformation.class );
    when( connectionInfo.getDatabaseMeta() ).thenReturn( databaseMeta );
    when( connectionInfo.getSchemaName() ).thenReturn( "" );
    when( connectionInfo.getTableName() ).thenReturn( "sales_test" );

    // Add Build Model job entry
    buildJobEntry = spy( new JobEntryBuildModel( "Build Model", "Builds sales analysis model" ) {
      @Override ModelServerFetcher getModelServerFetcher() {
        return modelServerFetcher;
      }

      @Override AnalysisModeler getAnalysisModeler() {
        return analysisModeler;
      }

      @Override protected ConnectionValidator getConnectionValidator( BiServerConnection biServerConnection ) {
        return connectionValidator;
      }

      @Override public void setParentJobMeta( JobMeta jb ) { }
      @Override public ProvidesDatabaseConnectionInformation getConnectionInfo() {
        return connectionInfo;
      }
    } );
    buildJobEntry.setOutputStep( "Sales Fact" );
    DswModeler modeler = new DswModeler();
    buildJobEntry.setModeler( modeler );
    buildCopy = new JobEntryCopy( buildJobEntry );
    buildCopy.setDrawn();
    job.getJobMeta().addJobEntry( buildCopy );
    buildJobEntry.setParentJob( job );

    JobHopMeta hop2 = new JobHopMeta( transCopy, buildCopy );
    job.getJobMeta().addJobHop( hop2 );

    setJobMeta();
    setAnnotations();
    mockLog = mock( LogChannelInterface.class );
  }

  private void setAnnotations() {
    CreateMeasure avgQuantity = new CreateMeasure();
    avgQuantity.setAggregateType( AggregationType.AVERAGE );
    avgQuantity.setName( "Average Quantity" );
    avgQuantity.setField( "QUANTITY" );
    job.getExtensionDataMap().put(
        JobEntryBuildModel.KEY_MODEL_ANNOTATIONS,
        new ModelAnnotationGroup( new ModelAnnotation<CreateMeasure>( avgQuantity ) ) );
  }

  private void setJobMeta() throws KettleException {
    StepMetaDataCombi metaDataCombi = new StepMetaDataCombi();
    RowMeta rowMeta = new RowMeta();
    rowMeta.addValueMeta( new ValueMetaBigNumber( "QUANTITY" ) );
    rowMeta.addValueMeta( new ValueMetaBigNumber( "CUSTOMER_ID" ) );
    rowMeta.addValueMeta( new ValueMetaString( "CUSTOMER" ) );
    rowMeta.addValueMeta( new ValueMetaString( "Product Name" ) );
    TableOutputData tableOutputData = new TableOutputData();
    tableOutputData.insertRowMeta = rowMeta;
    metaDataCombi.data = tableOutputData;
    TableOutputMeta tableOutputMeta = new TableOutputMeta();
    tableOutputMeta.setFieldStream( new String[] { "QUANTITY", "CUSTOMER_ID", "CUSTOMER", "Product Name" } );
    tableOutputMeta.setFieldDatabase( new String[] { "QUANTITY", "CUSTOMER_ID", "CUSTOMER", "Product Name" } );
    metaDataCombi.meta = tableOutputMeta;
    job.getExtensionDataMap().put( JobEntryBuildModel.KEY_OUTPUT_STEP_PREFIX + "Build Model", metaDataCombi );
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testErrorHop() throws Exception {
    JobEntrySuccess jobEntrySuccess = new JobEntrySuccess();
    JobEntryCopy copy = new JobEntryCopy( jobEntrySuccess );
    copy.setDrawn();
    job.getJobMeta().addJobEntry( copy );
    jobEntrySuccess.setParentJob( job );

    // Create a error hop that leads to the job success
    JobHopMeta hop2 = new JobHopMeta( buildCopy, copy );
    hop2.setEvaluation( false );
    job.getJobMeta().addJobHop( hop2 );

    job.run();
    // Build model job entry will fail but it'll follow the error hop to the job success entry
    // and thus the whole job will succeed with no errors.
    assertTrue( job.getResult().getResult() );
    assertEquals( 0, job.getResult().getNrErrors() );

    // Now switch the hop to success so that the job fails immediately in the build model job entry
    hop2.setEvaluation( true );
    job.run();
    assertFalse( job.getResult().getResult() );
    assertEquals( 1, job.getResult().getNrErrors() );
  }

  /**
   * Verifies that we can auto-model during runtime and generate a valid Metadata and Mondrian model.
   *
   * @throws Exception
   */
  @Test
  public void testJobEntry() throws Exception {

    job.setVariable( "logicalModelName", "\t\tSales Fact          \t" ); // test leading/trailing spaces
    job.setVariable( "dataSourceName", "Car Sales Analysis" );
    buildJobEntry.setOutputStep( "%%logicalModelName%%" );
    buildJobEntry.setModelName( "%%dataSourceName%%" );
    job.run();

    assertTrue( job.getResult().getResult() );

    String xmi = job.getVariable( "JobEntryBuildModel.XMI.Car Sales Analysis" );
    assertTrue( xmi, xmi.contains( "<CWM:Description body=\"SALES_TEST\" name=\"target_table\"" ) );
    assertTrue( xmi, xmi.contains( "<CWMOLAP:Schema name=\"MODEL_1_OLAP\"" ) );

    // Verify we can parse the XMI and read the cube. Note that the cube name was dynamically generated
    // because the output table was parameterized.
    XmiParser parser = new XmiParser();
    Domain d = parser.parseXmi( new ByteArrayInputStream( xmi.getBytes( "UTF-8" ) ) );
    assertEquals( "Car Sales Analysis", d.getLogicalModels().get( 0 ).getName().getLocalizedString( "en" ) );
    assertEquals( "Car Sales Analysis_OLAP", d.getLogicalModels().get( 1 ).getName().getLocalizedString( "en" ) );

    assertEquals( 2, d.getLogicalModels().size() );
    @SuppressWarnings( "unchecked" )
    List<OlapCube> cubes =
        (List<OlapCube>) d.getLogicalModels().get( 1 ).getProperty( LogicalModel.PROPERTY_OLAP_CUBES );
    assertEquals( "Car Sales Analysis", cubes.get( 0 ).getName() );
    assertEquals( 2, cubes.get( 0 ).getOlapMeasures().size() );
    assertEquals( "Average Quantity", cubes.get( 0 ).getOlapMeasures().get( 1 ).getName() );

    assertEquals( "myh2", job.getVariable( "JobEntryBuildModel.DatabaseConnection.Car Sales Analysis" ) );
    assertEquals( "true", job.getVariable( "JobEntryBuildModel.XMI.DSW.Car Sales Analysis" ) );

  }

  @Test
  public void testCanUseExistingDSW() throws Exception {

    job.setVariable( "logicalModelName", "\t\tSales Fact          \t" ); // test leading/trailing spaces
    job.setVariable( "dataSourceName", "Car Sales Analysis" );
    job.setVariable( "existingModelName", "testDSW.xmi" );
    buildJobEntry.setOutputStep( "%%logicalModelName%%" );
    buildJobEntry.setModelName( "%%dataSourceName%%" );
    buildJobEntry.setSelectedModel( "%%existingModelName%%" );
    buildJobEntry.setUseExistingModel( true );
    InputStream existingSchema =
        getClass().getResourceAsStream( "/org/pentaho/di/core/refinery/model/resources/testDSW.xmi" );
    when( modelServerFetcher.downloadDswFile( "testDSW.xmi" ) )
        .thenReturn( new XmiParser().parseXmi( existingSchema ) );
    when( modelServerFetcher.fetchDswList() ).thenReturn( asList( "testDSW.xmi" ) );
    job.run();

    assertTrue( job.getResult().getResult() );

    String xmi = job.getVariable( "JobEntryBuildModel.XMI.Car Sales Analysis" );
    assertTrue( xmi, xmi.contains( "<CWM:Description body=\"sales_test\" name=\"target_table\"" ) );
    assertTrue( xmi, xmi.contains( "<CWMOLAP:Schema name=\"MODEL_1_OLAP\"" ) );

    // Verify we can parse the XMI and read the cube. Note that the cube name was dynamically generated
    // because the output table was parameterized.
    XmiParser parser = new XmiParser();
    Domain d = parser.parseXmi( new ByteArrayInputStream( xmi.getBytes( "UTF-8" ) ) );
    assertEquals( "Car Sales Analysis", d.getLogicalModels().get( 0 ).getName().getLocalizedString( "en" ) );
    assertEquals( "Car Sales Analysis", d.getLogicalModels().get( 1 ).getName().getLocalizedString( "en" ) );

    assertEquals( 2, d.getLogicalModels().size() );
    @SuppressWarnings( "unchecked" )
    List<OlapCube> cubes =
        (List<OlapCube>) d.getLogicalModels().get( 1 ).getProperty( LogicalModel.PROPERTY_OLAP_CUBES );
    assertEquals( "Car Sales Analysis", cubes.get( 0 ).getName() );

    assertEquals( "myh2", job.getVariable( "JobEntryBuildModel.DatabaseConnection.Car Sales Analysis" ) );
    assertEquals( "true", job.getVariable( "JobEntryBuildModel.XMI.DSW.Car Sales Analysis" ) );

  }

  @Test
  public void testCanUseExistingMondrianModel() throws Exception {

    job.setVariable( "logicalModelName", "\t\tSales Fact          \t" ); // test leading/trailing spaces
    job.setVariable( "dataSourceName", "Car Sales Analysis" );
    String someModelName = "someModelName";
    job.setVariable( "existingModelName", someModelName );
    buildJobEntry.setOutputStep( "%%logicalModelName%%" );
    buildJobEntry.setModelName( "%%dataSourceName%%" );
    String existingModelName = "%%existingModelName%%";
    buildJobEntry.setExistingModel( existingModelName );
    buildJobEntry.setSelectedModel( existingModelName );
    buildJobEntry.setUseExistingModel( true );
    String existingSchema = IOUtils
        .toString( getClass().getResourceAsStream( "/org/pentaho/di/core/refinery/model/resources/testAnalysisSchema.xml" ) );
    when( modelServerFetcher.downloadAnalysisFile( someModelName ) )
        .thenReturn( existingSchema );
    String expectedSchema = IOUtils.toString(
        getClass().getResourceAsStream( "/org/pentaho/di/core/refinery/model/resources/salesTestAnalysisSchema.xml" ) );
    when( modelServerFetcher.downloadAnalysisFile( someModelName ) )
        .thenReturn( IOUtils
            .toString(
                getClass().getResourceAsStream( "/org/pentaho/di/core/refinery/model/resources/testAnalysisSchema.xml" ) ) );

    when( modelServerFetcher.fetchAnalysisList() ).thenReturn( asList( "someModelName" ) );

    when( analysisModeler.replaceTableAndSchemaNames(
        eq( existingSchema ), eq( "Car Sales Analysis" ) ) )
        .thenReturn( expectedSchema );
    doNothing().when( connectionValidator ).validateConnectionInRuntime();
    job.run();

    assertTrue( job.getResult().getResult() );

    String xmi = job.getVariable( "JobEntryBuildModel.XMI.Car Sales Analysis" );
    assertNull( xmi );

    assertEquals( expectedSchema,
        job.getVariable( "JobEntryBuildModel.Mondrian.Schema.Car Sales Analysis" ) );
    assertEquals( IOUtils.toString(
            getClass().getResourceAsStream( "/org/pentaho/di/core/refinery/model/resources/salesTestAnalysisSchema.xml" ) ),
        job.getVariable( "JobEntryBuildModel.Mondrian.Schema.Car Sales Analysis" ) );

    assertEquals( "myh2", job.getVariable( "JobEntryBuildModel.Mondrian.Datasource.Car Sales Analysis" ) );
  }

  @Test
  public void testBuildXmiShouldPassEmptySchemaNameWhenNull() throws Exception {

    DswModeler modeler = mock( DswModeler.class );
    ProvidesDatabaseConnectionInformation connectionInfo = mock( ProvidesDatabaseConnectionInformation.class );

    doReturn( connectionInfo ).when( buildJobEntry ).getConnectionInfo();
    doReturn( true ).when( buildJobEntry ).useExistingModel();
    doReturn( true ).when( buildJobEntry ).modelExists( any(), any( ModelServerFetcher.class ) );
    doReturn( modeler ).when( buildJobEntry ).getDswModeler();

    doReturn( null ).when( connectionInfo ).getSchemaName(); // return null schemaName
    buildJobEntry.buildXmi( null, "outputStep", "modelName" );

    String schemaName = ""; // verify
    verify( modeler, times( 1 ) ).updateModel( "modelName", null, null, schemaName, null );
  }

  @Test
  public void testEmptyExistingModelThrowsException() throws Exception {

    job.setVariable( "logicalModelName", "\t\tSales Fact          \t" ); // test leading/trailing spaces
    job.setVariable( "dataSourceName", "Car Sales Analysis" );
    buildJobEntry.setOutputStep( "%%logicalModelName%%" );
    buildJobEntry.setModelName( "%%dataSourceName%%" );
    buildJobEntry.setExistingModel( "" );
    buildJobEntry.setSelectedModel( "" );
    buildJobEntry.setUseExistingModel( true );
    doNothing().when( connectionValidator ).validateConnectionInRuntime();
    job.run();

    assertFalse( job.getResult().getResult() );
    assertTrue( job.getResult().getLogText().contains( "has a blank or missing modeling method" ) );
  }

  //BACKLOG-1577 - If the analysis model does not exist we can not create it for the time being
  @Test
  public void testCannotCreateNewMondrianModel() throws Exception {
    buildJobEntry.setOutputStep( "Sales Fact" );
    buildJobEntry.setModelName( "Car Sales Analysis" );

    buildJobEntry.setExistingModel( "someModelName" );
    buildJobEntry.setSelectedModel( "someModelName" );
    buildJobEntry.setUseExistingModel( true );

    when( modelServerFetcher.fetchAnalysisList() ).thenReturn( asList( "otherModel" ) );

    doNothing().when( connectionValidator ).validateConnectionInRuntime();
    job.run();
    assertFalse( job.getResult().getResult() );

  }

  @Test
  public void testGetLoadXML() throws Exception {

    buildJobEntry.setOutputStep( "testLogicalModel" );
    buildJobEntry.setModelName( "testDatasource" );
    buildJobEntry.setExistingModel( "http://somehost/path" );
    buildJobEntry.setUseExistingModel( true );

    BiServerConnection biServerModel = new BiServerConnection();
    biServerModel.setUrl( "http://localhost:8080/pentaho" );
    biServerModel.setUserId( "admin" );
    biServerModel.setPassword( "password" );
    buildJobEntry.setBiServerConnection( biServerModel );
    buildJobEntry.setCreateOnPublish( true );
    buildJobEntry.setSelectedModel( "SomeModel" );

    StringBuffer retval = new StringBuffer();
    retval.append( "    " ).append( XMLHandler.openTag( "entry" ) ).append( Const.CR );
    retval.append( buildJobEntry.getXML() );
    retval.append( "      " ).append( XMLHandler.closeTag( "entry" ) ).append( Const.CR );

    Document doc = XMLHandler.loadXMLString( retval.toString() );
    Node node = XMLHandler.getSubNode( doc, "entry" );

    JobEntryBuildModel local = new JobEntryBuildModel( "name", "desc" );
    local.loadXML( node, null, null, null, null );
    assertEquals( "testLogicalModel", local.getOutputStep() );
    assertEquals( "testDatasource", local.getModelName() );
    assertEquals( buildJobEntry.getName(), local.getName() );
    assertEquals( buildJobEntry.getDescription(), local.getDescription() );
    assertEquals( buildJobEntry.useExistingModel(), local.useExistingModel() );
    assertEquals( buildJobEntry.getExistingModel(), local.getExistingModel() );
    assertEquals( buildJobEntry.getBiServerConnection().getUrl(), local.getBiServerConnection().getUrl() );
    assertEquals( buildJobEntry.getBiServerConnection().getUserId(), local.getBiServerConnection().getUserId() );
    assertEquals( buildJobEntry.getBiServerConnection().getPassword(), local.getBiServerConnection().getPassword() );
    assertEquals( buildJobEntry.isCreateOnPublish(), local.isCreateOnPublish() );
    assertEquals( buildJobEntry.getSelectedModel(), local.getSelectedModel() );
  }

  @Test
  public void testGetOutputStepList() throws Exception {

    DataServiceContext dataServiceContext = mock( DataServiceContext.class );
    buildJobEntry.setDataServiceContext( dataServiceContext );
    IMetaStore metaStore = mock( IMetaStore.class );
    buildJobEntry.setMetaStore( metaStore );

    DataServiceMetaStoreUtil dataServiceMetaStoreUtil = mock( DataServiceMetaStoreUtil.class );
    when( dataServiceContext.getMetaStoreUtil() ).thenReturn( dataServiceMetaStoreUtil );
    when( dataServiceMetaStoreUtil.getDataServiceNames(
      trans.getTransMeta( null, metaStore, new Variables() ) ) )
      .thenReturn( asList( "Sales Service", "Customer Service" ) );
    // step names should be auto-trimmed -  see Sales Data Load.ktr (leading/trailing spaces in the step names)
    List<String> steps = asList( buildJobEntry.getOutputStepList( job.getJobMeta() ) );

    assertEquals( 4, steps.size() );

    // can't guarantee order of the underlying map
    assertTrue( steps.contains( "Customer Dimension" ) );
    assertTrue( steps.contains( "Sales Fact" ) );
    assertTrue( steps.contains( "Sales Service" ) );
    assertTrue( steps.contains( "Customer Service" ) );
  }

  @Test
  public void testGetOutputStepListNoConnection() throws Exception {

    // step names should be auto-trimmed -  see Sales Data Load.ktr (leading/trailing spaces in the step names)
    List<JobHopMeta> hops = job.getJobMeta().getJobhops();
    JobHopMeta theHop = null;
    JobEntryCopy theCopy = null;
    for ( JobHopMeta hop : hops ) {
      if ( hop.getFromEntry().getEntry() == trans ) {
        theCopy = hop.getFromEntry();
        theHop = hop;
        hop.setFromEntry( null );
        break;
      }
    }
    String[] steps = buildJobEntry.getOutputStepList( job.getJobMeta() );

    assertEquals( 0, steps.length );

    theHop.setFromEntry( theCopy );
    steps = buildJobEntry.getOutputStepList( job.getJobMeta() );
    assertEquals( 2, steps.length );

    // can't guarantee order of the underlying map
    assertTrue( "Customer Dimension".equals( steps[0] ) || "Customer Dimension".equals( steps[1] ) );
    assertTrue( "Sales Fact".equals( steps[1] ) || "Sales Fact".equals( steps[0] ) );
  }

  @Test
  public void testExecuteLogging() throws Exception {

    buildJobEntry.setOutputStep( "Sales Fact" );
    buildJobEntry.setModelName( "Car Sales Analysis" );
    Result result = mock( Result.class );

    buildJobEntry.execute( result, 0 );

    verify( buildJobEntry, times( 1 ) )
        .setVarAndLogBasic( "JobEntryBuildModel.DatabaseConnection.Car Sales Analysis", "myh2" );
    verify( buildJobEntry, times( 1 ) )
        .setVarAndLogBasic( "JobEntryBuildModel.XMI.DSW.Car Sales Analysis", "true" );
    verify( buildJobEntry, times( 1 ) )
        .setVarAndLogDebug( anyString(), anyString() );

    doReturn( true ).when( buildJobEntry ).isPublishAnalysis();
    doReturn( null ).when( buildJobEntry ).buildAnalysis( anyString() );
    buildJobEntry.execute( result, 0 );

    verify( buildJobEntry, times( 2 ) )
        .setVarAndLogBasic( "JobEntryBuildModel.DatabaseConnection.Car Sales Analysis", "myh2" );
    verify( buildJobEntry, times( 1 ) )
        .setVarAndLogDebug( "JobEntryBuildModel.Mondrian.Schema.Car Sales Analysis", null );
    verify( buildJobEntry, times( 1 ) )
        .setVarAndLogBasic( "JobEntryBuildModel.Mondrian.Datasource.Car Sales Analysis", "myh2" );
  }

  @Test
  public void testCanUseDataServiceForConnection() throws Exception {
    JobEntryTrans trans = new JobEntryTrans( "trans" ) {
      @Override
      public TransMeta getTransMeta( final Repository rep, final IMetaStore metaStore, final VariableSpace space )
        throws KettleException {
        return new TransMeta();
      }
    };
    trans.setPluginId( "TRANS" );
    JobEntryTrans badTrans = new JobEntryTrans( "badTrans" ) {
      @Override
      public TransMeta getTransMeta( final Repository rep, final IMetaStore metaStore, final VariableSpace space )
        throws KettleException {
        throw new KettleException( "something happend" );
      }
    };
    badTrans.setPluginId( "TRANS" );
    JobEntryCopy copyOne = new JobEntryCopy( trans );
    JobEntryCopy badCopy = new JobEntryCopy( badTrans );
    JobEntryCopy copyTwo = new JobEntryCopy( new JobEntrySuccess() );
    final List<JobEntryCopy> jobCopies = asList( copyOne, badCopy, copyTwo );

    JobEntryBuildModel jobEntry = new JobEntryBuildModelForTest( jobCopies );
    jobEntry.setOutputStep( "service two" );
    DataServiceContext dataServiceContext = mock( DataServiceContext.class );
    jobEntry.setDataServiceContext( dataServiceContext );
    final DataServiceMetaStoreUtil metaStoreUtil = mock( DataServiceMetaStoreUtil.class );
    when( dataServiceContext.getMetaStoreUtil() ).thenReturn( metaStoreUtil );
    when( metaStoreUtil.getDataServiceNames( any( TransMeta.class )  ) )
      .thenReturn( asList( "service one", "service two" ) );
    ProvidesDatabaseConnectionInformation connectionInfo = jobEntry.getConnectionInfo();
    assertTrue( connectionInfo instanceof DataServiceConnectionInformation );
    assertEquals( "service two", connectionInfo.getTableName() );
    verify( mockLog ).logDebug( eq( "Unable to look inside transformation badTrans." ), any() );
  }

  @Test
  public void testFailedDataServiceHasSpecificError() throws Exception {
    runModelerExceptionTest( true, "We weren't able to run the transformation associated with the Pentaho Data Service. The transformation needs to run from the server." );
    runModelerExceptionTest( false, "org.pentaho.agilebi.modeler.ModelerException: modeler Exception Message\n"
      + "modeler Exception Message" );
  }

  private void runModelerExceptionTest( final boolean isDataService, final String msg ) {
    final LogChannelInterface logChannel = mock( LogChannelInterface.class );
    final DatabaseMeta mockMeta = mock( DatabaseMeta.class );
    JobEntryBuildModel jobEntryBuildModel = new JobEntryBuildModel() {
      @Override public ProvidesDatabaseConnectionInformation getConnectionInfo() throws KettleException {
        return new DataServiceConnectionInformation( "aService", null, logChannel ) {
          @Override public DatabaseMeta getDatabaseMeta() {
            return mockMeta;
          }
        };
      }

      @Override PhysicalTableImporter.ImportStrategy getImportStrategy() throws KettleException, ModelerException {
        throw new ModelerException( "modeler Exception Message" );
      }

      @Override boolean isOutputStepADataService() throws KettleException {
        return isDataService;
      }
    };
    try {
      jobEntryBuildModel.buildXmi( null, "someStepName", "any" );
    } catch ( KettleException e ) {
      assertEquals( msg, e.getMessage().trim().replace( "\r", "" ) );
    }
  }

  private class JobEntryBuildModelForTest extends JobEntryBuildModel {
    private List<JobEntryCopy> jobCopies;

    public JobEntryBuildModelForTest( final List<JobEntryCopy> jobCopies ) {
      super();
      this.jobCopies = jobCopies;
      log = mockLog;
    }

    @Override StepMetaDataCombi getStepMetaDataCombi() {
      return null;
    }

    @Override List<JobEntryCopy> getParentJobCopies() {
      return jobCopies;
    }
  }

  @Test
  public void testEmptyOutputStepIsError() throws Exception {
    buildJobEntry.setOutputStep( null );
    job.run();
    assertEquals( 1, job.getResult().getNrErrors() );
    buildJobEntry.setOutputStep( "" );
    job.run();
    assertEquals( 1, job.getResult().getNrErrors() );
  }
}
