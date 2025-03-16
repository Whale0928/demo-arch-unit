package app.demoarchunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

public class ArchitectureTest {
    private JavaClasses importedClasses;

    @BeforeEach
    public void setup() {
        importedClasses = new ClassFileImporter().importPackages("app.demoarchunit");
    }

    @Test
    public void 계층형_아키텍처_의존성_방향_검증() {
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Repository").definedBy("..repository..")
                .layer("Domain").definedBy("..domain..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Controller", "Service", "Repository")
                .check(importedClasses);
    }

    @Test
    public void 도메인_모델은_외부_종속성을_가지지_않아야_함() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..", "..repository..")
                .because("도메인 모델은 다른 애플리케이션 계층에 의존하지 않아야 합니다");

        rule.check(importedClasses);
    }

    @Test
    public void 컨트롤러_클래스_명명_규칙_검증() {
        ArchRule rule = classes().that().resideInAPackage("..controller..")
                .should().haveSimpleNameEndingWith("Controller")
                .because("컨트롤러 클래스는 Controller로 끝나야 합니다");

        rule.check(importedClasses);
    }

    @Test
    public void 서비스_클래스_명명_규칙_검증() {
        ArchRule rule = classes().that().resideInAPackage("..service..")
                .should().haveSimpleNameEndingWith("Service")
                .because("서비스 클래스는 Service로 끝나야 합니다");

        rule.check(importedClasses);
    }

    @Test
    public void 리포지토리_클래스_명명_규칙_검증() {
        ArchRule rule = classes().that().resideInAPackage("..repository..")
                .should().haveSimpleNameEndingWith("Repository")
                .because("리포지토리 클래스는 Repository로 끝나야 합니다");

        rule.check(importedClasses);
    }

    @Test
    public void DTO_클래스는_서비스나_리포지토리에_의존하지_않아야_함() {
        ArchRule rule = noClasses().that().resideInAPackage("..model..")
                .should().dependOnClassesThat().resideInAnyPackage("..service..", "..repository..")
                .because("DTO 클래스는 서비스나 리포지토리에 의존해서는 안 됩니다");

        rule.check(importedClasses);
    }

    @Test
    public void 엔티티_클래스_애노테이션_규칙_검증() {
        ArchRule rule = classes().that().resideInAPackage("..domain..")
                .and().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().beAnnotatedWith(jakarta.persistence.Table.class)
                .because("모든 엔티티 클래스는 @Table 애노테이션을 가져야 합니다");

        rule.check(importedClasses);
    }

    @Test
    public void 트랜잭션_관리는_서비스_계층에서만_허용() {
        // 클래스에 @Transactional이 있는 경우 검사
        ArchRule classRule = classes()
                .that().areAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
                .should().resideInAPackage("..service..")
                .because("트랜잭션 관리는 서비스 계층에서만 허용됩니다");

        // 메소드에 @Transactional이 있는 경우 검사
        ArchRule methodRule = methods()
                .that().areAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
                .should().beDeclaredInClassesThat().resideInAPackage("..service..")
                .because("트랜잭션 메소드는 서비스 계층에서만 허용됩니다");

        classRule.check(importedClasses);
        methodRule.check(importedClasses);
    }
}
