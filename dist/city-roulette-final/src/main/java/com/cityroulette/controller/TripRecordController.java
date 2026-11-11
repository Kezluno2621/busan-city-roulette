package com.cityroulette.controller;

import com.cityroulette.domain.TripRecord;
import com.cityroulette.dto.trip.TripAttachment;
import com.cityroulette.dto.trip.TripRecordRequest;
import com.cityroulette.service.FileStorageException;
import com.cityroulette.service.FileValidationException;
import com.cityroulette.service.TripRecordSaveException;
import com.cityroulette.service.TripRecordService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;

@Controller
@RequestMapping("/my/trips")
public class TripRecordController {

    private static final Logger log = LoggerFactory.getLogger(TripRecordController.class);

    private final TripRecordService tripRecordService;

    public TripRecordController(TripRecordService tripRecordService) {
        this.tripRecordService = tripRecordService;
    }

    @GetMapping
    public String list(Principal principal, Model model) {
        model.addAttribute("tripRecords", tripRecordService.findAll(principal.getName()));
        return "my/trips";
    }

    @GetMapping("/new")
    public String createForm(
            @RequestParam(name = "placeName", defaultValue = "") String placeName,
            @RequestParam(name = "placeAddress", defaultValue = "") String placeAddress,
            Model model) {
        TripRecordRequest request = new TripRecordRequest();
        request.setPlaceName(placeName);
        request.setPlaceAddress(placeAddress);
        request.setVisitDate(LocalDate.now());
        model.addAttribute("tripRecordRequest", request);
        return "my/trip-form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("tripRecordRequest") TripRecordRequest request,
            BindingResult bindingResult,
            Principal principal) {
        if (bindingResult.hasErrors()) {
            return "my/trip-form";
        }

        try {
            TripRecord saved = tripRecordService.create(principal.getName(), request);
            return "redirect:/my/trips/" + saved.getId();
        } catch (FileValidationException exception) {
            bindingResult.rejectValue("image", "invalidFile", exception.getMessage());
        } catch (FileStorageException | TripRecordSaveException exception) {
            log.error("여행 기록 저장 중 파일 또는 DB 처리에 실패했습니다.", exception);
            bindingResult.reject("saveFailed", "여행 기록을 저장하지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
        return "my/trip-form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("tripRecord", tripRecordService.findOne(principal.getName(), id));
        return "my/trip-detail";
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long id,
            Principal principal) {
        TripAttachment attachment = tripRecordService.loadAttachment(principal.getName(), id);
        MediaType mediaType = parseMediaType(attachment.contentType());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(attachment.originalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(attachment.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(attachment.resource());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String oversizedUpload(Model model) {
        TripRecordRequest request = new TripRecordRequest();
        request.setVisitDate(LocalDate.now());
        model.addAttribute("tripRecordRequest", request);
        model.addAttribute("uploadError", "파일 크기는 5MB 이하만 가능합니다.");
        return "my/trip-form";
    }

    private MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
