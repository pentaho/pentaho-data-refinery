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

package org.pentaho.di.core.refinery.publish.model;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class DataSourceAclModelTest {

  @Test
  public void testCreateAndAddUserRoles() {
    DataSourceAclModel model = new DataSourceAclModel();
    assertNull( model.getRoles() );
    assertNull( model.getUsers() );
    model.addRole( "testRole" );
    model.addUser( "testUser" );
    assertEquals( "testRole", model.getRoles().get( 0 ) );
    assertEquals( "testUser", model.getUsers().get( 0 ) );

    model.setUsers( Arrays.asList( "otherTestUser" ) );
    model.setRoles( Arrays.asList( "otherTestRole" ) );

    assertEquals( "otherTestRole", model.getRoles().get( 0 ) );
    assertEquals( "otherTestUser", model.getUsers().get( 0 ) );
  }


  @Test
  public void testGetXmlNoUsersOrRoles() {
    DataSourceAclModel model = new DataSourceAclModel();
    assertNull( model.toXml() );
  }


  @Test
  public void testGetXmlOneUserNoRoles() {
    DataSourceAclModel model = new DataSourceAclModel();
    model.addUser( "testUser" );
    String xml = model.toXml();

    assertTrue( xml.contains( "  <aces>    <recipient>testUser</recipient>    <recipientType>0</recipientType>"
            + "    <permissions>4</permissions>    <modifiable>false</modifiable>  </aces>" ) );

  }


  @Test
  public void testGetXmlMultipleUsersNoRoles() {
    DataSourceAclModel model = new DataSourceAclModel();
    model.addUser( "testUser" );
    model.addUser( "testUser2" );
    String xml = model.toXml();

    assertTrue( xml.contains( "  <aces>    <recipient>testUser</recipient>    <recipientType>0</recipientType>"
            + "    <permissions>4</permissions>    <modifiable>false</modifiable>  </aces>" ) );
    assertTrue( xml.contains( "  <aces>    <recipient>testUser2</recipient>    <recipientType>0</recipientType>"
            + "    <permissions>4</permissions>    <modifiable>false</modifiable>  </aces>" ) );
  }

  @Test
  public void testGetXmlMultipleRolesNoUsers() {
    DataSourceAclModel model = new DataSourceAclModel();
    model.addRole( "testRole" );
    model.addRole( "testRole2" );
    String xml = model.toXml();

    assertTrue( xml.contains( "  <aces>    <recipient>testRole</recipient>    <recipientType>1</recipientType>"
            + "    <permissions>4</permissions>    <modifiable>false</modifiable>  </aces>" ) );
    assertTrue( xml.contains( "  <aces>    <recipient>testRole2</recipient>    <recipientType>1</recipientType>"
            + "    <permissions>4</permissions>    <modifiable>false</modifiable>  </aces>" ) );
  }

  @Test
  public void testGetXmlRolesAndUsers() {
    DataSourceAclModel model = new DataSourceAclModel();
    model.addUser( "testUser" );
    model.addRole( "testRole" );
    String xml = model.toXml();

    assertTrue( xml.contains( "  <aces>    <recipient>testRole</recipient>    <recipientType>1</recipientType>"
            + "    <permissions>4</permissions>    <modifiable>false</modifiable>  </aces>" ) );
    assertTrue( xml.contains( "  <aces>    <recipient>testUser</recipient>    <recipientType>0</recipientType>"
            + "    <permissions>4</permissions>    <modifiable>false</modifiable>  </aces>" ) );
  }


}
