/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.core.features.sbom.config.runtime;

import java.util.Arrays;
import java.util.List;

import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@Jacksonized
@JsonTypeName("default")
public class DefaultProcessorConfig extends ProcessorConfig {

    public ProcessorType getType() {
        return ProcessorType.DEFAULT;

    }

    @Override
    public List<String> toCommand() {
        return Arrays.asList("default");
    }

}
