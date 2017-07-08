/*! ******************************************************************************
 *
 * Pentaho Community Edition Project: data-refinery-pdi-plugin
 *
 * Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.core.refinery.model;

import com.google.gwt.safehtml.shared.UriUtils;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.refinery.publish.agilebi.BiServerConnection;
import org.pentaho.di.core.refinery.publish.agilebi.ModelServerAction;
import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.util.XmiParser;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModelServerFetcher extends ModelServerAction {

  private enum DataSourceType {
    ANALYSIS( "analysis" ),
    DSW( "dsw" );

    private static final String DATASOURCE_PATH = "plugin/data-access/api/datasource";
    private final String listPath;
    private final String downloadPath;

    DataSourceType( String basePath ) {
      listPath = StringUtils.join( new String[] { DATASOURCE_PATH, basePath, "ids" }, "/" );
      downloadPath = StringUtils.join( new String[] { DATASOURCE_PATH, basePath, "%s", "download" }, "/" );
    }

    public String getDownloadPath( String dataSourceId ) {
      assert !StringUtils.isEmpty( dataSourceId );
      return String.format( downloadPath, dataSourceId );
    }

    public String getListPath() {
      return listPath;
    }
  }

  public ModelServerFetcher() {
    super();
  }

  public ModelServerFetcher( BiServerConnection serverConnection ) {
    super( serverConnection );
  }

  /**
   *
   * @return list of accessible DSW IDs
   * @throws AuthorizationException
   * @throws ServerException
   */
  public List<String> fetchDswList() throws AuthorizationException, ServerException {
    return fetchDatasourceIds( DataSourceType.DSW.getListPath() );
  }

  /**
   *
   * @return list of accessible analysis datasource IDs

   * @throws AuthorizationException
   * @throws ServerException
   */
  public List<String> fetchAnalysisList() throws AuthorizationException, ServerException {
    return fetchDatasourceIds( DataSourceType.ANALYSIS.getListPath() );
  }

  protected List<String> fetchDatasourceIds( String path ) throws AuthorizationException, ServerException {
    WebResource listGet = getResource( path );
    ClientResponse response = httpGet( listGet.type( MediaType.APPLICATION_XML ) );
    if ( isSuccess( response ) ) {
      InputStream input = null;
      try {
        input = response.getEntity( InputStream.class );
        InputSource source = new InputSource( input );
        // <List>
        //   <Item ... xsi:type="xs:string">Model.xmi</Item>
        // </List>

        // Weird Mac OS issue.  Without it, XPathFactory.newInstance() could intermittently fail
        if ( Thread.currentThread().getContextClassLoader() == null ) {
          Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
        }
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList items = (NodeList) xpath.evaluate( "/List/Item/text()", source, XPathConstants.NODESET );
        ArrayList<String> result = new ArrayList<String>( items.getLength() );
        for ( int i = 0; i < items.getLength(); i++ ) {
          result.add( items.item( i ).getNodeValue() );
        }
        return result;
      } catch ( XPathExpressionException e ) {
        // shouldn't really happen
        throw new RuntimeException( e );
      } finally {
        IOUtils.closeQuietly( input );
      }
    } else {
      switch ( response.getStatus() ) {
        case 401:
          throw new AuthorizationException();
        case 500:
        default:
          throw new ServerException();
      }
    }
  }

  public String downloadAnalysisFile( String analysisId )
          throws KettleException, AuthorizationException, ServerException, UnsupportedEncodingException {
    String encodedId =  UriUtils.encode( analysisId );
    ClientResponse response =
        getResource( DataSourceType.ANALYSIS.getDownloadPath( encodedId ) ).get( ClientResponse.class );
    if ( isSuccess( response ) ) {
      if ( response.getType().toString().equals( "application/zip" ) ) {
        try ( ZipInputStream zipInputStream = extractFromZip( "schema.xml", response ) ) {
          return IOUtils.toString( zipInputStream );
        } catch ( IOException e ) {
          throw new KettleException( e );
        }
      } else {
        return response.getEntity( String.class );
      }
    } else {
      switch ( response.getStatus() ) {
        case 401:
          throw new AuthorizationException();
        case 500:
        default:
          throw new ServerException();
      }
    }
  }

  /**
   * Fetches and parses a DSW model
   * @param dswId
   * @return
   * @throws AuthorizationException 
   * @throws ServerException 
   */
  public Domain downloadDswFile( String dswId )
          throws KettleException, AuthorizationException, ServerException, UnsupportedEncodingException {
    String encodedId = UriUtils.encode( dswId );
    ClientResponse response = getResource( DataSourceType.DSW.getDownloadPath( encodedId ) ).get( ClientResponse.class );
    if ( isSuccess( response ) ) {
      try ( ZipInputStream zipInputStream = extractFromZip( dswId, response ) ) {
        XmiParser parser = new XmiParser();
        return parser.parseXmi( zipInputStream );
      } catch ( Exception e ) {
        throw new KettleException( e );
      }
    } else {
      switch ( response.getStatus() ) {
        case 401:
          throw new AuthorizationException();
        case 500:
        default:
          throw new ServerException();
      }
    }
  }

  private ZipInputStream extractFromZip( final String fileName, final ClientResponse response ) throws KettleException {
    try {
      InputStream input = response.getEntity( InputStream.class );
      ZipInputStream zipin = new ZipInputStream( input );
      // fileName=Model.xmi -> Model.zip[ Model.xmi, Model.mondrian.xml ]
      for ( ZipEntry entry = zipin.getNextEntry(); entry != null; entry = zipin.getNextEntry() ) {
        if ( entry.getName().equals( fileName ) ) {
          return zipin;
        }
      }
    } catch ( Exception e ) {
      // stream / model issues
      throw new KettleException( e );
    }
    throw new KettleException( "file not found" );
  }

  /**
   * generic 500 or something else not covered
   */
  public static class ServerException extends Exception {
    private static final long serialVersionUID = 1L;
  }
  /**
   * 401
   */
  public static class AuthorizationException extends Exception {
    private static final long serialVersionUID = 1L;
  }
}
