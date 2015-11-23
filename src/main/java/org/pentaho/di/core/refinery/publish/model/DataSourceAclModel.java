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
