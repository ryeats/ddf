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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.boon.json.JsonFactory;
import org.boon.json.annotations.JsonIgnore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.impl.validator.DecimalRangeValidator;
import ddf.catalog.validation.impl.validator.EnumerationValidator;
import ddf.catalog.validation.impl.validator.FutureDateValidator;
import ddf.catalog.validation.impl.validator.IntegerRangeValidator;
import ddf.catalog.validation.impl.validator.PastDateValidator;
import ddf.catalog.validation.impl.validator.PatternValidator;
import ddf.catalog.validation.impl.validator.RequiredAttributesMetacardValidator;
import ddf.catalog.validation.impl.validator.SizeValidator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings
public class ValidationParser implements ArtifactInstaller {
    private AttributeRegistry attributeRegistry;

    private AttributeValidatorRegistry attributeValidatorRegistry;

    private static Map<String, Outer> sourceMap = new ConcurrentHashMap<>();

    @Override
    public void install(File file) throws Exception {
        String data;
        try (InputStream input = new FileInputStream(file)) {
            data = IOUtils.toString(input, StandardCharsets.UTF_8.name());
        }

        Outer outer = JsonFactory.create()
                .readValue(data, Outer.class);

        /* Must manually parse validators */
        Map<String, Object> root = JsonFactory.create()
                .parser()
                .parseMap(data);
        parseValidators(root, outer);

        sourceMap.put(file.getName(), outer);

        if (outer.attributeTypes != null) {
            parseAttributeTypes(outer.attributeTypes);
        }
        if (outer.metacardTypes != null) {
            parseMetacardTypes(outer.metacardTypes);
        }
        if (outer.validators != null) {
            parseValidators(outer.validators);
        }

    }

    @Override
    public void update(File file) throws Exception {
        // you don't REALLY need updating do you?
    }

    @Override
    public void uninstall(File file) throws Exception {
        // like, come on. You can just restart your system.
    }

    @Override
    public boolean canHandle(File file) {
        return file.getName()
                .endsWith(".json");
    }

    public void setAttributeRegistry(AttributeRegistry attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
    }

    public void setAttributeValidatorRegistry(
            AttributeValidatorRegistry attributeValidatorRegistry) {
        this.attributeValidatorRegistry = attributeValidatorRegistry;
    }

    @SuppressWarnings("unchecked")
    private void parseValidators(Map<String, Object> root, Outer outer) {
        if (root == null || root.get("validators") == null) {
            return;
        }

        Map<String, List<Outer.Validator>> validators = new HashMap<>();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) root.get("validators")).entrySet()) {
            String rejson = JsonFactory.create()
                    .toJson(entry.getValue());
            List<Outer.Validator> lv = JsonFactory.create()
                    .readValue(rejson, List.class, Outer.Validator.class);
            validators.put(entry.getKey(), lv);
        }
        outer.validators = validators;
    }

    private void parseAttributeTypes(Map<String, Outer.AttributeType> attributeTypes) {
        // TODO (RCZ) - Maybe do some sort of validation with these?
        for (Map.Entry<String, Outer.AttributeType> entry : attributeTypes.entrySet()) {
            AttributeDescriptor descriptor = new AttributeDescriptorImpl(entry.getKey(),
                    entry.getValue().indexed,
                    entry.getValue().stored,
                    entry.getValue().tokenized,
                    entry.getValue().multivalued,
                    BasicTypes.getAttributeType(entry.getValue().type));
            attributeRegistry.registerAttribute(descriptor);

        }
    }

    private void parseMetacardTypes(List<Outer.MetacardType> metacardTypes) {
        BundleContext context = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        for (Outer.MetacardType metacardType : metacardTypes) {
            Set<AttributeDescriptor> attributeDescriptors =
                    new HashSet<>(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
            Set<String> requiredAttributes = new HashSet<>();

            for (Map.Entry<String, Outer.MetacardAttribute> entry : metacardType.attributes.entrySet()) {
                attributeDescriptors.add(attributeRegistry.getAttributeDescriptor(entry.getKey()));
                if (entry.getValue().required) {
                    requiredAttributes.add(entry.getKey());
                }
            }
            if (!requiredAttributes.isEmpty()) {
                MetacardValidator validator =
                        new RequiredAttributesMetacardValidator(metacardType.type,
                                requiredAttributes);
                // TODO (RCZ) - do we need any service properties?
                context.registerService(MetacardValidator.class, validator, null);
            }
            // TODO (RCZ) - What properties do we want?
            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put("name", metacardType.type);
            MetacardType type = new MetacardTypeImpl(metacardType.type, attributeDescriptors);
            context.registerService(MetacardType.class, type, properties);
        }
    }

    private void parseValidators(Map<String, List<Outer.Validator>> validators) {
        for (Map.Entry<String, List<Outer.Validator>> entry : validators.entrySet()) {
            List<AttributeValidator> attributeValidators = validatorFactory(entry.getValue());
            attributeValidatorRegistry.registerValidators(entry.getKey(), attributeValidators);
        }
    }

    private List<AttributeValidator> validatorFactory(List<Outer.Validator> validators) {
        return validators.stream()
                .filter(Objects::nonNull)
                .filter(v -> StringUtils.isNotBlank(v.validator))
                .map(this::getValidator)
                .collect(Collectors.toList());
    }

    private AttributeValidator getValidator(Outer.Validator validator) {
        switch (validator.validator) {
        case "size": {
            long lmin = Long.parseLong(validator.arguments.get(0));
            long lmax = Long.parseLong(validator.arguments.get(1));
            return new SizeValidator(lmin, lmax);
        }
        case "pattern": {
            String regex = validator.arguments.get(0);
            return new PatternValidator(regex);
        }
        case "pastdate": {
            return PastDateValidator.getInstance();
        }
        case "integerrange": {
            long lmin = Long.parseLong(validator.arguments.get(0));
            long lmax = Long.parseLong(validator.arguments.get(1));
            return new IntegerRangeValidator(lmin, lmax);
        }
        case "futuredate": {
            return FutureDateValidator.getInstance();
        }
        case "enumeration": {
            Set<String> values = new HashSet<>(validator.arguments);
            return new EnumerationValidator(values);
        }
        case "decimalrange": {
            double dmin = Double.parseDouble(validator.arguments.get(0));
            double dmax = Double.parseDouble(validator.arguments.get(1));
            if (validator.arguments.size() > 2) {
                double epsilon = Double.parseDouble(validator.arguments.get(2));
                return new DecimalRangeValidator(dmin, dmax, epsilon);
            }
            return new DecimalRangeValidator(dmin, dmax);
        }
        default:
            throw new IllegalStateException(
                    "Validator does not exist. (" + validator.validator + ")");
        }
    }

    private class Outer {
        List<MetacardType> metacardTypes;

        Map<String, AttributeType> attributeTypes;

        @JsonIgnore
        Map<String, List<Validator>> validators;

        class MetacardType {
            String type; // 'nitf'

            Map<String, MetacardAttribute> attributes;
        }

        class MetacardAttribute {
            boolean required;
        }

        class AttributeType {
            String type; // 'XML_TYPE',

            boolean tokenized;

            boolean stored;

            boolean indexed;

            boolean multivalued;
        }

        class Validator {
            String validator; // 'regex', 'length'

            List<String> arguments; // '(1,10)', '.*hi.*'
        }
    }

}
