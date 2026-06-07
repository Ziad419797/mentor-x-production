package com.educore.parent;

import com.educore.student.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParentRepository extends JpaRepository<Parent, Long> {

//    Optional<Parent> findByPhone(String phone);
//Optional<Parent> findByStudent(Student student);
//    @Query("SELECT p FROM Parent p WHERE p.student.phone = :phone")
//    Optional<Parent> findByPhone(@Param("phone") String phone);
//    boolean existsByPhone(String phone);


    // 1. دي أهم ميثود محتاجينها عشان الـ Seeding والـ Register والـ Login
    Optional<Parent> findByPhone(String phone);

    // 2. ميثود للتأكد من وجود الرقم (بتستخدميها في الـ Validation)
    boolean existsByPhone(String phone);

    // 3. لو محتاجة تدوري على الأب من خلال ابنه (بعد ما غيرنا العلاقة للجمع)
    // السبرينج بيفهم كلمة Containing لما تكون العلاقة Collection (Set/List)
    Optional<Parent> findByStudentsContaining(Student student);

}
