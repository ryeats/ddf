/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.platform.ignite;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.osgi.IgniteAbstractOsgiContextActivator;
import org.apache.ignite.osgi.classloaders.OsgiClassLoadingStrategyType;

public class IgniteActivator extends IgniteAbstractOsgiContextActivator {

    /**
     * Configure your Ignite instance as you would normally do,
     * and return it.
     */
    @Override
    public IgniteConfiguration igniteConfiguration() {
        IgniteConfiguration config = new IgniteConfiguration();
        config.setGridName("testGrid");

        // ...

        return config;
    }

    /**
     * Choose the classloading strategy for Ignite to use.
     */
    @Override
    public OsgiClassLoadingStrategyType classLoadingStrategy() {
        return OsgiClassLoadingStrategyType.BUNDLE_DELEGATING;
    }
}