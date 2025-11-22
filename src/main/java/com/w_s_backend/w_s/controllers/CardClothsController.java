package com.w_s_backend.w_s.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.w_s_backend.w_s.Services.ClothCardService;
import com.w_s_backend.w_s.models.ClothCard;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@AllArgsConstructor
@Slf4j
@RequestMapping("/cloth")
public class CardClothsController {
    @Autowired
    private final ClothCardService clothCardService;


    @GetMapping
    public List<ClothCard> read() {
        return clothCardService.readAllCards();
    }
    
}
