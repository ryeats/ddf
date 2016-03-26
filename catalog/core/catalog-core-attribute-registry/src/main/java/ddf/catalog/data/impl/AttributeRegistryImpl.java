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
package ddf.catalog.data.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;

public class AttributeRegistryImpl implements AttributeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeRegistryImpl.class);

    private final Map<String, AttributeDescriptor> attributeMap = new ConcurrentHashMap<>();

    public AttributeRegistryImpl() {
        BasicTypes.BASIC_METACARD.getAttributeDescriptors()
                .stream()
                .forEach(this::registerAttribute);
    }

    @Override
    public boolean registerAttribute(final AttributeDescriptor attributeDescriptor) {
        Preconditions.checkArgument(attributeDescriptor != null,
                "Attribute descriptor cannot be null.");
        Preconditions.checkArgument(attributeDescriptor.getName() != null,
                "Attribute name cannot be null.");

        final String name = attributeDescriptor.getName();

        if (!attributeMap.containsKey(name)) {
            attributeMap.put(name, attributeDescriptor);
            return true;
        }

        LOGGER.debug(
                "Attempted to register an attribute with name '{}', but an attribute with that name already exists.",
                name);
        return false;
    }

    @Override
    public void deregisterAttribute(final String name) {
        if (name != null) {
            attributeMap.remove(name);
        }
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(final String name) {
        if (name != null) {
            return attributeMap.get(name);
        }

        return null;
    }
}
