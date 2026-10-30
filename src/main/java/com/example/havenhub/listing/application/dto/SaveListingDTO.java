package com.example.havenhub.listing.application.dto;

import com.example.havenhub.listing.application.dto.sub.DescriptionDTO;
import com.example.havenhub.listing.application.dto.sub.ListingInfoDTO;
import com.example.havenhub.listing.application.dto.sub.PictureDTO;
import com.example.havenhub.listing.application.dto.vo.PriceVO;
import com.example.havenhub.listing.domain.BookingCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class SaveListingDTO {

    @NotNull
    BookingCategory category;

    @NotBlank @Size(max = 256)
    String location;

    @NotNull @Valid
    ListingInfoDTO infos;

    @NotNull @Valid
    DescriptionDTO description;

    @NotNull @Valid
    PriceVO price;

    @NotNull
    @Size(min = 5, max = 20, message = "A listing needs between 5 and 20 pictures")
    List<@NotNull @Valid PictureDTO> pictures;

    // The shared GuestsVO allows 0 because search uses 0 for "any"; a listing
    // must host at least one guest.
    @JsonIgnore
    @AssertTrue(message = "A listing must host at least 1 guest")
    public boolean isHostingAtLeastOneGuest() {
        return infos == null || infos.guests() == null || infos.guests().value() >= 1;
    }

    public BookingCategory getCategory() {
        return category;
    }

    public void setCategory(BookingCategory category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ListingInfoDTO getInfos() {
        return infos;
    }

    public void setInfos(ListingInfoDTO infos) {
        this.infos = infos;
    }

    public DescriptionDTO getDescription() {
        return description;
    }

    public void setDescription(DescriptionDTO description) {
        this.description = description;
    }

    public PriceVO getPrice() {
        return price;
    }

    public void setPrice(PriceVO price) {
        this.price = price;
    }

    public List<PictureDTO> getPictures() {
        return pictures;
    }

    public void setPictures(List<PictureDTO> pictures) {
        this.pictures = pictures;
    }
}
