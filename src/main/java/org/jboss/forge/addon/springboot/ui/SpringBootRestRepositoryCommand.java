/*
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
package org.jboss.forge.addon.springboot.ui;

import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import javax.inject.Inject;

public class SpringBootRestRepositoryCommand extends AbstractProjectCommand {

   @Override
   public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(SpringBootRestRepositoryCommand.class)
                       .name("Spring Boot: REST Repository")
                       .category(Categories.create("Spring Boot"));
   }

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    @WithAttributes(label = "Repository", required = true)
    private UIInput<JavaResource> repository;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        repository.getFacet(HintsFacet.class).setInputType(InputType.JAVA_CLASS_PICKER);
        Object selection = builder.getUIContext().getInitialSelection().get();
        if (selection instanceof JavaResource)
            repository.setDefaultValue((JavaResource) selection);
        builder.add(repository);
    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        Dependency dependency = DependencyBuilder.create("org.springframework.data").setArtifactId("spring-data-rest-core");
        dependencyInstaller.install(getSelectedProject(uiExecutionContext), dependency);

        JavaResource javaResource = repository.getValue().reify(JavaResource.class);
        JavaInterfaceSource repositorySource = javaResource.getJavaType();
        final String annotation = "org.springframework.data.rest.core.annotation.RepositoryRestResource";
        if (!repositorySource.hasAnnotation(annotation)) {
            repositorySource.addAnnotation(annotation);
        }
        javaResource.setContents(repositorySource);
        return Results.success();
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Inject
    private ProjectFactory projectFactory;

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }
}
