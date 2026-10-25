package br.com.cardapio.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Regras de dependência da arquitetura hexagonal (CLAUDE.md, "Regras de dependência").
 */
@AnalyzeClasses(packages = "br.com.cardapio", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "br.com.cardapio.domain..";
    private static final String APPLICATION = "br.com.cardapio.application..";
    private static final String ADAPTER = "br.com.cardapio.adapter..";
    private static final String CONFIG = "br.com.cardapio.config..";
    private static final String PERSISTENCE = "br.com.cardapio.adapter.out.persistence..";

    // Regra 1: o domínio só depende de Java.
    @ArchTest
    static final ArchRule domainDependsOnlyOnJava = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..")
            .as("1. domain não depende de nada do projeto nem de framework");

    // Regra 2: a aplicação só depende do domínio; única anotação de framework é @Transactional.
    @ArchTest
    static final ArchRule applicationDependsOnlyOnDomain = classes()
            .that().resideInAPackage(APPLICATION)
            .should().onlyDependOnClassesThat(
                    com.tngtech.archunit.base.DescribedPredicate.describe("Java, domain, application ou @Transactional",
                            c -> c.getPackageName().startsWith("java")
                                    || c.getPackageName().startsWith("br.com.cardapio.domain")
                                    || c.getPackageName().startsWith("br.com.cardapio.application")
                                    || c.getName().equals("org.springframework.transaction.annotation.Transactional")
                                    || c.getName().startsWith("org.springframework.transaction.annotation.Propagation")
                                    || c.getName().startsWith("org.springframework.transaction.annotation.Isolation")))
            .as("2. application depende só de domain (e de @Transactional)");

    @ArchTest
    static final ArchRule transactionalOnlyInUseCaseServices = methods()
            .that().areAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .and().areDeclaredInClassesThat().resideInAPackage(APPLICATION)
            .should().beDeclaredInClassesThat().resideInAPackage("br.com.cardapio.application.service..")
            .as("2. @Transactional só nos serviços de caso de uso");

    // Regra 3: adaptadores não se chamam diretamente.
    @ArchTest
    static final ArchRule inboundAdaptersDoNotUseOutboundAdapters = noClasses()
            .that().resideInAPackage("br.com.cardapio.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("br.com.cardapio.adapter.out..")
            .as("3. adaptador de entrada não chama adaptador de saída");

    @ArchTest
    static final ArchRule outboundAdaptersAreIndependent = slices()
            .matching("br.com.cardapio.adapter.out.(*)..")
            .should().notDependOnEachOther()
            .as("3. adaptadores de saída não se chamam");

    @ArchTest
    static final ArchRule adaptersDoNotDependOnConfig = noClasses()
            .that().resideInAPackage(ADAPTER)
            .should().dependOnClassesThat().resideInAPackage(CONFIG)
            .as("3. adaptadores não dependem de config");

    // Regra 4: controllers chamam portas de entrada.
    @ArchTest
    static final ArchRule webAdaptersUseInputPortsOnly = noClasses()
            .that().resideInAPackage("br.com.cardapio.adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "br.com.cardapio.application.service..",
                    "br.com.cardapio.application.port.out..",
                    "org.springframework.data..")
            .as("4. controllers chamam portas de entrada, nunca serviços concretos nem repositórios");

    // Regra 5: serviços de caso de uso não usam estereótipos do Spring.
    @ArchTest
    static final ArchRule useCasesAreNotSpringComponents = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().beAnnotatedWith("org.springframework.stereotype.Service")
            .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
            .as("5. serviços de caso de uso são registrados em config/, sem @Service/@Component");

    // Regra 6: JPA só no adaptador de persistência, e suas classes não vazam.
    @ArchTest
    static final ArchRule jpaOnlyInPersistenceAdapter = noClasses()
            .that().resideOutsideOfPackage(PERSISTENCE)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
            .as("6. JPA/Hibernate só em adapter/out/persistence");

    @ArchTest
    static final ArchRule persistenceClassesDoNotLeak = noClasses()
            .that().resideOutsideOfPackage(PERSISTENCE)
            .should().dependOnClassesThat().resideInAPackage(PERSISTENCE)
            .as("6. classes de persistência não saem de adapter/out/persistence");

    @ArchTest
    static final ArchRule entitiesLiveInPersistence = classes()
            .that().areAnnotatedWith("jakarta.persistence.Entity")
            .should().resideInAPackage(PERSISTENCE)
            .as("6. entidades JPA ficam em adapter/out/persistence");

    // Complemento: sem Lombok e sem Jackson no núcleo.
    @ArchTest
    static final ArchRule noLombokAnywhere = noClasses()
            .should().dependOnClassesThat().resideInAPackage("lombok..")
            .as("sem Lombok");
}
