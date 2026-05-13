package com.nailsalon.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nailsalon.booking.entity.PortfolioImage;
import com.nailsalon.booking.mapper.PortfolioImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioImageMapper portfolioImageMapper;

    public List<PortfolioImage> getAll() {
        return portfolioImageMapper.selectList(
                new LambdaQueryWrapper<PortfolioImage>()
                        .eq(PortfolioImage::getIsActive, 1)
                        .orderByAsc(PortfolioImage::getSortOrder)
                        .orderByDesc(PortfolioImage::getCreatedAt)
        );
    }

    @Transactional
    public void addImage(PortfolioImage image) {
        image.setIsActive(1);
        image.setCreatedAt(LocalDateTime.now());
        portfolioImageMapper.insert(image);
    }

    @Transactional
    public void deleteImage(Long id) {
        portfolioImageMapper.deleteById(id);
    }
}
