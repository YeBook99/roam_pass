package com.roampass;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.springframework.transaction.annotation.Transactional;

// ARCHITECTURE.md §4·§6·§15의 규칙을 빌드마다 검사한다 (§16)
// 실패하면 테스트가 아니라 코드를 고친다. 규칙을 바꿔야 하면 ARCHITECTURE.md를 먼저 고친다
@AnalyzeClasses(packages = ArchitectureTest.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String BASE = "com.roampass";

    private static final String CONTROLLER = BASE + ".*.controller..";
    private static final String SERVICE = BASE + ".*.service..";
    private static final String REPOSITORY = BASE + ".*.repository..";
    private static final String DOMAIN = BASE + ".*.domain..";
    private static final String DTO = BASE + ".*.dto..";
    private static final String INFRA = BASE + ".infra..";
    private static final String GLOBAL = BASE + ".global..";

    // §4-1~4: 위에서 아래로만, 계층 건너뛰기 금지, Controller는 Entity를 다루지 않음, domain은 다른 계층을 모름
    @ArchTest
    static final ArchRule 계층_접근_규칙 = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Controller").definedBy(CONTROLLER)
            .layer("Service").definedBy(SERVICE)
            .layer("Repository").definedBy(REPOSITORY)
            .layer("Domain").definedBy(DOMAIN)
            .layer("Dto").definedBy(DTO)
            .layer("Infra").definedBy(INFRA)
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Repository", "Dto")
            .whereLayer("Dto").mayOnlyBeAccessedByLayers("Controller", "Service", "Repository")
            .whereLayer("Infra").mayOnlyBeAccessedByLayers("Service");

    // §4-5: 다른 기능의 데이터는 그 기능의 Service로 바꾼다
    @ArchTest
    static final ArchRule 다른_기능의_repository_사용_금지 = ArchRuleDefinition.classes()
            .that().resideInAPackage(SERVICE)
            .should(useOnlyRepositoriesOfOwnFeature())
            .allowEmptyShould(true);

    // §4-6: 기능 사이 순환 의존 금지
    @ArchTest
    static final ArchRule 기능_사이_순환_금지 = slices()
            .matching(BASE + ".(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    // §4-7: global은 기능을 모른다
    @ArchTest
    static final ArchRule global은_기능에_의존하지_않음 = noClasses()
            .that().resideInAPackage(GLOBAL)
            .should().dependOnClassesThat(resideInAPackage(BASE + "..").and(resideOutsideOfPackage(GLOBAL)))
            .allowEmptyShould(true);

    // §2·§3: 문서에 없는 계층·잡동사니 패키지 금지
    @ArchTest
    static final ArchRule 문서에_없는_패키지_금지 = noClasses()
            .should().resideInAnyPackage("..facade..", "..manager..", "..util..", "..helper..", "..common..", "..api..")
            .allowEmptyShould(true);

    // §6.1-6: 서비스는 인터페이스로 부른다. service 패키지의 *Service는 인터페이스이고, 구현은 *ServiceImpl이다
    @ArchTest
    static final ArchRule 서비스는_인터페이스 = ArchRuleDefinition.classes()
            .that().resideInAPackage(SERVICE).and().haveSimpleNameEndingWith("Service")
            .should().beInterfaces()
            .allowEmptyShould(true);

    // §6.1-6: 구현(*ServiceImpl)은 자기 자신 말고 아무도 직접 쓰지 않는다. 컨트롤러·다른 기능·스케줄러·다른 구현도 인터페이스만 안다
    //     (구현 객체는 Spring이 만들어 인터페이스 자리에 넣어 준다)
    @ArchTest
    static final ArchRule 서비스_구현을_직접_쓰지_않음 = ArchRuleDefinition.classes()
            .that().resideInAPackage(BASE + "..")
            .should(notUseOtherServiceImpl())
            .allowEmptyShould(true);

    // §6.1-6: 구현(*ServiceImpl)은 service 아래 impl 폴더에만 둔다. service 폴더에는 인터페이스가 모여 있다
    @ArchTest
    static final ArchRule 구현은_impl_폴더에 = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("ServiceImpl")
            .should().resideInAPackage(BASE + ".*.service.impl")
            .allowEmptyShould(true);

    // §6.1-6: @Service는 구현 클래스에만 붙고, 구현 이름은 *ServiceImpl이다
    @ArchTest
    static final ArchRule Service_애노테이션은_구현에만 = ArchRuleDefinition.classes()
            .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
            .should().haveSimpleNameEndingWith("ServiceImpl")
            .allowEmptyShould(true);

    // §6.1-1·3: @Transactional은 Service 메서드에만
    @ArchTest
    static final ArchRule Transactional은_service_메서드에만 = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage(SERVICE)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule 클래스_레벨_Transactional_금지 = noClasses()
            .should().beAnnotatedWith(Transactional.class)
            .orShould().beAnnotatedWith(jakarta.transaction.Transactional.class)
            .allowEmptyShould(true);

    // §15-6: 생성자 주입만
    @ArchTest
    static final ArchRule 필드_주입_금지 = NO_CLASSES_SHOULD_USE_FIELD_INJECTION.allowEmptyShould(true);

    private static ArchCondition<JavaClass> notUseOtherServiceImpl() {
        return new ArchCondition<>("다른 서비스 구현(*ServiceImpl)을 직접 쓰지 않는다") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    boolean self = origin.getName().equals(target.getName())
                            || origin.getName().startsWith(target.getName() + "$");   // 구현 안의 내부 record 등
                    if (target.getSimpleName().endsWith("ServiceImpl") && !self) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> useOnlyRepositoriesOfOwnFeature() {
        return new ArchCondition<>("자기 기능의 repository만 사용한다") {
            @Override
            public void check(JavaClass service, ConditionEvents events) {
                String ownFeature = featureOf(service);
                for (Dependency dependency : service.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    if (isRepository(target) && !ownFeature.equals(featureOf(target))) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    // {base}.{feature}.{layer}... 에서 feature 부분
    private static String featureOf(JavaClass javaClass) {
        String[] segments = relativeSegments(javaClass);
        return segments.length > 0 ? segments[0] : "";
    }

    private static boolean isRepository(JavaClass javaClass) {
        String[] segments = relativeSegments(javaClass);
        return segments.length > 1 && segments[1].equals("repository");
    }

    private static String[] relativeSegments(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(BASE + ".")) {
            return new String[0];
        }
        return packageName.substring(BASE.length() + 1).split("\\.");
    }
}
