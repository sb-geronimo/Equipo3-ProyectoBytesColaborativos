package es.bytescolab.msmaintenance.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class ProbeController {

    @GetMapping("/probe/from")
    public String getFrom(@RequestParam("from") LocalDate from) {
        return "ok";
    }

    @GetMapping("/probe/from-to")
    public String getFromTo(@RequestParam("from") LocalDate from,
                            @RequestParam("to") LocalDate to) {
        return "ok";
    }
}