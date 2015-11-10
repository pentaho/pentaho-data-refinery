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
