package com.study.jpa;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class NPlusOneController {

    private final EntityManager em;
    private final TeamRepository teamRepository;


    /**
     * 순환참조 예외 발생
     * 해결방법 : @ToString.EXCLUDE
     */
    @Transactional
    @GetMapping("/test")
    public void test() {
        // team을 조회해보자 lazy상태
//        Team team = em.find(Team.class, 1L);
//        System.out.println(">>> team : "+ team);
        Team team2 = Team.builder().name("team2").build();
        Team team3 = Team.builder().name("team3").build();
        Team team4 = Team.builder().name("team4").build();
        Team team5 = Team.builder().name("team5").build();
        Team team6 = Team.builder().name("team6").build();
        Team team7 = Team.builder().name("team7").build();
        Team team8 = Team.builder().name("team8").build();

        em.persist(team2);
        em.persist(team3);
        em.persist(team4);
        em.persist(team5);
        em.persist(team6);
        em.persist(team7);
        em.persist(team8);
    }

    /**
     * sql = "SELECT t FROM Team t";
     * sql = "SELECT t FROM Team t where t.id in (1,2,3,4)";
     * OneToMany fetch EAGER시 N+1 확인
     * 해결방법 : fetch LAZY 지연로딩으로 변경
     */
    @Transactional
    @GetMapping("/test1")
    public void test1() {
        // team을 조회해보자 lazy상태
        // 이렇게 findALl 하게되면 join이 안되기에 team에 대한 모든 user를 조회하게 된다.
        String sql = "SELECT t FROM Team t";
        List<Team> teams = em.createQuery(sql).getResultList();
        System.out.println(">>> team : "+teams);
    }

    /**
     * OneToMany fetch LAZY시 N+1 확인 LAZY를 사용할때 특정 index의 team에 대한 member 조회는 괜찮
     * 당연히 모든 team에 대한 members를 조회하면 team 개수에 따른 멤버조회 sql N개 생성
     */
    @Transactional
    @GetMapping("/test2")
    public void test2() {
        // 이렇게 findALl 하게되면 join이 안되기에 team에 대한 모든 user를 조회하게 된다.
        String sql = "SELECT t FROM Team t";
        List<Team> teams = em.createQuery(sql).getResultList();
        //모든 팀의 멤버에 대해서 조회할떄 이러는 거고 특정 index의 팀일때의 members는 1번 조회된다.
//        for(Team t: teams) {
//            System.out.println(t.getMembers());
//        }
        System.out.println(teams.get(0).getMembers());
    }

    /**
     * OneToMany fetch LAZY시 N+1 확인
     * 해결방법 : fetch join 사용 -> 지연로딩이 걸려있어도 한번에 즉시로딩하는 방식
     * 그냥 left join시 team조회하고 teamId에 따른 멤버조회 발생.
     * fetch join시 team과 member를 동시에 즉시 로딩하여 1번의 쿼리 발생.
     */
    @Transactional
    @GetMapping("/test3")
    public void test3() {
        // 이렇게 findALl 하게되면 join이 안되기에 team에 대한 모든 user를 조회하게 된다.
        String sql = "SELECT t FROM Team t left join fetch t.members";
        List<Team> teams = em.createQuery(sql).getResultList();
        //모든 팀의 멤버에 대해서 조회할떄 이러는 거고 특정 index의 팀일때의 members는 1번 조회된다.
        for(Team t: teams) {
            System.out.println(t.getMembers());
        }
    }
    /**
     * OneToMany fetch LAZY시 N+1 확인
     * 해결방법 : @EntityGraph를 통한 fetch join
     * @EntityGraph(attributePaths = {"articles"}, type = EntityGraphType.FETCH)
     * @Query("select distinct u from User u left join u.articles")
     */
    @Transactional
    @GetMapping("/test4")
    public void test4() {
        // 이렇게 findALl 하게되면 join이 안되기에 team에 대한 모든 user를 조회하게 된다.
        String sql = "SELECT t FROM Team t left join fetch t.members";
        List<Team> teams = em.createQuery(sql).getResultList();
        //모든 팀의 멤버에 대해서 조회할떄 이러는 거고 특정 index의 팀일때의 members는 1번 조회된다.
        for(Team t: teams) {
            System.out.println(t.getMembers());
        }
    }

    /**
     * PageNation 문제. 처음에 하나만 조회하는거랑 무슨 차이가 있나 싶었지만 결국 쿼리 하나 나가냐 아니 1+1이냐 1번더 IO가 발생한다는 거임.
     * 기본적인 pagenation은 limit, offset을 통해 진행된다. limit는 가져올 개수, offset은 시작데이터 위치
     * 하지만 fetch join으로 페이지네이션 처리하면 limit, offset을 사용하지 않고
     * 리스트의 모든 데이터를 메모리에 저장해 애플리케이션 내에서 페이징 처리를 진행한다. - >> 메모리 부족문제 발생 야기한다.
     */
    @Transactional
    @GetMapping("/test5")
    public void test5() {
        System.out.println(">>>시작");
        PageRequest pageRequest = PageRequest.of(0,2);
        Page<Team> teams = teamRepository.findAllByPage(pageRequest);
        System.out.println(">>> all");
        for(Team t: teams) {
            System.out.println(t.getMembers());
        }

    }

    // ManyToOne관계에서는 fetchJoin을 해도 페이지네이션 처리가 가능하다.

    /**
     * BatchSize를 통해 해결 -> fetch join 걸면 안된다.
     * 지연로딩할 엔티티를 사이즈에 맞게 즉시로딩해줌으로 N+1을 완화하는 역할을 수행한다.
     */
    @Transactional
    @GetMapping("/test6")
    public void test6() {
        System.out.println(">>>시작");
        PageRequest pageRequest = PageRequest.of(0,2);
        Page<Team> teams = teamRepository.findAll(pageRequest);
        System.out.println(">>> all");
        for(Team t: teams) {
            System.out.println(t.getMembers());
        }

    }

    //@Fetch(FetchMode.SUBSELECT) 이건 모든 teamId에 대해 조회하기에 안쓴다.
}
