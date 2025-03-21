package app.demoarchunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * 컨트롤러 계층의 아키텍처 규칙을 검증하는 테스트 클래스입니다.
 * 스텝 1: 기본 구조 및 명명 규칙 검증
 */
public class ControllerArchitectureTest {
    private JavaClasses importedClasses;

    @BeforeEach
    public void setup() {
        // 모든 컨트롤러 클래스를 스캔합니다
        importedClasses = new ClassFileImporter().importPackages("app.demoarchunit");
    }

    /**
     * 컨트롤러 클래스 명명 규칙을 검증합니다.
     * 컨트롤러 애노테이션이 있는 모든 클래스는 이름이 'Controller'로 끝나야 합니다.
     */
    @Test
    public void 컨트롤러_클래스_명명_규칙_검증() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class).or().areAnnotatedWith(Controller.class)
                .should().haveSimpleNameEndingWith("Controller")
                .because("컨트롤러 클래스는 명확한 식별을 위해 'Controller'로 끝나야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 컨트롤러 패키지 구조를 검증합니다.
     * 모든 컨트롤러 클래스는 '.controller' 또는 '.api' 패키지에 위치해야 합니다.
     */
    @Test
    public void 컨트롤러_패키지_구조_검증() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class).or().areAnnotatedWith(Controller.class)
                .should().resideInAnyPackage("..controller..", "..api..")
                .because("컨트롤러 클래스는 구조적 일관성을 위해 '.controller' 또는 '.api' 패키지에 위치해야 합니다");

        rule.check(importedClasses);
    }

    /**
     * REST 애노테이션 검증을 수행합니다.
     * 'Controller'로 끝나는 모든 클래스는 @RestController 또는 @Controller 애노테이션을 가져야 합니다.
     */
    @Test
    public void REST_애노테이션_검증() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(RestController.class).orShould().beAnnotatedWith(Controller.class)
                .because("컨트롤러 클래스는 @RestController 또는 @Controller 애노테이션으로 명확히 식별되어야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 요청 매핑 검증을 수행합니다.
     * 모든 컨트롤러 클래스는 클래스 수준에서 @RequestMapping 애노테이션을 가져야 합니다.
     */
    @Test
    public void 요청_매핑_검증() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class).or().areAnnotatedWith(Controller.class)
                .should().beAnnotatedWith(RequestMapping.class)
                .because("컨트롤러 클래스는 기본 경로 정의를 위해 클래스 수준의 @RequestMapping을 가져야 합니다");

        rule.check(importedClasses);
    }

    /**
     * HTTP 메서드 애노테이션 검증을 수행합니다.
     * 컨트롤러의 public 메서드는 @GetMapping, @PostMapping 등의 HTTP 메서드 애노테이션을 가져야 합니다.
     */
    @Test
    public void HTTP_메서드_애노테이션_검증() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Controller.class)
                .and().arePublic()
                .and().areNotStatic()
                .should().beAnnotatedWith(GetMapping.class)
                .orShould().beAnnotatedWith(PostMapping.class)
                .orShould().beAnnotatedWith(PutMapping.class)
                .orShould().beAnnotatedWith(DeleteMapping.class)
                .orShould().beAnnotatedWith(PatchMapping.class)
                .orShould().beAnnotatedWith(RequestMapping.class)
                .because("컨트롤러의 public 메서드는 HTTP 메서드 애노테이션(@GetMapping, @PostMapping 등)을 가져야 합니다");

        rule.check(importedClasses);
    }


    /**
     * 계층 의존성 방향을 검증합니다.
     * 컨트롤러는 서비스에 의존할 수 있지만, 리포지토리에 직접 접근해서는 안 됩니다.
     */
    @Test
    public void 컨트롤러_계층_의존성_검증() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith(RestController.class).or().areAnnotatedWith(Controller.class)
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .because("컨트롤러는 리포지토리에 직접 접근하지 않고 서비스 계층을 통해 접근해야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 도메인 모델(엔티티) 노출 방지를 검증합니다.
     * 컨트롤러 메서드는 도메인 엔티티를 직접 반환하지 않고 DTO나 ResponseEntity를 사용해야 합니다.
     */
    @Test
    public void 도메인_모델_노출_방지_검증() {
        // 도메인 엔티티 클래스 정의 (JPA @Entity 애노테이션이 있는 클래스)
        DescribedPredicate<JavaClass> isEntityClass = new DescribedPredicate<>("JPA Entity 클래스") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.isAnnotatedWith("jakarta.persistence.Entity");
            }
        };

        // 컨트롤러 메서드가 엔티티를 직접 반환하지 않아야 함
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Controller.class)
                .should(notReturnDomainEntities(isEntityClass))
                .because("컨트롤러 메서드는 도메인 엔티티를 직접 반환하지 않고 DTO나 ResponseEntity를 사용해야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 응답 래핑 일관성을 검증합니다.
     * 컨트롤러 메서드는 일관된 응답 형식(ResponseEntity)을 사용해야 합니다.
     */
    @Test
    public void 응답_래핑_일관성_검증() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                .and().doNotHaveRawReturnType(void.class)
                .should().haveRawReturnType(ResponseEntity.class)
                .because("REST 컨트롤러의 메서드는 일관된 응답 구조를 위해 ResponseEntity를 반환해야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 요청 검증 적용을 확인합니다.
     *
     * @RequestBody가 붙은 파라미터에는 @Valid 애노테이션이 있어야 합니다.
     */
    @Test
    public void 요청_검증_적용_확인() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Controller.class)
                .should(validateRequestBodies())
                .because("@RequestBody 파라미터는 @Valid 애노테이션으로 검증되어야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 경로 변수 명시적 지정을 확인합니다.
     *
     * @PathVariable 애노테이션은 항상 명시적으로 변수명을 지정해야 합니다.
     */
    @Test
    public void 경로_변수_명시적_지정_확인() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Controller.class)
                .should(specifyPathVariableName())
                .because("@PathVariable 애노테이션은 명시적으로 변수명을 지정해야 합니다(예: @PathVariable(\"id\"))");

        rule.check(importedClasses);
    }

    // 커스텀 조건 메소드들

    /**
     * 컨트롤러 메서드가 도메인 엔티티를 직접 반환하지 않는지 확인하는 커스텀 조건입니다.
     */
    private ArchCondition<JavaMethod> notReturnDomainEntities(DescribedPredicate<JavaClass> isEntityClass) {
        return new ArchCondition<>("도메인 엔티티를 직접 반환하지 않아야 함") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                JavaClass returnType = method.getRawReturnType();

                // 리스트 또는 컬렉션 타입인 경우 제네릭 타입 파라미터를 확인
                if (returnType.getName().startsWith("java.util.List") ||
                        returnType.getName().startsWith("java.util.Collection") ||
                        returnType.getName().startsWith("java.util.Set")) {
                    // 현재 ArchUnit의 한계로 제네릭 타입 파라미터를 직접 확인하기 어려움
                    // 이상적으로는 여기서 제네릭 타입 파라미터가 엔티티인지 확인해야 함
                    return;
                }

                // ResponseEntity인 경우 제네릭 타입 파라미터를 확인 (ArchUnit 한계로 직접 확인 어려움)
                if (returnType.getName().equals(ResponseEntity.class.getName())) {
                    return;
                }

                // 반환 타입이 엔티티인 경우 위반
                if (isEntityClass.test(returnType)) {
                    events.add(SimpleConditionEvent.violated(
                            method,
                            method.getFullName() + "는 도메인 엔티티 " + returnType.getSimpleName() + "를 직접 반환합니다"
                    ));
                } else {
                    events.add(SimpleConditionEvent.satisfied(method, "도메인 엔티티를 직접 반환하지 않습니다"));
                }
            }
        };
    }

    /**
     * @RequestBody 파라미터에 @Valid 애노테이션이 있는지 확인하는 커스텀 조건입니다.
     */
    private ArchCondition<JavaMethod> validateRequestBodies() {
        return new ArchCondition<>("@RequestBody 파라미터에 @Valid 애노테이션 확인") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean hasRequestBody = method.getParameters().stream()
                        .anyMatch(param -> param.isAnnotatedWith(RequestBody.class));

                // @RequestBody가 없으면 검사 대상이 아님
                if (!hasRequestBody) {
                    events.add(SimpleConditionEvent.satisfied(method, "이 메서드는 @RequestBody를 사용하지 않습니다"));
                    return;
                }

                boolean allRequestBodiesValidated = method.getParameters().stream()
                        .filter(param -> param.isAnnotatedWith(RequestBody.class))
                        .allMatch(param -> param.isAnnotatedWith(Valid.class));

                if (!allRequestBodiesValidated) {
                    events.add(SimpleConditionEvent.violated(
                            method,
                            method.getFullName() + "는 @RequestBody 파라미터에 @Valid 애노테이션이 없습니다"
                    ));
                } else {
                    events.add(SimpleConditionEvent.satisfied(method, "모든 @RequestBody 파라미터에 @Valid 애노테이션이 있습니다"));
                }
            }
        };
    }

    /**
     * @PathVariable 애노테이션에 명시적으로 변수명이 지정되어 있는지 확인하는 커스텀 조건입니다.
     */
    private ArchCondition<JavaMethod> specifyPathVariableName() {
        return new ArchCondition<>("@PathVariable에 명시적 이름 지정") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean hasPathVariable = method.getParameters().stream()
                        .anyMatch(param -> param.isAnnotatedWith(PathVariable.class));

                // @PathVariable이 없으면 검사 대상이 아님
                if (!hasPathVariable) {
                    events.add(SimpleConditionEvent.satisfied(method, "이 메서드는 @PathVariable을 사용하지 않습니다"));
                    return;
                }

                boolean allPathVariablesNamed = method.getParameters().stream()
                        .filter(param -> param.isAnnotatedWith(PathVariable.class))
                        .allMatch(param -> {
                            PathVariable annotation = param.getAnnotationOfType(PathVariable.class);
                            // 어노테이션의 value 속성이 비어있지 않은지 확인
                            return !annotation.value().isEmpty() || !annotation.name().isEmpty();
                        });

                if (!allPathVariablesNamed) {
                    events.add(SimpleConditionEvent.violated(
                            method,
                            method.getFullName() + "의 @PathVariable 애노테이션에 명시적 이름이 지정되지 않았습니다"
                    ));
                } else {
                    events.add(SimpleConditionEvent.satisfied(method, "모든 @PathVariable에 명시적 이름이 지정되어 있습니다"));
                }
            }
        };
    }


    /**
     * 컨트롤러에서 트랜잭션 사용을 금지합니다.
     * 트랜잭션 관리는 서비스 계층의 책임이어야 합니다.
     */
    @Test
    public void 컨트롤러_트랜잭션_사용_금지() {
        ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Controller.class)
                .should().beAnnotatedWith(Transactional.class)
                .because("트랜잭션 관리는 컨트롤러가 아닌 서비스 계층의 책임입니다");

        rule.check(importedClasses);
    }

    /**
     * 컨트롤러 메서드의 명명 규칙을 검증합니다.
     * 메서드 이름은 HTTP 메서드와 리소스 동작을 명확하게 반영해야 합니다.
     */
    @Test
    public void 컨트롤러_메서드_명명_규칙_검증() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Controller.class)
                .and().arePublic()
                .should(followControllerMethodNamingConvention())
                .because("컨트롤러 메서드는 명확한 동사로 시작하는 명명 규칙을 따라야 합니다(예: getUser, createOrder)");

        rule.check(importedClasses);
    }

    /**
     * 필드 주입 사용을 금지합니다.
     * 컨트롤러에서는 생성자 주입 방식을 사용해야 합니다.
     */
    @Test
    public void 필드_주입_금지_검증() {
        ArchRule rule = noFields()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .or().areDeclaredInClassesThat().areAnnotatedWith(Controller.class)
                .should().beAnnotatedWith(Autowired.class)
                .because("필드 주입(@Autowired)은 금지되어 있으며, 생성자 주입을 사용해야 합니다");

        rule.check(importedClasses);
    }

    // 커스텀 조건 메소드들

    /**
     * 컨트롤러 메서드가 명명 규칙을 따르는지 확인하는 커스텀 조건입니다.
     */
    private ArchCondition<JavaMethod> followControllerMethodNamingConvention() {
        return new ArchCondition<>("컨트롤러 메서드 명명 규칙을 따름") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                String methodName = method.getName();
                // 허용되는 동사 접두사 목록
                String[] allowedPrefixes = {"get", "find", "retrieve", "create", "add", "update", "modify",
                        "delete", "remove", "process", "handle", "execute", "perform"};

                boolean startsWithVerb = Arrays.stream(allowedPrefixes)
                        .anyMatch(prefix -> methodName.startsWith(prefix));

                // camelCase 확인
                boolean isCamelCase = Character.isLowerCase(methodName.charAt(0)) &&
                        !methodName.contains("_") &&
                        methodName.matches("^[a-z][a-zA-Z0-9]*$");

                if (!startsWithVerb || !isCamelCase) {
                    events.add(SimpleConditionEvent.violated(
                            method,
                            "메서드 이름 '" + methodName + "'은(는) 동사로 시작하는 camelCase 형태가 아닙니다"
                    ));
                } else {
                    events.add(SimpleConditionEvent.satisfied(method, "메서드 이름이 명명 규칙을 따릅니다"));
                }
            }
        };
    }
}
