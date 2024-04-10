package com.chainstep.atlas.example2;

import com.cartrust.atlas.ssikit.AtlasCommunicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


@Slf4j
@RestController
@RequestMapping(path = "example")
public class ExampleSenderController {

    private final AtlasCommunicator communicator;

    public ExampleSenderController(AtlasCommunicator communicator) {
        this.communicator = communicator;
    }

    @GetMapping(value = "send")
    public ResponseEntity<String> receiveAuthorizedRequest() {
        HttpEntity<?> newHttpEntity = communicator.createHttpEntity(null, null, true);
        try {
            var response = communicator.exchange(HttpMethod.GET, newHttpEntity, URI.create("http://localhost:9180/example"), String.class);
            log.info("Response completed: {}", response.getBody());
        } catch (Exception e) {
            log.error("Error sending request", e);
        }
        return new ResponseEntity<String>("OK", HttpStatus.OK);
    }
}
