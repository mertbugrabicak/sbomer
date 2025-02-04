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
package org.jboss.sbomer.service.feature.sbom.k8s.resources;

import java.util.Map;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.TektonConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDNoGCKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@KubernetesDependent(resourceDiscriminator = OperationInitResourceDiscriminator.class)
@Slf4j
public class TaskRunOperationInitDependentResource
        extends CRUDNoGCKubernetesDependentResource<TaskRun, GenerationRequest> {

    public static final String RESULT_NAME = "operation-config";
    public static final String PARAM_OPERATION_ID_NAME = "operation-id";
    public static final String PARAM_OPERATION_CONFIG_NAME = "config";
    public static final String TASK_NAME = "sbomer-operation-init";

    @Inject
    TektonConfig tektonConfig;

    TaskRunOperationInitDependentResource() {
        super(TaskRun.class);
    }

    public TaskRunOperationInitDependentResource(Class<TaskRun> resourceType) {
        super(TaskRun.class);
    }

    /**
     * <p>
     * Method that creates a {@link TaskRun} related to the {@link GenerationRequest} in order to perform the
     * initialization.
     * </p>
     *
     * <p>
     * This is done just one right after the {@link GenerationRequest} is created within the system.
     * </p>
     */
    @Override
    protected TaskRun desired(GenerationRequest generationRequest, Context<GenerationRequest> context) {

        log.debug(
                "Preparing dependent resource for the '{}' phase related to '{}'",
                SbomGenerationPhase.OPERATIONINIT,
                generationRequest.getMetadata().getName());

        Map<String, String> labels = Labels.defaultLabelsToMap(GenerationRequestType.OPERATION);

        labels.put(Labels.LABEL_IDENTIFIER, generationRequest.getIdentifier());
        labels.put(Labels.LABEL_PHASE, SbomGenerationPhase.OPERATIONINIT.name().toLowerCase());
        labels.put(Labels.LABEL_GENERATION_REQUEST_ID, generationRequest.getId());

        String configStr;

        try {
            configStr = ObjectMapperProvider.json().writeValueAsString(generationRequest.toOperationConfig());
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Could not serialize runtime operation configuration into JSON", e);
        }

        return new TaskRunBuilder().withNewMetadata()
                .withNamespace(generationRequest.getMetadata().getNamespace())
                .withLabels(labels)
                .withName(generationRequest.dependentResourceName(SbomGenerationPhase.OPERATIONINIT))
                .withOwnerReferences(
                        new OwnerReferenceBuilder().withKind(generationRequest.getKind())
                                .withName(generationRequest.getMetadata().getName())
                                .withApiVersion(generationRequest.getApiVersion())
                                .withUid(generationRequest.getMetadata().getUid())
                                .build())
                .endMetadata()
                .withNewSpec()
                .withServiceAccountName(tektonConfig.sa())
                .withParams(
                        new ParamBuilder().withName(PARAM_OPERATION_ID_NAME)
                                .withNewValue(generationRequest.getIdentifier())
                                .build(),
                        new ParamBuilder().withName(PARAM_OPERATION_CONFIG_NAME).withNewValue(configStr).build())
                .withTaskRef(new TaskRefBuilder().withName(TASK_NAME).build())
                .endSpec()
                .build();

    }
}
