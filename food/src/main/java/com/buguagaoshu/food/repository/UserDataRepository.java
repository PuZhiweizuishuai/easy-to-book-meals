package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.UserData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDataRepository extends CrudRepository<UserData, Long> {

    Optional<UserData> findByWxOpenid(String wxOpenid);

    Optional<UserData> findByPhone(String phone);

    Optional<UserData> findByEmail(String email);

    Optional<UserData> findByNickname(String nickname);
}
