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
package org.jboss.sbomer.service.rest.api.v1alpha3;

import static org.jboss.sbomer.service.feature.sbom.UserRoles.USER_DELETE_ROLE;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.config.request.PncAnalysisRequestConfig;
import org.jboss.sbomer.core.config.request.PncBuildRequestConfig;
import org.jboss.sbomer.core.config.request.PncOperationRequestConfig;
import org.jboss.sbomer.core.dto.BaseSbomGenerationRequestRecord;
import org.jboss.sbomer.core.dto.BaseSbomRecord;
import org.jboss.sbomer.core.dto.v1alpha3.SbomGenerationRequestRecord;
import org.jboss.sbomer.core.dto.v1alpha3.SbomRecord;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.config.DeliverableAnalysisConfig;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.model.Stats;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.rest.RestUtils;
import org.jboss.sbomer.service.rest.mapper.V1Alpha3Mapper;
import org.jboss.sbomer.service.stats.StatsService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import io.opentelemetry.api.trace.Span;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;

@Path("/api/v1alpha3")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "v1alpha3", description = "(deprecated)")
@PermitAll
@Slf4j
@Deprecated(since = "1.0.0", forRemoval = true)
public class ApiV1Alpha3 {
    @Inject
    V1Alpha3Mapper mapper;

    @Inject
    SbomService sbomService;

    @Inject
    FeatureFlags featureFlags;

    @Inject
    StatsService statsService;

    private SbomGenerationRequest doGetSbomGenerationRequestById(String generationRequestId) {
        SbomGenerationRequest generationRequest = SbomGenerationRequest.findById(generationRequestId); // NOSONAR

        if (generationRequest == null) {
            throw new NotFoundException("Generation request with id '{}' could not be found", generationRequestId);
        }

        return generationRequest;
    }

    private Sbom doGetSbomByPurl(String purl) {
        Sbom sbom = sbomService.findByPurl(purl);

        if (sbom == null) {
            throw new NotFoundException("Manifest with provided identifier: '" + purl + "' couldn't be found");
        }

        return sbom;
    }

    private Sbom doGetSbomById(String sbomId) {
        Sbom sbom = sbomService.get(sbomId);

        if (sbom == null) {
            throw new NotFoundException("Manifest with provided identifier: '{}' couldn't be found", sbomId);
        }

        return sbom;
    }

    private JsonNode doGetBomById(String sbomId) {
        Sbom sbom = doGetSbomById(sbomId);
        return sbom.getSbom();
    }

    private JsonNode doGetBomByPurl(String purl) {
        Sbom sbom = doGetSbomByPurl(purl);
        return sbom.getSbom();
    }

    @GET
    @Path("/sboms/requests")
    @Operation(
            summary = "List SBOM generation requests",
            description = "Paginated list of SBOM generation requests using RSQL advanced search.")
    @Parameter(
            name = "query",
            description = "A RSQL query to search the generation requests",
            examples = { @ExampleObject(
                    name = "Find all SBOM generation requests with provided identifier",
                    value = "identifier=eq=ABCDEFGHIJKLM") })
    @Parameter(
            name = "sort",
            description = "Optional RSQL sort",
            examples = { @ExampleObject(name = "Order generation requests by id in ascending order", value = "id=asc="),
                    @ExampleObject(
                            name = "Order generation requests by creation time in descending order",
                            value = "creationTime=desc=") })
    @APIResponse(
            responseCode = "200",
            description = "List of SBOM generation requests in the system for a specified RSQL query.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "400",
            description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Page<BaseSbomGenerationRequestRecord> searchGenerationRequests(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {
        Page<SbomGenerationRequest> requests = sbomService.searchSbomRequestsByQueryPaginated(
                paginationParams.getPageIndex(),
                paginationParams.getPageSize(),
                rsqlQuery,
                sort);
        return mapper.requestsToBaseRecordPage(requests);
    }

    @GET
    @Path("/sboms/requests/{id}")
    @Operation(
            summary = "Get specific SBOM generation request",
            description = "Get specific SBOM generation request with the provided ID.")
    @Parameter(name = "id", description = "SBOM generation request identifier", example = "88CA2291D4014C6")
    @APIResponse(
            responseCode = "200",
            description = "The generation request",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "404",
            description = "Requested generation request could not be found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public SbomGenerationRequestRecord getGenerationRequestById(@PathParam("id") String generationRequestId) {
        return mapper.toRecord(doGetSbomGenerationRequestById(generationRequestId));
    }

    @GET
    @Path("/sboms")
    @Operation(summary = "Search SBOMs", description = "List paginated SBOMs using RSQL advanced search.")
    @Parameter(
            name = "query",
            description = "A RSQL query to search the SBOMs",
            examples = {
                    @ExampleObject(name = "Find all SBOMs with provided buildId", value = "buildId=eq=ABCDEFGHIJKLM"),
                    @ExampleObject(
                            name = "Find all SBOMs with provided purl",
                            value = "rootPurl=eq='pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00270?type=jar'") })
    @Parameter(
            name = "sort",
            description = "Optional RSQL sort",
            examples = { @ExampleObject(name = "Order SBOMs by id in ascending order", value = "id=asc="),
                    @ExampleObject(
                            name = "Order SBOMs by creation time in descending order",
                            value = "creationTime=desc=") })
    @APIResponse(responseCode = "200", description = "List of SBOMs in the system for a specified RSQL query.")
    @APIResponse(
            responseCode = "400",
            description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Page<BaseSbomRecord> searchSboms(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        return sbomService.searchSbomRecordsByQueryPaginated(
                paginationParams.getPageIndex(),
                paginationParams.getPageSize(),
                rsqlQuery,
                sort);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(
            summary = "Generate SBOM based on the PNC build",
            description = "SBOM base generation for a particular PNC build Id offloaded to the service.")
    @Parameter(name = "buildId", description = "PNC build identifier", example = "ARYT3LBXDVYAC")
    @Path("/sboms/generate/build/{buildId}")
    @APIResponse(
            responseCode = "202",
            description = "Schedules generation of a SBOM for a particular PNC buildId. This is an asynchronous call. It does execute the generation behind the scenes.",
            content = @Content(schema = @Schema(implementation = SbomGenerationRequestRecord.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response generate(
            @PathParam("buildId") String buildId,
            PncBuildConfig config,
            @Context ContainerRequestContext requestContext) throws Exception {
        if (featureFlags.isDryRun()) {
            log.warn(
                    "Skipping creating new Generation Request for buildId '{}' because of SBOMer running in dry-run mode",
                    buildId);
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }

        // Create the Request to be associated with this REST API call event
        PncBuildRequestConfig pncBuildRequestConfig = PncBuildRequestConfig.builder().withBuildId(buildId).build();
        RequestEvent request = RestUtils
                .createRequestFromRestEvent(pncBuildRequestConfig, requestContext, Span.current());

        return Response.accepted(mapper.toRecord(sbomService.generateFromBuild(request, pncBuildRequestConfig, config)))
                .build();
    }

    @GET
    @Path("/sboms/{id}")
    @Operation(summary = "Get specific SBOM", description = "Get specific SBOM with the provided ID.")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @APIResponse(
            responseCode = "200",
            description = "The SBOM",
            content = @Content(schema = @Schema(implementation = SbomRecord.class)))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Requested SBOM could not be found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SbomRecord getSbomById(@PathParam("id") String sbomId) {
        return mapper.toRecord(doGetSbomById(sbomId));
    }

    @GET
    @Path("/sboms/{id}/bom")
    @Operation(
            summary = "Get the BOM content of particular SBOM",
            description = "Get the BOM content of particular SBOM")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @APIResponse(
            responseCode = "200",
            description = "The BOM in CycloneDX format",
            content = @Content(schema = @Schema(implementation = Map.class)))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Requested SBOM could not be found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public JsonNode getBomById(@PathParam("id") String sbomId) {
        return doGetBomById(sbomId);
    }

    @GET
    @Path("/sboms/purl/{purl}")
    @Operation(summary = "Get specific SBOM", description = "Find latest generated SBOM for a given purl.")
    @Parameter(
            name = "purl",
            description = "Package URL identifier",
            example = "scheme:type/namespace/name@version?qualifiers#subpath")
    @APIResponse(responseCode = "200", description = "The SBOM")
    @APIResponse(responseCode = "400", description = "Could not parse provided arguments")
    @APIResponse(responseCode = "404", description = "Requested SBOM could not be found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public SbomRecord getSbomByPurl(@PathParam("purl") String purl) {
        return mapper.toRecord(doGetSbomByPurl(purl));
    }

    @GET
    @Path("/sboms/purl/{purl}/bom")
    @Operation(
            summary = "Get the BOM content of particular SBOM identified by provided purl",
            description = "Returns the CycloneDX BOM content of particular SBOM identified by provided purl")
    @Parameter(
            name = "purl",
            description = "Package URL identifier",
            example = "scheme:type/namespace/name@version?qualifiers#subpath")
    @APIResponse(
            responseCode = "200",
            description = "The BOM in CycloneDX format",
            content = @Content(schema = @Schema(implementation = Map.class)))
    @APIResponse(responseCode = "400", description = "Could not parse provided arguments")
    @APIResponse(responseCode = "404", description = "Requested SBOM could not be found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public JsonNode getBomByPurl(@PathParam("purl") String purl) {
        return doGetBomByPurl(purl);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(
            summary = "Generate SBOM based on the PNC Deliverable Analysis operation",
            description = "SBOM base generation for a particular Deliverable Analysis operation Id.")
    @Parameter(
            name = "operationId",
            description = "PNC Deliverable Analysis operation identifier",
            example = "A5WL3DFZ3AIAA")
    @Path("/sboms/generate/operation/{operationId}")
    @APIResponse(
            responseCode = "202",
            description = "Schedules generation of a SBOM for a particular PNC operationId. This is an asynchronous call. It does execute the generation behind the scenes.",
            content = @Content(schema = @Schema(implementation = SbomGenerationRequestRecord.class)))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response generateFromOperation(
            @PathParam("operationId") String operationId,
            OperationConfig config,
            @Context ContainerRequestContext requestContext) {

        if (config == null) {
            config = new OperationConfig();
        }

        config.setOperationId(operationId);

        // Create the Request to be associated with this REST API call event
        RequestEvent request = RestUtils.createRequestFromRestEvent(
                PncOperationRequestConfig.builder().withOperationId(operationId).build(),
                requestContext,
                Span.current());
        return Response.accepted(mapper.toRecord(sbomService.generateFromOperation(request, config))).build();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @Operation(
            summary = "Triggers a PNC Deliverable Analysis using the provided information",
            description = "Triggers a PNC Deliverable Analysis using the provided information.")
    @Path("/sboms/generate/analysis")
    @APIResponse(
            responseCode = "202",
            description = "Schedules a new PNC operation of type deliverable analysis. This is an asynchronous call. PNC will start the deliverable analysis from the provided configuration and will notify the end of the analysis asynchronously.",
            content = @Content(schema = @Schema(implementation = SbomGenerationRequestRecord.class)))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response generateNewOperation(
            DeliverableAnalysisConfig config,
            @Context ContainerRequestContext requestContext) {

        if (config == null) {
            config = new DeliverableAnalysisConfig();
        }

        // Create the Request to be associated with this REST API call event
        RequestEvent request = RestUtils.createRequestFromRestEvent(
                PncAnalysisRequestConfig.builder()
                        .withMilestoneId(config.getMilestoneId())
                        .withUrls(config.getDeliverableUrls())
                        .build(),
                requestContext,
                Span.current());

        return Response.accepted(mapper.toRecord(sbomService.generateNewOperation(request, config))).build();
    }

    @GET
    @Path("/stats")
    @Operation(summary = "Retrieve service runtime information", description = "Service information and statistics.")
    @APIResponse(responseCode = "200", description = "Available runtime information")
    public Stats getStats() {
        return statsService.getStats();
    }

    @DELETE
    @Consumes(MediaType.WILDCARD)
    @Path("/sboms/requests/{id}")
    @RolesAllowed(USER_DELETE_ROLE)
    @Operation(
            summary = "Delete SBOM generation request specified by id",
            description = "Delete the specified SBOM generation request from the database")
    @Parameter(name = "id", description = "The SBOM request identifier")
    @APIResponse(responseCode = "200", description = "SBOM generation request was successfully deleted")
    @APIResponse(responseCode = "404", description = "Specified SBOM generation request could not be found")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response deleteGenerationRequest(@PathParam("id") final String id) {

        try {
            MDCUtils.addIdentifierContext(id);
            sbomService.deleteSbomRequest(id);

            return Response.ok().build();
        } finally {
            MDCUtils.removeIdentifierContext();
        }
    }

    @POST
    @Consumes(MediaType.WILDCARD)
    @Operation(
            summary = "Resend UMB notification message for a completed SBOM",
            description = "Force the resending of the UMB notification message for an already generated SBOM.")
    @Parameter(name = "id", description = "SBOM identifier", example = "429305915731435500")
    @Path("/sboms/{id}/notify")
    @APIResponse(responseCode = "200")
    @APIResponse(
            responseCode = "404",
            description = "Requested SBOM could not be found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    public Response notify(@PathParam("id") String sbomId) {
        if (featureFlags.isDryRun()) {
            log.warn("Skipping notification for SBOM '{}' because of SBOMer running in dry-run mode", sbomId);
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }

        Sbom sbom = doGetSbomById(sbomId);
        sbomService.notifyCompleted(sbom);
        return Response.ok().build();
    }
}
