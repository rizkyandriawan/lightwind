package dev.kakrizky.lightwind.build.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import dev.kakrizky.lightwind.build.LayerInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quarkus build step processor for Lightwind.
 *
 * <p>Runs during Quarkus augmentation (build time) to:</p>
 * <ol>
 *   <li>Detect all LightEntity/LightCrudService/LightCrudResource subclasses</li>
 *   <li>Register app-specific entity and DTO classes for GraalVM reflection</li>
 *   <li>Produce a {@link LayerInfo} bean with discovered class counts</li>
 * </ol>
 */
public class LightwindProcessor {

    private static final Logger LOG = Logger.getLogger(LightwindProcessor.class);

    private static final String FEATURE = "lightwind";

    static final DotName LIGHT_ENTITY = DotName.createSimple("dev.kakrizky.lightwind.entity.LightEntity");
    static final DotName LIGHT_CRUD_SERVICE = DotName.createSimple("dev.kakrizky.lightwind.crud.LightCrudService");
    static final DotName LIGHT_CRUD_RESOURCE = DotName.createSimple("dev.kakrizky.lightwind.crud.LightCrudResource");

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerLayerInfoBean() {
        return AdditionalBeanBuildItem.unremovableOf(LayerInfo.class);
    }

    /**
     * Scans the Jandex index for all app-specific classes that extend
     * LightEntity, LightCrudService, or LightCrudResource. Registers
     * entity classes and their DTO type parameters for reflection
     * (needed for GraalVM native image).
     */
    @BuildStep
    LightwindClassesBuildItem scanLightwindClasses(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        IndexView index = combinedIndex.getIndex();

        // Discover entity subclasses
        Collection<ClassInfo> entities = index.getAllKnownSubclasses(LIGHT_ENTITY);
        Set<String> entityClassNames = new HashSet<>();
        Set<String> dtoClassNames = new HashSet<>();

        for (ClassInfo entity : entities) {
            String name = entity.name().toString();
            // Skip the framework base class itself
            if (name.startsWith("dev.kakrizky.lightwind.")) {
                continue;
            }
            entityClassNames.add(name);
            LOG.infof("Lightwind entity discovered: %s", name);

            // Extract DTO type parameter from LightEntity<E, D>
            extractDtoType(entity, index, dtoClassNames);
        }

        // Discover service subclasses
        Collection<ClassInfo> services = index.getAllKnownSubclasses(LIGHT_CRUD_SERVICE);
        Set<String> serviceClassNames = new HashSet<>();
        for (ClassInfo service : services) {
            String name = service.name().toString();
            if (name.startsWith("dev.kakrizky.lightwind.")) {
                continue;
            }
            serviceClassNames.add(name);
            LOG.infof("Lightwind service discovered: %s", name);
        }

        // Discover resource subclasses
        Collection<ClassInfo> resources = index.getAllKnownSubclasses(LIGHT_CRUD_RESOURCE);
        Set<String> resourceClassNames = new HashSet<>();
        for (ClassInfo resource : resources) {
            String name = resource.name().toString();
            if (name.startsWith("dev.kakrizky.lightwind.")) {
                continue;
            }
            resourceClassNames.add(name);
            LOG.infof("Lightwind resource discovered: %s", name);
        }

        // Register all discovered classes for reflection
        Set<String> allClasses = new HashSet<>();
        allClasses.addAll(entityClassNames);
        allClasses.addAll(dtoClassNames);
        allClasses.addAll(serviceClassNames);
        allClasses.addAll(resourceClassNames);

        if (!allClasses.isEmpty()) {
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                    allClasses.toArray(new String[0]))
                    .methods(true)
                    .fields(true)
                    .build());

            LOG.infof("Lightwind: registered %d classes for reflection " +
                            "(entities=%d, DTOs=%d, services=%d, resources=%d)",
                    allClasses.size(),
                    entityClassNames.size(), dtoClassNames.size(),
                    serviceClassNames.size(), resourceClassNames.size());
        }

        return new LightwindClassesBuildItem(
                entityClassNames, dtoClassNames,
                serviceClassNames, resourceClassNames);
    }

    /**
     * Walks the superclass chain to find LightEntity and extract the D (DTO)
     * type parameter from the parameterized superclass declaration.
     */
    private void extractDtoType(ClassInfo entity, IndexView index, Set<String> dtoClassNames) {
        ClassInfo current = entity;
        while (current != null) {
            Type superType = current.superClassType();
            if (superType == null) {
                break;
            }

            if (superType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                ParameterizedType parameterized = superType.asParameterizedType();
                if (parameterized.name().equals(LIGHT_ENTITY)) {
                    List<Type> args = parameterized.arguments();
                    if (args.size() >= 2) {
                        Type dtoType = args.get(1);
                        if (dtoType.kind() == Type.Kind.CLASS) {
                            String dtoName = dtoType.asClassType().name().toString();
                            dtoClassNames.add(dtoName);
                            LOG.infof("Lightwind DTO discovered: %s (from entity %s)",
                                    dtoName, entity.name());
                        }
                    }
                    return;
                }
            }

            // Walk up
            DotName superName = current.superName();
            if (superName == null || superName.equals(DotName.OBJECT_NAME)) {
                break;
            }
            current = index.getClassByName(superName);
        }
    }
}
