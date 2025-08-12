package org.devdaniel.clone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private String password;
    @Enumerated(EnumType.STRING)
    private ProfileRole role;
    @Column(nullable = false)
    private Integer credit;

    public void removeCredits(int quantity) {
        if(credit >= quantity){
            credit -= quantity;
        }
    }

}
