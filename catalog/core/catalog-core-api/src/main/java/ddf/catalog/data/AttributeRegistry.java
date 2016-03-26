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
package ddf.catalog.data;

/**
 * Manages registered attribute types.
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface AttributeRegistry {
    /**
     * Registers a new attribute. Returns false if an attribute with the same name already exists
     * (in which case the attribute is not registered) and true otherwise.
     *
     * @param attributeDescriptor the {@link AttributeDescriptor} describing the attribute
     * @return whether the attribute was registered
     * @throws IllegalArgumentException if {@code attributeDescriptor} or
     *                                  {@link AttributeDescriptor#getName()} is null
     */
    boolean registerAttribute(AttributeDescriptor attributeDescriptor);

    /**
     * Removes an attribute from the registry.
     *
     * @param name the name of the attribute to remove
     */
    void deregisterAttribute(String name);

    /**
     * Gets the {@link AttributeDescriptor} for the attribute with the given name.
     *
     * @param name the name of the attribute
     * @return the {@link AttributeDescriptor} describing the attribute, or null if no attribute
     * with that name exists
     */
    AttributeDescriptor getAttributeDescriptor(String name);
}
