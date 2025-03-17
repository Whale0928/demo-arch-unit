package app.demoarchunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static jakarta.persistence.EnumType.STRING;

public class EntityArchitectureTest {
    private JavaClasses importedClasses;

    @BeforeEach
    public void setup() {
        importedClasses = new ClassFileImporter().importPackages("app.demoarchunit");
    }

    @Test
    public void 엔티티_클래스는_Entity와_Table_애노테이션을_가져야_함() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should().beAnnotatedWith(Table.class)
                .because("모든 엔티티 클래스는 @Entity와 @Table 애노테이션을 모두 가져야 합니다");

        rule.check(importedClasses);
    }

    @Test
    public void 생성자는_적절한_접근_제한자를_가져야_함() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should(haveConstructorsWithAccessModifiers())
                .because("엔티티의 생성자는 적절한 접근 제한자(protected, private)를 가져야 합니다");
        rule.check(importedClasses);
    }

    @Test
    public void 엔티티는_기본_생성자_내에서_초기화_로직이_없어야_함() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(Entity.class)
                .should(haveEmptyDefaultConstructor())
                .because("엔티티의 기본 생성자는 JPA에서만 사용되기 위해 비어있어야 합니다");

        rule.check(importedClasses);
    }

    private ArchCondition<JavaClass> haveConstructorsWithAccessModifiers() {
        return new ArchCondition<>("생성자가 적절한 접근 제한자를 가져야 함") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasNoArgsConstructor = javaClass.isAnnotatedWith(NoArgsConstructor.class);
                boolean hasAllArgsConstructor = javaClass.isAnnotatedWith(AllArgsConstructor.class);

                if (hasNoArgsConstructor && hasAllArgsConstructor) {
                    NoArgsConstructor noArgsAnnotation = javaClass.getAnnotationOfType(NoArgsConstructor.class);
                    AllArgsConstructor allArgsAnnotation = javaClass.getAnnotationOfType(AllArgsConstructor.class);

                    // 접근 제한자 확인
                    lombok.AccessLevel noArgsAccess = noArgsAnnotation.access();
                    lombok.AccessLevel allArgsAccess = allArgsAnnotation.access();

                    boolean isValidNoArgsAccess = noArgsAccess == lombok.AccessLevel.PROTECTED || noArgsAccess == lombok.AccessLevel.PRIVATE;
                    boolean isValidAllArgsAccess = allArgsAccess == lombok.AccessLevel.PROTECTED || allArgsAccess == lombok.AccessLevel.PRIVATE;

                    if (!isValidNoArgsAccess || !isValidAllArgsAccess) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                "생성자의 접근 제한자가 적절하지 않습니다. NoArgsConstructor: " + noArgsAccess +
                                        ", AllArgsConstructor: " + allArgsAccess + " (protected 또는 private이어야 함)"
                        ));
                    } else {
                        events.add(SimpleConditionEvent.satisfied(javaClass, "생성자의 접근 제한자가 적절합니다"));
                    }
                }
            }
        };
    }

    private ArchCondition<JavaClass> haveEmptyDefaultConstructor() {
        return new ArchCondition<>("기본 생성자가 비어있어야 함") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                // 기본 생성자 찾기
                boolean hasNoArgsConstructor = javaClass.isAnnotatedWith(NoArgsConstructor.class);

                if (hasNoArgsConstructor) {
                    // 실제 생성자 구현을 확인하는 것은 바이트코드 분석으로는 한계가 있으므로,
                    // 애노테이션의 force 속성을 확인하여 초기화 로직이 있는지 간접적으로 추론
                    NoArgsConstructor annotation = javaClass.getAnnotationOfType(NoArgsConstructor.class);
                    boolean hasForce = annotation.force();

                    if (hasForce) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass,
                                "NoArgsConstructor에 force=true가 설정되어 있어 초기화 로직이 있을 수 있습니다"
                        ));
                    } else {
                        events.add(SimpleConditionEvent.satisfied(javaClass, "기본 생성자가 적절합니다"));
                    }
                }
            }
        };
    }
}
