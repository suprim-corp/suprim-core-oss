package sant1ago.dev.suprim.processor;

import sant1ago.dev.suprim.annotation.entity.Entity;
import sant1ago.dev.suprim.processor.model.EntityMetadata;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

/**
 * Annotation processor for generating type-safe metamodel classes.
 * Processes @Entity annotations and generates Entity_ classes with typed column references.
 */
@SupportedAnnotationTypes("sant1ago.dev.suprim.annotation.entity.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class SuprimProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;
    private MetadataExtractor extractor;
    private MetamodelGenerator generator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elementUtils = processingEnv.getElementUtils();
        this.extractor = new MetadataExtractor(elementUtils);
        this.generator = new MetamodelGenerator(filer);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "@Entity can only be applied to classes", element);
                continue;
            }

            try {
                processEntity((TypeElement) element);
            } catch (Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Failed to generate metamodel: " + e.getMessage(), element);
            }
        }

        return true; // Claim the annotations
    }

    private void processEntity(TypeElement entityElement) throws IOException {
        messager.printMessage(Diagnostic.Kind.NOTE,
                "Generating metamodel for: " + entityElement.getQualifiedName());

        EntityMetadata metadata = extractor.extract(entityElement);
        generator.generate(metadata);

        messager.printMessage(Diagnostic.Kind.NOTE,
                "Generated: " + metadata.getMetamodelClassName());
    }
}
