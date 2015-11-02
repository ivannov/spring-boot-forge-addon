package org.jboss.forge.addon.springboot.facet;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.springboot.spring.SpringBootMetadataRetriever;
import org.jboss.forge.addon.springboot.ui.SpringBootRepositoryType;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import static org.jboss.forge.addon.springboot.spring.SpringBootMetadataRetriever.*;

import javax.inject.Inject;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.IdClass;

/**
 * @author <a href="ivan.st.ivanov@gmail.com">Ivan St. Ivanov</a>
 */
public class SpringBootFacet extends AbstractFacet<Project> implements ProjectFacet {

    @Inject
    private DependencyInstaller dependencyInstaller;

    @Inject
    private SpringBootMetadataRetriever metadataRetriever;

    @Override
    public boolean install() {
        if (metadataRetriever.getSelectedSpringBootVersion() == null) {
            throw new IllegalStateException(
                    "First set the Spring Boot version before installing the facet.");
        }

        Parent parent = new Parent();
        parent.setGroupId(SPRING_BOOT_GROUP_ID);
        parent.setArtifactId(SPRING_BOOT_PARENT_ARTIFACT_ID);
        parent.setVersion(metadataRetriever.getSelectedSpringBootVersion());

        MavenFacet maven = getFaceted().getFacet(MavenFacet.class);
        Model model = maven.getModel();
        model.setParent(parent);
        maven.setModel(model);

        dependencyInstaller.install(getFaceted(), getStarterWebDependency());

        return true;
    }

    @Override
    public boolean isInstalled() {
        MavenFacet maven = getFaceted().getFacet(MavenFacet.class);
        return isParentSpringBoot(maven.getModel().getParent()) && hasSpringBootWebStarterDependency();
    }

    private boolean isParentSpringBoot(Parent parent) {
        return parent != null && parent.getGroupId().equals(SPRING_BOOT_GROUP_ID) &&
                parent.getArtifactId().equals(SPRING_BOOT_PARENT_ARTIFACT_ID);
    }

    private boolean hasSpringBootWebStarterDependency() {
        return getFaceted().getFacet(DependencyFacet.class).getDirectDependency(
                getStarterWebDependency()) != null;
    }

    private DependencyBuilder getStarterWebDependency() {
        return DependencyBuilder
                    .create()
                    .setGroupId(SPRING_BOOT_GROUP_ID)
                    .setArtifactId(SPRING_BOOT_STARTER_WEB_ARTIFACT_ID);
    }

    public JavaInterfaceSource exportRepositoryForEntity(JavaInterfaceSource source,
            JavaClassSource entityClass, SpringBootRepositoryType repositoryType) {
        String entytPkQualifiedName = null;
        String simplePkName = null;
        if (entityClass.hasAnnotation(IdClass.class)) {
            // AnnotationSource<JavaClassSource> idClassAnotation = entityClass
            // .getAnnotation(IdClass.class);
            // String valueClassName = idClassAnotation.getLiteralValue();

            Results.fail("Entites with IdClass primary key are not supported!");
        } else {
            for (FieldSource<JavaClassSource> field : entityClass.getFields()) {
                if (field.hasAnnotation(Id.class)
                        || field.hasAnnotation(EmbeddedId.class)) {
                    Type<JavaClassSource> fieldType = field.getType();

                    entytPkQualifiedName = fieldType.getQualifiedName();
                    simplePkName = fieldType.getSimpleName();

                    break;
                }
            }
        }

        if (entytPkQualifiedName == null) {
            Results.fail("Unable to detect entity primary key class type!");
        }

        switch (repositoryType) {
        case CRUD:
            source.addImport("org.springframework.data.repository.CrudRepository");
            source.addInterface("CrudRepository<" + entityClass.getName() + ","
                    + simplePkName + ">");
            break;
        case PAGING_AND_SORTING:
            source.addImport("org.springframework.data.repository.PagingAndSortingRepository");
            source.addInterface("PagingAndSortingRepository<"
                    + entityClass.getName() + "," + simplePkName + ">");
            break;
        }

        source.addImport(entytPkQualifiedName);
        source.addImport(entityClass.getQualifiedName());

        return source;
    }

}
