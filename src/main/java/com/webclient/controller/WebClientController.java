package com.webclient.controller;

import com.webclient.dto.Info;
import com.webclient.service.WebClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class WebClientController {

    private final WebClientService webClientService;

    @GetMapping("/webclient/test")
    public void getWebClient(@RequestParam String name, @RequestParam String id){
        webClientService.getWebClientTest(name, id);
    }

    @PostMapping("/webclient/test")
    public void postWebClient(@RequestBody Info info){
        webClientService.postWebClientTest(info.getName(), info.getId());

    }

}
