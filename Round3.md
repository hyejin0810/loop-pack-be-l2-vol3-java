## 도메인 & 객체 설계 전략

비즈니스 규칙 캡슐화: 도메인 객체(Entity, VO)는 데이터만 가진 구조체가 아니라, 자신의 비즈니스 규칙을 스스로 검증하고 수행해야 합니다.


애플리케이션 서비스의 역할: 서로 다른 도메인 객체들을 조합하고 로직을 조정(Orchestration)하여 기능을 완성하는 데 집중하며, 핵심 비즈니스 로직은 도메인으로 위임합니다.



규칙의 위치: 특정 규칙이 여러 서비스에서 중복되어 나타난다면, 해당 규칙은 도메인 객체의 책임일 가능성이 높으므로 도메인 내부로 옮깁니다.


의도적인 설계: 각 기능의 책임 소재와 객체 간 결합도에 대해 개발자의 의도를 명확히 반영하여 개발을 진행합니다.

## 아키텍처 및 패키지 구성 전략
본 프로젝트는 **레이어드 아키텍처(Layered Architecture)**를 기반으로 하며, **DIP(의존성 역전 원칙)**를 jpa 관점에서 적당히 편리한 만큼만  적용한다.

패키지 구조 (Layer + Domain)
패키징은 4개의 계층을 최상위에 두고, 그 하위에 도메인별로 구성합니다.



/interfaces/api: Presentation 레이어로 API 컨트롤러와 요청/응답 객체가 위치합니다.



/application/..: Application 레이어로 도메인 레이어를 조합하여 유스케이스 기능을 제공합니다.



/domain/..: Domain 레이어로 도메인 객체(Entity, VO, Domain Service)와 Repository 인터페이스가 위치합니다.



/infrastructure/..: Infrastructure 레이어로 JPA, Redis 등 기술적인 Repository 구현체를 제공합니다.


데이터 전달 객체(DTO) 정책

DTO 분리: API 계층에서 사용하는 Request/Response DTO와 Application 계층에서 사용하는 DTO를 엄격히 분리하여 작성합니다.

의존성 및 테스트 전략
DIP 적용: 의존성 방향은 항상 Domain을 향해야 합니다. Infrastructure 구현체는 Domain에 정의된 인터페이스를 상속합니다.


단위 테스트: 핵심 도메인 로직은 외부 의존성이 분리된 상태에서 Fake 또는 Stub을 사용하여 테스트 가능한 구조로 설계하고 검증합니다.
+2