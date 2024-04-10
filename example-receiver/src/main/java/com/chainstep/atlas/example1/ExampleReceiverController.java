package com.chainstep.atlas.example1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = "example")
public class ExampleReceiverController {
    @GetMapping(value = "")
    public ResponseEntity<String> receiveAuthorizedRequest(@RequestHeader(value = "Authorization") String auth) {
        return ResponseEntity.ofNullable("Hello World!");
    }
}
