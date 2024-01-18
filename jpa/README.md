## 엔티티 설정
```java
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @BatchSize(size = 100)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "team")
    @ToString.Exclude
    private Set<Member> members;

public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 20)
    private String password;

    @ManyToOne
    private Team team;
}
}

```

## 1. members FetchType.EAGER시 N+1 발생
모든 Team에 대해 조회하면 members에 대한 정보도 즉시 로딩되야 하기에 team 조회 sql 1번 + 모든 팀마다의 멤버 조회 sql N개가 발생한다.      
1차적인 해법으로는 FetchType.LAZY로 설정하면 된다.

## 2. FetchType.LAZY 도 결국 sql N개 발생의 실행시점을 미루는 방식
모든 Team을 조회할 때는 Team에 대한 sql 1개만 발생하는데 추후에 모든 팀 내부의 멤버 정보가 필요해서 객체 그래프 탐색 즉 `getMembers`를 실행하면       
팀마다 멤버를 조회하는 sql이 발생해서 N개의 추가적인 sql이 발생한다.

## 3. 해결방법은?
jpal을 통한 fetch join이다. 
```java
@Query("SELECT t FROM Team t left join fetch t.members)
List<Team> findAll();
```
또는 @EntityGraph을 이용해서 fetch join을 적용히시키는 것이다.
```java
@EntityGraph(attributePaths ={"members"}, type = EntityGraph.EntityGraphType.FETCH)
@Query("SELECT t FROM Team t left join t.members)
List<Team> findAll();
```

## 4. fetch join의 문제점.
fetch join을 통해 N+1의 문제는 해결했지만. 페이지네이션 처리를 할때 기존의 MySQL은 limit, offset을 통해 처리하는데 fetch join을 하면 
모든 데이터를 메모리에 올려둔 후 애플리케이션에서 페이지 처리를 한 뒤에 리턴해줍니다. 이럴 경우 `메모리 부족 현상`이 발생할 수 있어서 오류로 인한 셧다운이 발생할 수 있습니다.      

## 5. 페이지 네이션시 메모리 부족문제 해결
일단 OneToMany관계를 갖는 엔티티에 대해서는 안하는게 좋은 방법이고, ManyToOne은 페이지네이션을 사용해도 됩니다.      
또는 OneToMany 관계를 갖는 엔티티의 필드에 @BatchSize(size=100)를 통해 지연 로딩할 데이터를 배치로딩을 통해 즉시 가져오게 됩니다.    

