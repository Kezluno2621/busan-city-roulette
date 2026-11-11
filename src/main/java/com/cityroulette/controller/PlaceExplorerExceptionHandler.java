package com.cityroulette.controller;
import com.cityroulette.service.PlaceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
@ControllerAdvice(assignableTypes=PlaceExplorerController.class)
public class PlaceExplorerExceptionHandler {
    @ExceptionHandler(PlaceNotFoundException.class) public ModelAndView notFound(PlaceNotFoundException e){ModelAndView view=new ModelAndView("error/404");view.setStatus(HttpStatus.NOT_FOUND);view.addObject("message",e.getMessage());return view;}
}
