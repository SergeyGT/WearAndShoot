package com.w_s_backend.w_s.Services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.w_s_backend.w_s.DTOs.ClothCardDTO;
import com.w_s_backend.w_s.Repositories.ClothCardPepository;
import com.w_s_backend.w_s.models.ClothCard;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ClothCardService {
    private final ClothCardPepository _clothCardPepository;

    public void createCard(ClothCardDTO  clothCardDTO){
        _clothCardPepository.save(null);
    }

    public List<ClothCard> readAllCards(){
        return _clothCardPepository.findAll();
    }

}
