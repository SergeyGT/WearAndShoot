package com.w_s_backend.w_s.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.w_s_backend.w_s.DTOs.ClothCardDTO;
import com.w_s_backend.w_s.Repositories.ClothCardPepository;
import com.w_s_backend.w_s.models.ClothCard;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ClothCardService {
    private final ClothCardPepository _clothCardPepository;


    @Value("${file.upload-dir}")
    private final String UPLOAD_DIR;

    public ClothCard createCard(ClothCardDTO  clothCardDTO){
        if(clothCardDTO.getClothName().isEmpty()) {
            //throw new ApiRequestException("Empty or Null Request data!");
        }

        String imagePath = SaveImage(clothCardDTO.getImage());

        ClothCard createdCard = ClothCard.builder()
            .clothName(clothCardDTO.getClothName())
            .category(clothCardDTO.getCategory())
            .imagePath(imagePath)
            .color(clothCardDTO.getColor())
            .season(clothCardDTO.getSeason())
            .warmthLevel(clothCardDTO.getWarmthLevel())
            .build();

        ClothCard clothCard = _clothCardPepository.save(createdCard);

        return clothCard;
    }

    public List<ClothCard> readAllCards(){
        return _clothCardPepository.findAll();
    }


    private String SaveImage(MultipartFile image){
        if(image == null || image.isEmpty()){
            return "";
        }

        try{
            Path pathUpload = Paths.get(UPLOAD_DIR);

            if (!Files.exists(pathUpload)) {
                Files.createDirectories(pathUpload);
            }

            String originalFileName = image.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            Path filePath = pathUpload.resolve(fileName);
            Files.copy(image.getInputStream(), filePath);
            
            return UPLOAD_DIR + fileName;
        } catch(Exception ex){
                return "";
        }
    }
}
