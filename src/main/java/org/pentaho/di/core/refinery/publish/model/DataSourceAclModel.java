/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.core.refinery.publish.model;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;

public class DataSourceAclModel {

  private List<String> users;
  private List<String> roles;



  public void addUser( String user ) {
    if ( users == null ) {
      users = new ArrayList<String>();
    }

    users.add( user );
  }


  public void addRole( String role ) {
    if ( roles == null ) {
      roles = new ArrayList<String>();
    }
    roles.add( role );
  }

  public List<String> getUsers() {
    return users;
  }

  public void setUsers( List<String> users ) {
    this.users = users;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles( List<String> roles ) {
    this.roles = roles;
  }

  public String toXml() {
    if ( ( users == null || users.size() == 0 )
         && ( roles == null || roles.size() == 0 ) ) {
      return null;
    }

    StringBuilder builder = new StringBuilder();
    builder.append( "<repositoryFileAclDto>" );
    if ( users != null ) {
      for ( String user : users ) {
        builder.append( "  <aces>" );
        builder.append( "    <recipient>" ).append( StringEscapeUtils.escapeXml( user ) ).append( "</recipient>" );
        builder.append( "    <recipientType>0</recipientType>" );
        builder.append( "    <permissions>4</permissions>" );
        builder.append( "    <modifiable>false</modifiable>" );
        builder.append( "  </aces>" );
      }
    }
    if ( roles != null ) {
      for ( String role : roles ) {
        builder.append( "  <aces>" );
        builder.append( "    <recipient>" ).append( StringEscapeUtils.escapeXml( role ) ).append( "</recipient>" );
        builder.append( "    <recipientType>1</recipientType>" );
        builder.append( "    <permissions>4</permissions>" );
        builder.append( "    <modifiable>false</modifiable>" );
        builder.append( "  </aces>" );

      }
    }

    builder.append( "  <entriesInheriting>false</entriesInheriting>" );
    builder.append( "  <id></id>" );
    builder.append( "  <owner></owner>" );
    builder.append( "  <ownerType></ownerType>" );
    builder.append( "</repositoryFileAclDto>" );

    return builder.toString();
  }

}
