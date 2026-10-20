---
name: lombok-support
description: Lombok을 기본 규칙으로 적용하고, 예외적인 경우에만 사용 범위를 좁히는 skill. constructor injection, boilerplate 축소, 필요한 Lombok 애노테이션의 사용 범위를 정리한다.
---

# Skill: lombok-support

## Purpose

일관된 코드를 위해 Lombok을 기본 규칙으로 적용한다. 명시된 예외가 없는 한 반복적인 보일러플레이트를 줄이기 위해 Lombok을 우선 사용하고, 아키텍처나 JPA 동작을 해칠 수 있는 경우에만 예외를 둔다.

## Procedure

1. 확정된 Lombok 사용 설정에 따라 신규 업무 코드에 Lombok을 적용한다. 제공된 공통 Java 코드를 애노테이션으로 다시 작성하지 않는다.
2. Controller와 Service에서는 `@RequiredArgsConstructor` 같은 생성자 주입 보조를 우선 적용한다.
3. DTO, command object, response object 등 반복 필드가 많은 타입에는 `@Getter`, `@Setter`, 생성자 보조 등 필요한 Lombok 애노테이션을 적극적으로 사용한다.
4. Entity와 domain object에 Lombok을 적용할 때는 equals, hashCode, toString이 아키텍처와 JPA 동작을 해치지 않는지 확인한다.
5. 순환 참조, 지연 로딩, 프록시 동작, 직렬화 영향이 있는 경우에는 예외를 명시하고 적용 범위를 좁힌다.
6. Lombok 사용 시 빌드에 compileOnly와 annotationProcessor를 추가하고 테스트에서도 사용한다면 testCompileOnly와 testAnnotationProcessor를 추가한다.

## Heuristics

- Lombok 사용 여부는 확정된 프로젝트 설정을 따른다.
- 생성자 주입 규칙은 유지하고, 주입 코드를 줄이는 용도로 Lombok을 사용한다.
- `@Getter`와 `@Setter`는 반복이 많은 단순 데이터 타입에 우선 적용한다.
- `@ToString`과 `@EqualsAndHashCode`는 순환 참조와 지연 로딩 문제를 고려해 신중하게 사용한다.

## Do Not

- 명시된 예외 없이 Lombok 적용을 빼지 않는다.
- Entity에 `@Data`나 자동 `equals`·`hashCode`를 만드는 Lombok 애노테이션을 쓰지 않는다.
- Lombok 적용을 이유로 constructor injection 규칙을 흐리지 않는다.
- 보일러플레이트 감소를 이유로 설계 규칙을 약하게 만들지 않는다.
