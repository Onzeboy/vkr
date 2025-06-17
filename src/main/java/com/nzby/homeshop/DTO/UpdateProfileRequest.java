package com.nzby.homeshop.DTO;


import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @Size(max = 50, message = "Имя не может превышать 50 символов")
    private String name;

    @Size(max = 50, message = "Фамилия не может превышать 50 символов")
    private String surname;

    @Pattern(regexp = "^(\\+7|8)?\\d{10}$|^$", message = "Номер телефона должен начинаться с +7 или 8 и содержать 10 цифр, или быть пустым")
    private String phoneNumber;

    // Геттеры и сеттеры
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}