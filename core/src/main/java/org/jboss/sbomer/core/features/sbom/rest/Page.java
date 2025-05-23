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
package org.jboss.sbomer.core.features.sbom.rest;

import java.util.Collection;
import java.util.Collections;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Collection REST response.
 *
 */
@Data
@AllArgsConstructor
@RegisterForReflection
public class Page<T> {

    /**
     * Page index.
     */
    private int pageIndex;

    /**
     * Number of records per page.
     */
    private int pageSize;

    /**
     * Total pages provided by this query or -1 if unknown.
     */
    private int totalPages;

    /**
     * Number of all hits (not only this page).
     */
    private long totalHits;

    /**
     * Embedded collection of data.
     */
    private Collection<T> content;

    public Page() {
        content = Collections.emptyList();
    }
}
