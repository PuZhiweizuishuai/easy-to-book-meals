package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.UserData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDataRepository extends PagingAndSortingRepository<UserData, Long>, CrudRepository<UserData, Long> {

    Optional<UserData> findByWxOpenid(String wxOpenid);

    Optional<UserData> findByPhone(String phone);

    Optional<UserData> findByEmail(String email);

    Optional<UserData> findByNickname(String nickname);

    Page<UserData> findByNicknameLike(String nickname, Pageable pageable);

    Page<UserData> findAll(Pageable pageable);
}
