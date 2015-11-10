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
