/*
 * ******************************************************************************
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
 *
 * ******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pentaho.di.core.refinery.model;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.pentaho.agilebi.modeler.ModelerException;
import org.pentaho.agilebi.modeler.geo.GeoContextConfigProvider;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.Configuration;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Created by rfellows on 11/23/15.
 */
public class GeoContextBlueprintConfigProvider implements GeoContextConfigProvider {

  private BundleContext bundleContext;
  protected Dictionary<String, Object> props = new Hashtable<>();

  public BundleContext getBundleContext() {
    return bundleContext;
  }

  public void setBundleContext( BundleContext bundleContext ) {
    this.bundleContext = bundleContext;

    initProps();
  }

  protected void initProps() {

    final ServiceReference serviceReference = getBundleContext().getServiceReference( ConfigurationAdmin.class.getName() );
    if ( serviceReference != null ) {

      try {
        final ConfigurationAdmin admin = (ConfigurationAdmin) getBundleContext().getService( serviceReference );
        final Configuration configuration = admin.getConfiguration( "pentaho.geo.roles" );
        props = configuration.getProperties();
      } catch ( Exception e ) {
        props = new Hashtable<>();
      }
    }
  }

  @Override
  public String getDimensionName() throws ModelerException {
    Object value = this.props.get( "geo.dimension.name" );
    if ( value != null ) {
      return value.toString();
    } else {
      return null;
    }
  }

  @Override
  public String getRoles() throws ModelerException {
    Object value = this.props.get( "geo.roles" );
    if ( value != null ) {
      return value.toString();
    } else {
      return null;
    }
  }

  @Override
  public String getRoleAliases( String roleName ) throws ModelerException {
    String aliasKey = "geo." + roleName + ".aliases";
    Object aliases = this.props.get( aliasKey );
    if ( aliases != null && aliases.toString().trim().length() != 0 ) {
      return aliases.toString();
    } else {
      throw new ModelerException( "Error while building GeoContext from configuration: No Aliases found for role  " + roleName + ". Make sure there is a " + aliasKey + " property defined" );
    }
  }

  @Override
  public String getRoleRequirements( String roleName ) throws ModelerException {
    String key = "geo." + roleName + ".required-parents";
    Object value = this.props.get( key );
    if ( value != null ) {
      return value.toString();
    } else {
      return null;
    }
  }
}
