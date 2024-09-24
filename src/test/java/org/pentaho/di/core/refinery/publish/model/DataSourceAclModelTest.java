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
