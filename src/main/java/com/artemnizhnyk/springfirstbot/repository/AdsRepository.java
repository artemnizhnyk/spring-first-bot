package com.artemnizhnyk.springfirstbot.repository;

import com.artemnizhnyk.springfirstbot.entity.Ads;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdsRepository extends CrudRepository<Ads, Long> {
}
