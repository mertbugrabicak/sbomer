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
package org.jboss.sbomer.cli.feature.sbom.client;

import static org.jboss.sbomer.core.rest.faulttolerance.Constants.SBOMER_CLIENT_DELAY;
import static org.jboss.sbomer.core.rest.faulttolerance.Constants.SBOMER_CLIENT_MAX_RETRIES;

import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.sbomer.cli.feature.sbom.model.Sbom;
import org.jboss.sbomer.cli.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.core.rest.faulttolerance.RetryLogger;
import org.jboss.sbomer.core.utils.PaginationParameters;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Client used to interact with the SBOMer REST API.
 */
@ApplicationScoped
@RegisterRestClient(configKey = "sbomer")
@Path("/api/v1beta1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SBOMerClient {

    /**
     * Retrieves SBOM based on the ID.
     *
     * @param id the identifier of the SBOM
     * @return the {@link Sbom SBOM}
     */
    @Retry(maxRetries = SBOMER_CLIENT_MAX_RETRIES, delay = SBOMER_CLIENT_DELAY, delayUnit = ChronoUnit.SECONDS)
    @BeforeRetry(RetryLogger.class)
    @ExponentialBackoff
    @GET
    @Path("/manifests/{id}")
    Response getById(@HeaderParam("log-process-context") String processContext, @PathParam("id") String id);

    /**
     * Retrieves SBOM Generation Request based on the ID.
     *
     * @param id the identifier of the SBOM Generation Request
     * @return {@link SbomGenerationRequest SBOM Generation Request}
     */
    @Retry(maxRetries = SBOMER_CLIENT_MAX_RETRIES, delay = SBOMER_CLIENT_DELAY, delayUnit = ChronoUnit.SECONDS)
    @BeforeRetry(RetryLogger.class)
    @ExponentialBackoff
    @GET
    @Path("/generations/{id}")
    Response getGenerationRequestById(
            @HeaderParam("log-process-context") String processContext,
            @PathParam("id") String id);

    /**
     * Search the base SBOM based via RSQL search and pagination.
     *
     * @param paginationParams the pagination parameters
     * @param rsqlQuery the RSQL query
     * @return {@link Response res}
     */
    @Retry(maxRetries = SBOMER_CLIENT_MAX_RETRIES, delay = SBOMER_CLIENT_DELAY, delayUnit = ChronoUnit.SECONDS)
    @BeforeRetry(RetryLogger.class)
    @ExponentialBackoff
    @GET
    @Path("/manifests")
    Response searchSboms(
            @HeaderParam("log-process-context") String processContext,
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @QueryParam("sort") String rsqlSort);

    /**
     * Search the generation requests via RSQL search and pagination.
     *
     * @param paginationParams the pagination parameters
     * @param rsqlQuery the RSQL query
     * @return {@link Response response}
     */
    @Retry(maxRetries = SBOMER_CLIENT_MAX_RETRIES, delay = SBOMER_CLIENT_DELAY, delayUnit = ChronoUnit.SECONDS)
    @BeforeRetry(RetryLogger.class)
    @ExponentialBackoff
    @GET
    @Path("/generations")
    Response searchGenerationRequests(
            @HeaderParam("log-process-context") String processContext,
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @QueryParam("sort") String rsqlSort);

    @Retry(maxRetries = SBOMER_CLIENT_MAX_RETRIES, delay = SBOMER_CLIENT_DELAY, delayUnit = ChronoUnit.SECONDS)
    @BeforeRetry(RetryLogger.class)
    @ExponentialBackoff
    @GET
    @Path("/stats")
    Response getStats();
}
