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
package ddf.catalog.validation.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;

import com.google.common.base.Preconditions;

import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;

public class AttributeValidatorRegistryImpl implements AttributeValidatorRegistry {
    private final Map<String, Set<AttributeValidator>> attributeValidatorMap =
            new ConcurrentHashMap<>();

    @Override
    public void registerValidators(final String attributeName,
            final Collection<? extends AttributeValidator> validators) {
        Preconditions.checkArgument(attributeName != null, "Attribute name cannot be null.");
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(validators),
                "Must register at least one validator.");

        if (attributeValidatorMap.containsKey(attributeName)) {
            final Set<AttributeValidator> validatorSet = attributeValidatorMap.get(attributeName);
            validatorSet.addAll(validators);
        } else {
            attributeValidatorMap.put(attributeName, new HashSet<>(validators));
        }
    }

    @Override
    public void deregisterValidators(final String attributeName) {
        if (attributeName != null) {
            attributeValidatorMap.remove(attributeName);
        }
    }

    @Override
    public Set<AttributeValidator> getValidators(final String attributeName) {
        if (attributeName != null && attributeValidatorMap.containsKey(attributeName)) {
            return attributeValidatorMap.get(attributeName);
        }

        return Collections.emptySet();
    }
}
