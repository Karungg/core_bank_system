package com.miftah.core_bank_system.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponse {

    private UUID id;

    private UUID userId;

    private ProfileType type;

    private LocalDate expiryDate;

    private String identityNumber;

    private String name;

    private String country;

    private String placeOfBirth;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String phone;

    private String nationality;

    private MaritalStatus maritalStatus;

    private String religion;

    private String address;

    private String building;

    private String rt;

    private String rw;

    private String province;

    private String documentPhoto;

    private String occupation;

    private String officeName;

    private String officeAddress;

    private String officePhone;

    private Instant createdAt;

    private Instant updatedAt;
}
