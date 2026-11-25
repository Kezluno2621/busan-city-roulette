package com.cityroulette.controller;
import com.cityroulette.service.PlaceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
/** 탐색 중 찾을 수 없는 장소를 404 화면으로 안내합니다. */
@ControllerAdvice(assignableTypes=PlaceExplorerController.class)
public class PlaceExplorerExceptionHandler {
    @ExceptionHandler(PlaceNotFoundException.class) public ModelAndView notFound(PlaceNotFoundException e){ModelAndView view=new ModelAndView("error/404");view.setStatus(HttpStatus.NOT_FOUND);view.addObject("message",e.getMessage());return view;}
}
