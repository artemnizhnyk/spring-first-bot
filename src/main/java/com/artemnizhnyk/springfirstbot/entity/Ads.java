package com.artemnizhnyk.springfirstbot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity(name = "ads_table")
@Data
public class Ads {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String ad;
}
