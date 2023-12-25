package com.artemnizhnyk.springfirstbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.sql.Timestamp;

@Entity(name = "users_data_table")
@Data
public class User {
    @Id
    @Column(name = "chat_id")
    private Long chatId;
    private String firstname;
    private String lastname;
    private String username;
    @Column(name = "registered_at")
    private Timestamp registeredAt;
}
