package app.demoarchunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * DTO 및 관련 데이터 클래스의 아키텍처 규칙을 검증하는 테스트 클래스입니다.
 */
public class DataClassArchitectureTest {
    private JavaClasses importedClasses;

    @BeforeEach
    public void setup() {
        importedClasses = new ClassFileImporter().importPackages("app.demoarchunit");
    }

    /**
     * 요청 DTO 클래스 명명 규칙을 검증합니다.
     * 모든 요청 DTO는 이름이 'Request'로 끝나야 합니다.
     */
    @Test
    public void 요청_DTO_명명_규칙_검증() {
        ArchRule rule = classes()
                .that().resideInAPackage("..dto.request..")
                .should().haveSimpleNameEndingWith("Request")
                .allowEmptyShould(true)

                .because("요청 DTO 클래스는 명확한 식별을 위해 'Request'로 끝나야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 응답 DTO 클래스 명명 규칙을 검증합니다.
     * 모든 응답 DTO는 이름이 'Response'로 끝나야 합니다.
     */
    @Test
    public void 응답_DTO_명명_규칙_검증() {
        ArchRule rule = classes()
                .that().resideInAPackage("..dto.response..")
                .should().haveSimpleNameEndingWith("Response")
                .allowEmptyShould(true)
                .because("응답 DTO 클래스는 명확한 식별을 위해 'Response'로 끝나야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 이벤트 클래스 명명 규칙을 검증합니다.
     * 모든 이벤트 클래스는 이름이 'Event'로 끝나야 합니다.
     */
    @Test
    public void 이벤트_클래스_명명_규칙_검증() {
        ArchRule rule = classes()
                .that().resideInAPackage("..messaging.event..")
                .should().haveSimpleNameEndingWith("Event")
                .allowEmptyShould(true)

                .because("이벤트 클래스는 명확한 식별을 위해 'Event'로 끝나야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 이벤트 클래스 구조를 검증합니다.
     * 이벤트 클래스는 불변(필드 변경 불가능)이어야 합니다.
     */
    @Test
    public void 이벤트_클래스_불변성_검증() {
        ArchRule rule = classes()
                .that().resideInAPackage("..messaging.event..")
                .should().bePublic()
                .andShould().haveOnlyFinalFields()
                .allowEmptyShould(true)
                .because("이벤트 클래스는 public이어야 하며, 불변성을 위해 모든 필드는 final이어야 합니다");

        rule.check(importedClasses);
    }

    /**
     * 데이터 클래스 의존성 검증
     * DTO와 이벤트 클래스는 도메인 모델에 의존할 수 있지만, 서비스나 컨트롤러에 의존해서는 안 됩니다.
     */
    @Test
    public void 데이터_클래스_의존성_검증() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..dto..", "..messaging..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        "..dto..", "..messaging..", "..domain..",  // 허용된 의존성
                        "java..", "javax..", "lombok.."            // 표준 라이브러리 및 유틸리티
                )
                .allowEmptyShould(true)
                .because("데이터 클래스는 서비스나 컨트롤러 계층에 의존해서는 안 됩니다");

        rule.check(importedClasses);
    }
}
