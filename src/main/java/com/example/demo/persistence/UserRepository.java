package com.example.demo.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.model.UserEntity;

/*===============================================================
*Dec 30th 2021, 
*Authenticated Backend implementation.
*UserRepository.java
*UserEntity를 사용하기 위해서 해당 interface가 필요하다.
*================================================================
*/

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String>{
	UserEntity findByEmail(String email);
	Boolean existsByEmail(String email);
	UserEntity findByEmailAndPassword(String email, String password);
}
